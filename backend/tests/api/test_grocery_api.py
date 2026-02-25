"""
Tests for Grocery API endpoints.

Covers:
- GET /api/v1/grocery — get aggregated grocery list
- GET /api/v1/grocery/whatsapp — WhatsApp-formatted grocery list
"""

import pytest
from datetime import date, timedelta
from uuid import uuid4

import pytest_asyncio
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.recipe import Recipe, RecipeIngredient
from app.models.user import User
from tests.api.conftest import make_api_client
from tests.factories import make_user


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def grocery_user(db_session: AsyncSession) -> User:
    """Create a user for grocery tests."""
    user = make_user()
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def grocery_client(db_session: AsyncSession, grocery_user: User):
    """Authenticated client for grocery user."""
    async with make_api_client(db_session, grocery_user) as c:
        yield c


@pytest_asyncio.fixture
async def meal_plan_with_recipes(
    db_session: AsyncSession, grocery_user: User
) -> MealPlan:
    """Create a meal plan with recipes that have overlapping ingredients."""
    today = date.today()
    week_start = today - timedelta(days=today.weekday())
    week_end = week_start + timedelta(days=6)

    # Create Recipe 1: Dal Tadka
    recipe1_id = str(uuid4())
    recipe1 = Recipe(
        id=recipe1_id,
        name="Dal Tadka",
        cuisine_type="north",
        meal_types=["lunch", "dinner"],
        dietary_tags=["vegetarian"],
        prep_time_minutes=10,
        cook_time_minutes=30,
        total_time_minutes=40,
        servings=4,
        is_active=True,
    )
    db_session.add(recipe1)
    await db_session.flush()

    # Recipe 1 ingredients
    for name, qty, unit, cat in [
        ("Toor Dal", 200.0, "grams", "pulses"),
        ("Onion", 2.0, "pieces", "vegetables"),
        ("Cumin Seeds", 1.0, "tsp", "spices"),
        ("Ghee", 2.0, "tbsp", "dairy"),
    ]:
        db_session.add(
            RecipeIngredient(
                id=str(uuid4()),
                recipe_id=recipe1_id,
                name=name,
                quantity=qty,
                unit=unit,
                category=cat,
                is_optional=False,
                order=0,
            )
        )

    # Create Recipe 2: Jeera Rice (shares Cumin Seeds with Dal)
    recipe2_id = str(uuid4())
    recipe2 = Recipe(
        id=recipe2_id,
        name="Jeera Rice",
        cuisine_type="north",
        meal_types=["lunch", "dinner"],
        dietary_tags=["vegetarian"],
        prep_time_minutes=5,
        cook_time_minutes=20,
        total_time_minutes=25,
        servings=4,
        is_active=True,
    )
    db_session.add(recipe2)
    await db_session.flush()

    # Recipe 2 ingredients (Cumin Seeds overlaps)
    for name, qty, unit, cat in [
        ("Basmati Rice", 300.0, "grams", "grains"),
        ("Cumin Seeds", 2.0, "tsp", "spices"),
        ("Ghee", 1.0, "tbsp", "dairy"),
    ]:
        db_session.add(
            RecipeIngredient(
                id=str(uuid4()),
                recipe_id=recipe2_id,
                name=name,
                quantity=qty,
                unit=unit,
                category=cat,
                is_optional=False,
                order=0,
            )
        )

    await db_session.flush()

    # Create meal plan
    plan_id = str(uuid4())
    plan = MealPlan(
        id=plan_id,
        user_id=grocery_user.id,
        week_start_date=week_start,
        week_end_date=week_end,
        is_active=True,
    )
    db_session.add(plan)
    await db_session.flush()

    # Add meal plan items referencing the recipes
    item1 = MealPlanItem(
        id=str(uuid4()),
        meal_plan_id=plan_id,
        recipe_id=recipe1_id,
        date=week_start,
        meal_type="lunch",
        servings=4,
        recipe_name="Dal Tadka",
    )
    item2 = MealPlanItem(
        id=str(uuid4()),
        meal_plan_id=plan_id,
        recipe_id=recipe2_id,
        date=week_start,
        meal_type="dinner",
        servings=4,
        recipe_name="Jeera Rice",
    )
    db_session.add(item1)
    db_session.add(item2)

    await db_session.commit()
    await db_session.refresh(plan)
    return plan


# ==================== GET / Tests ====================


@pytest.mark.asyncio
async def test_grocery_list_with_meal_plan_id(
    grocery_client: AsyncClient, meal_plan_with_recipes: MealPlan
):
    """GET / with mealPlanId returns grouped ingredients for specific plan."""
    plan_id = meal_plan_with_recipes.id
    response = await grocery_client.get(f"/api/v1/grocery?mealPlanId={plan_id}")

    assert response.status_code == 200
    data = response.json()
    assert data["meal_plan_id"] == str(plan_id)
    assert data["total_items"] > 0
    assert len(data["categories"]) > 0


@pytest.mark.asyncio
async def test_grocery_list_aggregation(
    grocery_client: AsyncClient, meal_plan_with_recipes: MealPlan
):
    """GET / aggregates same ingredient across recipes."""
    plan_id = meal_plan_with_recipes.id
    response = await grocery_client.get(f"/api/v1/grocery?mealPlanId={plan_id}")

    assert response.status_code == 200
    data = response.json()

    # Find Cumin Seeds - should be aggregated (1 tsp + 2 tsp = 3 tsp)
    all_items = []
    for cat in data["categories"]:
        all_items.extend(cat["items"])

    cumin_items = [i for i in all_items if "cumin" in i["name"].lower()]
    assert len(cumin_items) == 1  # Aggregated into one
    assert cumin_items[0]["quantity"] == 3.0  # 1 + 2

    # Ghee should also aggregate (2 tbsp + 1 tbsp = 3 tbsp)
    ghee_items = [i for i in all_items if "ghee" in i["name"].lower()]
    assert len(ghee_items) == 1
    assert ghee_items[0]["quantity"] == 3.0


@pytest.mark.asyncio
async def test_grocery_list_category_grouping(
    grocery_client: AsyncClient, meal_plan_with_recipes: MealPlan
):
    """GET / groups items by category."""
    plan_id = meal_plan_with_recipes.id
    response = await grocery_client.get(f"/api/v1/grocery?mealPlanId={plan_id}")

    assert response.status_code == 200
    data = response.json()

    category_names = [c["category"].lower() for c in data["categories"]]
    # Should have categories from our test data
    assert any("pulse" in c for c in category_names)
    assert any("spice" in c for c in category_names)
    assert any("vegetable" in c for c in category_names)


@pytest.mark.asyncio
async def test_grocery_list_no_plan(grocery_client: AsyncClient):
    """GET / returns 404 when no meal plan exists for current week."""
    response = await grocery_client.get("/api/v1/grocery")
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_grocery_list_unauthorized(unauthenticated_client: AsyncClient):
    """GET / returns 401 without auth."""
    response = await unauthenticated_client.get("/api/v1/grocery")
    assert response.status_code == 401


# ==================== GET /whatsapp Tests ====================


@pytest.mark.asyncio
async def test_grocery_whatsapp_format(
    grocery_client: AsyncClient, meal_plan_with_recipes: MealPlan
):
    """GET /whatsapp returns emoji-formatted text."""
    plan_id = meal_plan_with_recipes.id
    response = await grocery_client.get(
        f"/api/v1/grocery/whatsapp?mealPlanId={plan_id}"
    )

    assert response.status_code == 200
    # Response is a plain string (not JSON dict)
    text = response.text.strip('"')  # Remove JSON string quotes
    assert "Grocery List" in text
    assert "Total items" in text


@pytest.mark.asyncio
async def test_grocery_whatsapp_structure(
    grocery_client: AsyncClient, meal_plan_with_recipes: MealPlan
):
    """GET /whatsapp has header, categories, and RasoiAI branding."""
    plan_id = meal_plan_with_recipes.id
    response = await grocery_client.get(
        f"/api/v1/grocery/whatsapp?mealPlanId={plan_id}"
    )

    assert response.status_code == 200
    text = response.text
    assert "RasoiAI" in text


@pytest.mark.asyncio
async def test_grocery_whatsapp_no_plan(grocery_client: AsyncClient):
    """GET /whatsapp returns 404 when plan not found."""
    fake_id = str(uuid4())
    response = await grocery_client.get(
        f"/api/v1/grocery/whatsapp?mealPlanId={fake_id}"
    )
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_grocery_whatsapp_unauthorized(unauthenticated_client: AsyncClient):
    """GET /whatsapp returns 401 without auth."""
    response = await unauthenticated_client.get(
        f"/api/v1/grocery/whatsapp?mealPlanId={uuid4()}"
    )
    assert response.status_code == 401
