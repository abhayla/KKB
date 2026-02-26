"""
Tests for Recipe API endpoints.

Covers:
- GET /api/v1/recipes/ai-catalog/search — AI recipe catalog search
- GET /api/v1/recipes/{recipe_id} — get recipe by ID
- GET /api/v1/recipes/{recipe_id}/scale — scale recipe servings
"""

import json
import pytest
from uuid import uuid4

import pytest_asyncio
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.ai_recipe_catalog import AiRecipeCatalog
from app.models.recipe import (
    Recipe,
    RecipeIngredient,
    RecipeInstruction,
    RecipeNutrition,
)
from app.models.user import User

from tests.factories import make_user, make_preferences
from tests.api.conftest import make_api_client


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def recipe_user(db_session: AsyncSession) -> User:
    """Create a user with dietary preferences for recipe tests."""
    user = make_user()
    db_session.add(user)
    prefs = make_preferences(user.id, dietary_type="vegetarian", family_size=4)
    db_session.add(prefs)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def recipe_client(db_session: AsyncSession, recipe_user: User) -> AsyncClient:
    """Authenticated client for recipe user using shared make_api_client."""
    async with make_api_client(db_session, recipe_user) as c:
        yield c


@pytest_asyncio.fixture
async def sample_recipe(db_session: AsyncSession) -> Recipe:
    """Create a full recipe with ingredients, instructions, and nutrition."""
    recipe_id = str(uuid4())
    recipe = Recipe(
        id=recipe_id,
        name="Dal Tadka",
        description="Classic North Indian lentil dish",
        cuisine_type="north",
        meal_types=["lunch", "dinner"],
        dietary_tags=["vegetarian"],
        prep_time_minutes=10,
        cook_time_minutes=30,
        total_time_minutes=40,
        servings=4,
        difficulty_level="easy",
        is_active=True,
    )
    db_session.add(recipe)
    await db_session.flush()

    # Ingredients
    ingredients = [
        RecipeIngredient(
            id=str(uuid4()),
            recipe_id=recipe_id,
            name="Toor Dal",
            quantity=200.0,
            unit="grams",
            category="pulses",
            is_optional=False,
            order=0,
        ),
        RecipeIngredient(
            id=str(uuid4()),
            recipe_id=recipe_id,
            name="Onion",
            quantity=2.0,
            unit="pieces",
            category="vegetables",
            is_optional=False,
            order=1,
        ),
        RecipeIngredient(
            id=str(uuid4()),
            recipe_id=recipe_id,
            name="Cumin Seeds",
            quantity=1.0,
            unit="tsp",
            category="spices",
            is_optional=False,
            order=2,
        ),
    ]
    for ing in ingredients:
        db_session.add(ing)

    # Instructions
    instructions = [
        RecipeInstruction(
            id=str(uuid4()),
            recipe_id=recipe_id,
            step_number=1,
            instruction="Wash and soak the dal for 30 minutes",
            duration_minutes=30,
        ),
        RecipeInstruction(
            id=str(uuid4()),
            recipe_id=recipe_id,
            step_number=2,
            instruction="Pressure cook the dal until soft",
            duration_minutes=15,
            timer_required=True,
        ),
    ]
    for inst in instructions:
        db_session.add(inst)

    # Nutrition
    nutrition = RecipeNutrition(
        id=str(uuid4()),
        recipe_id=recipe_id,
        calories=250,
        protein_grams=12.0,
        carbohydrates_grams=35.0,
        fat_grams=8.0,
        fiber_grams=6.0,
    )
    db_session.add(nutrition)

    await db_session.commit()
    await db_session.refresh(recipe)
    return recipe


@pytest_asyncio.fixture
async def ai_catalog_entries(
    db_session: AsyncSession, recipe_user: User
) -> list[AiRecipeCatalog]:
    """Create AI recipe catalog entries for search tests."""
    entries = []
    catalog_data = [
        {
            "display_name": "Paneer Tikka",
            "normalized_name": "paneer tikka",
            "dietary_tags": json.dumps(["vegetarian"]),
            "cuisine_type": "north",
            "meal_types": json.dumps(["snacks", "dinner"]),
            "category": "snack",
            "usage_count": 15,
        },
        {
            "display_name": "Chicken Biryani",
            "normalized_name": "chicken biryani",
            "dietary_tags": json.dumps(["non_vegetarian"]),
            "cuisine_type": "south",
            "meal_types": json.dumps(["lunch", "dinner"]),
            "category": "rice",
            "usage_count": 20,
        },
        {
            "display_name": "Masala Dosa",
            "normalized_name": "masala dosa",
            "dietary_tags": json.dumps(["vegetarian", "vegan"]),
            "cuisine_type": "south",
            "meal_types": json.dumps(["breakfast"]),
            "category": "breakfast",
            "usage_count": 10,
        },
        {
            "display_name": "Chole Bhature",
            "normalized_name": "chole bhature",
            "dietary_tags": json.dumps(["vegetarian"]),
            "cuisine_type": "north",
            "meal_types": json.dumps(["lunch"]),
            "category": "main",
            "usage_count": 8,
        },
        {
            "display_name": "Fish Curry",
            "normalized_name": "fish curry",
            "dietary_tags": json.dumps(["non_vegetarian"]),
            "cuisine_type": "east",
            "meal_types": json.dumps(["lunch", "dinner"]),
            "category": "main",
            "usage_count": 5,
        },
    ]

    for item in catalog_data:
        entry = AiRecipeCatalog(
            id=str(uuid4()),
            first_generated_by=recipe_user.id,
            **item,
        )
        db_session.add(entry)
        entries.append(entry)

    await db_session.commit()
    return entries


# ==================== AI Catalog Search Tests ====================


@pytest.mark.asyncio
async def test_ai_catalog_search_basic(
    recipe_client: AsyncClient, ai_catalog_entries: list
):
    """GET /ai-catalog/search returns matching catalog entries."""
    response = await recipe_client.get("/api/v1/recipes/ai-catalog/search?q=paneer")

    assert response.status_code == 200
    data = response.json()
    assert len(data) >= 1
    names = [r["display_name"] for r in data]
    assert "Paneer Tikka" in names


@pytest.mark.asyncio
async def test_ai_catalog_search_dietary_filter(
    recipe_client: AsyncClient, ai_catalog_entries: list
):
    """GET /ai-catalog/search filters by user's dietary_type (vegetarian)."""
    response = await recipe_client.get("/api/v1/recipes/ai-catalog/search")

    assert response.status_code == 200
    data = response.json()
    # User is vegetarian; non-veg items should be filtered out
    names = [r["display_name"] for r in data]
    assert "Chicken Biryani" not in names
    assert "Fish Curry" not in names
    # Vegetarian items should be present
    assert "Paneer Tikka" in names
    assert "Masala Dosa" in names


@pytest.mark.asyncio
async def test_ai_catalog_search_favorites_first(
    recipe_client: AsyncClient, ai_catalog_entries: list
):
    """GET /ai-catalog/search sorts favorites to top."""
    response = await recipe_client.get(
        "/api/v1/recipes/ai-catalog/search?favorites=Masala Dosa,Chole Bhature"
    )

    assert response.status_code == 200
    data = response.json()
    assert len(data) >= 2
    # Favorites should appear before non-favorites
    fav_names = {"Masala Dosa", "Chole Bhature"}
    first_two = {data[0]["display_name"], data[1]["display_name"]}
    assert fav_names.issubset(first_two)


@pytest.mark.asyncio
async def test_ai_catalog_search_limit(
    recipe_client: AsyncClient, ai_catalog_entries: list
):
    """GET /ai-catalog/search respects limit param."""
    response = await recipe_client.get("/api/v1/recipes/ai-catalog/search?limit=2")

    assert response.status_code == 200
    data = response.json()
    assert len(data) <= 2


@pytest.mark.asyncio
async def test_ai_catalog_search_empty_query(
    recipe_client: AsyncClient, ai_catalog_entries: list
):
    """GET /ai-catalog/search with no query returns all compatible."""
    response = await recipe_client.get("/api/v1/recipes/ai-catalog/search")

    assert response.status_code == 200
    data = response.json()
    # Should return all vegetarian-compatible entries
    assert len(data) >= 1


@pytest.mark.asyncio
async def test_ai_catalog_unauthorized(unauthenticated_client: AsyncClient):
    """GET /ai-catalog/search returns 401 without auth."""
    response = await unauthenticated_client.get("/api/v1/recipes/ai-catalog/search")
    assert response.status_code == 401


# ==================== GET /{recipe_id} Tests ====================


@pytest.mark.asyncio
async def test_get_recipe_by_id(recipe_client: AsyncClient, sample_recipe: Recipe):
    """GET /{recipe_id} returns full recipe with ingredients."""
    response = await recipe_client.get(f"/api/v1/recipes/{sample_recipe.id}")

    assert response.status_code == 200
    data = response.json()
    assert data["id"] == str(sample_recipe.id)
    assert data["name"] == "Dal Tadka"
    assert data["cuisine_type"] == "north"
    assert data["servings"] == 4
    assert data["prep_time_minutes"] == 10
    assert data["cook_time_minutes"] == 30
    assert data["difficulty"] == "easy"
    assert data["meal_types"] == ["lunch", "dinner"]
    assert data["dietary_tags"] == ["vegetarian"]

    # Ingredients
    assert len(data["ingredients"]) == 3
    ing_names = [i["name"] for i in data["ingredients"]]
    assert "Toor Dal" in ing_names
    assert "Onion" in ing_names

    # Instructions
    assert len(data["instructions"]) == 2
    assert data["instructions"][0]["step_number"] == 1

    # Nutrition
    assert data["nutrition"] is not None
    assert data["nutrition"]["calories"] == 250
    assert data["nutrition"]["protein"] == 12


@pytest.mark.asyncio
async def test_get_recipe_not_found(recipe_client: AsyncClient):
    """GET /{recipe_id} returns 404 for invalid ID."""
    fake_id = str(uuid4())
    response = await recipe_client.get(f"/api/v1/recipes/{fake_id}")
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_get_recipe_unauthorized(
    unauthenticated_client: AsyncClient, sample_recipe: Recipe
):
    """GET /{recipe_id} returns 401 without auth."""
    response = await unauthenticated_client.get(f"/api/v1/recipes/{sample_recipe.id}")
    assert response.status_code == 401


# ==================== GET /{recipe_id}/scale Tests ====================


@pytest.mark.asyncio
async def test_scale_recipe_up(recipe_client: AsyncClient, sample_recipe: Recipe):
    """GET /{id}/scale?servings=8 doubles ingredients (4->8)."""
    response = await recipe_client.get(
        f"/api/v1/recipes/{sample_recipe.id}/scale?servings=8"
    )

    assert response.status_code == 200
    data = response.json()
    assert data["servings"] == 8

    # Toor Dal: 200g * 2 = 400g
    toor = next(i for i in data["ingredients"] if i["name"] == "Toor Dal")
    assert float(toor["quantity"]) == 400.0

    # Onion: 2 * 2 = 4
    onion = next(i for i in data["ingredients"] if i["name"] == "Onion")
    assert float(onion["quantity"]) == 4.0

    # Nutrition should also scale
    assert data["nutrition"]["calories"] == 500  # 250 * 2


@pytest.mark.asyncio
async def test_scale_recipe_down(recipe_client: AsyncClient, sample_recipe: Recipe):
    """GET /{id}/scale?servings=2 halves ingredients (4->2)."""
    response = await recipe_client.get(
        f"/api/v1/recipes/{sample_recipe.id}/scale?servings=2"
    )

    assert response.status_code == 200
    data = response.json()
    assert data["servings"] == 2

    # Toor Dal: 200g / 2 = 100g
    toor = next(i for i in data["ingredients"] if i["name"] == "Toor Dal")
    assert float(toor["quantity"]) == 100.0

    # Nutrition should also scale
    assert data["nutrition"]["calories"] == 125  # 250 / 2


@pytest.mark.asyncio
async def test_scale_recipe_not_found(recipe_client: AsyncClient):
    """GET /{id}/scale returns 404 for invalid recipe."""
    fake_id = str(uuid4())
    response = await recipe_client.get(f"/api/v1/recipes/{fake_id}/scale?servings=4")
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_scale_recipe_unauthorized(
    unauthenticated_client: AsyncClient, sample_recipe: Recipe
):
    """GET /{id}/scale returns 401 without auth."""
    response = await unauthenticated_client.get(
        f"/api/v1/recipes/{sample_recipe.id}/scale?servings=4"
    )
    assert response.status_code == 401
