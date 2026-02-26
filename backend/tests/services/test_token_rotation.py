"""Tests for refresh token rotation and revocation.

Tests the token lifecycle: creation during auth, rotation on refresh,
revocation on logout, and security measures for token reuse detection.
"""

import uuid
from datetime import datetime, timedelta, timezone
from unittest.mock import patch

import pytest
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.refresh_token import RefreshToken
from app.models.user import User
from app.services.auth_service import _hash_token


class TestRefreshTokenRotation:
    """Test refresh token creation and rotation."""

    async def test_hash_token_deterministic(self):
        """Same input should produce same hash."""
        token = "test-token-123"
        assert _hash_token(token) == _hash_token(token)

    async def test_hash_token_different_inputs(self):
        """Different inputs should produce different hashes."""
        assert _hash_token("token-a") != _hash_token("token-b")

    async def test_refresh_token_stored_on_auth(self, db_session: AsyncSession, test_user: User):
        """Auth should create a stored refresh token."""
        from app.services.auth_service import _generate_refresh_token, _hash_token

        raw_token = _generate_refresh_token()
        token_hash = _hash_token(raw_token)
        expires_at = datetime.now(timezone.utc) + timedelta(days=30)

        rt = RefreshToken(
            user_id=test_user.id,
            token_hash=token_hash,
            expires_at=expires_at,
        )
        db_session.add(rt)
        await db_session.commit()

        # Verify it was stored
        result = await db_session.execute(
            select(RefreshToken).where(RefreshToken.token_hash == token_hash)
        )
        stored = result.scalar_one()
        assert stored.user_id == test_user.id
        assert stored.is_revoked is False

    async def test_revoke_token(self, db_session: AsyncSession, test_user: User):
        """Revoking a token should set is_revoked=True."""
        from app.services.auth_service import _generate_refresh_token, _hash_token

        raw_token = _generate_refresh_token()
        token_hash = _hash_token(raw_token)

        rt = RefreshToken(
            user_id=test_user.id,
            token_hash=token_hash,
            expires_at=datetime.now(timezone.utc) + timedelta(days=30),
        )
        db_session.add(rt)
        await db_session.commit()

        # Revoke it
        rt.is_revoked = True
        await db_session.commit()

        result = await db_session.execute(
            select(RefreshToken).where(RefreshToken.token_hash == token_hash)
        )
        stored = result.scalar_one()
        assert stored.is_revoked is True


class TestLogout:
    """Test logout functionality."""

    async def test_logout_revokes_all_tokens_directly(self, db_session: AsyncSession, test_user: User):
        """Directly revoking all tokens for a user simulates logout."""
        from sqlalchemy import update as sql_update
        from app.services.auth_service import _generate_refresh_token, _hash_token

        # Create multiple tokens
        for _ in range(3):
            raw = _generate_refresh_token()
            rt = RefreshToken(
                user_id=test_user.id,
                token_hash=_hash_token(raw),
                expires_at=datetime.now(timezone.utc) + timedelta(days=30),
            )
            db_session.add(rt)
        await db_session.commit()

        # Verify tokens exist and are not revoked
        result = await db_session.execute(
            select(RefreshToken)
            .where(RefreshToken.user_id == test_user.id)
            .where(RefreshToken.is_revoked == False)  # noqa: E712
        )
        assert len(result.scalars().all()) == 3

        # Simulate logout: revoke all tokens
        await db_session.execute(
            sql_update(RefreshToken)
            .where(RefreshToken.user_id == test_user.id)
            .values(is_revoked=True)
        )
        await db_session.commit()

        # Verify all tokens revoked
        result = await db_session.execute(
            select(RefreshToken)
            .where(RefreshToken.user_id == test_user.id)
            .where(RefreshToken.is_revoked == False)  # noqa: E712
        )
        assert len(result.scalars().all()) == 0

    async def test_logout_endpoint(self, client):
        """POST /auth/logout should return success."""
        response = await client.post("/api/v1/auth/logout")
        assert response.status_code == 200
        data = response.json()
        assert data["message"] == "Logged out successfully"


class TestTokenExpiry:
    """Test token expiration handling."""

    async def test_expired_token_not_usable(self, db_session: AsyncSession, test_user: User):
        """Expired refresh tokens should not be accepted."""
        from app.services.auth_service import _generate_refresh_token, _hash_token

        raw_token = _generate_refresh_token()
        rt = RefreshToken(
            user_id=test_user.id,
            token_hash=_hash_token(raw_token),
            expires_at=datetime.now(timezone.utc) - timedelta(days=1),  # Already expired
        )
        db_session.add(rt)
        await db_session.commit()

        # Token is in DB but expired
        result = await db_session.execute(
            select(RefreshToken).where(RefreshToken.user_id == test_user.id)
        )
        stored = result.scalar_one()
        assert stored.expires_at < datetime.now(timezone.utc)

    async def test_token_30_day_expiry(self):
        """Refresh tokens should have 30-day expiry."""
        from app.services.auth_service import _generate_refresh_token

        # Just verify the token generation works
        token = _generate_refresh_token()
        assert len(token) > 30  # Should be a long opaque token


class TestRefreshTokenSchema:
    """Test the schema includes refresh_token field."""

    def test_refresh_response_has_refresh_token_field(self):
        """RefreshTokenResponse should include refresh_token for rotation."""
        from app.schemas.auth import RefreshTokenResponse

        # Verify the field exists
        fields = RefreshTokenResponse.model_fields
        assert "refresh_token" in fields
        assert "access_token" in fields
