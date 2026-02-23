"""Tests for AI usage limit service."""

import uuid
from datetime import datetime, timedelta, timezone

import pytest
from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.usage_log import UsageLog
from app.models.user import User


class TestUsageLimitService:
    """Test the usage limit checking and recording."""

    async def test_record_usage(self, db_session: AsyncSession, test_user: User):
        """Recording usage should create a UsageLog entry."""
        from app.services.usage_limit_service import record_usage

        await record_usage(db_session, test_user.id, "chat_message")

        result = await db_session.execute(
            select(UsageLog).where(UsageLog.user_id == test_user.id)
        )
        logs = result.scalars().all()
        assert len(logs) == 1
        assert logs[0].action == "chat_message"

    async def test_check_under_limit(self, db_session: AsyncSession, test_user: User):
        """Should not raise when under daily limit."""
        from app.services.usage_limit_service import check_usage_limit

        # Should not raise
        await check_usage_limit(db_session, test_user.id, "meal_generation")

    async def test_check_at_limit_raises(self, db_session: AsyncSession, test_user: User):
        """Should raise 429 when daily limit is reached."""
        from app.services.usage_limit_service import check_usage_limit, record_usage

        # Record 5 uses (the default daily_meal_generation_limit)
        for _ in range(5):
            await record_usage(db_session, test_user.id, "meal_generation")

        with pytest.raises(HTTPException) as exc_info:
            await check_usage_limit(db_session, test_user.id, "meal_generation")

        assert exc_info.value.status_code == 429
        assert "limit reached" in exc_info.value.detail

    async def test_different_actions_separate_limits(self, db_session: AsyncSession, test_user: User):
        """Different action types should have separate limits."""
        from app.services.usage_limit_service import check_usage_limit, record_usage

        # Use up meal_generation limit
        for _ in range(5):
            await record_usage(db_session, test_user.id, "meal_generation")

        # Chat should still be allowed
        await check_usage_limit(db_session, test_user.id, "chat_message")

    async def test_old_usage_not_counted(self, db_session: AsyncSession, test_user: User):
        """Usage from previous days should not count towards today's limit."""
        from app.services.usage_limit_service import check_usage_limit

        # Add old usage records
        yesterday = datetime.now(timezone.utc) - timedelta(days=1)
        for _ in range(10):
            log = UsageLog(
                user_id=test_user.id,
                action="meal_generation",
                created_at=yesterday,
            )
            db_session.add(log)
        await db_session.commit()

        # Should not raise (old records don't count)
        await check_usage_limit(db_session, test_user.id, "meal_generation")

    async def test_different_users_separate_limits(self, db_session: AsyncSession, test_user: User):
        """Different users should have separate limits."""
        from app.services.usage_limit_service import check_usage_limit, record_usage

        # Use up limit for test_user
        for _ in range(5):
            await record_usage(db_session, test_user.id, "meal_generation")

        # Create second user
        user2 = User(
            id=str(uuid.uuid4()),
            firebase_uid="other-uid",
            email="other@example.com",
            name="Other User",
            is_onboarded=True,
            is_active=True,
        )
        db_session.add(user2)
        await db_session.commit()

        # Second user should not be limited
        await check_usage_limit(db_session, user2.id, "meal_generation")

    async def test_unknown_action_no_limit(self, db_session: AsyncSession, test_user: User):
        """Unknown action types should have no limit (returns 0)."""
        from app.services.usage_limit_service import check_usage_limit

        # Should not raise for unknown action
        await check_usage_limit(db_session, test_user.id, "unknown_action")

    async def test_chat_limit_higher_than_meal_gen(self, db_session: AsyncSession, test_user: User):
        """Chat limit (50) should be higher than meal generation limit (5)."""
        from app.config import settings

        assert settings.daily_chat_limit > settings.daily_meal_generation_limit
