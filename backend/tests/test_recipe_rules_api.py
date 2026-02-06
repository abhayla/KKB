"""
Requirement: #45 - FR-006: Recipe Rules Backend Sync (Offline-First)

Tests for the recipe rules and nutrition goals API endpoints.
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
from app.models.recipe_rule import NutritionGoal, RecipeRule
from app.models.user import User, UserPreferences


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def test_user(db_session: AsyncSession) -> User:
    """Create a test user in the test database."""
    user_id = str(uuid4())
    user = User(
        id=user_id,
        firebase_uid=f"firebase-rules-test-{user_id}",
        email=f"rules-test-{user_id}@example.com",
        name="Rules Test User",
        is_onboarded=True,
        is_active=True,
    )
    db_session.add(user)

    # Add preferences
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


# ==================== Recipe Rules Tests ====================


@pytest.mark.asyncio
async def test_get_recipe_rules_unauthorized(unauthenticated_client: AsyncClient):
    """Test getting recipe rules without auth."""
    response = await unauthenticated_client.get("/api/v1/recipe-rules")
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_get_recipe_rules_empty(authenticated_client: AsyncClient):
    """Test getting recipe rules when none exist."""
    response = await authenticated_client.get("/api/v1/recipe-rules")

    assert response.status_code == 200
    data = response.json()
    assert data["rules"] == []
    assert data["total_count"] == 0


@pytest.mark.asyncio
async def test_create_recipe_rule(authenticated_client: AsyncClient):
    """Test creating a new recipe rule."""
    rule_data = {
        "target_type": "INGREDIENT",
        "action": "INCLUDE",
        "target_name": "Chai",
        "frequency_type": "DAILY",
        "enforcement": "REQUIRED",
        "meal_slot": "BREAKFAST",
        "is_active": True,
    }

    response = await authenticated_client.post(
        "/api/v1/recipe-rules",
        json=rule_data,
    )

    assert response.status_code == 201
    data = response.json()
    assert data["target_name"] == "Chai"
    assert data["action"] == "INCLUDE"
    assert data["frequency_type"] == "DAILY"
    assert data["meal_slot"] == "BREAKFAST"
    assert data["is_active"] is True
    assert "id" in data
    assert "created_at" in data


@pytest.mark.asyncio
async def test_create_recipe_rule_exclude(authenticated_client: AsyncClient):
    """Test creating an EXCLUDE recipe rule."""
    rule_data = {
        "target_type": "INGREDIENT",
        "action": "EXCLUDE",
        "target_name": "Bitter Gourd",
        "frequency_type": "NEVER",
        "enforcement": "REQUIRED",
        "is_active": True,
    }

    response = await authenticated_client.post(
        "/api/v1/recipe-rules",
        json=rule_data,
    )

    assert response.status_code == 201
    data = response.json()
    assert data["target_name"] == "Bitter Gourd"
    assert data["action"] == "EXCLUDE"
    assert data["frequency_type"] == "NEVER"


@pytest.mark.asyncio
async def test_create_rule_times_per_week(authenticated_client: AsyncClient):
    """Test creating a rule with TIMES_PER_WEEK frequency."""
    rule_data = {
        "target_type": "RECIPE",
        "action": "INCLUDE",
        "target_name": "Paneer Butter Masala",
        "frequency_type": "TIMES_PER_WEEK",
        "frequency_count": 2,
        "enforcement": "PREFERRED",
        "is_active": True,
    }

    response = await authenticated_client.post(
        "/api/v1/recipe-rules",
        json=rule_data,
    )

    assert response.status_code == 201
    data = response.json()
    assert data["frequency_type"] == "TIMES_PER_WEEK"
    assert data["frequency_count"] == 2


@pytest.mark.asyncio
async def test_create_rule_specific_days(authenticated_client: AsyncClient):
    """Test creating a rule with SPECIFIC_DAYS frequency."""
    rule_data = {
        "target_type": "INGREDIENT",
        "action": "EXCLUDE",
        "target_name": "Non-veg",
        "frequency_type": "SPECIFIC_DAYS",
        "frequency_days": "TUESDAY,THURSDAY",
        "enforcement": "REQUIRED",
        "is_active": True,
    }

    response = await authenticated_client.post(
        "/api/v1/recipe-rules",
        json=rule_data,
    )

    assert response.status_code == 201
    data = response.json()
    assert data["frequency_type"] == "SPECIFIC_DAYS"
    assert data["frequency_days"] == "TUESDAY,THURSDAY"


@pytest.mark.asyncio
async def test_get_recipe_rules_list(authenticated_client: AsyncClient):
    """Test getting list of recipe rules after creating some."""
    # Create two rules
    for target in ["Chai", "Dal"]:
        await authenticated_client.post(
            "/api/v1/recipe-rules",
            json={
                "target_type": "INGREDIENT",
                "action": "INCLUDE",
                "target_name": target,
                "frequency_type": "DAILY",
                "enforcement": "REQUIRED",
                "is_active": True,
            },
        )

    response = await authenticated_client.get("/api/v1/recipe-rules")

    assert response.status_code == 200
    data = response.json()
    assert data["total_count"] == 2
    assert len(data["rules"]) == 2


@pytest.mark.asyncio
async def test_get_single_rule(authenticated_client: AsyncClient):
    """Test getting a single rule by ID."""
    # Create a rule
    create_response = await authenticated_client.post(
        "/api/v1/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "Rice",
            "frequency_type": "DAILY",
            "enforcement": "REQUIRED",
            "is_active": True,
        },
    )
    rule_id = create_response.json()["id"]

    # Get the rule
    response = await authenticated_client.get(f"/api/v1/recipe-rules/{rule_id}")

    assert response.status_code == 200
    data = response.json()
    assert data["id"] == rule_id
    assert data["target_name"] == "Rice"


@pytest.mark.asyncio
async def test_get_nonexistent_rule(authenticated_client: AsyncClient):
    """Test getting a rule that doesn't exist."""
    fake_id = str(uuid4())

    response = await authenticated_client.get(f"/api/v1/recipe-rules/{fake_id}")

    assert response.status_code == 404


@pytest.mark.asyncio
async def test_update_recipe_rule(authenticated_client: AsyncClient):
    """Test updating a recipe rule."""
    # Create a rule
    create_response = await authenticated_client.post(
        "/api/v1/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "Dosa",
            "frequency_type": "DAILY",
            "enforcement": "REQUIRED",
            "is_active": True,
        },
    )
    rule_id = create_response.json()["id"]

    # Update the rule
    response = await authenticated_client.put(
        f"/api/v1/recipe-rules/{rule_id}",
        json={
            "frequency_type": "TIMES_PER_WEEK",
            "frequency_count": 3,
            "is_active": False,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert data["frequency_type"] == "TIMES_PER_WEEK"
    assert data["frequency_count"] == 3
    assert data["is_active"] is False


@pytest.mark.asyncio
async def test_delete_recipe_rule(authenticated_client: AsyncClient):
    """Test deleting a recipe rule."""
    # Create a rule
    create_response = await authenticated_client.post(
        "/api/v1/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "Idli",
            "frequency_type": "DAILY",
            "enforcement": "REQUIRED",
            "is_active": True,
        },
    )
    rule_id = create_response.json()["id"]

    # Delete the rule
    response = await authenticated_client.delete(f"/api/v1/recipe-rules/{rule_id}")

    assert response.status_code == 204

    # Verify it's deleted
    get_response = await authenticated_client.get(f"/api/v1/recipe-rules/{rule_id}")
    assert get_response.status_code == 404


# ==================== Nutrition Goals Tests ====================


@pytest.mark.asyncio
async def test_get_nutrition_goals_unauthorized(unauthenticated_client: AsyncClient):
    """Test getting nutrition goals without auth."""
    response = await unauthenticated_client.get("/api/v1/nutrition-goals")
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_get_nutrition_goals_empty(authenticated_client: AsyncClient):
    """Test getting nutrition goals when none exist."""
    response = await authenticated_client.get("/api/v1/nutrition-goals")

    assert response.status_code == 200
    data = response.json()
    assert data["goals"] == []
    assert data["total_count"] == 0


@pytest.mark.asyncio
async def test_create_nutrition_goal(authenticated_client: AsyncClient):
    """Test creating a nutrition goal."""
    goal_data = {
        "food_category": "LEAFY_GREENS",
        "weekly_target": 5,
        "enforcement": "PREFERRED",
        "is_active": True,
    }

    response = await authenticated_client.post(
        "/api/v1/nutrition-goals",
        json=goal_data,
    )

    assert response.status_code == 201
    data = response.json()
    assert data["food_category"] == "LEAFY_GREENS"
    assert data["weekly_target"] == 5
    assert data["current_progress"] == 0
    assert data["enforcement"] == "PREFERRED"


@pytest.mark.asyncio
async def test_create_duplicate_category_fails(authenticated_client: AsyncClient):
    """Test that creating duplicate category goals fails."""
    goal_data = {
        "food_category": "PROTEIN",
        "weekly_target": 7,
        "enforcement": "REQUIRED",
        "is_active": True,
    }

    # First creation should succeed
    response1 = await authenticated_client.post(
        "/api/v1/nutrition-goals",
        json=goal_data,
    )
    assert response1.status_code == 201

    # Second creation with same category should fail
    response2 = await authenticated_client.post(
        "/api/v1/nutrition-goals",
        json=goal_data,
    )
    assert response2.status_code == 409


@pytest.mark.asyncio
async def test_update_nutrition_goal(authenticated_client: AsyncClient):
    """Test updating a nutrition goal."""
    # Create a goal
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

    # Update the goal
    response = await authenticated_client.put(
        f"/api/v1/nutrition-goals/{goal_id}",
        json={
            "weekly_target": 5,
            "current_progress": 2,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert data["weekly_target"] == 5
    assert data["current_progress"] == 2


@pytest.mark.asyncio
async def test_delete_nutrition_goal(authenticated_client: AsyncClient):
    """Test deleting a nutrition goal."""
    # Create a goal
    create_response = await authenticated_client.post(
        "/api/v1/nutrition-goals",
        json={
            "food_category": "WHOLE_GRAINS",
            "weekly_target": 4,
            "enforcement": "PREFERRED",
            "is_active": True,
        },
    )
    goal_id = create_response.json()["id"]

    # Delete the goal
    response = await authenticated_client.delete(
        f"/api/v1/nutrition-goals/{goal_id}"
    )

    assert response.status_code == 204


# ==================== Sync Tests ====================


@pytest.mark.asyncio
async def test_sync_empty(authenticated_client: AsyncClient):
    """Test sync with no pending changes."""
    sync_request = {
        "recipe_rules": [],
        "nutrition_goals": [],
        "last_sync_time": None,
    }

    response = await authenticated_client.post(
        "/api/v1/recipe-rules/sync",
        json=sync_request,
    )

    assert response.status_code == 200
    data = response.json()
    assert data["synced_rule_ids"] == []
    assert data["synced_goal_ids"] == []
    assert "sync_time" in data


@pytest.mark.asyncio
async def test_sync_new_rule(authenticated_client: AsyncClient):
    """Test syncing a new rule from client."""
    rule_id = str(uuid4())
    sync_request = {
        "recipe_rules": [
            {
                "id": rule_id,
                "target_type": "INGREDIENT",
                "action": "INCLUDE",
                "target_name": "Paratha",
                "frequency_type": "DAILY",
                "enforcement": "REQUIRED",
                "is_active": True,
                "local_updated_at": datetime.now(timezone.utc).isoformat(),
            }
        ],
        "nutrition_goals": [],
        "last_sync_time": None,
    }

    response = await authenticated_client.post(
        "/api/v1/recipe-rules/sync",
        json=sync_request,
    )

    assert response.status_code == 200
    data = response.json()
    assert rule_id in data["synced_rule_ids"]


@pytest.mark.asyncio
async def test_sync_conflict_server_wins(authenticated_client: AsyncClient):
    """Test that server version wins in conflict (newer timestamp)."""
    # Create a rule on server
    create_response = await authenticated_client.post(
        "/api/v1/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "Samosa",
            "frequency_type": "TIMES_PER_WEEK",
            "frequency_count": 2,
            "enforcement": "PREFERRED",
            "is_active": True,
        },
    )
    rule_id = create_response.json()["id"]

    # Try to sync with an older timestamp (should conflict)
    old_time = "2020-01-01T00:00:00+00:00"
    sync_request = {
        "recipe_rules": [
            {
                "id": rule_id,
                "target_type": "INGREDIENT",
                "action": "INCLUDE",
                "target_name": "Samosa Updated",  # Different name
                "frequency_type": "DAILY",  # Different frequency
                "enforcement": "REQUIRED",
                "is_active": True,
                "local_updated_at": old_time,
            }
        ],
        "nutrition_goals": [],
        "last_sync_time": None,
    }

    response = await authenticated_client.post(
        "/api/v1/recipe-rules/sync",
        json=sync_request,
    )

    assert response.status_code == 200
    data = response.json()
    assert rule_id in data["conflict_rule_ids"]
    assert rule_id not in data["synced_rule_ids"]
