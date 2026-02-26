"""API test fixtures — eliminates auth-override boilerplate.

The make_api_client() async context manager encapsulates all the setup
needed for an authenticated API test client:
  - get_db override → test db_session
  - get_current_user override → provided user
  - session_maker patches for UserRepository and AuthService
  - AsyncClient creation and cleanup
  - dependency_overrides.clear() on exit

Usage in test files (as a fixture):

    @pytest_asyncio.fixture
    async def client(db_session, test_user):
        async with make_api_client(db_session, test_user) as c:
            yield c

Or inline in a test:

    async def test_something(db_session, test_user):
        async with make_api_client(db_session, test_user) as client:
            resp = await client.get("/api/v1/health")
"""

from contextlib import asynccontextmanager
from typing import AsyncGenerator, Optional
from unittest.mock import patch

import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.database import get_db
from app.main import app
from app.models.user import User


@asynccontextmanager
async def make_api_client(
    db_session: AsyncSession,
    user: Optional[User] = None,
) -> AsyncGenerator[AsyncClient, None]:
    """Create an authenticated (or unauthenticated) test API client.

    Args:
        db_session: The test database session (from root conftest).
        user: If provided, overrides get_current_user. If None, no auth
              override is applied (for testing 401 responses).
    """
    from tests.conftest import _test_session_maker

    async def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db

    if user is not None:
        async def override_get_current_user():
            return user
        app.dependency_overrides[get_current_user] = override_get_current_user

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


# ==================== Standard Fixtures ====================
# These mirror the old root conftest fixtures but use make_api_client.


@pytest_asyncio.fixture(scope="function")
async def client(db_session: AsyncSession, test_user: User) -> AsyncGenerator[AsyncClient, None]:
    """Authenticated test client (overrides get_db + get_current_user)."""
    async with make_api_client(db_session, test_user) as c:
        yield c


@pytest_asyncio.fixture(scope="function")
async def unauthenticated_client(db_session: AsyncSession) -> AsyncGenerator[AsyncClient, None]:
    """Test client WITHOUT auth override — for testing 401 responses."""
    async with make_api_client(db_session, user=None) as c:
        yield c


@pytest_asyncio.fixture(scope="function")
async def auth_token(test_user: User) -> str:
    """Create a valid auth token for the test user."""
    from app.core.security import create_access_token
    from datetime import timedelta

    token = create_access_token(
        data={"sub": test_user.id},
        expires_delta=timedelta(hours=1),
    )
    return token


@pytest_asyncio.fixture(scope="function")
async def authenticated_client(
    db_session: AsyncSession,
    test_user: User,
    auth_token: str,
) -> AsyncGenerator[AsyncClient, None]:
    """Test client with Authorization header pre-configured."""
    from tests.conftest import _test_session_maker

    async def override_get_db():
        yield db_session

    async def override_get_current_user():
        return test_user

    app.dependency_overrides[get_db] = override_get_db
    app.dependency_overrides[get_current_user] = override_get_current_user

    def mock_session_maker():
        return _test_session_maker()

    with patch('app.repositories.user_repository.async_session_maker', mock_session_maker), \
         patch('app.services.auth_service.async_session_maker', mock_session_maker):
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
            headers={"Authorization": f"Bearer {auth_token}"},
        ) as ac:
            yield ac

    app.dependency_overrides.clear()
