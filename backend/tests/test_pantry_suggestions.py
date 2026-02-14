"""Tests for pantry recipe suggestions.

Verifies that the suggest_from_pantry function correctly matches
pantry ingredients against recipe ingredient lists and returns
ranked suggestions.
"""

import uuid
import pytest

from app.models.recipe import Recipe, RecipeIngredient
from app.services.recipe_service import suggest_from_pantry


@pytest.fixture
async def recipes_with_ingredients(db_session):
    """Create test recipes with known ingredients."""
    recipes = []

    # Recipe 1: Dal Tadka (rice, dal, ghee, turmeric, cumin)
    r1_id = str(uuid.uuid4())
    r1 = Recipe(
        id=r1_id,
        name="Dal Tadka",
        cuisine_type="north",
        dietary_tags=["vegetarian"],
        meal_types=["lunch", "dinner"],
        prep_time_minutes=15,
        cook_time_minutes=25,
        servings=4,
        is_active=True,
    )
    db_session.add(r1)
    for name, qty, unit, cat in [
        ("Toor Dal", 1, "cup", "pulses"),
        ("Ghee", 2, "tbsp", "dairy"),
        ("Turmeric", 0.5, "tsp", "spices"),
        ("Cumin Seeds", 1, "tsp", "spices"),
        ("Onion", 1, "medium", "vegetables"),
    ]:
        db_session.add(RecipeIngredient(
            id=str(uuid.uuid4()),
            recipe_id=r1_id,
            name=name,
            quantity=qty,
            unit=unit,
            category=cat,
        ))

    # Recipe 2: Aloo Paratha (wheat flour, potato, ghee, salt)
    r2_id = str(uuid.uuid4())
    r2 = Recipe(
        id=r2_id,
        name="Aloo Paratha",
        cuisine_type="north",
        dietary_tags=["vegetarian"],
        meal_types=["breakfast"],
        prep_time_minutes=20,
        cook_time_minutes=15,
        servings=4,
        is_active=True,
    )
    db_session.add(r2)
    for name, qty, unit, cat in [
        ("Wheat Flour", 2, "cup", "grains"),
        ("Potato", 3, "medium", "vegetables"),
        ("Ghee", 2, "tbsp", "dairy"),
        ("Salt", 1, "tsp", "other"),
    ]:
        db_session.add(RecipeIngredient(
            id=str(uuid.uuid4()),
            recipe_id=r2_id,
            name=name,
            quantity=qty,
            unit=unit,
            category=cat,
        ))

    # Recipe 3: Masala Chai (tea leaves, milk, sugar, ginger)
    r3_id = str(uuid.uuid4())
    r3 = Recipe(
        id=r3_id,
        name="Masala Chai",
        cuisine_type="north",
        dietary_tags=["vegetarian"],
        meal_types=["breakfast", "snacks"],
        prep_time_minutes=10,
        cook_time_minutes=5,
        servings=2,
        is_active=True,
    )
    db_session.add(r3)
    for name, qty, unit, cat in [
        ("Tea Leaves", 2, "tsp", "other"),
        ("Milk", 1, "cup", "dairy"),
        ("Sugar", 2, "tsp", "other"),
        ("Ginger", 1, "inch", "spices"),
    ]:
        db_session.add(RecipeIngredient(
            id=str(uuid.uuid4()),
            recipe_id=r3_id,
            name=name,
            quantity=qty,
            unit=unit,
            category=cat,
        ))

    await db_session.commit()
    return [r1, r2, r3]


class TestPantrySuggestions:
    """Test pantry-based recipe suggestions."""

    @pytest.mark.asyncio
    async def test_matching_ingredients_returns_recipes(self, db_session, recipes_with_ingredients):
        """Recipes with matching ingredients are returned."""
        results = await suggest_from_pantry(
            db_session, ["ghee", "toor dal", "turmeric"]
        )
        assert len(results) > 0
        # Dal Tadka should be first (most matches)
        assert results[0]["recipe"].name == "Dal Tadka"

    @pytest.mark.asyncio
    async def test_match_percentage_calculated(self, db_session, recipes_with_ingredients):
        """Match percentage is correctly calculated."""
        results = await suggest_from_pantry(
            db_session, ["ghee", "toor dal", "turmeric", "cumin seeds", "onion"]
        )
        # Dal Tadka has 5 ingredients, all 5 matched = 100%
        dal_result = next(r for r in results if r["recipe"].name == "Dal Tadka")
        assert dal_result["match_percentage"] == 100.0

    @pytest.mark.asyncio
    async def test_missing_ingredients_listed(self, db_session, recipes_with_ingredients):
        """Missing ingredients are included in results."""
        results = await suggest_from_pantry(
            db_session, ["ghee", "potato"]
        )
        paratha_result = next(r for r in results if r["recipe"].name == "Aloo Paratha")
        assert "wheat flour" in paratha_result["missing_ingredients"]

    @pytest.mark.asyncio
    async def test_empty_pantry_returns_empty(self, db_session, recipes_with_ingredients):
        """Empty ingredient list returns no suggestions."""
        results = await suggest_from_pantry(db_session, [])
        assert len(results) == 0

    @pytest.mark.asyncio
    async def test_no_matching_recipes(self, db_session, recipes_with_ingredients):
        """Ingredients that don't match any recipe return empty."""
        results = await suggest_from_pantry(
            db_session, ["dragon fruit", "quinoa", "avocado"]
        )
        assert len(results) == 0

    @pytest.mark.asyncio
    async def test_results_sorted_by_match_percentage(self, db_session, recipes_with_ingredients):
        """Results are sorted by match percentage descending."""
        results = await suggest_from_pantry(
            db_session, ["ghee", "potato", "wheat flour", "salt"]
        )
        if len(results) > 1:
            for i in range(len(results) - 1):
                assert results[i]["match_percentage"] >= results[i + 1]["match_percentage"]
