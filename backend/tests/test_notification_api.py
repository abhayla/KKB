"""Notification API endpoint tests."""

import pytest
from datetime import datetime, timezone, timedelta
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.notification import Notification
from app.models.user import User


async def create_test_user(db: AsyncSession) -> User:
    """Helper to create a test user."""
    user = User(
        id="api-test-user-id",
        firebase_uid="firebase-api-test-uid",
        email="api-test@example.com",
        name="API Test User",
        is_onboarded=True,
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)
    return user


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


async def get_auth_token(client: AsyncClient) -> str:
    """Get a valid auth token for testing."""
    response = await client.post(
        "/api/v1/auth/firebase",
        json={"firebase_token": "mock-token-for-testing"},
    )
    return response.json()["access_token"]


@pytest.mark.asyncio
async def test_get_notifications_unauthorized(client: AsyncClient):
    """Test getting notifications without auth."""
    response = await client.get("/api/v1/notifications")
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_get_notifications_empty(client: AsyncClient, db_session: AsyncSession):
    """Test getting notifications when none exist."""
    token = await get_auth_token(client)

    response = await client.get(
        "/api/v1/notifications",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["notifications"] == []
    assert data["unread_count"] == 0
    assert data["total_count"] == 0


@pytest.mark.asyncio
async def test_mark_notification_as_read_unauthorized(client: AsyncClient):
    """Test marking notification as read without auth."""
    response = await client.put("/api/v1/notifications/some-id/read")
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_mark_notification_as_read_not_found(client: AsyncClient):
    """Test marking nonexistent notification as read."""
    token = await get_auth_token(client)

    response = await client.put(
        "/api/v1/notifications/nonexistent-id/read",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 404


@pytest.mark.asyncio
async def test_mark_all_notifications_as_read(client: AsyncClient):
    """Test marking all notifications as read."""
    token = await get_auth_token(client)

    response = await client.put(
        "/api/v1/notifications/read-all",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True


@pytest.mark.asyncio
async def test_delete_notification_unauthorized(client: AsyncClient):
    """Test deleting notification without auth."""
    response = await client.delete("/api/v1/notifications/some-id")
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_delete_notification_not_found(client: AsyncClient):
    """Test deleting nonexistent notification."""
    token = await get_auth_token(client)

    response = await client.delete(
        "/api/v1/notifications/nonexistent-id",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 404


@pytest.mark.asyncio
async def test_register_fcm_token(client: AsyncClient):
    """Test registering an FCM token."""
    token = await get_auth_token(client)

    response = await client.post(
        "/api/v1/notifications/fcm-token",
        headers={"Authorization": f"Bearer {token}"},
        json={"fcm_token": "test-fcm-token-12345"},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True


@pytest.mark.asyncio
async def test_register_fcm_token_unauthorized(client: AsyncClient):
    """Test registering FCM token without auth."""
    response = await client.post(
        "/api/v1/notifications/fcm-token",
        json={"fcm_token": "test-token"},
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_unregister_fcm_token(client: AsyncClient):
    """Test unregistering an FCM token."""
    token = await get_auth_token(client)

    # First register a token
    await client.post(
        "/api/v1/notifications/fcm-token",
        headers={"Authorization": f"Bearer {token}"},
        json={"fcm_token": "token-to-unregister"},
    )

    # Then unregister it (using query parameter)
    response = await client.delete(
        "/api/v1/notifications/fcm-token?fcm_token=token-to-unregister",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
