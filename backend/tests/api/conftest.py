"""API test fixtures — make_api_client helper for authenticated test clients.

The make_api_client() async context manager encapsulates all the setup
needed for an authenticated API test client:
  - get_db override → test db_session
  - get_current_user override → provided user
  - session_maker patches for UserRepository and AuthService
  - AsyncClient creation and cleanup
  - dependency_overrides.clear() on exit

Standard fixtures (client, unauthenticated_client, authenticated_client,
auth_token) are defined in root conftest.py — they use make_api_client
and are available to all test subdirectories.

Usage inline in a test (for custom setup):

    async def test_something(db_session, test_user):
        async with make_api_client(db_session, test_user) as client:
            resp = await client.get("/api/v1/health")
"""

from contextlib import asynccontextmanager
from typing import AsyncGenerator, Optional
from unittest.mock import patch

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

    with patch(
        "app.repositories.user_repository.async_session_maker", mock_session_maker
    ), patch("app.services.auth_service.async_session_maker", mock_session_maker):
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as ac:
            yield ac

    app.dependency_overrides.clear()
