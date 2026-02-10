"""
Tests for User API endpoints.

Covers:
- GET /api/v1/users/me — get current user with preferences
- PUT /api/v1/users/preferences — update user preferences
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
from app.models.user import User, UserPreferences


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def user_no_prefs(db_session: AsyncSession) -> User:
    """Create a test user WITHOUT preferences."""
    user_id = str(uuid4())
    user = User(
        id=user_id,
        firebase_uid=f"firebase-user-noprefs-{user_id}",
        email=f"noprefs-{user_id}@example.com",
        name="User No Prefs",
        is_onboarded=False,
        is_active=True,
    )
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def user_with_prefs(db_session: AsyncSession) -> User:
    """Create a test user WITH existing preferences."""
    user_id = str(uuid4())
    user = User(
        id=user_id,
        firebase_uid=f"firebase-user-prefs-{user_id}",
        email=f"prefs-{user_id}@example.com",
        name="User With Prefs",
        is_onboarded=True,
        is_active=True,
    )
    db_session.add(user)
    await db_session.flush()

    prefs = UserPreferences(
        id=str(uuid4()),
        user_id=user_id,
        dietary_type="vegetarian",
        family_size=4,
        cuisine_preferences=["north", "south"],
        spice_level="medium",
        cooking_time_preference="moderate",
        disliked_ingredients=["bitter gourd"],
        busy_days=["MONDAY", "FRIDAY"],
        weekday_cooking_time_minutes=30,
        weekend_cooking_time_minutes=60,
    )
    db_session.add(prefs)
    await db_session.commit()
    await db_session.refresh(user)
    return user


def _make_client(db_session, user):
    """Helper to build an authenticated test client for a specific user."""

    async def override_get_db():
        yield db_session

    async def override_get_current_user():
        return user

    app.dependency_overrides[get_db] = override_get_db
    app.dependency_overrides[get_current_user] = override_get_current_user

    return AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://test",
    )


# ==================== GET /me Tests ====================


@pytest.mark.asyncio
async def test_get_current_user_with_preferences(
    db_session: AsyncSession, user_with_prefs: User
):
    """GET /me returns user with nested preferences when they exist."""
    async with _make_client(db_session, user_with_prefs) as ac:
        response = await ac.get("/api/v1/users/me")

    app.dependency_overrides.clear()

    assert response.status_code == 200
    data = response.json()
    assert data["id"] == user_with_prefs.id
    assert data["email"] == user_with_prefs.email
    assert data["name"] == "User With Prefs"
    assert data["is_onboarded"] is True

    prefs = data["preferences"]
    assert prefs is not None
    assert prefs["dietary_type"] == "vegetarian"
    assert prefs["household_size"] == 4
    assert prefs["cuisine_preferences"] == ["north", "south"]
    assert prefs["spice_level"] == "medium"
    assert prefs["disliked_ingredients"] == ["bitter gourd"]
    assert prefs["busy_days"] == ["MONDAY", "FRIDAY"]
    assert prefs["weekday_cooking_time_minutes"] == 30
    assert prefs["weekend_cooking_time_minutes"] == 60


@pytest.mark.asyncio
async def test_get_current_user_no_preferences(
    db_session: AsyncSession, user_no_prefs: User
):
    """GET /me returns user with null preferences when none exist."""
    async with _make_client(db_session, user_no_prefs) as ac:
        response = await ac.get("/api/v1/users/me")

    app.dependency_overrides.clear()

    assert response.status_code == 200
    data = response.json()
    assert data["id"] == user_no_prefs.id
    assert data["is_onboarded"] is False
    assert data["preferences"] is None


@pytest.mark.asyncio
async def test_get_current_user_unauthorized(unauthenticated_client: AsyncClient):
    """GET /me returns 401 without authentication."""
    response = await unauthenticated_client.get("/api/v1/users/me")
    assert response.status_code == 401


# ==================== PUT /preferences Tests ====================


@pytest.mark.asyncio
async def test_update_preferences_first_time(
    db_session: AsyncSession, user_no_prefs: User
):
    """PUT /preferences creates new preferences record when none exists."""
    async with _make_client(db_session, user_no_prefs) as ac:
        response = await ac.put(
            "/api/v1/users/preferences",
            json={
                "household_size": 3,
                "primary_diet": "non_vegetarian",
                "cuisine_preferences": ["north"],
            },
        )

    app.dependency_overrides.clear()

    assert response.status_code == 200
    data = response.json()
    assert data["preferences"] is not None
    assert data["preferences"]["household_size"] == 3
    assert data["preferences"]["dietary_type"] == "non_vegetarian"
    assert data["preferences"]["cuisine_preferences"] == ["north"]


@pytest.mark.asyncio
async def test_update_preferences_existing(
    db_session: AsyncSession, user_with_prefs: User
):
    """PUT /preferences updates existing preferences record."""
    async with _make_client(db_session, user_with_prefs) as ac:
        response = await ac.put(
            "/api/v1/users/preferences",
            json={
                "spice_level": "spicy",
                "cuisine_preferences": ["south", "west"],
            },
        )

    app.dependency_overrides.clear()

    assert response.status_code == 200
    data = response.json()
    prefs = data["preferences"]
    assert prefs["spice_level"] == "spicy"
    assert prefs["cuisine_preferences"] == ["south", "west"]
    # Other fields preserved
    assert prefs["dietary_type"] == "vegetarian"
    assert prefs["household_size"] == 4


@pytest.mark.asyncio
async def test_update_preferences_primary_diet_mapping(
    db_session: AsyncSession, user_no_prefs: User
):
    """PUT /preferences maps primary_diet field to dietary_type column."""
    async with _make_client(db_session, user_no_prefs) as ac:
        response = await ac.put(
            "/api/v1/users/preferences",
            json={"primary_diet": "vegan"},
        )

    app.dependency_overrides.clear()

    assert response.status_code == 200
    assert response.json()["preferences"]["dietary_type"] == "vegan"

    # Verify in DB
    result = await db_session.execute(
        select(UserPreferences).where(UserPreferences.user_id == user_no_prefs.id)
    )
    prefs = result.scalar_one()
    assert prefs.dietary_type == "vegan"


@pytest.mark.asyncio
async def test_update_preferences_busy_days(
    db_session: AsyncSession, user_no_prefs: User
):
    """PUT /preferences persists busy_days array correctly."""
    async with _make_client(db_session, user_no_prefs) as ac:
        response = await ac.put(
            "/api/v1/users/preferences",
            json={"busy_days": ["TUESDAY", "THURSDAY"]},
        )

    app.dependency_overrides.clear()

    assert response.status_code == 200
    assert response.json()["preferences"]["busy_days"] == ["TUESDAY", "THURSDAY"]


@pytest.mark.asyncio
async def test_update_preferences_cooking_times(
    db_session: AsyncSession, user_no_prefs: User
):
    """PUT /preferences maps weekday/weekend cooking time to _minutes columns."""
    async with _make_client(db_session, user_no_prefs) as ac:
        response = await ac.put(
            "/api/v1/users/preferences",
            json={
                "weekday_cooking_time": 20,
                "weekend_cooking_time": 90,
            },
        )

    app.dependency_overrides.clear()

    assert response.status_code == 200
    prefs = response.json()["preferences"]
    assert prefs["weekday_cooking_time_minutes"] == 20
    assert prefs["weekend_cooking_time_minutes"] == 90


@pytest.mark.asyncio
async def test_update_preferences_marks_onboarded(
    db_session: AsyncSession, user_no_prefs: User
):
    """PUT /preferences sets is_onboarded to True."""
    assert user_no_prefs.is_onboarded is False

    async with _make_client(db_session, user_no_prefs) as ac:
        response = await ac.put(
            "/api/v1/users/preferences",
            json={"household_size": 2},
        )

    app.dependency_overrides.clear()

    assert response.status_code == 200
    assert response.json()["is_onboarded"] is True


@pytest.mark.asyncio
async def test_update_preferences_unauthorized(unauthenticated_client: AsyncClient):
    """PUT /preferences returns 401 without authentication."""
    response = await unauthenticated_client.put(
        "/api/v1/users/preferences",
        json={"household_size": 5},
    )
    assert response.status_code == 401
