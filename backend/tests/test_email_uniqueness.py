"""Tests for email uniqueness enforcement.

Verifies:
- Email normalization (lowercase, trimmed) on user creation
- 409 CONFLICT when a new Firebase UID tries to register with an existing email
- Case-insensitive duplicate detection
- NULL emails are allowed for multiple users
- Re-auth with same UID does not trigger conflict
- fake-firebase-token returns e2e-test@rasoiai.test
- 409 response body contains descriptive detail
"""

import pytest
from uuid import uuid4
from unittest.mock import patch

import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.database import get_db
from app.main import app
from app.models.user import User


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def email_client(db_session: AsyncSession) -> AsyncClient:
    """Create a test client with DB override but NO auth override.

    Auth endpoint doesn't require auth — it creates/returns a user.
    We mock verify_firebase_token to control UID/email.
    """
    async def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db

    # Patch UserRepository to use test session
    from tests.conftest import _test_session_maker

    def mock_session_maker():
        return _test_session_maker()

    with patch('app.repositories.user_repository.async_session_maker', mock_session_maker):
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as ac:
            yield ac

    app.dependency_overrides.clear()


# ==================== Tests ====================


@pytest.mark.asyncio
async def test_new_user_email_stored_lowercase(email_client: AsyncClient):
    """Email 'Test@Example.COM' should be stored as 'test@example.com'."""
    mock_firebase = {
        "uid": "uid-lowercase-test",
        "email": "Test@Example.COM",
        "name": "Test User",
        "picture": None,
    }

    with patch("app.services.auth_service.verify_firebase_token", return_value=mock_firebase):
        response = await email_client.post(
            "/api/v1/auth/firebase",
            json={"firebase_token": "any-token"},
        )

    assert response.status_code == 200
    data = response.json()
    assert data["user"]["email"] == "test@example.com"


@pytest.mark.asyncio
async def test_duplicate_email_different_firebase_uid_returns_409(
    db_session: AsyncSession, email_client: AsyncClient
):
    """Same email + different UID should return 409 CONFLICT."""
    # Pre-create a user with this email
    user = User(
        id=str(uuid4()),
        firebase_uid="original-uid-123",
        email="duplicate@test.com",
        name="Original User",
        is_onboarded=False,
        is_active=True,
    )
    db_session.add(user)
    await db_session.commit()

    # Try to register with different UID but same email
    mock_firebase = {
        "uid": "different-uid-456",
        "email": "duplicate@test.com",
        "name": "New User",
        "picture": None,
    }

    with patch("app.services.auth_service.verify_firebase_token", return_value=mock_firebase):
        response = await email_client.post(
            "/api/v1/auth/firebase",
            json={"firebase_token": "any-token"},
        )

    assert response.status_code == 409


@pytest.mark.asyncio
async def test_duplicate_email_case_insensitive_returns_409(
    db_session: AsyncSession, email_client: AsyncClient
):
    """'USER@TEST.COM' vs 'user@test.com' should be treated as duplicate."""
    # Pre-create a user with lowercase email
    user = User(
        id=str(uuid4()),
        firebase_uid="case-uid-111",
        email="user@test.com",
        name="Case User",
        is_onboarded=False,
        is_active=True,
    )
    db_session.add(user)
    await db_session.commit()

    # Try to register with uppercase variant
    mock_firebase = {
        "uid": "case-uid-222",
        "email": "USER@TEST.COM",
        "name": "New Case User",
        "picture": None,
    }

    with patch("app.services.auth_service.verify_firebase_token", return_value=mock_firebase):
        response = await email_client.post(
            "/api/v1/auth/firebase",
            json={"firebase_token": "any-token"},
        )

    assert response.status_code == 409


@pytest.mark.asyncio
async def test_null_email_allows_multiple_users(email_client: AsyncClient):
    """Two users with NULL email should both succeed."""
    mock1 = {
        "uid": "null-email-uid-1",
        "email": None,
        "name": "User One",
        "picture": None,
    }
    mock2 = {
        "uid": "null-email-uid-2",
        "email": None,
        "name": "User Two",
        "picture": None,
    }

    with patch("app.services.auth_service.verify_firebase_token", return_value=mock1):
        resp1 = await email_client.post(
            "/api/v1/auth/firebase",
            json={"firebase_token": "token1"},
        )
    assert resp1.status_code == 200

    with patch("app.services.auth_service.verify_firebase_token", return_value=mock2):
        resp2 = await email_client.post(
            "/api/v1/auth/firebase",
            json={"firebase_token": "token2"},
        )
    assert resp2.status_code == 200


@pytest.mark.asyncio
async def test_existing_user_same_firebase_uid_no_conflict(
    db_session: AsyncSession, email_client: AsyncClient
):
    """Re-auth with the same UID should return 200, not 409."""
    # Pre-create user
    user = User(
        id=str(uuid4()),
        firebase_uid="reauth-uid-same",
        email="reauth@test.com",
        name="Reauth User",
        is_onboarded=False,
        is_active=True,
    )
    db_session.add(user)
    await db_session.commit()

    # Re-authenticate with the same UID (should find existing user)
    mock_firebase = {
        "uid": "reauth-uid-same",
        "email": "reauth@test.com",
        "name": "Reauth User",
        "picture": None,
    }

    with patch("app.services.auth_service.verify_firebase_token", return_value=mock_firebase):
        response = await email_client.post(
            "/api/v1/auth/firebase",
            json={"firebase_token": "any-token"},
        )

    assert response.status_code == 200


@pytest.mark.asyncio
async def test_fake_firebase_token_uses_test_email(email_client: AsyncClient):
    """fake-firebase-token should return e2e-test@rasoiai.test email."""
    # Call with the actual fake-firebase-token (no mock needed - uses real code path)
    response = await email_client.post(
        "/api/v1/auth/firebase",
        json={"firebase_token": "fake-firebase-token"},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["user"]["email"] == "e2e-test@rasoiai.test"


@pytest.mark.asyncio
async def test_409_response_has_descriptive_detail(
    db_session: AsyncSession, email_client: AsyncClient
):
    """409 response body should contain 'already exists'."""
    # Pre-create user
    user = User(
        id=str(uuid4()),
        firebase_uid="detail-uid-aaa",
        email="detail@test.com",
        name="Detail User",
        is_onboarded=False,
        is_active=True,
    )
    db_session.add(user)
    await db_session.commit()

    mock_firebase = {
        "uid": "detail-uid-bbb",
        "email": "detail@test.com",
        "name": "Another User",
        "picture": None,
    }

    with patch("app.services.auth_service.verify_firebase_token", return_value=mock_firebase):
        response = await email_client.post(
            "/api/v1/auth/firebase",
            json={"firebase_token": "any-token"},
        )

    assert response.status_code == 409
    assert "already exists" in response.json()["detail"]
