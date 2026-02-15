"""
Comprehensive tests for the Nutrition Goals API endpoints.

Covers: auth, CRUD for all 8 food categories, updates (target, enforcement,
active, progress), deletes, edge cases (duplicate category, boundary values).
"""

import pytest
from datetime import datetime, timezone
from uuid import uuid4

import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.database import get_db
from app.main import app
from app.models.recipe_rule import NutritionGoal
from app.models.user import User, UserPreferences


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def test_user(db_session: AsyncSession) -> User:
    """Create a test user in the test database."""
    user_id = str(uuid4())
    user = User(
        id=user_id,
        firebase_uid=f"firebase-nutrition-test-{user_id}",
        email=f"nutrition-test-{user_id}@example.com",
        name="Nutrition Test User",
        is_onboarded=True,
        is_active=True,
    )
    db_session.add(user)

    prefs = UserPreferences(
        id=str(uuid4()),
        user_id=user_id,
        dietary_type="vegetarian",
        family_size=4,
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


# ==================== Auth Tests ====================


@pytest.mark.asyncio
async def test_get_nutrition_goals_unauthorized(unauthenticated_client: AsyncClient):
    """Test getting nutrition goals without auth returns 401."""
    response = await unauthenticated_client.get("/api/v1/nutrition-goals")
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_create_nutrition_goal_unauthorized(unauthenticated_client: AsyncClient):
    """Test creating a nutrition goal without auth returns 401."""
    response = await unauthenticated_client.post(
        "/api/v1/nutrition-goals",
        json={"food_category": "LEAFY_GREENS", "weekly_target": 5},
    )
    assert response.status_code == 401


# ==================== CRUD Tests ====================


@pytest.mark.asyncio
async def test_get_nutrition_goals_empty(authenticated_client: AsyncClient):
    """Test getting nutrition goals when none exist."""
    response = await authenticated_client.get("/api/v1/nutrition-goals")

    assert response.status_code == 200
    data = response.json()
    assert data["goals"] == []
    assert data["total_count"] == 0


@pytest.mark.asyncio
async def test_create_nutrition_goal_green_leafy(authenticated_client: AsyncClient):
    """Test creating a GREEN_LEAFY nutrition goal."""
    goal_data = {
        "food_category": "LEAFY_GREENS",
        "weekly_target": 5,
        "enforcement": "PREFERRED",
        "is_active": True,
    }

    response = await authenticated_client.post(
        "/api/v1/nutrition-goals", json=goal_data
    )

    assert response.status_code == 201
    data = response.json()
    assert data["food_category"] == "LEAFY_GREENS"
    assert data["weekly_target"] == 5
    assert data["current_progress"] == 0
    assert data["enforcement"] == "PREFERRED"
    assert data["is_active"] is True
    assert "id" in data
    assert "created_at" in data


@pytest.mark.asyncio
async def test_create_nutrition_goal_high_protein(authenticated_client: AsyncClient):
    """Test creating a HIGH_PROTEIN nutrition goal with REQUIRED enforcement."""
    goal_data = {
        "food_category": "PROTEIN",
        "weekly_target": 7,
        "enforcement": "REQUIRED",
        "is_active": True,
    }

    response = await authenticated_client.post(
        "/api/v1/nutrition-goals", json=goal_data
    )

    assert response.status_code == 201
    data = response.json()
    assert data["food_category"] == "PROTEIN"
    assert data["weekly_target"] == 7
    assert data["enforcement"] == "REQUIRED"


@pytest.mark.asyncio
async def test_create_nutrition_goal_all_eight_categories(
    authenticated_client: AsyncClient,
):
    """Test creating goals for all 8 food categories."""
    categories = [
        "LEAFY_GREENS",
        "PROTEIN",
        "FERMENTED",
        "WHOLE_GRAINS",
        "CITRUS",
        "IRON_RICH",
        "CALCIUM_RICH",
        "OMEGA_3",
    ]

    for category in categories:
        response = await authenticated_client.post(
            "/api/v1/nutrition-goals",
            json={
                "food_category": category,
                "weekly_target": 3,
                "enforcement": "PREFERRED",
                "is_active": True,
            },
        )
        assert response.status_code == 201, f"Failed to create {category}: {response.text}"

    # Verify count
    list_response = await authenticated_client.get("/api/v1/nutrition-goals")
    assert list_response.status_code == 200
    assert list_response.json()["total_count"] == 8


@pytest.mark.asyncio
async def test_get_single_nutrition_goal(authenticated_client: AsyncClient):
    """Test getting a single nutrition goal by ID."""
    create_response = await authenticated_client.post(
        "/api/v1/nutrition-goals",
        json={
            "food_category": "PROTEIN",
            "weekly_target": 7,
            "enforcement": "REQUIRED",
            "is_active": True,
        },
    )
    goal_id = create_response.json()["id"]

    response = await authenticated_client.get(f"/api/v1/nutrition-goals/{goal_id}")

    assert response.status_code == 200
    data = response.json()
    assert data["id"] == goal_id
    assert data["food_category"] == "PROTEIN"
    assert data["weekly_target"] == 7


@pytest.mark.asyncio
async def test_get_nonexistent_nutrition_goal_404(authenticated_client: AsyncClient):
    """Test getting a nonexistent nutrition goal returns 404."""
    fake_id = str(uuid4())
    response = await authenticated_client.get(f"/api/v1/nutrition-goals/{fake_id}")
    assert response.status_code == 404


# ==================== Update Tests ====================


@pytest.mark.asyncio
async def test_update_nutrition_goal_weekly_target(authenticated_client: AsyncClient):
    """Test updating weekly target from 5 to 8."""
    create_response = await authenticated_client.post(
        "/api/v1/nutrition-goals",
        json={
            "food_category": "LEAFY_GREENS",
            "weekly_target": 5,
            "enforcement": "PREFERRED",
            "is_active": True,
        },
    )
    goal_id = create_response.json()["id"]

    response = await authenticated_client.put(
        f"/api/v1/nutrition-goals/{goal_id}",
        json={"weekly_target": 8},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["weekly_target"] == 8
    assert data["food_category"] == "LEAFY_GREENS"  # unchanged


@pytest.mark.asyncio
async def test_update_nutrition_goal_toggle_enforcement(
    authenticated_client: AsyncClient,
):
    """Test toggling enforcement from PREFERRED to REQUIRED."""
    create_response = await authenticated_client.post(
        "/api/v1/nutrition-goals",
        json={
            "food_category": "PROTEIN",
            "weekly_target": 5,
            "enforcement": "PREFERRED",
            "is_active": True,
        },
    )
    goal_id = create_response.json()["id"]

    response = await authenticated_client.put(
        f"/api/v1/nutrition-goals/{goal_id}",
        json={"enforcement": "REQUIRED"},
    )

    assert response.status_code == 200
    assert response.json()["enforcement"] == "REQUIRED"


@pytest.mark.asyncio
async def test_update_nutrition_goal_toggle_active(authenticated_client: AsyncClient):
    """Test toggling is_active from True to False."""
    create_response = await authenticated_client.post(
        "/api/v1/nutrition-goals",
        json={
            "food_category": "FERMENTED",
            "weekly_target": 3,
            "enforcement": "PREFERRED",
            "is_active": True,
        },
    )
    goal_id = create_response.json()["id"]

    response = await authenticated_client.put(
        f"/api/v1/nutrition-goals/{goal_id}",
        json={"is_active": False},
    )

    assert response.status_code == 200
    assert response.json()["is_active"] is False


@pytest.mark.asyncio
async def test_update_nutrition_goal_progress(authenticated_client: AsyncClient):
    """Test updating current_progress from 0 to 3."""
    create_response = await authenticated_client.post(
        "/api/v1/nutrition-goals",
        json={
            "food_category": "WHOLE_GRAINS",
            "weekly_target": 5,
            "enforcement": "PREFERRED",
            "is_active": True,
        },
    )
    goal_id = create_response.json()["id"]

    response = await authenticated_client.put(
        f"/api/v1/nutrition-goals/{goal_id}",
        json={"current_progress": 3},
    )

    assert response.status_code == 200
    assert response.json()["current_progress"] == 3


@pytest.mark.asyncio
async def test_update_nutrition_goal_progress_reset(authenticated_client: AsyncClient):
    """Test resetting current_progress to 0."""
    create_response = await authenticated_client.post(
        "/api/v1/nutrition-goals",
        json={
            "food_category": "IRON_RICH",
            "weekly_target": 4,
            "enforcement": "PREFERRED",
            "is_active": True,
        },
    )
    goal_id = create_response.json()["id"]

    # Set progress to 3
    await authenticated_client.put(
        f"/api/v1/nutrition-goals/{goal_id}",
        json={"current_progress": 3},
    )

    # Reset to 0
    response = await authenticated_client.put(
        f"/api/v1/nutrition-goals/{goal_id}",
        json={"current_progress": 0},
    )

    assert response.status_code == 200
    assert response.json()["current_progress"] == 0


# ==================== Delete Tests ====================


@pytest.mark.asyncio
async def test_delete_nutrition_goal(authenticated_client: AsyncClient):
    """Test deleting a nutrition goal."""
    create_response = await authenticated_client.post(
        "/api/v1/nutrition-goals",
        json={
            "food_category": "CALCIUM_RICH",
            "weekly_target": 4,
            "enforcement": "PREFERRED",
            "is_active": True,
        },
    )
    goal_id = create_response.json()["id"]

    response = await authenticated_client.delete(
        f"/api/v1/nutrition-goals/{goal_id}"
    )
    assert response.status_code == 204

    # Verify it's gone
    get_response = await authenticated_client.get(
        f"/api/v1/nutrition-goals/{goal_id}"
    )
    assert get_response.status_code == 404


@pytest.mark.asyncio
async def test_delete_nonexistent_nutrition_goal_404(
    authenticated_client: AsyncClient,
):
    """Test deleting a nonexistent nutrition goal returns 404."""
    fake_id = str(uuid4())
    response = await authenticated_client.delete(
        f"/api/v1/nutrition-goals/{fake_id}"
    )
    assert response.status_code == 404


# ==================== Edge Cases ====================


@pytest.mark.asyncio
async def test_create_duplicate_category_returns_409(
    authenticated_client: AsyncClient,
):
    """Test that creating a duplicate category goal returns 409."""
    goal_data = {
        "food_category": "PROTEIN",
        "weekly_target": 7,
        "enforcement": "REQUIRED",
        "is_active": True,
    }

    response1 = await authenticated_client.post(
        "/api/v1/nutrition-goals", json=goal_data
    )
    assert response1.status_code == 201

    response2 = await authenticated_client.post(
        "/api/v1/nutrition-goals", json=goal_data
    )
    assert response2.status_code == 409


@pytest.mark.asyncio
async def test_weekly_target_boundary_min_1(authenticated_client: AsyncClient):
    """Test creating a goal with minimum weekly_target of 1."""
    response = await authenticated_client.post(
        "/api/v1/nutrition-goals",
        json={
            "food_category": "OMEGA_3",
            "weekly_target": 1,
            "enforcement": "PREFERRED",
            "is_active": True,
        },
    )

    assert response.status_code == 201
    assert response.json()["weekly_target"] == 1


@pytest.mark.asyncio
async def test_weekly_target_boundary_max_14(authenticated_client: AsyncClient):
    """Test creating a goal with maximum weekly_target of 14."""
    response = await authenticated_client.post(
        "/api/v1/nutrition-goals",
        json={
            "food_category": "LEAFY_GREENS",
            "weekly_target": 14,
            "enforcement": "PREFERRED",
            "is_active": True,
        },
    )

    assert response.status_code == 201
    assert response.json()["weekly_target"] == 14


@pytest.mark.asyncio
async def test_weekly_target_exceeds_max_rejected(authenticated_client: AsyncClient):
    """Test that weekly_target > 14 is rejected by Pydantic validation."""
    response = await authenticated_client.post(
        "/api/v1/nutrition-goals",
        json={
            "food_category": "PROTEIN",
            "weekly_target": 21,
            "enforcement": "PREFERRED",
            "is_active": True,
        },
    )

    assert response.status_code == 422  # Validation error
