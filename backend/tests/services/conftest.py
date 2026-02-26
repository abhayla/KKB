"""Service test fixtures — inherits db fixtures from root conftest.

Some service tests (items_per_meal, recipe_rule_family_conflict,
token_rotation, user_deletion) also exercise API endpoints directly,
so we provide client fixtures here via make_api_client.
"""

from typing import AsyncGenerator

import pytest_asyncio
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User

from tests.api.conftest import make_api_client


@pytest_asyncio.fixture(scope="function")
async def client(db_session: AsyncSession, test_user: User) -> AsyncGenerator[AsyncClient, None]:
    """Authenticated HTTP client (convenience for service tests that hit API)."""
    async with make_api_client(db_session, test_user) as c:
        yield c


@pytest_asyncio.fixture(scope="function")
async def unauthenticated_client(db_session: AsyncSession) -> AsyncGenerator[AsyncClient, None]:
    """Unauthenticated HTTP client for testing 401 responses."""
    async with make_api_client(db_session) as c:
        yield c


@pytest_asyncio.fixture(scope="function")
async def auth_token(test_user: User) -> str:
    """Create a valid auth token for the test user."""
    from app.core.security import create_access_token
    from datetime import timedelta

    return create_access_token(
        data={"sub": test_user.id},
        expires_delta=timedelta(hours=1),
    )


@pytest_asyncio.fixture(scope="function")
async def authenticated_client(
    db_session: AsyncSession,
    test_user: User,
    auth_token: str,
) -> AsyncGenerator[AsyncClient, None]:
    """Authenticated HTTP client with Bearer header."""
    async with make_api_client(db_session, test_user) as c:
        c.headers["Authorization"] = f"Bearer {auth_token}"
        yield c
