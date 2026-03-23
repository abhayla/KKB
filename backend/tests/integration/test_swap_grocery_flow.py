"""Integration tests: swapping a meal reflects in the grocery list.

The grocery list is computed dynamically from MealPlan -> MealPlanItem -> Recipe
-> RecipeIngredient. There is no stored grocery list — it is regenerated on each
GET /api/v1/grocery request. These tests verify that after a swap, the grocery
endpoint returns ingredients matching the new recipe, not the old one.

The swap endpoint uses MealPlanRepository and RecipeRepository which call
async_session_maker directly, so both must be patched to use the test session.
"""

import pytest
from datetime import date, timedelta
from unittest.mock import patch
from uuid import uuid4

import pytest_asyncio
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.recipe import Recipe, RecipeIngredient
from app.models.user import User
from tests.api.conftest import make_api_client
from tests.factories import make_user


# ==================== Helpers ====================


def _make_recipe(
    name: str,
    ingredients: list[tuple[str, float, str, str]],
    meal_types: list[str] | None = None,
) -> tuple[Recipe, list[RecipeIngredient]]:
    """Build a Recipe + ingredients (not yet added to session).

    Args:
        name: Recipe name.
        ingredients: List of (name, quantity, unit, category) tuples.
        meal_types: Meal types this recipe is suitable for.

    Returns:
        (Recipe, list[RecipeIngredient]) — caller must add to session.
    """
    recipe_id = str(uuid4())
    recipe = Recipe(
        id=recipe_id,
        name=name,
        cuisine_type="north",
        meal_types=meal_types or ["lunch", "dinner"],
        dietary_tags=["vegetarian"],
        prep_time_minutes=15,
        cook_time_minutes=30,
        total_time_minutes=45,
        servings=4,
        is_active=True,
    )
    ing_objects = []
    for idx, (ing_name, qty, unit, cat) in enumerate(ingredients):
        ing_objects.append(
            RecipeIngredient(
                id=str(uuid4()),
                recipe_id=recipe_id,
                name=ing_name,
                quantity=qty,
                unit=unit,
                category=cat,
                is_optional=False,
                order=idx,
            )
        )
    return recipe, ing_objects


def _extract_all_grocery_names(grocery_response_json: dict) -> set[str]:
    """Extract all ingredient names (lowercased) from a grocery API response."""
    names = set()
    for cat in grocery_response_json.get("categories", []):
        for item in cat["items"]:
            names.add(item["name"].lower())
    return names


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def swap_user(db_session: AsyncSession) -> User:
    """Create a user for swap-grocery integration tests."""
    user = make_user(name="Swap Grocery Test User")
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def swap_client(db_session: AsyncSession, swap_user: User):
    """Authenticated client with session_maker patches for repositories."""
    from tests.conftest import _test_session_maker

    def mock_session_maker():
        return _test_session_maker()

    async with make_api_client(db_session, swap_user) as c:
        with (
            patch(
                "app.repositories.meal_plan_repository.async_session_maker",
                mock_session_maker,
            ),
            patch(
                "app.repositories.recipe_repository.async_session_maker",
                mock_session_maker,
            ),
        ):
            yield c


@pytest_asyncio.fixture
async def plan_with_two_recipes(
    db_session: AsyncSession, swap_user: User
) -> dict:
    """Create a meal plan with two recipes that have distinct ingredients.

    Recipe A (Dal Tadka): Toor Dal, Onion, Cumin Seeds, Ghee
    Recipe B (Aloo Gobi): Potato, Cauliflower, Turmeric, Oil
    Recipe C (Paneer Butter Masala): Paneer, Butter, Tomato, Cream
        — used as the swap target

    Returns dict with plan, recipes, items, and IDs for test assertions.
    """
    today = date.today()
    week_start = today - timedelta(days=today.weekday())
    week_end = week_start + timedelta(days=6)

    # Recipe A: Dal Tadka
    recipe_a, ings_a = _make_recipe(
        "Dal Tadka",
        [
            ("Toor Dal", 200.0, "grams", "pulses"),
            ("Onion", 2.0, "pieces", "vegetables"),
            ("Cumin Seeds", 1.0, "tsp", "spices"),
            ("Ghee", 2.0, "tbsp", "dairy"),
        ],
    )
    db_session.add(recipe_a)
    for ing in ings_a:
        db_session.add(ing)

    # Recipe B: Aloo Gobi
    recipe_b, ings_b = _make_recipe(
        "Aloo Gobi",
        [
            ("Potato", 3.0, "pieces", "vegetables"),
            ("Cauliflower", 1.0, "pieces", "vegetables"),
            ("Turmeric", 0.5, "tsp", "spices"),
            ("Oil", 3.0, "tbsp", "oils"),
        ],
    )
    db_session.add(recipe_b)
    for ing in ings_b:
        db_session.add(ing)

    # Recipe C: Paneer Butter Masala (swap target)
    recipe_c, ings_c = _make_recipe(
        "Paneer Butter Masala",
        [
            ("Paneer", 250.0, "grams", "dairy"),
            ("Butter", 3.0, "tbsp", "dairy"),
            ("Tomato", 4.0, "pieces", "vegetables"),
            ("Cream", 100.0, "ml", "dairy"),
        ],
    )
    db_session.add(recipe_c)
    for ing in ings_c:
        db_session.add(ing)

    await db_session.flush()

    # Create meal plan
    plan_id = str(uuid4())
    plan = MealPlan(
        id=plan_id,
        user_id=swap_user.id,
        week_start_date=week_start,
        week_end_date=week_end,
        is_active=True,
    )
    db_session.add(plan)
    await db_session.flush()

    # Meal plan items: lunch = Dal Tadka, dinner = Aloo Gobi
    item_a_id = str(uuid4())
    item_a = MealPlanItem(
        id=item_a_id,
        meal_plan_id=plan_id,
        recipe_id=recipe_a.id,
        date=week_start,
        meal_type="lunch",
        servings=4,
        recipe_name="Dal Tadka",
    )
    item_b_id = str(uuid4())
    item_b = MealPlanItem(
        id=item_b_id,
        meal_plan_id=plan_id,
        recipe_id=recipe_b.id,
        date=week_start,
        meal_type="dinner",
        servings=4,
        recipe_name="Aloo Gobi",
    )
    db_session.add(item_a)
    db_session.add(item_b)

    await db_session.commit()

    return {
        "plan_id": plan_id,
        "item_a_id": item_a_id,
        "item_b_id": item_b_id,
        "recipe_a_id": recipe_a.id,
        "recipe_b_id": recipe_b.id,
        "recipe_c_id": recipe_c.id,
    }


# ==================== Tests ====================


@pytest.mark.asyncio
async def test_swap_meal_updates_grocery_list(
    swap_client: AsyncClient,
    plan_with_two_recipes: dict,
):
    """After swapping Dal Tadka -> Paneer Butter Masala, grocery reflects new ingredients.

    Before swap: Toor Dal, Onion, Cumin Seeds, Ghee, Potato, Cauliflower, Turmeric, Oil
    After swap: Paneer, Butter, Tomato, Cream, Potato, Cauliflower, Turmeric, Oil
    """
    ids = plan_with_two_recipes

    # 1. Get initial grocery list
    resp = await swap_client.get(
        f"/api/v1/grocery?mealPlanId={ids['plan_id']}"
    )
    assert resp.status_code == 200
    initial_names = _extract_all_grocery_names(resp.json())

    # Verify initial state: Dal Tadka ingredients present
    assert "toor dal" in initial_names
    assert "onion" in initial_names
    assert "cumin seeds" in initial_names
    # Aloo Gobi ingredients also present
    assert "potato" in initial_names
    assert "cauliflower" in initial_names

    # Paneer Butter Masala ingredients NOT present yet
    assert "paneer" not in initial_names
    assert "cream" not in initial_names

    # 2. Swap Dal Tadka (lunch item) -> Paneer Butter Masala
    swap_resp = await swap_client.post(
        f"/api/v1/meal-plans/{ids['plan_id']}/items/{ids['item_a_id']}/swap",
        json={"specific_recipe_id": ids["recipe_c_id"]},
    )
    assert swap_resp.status_code == 200

    # 3. Get grocery list again — should reflect the new recipe
    resp2 = await swap_client.get(
        f"/api/v1/grocery?mealPlanId={ids['plan_id']}"
    )
    assert resp2.status_code == 200
    updated_names = _extract_all_grocery_names(resp2.json())

    # Old Dal Tadka ingredients should be gone
    assert "toor dal" not in updated_names
    assert "onion" not in updated_names

    # New Paneer Butter Masala ingredients should appear
    assert "paneer" in updated_names
    assert "butter" in updated_names
    assert "tomato" in updated_names
    assert "cream" in updated_names

    # Aloo Gobi ingredients should still be present (dinner was not swapped)
    assert "potato" in updated_names
    assert "cauliflower" in updated_names


@pytest.mark.asyncio
async def test_grocery_list_reflects_current_plan(
    swap_client: AsyncClient,
    plan_with_two_recipes: dict,
):
    """Grocery list items match the recipes currently in the plan."""
    ids = plan_with_two_recipes

    resp = await swap_client.get(
        f"/api/v1/grocery?mealPlanId={ids['plan_id']}"
    )
    assert resp.status_code == 200
    data = resp.json()
    names = _extract_all_grocery_names(data)

    # All non-optional ingredients from both recipes should be present
    # Dal Tadka: Toor Dal, Onion, Cumin Seeds, Ghee
    assert "toor dal" in names
    assert "onion" in names
    assert "cumin seeds" in names
    assert "ghee" in names

    # Aloo Gobi: Potato, Cauliflower, Turmeric, Oil
    assert "potato" in names
    assert "cauliflower" in names
    assert "turmeric" in names
    assert "oil" in names

    # Total should be exactly 8 unique ingredients (no overlap between recipes)
    assert data["total_items"] == 8


@pytest.mark.asyncio
async def test_swap_does_not_affect_unswapped_items(
    swap_client: AsyncClient,
    plan_with_two_recipes: dict,
):
    """Swapping one meal preserves the other meal's grocery contributions."""
    ids = plan_with_two_recipes

    # Swap lunch (Dal Tadka) -> Paneer Butter Masala
    swap_resp = await swap_client.post(
        f"/api/v1/meal-plans/{ids['plan_id']}/items/{ids['item_a_id']}/swap",
        json={"specific_recipe_id": ids["recipe_c_id"]},
    )
    assert swap_resp.status_code == 200

    # Get updated grocery
    resp = await swap_client.get(
        f"/api/v1/grocery?mealPlanId={ids['plan_id']}"
    )
    assert resp.status_code == 200
    names = _extract_all_grocery_names(resp.json())

    # Aloo Gobi (dinner) ingredients must still be present
    assert "potato" in names
    assert "cauliflower" in names
    assert "turmeric" in names
    assert "oil" in names


@pytest.mark.asyncio
async def test_swap_back_to_original_restores_grocery(
    swap_client: AsyncClient,
    plan_with_two_recipes: dict,
):
    """Swap meal A -> C, then swap C -> A. Grocery should return to original state."""
    ids = plan_with_two_recipes

    # Capture original grocery
    resp_original = await swap_client.get(
        f"/api/v1/grocery?mealPlanId={ids['plan_id']}"
    )
    assert resp_original.status_code == 200
    original_names = _extract_all_grocery_names(resp_original.json())

    # Swap Dal Tadka -> Paneer Butter Masala
    swap1 = await swap_client.post(
        f"/api/v1/meal-plans/{ids['plan_id']}/items/{ids['item_a_id']}/swap",
        json={"specific_recipe_id": ids["recipe_c_id"]},
    )
    assert swap1.status_code == 200

    # The swap creates a new item ID for the replacement, so we need to find it
    swap1_data = swap1.json()
    # Find the new lunch item (it replaced the original item_a)
    new_lunch_item_id = None
    for day in swap1_data["days"]:
        for meal in day["meals"].get("lunch", []):
            if meal["recipe_name"] == "Paneer Butter Masala":
                new_lunch_item_id = meal["id"]
                break
        if new_lunch_item_id:
            break

    assert new_lunch_item_id is not None, "Swapped item not found in response"

    # Verify grocery changed
    resp_mid = await swap_client.get(
        f"/api/v1/grocery?mealPlanId={ids['plan_id']}"
    )
    mid_names = _extract_all_grocery_names(resp_mid.json())
    assert "paneer" in mid_names
    assert "toor dal" not in mid_names

    # Swap back: Paneer Butter Masala -> Dal Tadka
    swap2 = await swap_client.post(
        f"/api/v1/meal-plans/{ids['plan_id']}/items/{new_lunch_item_id}/swap",
        json={"specific_recipe_id": ids["recipe_a_id"]},
    )
    assert swap2.status_code == 200

    # Grocery should match the original
    resp_restored = await swap_client.get(
        f"/api/v1/grocery?mealPlanId={ids['plan_id']}"
    )
    assert resp_restored.status_code == 200
    restored_names = _extract_all_grocery_names(resp_restored.json())

    assert restored_names == original_names
