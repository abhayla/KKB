"""Pytest configuration and fixtures."""

import sqlite3
import uuid
from typing import AsyncGenerator
from unittest.mock import patch

# Register UUID adapter for SQLite so that uuid.UUID objects are
# automatically converted to strings when used as bind parameters.
# This is needed because some services do uuid.UUID(id_str) and compare
# against String(36) columns — PostgreSQL handles this natively but
# SQLite (used in tests) does not.
sqlite3.register_adapter(uuid.UUID, str)

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.db.base import Base
from app.db.database import get_db
from app.db.postgres import engine as production_engine
from app.main import app
from app.models.user import User

# Import all models to register them with SQLAlchemy
from app.models import (  # noqa: F401
    ai_recipe_catalog,
    chat,
    config,
    festival,
    grocery,
    meal_plan,
    notification,
    recipe,
    recipe_rule,
    refresh_token,
    stats,
    usage_log,
    user,
)

# Use in-memory SQLite for testing. StaticPool ensures all connections within
# a single engine share the same underlying database, so the test session and
# any patched async_session_maker sessions see the same tables/data.
# Each test gets its own engine, providing full isolation between tests.
TEST_DATABASE_URL = "sqlite+aiosqlite://"

# Test session maker (will be set in fixtures)
_test_session_maker = None


@pytest_asyncio.fixture(scope="function", autouse=True)
async def cleanup_production_engine():
    """Dispose of the production asyncpg engine after each test.

    This prevents connection pool issues when running multiple tests.
    The production engine is created at module import time but we use
    SQLite for tests, so we need to clean up the unused asyncpg connections.
    """
    yield
    # Dispose of any pending asyncpg connections from the production engine
    await production_engine.dispose()


@pytest_asyncio.fixture(scope="function")
async def db_engine():
    """Create test database engine using in-memory SQLite.

    StaticPool ensures all sessions from this engine share the same
    in-memory database. Each test function gets a fresh engine (and
    therefore a fresh empty database), providing full test isolation
    without file-based SQLite locking/collision issues.
    """
    engine = create_async_engine(
        TEST_DATABASE_URL,
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )

    # Create all tables
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    yield engine

    # Drop all tables
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)

    await engine.dispose()


@pytest_asyncio.fixture(scope="function")
async def db_session(db_engine) -> AsyncGenerator[AsyncSession, None]:
    """Create test database session."""
    global _test_session_maker
    _test_session_maker = async_sessionmaker(
        db_engine,
        class_=AsyncSession,
        expire_on_commit=False,
    )

    async with _test_session_maker() as session:
        yield session


@pytest_asyncio.fixture(scope="function")
async def test_user(db_session: AsyncSession) -> User:
    """Create a test user in the database."""
    user = User(
        id=str(uuid.uuid4()),
        firebase_uid="test-firebase-uid",
        email="test@example.com",
        name="Test User",
        is_onboarded=True,
        is_active=True,
    )
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest.fixture(autouse=True)
def _clear_dependency_overrides():
    """Clear FastAPI dependency overrides after every test.

    Prevents stale overrides from leaking between tests when they run
    in different orderings (fixes email_uniqueness test failures in
    full-suite runs).
    """
    yield
    app.dependency_overrides.clear()


# ==================== Legacy Client Fixtures ====================
# These will be removed after all test files are moved to subdirectories.
# They are duplicated in tests/api/conftest.py using make_api_client.


@pytest_asyncio.fixture(scope="function")
async def client(db_session: AsyncSession, test_user: User) -> AsyncGenerator[AsyncClient, None]:
    """Create test HTTP client with database and auth overrides."""
    from app.api.deps import get_current_user

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
        ) as ac:
            yield ac


@pytest_asyncio.fixture(scope="function")
async def unauthenticated_client(db_session: AsyncSession) -> AsyncGenerator[AsyncClient, None]:
    """Create test HTTP client WITHOUT auth override — for testing 401 responses."""

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
    auth_token: str
) -> AsyncGenerator[AsyncClient, None]:
    """Create test HTTP client with auth header pre-configured."""
    from app.api.deps import get_current_user

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
