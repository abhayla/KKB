"""Notification service unit tests."""

import pytest
from datetime import datetime, timezone, timedelta
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.notification import FcmToken, Notification
from app.models.user import User
from app.schemas.notification import CreateNotificationRequest
from app.services import notification_service


async def create_test_user(db: AsyncSession) -> User:
    """Helper to create a test user."""
    user = User(
        id="test-user-id",
        firebase_uid="firebase-test-uid",
        email="test@example.com",
        name="Test User",
        is_onboarded=True,
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)
    return user


async def create_test_notification(
    db: AsyncSession,
    user_id: str,
    notification_type: str = "meal_plan_update",
    is_read: bool = False,
    expires_at: datetime | None = None,
) -> Notification:
    """Helper to create a test notification."""
    notification = Notification(
        user_id=user_id,
        type=notification_type,
        title=f"Test {notification_type}",
        body="Test notification body",
        is_read=is_read,
        expires_at=expires_at,
    )
    db.add(notification)
    await db.commit()
    await db.refresh(notification)
    return notification


@pytest.mark.asyncio
async def test_get_notifications_empty(db_session: AsyncSession):
    """Test getting notifications when none exist."""
    user = await create_test_user(db_session)

    result = await notification_service.get_notifications(db_session, user.id)

    assert result.notifications == []
    assert result.unread_count == 0
    assert result.total_count == 0


@pytest.mark.asyncio
async def test_get_notifications_with_data(db_session: AsyncSession):
    """Test getting notifications with existing data."""
    user = await create_test_user(db_session)

    # Create some notifications
    await create_test_notification(db_session, user.id, "festival_reminder", is_read=False)
    await create_test_notification(db_session, user.id, "meal_plan_update", is_read=False)
    await create_test_notification(db_session, user.id, "recipe_suggestion", is_read=True)

    result = await notification_service.get_notifications(db_session, user.id)

    assert len(result.notifications) == 3
    assert result.unread_count == 2
    assert result.total_count == 3


@pytest.mark.asyncio
async def test_get_notifications_excludes_expired(db_session: AsyncSession):
    """Test that expired notifications are excluded by default."""
    user = await create_test_user(db_session)

    # Create an expired notification
    past = datetime.now(timezone.utc) - timedelta(days=1)
    await create_test_notification(db_session, user.id, "shopping_reminder", expires_at=past)

    # Create a non-expired notification
    future = datetime.now(timezone.utc) + timedelta(days=1)
    await create_test_notification(db_session, user.id, "meal_plan_update", expires_at=future)

    result = await notification_service.get_notifications(db_session, user.id)

    assert len(result.notifications) == 1
    assert result.notifications[0].type == "meal_plan_update"


@pytest.mark.asyncio
async def test_get_notifications_includes_expired_when_requested(db_session: AsyncSession):
    """Test that expired notifications can be included."""
    user = await create_test_user(db_session)

    past = datetime.now(timezone.utc) - timedelta(days=1)
    await create_test_notification(db_session, user.id, "shopping_reminder", expires_at=past)

    result = await notification_service.get_notifications(db_session, user.id, include_expired=True)

    assert len(result.notifications) == 1


@pytest.mark.asyncio
async def test_get_notification_by_id(db_session: AsyncSession):
    """Test getting a notification by ID."""
    user = await create_test_user(db_session)
    notif = await create_test_notification(db_session, user.id)

    result = await notification_service.get_notification_by_id(db_session, notif.id, user.id)

    assert result is not None
    assert result.id == notif.id


@pytest.mark.asyncio
async def test_get_notification_by_id_wrong_user(db_session: AsyncSession):
    """Test that getting a notification with wrong user ID returns None."""
    user = await create_test_user(db_session)
    notif = await create_test_notification(db_session, user.id)

    result = await notification_service.get_notification_by_id(db_session, notif.id, "wrong-user")

    assert result is None


@pytest.mark.asyncio
async def test_mark_as_read(db_session: AsyncSession):
    """Test marking a notification as read."""
    user = await create_test_user(db_session)
    notif = await create_test_notification(db_session, user.id, is_read=False)

    result = await notification_service.mark_as_read(db_session, notif.id, user.id)

    assert result is True

    # Verify it's actually read
    updated = await notification_service.get_notification_by_id(db_session, notif.id, user.id)
    assert updated.is_read is True


@pytest.mark.asyncio
async def test_mark_as_read_nonexistent(db_session: AsyncSession):
    """Test marking a nonexistent notification as read."""
    result = await notification_service.mark_as_read(db_session, "nonexistent", "user")
    assert result is False


@pytest.mark.asyncio
async def test_mark_all_as_read(db_session: AsyncSession):
    """Test marking all notifications as read."""
    user = await create_test_user(db_session)

    await create_test_notification(db_session, user.id, is_read=False)
    await create_test_notification(db_session, user.id, is_read=False)
    await create_test_notification(db_session, user.id, is_read=True)

    count = await notification_service.mark_all_as_read(db_session, user.id)

    assert count == 2

    # Verify all are read
    result = await notification_service.get_notifications(db_session, user.id)
    assert result.unread_count == 0


@pytest.mark.asyncio
async def test_delete_notification(db_session: AsyncSession):
    """Test deleting a notification."""
    user = await create_test_user(db_session)
    notif = await create_test_notification(db_session, user.id)

    result = await notification_service.delete_notification(db_session, notif.id, user.id)

    assert result is True

    # Verify it's deleted
    deleted = await notification_service.get_notification_by_id(db_session, notif.id, user.id)
    assert deleted is None


@pytest.mark.asyncio
async def test_delete_notification_wrong_user(db_session: AsyncSession):
    """Test that deleting a notification with wrong user fails."""
    user = await create_test_user(db_session)
    notif = await create_test_notification(db_session, user.id)

    result = await notification_service.delete_notification(db_session, notif.id, "wrong-user")

    assert result is False


@pytest.mark.asyncio
async def test_create_notification(db_session: AsyncSession):
    """Test creating a notification."""
    user = await create_test_user(db_session)

    request = CreateNotificationRequest(
        user_id=user.id,
        type="streak_milestone",
        title="7-day streak!",
        body="Congratulations!",
        action_type="open_stats",
        action_data={"streak_count": 7},
    )

    notif = await notification_service.create_notification(db_session, request)

    assert notif.id is not None
    assert notif.type == "streak_milestone"
    assert notif.title == "7-day streak!"


@pytest.mark.asyncio
async def test_cleanup_expired_notifications(db_session: AsyncSession):
    """Test cleaning up expired notifications."""
    user = await create_test_user(db_session)

    past = datetime.now(timezone.utc) - timedelta(days=1)
    await create_test_notification(db_session, user.id, expires_at=past)
    await create_test_notification(db_session, user.id, expires_at=past)
    await create_test_notification(db_session, user.id)  # No expiry

    count = await notification_service.cleanup_expired_notifications(db_session)

    assert count == 2

    # Verify only non-expired remains
    result = await notification_service.get_notifications(db_session, user.id, include_expired=True)
    assert len(result.notifications) == 1


# region FCM Token Tests


@pytest.mark.asyncio
async def test_register_fcm_token(db_session: AsyncSession):
    """Test registering an FCM token."""
    user = await create_test_user(db_session)

    token = await notification_service.register_fcm_token(
        db_session, user.id, "test-fcm-token-123"
    )

    assert token.id is not None
    assert token.token == "test-fcm-token-123"
    assert token.user_id == user.id
    assert token.is_active is True


@pytest.mark.asyncio
async def test_register_fcm_token_updates_existing(db_session: AsyncSession):
    """Test that registering an existing token updates it."""
    user = await create_test_user(db_session)

    # Register initial token
    await notification_service.register_fcm_token(db_session, user.id, "existing-token")

    # Re-register with different user (shouldn't happen but test it)
    token = await notification_service.register_fcm_token(
        db_session, "different-user", "existing-token"
    )

    assert token.token == "existing-token"
    assert token.user_id == "different-user"


@pytest.mark.asyncio
async def test_unregister_fcm_token(db_session: AsyncSession):
    """Test unregistering an FCM token."""
    user = await create_test_user(db_session)

    await notification_service.register_fcm_token(db_session, user.id, "token-to-remove")

    result = await notification_service.unregister_fcm_token(db_session, "token-to-remove")

    assert result is True

    # Verify it's deactivated
    tokens = await notification_service.get_user_fcm_tokens(db_session, user.id)
    assert len(tokens) == 0  # Inactive tokens not returned


@pytest.mark.asyncio
async def test_unregister_nonexistent_token(db_session: AsyncSession):
    """Test unregistering a nonexistent token."""
    result = await notification_service.unregister_fcm_token(db_session, "nonexistent")
    assert result is False


@pytest.mark.asyncio
async def test_get_user_fcm_tokens(db_session: AsyncSession):
    """Test getting all FCM tokens for a user."""
    user = await create_test_user(db_session)

    await notification_service.register_fcm_token(db_session, user.id, "token-1")
    await notification_service.register_fcm_token(db_session, user.id, "token-2")

    tokens = await notification_service.get_user_fcm_tokens(db_session, user.id)

    assert len(tokens) == 2


@pytest.mark.asyncio
async def test_get_user_fcm_tokens_excludes_inactive(db_session: AsyncSession):
    """Test that inactive tokens are not returned."""
    user = await create_test_user(db_session)

    await notification_service.register_fcm_token(db_session, user.id, "active-token")
    await notification_service.register_fcm_token(db_session, user.id, "inactive-token")
    await notification_service.unregister_fcm_token(db_session, "inactive-token")

    tokens = await notification_service.get_user_fcm_tokens(db_session, user.id)

    assert len(tokens) == 1
    assert tokens[0].token == "active-token"


# endregion
