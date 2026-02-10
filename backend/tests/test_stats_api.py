"""
Tests for Stats API endpoints.

Covers:
- GET /api/v1/stats/streak — cooking streak statistics
- GET /api/v1/stats/monthly — monthly cooking statistics
"""

import pytest
from datetime import date, timedelta
from uuid import uuid4

import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.database import get_db
from app.main import app
from app.models.stats import Achievement, CookingDay, CookingStreak, UserAchievement
from app.models.user import User


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def stats_user(db_session: AsyncSession) -> User:
    """Create a user for stats tests."""
    user_id = str(uuid4())
    user = User(
        id=user_id,
        firebase_uid=f"firebase-stats-{user_id}",
        email=f"stats-{user_id}@example.com",
        name="Stats Test User",
        is_onboarded=True,
        is_active=True,
    )
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def stats_client(db_session: AsyncSession, stats_user: User):
    """Authenticated client for the stats user."""

    async def override_get_db():
        yield db_session

    async def override_get_current_user():
        return stats_user

    app.dependency_overrides[get_db] = override_get_db
    app.dependency_overrides[get_current_user] = override_get_current_user

    async with AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://test",
    ) as ac:
        yield ac

    app.dependency_overrides.clear()


@pytest_asyncio.fixture
async def cooking_streak_data(
    db_session: AsyncSession, stats_user: User
) -> CookingStreak:
    """Create cooking streak with cooking days."""
    today = date.today()

    streak = CookingStreak(
        id=str(uuid4()),
        user_id=stats_user.id,
        current_streak=5,
        longest_streak=12,
        total_meals_cooked=45,
        last_cooking_date=today,
    )
    db_session.add(streak)
    await db_session.flush()

    # Add cooking days for this week
    week_start = today - timedelta(days=today.weekday())
    for i in range(5):
        day_date = week_start + timedelta(days=i)
        day = CookingDay(
            id=str(uuid4()),
            cooking_streak_id=streak.id,
            date=day_date,
            meals_cooked=2,
            breakfast_cooked=True,
            lunch_cooked=True,
            dinner_cooked=False,
        )
        db_session.add(day)

    await db_session.commit()
    await db_session.refresh(streak)
    return streak


@pytest_asyncio.fixture
async def achievement_data(db_session: AsyncSession, stats_user: User) -> Achievement:
    """Create achievement and user achievement."""
    today = date.today()

    achievement = Achievement(
        id=str(uuid4()),
        name="First Meal",
        description="Cook your first meal",
        icon="🍳",
        category="streak",
        requirement_type="meals_cooked",
        requirement_value=1,
        is_active=True,
    )
    db_session.add(achievement)
    await db_session.flush()

    user_achievement = UserAchievement(
        id=str(uuid4()),
        user_id=stats_user.id,
        achievement_id=achievement.id,
        unlocked_at=today,
    )
    db_session.add(user_achievement)
    await db_session.commit()
    return achievement


# ==================== GET /streak Tests ====================


@pytest.mark.asyncio
async def test_get_streak_with_data(
    stats_client: AsyncClient, cooking_streak_data: CookingStreak
):
    """GET /streak returns current and longest streak when data exists."""
    response = await stats_client.get("/api/v1/stats/streak")

    assert response.status_code == 200
    data = response.json()
    assert data["current_streak"] == 5
    assert data["longest_streak"] == 12
    assert data["total_meals_cooked"] == 45
    assert data["last_cooking_date"] is not None


@pytest.mark.asyncio
async def test_get_streak_no_data(stats_client: AsyncClient):
    """GET /streak returns zeros when no streak exists."""
    response = await stats_client.get("/api/v1/stats/streak")

    assert response.status_code == 200
    data = response.json()
    assert data["current_streak"] == 0
    assert data["longest_streak"] == 0
    assert data["total_meals_cooked"] == 0
    assert data["last_cooking_date"] is None
    assert data["streak_start_date"] is None
    assert data["days_this_week"] == 0


@pytest.mark.asyncio
async def test_get_streak_days_this_week(
    stats_client: AsyncClient, cooking_streak_data: CookingStreak
):
    """GET /streak returns correct weekday count for current week."""
    response = await stats_client.get("/api/v1/stats/streak")

    assert response.status_code == 200
    data = response.json()
    # We created 5 cooking days (Mon-Fri of current week), all with meals_cooked=2.
    # The service counts ALL days in current week where meals_cooked > 0,
    # including future days (no "today" cutoff), so all 5 are counted.
    assert data["days_this_week"] == 5


@pytest.mark.asyncio
async def test_get_streak_unauthorized(unauthenticated_client: AsyncClient):
    """GET /streak returns 401 without authentication."""
    response = await unauthenticated_client.get("/api/v1/stats/streak")
    assert response.status_code == 401


# ==================== GET /monthly Tests ====================


@pytest.mark.asyncio
async def test_monthly_stats_valid_month(
    stats_client: AsyncClient, cooking_streak_data: CookingStreak
):
    """GET /monthly returns stats for specified month."""
    today = date.today()
    month_str = f"{today.year}-{today.month:02d}"

    response = await stats_client.get(f"/api/v1/stats/monthly?month={month_str}")

    assert response.status_code == 200
    data = response.json()
    assert data["month"] == month_str
    assert data["total_meals_cooked"] >= 0
    assert isinstance(data["daily_records"], list)
    assert isinstance(data["cuisine_breakdown"], list)
    assert isinstance(data["achievements_unlocked"], list)


@pytest.mark.asyncio
async def test_monthly_stats_with_cooking_days(
    stats_client: AsyncClient, cooking_streak_data: CookingStreak
):
    """GET /monthly has daily_records populated from cooking days."""
    today = date.today()
    month_str = f"{today.year}-{today.month:02d}"

    response = await stats_client.get(f"/api/v1/stats/monthly?month={month_str}")

    assert response.status_code == 200
    data = response.json()
    # We created 5 cooking days, all in current week
    assert len(data["daily_records"]) >= 1
    assert data["total_cooking_days"] >= 1

    # Each record should have the expected structure
    if data["daily_records"]:
        record = data["daily_records"][0]
        assert "date" in record
        assert "meals_cooked" in record
        assert "breakfast_cooked" in record
        assert "lunch_cooked" in record
        assert "dinner_cooked" in record


@pytest.mark.asyncio
async def test_monthly_stats_with_achievements(
    stats_client: AsyncClient,
    cooking_streak_data: CookingStreak,
    achievement_data: Achievement,
):
    """GET /monthly has achievements array populated."""
    today = date.today()
    month_str = f"{today.year}-{today.month:02d}"

    response = await stats_client.get(f"/api/v1/stats/monthly?month={month_str}")

    assert response.status_code == 200
    data = response.json()
    assert len(data["achievements_unlocked"]) == 1
    achievement = data["achievements_unlocked"][0]
    assert achievement["name"] == "First Meal"
    assert achievement["is_unlocked"] is True
    assert achievement["icon"] == "🍳"


@pytest.mark.asyncio
async def test_monthly_stats_no_data(stats_client: AsyncClient):
    """GET /monthly returns empty arrays for a quiet month."""
    response = await stats_client.get("/api/v1/stats/monthly?month=2020-01")

    assert response.status_code == 200
    data = response.json()
    assert data["total_meals_cooked"] == 0
    assert data["total_cooking_days"] == 0
    assert data["daily_records"] == []
    assert data["cuisine_breakdown"] == []
    assert data["achievements_unlocked"] == []
    assert data["favorite_cuisine"] is None


@pytest.mark.asyncio
async def test_monthly_stats_invalid_format(stats_client: AsyncClient):
    """GET /monthly falls back to current month for invalid format."""
    response = await stats_client.get("/api/v1/stats/monthly?month=invalid")

    assert response.status_code == 200
    data = response.json()
    today = date.today()
    expected_month = f"{today.year}-{today.month:02d}"
    assert data["month"] == expected_month


@pytest.mark.asyncio
async def test_monthly_stats_unauthorized(unauthenticated_client: AsyncClient):
    """GET /monthly returns 401 without authentication."""
    response = await unauthenticated_client.get(
        "/api/v1/stats/monthly?month=2026-01"
    )
    assert response.status_code == 401
