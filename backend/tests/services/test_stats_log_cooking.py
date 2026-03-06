"""Tests for log_cooking() in stats_service.

Requirement: #35 - Service-level unit tests for cooking streak logging.
Verifies streak creation, meal type marking, double-count prevention,
consecutive day logic, gap reset, and custom date support.

Note: SQLite doesn't apply column defaults to in-memory ORM objects.
CookingDay created by log_cooking() needs explicit defaults for
meals_cooked, breakfast_cooked, etc. We use an event listener to
apply these defaults in tests.
"""

import uuid
from datetime import date, timedelta
from unittest.mock import AsyncMock, patch

import pytest
from sqlalchemy import event

from app.models.stats import CookingDay, CookingStreak
from app.services.stats_service import log_cooking


@pytest.fixture(autouse=True)
def apply_cooking_day_defaults():
    """Apply column defaults to CookingDay on init (SQLite workaround).

    SQLAlchemy column defaults are applied server-side on INSERT, not on
    Python object creation. PostgreSQL returns defaults after INSERT, but
    SQLite in-memory doesn't. This listener ensures the defaults are set.
    """

    def set_defaults(target, args, kwargs):
        if target.meals_cooked is None:
            target.meals_cooked = 0
        if target.breakfast_cooked is None:
            target.breakfast_cooked = False
        if target.lunch_cooked is None:
            target.lunch_cooked = False
        if target.dinner_cooked is None:
            target.dinner_cooked = False

    event.listen(CookingDay, "init", set_defaults)
    yield
    event.remove(CookingDay, "init", set_defaults)


@pytest.fixture
async def streak(db_session, test_user):
    """Create a cooking streak for the test user (no days yet)."""
    s = CookingStreak(
        id=str(uuid.uuid4()),
        user_id=test_user.id,
        current_streak=0,
        longest_streak=0,
        total_meals_cooked=0,
    )
    db_session.add(s)
    await db_session.commit()
    await db_session.refresh(s)
    return s


@patch(
    "app.services.stats_service.check_and_grant_achievements",
    new_callable=AsyncMock,
)
class TestLogCookingCreatesStreak:
    """Tests for initial cooking day creation on an existing streak."""

    async def test_log_cooking_creates_cooking_day(
        self, mock_achievements, db_session, test_user, streak
    ):
        """First log_cooking call creates a CookingDay and sets streak to 1."""
        result = await log_cooking(db_session, test_user, "breakfast")

        assert result.current_streak == 1
        assert result.total_meals_cooked == 1
        assert result.last_cooking_date is not None


@patch(
    "app.services.stats_service.check_and_grant_achievements",
    new_callable=AsyncMock,
)
class TestLogCookingMealTypes:
    """Tests for meal type marking and double-count prevention."""

    async def test_log_cooking_marks_meal_type(
        self, mock_achievements, db_session, test_user, streak
    ):
        """Logging breakfast sets breakfast_cooked=True and increments total."""
        result = await log_cooking(db_session, test_user, "breakfast")

        assert result.total_meals_cooked == 1

    async def test_log_cooking_no_double_count(
        self, mock_achievements, db_session, test_user, streak
    ):
        """Same meal type twice on same day doesn't increment meals_cooked twice."""
        today = date.today()
        await log_cooking(db_session, test_user, "breakfast", cooking_date=today)
        result = await log_cooking(
            db_session, test_user, "breakfast", cooking_date=today
        )

        assert result.total_meals_cooked == 1


@patch(
    "app.services.stats_service.check_and_grant_achievements",
    new_callable=AsyncMock,
)
class TestLogCookingStreakLogic:
    """Tests for streak increment, reset, and same-day behavior."""

    async def test_log_cooking_consecutive_day_increments_streak(
        self, mock_achievements, db_session, test_user, streak
    ):
        """Day after last_cooking_date bumps current_streak."""
        yesterday = date.today() - timedelta(days=1)
        today = date.today()

        await log_cooking(db_session, test_user, "breakfast", cooking_date=yesterday)
        result = await log_cooking(db_session, test_user, "lunch", cooking_date=today)

        assert result.current_streak == 2

    async def test_log_cooking_gap_resets_streak(
        self, mock_achievements, db_session, test_user, streak
    ):
        """Gap > 1 day resets current_streak to 1."""
        three_days_ago = date.today() - timedelta(days=3)
        today = date.today()

        await log_cooking(
            db_session, test_user, "breakfast", cooking_date=three_days_ago
        )
        result = await log_cooking(
            db_session, test_user, "breakfast", cooking_date=today
        )

        assert result.current_streak == 1

    async def test_log_cooking_same_day_no_streak_change(
        self, mock_achievements, db_session, test_user, streak
    ):
        """Multiple meals on same day keep streak the same."""
        today = date.today()

        await log_cooking(db_session, test_user, "breakfast", cooking_date=today)
        result = await log_cooking(db_session, test_user, "lunch", cooking_date=today)

        assert result.current_streak == 1

    async def test_log_cooking_updates_longest_streak(
        self, mock_achievements, db_session, test_user, streak
    ):
        """longest_streak updated when current exceeds it."""
        day1 = date.today() - timedelta(days=2)
        day2 = date.today() - timedelta(days=1)
        day3 = date.today()

        await log_cooking(db_session, test_user, "breakfast", cooking_date=day1)
        await log_cooking(db_session, test_user, "breakfast", cooking_date=day2)
        result = await log_cooking(
            db_session, test_user, "breakfast", cooking_date=day3
        )

        assert result.longest_streak == 3
        assert result.current_streak == 3

    async def test_log_cooking_custom_date(
        self, mock_achievements, db_session, test_user, streak
    ):
        """Passing cooking_date uses that date instead of today."""
        custom_date = date(2026, 1, 15)

        result = await log_cooking(
            db_session, test_user, "dinner", cooking_date=custom_date
        )

        assert result.last_cooking_date == "2026-01-15"
        assert result.total_meals_cooked == 1
