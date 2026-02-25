"""Tests for notification auto-creation triggers.

Verifies that notifications are automatically created when key events
occur: meal plan generation, achievement unlock, cooking streak milestones.
"""

import pytest
from datetime import datetime, timezone

from app.services.notification_service import (
    notify_meal_plan_generated,
    notify_achievement_unlocked,
    notify_cooking_streak_milestone,
    get_notifications,
)


@pytest.fixture
async def user_id(db_session, test_user):
    """Get test user ID."""
    return test_user.id


class TestNotificationTriggers:
    """Test automatic notification creation on events."""

    @pytest.mark.asyncio
    async def test_meal_plan_notification_created(self, db_session, user_id):
        """Notification created when meal plan is generated."""
        notification = await notify_meal_plan_generated(
            db_session, user_id, "test-meal-plan-id"
        )
        assert notification is not None
        assert notification.type == "meal_plan_update"
        assert "meal plan is ready" in notification.title.lower()
        assert notification.user_id == user_id

    @pytest.mark.asyncio
    async def test_meal_plan_notification_has_action_data(self, db_session, user_id):
        """Meal plan notification includes action data with plan ID."""
        notification = await notify_meal_plan_generated(
            db_session, user_id, "mp-123"
        )
        assert notification.action_type == "open_meal_plan"
        assert "mp-123" in notification.action_data

    @pytest.mark.asyncio
    async def test_achievement_notification_created(self, db_session, user_id):
        """Notification created when achievement is unlocked."""
        notification = await notify_achievement_unlocked(
            db_session, user_id, "First Meal", "🍳"
        )
        assert notification is not None
        assert "First Meal" in notification.title
        assert notification.action_type == "open_stats"

    @pytest.mark.asyncio
    async def test_streak_milestone_3_days(self, db_session, user_id):
        """Notification created at 3-day streak milestone."""
        notification = await notify_cooking_streak_milestone(
            db_session, user_id, 3
        )
        assert notification is not None
        assert "3-day" in notification.title
        assert "streak" in notification.title.lower()

    @pytest.mark.asyncio
    async def test_streak_milestone_non_milestone_ignored(self, db_session, user_id):
        """No notification for non-milestone streak counts."""
        result = await notify_cooking_streak_milestone(
            db_session, user_id, 5
        )
        assert result is None

    @pytest.mark.asyncio
    async def test_streak_milestones_7_14_30(self, db_session, user_id):
        """Notifications created for all milestone values."""
        for days in [7, 14, 30]:
            notification = await notify_cooking_streak_milestone(
                db_session, user_id, days
            )
            assert notification is not None, f"No notification for {days}-day milestone"
            assert str(days) in notification.title
