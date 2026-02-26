"""Tests for the cleanup service.

Covers:
- delete_old_chat_messages: removes chat messages older than retention period
- delete_inactive_meal_plans: removes inactive meal plans older than retention period
- run_all_cleanups: orchestrates all cleanup tasks
"""

import uuid
from datetime import date, datetime, timedelta, timezone

import pytest
import pytest_asyncio
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func

from app.models.chat import ChatMessage
from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.user import User
from app.services.cleanup_service import (
    delete_old_chat_messages,
    delete_inactive_meal_plans,
    run_all_cleanups,
)


# ==================== Helpers ====================


def _make_chat_message(user_id: str, created_at: datetime) -> ChatMessage:
    """Create a ChatMessage instance with a specific created_at timestamp."""
    msg = ChatMessage(
        id=str(uuid.uuid4()),
        user_id=user_id,
        role="user",
        content="test message",
        message_type="text",
    )
    # Override the default so we can control the timestamp in tests
    msg.created_at = created_at
    msg.updated_at = created_at
    return msg


def _make_meal_plan(
    user_id: str,
    is_active: bool,
    created_at: datetime,
) -> MealPlan:
    """Create a MealPlan instance with controlled is_active and created_at."""
    plan = MealPlan(
        id=str(uuid.uuid4()),
        user_id=user_id,
        week_start_date=date(2025, 1, 6),
        week_end_date=date(2025, 1, 12),
        is_active=is_active,
    )
    plan.created_at = created_at
    plan.updated_at = created_at
    return plan


# ==================== delete_old_chat_messages ====================


@pytest.mark.asyncio
async def test_delete_old_chat_messages_removes_old(
    db_session: AsyncSession, test_user: User
):
    """Old chat messages beyond the retention window are deleted."""
    now = datetime.now(timezone.utc)
    old_msg = _make_chat_message(test_user.id, now - timedelta(days=45))
    recent_msg = _make_chat_message(test_user.id, now - timedelta(days=5))

    db_session.add_all([old_msg, recent_msg])
    await db_session.commit()

    deleted = await delete_old_chat_messages(db_session, retention_days=30)

    assert deleted == 1

    # Verify only the recent message remains
    result = await db_session.execute(
        select(func.count(ChatMessage.id)).where(
            ChatMessage.user_id == test_user.id
        )
    )
    remaining = result.scalar()
    assert remaining == 1


@pytest.mark.asyncio
async def test_delete_old_chat_messages_keeps_recent(
    db_session: AsyncSession, test_user: User
):
    """Chat messages within the retention window are preserved."""
    now = datetime.now(timezone.utc)
    msg1 = _make_chat_message(test_user.id, now - timedelta(days=10))
    msg2 = _make_chat_message(test_user.id, now - timedelta(days=1))

    db_session.add_all([msg1, msg2])
    await db_session.commit()

    deleted = await delete_old_chat_messages(db_session, retention_days=30)

    assert deleted == 0

    result = await db_session.execute(
        select(func.count(ChatMessage.id)).where(
            ChatMessage.user_id == test_user.id
        )
    )
    remaining = result.scalar()
    assert remaining == 2


@pytest.mark.asyncio
async def test_delete_old_chat_messages_empty_table(
    db_session: AsyncSession, test_user: User
):
    """Calling cleanup on an empty table returns 0 without error."""
    deleted = await delete_old_chat_messages(db_session, retention_days=30)
    assert deleted == 0


# ==================== delete_inactive_meal_plans ====================


@pytest.mark.asyncio
async def test_delete_inactive_meal_plans_removes_old_inactive(
    db_session: AsyncSession, test_user: User
):
    """Inactive meal plans older than the retention window are deleted."""
    now = datetime.now(timezone.utc)
    old_inactive = _make_meal_plan(test_user.id, is_active=False, created_at=now - timedelta(days=120))
    recent_inactive = _make_meal_plan(test_user.id, is_active=False, created_at=now - timedelta(days=30))
    old_active = _make_meal_plan(test_user.id, is_active=True, created_at=now - timedelta(days=120))

    db_session.add_all([old_inactive, recent_inactive, old_active])
    await db_session.commit()

    deleted = await delete_inactive_meal_plans(db_session, retention_days=90)

    assert deleted == 1

    # Verify: recent_inactive and old_active remain
    result = await db_session.execute(
        select(func.count(MealPlan.id)).where(
            MealPlan.user_id == test_user.id
        )
    )
    remaining = result.scalar()
    assert remaining == 2


@pytest.mark.asyncio
async def test_delete_inactive_meal_plans_keeps_active(
    db_session: AsyncSession, test_user: User
):
    """Active meal plans are never deleted regardless of age."""
    now = datetime.now(timezone.utc)
    old_active = _make_meal_plan(test_user.id, is_active=True, created_at=now - timedelta(days=365))

    db_session.add(old_active)
    await db_session.commit()

    deleted = await delete_inactive_meal_plans(db_session, retention_days=90)

    assert deleted == 0

    result = await db_session.execute(
        select(func.count(MealPlan.id)).where(
            MealPlan.user_id == test_user.id
        )
    )
    remaining = result.scalar()
    assert remaining == 1


# ==================== run_all_cleanups ====================


@pytest.mark.asyncio
async def test_run_all_cleanups(
    db_session: AsyncSession, test_user: User
):
    """run_all_cleanups orchestrates all individual cleanup functions."""
    now = datetime.now(timezone.utc)

    # One old chat message (> 30 days)
    old_msg = _make_chat_message(test_user.id, now - timedelta(days=60))
    db_session.add(old_msg)

    # One old inactive meal plan (> 90 days)
    old_plan = _make_meal_plan(test_user.id, is_active=False, created_at=now - timedelta(days=100))
    db_session.add(old_plan)

    await db_session.commit()

    results = await run_all_cleanups(db_session)

    assert results["chat_messages"] == 1
    assert results["inactive_meal_plans"] == 1
