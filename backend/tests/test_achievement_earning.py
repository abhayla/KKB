"""Tests for achievement earning logic.

Verifies that achievements are automatically granted when user actions
meet the requirements (meals cooked, streak milestones, etc.).
"""

import uuid
import pytest
from datetime import date, timedelta
from unittest.mock import patch, AsyncMock

from app.models.stats import Achievement, CookingDay, CookingStreak, UserAchievement
from app.models.user import User
from app.services.stats_service import check_and_grant_achievements, log_cooking


@pytest.fixture
async def achievements(db_session):
    """Seed test achievements."""
    achievement_defs = [
        ("First Meal", "Cook your first meal", "🍳", "cooking", "meals_cooked", 1),
        ("5 Meals", "Cook 5 meals", "🥘", "cooking", "meals_cooked", 5),
        ("10 Meals", "Cook 10 meals", "👨‍🍳", "cooking", "meals_cooked", 10),
        ("3-Day Streak", "Cook 3 days in a row", "🔥", "streak", "streak_days", 3),
        ("7-Day Streak", "Cook 7 days in a row", "⚡", "streak", "streak_days", 7),
    ]
    achievements = []
    for name, desc, icon, cat, req_type, req_val in achievement_defs:
        a = Achievement(
            id=str(uuid.uuid4()),
            name=name,
            description=desc,
            icon=icon,
            category=cat,
            requirement_type=req_type,
            requirement_value=req_val,
        )
        db_session.add(a)
        achievements.append(a)
    await db_session.commit()
    return achievements


@pytest.fixture
async def streak(db_session, test_user):
    """Create a cooking streak for test user."""
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


class TestAchievementEarning:
    """Test achievement granting logic."""

    @pytest.mark.asyncio
    @patch("app.services.notification_service.create_notification", new_callable=AsyncMock)
    async def test_first_meal_achievement_granted(self, mock_notify, db_session, test_user, achievements, streak):
        """First Meal achievement granted after 1 meal."""
        streak.total_meals_cooked = 1
        streak.current_streak = 1
        await db_session.commit()

        unlocked = await check_and_grant_achievements(db_session, test_user, streak)

        assert "First Meal" in unlocked

    @pytest.mark.asyncio
    @patch("app.services.notification_service.create_notification", new_callable=AsyncMock)
    async def test_multiple_achievements_granted(self, mock_notify, db_session, test_user, achievements, streak):
        """Multiple achievements granted when multiple thresholds met."""
        streak.total_meals_cooked = 5
        streak.current_streak = 1
        await db_session.commit()

        unlocked = await check_and_grant_achievements(db_session, test_user, streak)

        assert "First Meal" in unlocked
        assert "5 Meals" in unlocked

    @pytest.mark.asyncio
    @patch("app.services.notification_service.create_notification", new_callable=AsyncMock)
    async def test_already_unlocked_not_duplicated(self, mock_notify, db_session, test_user, achievements, streak):
        """Already unlocked achievements are not granted again."""
        streak.total_meals_cooked = 5
        streak.current_streak = 1
        await db_session.commit()

        # Grant first time
        await check_and_grant_achievements(db_session, test_user, streak)

        # Grant second time - should not duplicate
        unlocked2 = await check_and_grant_achievements(db_session, test_user, streak)
        assert len(unlocked2) == 0

    @pytest.mark.asyncio
    @patch("app.services.notification_service.create_notification", new_callable=AsyncMock)
    async def test_streak_achievement_granted(self, mock_notify, db_session, test_user, achievements, streak):
        """Streak achievement granted when streak threshold met."""
        streak.total_meals_cooked = 3
        streak.current_streak = 3
        await db_session.commit()

        unlocked = await check_and_grant_achievements(db_session, test_user, streak)

        assert "3-Day Streak" in unlocked

    @pytest.mark.asyncio
    @patch("app.services.notification_service.create_notification", new_callable=AsyncMock)
    async def test_no_achievements_when_below_threshold(self, mock_notify, db_session, test_user, achievements, streak):
        """No achievements granted when below all thresholds."""
        streak.total_meals_cooked = 0
        streak.current_streak = 0
        await db_session.commit()

        unlocked = await check_and_grant_achievements(db_session, test_user, streak)

        assert len(unlocked) == 0

    @pytest.mark.asyncio
    @patch("app.services.notification_service.create_notification", new_callable=AsyncMock)
    async def test_achievement_notification_called(self, mock_notify, db_session, test_user, achievements, streak):
        """Achievement unlock triggers notification."""
        streak.total_meals_cooked = 1
        streak.current_streak = 1
        await db_session.commit()

        await check_and_grant_achievements(db_session, test_user, streak)

        # Notification should have been called (achievement + potential streak milestone)
        assert mock_notify.called

    @pytest.mark.asyncio
    @patch("app.services.notification_service.create_notification", new_callable=AsyncMock)
    async def test_10_meals_requires_10_not_less(self, mock_notify, db_session, test_user, achievements, streak):
        """10 Meals achievement not granted at 9 meals."""
        streak.total_meals_cooked = 9
        streak.current_streak = 1
        await db_session.commit()

        unlocked = await check_and_grant_achievements(db_session, test_user, streak)

        assert "10 Meals" not in unlocked
        assert "First Meal" in unlocked
        assert "5 Meals" in unlocked

    @pytest.mark.asyncio
    @patch("app.services.notification_service.create_notification", new_callable=AsyncMock)
    async def test_7_day_streak_achievement(self, mock_notify, db_session, test_user, achievements, streak):
        """7-Day Streak achievement granted at 7-day streak."""
        streak.total_meals_cooked = 7
        streak.current_streak = 7
        streak.longest_streak = 7
        await db_session.commit()

        unlocked = await check_and_grant_achievements(db_session, test_user, streak)

        assert "7-Day Streak" in unlocked
        assert "3-Day Streak" in unlocked
