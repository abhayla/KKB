"""Tests for grocery_service.

Covers:
- _normalize_unit: pure unit mapping
- get_grocery_list_for_meal_plan: happy path, invalid UUID, missing plan,
  ingredient aggregation, servings scaling, optional-ingredient skipping
- get_grocery_list_whatsapp: formatted text output

Requirement: #35 - Service-level unit tests for grocery operations.
"""

import uuid
from datetime import date, timedelta

import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import NotFoundError
from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.recipe import Recipe, RecipeIngredient
from app.models.user import User
from app.services.grocery_service import (
    _normalize_unit,
    get_grocery_list_for_meal_plan,
    get_grocery_list_whatsapp,
)


# ==================== Helpers ====================


def _make_recipe(
    name: str = "Paneer Butter Masala",
    servings: int = 4,
    ingredients: list[dict] | None = None,
) -> Recipe:
    """Create a Recipe with the given ingredients (each dict matches RecipeIngredient fields)."""
    r = Recipe(
        id=str(uuid.uuid4()),
        name=name,
        cuisine_type="north",
        meal_types=["dinner"],
        dietary_tags=["vegetarian"],
        prep_time_minutes=15,
        cook_time_minutes=30,
        total_time_minutes=45,
        servings=servings,
    )
    if ingredients:
        r.ingredients = [
            RecipeIngredient(
                id=str(uuid.uuid4()),
                recipe_id=r.id,
                name=i["name"],
                quantity=i["quantity"],
                unit=i["unit"],
                category=i["category"],
                is_optional=i.get("is_optional", False),
                order=idx,
            )
            for idx, i in enumerate(ingredients)
        ]
    else:
        r.ingredients = []
    return r


def _make_meal_plan(
    user_id: str,
    *,
    week_start: date | None = None,
    is_active: bool = True,
) -> MealPlan:
    start = week_start or (date.today() - timedelta(days=date.today().weekday()))
    return MealPlan(
        id=str(uuid.uuid4()),
        user_id=user_id,
        week_start_date=start,
        week_end_date=start + timedelta(days=6),
        is_active=is_active,
    )


def _make_meal_plan_item(
    meal_plan_id: str,
    recipe_id: str,
    *,
    item_date: date | None = None,
    meal_type: str = "dinner",
    servings: int = 4,
) -> MealPlanItem:
    return MealPlanItem(
        id=str(uuid.uuid4()),
        meal_plan_id=meal_plan_id,
        recipe_id=recipe_id,
        date=item_date or date.today(),
        meal_type=meal_type,
        servings=servings,
    )


# ==================== _normalize_unit ====================


class TestNormalizeUnit:
    def test_maps_short_forms_to_canonical(self):
        assert _normalize_unit("g") == "grams"
        assert _normalize_unit("kg") == "kg"
        assert _normalize_unit("ml") == "ml"
        assert _normalize_unit("tbsp") == "tbsp"
        assert _normalize_unit("tsp") == "tsp"
        assert _normalize_unit("pc") == "pieces"

    def test_handles_uppercase_and_whitespace(self):
        assert _normalize_unit(" TBSP ") == "tbsp"
        assert _normalize_unit("GRAM") == "grams"

    def test_passes_through_unknown_units(self):
        # Unknown units should come back as-is (lowercased+stripped).
        assert _normalize_unit("bunch") == "bunch"

    def test_plural_variants(self):
        assert _normalize_unit("tablespoons") == "tbsp"
        assert _normalize_unit("teaspoons") == "tsp"
        assert _normalize_unit("kilograms") == "kg"


# ==================== get_grocery_list_for_meal_plan ====================


@pytest.mark.asyncio
async def test_invalid_uuid_raises_not_found(
    db_session: AsyncSession, test_user: User
):
    """An unparseable meal_plan_id should raise NotFoundError, not ValueError."""
    with pytest.raises(NotFoundError):
        await get_grocery_list_for_meal_plan(db_session, test_user, "not-a-uuid")


@pytest.mark.asyncio
async def test_missing_plan_raises_not_found(
    db_session: AsyncSession, test_user: User
):
    """A syntactically valid but non-existent meal_plan_id should raise NotFoundError."""
    missing_id = str(uuid.uuid4())
    with pytest.raises(NotFoundError):
        await get_grocery_list_for_meal_plan(db_session, test_user, missing_id)


@pytest.mark.asyncio
async def test_aggregates_duplicate_ingredients_across_recipes(
    db_session: AsyncSession, test_user: User
):
    """Same (name, unit, category) across two recipes should aggregate into one entry."""
    recipe_a = _make_recipe(
        name="Dish A",
        servings=4,
        ingredients=[
            {"name": "Onion", "quantity": 200.0, "unit": "g", "category": "vegetables"},
        ],
    )
    recipe_b = _make_recipe(
        name="Dish B",
        servings=4,
        ingredients=[
            {"name": "Onion", "quantity": 150.0, "unit": "g", "category": "vegetables"},
        ],
    )
    plan = _make_meal_plan(test_user.id)
    item_a = _make_meal_plan_item(plan.id, recipe_a.id, servings=4)
    item_b = _make_meal_plan_item(plan.id, recipe_b.id, servings=4)

    db_session.add_all([recipe_a, recipe_b, plan, item_a, item_b])
    await db_session.commit()

    result = await get_grocery_list_for_meal_plan(db_session, test_user, plan.id)

    # Find the vegetables category
    veg_cats = [c for c in result.categories if c.category.lower() == "vegetables"]
    assert len(veg_cats) == 1
    onion_entries = [i for i in veg_cats[0].items if i.name.lower() == "onion"]
    assert len(onion_entries) == 1
    # Units got normalized (g -> grams) and quantities summed.
    assert onion_entries[0].unit == "grams"
    assert onion_entries[0].quantity == 350.0


@pytest.mark.asyncio
async def test_scales_ingredient_quantity_by_servings(
    db_session: AsyncSession, test_user: User
):
    """If the item has more servings than the recipe, ingredient quantities scale up."""
    recipe = _make_recipe(
        name="Scaled Dish",
        servings=2,
        ingredients=[
            {"name": "Rice", "quantity": 100.0, "unit": "g", "category": "grains"},
        ],
    )
    plan = _make_meal_plan(test_user.id)
    item = _make_meal_plan_item(plan.id, recipe.id, servings=6)  # 3x recipe servings

    db_session.add_all([recipe, plan, item])
    await db_session.commit()

    result = await get_grocery_list_for_meal_plan(db_session, test_user, plan.id)

    grain_cat = next(c for c in result.categories if c.category.lower() == "grains")
    rice = next(i for i in grain_cat.items if i.name.lower() == "rice")
    assert rice.quantity == 300.0  # 100g * (6/2)


@pytest.mark.asyncio
async def test_skips_optional_ingredients(
    db_session: AsyncSession, test_user: User
):
    """Ingredients flagged is_optional=True must NOT appear in the grocery list."""
    recipe = _make_recipe(
        name="With Optional",
        servings=4,
        ingredients=[
            {"name": "Tomato", "quantity": 2.0, "unit": "pc", "category": "vegetables"},
            {
                "name": "Cilantro",
                "quantity": 10.0,
                "unit": "g",
                "category": "vegetables",
                "is_optional": True,
            },
        ],
    )
    plan = _make_meal_plan(test_user.id)
    item = _make_meal_plan_item(plan.id, recipe.id, servings=4)

    db_session.add_all([recipe, plan, item])
    await db_session.commit()

    result = await get_grocery_list_for_meal_plan(db_session, test_user, plan.id)

    all_names = [i.name.lower() for c in result.categories for i in c.items]
    assert "tomato" in all_names
    assert "cilantro" not in all_names


@pytest.mark.asyncio
async def test_response_shape_and_counts(
    db_session: AsyncSession, test_user: User
):
    """The response should carry the meal plan id as both id and meal_plan_id,
    contain categories, and report total_items matching the flat count."""
    recipe = _make_recipe(
        ingredients=[
            {"name": "Salt", "quantity": 5.0, "unit": "g", "category": "spices"},
            {"name": "Oil", "quantity": 10.0, "unit": "ml", "category": "oils"},
        ],
    )
    plan = _make_meal_plan(test_user.id)
    item = _make_meal_plan_item(plan.id, recipe.id, servings=4)

    db_session.add_all([recipe, plan, item])
    await db_session.commit()

    result = await get_grocery_list_for_meal_plan(db_session, test_user, plan.id)

    assert result.id == str(plan.id)
    assert result.meal_plan_id == str(plan.id)
    assert result.total_items == sum(len(c.items) for c in result.categories)
    assert result.checked_items == 0


# ==================== get_grocery_list_whatsapp ====================


@pytest.mark.asyncio
async def test_whatsapp_format_includes_name_and_total(
    db_session: AsyncSession, test_user: User
):
    """WhatsApp output must be a single formatted string with header and footer."""
    recipe = _make_recipe(
        ingredients=[
            {"name": "Salt", "quantity": 5.0, "unit": "g", "category": "spices"},
        ],
    )
    plan = _make_meal_plan(test_user.id)
    item = _make_meal_plan_item(plan.id, recipe.id, servings=4)

    db_session.add_all([recipe, plan, item])
    await db_session.commit()

    result = await get_grocery_list_whatsapp(db_session, test_user, plan.id)

    assert "Grocery List" in result.formatted_text
    assert "Salt" in result.formatted_text
    assert "Total items:" in result.formatted_text
    assert result.item_count == 1
