"""Notification API endpoint tests."""

import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.notification import Notification
from app.models.user import User


async def create_test_notification(
    db: AsyncSession,
    user_id: str,
    notification_id: str | None = None,
    notification_type: str = "meal_plan_update",
    is_read: bool = False,
) -> Notification:
    """Helper to create a test notification."""
    notification = Notification(
        id=notification_id or "test-notification-id",
        user_id=user_id,
        type=notification_type,
        title=f"Test {notification_type}",
        body="Test notification body",
        is_read=is_read,
    )
    db.add(notification)
    await db.commit()
    await db.refresh(notification)
    return notification


@pytest.mark.asyncio
async def test_get_notifications_unauthorized(client: AsyncClient):
    """Test getting notifications without auth."""
    # Override the dependency to not return a user
    from app.main import app
    from app.api.deps import get_current_user
    from app.core.exceptions import AuthenticationError

    async def no_auth():
        raise AuthenticationError("Missing authorization header")

    app.dependency_overrides[get_current_user] = no_auth

    response = await client.get("/api/v1/notifications")
    assert response.status_code == 401

    # Restore
    app.dependency_overrides.pop(get_current_user, None)


@pytest.mark.asyncio
async def test_get_notifications_empty(authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User):
    """Test getting notifications when none exist."""
    response = await authenticated_client.get("/api/v1/notifications")

    assert response.status_code == 200
    data = response.json()
    assert data["notifications"] == []
    assert data["unread_count"] == 0
    assert data["total_count"] == 0


@pytest.mark.asyncio
async def test_get_notifications_with_data(authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User):
    """Test getting notifications when data exists."""
    # Create test notification
    await create_test_notification(db_session, test_user.id)

    response = await authenticated_client.get("/api/v1/notifications")

    assert response.status_code == 200
    data = response.json()
    assert len(data["notifications"]) == 1
    assert data["unread_count"] == 1
    assert data["total_count"] == 1


@pytest.mark.asyncio
async def test_mark_notification_as_read_unauthorized(client: AsyncClient):
    """Test marking notification as read without auth."""
    from app.main import app
    from app.api.deps import get_current_user
    from app.core.exceptions import AuthenticationError

    async def no_auth():
        raise AuthenticationError("Missing authorization header")

    app.dependency_overrides[get_current_user] = no_auth

    response = await client.put("/api/v1/notifications/some-id/read")
    assert response.status_code == 401

    app.dependency_overrides.pop(get_current_user, None)


@pytest.mark.asyncio
async def test_mark_notification_as_read_not_found(authenticated_client: AsyncClient):
    """Test marking nonexistent notification as read."""
    response = await authenticated_client.put("/api/v1/notifications/nonexistent-id/read")
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_mark_all_notifications_as_read(authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User):
    """Test marking all notifications as read."""
    # Create unread notifications
    await create_test_notification(db_session, test_user.id, "notif-1")
    await create_test_notification(db_session, test_user.id, "notif-2")

    response = await authenticated_client.put("/api/v1/notifications/read-all")

    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True


@pytest.mark.asyncio
async def test_delete_notification_unauthorized(client: AsyncClient):
    """Test deleting notification without auth."""
    from app.main import app
    from app.api.deps import get_current_user
    from app.core.exceptions import AuthenticationError

    async def no_auth():
        raise AuthenticationError("Missing authorization header")

    app.dependency_overrides[get_current_user] = no_auth

    response = await client.delete("/api/v1/notifications/some-id")
    assert response.status_code == 401

    app.dependency_overrides.pop(get_current_user, None)


@pytest.mark.asyncio
async def test_delete_notification_not_found(authenticated_client: AsyncClient):
    """Test deleting nonexistent notification."""
    response = await authenticated_client.delete("/api/v1/notifications/nonexistent-id")
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_register_fcm_token(authenticated_client: AsyncClient):
    """Test registering an FCM token."""
    response = await authenticated_client.post(
        "/api/v1/notifications/fcm-token",
        json={"fcm_token": "test-fcm-token-12345"},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True


@pytest.mark.asyncio
async def test_register_fcm_token_unauthorized(client: AsyncClient):
    """Test registering FCM token without auth."""
    from app.main import app
    from app.api.deps import get_current_user
    from app.core.exceptions import AuthenticationError

    async def no_auth():
        raise AuthenticationError("Missing authorization header")

    app.dependency_overrides[get_current_user] = no_auth

    response = await client.post(
        "/api/v1/notifications/fcm-token",
        json={"fcm_token": "test-token"},
    )
    assert response.status_code == 401

    app.dependency_overrides.pop(get_current_user, None)


@pytest.mark.asyncio
async def test_unregister_fcm_token(authenticated_client: AsyncClient):
    """Test unregistering an FCM token."""
    # First register a token
    await authenticated_client.post(
        "/api/v1/notifications/fcm-token",
        json={"fcm_token": "token-to-unregister"},
    )

    # Then unregister it (using query parameter)
    response = await authenticated_client.delete(
        "/api/v1/notifications/fcm-token?fcm_token=token-to-unregister",
    )

    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
