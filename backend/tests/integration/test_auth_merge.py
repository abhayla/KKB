"""Tests for Firebase UID merge on same email.

When a user signs in with a new Firebase UID but an email that already exists,
the backend should merge (update the firebase_uid) rather than returning 409.

This is the standard OAuth account-merge pattern: same person, different device/provider.

Verifies:
- Merge returns 200 with JWT (not 409)
- Merge preserves user ID, preferences, and onboarded status
- Merge updates firebase_uid in database
- New user creation still works normally (no regression)
- Same firebase_uid re-auth still works (no regression)
"""

import pytest
from uuid import uuid4
from unittest.mock import patch

from sqlalchemy.ext.asyncio import AsyncSession

from tests.integration.conftest import make_firebase_mock_client
from tests.factories import make_user, make_preferences


# ==================== Tests ====================


@pytest.mark.asyncio
async def test_merge_firebase_uid_on_same_email(db_session: AsyncSession):
    """User exists with UID-A, sign in with UID-B + same email -> merges, returns JWT."""
    user = make_user(firebase_uid="original-uid-AAA", email="merge@test.com", name="Merge User", is_onboarded=False)
    db_session.add(user)
    await db_session.commit()

    mock_firebase = {
        "uid": "new-uid-BBB",
        "email": "merge@test.com",
        "name": "Merge User",
        "picture": None,
    }

    async with make_firebase_mock_client(db_session) as client:
        with patch("app.services.auth_service.verify_firebase_token", return_value=mock_firebase):
            response = await client.post(
                "/api/v1/auth/firebase",
                json={"firebase_token": "any-token"},
            )

    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["user"]["email"] == "merge@test.com"


@pytest.mark.asyncio
async def test_merge_preserves_user_data(db_session: AsyncSession):
    """After merge, user ID, preferences, and onboarded status are preserved."""
    user_id = str(uuid4())
    user = make_user(id=user_id, firebase_uid="preserve-uid-AAA", email="preserve@test.com", name="Preserve User", is_onboarded=True)
    db_session.add(user)

    prefs = make_preferences(user_id, family_size=4, spice_level="hot")
    db_session.add(prefs)
    await db_session.commit()

    mock_firebase = {
        "uid": "preserve-uid-BBB",
        "email": "preserve@test.com",
        "name": "Preserve User",
        "picture": None,
    }

    async with make_firebase_mock_client(db_session) as client:
        with patch("app.services.auth_service.verify_firebase_token", return_value=mock_firebase):
            response = await client.post(
                "/api/v1/auth/firebase",
                json={"firebase_token": "any-token"},
            )

    assert response.status_code == 200
    data = response.json()
    assert data["user"]["id"] == user_id
    assert data["user"]["is_onboarded"] is True
    assert data["user"]["preferences"] is not None
    assert data["user"]["preferences"]["household_size"] == 4
    assert data["user"]["preferences"]["spice_level"] == "hot"


@pytest.mark.asyncio
async def test_merge_updates_firebase_uid_in_db(db_session: AsyncSession):
    """After merge, firebase_uid column reflects the new UID."""
    user_id = str(uuid4())
    user = make_user(id=user_id, firebase_uid="db-uid-OLD", email="dbcheck@test.com", name="DB Check User", is_onboarded=False)
    db_session.add(user)
    await db_session.commit()

    mock_firebase = {
        "uid": "db-uid-NEW",
        "email": "dbcheck@test.com",
        "name": "DB Check User",
        "picture": None,
    }

    async with make_firebase_mock_client(db_session) as client:
        with patch("app.services.auth_service.verify_firebase_token", return_value=mock_firebase):
            response = await client.post(
                "/api/v1/auth/firebase",
                json={"firebase_token": "any-token"},
            )

        assert response.status_code == 200

        # Verify the UID was updated by re-authenticating with new UID
        mock_firebase_reauth = {
            "uid": "db-uid-NEW",
            "email": "dbcheck@test.com",
            "name": "DB Check User",
            "picture": None,
        }

        with patch("app.services.auth_service.verify_firebase_token", return_value=mock_firebase_reauth):
            reauth_response = await client.post(
                "/api/v1/auth/firebase",
                json={"firebase_token": "any-token"},
            )

    assert reauth_response.status_code == 200
    reauth_data = reauth_response.json()
    assert reauth_data["user"]["id"] == user_id


@pytest.mark.asyncio
async def test_new_user_still_creates_normally(db_session: AsyncSession):
    """New email + new UID -> creates user (no regression)."""
    mock_firebase = {
        "uid": "brand-new-uid",
        "email": "brandnew@test.com",
        "name": "Brand New User",
        "picture": None,
    }

    async with make_firebase_mock_client(db_session) as client:
        with patch("app.services.auth_service.verify_firebase_token", return_value=mock_firebase):
            response = await client.post(
                "/api/v1/auth/firebase",
                json={"firebase_token": "any-token"},
            )

    assert response.status_code == 200
    data = response.json()
    assert data["user"]["email"] == "brandnew@test.com"
    assert "access_token" in data


@pytest.mark.asyncio
async def test_same_firebase_uid_returns_existing(db_session: AsyncSession):
    """Same UID -> returns existing user (no regression)."""
    user_id = str(uuid4())
    user = make_user(id=user_id, firebase_uid="same-uid-reauth", email="sameuid@test.com", name="Same UID User", is_onboarded=True)
    db_session.add(user)
    await db_session.commit()

    mock_firebase = {
        "uid": "same-uid-reauth",
        "email": "sameuid@test.com",
        "name": "Same UID User",
        "picture": None,
    }

    async with make_firebase_mock_client(db_session) as client:
        with patch("app.services.auth_service.verify_firebase_token", return_value=mock_firebase):
            response = await client.post(
                "/api/v1/auth/firebase",
                json={"firebase_token": "any-token"},
            )

    assert response.status_code == 200
    data = response.json()
    assert data["user"]["id"] == user_id
    assert data["user"]["is_onboarded"] is True
