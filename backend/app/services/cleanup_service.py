"""Cleanup service for removing old data.

Provides functions to delete stale chat messages and inactive meal plans
to keep the database lean.
"""

import logging
from datetime import datetime, timedelta, timezone

from sqlalchemy import delete, select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.chat import ChatMessage
from app.models.meal_plan import MealPlan, MealPlanItem

logger = logging.getLogger(__name__)

# Default retention periods
CHAT_MESSAGE_RETENTION_DAYS = 30
INACTIVE_MEAL_PLAN_RETENTION_DAYS = 90


async def delete_old_chat_messages(
    db: AsyncSession,
    retention_days: int = CHAT_MESSAGE_RETENTION_DAYS,
) -> int:
    """Delete chat messages older than the retention period.

    Args:
        db: Database session.
        retention_days: Number of days to retain messages. Messages older
            than this are deleted. Defaults to 30.

    Returns:
        Number of deleted chat messages.
    """
    cutoff = datetime.now(timezone.utc) - timedelta(days=retention_days)

    # Count first for logging
    count_result = await db.execute(
        select(func.count(ChatMessage.id)).where(
            ChatMessage.created_at < cutoff
        )
    )
    count = count_result.scalar() or 0

    if count == 0:
        logger.info("No old chat messages to delete (cutoff: %s)", cutoff.isoformat())
        return 0

    await db.execute(
        delete(ChatMessage).where(ChatMessage.created_at < cutoff)
    )
    await db.commit()

    logger.info(
        "Deleted %d chat messages older than %d days (cutoff: %s)",
        count,
        retention_days,
        cutoff.isoformat(),
    )
    return count


async def delete_inactive_meal_plans(
    db: AsyncSession,
    retention_days: int = INACTIVE_MEAL_PLAN_RETENTION_DAYS,
) -> int:
    """Delete inactive meal plans older than the retention period.

    Only meal plans where ``is_active=False`` are considered. Active meal
    plans are never removed regardless of age. Associated ``MealPlanItem``
    rows are cascade-deleted by the database foreign key constraint.

    Args:
        db: Database session.
        retention_days: Number of days to retain inactive plans. Defaults
            to 90.

    Returns:
        Number of deleted meal plans.
    """
    cutoff = datetime.now(timezone.utc) - timedelta(days=retention_days)

    # Count first for logging
    count_result = await db.execute(
        select(func.count(MealPlan.id)).where(
            MealPlan.is_active == False,  # noqa: E712
            MealPlan.created_at < cutoff,
        )
    )
    count = count_result.scalar() or 0

    if count == 0:
        logger.info(
            "No inactive meal plans to delete (cutoff: %s)", cutoff.isoformat()
        )
        return 0

    await db.execute(
        delete(MealPlan).where(
            MealPlan.is_active == False,  # noqa: E712
            MealPlan.created_at < cutoff,
        )
    )
    await db.commit()

    logger.info(
        "Deleted %d inactive meal plans older than %d days (cutoff: %s)",
        count,
        retention_days,
        cutoff.isoformat(),
    )
    return count


async def run_all_cleanups(db: AsyncSession) -> dict[str, int]:
    """Run all cleanup tasks.

    Convenience function that runs every cleanup operation and returns
    a summary of what was deleted.

    Args:
        db: Database session.

    Returns:
        Dictionary mapping cleanup task name to number of deleted rows.
    """
    results: dict[str, int] = {}

    results["chat_messages"] = await delete_old_chat_messages(db)
    results["inactive_meal_plans"] = await delete_inactive_meal_plans(db)

    logger.info("Cleanup complete: %s", results)
    return results
