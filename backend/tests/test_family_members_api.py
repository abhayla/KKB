"""
Requirement: #50 - FR-013: Sync Missing Preferences + Family Members CRUD Endpoint

Tests for missing preference field sync and family members CRUD API.
"""

import pytest
from uuid import uuid4

import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.database import get_db
from app.main import app
from app.models.user import FamilyMember, User, UserPreferences


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def test_user(db_session: AsyncSession) -> User:
    """Create a test user for family members tests."""
    user_id = str(uuid4())
    user = User(
        id=user_id,
        firebase_uid=f"firebase-family-test-{user_id}",
        email=f"family-test-{user_id}@example.com",
        name="Family Test User",
        is_onboarded=True,
        is_active=True,
    )
    db_session.add(user)

    prefs = UserPreferences(
        id=str(uuid4()),
        user_id=user_id,
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


# ==================== Missing Preference Fields Tests ====================


@pytest.mark.asyncio
async def test_update_preferences_with_primary_diet(
    authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User
):
    """Test that primary_diet maps to dietary_type column."""
    response = await authenticated_client.put(
        "/api/v1/users/preferences",
        json={"primary_diet": "vegetarian"},
    )

    assert response.status_code == 200

    # Verify dietary_type was set in DB
    result = await db_session.execute(
        select(UserPreferences).where(UserPreferences.user_id == test_user.id)
    )
    prefs = result.scalar_one()
    assert prefs.dietary_type == "vegetarian"


@pytest.mark.asyncio
async def test_update_preferences_with_busy_days(
    authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User
):
    """Test that busy_days array is saved to database."""
    response = await authenticated_client.put(
        "/api/v1/users/preferences",
        json={"busy_days": ["MONDAY", "WEDNESDAY", "FRIDAY"]},
    )

    assert response.status_code == 200

    # Verify busy_days was set in DB
    result = await db_session.execute(
        select(UserPreferences).where(UserPreferences.user_id == test_user.id)
    )
    prefs = result.scalar_one()
    assert prefs.busy_days == ["MONDAY", "WEDNESDAY", "FRIDAY"]


# ==================== Family Members CRUD Tests ====================


@pytest.mark.asyncio
async def test_get_family_members_empty(authenticated_client: AsyncClient):
    """Test getting family members when none exist."""
    response = await authenticated_client.get("/api/v1/family-members")

    assert response.status_code == 200
    data = response.json()
    assert data["members"] == []
    assert data["total_count"] == 0


@pytest.mark.asyncio
async def test_create_family_member(authenticated_client: AsyncClient):
    """Test creating a family member."""
    member_data = {
        "name": "Priya Sharma",
        "age_group": "adult",
        "dietary_restrictions": ["vegetarian"],
        "health_conditions": [],
    }

    response = await authenticated_client.post(
        "/api/v1/family-members",
        json=member_data,
    )

    assert response.status_code == 201
    data = response.json()
    assert data["name"] == "Priya Sharma"
    assert data["age_group"] == "adult"
    assert data["dietary_restrictions"] == ["vegetarian"]
    assert "id" in data


@pytest.mark.asyncio
async def test_get_family_members_list(authenticated_client: AsyncClient):
    """Test getting family members after creating some."""
    # Create two members
    for name in ["Rahul Sharma", "Anita Sharma"]:
        await authenticated_client.post(
            "/api/v1/family-members",
            json={"name": name, "age_group": "adult"},
        )

    response = await authenticated_client.get("/api/v1/family-members")

    assert response.status_code == 200
    data = response.json()
    assert data["total_count"] == 2
    assert len(data["members"]) == 2


@pytest.mark.asyncio
async def test_update_family_member(authenticated_client: AsyncClient):
    """Test updating a family member."""
    # Create a member
    create_response = await authenticated_client.post(
        "/api/v1/family-members",
        json={"name": "Amit Sharma", "age_group": "teen"},
    )
    member_id = create_response.json()["id"]

    # Update the member
    response = await authenticated_client.put(
        f"/api/v1/family-members/{member_id}",
        json={"name": "Amit Sharma", "age_group": "adult"},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["age_group"] == "adult"
    assert data["name"] == "Amit Sharma"


@pytest.mark.asyncio
async def test_delete_family_member(authenticated_client: AsyncClient):
    """Test deleting a family member."""
    # Create a member
    create_response = await authenticated_client.post(
        "/api/v1/family-members",
        json={"name": "Temp Member", "age_group": "child"},
    )
    member_id = create_response.json()["id"]

    # Delete the member
    response = await authenticated_client.delete(
        f"/api/v1/family-members/{member_id}"
    )
    assert response.status_code == 204

    # Verify it's deleted
    get_response = await authenticated_client.get("/api/v1/family-members")
    assert get_response.json()["total_count"] == 0


@pytest.mark.asyncio
async def test_family_member_with_health_conditions(authenticated_client: AsyncClient):
    """Test creating a family member with health conditions."""
    member_data = {
        "name": "Dadi Sharma",
        "age_group": "senior",
        "dietary_restrictions": ["vegetarian", "low_salt"],
        "health_conditions": ["diabetes", "hypertension"],
    }

    response = await authenticated_client.post(
        "/api/v1/family-members",
        json=member_data,
    )

    assert response.status_code == 201
    data = response.json()
    assert data["health_conditions"] == ["diabetes", "hypertension"]
    assert data["dietary_restrictions"] == ["vegetarian", "low_salt"]
    assert data["age_group"] == "senior"
