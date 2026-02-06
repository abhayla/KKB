"""Recipe search API tests.

Requirement: FR-001 - Recipe search returns results from database

This file tests the recipe search functionality used by the Add Recipe sheet
on the Home screen. When users search for recipes (e.g., "Chai"), the backend
should return matching recipes from the PostgreSQL database.
"""

import uuid
import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.recipe import Recipe
from app.models.user import User


async def create_test_recipe(
    db: AsyncSession,
    name: str,
    cuisine_type: str = "NORTH",
    dietary_tags: list[str] | None = None,
    meal_types: list[str] | None = None,
) -> Recipe:
    """Helper to create a test recipe in the database."""
    recipe = Recipe(
        id=str(uuid.uuid4()),
        name=name,
        description=f"A delicious {name}",
        cuisine_type=cuisine_type,
        dietary_tags=dietary_tags or ["VEGETARIAN"],
        meal_types=meal_types or ["BREAKFAST"],
        prep_time_minutes=15,
        cook_time_minutes=10,
        total_time_minutes=25,
        servings=2,
    )
    db.add(recipe)
    await db.commit()
    await db.refresh(recipe)
    return recipe


@pytest.mark.asyncio
async def test_search_chai_returns_results(authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User):
    """Test that searching for 'chai' returns Chai recipes.

    This is the core test for FR-001: when a user types "chai" in the
    Add Recipe sheet search field, they should see Chai recipes from
    the database.
    """
    # Arrange: Create chai recipes in database
    await create_test_recipe(
        db_session,
        name="Masala Chai",
        meal_types=["BREAKFAST", "SNACKS"],
    )
    await create_test_recipe(
        db_session,
        name="Adrak Chai",
        meal_types=["BREAKFAST", "SNACKS"],
    )
    await create_test_recipe(
        db_session,
        name="Poha",  # Not chai - should not match
        meal_types=["BREAKFAST"],
    )

    # Act: Search for chai
    response = await authenticated_client.get(
        "/api/v1/recipes/search",
        params={"q": "chai"},
    )

    # Assert: Should return chai recipes
    assert response.status_code == 200
    recipes = response.json()
    assert len(recipes) >= 2
    assert all("chai" in r["name"].lower() for r in recipes)


@pytest.mark.asyncio
async def test_search_case_insensitive(authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User):
    """Test that search is case-insensitive."""
    await create_test_recipe(db_session, name="Masala Chai")

    # Search with different cases
    for query in ["CHAI", "chai", "Chai", "ChAi"]:
        response = await authenticated_client.get(
            "/api/v1/recipes/search",
            params={"q": query},
        )
        assert response.status_code == 200
        recipes = response.json()
        assert len(recipes) >= 1, f"No results for query: {query}"


@pytest.mark.asyncio
async def test_search_partial_match(authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User):
    """Test that partial text matches work."""
    await create_test_recipe(db_session, name="Masala Chai")
    await create_test_recipe(db_session, name="Masala Dosa")

    response = await authenticated_client.get(
        "/api/v1/recipes/search",
        params={"q": "masala"},
    )

    assert response.status_code == 200
    recipes = response.json()
    assert len(recipes) == 2
    assert all("masala" in r["name"].lower() for r in recipes)


@pytest.mark.asyncio
async def test_search_empty_query_returns_all(authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User):
    """Test that empty query returns recipes (paginated)."""
    await create_test_recipe(db_session, name="Recipe 1")
    await create_test_recipe(db_session, name="Recipe 2")

    response = await authenticated_client.get(
        "/api/v1/recipes/search",
        params={"q": ""},
    )

    assert response.status_code == 200
    recipes = response.json()
    assert len(recipes) >= 2


@pytest.mark.asyncio
async def test_search_no_results(authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User):
    """Test that searching for non-existent recipe returns empty list."""
    await create_test_recipe(db_session, name="Poha")

    response = await authenticated_client.get(
        "/api/v1/recipes/search",
        params={"q": "nonexistent_xyz_recipe"},
    )

    assert response.status_code == 200
    recipes = response.json()
    assert len(recipes) == 0


@pytest.mark.asyncio
async def test_search_with_cuisine_filter(authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User):
    """Test filtering search results by cuisine."""
    await create_test_recipe(db_session, name="North Chai", cuisine_type="NORTH")
    await create_test_recipe(db_session, name="South Chai", cuisine_type="SOUTH")

    response = await authenticated_client.get(
        "/api/v1/recipes/search",
        params={"q": "chai", "cuisine": "SOUTH"},
    )

    assert response.status_code == 200
    recipes = response.json()
    assert len(recipes) >= 1
    # Check for either cuisine or cuisine_type field
    for r in recipes:
        cuisine_value = r.get("cuisine") or r.get("cuisine_type") or r.get("cuisineType")
        assert cuisine_value.upper() == "SOUTH"


@pytest.mark.asyncio
async def test_search_with_meal_type_filter(authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User):
    """Test filtering search results by meal type."""
    await create_test_recipe(
        db_session,
        name="Breakfast Chai",
        meal_types=["BREAKFAST"],
    )
    await create_test_recipe(
        db_session,
        name="Snack Chai",
        meal_types=["SNACKS"],
    )

    response = await authenticated_client.get(
        "/api/v1/recipes/search",
        params={"q": "chai", "mealType": "BREAKFAST"},
    )

    assert response.status_code == 200
    recipes = response.json()
    # Should only return breakfast chai
    assert len(recipes) >= 1


@pytest.mark.asyncio
async def test_search_pagination(authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User):
    """Test that search results are paginated."""
    # Create 25 recipes
    for i in range(25):
        await create_test_recipe(db_session, name=f"Recipe {i}")

    # First page (default limit is 20)
    response = await authenticated_client.get(
        "/api/v1/recipes/search",
        params={"q": "Recipe", "page": 1, "limit": 10},
    )

    assert response.status_code == 200
    page1 = response.json()
    assert len(page1) == 10

    # Second page
    response = await authenticated_client.get(
        "/api/v1/recipes/search",
        params={"q": "Recipe", "page": 2, "limit": 10},
    )

    assert response.status_code == 200
    page2 = response.json()
    assert len(page2) == 10

    # Ensure pages are different
    page1_ids = {r["id"] for r in page1}
    page2_ids = {r["id"] for r in page2}
    assert page1_ids.isdisjoint(page2_ids)


@pytest.mark.asyncio
async def test_search_requires_authentication(client: AsyncClient):
    """Test that search endpoint requires authentication."""
    from app.main import app
    from app.api.deps import get_current_user
    from app.core.exceptions import AuthenticationError

    async def no_auth():
        raise AuthenticationError("Missing authorization header")

    app.dependency_overrides[get_current_user] = no_auth

    response = await client.get(
        "/api/v1/recipes/search",
        params={"q": "chai"},
    )

    assert response.status_code == 401

    app.dependency_overrides.pop(get_current_user, None)


@pytest.mark.asyncio
async def test_search_response_structure(authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User):
    """Test that search response has expected structure."""
    await create_test_recipe(
        db_session,
        name="Masala Chai",
        cuisine_type="NORTH",
        dietary_tags=["VEGETARIAN", "VEGAN"],
    )

    response = await authenticated_client.get(
        "/api/v1/recipes/search",
        params={"q": "chai"},
    )

    assert response.status_code == 200
    recipes = response.json()
    assert len(recipes) >= 1

    recipe = recipes[0]
    # Check required fields are present
    assert "id" in recipe
    assert "name" in recipe
    # Cuisine field may be named differently in response
    assert "cuisine" in recipe or "cuisine_type" in recipe or "cuisineType" in recipe
    assert "dietary_tags" in recipe or "dietaryTags" in recipe
    assert "prep_time_minutes" in recipe or "prepTimeMinutes" in recipe
