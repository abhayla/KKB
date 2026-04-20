"""Tests for usage_limit_service.

Covers:
- _get_limit_for_action: pure lookup
- record_usage: INSERTs a UsageLog row
- check_usage_limit: passes under limit, raises 429 at/over limit,
  counts only today's events for the user, handles unknown action (no limit)
"""

import uuid
from datetime import datetime, timedelta, timezone

import pytest
from fastapi import HTTPException
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import settings
from app.models.usage_log import UsageLog
from app.models.user import User
from app.services.usage_limit_service import (
    _get_limit_for_action,
    check_usage_limit,
    record_usage,
)


# ==================== _get_limit_for_action ====================


class TestGetLimitForAction:
    def test_meal_generation_uses_settings(self):
        assert _get_limit_for_action("meal_generation") == settings.daily_meal_generation_limit

    def test_chat_message_uses_settings(self):
        assert _get_limit_for_action("chat_message") == settings.daily_chat_limit

    def test_photo_analysis_uses_settings(self):
        assert _get_limit_for_action("photo_analysis") == settings.daily_photo_analysis_limit

    def test_unknown_action_returns_zero(self):
        """Unknown actions are treated as 'no limit configured' (returns 0)
        and check_usage_limit returns early."""
        assert _get_limit_for_action("some_other_action") == 0


# ==================== record_usage ====================


@pytest.mark.asyncio
async def test_record_usage_persists_row(
    db_session: AsyncSession, test_user: User
):
    await record_usage(db_session, test_user.id, "chat_message")

    result = await db_session.execute(
        select(UsageLog).where(UsageLog.user_id == test_user.id)
    )
    rows = result.scalars().all()
    assert len(rows) == 1
    assert rows[0].action == "chat_message"


# ==================== check_usage_limit ====================


@pytest.mark.asyncio
async def test_check_usage_limit_passes_when_under_limit(
    db_session: AsyncSession, test_user: User
):
    # No usage yet -> should pass without raising.
    await check_usage_limit(db_session, test_user.id, "chat_message")


@pytest.mark.asyncio
async def test_check_usage_limit_raises_429_when_at_limit(
    db_session: AsyncSession, test_user: User
):
    limit = settings.daily_meal_generation_limit
    # Seed `limit` logs for today.
    for _ in range(limit):
        await record_usage(db_session, test_user.id, "meal_generation")

    with pytest.raises(HTTPException) as exc_info:
        await check_usage_limit(db_session, test_user.id, "meal_generation")

    assert exc_info.value.status_code == 429
    assert "meal generation" in exc_info.value.detail.lower()


@pytest.mark.asyncio
async def test_check_usage_limit_ignores_unknown_action(
    db_session: AsyncSession, test_user: User
):
    """Unknown actions have limit 0 -> check returns early without raising."""
    # Even seeding 1000 "unknown_action" logs wouldn't trigger.
    await check_usage_limit(db_session, test_user.id, "unknown_action")


@pytest.mark.asyncio
async def test_check_usage_limit_counts_only_today(
    db_session: AsyncSession, test_user: User
):
    """Usage from yesterday must NOT count against today's limit."""
    limit = settings.daily_chat_limit

    # Insert `limit` logs dated yesterday directly (bypass record_usage so we
    # can set created_at).
    yesterday = datetime.now(timezone.utc) - timedelta(days=1)
    for _ in range(limit):
        log = UsageLog(
            id=str(uuid.uuid4()),
            user_id=test_user.id,
            action="chat_message",
        )
        log.created_at = yesterday
        db_session.add(log)
    await db_session.commit()

    # Today's count is 0 -> limit not reached.
    await check_usage_limit(db_session, test_user.id, "chat_message")


@pytest.mark.asyncio
async def test_check_usage_limit_isolates_by_user(
    db_session: AsyncSession, test_user: User
):
    """One user's usage must not count against another user's limit."""
    other = User(
        id=str(uuid.uuid4()),
        firebase_uid=f"firebase-{uuid.uuid4().hex[:8]}",
        email=f"o-{uuid.uuid4().hex[:6]}@example.com",
        name="Other",
        is_active=True,
    )
    db_session.add(other)
    await db_session.commit()

    limit = settings.daily_meal_generation_limit
    # Max out the OTHER user's quota.
    for _ in range(limit):
        await record_usage(db_session, other.id, "meal_generation")

    # test_user should still be under-limit.
    await check_usage_limit(db_session, test_user.id, "meal_generation")
