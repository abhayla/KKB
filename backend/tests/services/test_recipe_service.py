"""Tests for recipe_service.

Covers:
- build_recipe_response: pure fn — response mapping + ingredient/nutrition scaling
- get_recipe_by_id: NotFound paths (invalid UUID + non-existent)
- scale_recipe: NotFound paths
- rate_recipe: issue #21 — full coverage of upsert + validation branches

The DB-backed happy paths for get_recipe_by_id / scale_recipe / search /
suggest_from_pantry need heavy Recipe+Ingredient+Nutrition fixtures and
are exercised by the existing recipe integration tests.
"""

import uuid
from sqlalchemy import select

import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import NotFoundError
from app.models.recipe import Recipe, RecipeIngredient, RecipeInstruction, RecipeNutrition, RecipeRating
from app.models.user import User
from app.services.recipe_service import (
    build_recipe_response,
    get_recipe_by_id,
    rate_recipe,
    scale_recipe,
)


# ==================== Helpers ====================


def _make_recipe(
    *,
    servings: int = 4,
    ingredients: list[dict] | None = None,
    instructions: list[dict] | None = None,
    nutrition: RecipeNutrition | None = None,
    name: str = "Dal Tadka",
) -> Recipe:
    r = Recipe(
        id=str(uuid.uuid4()),
        name=name,
        description="Test dish",
        image_url=None,
        cuisine_type="north",
        meal_types=["lunch", "dinner"],
        dietary_tags=["vegetarian"],
        difficulty_level="easy",
        prep_time_minutes=15,
        cook_time_minutes=20,
        total_time_minutes=35,
        servings=servings,
        is_active=True,
    )
    r.ingredients = [
        RecipeIngredient(
            id=str(uuid.uuid4()),
            recipe_id=r.id,
            name=i["name"],
            quantity=i["quantity"],
            unit=i["unit"],
            category=i.get("category", "pulses"),
            is_optional=i.get("is_optional", False),
            order=idx,
        )
        for idx, i in enumerate(ingredients or [])
    ]
    r.instructions = [
        RecipeInstruction(
            id=str(uuid.uuid4()),
            recipe_id=r.id,
            step_number=inst["step_number"],
            instruction=inst["instruction"],
            duration_minutes=inst.get("duration_minutes"),
            timer_required=inst.get("timer_required", False),
        )
        for inst in (instructions or [])
    ]
    r.nutrition = nutrition
    return r


# ==================== build_recipe_response ====================


class TestBuildRecipeResponse:
    def test_maps_basic_fields(self):
        recipe = _make_recipe()
        response = build_recipe_response(recipe)

        assert response.id == str(recipe.id)
        assert response.name == "Dal Tadka"
        assert response.cuisine_type == "north"
        assert response.difficulty == "easy"
        assert response.meal_types == ["lunch", "dinner"]
        assert response.dietary_tags == ["vegetarian"]

    def test_ingredients_quantity_formatted_as_string(self):
        recipe = _make_recipe(
            ingredients=[
                {"name": "Toor Dal", "quantity": 200.0, "unit": "g"},
                {"name": "Salt", "quantity": 2.5, "unit": "tsp"},
            ]
        )
        response = build_recipe_response(recipe)

        qtys = {ing.name: ing.quantity for ing in response.ingredients}
        # Whole number rendered without decimals, non-whole preserves one decimal.
        assert qtys["Toor Dal"] == "200"
        assert qtys["Salt"] == "2.5"

    def test_scale_factor_applies_to_ingredient_quantities(self):
        recipe = _make_recipe(
            ingredients=[{"name": "Rice", "quantity": 100.0, "unit": "g"}],
        )
        response = build_recipe_response(recipe, scale_factor=2.0)
        assert response.ingredients[0].quantity == "200"

    def test_scale_factor_applies_to_servings(self):
        recipe = _make_recipe(servings=4)
        response = build_recipe_response(recipe, scale_factor=2.0)
        assert response.servings == 8

    def test_scale_factor_applies_to_nutrition(self):
        nutrition = RecipeNutrition(
            id=str(uuid.uuid4()),
            recipe_id="placeholder",
            calories=200,
            protein_grams=10,
            carbohydrates_grams=30,
            fat_grams=5,
            fiber_grams=4,
            sugar_grams=2,
            sodium_mg=300,
        )
        recipe = _make_recipe(nutrition=nutrition)

        response = build_recipe_response(recipe, scale_factor=1.5)

        assert response.nutrition is not None
        assert response.nutrition.calories == 300
        assert response.nutrition.protein == 15
        assert response.nutrition.carbohydrates == 45
        assert response.nutrition.fat == 7   # int(5 * 1.5) = int(7.5) = 7
        assert response.nutrition.sodium == 450

    def test_nutrition_is_none_when_not_loaded(self):
        recipe = _make_recipe(nutrition=None)
        response = build_recipe_response(recipe)
        assert response.nutrition is None

    def test_instructions_mapped_with_all_fields(self):
        recipe = _make_recipe(
            instructions=[
                {"step_number": 1, "instruction": "Boil water", "duration_minutes": 5, "timer_required": True},
                {"step_number": 2, "instruction": "Add dal"},
            ]
        )
        response = build_recipe_response(recipe)

        assert len(response.instructions) == 2
        assert response.instructions[0].step_number == 1
        assert response.instructions[0].duration_minutes == 5
        assert response.instructions[0].timer_required is True
        assert response.instructions[1].duration_minutes is None

    def test_empty_description_maps_to_empty_string_not_none(self):
        recipe = _make_recipe()
        recipe.description = None
        response = build_recipe_response(recipe)
        assert response.description == ""

    def test_difficulty_defaults_to_medium_when_missing(self):
        recipe = _make_recipe()
        recipe.difficulty_level = None
        response = build_recipe_response(recipe)
        assert response.difficulty == "medium"


# ==================== get_recipe_by_id NotFound paths ====================


@pytest.mark.asyncio
async def test_get_recipe_by_id_rejects_invalid_uuid(
    db_session: AsyncSession, test_user: User
):
    with pytest.raises(NotFoundError):
        await get_recipe_by_id(db_session, "not-a-uuid")


@pytest.mark.asyncio
async def test_get_recipe_by_id_not_found_when_missing(
    db_session: AsyncSession, test_user: User
):
    with pytest.raises(NotFoundError):
        await get_recipe_by_id(db_session, str(uuid.uuid4()))


# ==================== scale_recipe NotFound paths ====================


@pytest.mark.asyncio
async def test_scale_recipe_rejects_invalid_uuid(
    db_session: AsyncSession, test_user: User
):
    with pytest.raises(NotFoundError):
        await scale_recipe(db_session, "not-a-uuid", target_servings=6)


@pytest.mark.asyncio
async def test_scale_recipe_not_found_when_missing(
    db_session: AsyncSession, test_user: User
):
    with pytest.raises(NotFoundError):
        await scale_recipe(db_session, str(uuid.uuid4()), target_servings=6)


# ==================== rate_recipe (issue #21) ====================


async def _persist_minimal_recipe(db_session: AsyncSession, **overrides) -> Recipe:
    """Persist a minimally-valid Recipe row. rate_recipe only needs existence + is_active."""
    defaults: dict = {
        "id": str(uuid.uuid4()),
        "name": "Test Recipe",
        "description": "desc",
        "image_url": None,
        "cuisine_type": "north",
        "meal_types": ["dinner"],
        "dietary_tags": ["vegetarian"],
        "difficulty_level": "easy",
        "prep_time_minutes": 10,
        "cook_time_minutes": 15,
        "total_time_minutes": 25,
        "servings": 2,
        "is_active": True,
    }
    defaults.update(overrides)
    recipe = Recipe(**defaults)
    db_session.add(recipe)
    await db_session.commit()
    await db_session.refresh(recipe)
    return recipe


@pytest.mark.asyncio
async def test_rate_recipe_rejects_invalid_uuid(
    db_session: AsyncSession, test_user: User
):
    with pytest.raises(NotFoundError):
        await rate_recipe(
            db=db_session, recipe_id="not-a-uuid", user_id=test_user.id, rating=4.0
        )


@pytest.mark.asyncio
async def test_rate_recipe_not_found_when_recipe_missing(
    db_session: AsyncSession, test_user: User
):
    with pytest.raises(NotFoundError):
        await rate_recipe(
            db=db_session,
            recipe_id=str(uuid.uuid4()),
            user_id=test_user.id,
            rating=4.0,
        )


@pytest.mark.asyncio
async def test_rate_recipe_not_found_when_recipe_inactive(
    db_session: AsyncSession, test_user: User
):
    recipe = await _persist_minimal_recipe(db_session, is_active=False)
    with pytest.raises(NotFoundError):
        await rate_recipe(
            db=db_session, recipe_id=recipe.id, user_id=test_user.id, rating=5.0
        )


@pytest.mark.asyncio
async def test_rate_recipe_creates_new_rating_when_none_exists(
    db_session: AsyncSession, test_user: User
):
    recipe = await _persist_minimal_recipe(db_session)

    response = await rate_recipe(
        db=db_session,
        recipe_id=recipe.id,
        user_id=test_user.id,
        rating=4.5,
        feedback="solid weeknight dal",
    )

    assert response.recipe_id == recipe.id
    assert response.rating == 4.5
    assert response.feedback == "solid weeknight dal"
    assert response.id is not None
    # Persisted row matches
    row = (
        await db_session.execute(
            select(RecipeRating).where(
                RecipeRating.recipe_id == recipe.id,
                RecipeRating.user_id == test_user.id,
            )
        )
    ).scalar_one()
    assert row.rating == 4.5
    assert row.feedback == "solid weeknight dal"


@pytest.mark.asyncio
async def test_rate_recipe_updates_existing_rating_upsert(
    db_session: AsyncSession, test_user: User
):
    recipe = await _persist_minimal_recipe(db_session)

    first = await rate_recipe(
        db=db_session,
        recipe_id=recipe.id,
        user_id=test_user.id,
        rating=3.0,
        feedback="okay",
    )
    second = await rate_recipe(
        db=db_session,
        recipe_id=recipe.id,
        user_id=test_user.id,
        rating=5.0,
        feedback="nailed it second time",
    )

    # Upsert — same row id, updated values
    assert second.id == first.id
    assert second.rating == 5.0
    assert second.feedback == "nailed it second time"
    # Only one row exists for (recipe, user)
    rows = (
        await db_session.execute(
            select(RecipeRating).where(
                RecipeRating.recipe_id == recipe.id,
                RecipeRating.user_id == test_user.id,
            )
        )
    ).scalars().all()
    assert len(rows) == 1


@pytest.mark.asyncio
async def test_rate_recipe_allows_null_feedback(
    db_session: AsyncSession, test_user: User
):
    recipe = await _persist_minimal_recipe(db_session)

    response = await rate_recipe(
        db=db_session,
        recipe_id=recipe.id,
        user_id=test_user.id,
        rating=4.0,
        feedback=None,
    )

    assert response.rating == 4.0
    assert response.feedback is None


@pytest.mark.asyncio
async def test_rate_recipe_update_can_clear_feedback(
    db_session: AsyncSession, test_user: User
):
    recipe = await _persist_minimal_recipe(db_session)

    await rate_recipe(
        db=db_session,
        recipe_id=recipe.id,
        user_id=test_user.id,
        rating=3.0,
        feedback="first pass note",
    )
    updated = await rate_recipe(
        db=db_session,
        recipe_id=recipe.id,
        user_id=test_user.id,
        rating=4.0,
        feedback=None,
    )

    assert updated.rating == 4.0
    assert updated.feedback is None


# ==================== Rating aggregation surfacing (issue #21 Part 2) ====================


class TestBuildRecipeResponseRatingSummary:
    """build_recipe_response should surface average_rating/rating_count/user_rating."""

    def test_no_rating_summary_defaults_to_null_and_zero(self):
        recipe = _make_recipe()
        response = build_recipe_response(recipe)

        assert response.average_rating is None
        assert response.rating_count == 0
        assert response.user_rating is None

    def test_rating_summary_populates_fields(self):
        recipe = _make_recipe()
        summary = {
            "average_rating": 4.25,
            "rating_count": 4,
            "user_rating": 5.0,
        }
        response = build_recipe_response(recipe, rating_summary=summary)

        assert response.average_rating == 4.25
        assert response.rating_count == 4
        assert response.user_rating == 5.0

    def test_rating_summary_with_no_user_rating(self):
        recipe = _make_recipe()
        summary = {
            "average_rating": 3.5,
            "rating_count": 2,
            "user_rating": None,
        }
        response = build_recipe_response(recipe, rating_summary=summary)

        assert response.average_rating == 3.5
        assert response.rating_count == 2
        assert response.user_rating is None


@pytest.mark.asyncio
async def test_get_recipe_by_id_with_no_ratings(
    db_session: AsyncSession, test_user: User
):
    recipe = await _persist_minimal_recipe(db_session)

    response = await get_recipe_by_id(db_session, recipe.id, user_id=test_user.id)

    assert response.average_rating is None
    assert response.rating_count == 0
    assert response.user_rating is None


@pytest.mark.asyncio
async def test_get_recipe_by_id_without_user_id_omits_user_rating(
    db_session: AsyncSession, test_user: User
):
    recipe = await _persist_minimal_recipe(db_session)
    await rate_recipe(
        db=db_session,
        recipe_id=recipe.id,
        user_id=test_user.id,
        rating=4.0,
    )

    response = await get_recipe_by_id(db_session, recipe.id)

    assert response.rating_count == 1
    assert response.average_rating == 4.0
    assert response.user_rating is None


@pytest.mark.asyncio
async def test_get_recipe_by_id_with_user_id_returns_user_rating(
    db_session: AsyncSession, test_user: User
):
    recipe = await _persist_minimal_recipe(db_session)
    await rate_recipe(
        db=db_session,
        recipe_id=recipe.id,
        user_id=test_user.id,
        rating=4.0,
    )

    response = await get_recipe_by_id(db_session, recipe.id, user_id=test_user.id)

    assert response.user_rating == 4.0
    assert response.rating_count == 1
    assert response.average_rating == 4.0


@pytest.mark.asyncio
async def test_get_recipe_by_id_user_not_rated_returns_null_user_rating(
    db_session: AsyncSession, test_user: User
):
    recipe = await _persist_minimal_recipe(db_session)
    # Different user rates — create one directly via the model to avoid needing a second user fixture
    other_rating = RecipeRating(
        id=str(uuid.uuid4()),
        recipe_id=recipe.id,
        user_id=str(uuid.uuid4()),  # some other user
        rating=3.0,
        feedback=None,
    )
    db_session.add(other_rating)
    await db_session.commit()

    response = await get_recipe_by_id(db_session, recipe.id, user_id=test_user.id)

    assert response.rating_count == 1
    assert response.average_rating == 3.0
    # test_user hasn't rated, so their user_rating should be null
    assert response.user_rating is None


@pytest.mark.asyncio
async def test_get_recipe_by_id_averages_multiple_ratings(
    db_session: AsyncSession, test_user: User
):
    recipe = await _persist_minimal_recipe(db_session)

    # Three different users rate: 3, 4, 5 → average 4.0
    for rating_value in (3.0, 4.0, 5.0):
        db_session.add(
            RecipeRating(
                id=str(uuid.uuid4()),
                recipe_id=recipe.id,
                user_id=str(uuid.uuid4()),
                rating=rating_value,
                feedback=None,
            )
        )
    await db_session.commit()

    response = await get_recipe_by_id(db_session, recipe.id)

    assert response.rating_count == 3
    assert response.average_rating == 4.0
