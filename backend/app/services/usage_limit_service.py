"""AI usage limit service for rate limiting expensive operations."""

import logging
from datetime import datetime, timedelta, timezone

from fastapi import HTTPException
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import settings
from app.models.usage_log import UsageLog

logger = logging.getLogger(__name__)


async def check_usage_limit(
    db: AsyncSession, user_id: str, action: str
) -> None:
    """Check if user has exceeded their daily limit for an action.

    Args:
        db: Database session
        user_id: User ID
        action: Action type ("meal_generation", "chat_message", "photo_analysis")

    Raises:
        HTTPException: 429 if limit exceeded
    """
    limit = _get_limit_for_action(action)
    if limit <= 0:
        return  # No limit configured

    today_start = datetime.now(timezone.utc).replace(
        hour=0, minute=0, second=0, microsecond=0
    )

    result = await db.execute(
        select(func.count(UsageLog.id))
        .where(UsageLog.user_id == user_id)
        .where(UsageLog.action == action)
        .where(UsageLog.created_at >= today_start)
    )
    count = result.scalar() or 0

    if count >= limit:
        logger.warning(
            f"User {user_id} exceeded daily {action} limit: {count}/{limit}"
        )
        raise HTTPException(
            status_code=429,
            detail=f"Daily {action.replace('_', ' ')} limit reached ({limit}/day). Try again tomorrow.",
        )


async def record_usage(
    db: AsyncSession, user_id: str, action: str
) -> None:
    """Record a usage event.

    Args:
        db: Database session
        user_id: User ID
        action: Action type
    """
    log = UsageLog(user_id=user_id, action=action)
    db.add(log)
    await db.commit()


def _get_limit_for_action(action: str) -> int:
    """Get the daily limit for a given action."""
    limits = {
        "meal_generation": settings.daily_meal_generation_limit,
        "chat_message": settings.daily_chat_limit,
        "photo_analysis": settings.daily_photo_analysis_limit,
    }
    return limits.get(action, 0)
