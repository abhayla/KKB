"""
Requirement: #49 - FR-012: Recipe Rules Duplicate Prevention & Case Normalization

Tests for duplicate detection and case normalization in recipe rules API.
"""

import pytest
from uuid import uuid4

import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.database import get_db
from app.main import app
from app.models.user import User, UserPreferences


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def test_user(db_session: AsyncSession) -> User:
    """Create a test user for dedup tests."""
    user_id = str(uuid4())
    user = User(
        id=user_id,
        firebase_uid=f"firebase-dedup-test-{user_id}",
        email=f"dedup-test-{user_id}@example.com",
        name="Dedup Test User",
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


# ==================== Duplicate Detection Tests ====================


@pytest.mark.asyncio
async def test_create_duplicate_rule_returns_409(authenticated_client: AsyncClient):
    """Test that creating an exact duplicate rule returns 409 CONFLICT."""
    rule_data = {
        "target_type": "INGREDIENT",
        "action": "INCLUDE",
        "target_name": "Chai",
        "frequency_type": "DAILY",
        "enforcement": "REQUIRED",
        "meal_slot": "BREAKFAST",
        "is_active": True,
    }

    # First creation should succeed
    response1 = await authenticated_client.post("/api/v1/recipe-rules", json=rule_data)
    assert response1.status_code == 201

    # Second creation with same target should return 409
    response2 = await authenticated_client.post("/api/v1/recipe-rules", json=rule_data)
    assert response2.status_code == 409
    assert "already exists" in response2.json()["detail"].lower()


@pytest.mark.asyncio
async def test_create_duplicate_case_insensitive(authenticated_client: AsyncClient):
    """Test that duplicate check is case-insensitive ('chai' then 'CHAI' -> 409)."""
    rule1 = {
        "target_type": "INGREDIENT",
        "action": "INCLUDE",
        "target_name": "chai",
        "frequency_type": "DAILY",
        "enforcement": "REQUIRED",
        "meal_slot": "BREAKFAST",
        "is_active": True,
    }

    response1 = await authenticated_client.post("/api/v1/recipe-rules", json=rule1)
    assert response1.status_code == 201

    # Same target but different case
    rule2 = {
        "target_type": "INGREDIENT",
        "action": "INCLUDE",
        "target_name": "CHAI",
        "frequency_type": "DAILY",
        "enforcement": "REQUIRED",
        "meal_slot": "BREAKFAST",
        "is_active": True,
    }

    response2 = await authenticated_client.post("/api/v1/recipe-rules", json=rule2)
    assert response2.status_code == 409


@pytest.mark.asyncio
async def test_case_normalization_stores_uppercase(authenticated_client: AsyncClient):
    """Test that enum fields are normalized to UPPERCASE on create."""
    rule_data = {
        "target_type": "ingredient",
        "action": "include",
        "target_name": "Masala Dosa",
        "frequency_type": "daily",
        "enforcement": "required",
        "meal_slot": "breakfast",
        "is_active": True,
    }

    response = await authenticated_client.post("/api/v1/recipe-rules", json=rule_data)
    assert response.status_code == 201

    data = response.json()
    assert data["target_type"] == "INGREDIENT"
    assert data["action"] == "INCLUDE"
    assert data["frequency_type"] == "DAILY"
    assert data["enforcement"] == "REQUIRED"
    assert data["meal_slot"] == "BREAKFAST"
    # target_name preserves original case
    assert data["target_name"] == "Masala Dosa"


@pytest.mark.asyncio
async def test_different_meal_slot_not_duplicate(authenticated_client: AsyncClient):
    """Test that same target with different meal_slot is NOT a duplicate."""
    base_rule = {
        "target_type": "INGREDIENT",
        "action": "INCLUDE",
        "target_name": "Chai",
        "frequency_type": "DAILY",
        "enforcement": "REQUIRED",
        "is_active": True,
    }

    # Create for BREAKFAST
    rule1 = {**base_rule, "meal_slot": "BREAKFAST"}
    response1 = await authenticated_client.post("/api/v1/recipe-rules", json=rule1)
    assert response1.status_code == 201

    # Create for DINNER - should succeed (different meal_slot)
    rule2 = {**base_rule, "meal_slot": "DINNER"}
    response2 = await authenticated_client.post("/api/v1/recipe-rules", json=rule2)
    assert response2.status_code == 201

    # Verify both exist
    response = await authenticated_client.get("/api/v1/recipe-rules")
    assert response.json()["total_count"] == 2


@pytest.mark.asyncio
async def test_different_action_not_duplicate(authenticated_client: AsyncClient):
    """Test that same target with different action is NOT a duplicate."""
    # INCLUDE Paneer
    rule1 = {
        "target_type": "INGREDIENT",
        "action": "INCLUDE",
        "target_name": "Paneer",
        "frequency_type": "TIMES_PER_WEEK",
        "frequency_count": 3,
        "enforcement": "PREFERRED",
        "meal_slot": "LUNCH",
        "is_active": True,
    }
    response1 = await authenticated_client.post("/api/v1/recipe-rules", json=rule1)
    assert response1.status_code == 201

    # EXCLUDE Paneer - should succeed (different action)
    rule2 = {
        "target_type": "INGREDIENT",
        "action": "EXCLUDE",
        "target_name": "Paneer",
        "frequency_type": "NEVER",
        "enforcement": "REQUIRED",
        "meal_slot": "LUNCH",
        "is_active": True,
    }
    response2 = await authenticated_client.post("/api/v1/recipe-rules", json=rule2)
    assert response2.status_code == 201
