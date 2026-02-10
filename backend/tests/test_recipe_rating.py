"""Tests for recipe rating endpoint.

Requirement: #21 - Recipe rating submission to backend
"""

import uuid

import pytest
import pytest_asyncio
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.recipe import Recipe, RecipeRating


@pytest_asyncio.fixture
async def sample_recipe(db_session: AsyncSession) -> Recipe:
    """Create a sample recipe for rating tests."""
    recipe = Recipe(
        id=str(uuid.uuid4()),
        name="Test Dal Tadka",
        description="A simple dal recipe for testing",
        cuisine_type="north",
        meal_types=["lunch", "dinner"],
        dietary_tags=["vegetarian"],
        prep_time_minutes=10,
        cook_time_minutes=25,
        total_time_minutes=35,
        servings=4,
        difficulty_level="easy",
        is_active=True,
    )
    db_session.add(recipe)
    await db_session.commit()
    await db_session.refresh(recipe)
    return recipe


@pytest.mark.asyncio
async def test_rate_recipe_success(client, sample_recipe):
    """Test successfully rating a recipe."""
    response = await client.post(
        f"/api/v1/recipes/{sample_recipe.id}/rate",
        json={"rating": 4.5},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["recipe_id"] == sample_recipe.id
    assert data["rating"] == 4.5
    assert data["feedback"] is None
    assert "id" in data
    assert "created_at" in data
    assert "updated_at" in data


@pytest.mark.asyncio
async def test_rate_recipe_with_feedback(client, sample_recipe):
    """Test rating a recipe with feedback text."""
    response = await client.post(
        f"/api/v1/recipes/{sample_recipe.id}/rate",
        json={"rating": 5.0, "feedback": "Absolutely delicious!"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["rating"] == 5.0
    assert data["feedback"] == "Absolutely delicious!"


@pytest.mark.asyncio
async def test_rate_recipe_update_existing(client, sample_recipe):
    """Test updating an existing rating (upsert behavior)."""
    # First rating
    response1 = await client.post(
        f"/api/v1/recipes/{sample_recipe.id}/rate",
        json={"rating": 3.0, "feedback": "It was okay"},
    )
    assert response1.status_code == 200
    data1 = response1.json()
    rating_id = data1["id"]

    # Update rating
    response2 = await client.post(
        f"/api/v1/recipes/{sample_recipe.id}/rate",
        json={"rating": 4.0, "feedback": "Actually, it was pretty good!"},
    )
    assert response2.status_code == 200
    data2 = response2.json()

    # Should be same rating record (updated, not duplicated)
    assert data2["id"] == rating_id
    assert data2["rating"] == 4.0
    assert data2["feedback"] == "Actually, it was pretty good!"


@pytest.mark.asyncio
async def test_rate_recipe_minimum_rating(client, sample_recipe):
    """Test rating with minimum value (1.0)."""
    response = await client.post(
        f"/api/v1/recipes/{sample_recipe.id}/rate",
        json={"rating": 1.0},
    )
    assert response.status_code == 200
    assert response.json()["rating"] == 1.0


@pytest.mark.asyncio
async def test_rate_recipe_maximum_rating(client, sample_recipe):
    """Test rating with maximum value (5.0)."""
    response = await client.post(
        f"/api/v1/recipes/{sample_recipe.id}/rate",
        json={"rating": 5.0},
    )
    assert response.status_code == 200
    assert response.json()["rating"] == 5.0


@pytest.mark.asyncio
async def test_rate_recipe_invalid_rating_too_low(client, sample_recipe):
    """Test that rating below 1.0 is rejected."""
    response = await client.post(
        f"/api/v1/recipes/{sample_recipe.id}/rate",
        json={"rating": 0.5},
    )
    assert response.status_code == 422  # Validation error


@pytest.mark.asyncio
async def test_rate_recipe_invalid_rating_too_high(client, sample_recipe):
    """Test that rating above 5.0 is rejected."""
    response = await client.post(
        f"/api/v1/recipes/{sample_recipe.id}/rate",
        json={"rating": 5.5},
    )
    assert response.status_code == 422  # Validation error


@pytest.mark.asyncio
async def test_rate_recipe_missing_rating(client, sample_recipe):
    """Test that missing rating field is rejected."""
    response = await client.post(
        f"/api/v1/recipes/{sample_recipe.id}/rate",
        json={"feedback": "No rating provided"},
    )
    assert response.status_code == 422  # Validation error


@pytest.mark.asyncio
async def test_rate_recipe_not_found(client):
    """Test rating a non-existent recipe returns 404."""
    fake_id = str(uuid.uuid4())
    response = await client.post(
        f"/api/v1/recipes/{fake_id}/rate",
        json={"rating": 4.0},
    )
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_rate_recipe_invalid_id(client):
    """Test rating with invalid recipe ID returns 404."""
    response = await client.post(
        "/api/v1/recipes/not-a-uuid/rate",
        json={"rating": 4.0},
    )
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_rate_recipe_unauthenticated(unauthenticated_client, sample_recipe):
    """Test that rating without authentication returns 401."""
    response = await unauthenticated_client.post(
        f"/api/v1/recipes/{sample_recipe.id}/rate",
        json={"rating": 4.0},
    )
    assert response.status_code == 401
