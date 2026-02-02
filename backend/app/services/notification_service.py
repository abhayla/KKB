"""Notification service for managing user notifications."""

import json
import uuid
from datetime import datetime, timezone
from typing import Any, Optional

from sqlalchemy import and_, delete, select, update
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models.notification import FcmToken, Notification
from app.schemas.notification import (
    CreateNotificationRequest,
    NotificationActionData,
    NotificationResponse,
    NotificationsListResponse,
)


async def get_notifications(
    db: AsyncSession,
    user_id: str,
    include_expired: bool = False,
) -> NotificationsListResponse:
    """Get all notifications for a user.

    Args:
        db: Database session
        user_id: User ID
        include_expired: Whether to include expired notifications

    Returns:
        NotificationsListResponse with notifications list and counts
    """
    query = select(Notification).where(Notification.user_id == user_id)

    if not include_expired:
        query = query.where(
            (Notification.expires_at.is_(None))
            | (Notification.expires_at > datetime.now(timezone.utc))
        )

    query = query.order_by(Notification.created_at.desc())

    result = await db.execute(query)
    notifications = result.scalars().all()

    # Count unread
    unread_count = sum(1 for n in notifications if not n.is_read)

    return NotificationsListResponse(
        notifications=[_to_response(n) for n in notifications],
        unread_count=unread_count,
        total_count=len(notifications),
    )


async def get_notification_by_id(
    db: AsyncSession,
    notification_id: str,
    user_id: str,
) -> Optional[Notification]:
    """Get a notification by ID.

    Args:
        db: Database session
        notification_id: Notification ID
        user_id: User ID (for authorization)

    Returns:
        Notification or None
    """
    result = await db.execute(
        select(Notification).where(
            and_(
                Notification.id == notification_id,
                Notification.user_id == user_id,
            )
        )
    )
    return result.scalar_one_or_none()


async def mark_as_read(
    db: AsyncSession,
    notification_id: str,
    user_id: str,
) -> bool:
    """Mark a notification as read.

    Args:
        db: Database session
        notification_id: Notification ID
        user_id: User ID

    Returns:
        True if notification was found and updated
    """
    result = await db.execute(
        update(Notification)
        .where(
            and_(
                Notification.id == notification_id,
                Notification.user_id == user_id,
            )
        )
        .values(is_read=True)
    )
    await db.commit()
    return result.rowcount > 0


async def mark_all_as_read(
    db: AsyncSession,
    user_id: str,
) -> int:
    """Mark all notifications as read for a user.

    Args:
        db: Database session
        user_id: User ID

    Returns:
        Number of notifications updated
    """
    result = await db.execute(
        update(Notification)
        .where(
            and_(
                Notification.user_id == user_id,
                Notification.is_read == False,  # noqa: E712
            )
        )
        .values(is_read=True)
    )
    await db.commit()
    return result.rowcount


async def delete_notification(
    db: AsyncSession,
    notification_id: str,
    user_id: str,
) -> bool:
    """Delete a notification.

    Args:
        db: Database session
        notification_id: Notification ID
        user_id: User ID

    Returns:
        True if notification was found and deleted
    """
    result = await db.execute(
        delete(Notification).where(
            and_(
                Notification.id == notification_id,
                Notification.user_id == user_id,
            )
        )
    )
    await db.commit()
    return result.rowcount > 0


async def create_notification(
    db: AsyncSession,
    request: CreateNotificationRequest,
) -> Notification:
    """Create a new notification.

    Args:
        db: Database session
        request: Notification creation request

    Returns:
        Created notification
    """
    notification = Notification(
        id=str(uuid.uuid4()),
        user_id=request.user_id,
        type=request.type,
        title=request.title,
        body=request.body,
        image_url=request.image_url,
        action_type=request.action_type,
        action_data=json.dumps(request.action_data) if request.action_data else None,
        expires_at=request.expires_at,
    )

    db.add(notification)
    await db.commit()
    await db.refresh(notification)

    return notification


async def cleanup_expired_notifications(
    db: AsyncSession,
    user_id: Optional[str] = None,
) -> int:
    """Delete expired notifications.

    Args:
        db: Database session
        user_id: Optional user ID to limit cleanup

    Returns:
        Number of deleted notifications
    """
    query = delete(Notification).where(
        and_(
            Notification.expires_at.is_not(None),
            Notification.expires_at <= datetime.now(timezone.utc),
        )
    )

    if user_id:
        query = query.where(Notification.user_id == user_id)

    result = await db.execute(query)
    await db.commit()
    return result.rowcount


# region FCM Token Operations


async def register_fcm_token(
    db: AsyncSession,
    user_id: str,
    token: str,
    device_type: str = "android",
) -> FcmToken:
    """Register or update an FCM token.

    Args:
        db: Database session
        user_id: User ID
        token: FCM token
        device_type: Device type (android/ios)

    Returns:
        FCM token record
    """
    # Check if token already exists
    result = await db.execute(
        select(FcmToken).where(FcmToken.token == token)
    )
    existing = result.scalar_one_or_none()

    if existing:
        # Update existing token
        existing.user_id = user_id
        existing.device_type = device_type
        existing.is_active = True
        await db.commit()
        await db.refresh(existing)
        return existing

    # Create new token
    fcm_token = FcmToken(
        id=str(uuid.uuid4()),
        user_id=user_id,
        token=token,
        device_type=device_type,
        is_active=True,
    )
    db.add(fcm_token)
    await db.commit()
    await db.refresh(fcm_token)
    return fcm_token


async def unregister_fcm_token(
    db: AsyncSession,
    token: str,
) -> bool:
    """Unregister an FCM token.

    Args:
        db: Database session
        token: FCM token

    Returns:
        True if token was found and deactivated
    """
    result = await db.execute(
        update(FcmToken)
        .where(FcmToken.token == token)
        .values(is_active=False)
    )
    await db.commit()
    return result.rowcount > 0


async def get_user_fcm_tokens(
    db: AsyncSession,
    user_id: str,
) -> list[FcmToken]:
    """Get all active FCM tokens for a user.

    Args:
        db: Database session
        user_id: User ID

    Returns:
        List of active FCM tokens
    """
    result = await db.execute(
        select(FcmToken).where(
            and_(
                FcmToken.user_id == user_id,
                FcmToken.is_active == True,  # noqa: E712
            )
        )
    )
    return list(result.scalars().all())


# endregion


# region Helper Functions


def _to_response(notification: Notification) -> NotificationResponse:
    """Convert notification model to response schema."""
    action_data = None
    if notification.action_data:
        try:
            data = json.loads(notification.action_data)
            action_data = NotificationActionData(**data)
        except (json.JSONDecodeError, TypeError):
            pass

    return NotificationResponse(
        id=notification.id,
        type=notification.type,
        title=notification.title,
        body=notification.body,
        image_url=notification.image_url,
        action_type=notification.action_type,
        action_data=action_data,
        is_read=notification.is_read,
        created_at=notification.created_at.isoformat(),
        expires_at=notification.expires_at.isoformat() if notification.expires_at else None,
    )


# endregion
