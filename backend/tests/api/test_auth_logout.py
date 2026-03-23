"""Tests for POST /api/v1/auth/logout endpoint."""

from datetime import datetime, timedelta, timezone

import pytest
from httpx import AsyncClient
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.refresh_token import RefreshToken
from app.models.user import User
from app.services.auth_service import _generate_refresh_token, _hash_token


async def test_logout_success(client: AsyncClient):
    """Authenticated user can logout and receives 200."""
    response = await client.post("/api/v1/auth/logout")
    assert response.status_code == 200


async def test_logout_unauthenticated(unauthenticated_client: AsyncClient):
    """Unauthenticated request to logout returns 401."""
    response = await unauthenticated_client.post("/api/v1/auth/logout")
    assert response.status_code == 401


async def test_logout_response_structure(client: AsyncClient):
    """Logout response contains the expected message field."""
    response = await client.post("/api/v1/auth/logout")
    assert response.status_code == 200
    data = response.json()
    assert "message" in data
    assert data["message"] == "Logged out successfully"


async def test_logout_revokes_refresh_tokens(
    client: AsyncClient, db_session: AsyncSession, test_user: User
):
    """Logout revokes all refresh tokens for the user."""
    # Create 3 active refresh tokens for the test user
    for _ in range(3):
        raw = _generate_refresh_token()
        rt = RefreshToken(
            user_id=test_user.id,
            token_hash=_hash_token(raw),
            expires_at=datetime.now(timezone.utc) + timedelta(days=30),
        )
        db_session.add(rt)
    await db_session.commit()

    # Verify tokens exist and are active
    result = await db_session.execute(
        select(RefreshToken)
        .where(RefreshToken.user_id == test_user.id)
        .where(RefreshToken.is_revoked == False)  # noqa: E712
    )
    active_tokens = result.scalars().all()
    assert len(active_tokens) == 3

    # Logout
    response = await client.post("/api/v1/auth/logout")
    assert response.status_code == 200

    # Verify all tokens are now revoked
    result = await db_session.execute(
        select(RefreshToken)
        .where(RefreshToken.user_id == test_user.id)
        .where(RefreshToken.is_revoked == False)  # noqa: E712
    )
    remaining_active = result.scalars().all()
    assert len(remaining_active) == 0


async def test_logout_idempotent(client: AsyncClient):
    """Calling logout twice should succeed both times."""
    response1 = await client.post("/api/v1/auth/logout")
    assert response1.status_code == 200

    response2 = await client.post("/api/v1/auth/logout")
    assert response2.status_code == 200
    assert response2.json()["message"] == "Logged out successfully"


async def test_logout_no_existing_tokens(
    client: AsyncClient, db_session: AsyncSession, test_user: User
):
    """Logout succeeds even when the user has no refresh tokens."""
    # Verify no tokens exist for this user
    result = await db_session.execute(
        select(RefreshToken).where(RefreshToken.user_id == test_user.id)
    )
    assert len(result.scalars().all()) == 0

    # Logout should still succeed
    response = await client.post("/api/v1/auth/logout")
    assert response.status_code == 200
    assert response.json()["message"] == "Logged out successfully"
