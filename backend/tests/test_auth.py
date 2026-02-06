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
async def test_get_current_user_unauthorized(unauthenticated_client: AsyncClient):
    """Test get current user without auth token."""
    response = await unauthenticated_client.get("/api/v1/users/me")
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_get_current_user_invalid_token(unauthenticated_client: AsyncClient):
    """Test get current user with invalid token."""
    response = await unauthenticated_client.get(
        "/api/v1/users/me",
        headers={"Authorization": "Bearer invalid-token"},
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_refresh_token_success(client: AsyncClient):
    """Test refresh token endpoint returns new access token."""
    # First, authenticate to get a refresh token
    auth_response = await client.post(
        "/api/v1/auth/firebase",
        json={"firebase_token": "mock-token-for-testing"},
    )
    assert auth_response.status_code == 200
    auth_data = auth_response.json()
    refresh_token = auth_data["refresh_token"]

    # Use refresh token to get new access token
    response = await client.post(
        "/api/v1/auth/refresh",
        json={"refresh_token": refresh_token},
    )

    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"
    assert "expires_in" in data
    assert data["expires_in"] > 0


@pytest.mark.asyncio
async def test_refresh_token_invalid_token(client: AsyncClient):
    """Test refresh token endpoint with invalid token."""
    response = await client.post(
        "/api/v1/auth/refresh",
        json={"refresh_token": "invalid-refresh-token"},
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_refresh_token_with_access_token_fails(client: AsyncClient):
    """Test that using an access token as refresh token fails."""
    # First, authenticate to get tokens
    auth_response = await client.post(
        "/api/v1/auth/firebase",
        json={"firebase_token": "mock-token-for-testing"},
    )
    assert auth_response.status_code == 200
    auth_data = auth_response.json()
    access_token = auth_data["access_token"]

    # Try to use access token as refresh token - should fail
    response = await client.post(
        "/api/v1/auth/refresh",
        json={"refresh_token": access_token},
    )
    assert response.status_code == 401
