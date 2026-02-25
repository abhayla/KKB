"""Integration test fixtures — cross-cutting tests (auth flow, email merge, etc.).

These tests need DB + session_maker patches but NO auth override,
because they test the auth endpoints themselves.
"""

from contextlib import asynccontextmanager
from typing import AsyncGenerator
from unittest.mock import patch

import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.database import get_db
from app.main import app
from app.models.user import User, UserPreferences

from tests.factories import make_user, make_preferences


@asynccontextmanager
async def make_firebase_mock_client(
    db_session: AsyncSession,
) -> AsyncGenerator[AsyncClient, None]:
    """Create a test client with DB override + session_maker patches but NO auth override.

    Used for auth-flow tests where the endpoint itself handles authentication
    (e.g., POST /auth/firebase). We mock verify_firebase_token per-test instead.
    """
    from tests.conftest import _test_session_maker

    async def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db

    def mock_session_maker():
        return _test_session_maker()

    with patch('app.repositories.user_repository.async_session_maker', mock_session_maker), \
         patch('app.services.auth_service.async_session_maker', mock_session_maker):
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as ac:
            yield ac

    app.dependency_overrides.clear()


@pytest_asyncio.fixture
async def sharma_user(db_session: AsyncSession) -> User:
    """Create the Sharma family test user (non-vegetarian, family_size=3)."""
    user = make_user(name="Sharma Family")
    db_session.add(user)

    prefs = make_preferences(
        user.id,
        dietary_type="non_vegetarian",
        family_size=3,
    )
    db_session.add(prefs)

    await db_session.commit()
    await db_session.refresh(user)
    return user
