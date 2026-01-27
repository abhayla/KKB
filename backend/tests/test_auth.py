"""Authentication endpoint tests."""

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_firebase_auth_mock(client: AsyncClient):
    """Test Firebase auth endpoint with mock token (development mode)."""
    response = await client.post(
        "/api/v1/auth/firebase",
        json={"firebase_token": "mock-token-for-testing"},
    )

    # In debug mode, should return mock user
    assert response.status_code == 200
    data = response.json()

    assert "access_token" in data
    assert "refresh_token" in data
    assert data["token_type"] == "bearer"
    assert "user" in data

    user = data["user"]
    assert "id" in user
    assert "email" in user


@pytest.mark.asyncio
async def test_get_current_user_unauthorized(client: AsyncClient):
    """Test get current user without auth token."""
    response = await client.get("/api/v1/users/me")
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_get_current_user_invalid_token(client: AsyncClient):
    """Test get current user with invalid token."""
    response = await client.get(
        "/api/v1/users/me",
        headers={"Authorization": "Bearer invalid-token"},
    )
    assert response.status_code == 401
