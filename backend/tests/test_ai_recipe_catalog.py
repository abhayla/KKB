"""
Requirement: #47 - FR-010: AI Recipe Catalog — shared recipe search for Recipe Rules

Tests for the AI recipe catalog service: cataloging recipes from meal plan generation,
deduplication with usage_count, dietary filtering, favorites-first sorting,
and ingredient/nutrition persistence.
"""

import json
import pytest
from datetime import datetime, timezone
from uuid import uuid4

import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.database import get_db
from app.main import app
from app.models.ai_recipe_catalog import AiRecipeCatalog
from app.models.user import User, UserPreferences
from app.services.ai_recipe_catalog_service import (
    catalog_recipes,
    normalize_recipe_name,
    search_catalog,
    _passes_dietary_filter,
)


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def test_user(db_session: AsyncSession) -> User:
    """Create a test user with vegetarian preferences."""
    user_id = str(uuid4())
    user = User(
        id=user_id,
        firebase_uid=f"firebase-catalog-{user_id}",
        email=f"catalog-{user_id}@example.com",
        name="Catalog Test User",
        is_onboarded=True,
        is_active=True,
    )
    db_session.add(user)

    prefs = UserPreferences(
        id=str(uuid4()),
        user_id=user_id,
        dietary_type="vegetarian",
        family_size=4,
        cuisine_preferences=json.dumps(["north"]),
    )
    db_session.add(prefs)

    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def authenticated_client(
    db_session: AsyncSession, test_user: User
) -> AsyncClient:
    """Create a test client with authentication overridden."""

    async def override_get_db():
        yield db_session

    async def override_get_current_user():
        return test_user

    app.dependency_overrides[get_db] = override_get_db
    app.dependency_overrides[get_current_user] = override_get_current_user

    async with AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://test",
    ) as ac:
        yield ac

    app.dependency_overrides.clear()


@pytest_asyncio.fixture
def sample_meal_plan() -> dict:
    """A minimal generated meal plan with known recipe names."""
    return {
        "days": [
            {
                "date": "2026-02-02",
                "day_name": "Monday",
                "breakfast": [
                    {
                        "recipe_name": "Dal Tadka",
                        "prep_time_minutes": 30,
                        "dietary_tags": ["vegetarian", "vegan"],
                        "category": "dal",
                        "calories": 250,
                        "ingredients": [
                            {"name": "Toor Dal", "quantity": 1, "unit": "cup", "category": "pulses"},
                            {"name": "Ghee", "quantity": 2, "unit": "tbsp", "category": "dairy"},
                        ],
                        "nutrition": {"protein_g": 12, "carbs_g": 35, "fat_g": 8, "fiber_g": 6},
                    },
                    {
                        "recipe_name": "Jeera Rice",
                        "prep_time_minutes": 20,
                        "dietary_tags": ["vegetarian", "vegan"],
                        "category": "rice",
                        "calories": 200,
                    },
                ],
                "lunch": [
                    {
                        "recipe_name": "Chicken Biryani",
                        "prep_time_minutes": 60,
                        "dietary_tags": ["non_vegetarian"],
                        "category": "biryani",
                        "calories": 500,
                    },
                    {
                        "recipe_name": "Raita",
                        "prep_time_minutes": 10,
                        "dietary_tags": ["vegetarian"],
                        "category": "side",
                        "calories": 50,
                    },
                ],
                "dinner": [],
                "snacks": [],
            }
        ]
    }


# ==================== Unit Tests (Service Layer) ====================


@pytest.mark.asyncio
async def test_catalog_recipe_on_meal_generation(
    db_session: AsyncSession, test_user: User, sample_meal_plan: dict
):
    """Test that recipes are cataloged after meal plan generation."""
    count = await catalog_recipes(
        db=db_session,
        user_id=test_user.id,
        generated_plan=sample_meal_plan,
        cuisine_type="north",
    )

    # Should have cataloged 4 items (Dal Tadka, Jeera Rice, Chicken Biryani, Raita)
    assert count == 4

    # Verify entries exist
    from sqlalchemy import select
    result = await db_session.execute(select(AiRecipeCatalog))
    entries = result.scalars().all()
    assert len(entries) == 4

    # Verify specific entry
    dal_result = await db_session.execute(
        select(AiRecipeCatalog).where(AiRecipeCatalog.normalized_name == "dal tadka")
    )
    dal = dal_result.scalar_one_or_none()
    assert dal is not None
    assert dal.display_name == "Dal Tadka"
    assert dal.cuisine_type == "north"
    assert dal.category == "dal"
    assert dal.usage_count == 1
    assert dal.first_generated_by == test_user.id


@pytest.mark.asyncio
async def test_catalog_dedup_increments_usage_count(
    db_session: AsyncSession, test_user: User, sample_meal_plan: dict
):
    """Test that cataloging the same recipe name increments usage_count."""
    # First catalog
    await catalog_recipes(
        db=db_session,
        user_id=test_user.id,
        generated_plan=sample_meal_plan,
        cuisine_type="north",
    )

    # Second catalog with overlapping recipes
    await catalog_recipes(
        db=db_session,
        user_id=test_user.id,
        generated_plan=sample_meal_plan,
        cuisine_type="north",
    )

    # Verify usage_count incremented
    from sqlalchemy import select
    result = await db_session.execute(
        select(AiRecipeCatalog).where(AiRecipeCatalog.normalized_name == "dal tadka")
    )
    dal = result.scalar_one_or_none()
    assert dal is not None
    assert dal.usage_count == 2  # Was 1, then incremented


@pytest.mark.asyncio
async def test_search_filters_by_dietary_tags(
    db_session: AsyncSession, test_user: User, sample_meal_plan: dict
):
    """Test that vegetarian user doesn't see non_vegetarian recipes."""
    # Catalog all recipes (including Chicken Biryani)
    await catalog_recipes(
        db=db_session,
        user_id=test_user.id,
        generated_plan=sample_meal_plan,
        cuisine_type="north",
    )

    # Search as vegetarian user
    results = await search_catalog(
        db=db_session,
        query="",
        user_dietary_tags=["vegetarian"],
        limit=10,
    )

    # Should NOT include Chicken Biryani
    names = [r["display_name"] for r in results]
    assert "Chicken Biryani" not in names
    # Should include vegetarian items
    assert "Dal Tadka" in names
    assert "Raita" in names


@pytest.mark.asyncio
async def test_search_sorts_favorites_first(
    db_session: AsyncSession, test_user: User, sample_meal_plan: dict
):
    """Test that favorite recipes are sorted to the top."""
    await catalog_recipes(
        db=db_session,
        user_id=test_user.id,
        generated_plan=sample_meal_plan,
        cuisine_type="north",
    )

    # Search with Raita as favorite (should come first even with lower usage)
    results = await search_catalog(
        db=db_session,
        query="",
        user_dietary_tags=["vegetarian"],
        favorite_names=["Raita"],
        limit=10,
    )

    assert len(results) > 0
    assert results[0]["display_name"] == "Raita"


@pytest.mark.asyncio
async def test_search_sorts_by_usage_count(
    db_session: AsyncSession, test_user: User
):
    """Test that popular recipes appear before rare ones."""
    # Insert recipes with different usage counts
    popular = AiRecipeCatalog(
        id=str(uuid4()),
        display_name="Paneer Butter Masala",
        normalized_name="paneer butter masala",
        dietary_tags=json.dumps(["vegetarian"]),
        cuisine_type="north",
        usage_count=50,
        created_at=datetime.now(timezone.utc),
        updated_at=datetime.now(timezone.utc),
    )
    rare = AiRecipeCatalog(
        id=str(uuid4()),
        display_name="Paneer Tikka",
        normalized_name="paneer tikka",
        dietary_tags=json.dumps(["vegetarian"]),
        cuisine_type="north",
        usage_count=2,
        created_at=datetime.now(timezone.utc),
        updated_at=datetime.now(timezone.utc),
    )
    db_session.add(popular)
    db_session.add(rare)
    await db_session.commit()

    results = await search_catalog(
        db=db_session,
        query="paneer",
        user_dietary_tags=[],
        limit=10,
    )

    assert len(results) == 2
    assert results[0]["display_name"] == "Paneer Butter Masala"
    assert results[1]["display_name"] == "Paneer Tikka"


@pytest.mark.asyncio
async def test_search_empty_query_returns_popular(
    db_session: AsyncSession, test_user: User, sample_meal_plan: dict
):
    """Test that empty query returns top recipes by usage_count."""
    await catalog_recipes(
        db=db_session,
        user_id=test_user.id,
        generated_plan=sample_meal_plan,
        cuisine_type="north",
    )

    results = await search_catalog(
        db=db_session,
        query="",
        user_dietary_tags=[],
        limit=10,
    )

    # Should return all cataloged items
    assert len(results) == 4


@pytest.mark.asyncio
async def test_catalog_stores_ingredients_and_nutrition(
    db_session: AsyncSession, test_user: User, sample_meal_plan: dict
):
    """Test that ingredients and nutrition data are persisted."""
    await catalog_recipes(
        db=db_session,
        user_id=test_user.id,
        generated_plan=sample_meal_plan,
        cuisine_type="north",
    )

    from sqlalchemy import select
    result = await db_session.execute(
        select(AiRecipeCatalog).where(AiRecipeCatalog.normalized_name == "dal tadka")
    )
    dal = result.scalar_one_or_none()
    assert dal is not None

    # Verify ingredients stored
    ingredients = json.loads(dal.ingredients)
    assert len(ingredients) == 2
    assert ingredients[0]["name"] == "Toor Dal"
    assert ingredients[0]["category"] == "pulses"

    # Verify nutrition stored
    nutrition = json.loads(dal.nutrition)
    assert nutrition["protein_g"] == 12
    assert nutrition["carbs_g"] == 35
    assert nutrition["fat_g"] == 8
    assert nutrition["fiber_g"] == 6


# ==================== Unit Tests (Dietary Filter Logic) ====================


def test_dietary_filter_vegetarian_excludes_nonveg():
    """Vegetarian user should not see non_vegetarian recipes."""
    assert _passes_dietary_filter(["non_vegetarian"], ["vegetarian"]) is False


def test_dietary_filter_vegetarian_allows_veg():
    """Vegetarian user should see vegetarian recipes."""
    assert _passes_dietary_filter(["vegetarian"], ["vegetarian"]) is True


def test_dietary_filter_nonveg_sees_everything():
    """Non-vegetarian user sees all recipes."""
    assert _passes_dietary_filter(["non_vegetarian"], ["non_vegetarian"]) is True
    assert _passes_dietary_filter(["vegetarian"], ["non_vegetarian"]) is True


def test_dietary_filter_vegan_requires_vegan_tag():
    """Vegan user requires the 'vegan' tag on recipes."""
    assert _passes_dietary_filter(["vegetarian"], ["vegan"]) is False
    assert _passes_dietary_filter(["vegetarian", "vegan"], ["vegan"]) is True


def test_dietary_filter_jain_requires_jain_tag():
    """Jain user requires the 'jain' tag on recipes."""
    assert _passes_dietary_filter(["vegetarian"], ["jain"]) is False
    assert _passes_dietary_filter(["vegetarian", "jain"], ["jain"]) is True


def test_normalize_recipe_name():
    """Test recipe name normalization."""
    assert normalize_recipe_name("Dal Tadka") == "dal tadka"
    assert normalize_recipe_name("  Masala Chai  ") == "masala chai"
    assert normalize_recipe_name("PANEER BUTTER MASALA") == "paneer butter masala"


# ==================== API Endpoint Tests ====================


@pytest.mark.asyncio
async def test_search_ai_catalog_endpoint_empty(authenticated_client: AsyncClient):
    """Test /ai-catalog/search returns empty when catalog is empty."""
    response = await authenticated_client.get("/api/v1/recipes/ai-catalog/search")
    assert response.status_code == 200
    assert response.json() == []


@pytest.mark.asyncio
async def test_search_ai_catalog_endpoint_with_results(
    db_session: AsyncSession,
    test_user: User,
    authenticated_client: AsyncClient,
    sample_meal_plan: dict,
):
    """Test /ai-catalog/search returns results after cataloging."""
    # Catalog recipes
    await catalog_recipes(
        db=db_session,
        user_id=test_user.id,
        generated_plan=sample_meal_plan,
        cuisine_type="north",
    )

    response = await authenticated_client.get(
        "/api/v1/recipes/ai-catalog/search", params={"q": "dal"}
    )
    assert response.status_code == 200
    data = response.json()
    assert len(data) >= 1
    assert any(r["display_name"] == "Dal Tadka" for r in data)


@pytest.mark.asyncio
async def test_search_ai_catalog_endpoint_with_favorites(
    db_session: AsyncSession,
    test_user: User,
    authenticated_client: AsyncClient,
    sample_meal_plan: dict,
):
    """Test /ai-catalog/search sorts favorites first."""
    await catalog_recipes(
        db=db_session,
        user_id=test_user.id,
        generated_plan=sample_meal_plan,
        cuisine_type="north",
    )

    response = await authenticated_client.get(
        "/api/v1/recipes/ai-catalog/search",
        params={"favorites": "Raita"},
    )
    assert response.status_code == 200
    data = response.json()
    # First result should be Raita (vegetarian user, Chicken Biryani filtered out)
    # Among vegetarian results, Raita should be first due to favorites param
    if data:
        assert data[0]["display_name"] == "Raita"
