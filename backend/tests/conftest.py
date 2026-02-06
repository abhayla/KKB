"""Pytest configuration and fixtures."""

import asyncio
import uuid
from typing import AsyncGenerator
from unittest.mock import patch

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import NullPool

from app.config import settings
from app.db.base import Base
from app.db.database import get_db
from app.db.postgres import engine as production_engine, async_session_maker as prod_session_maker
from app.main import app
from app.models.user import User

# Import all models to register them with SQLAlchemy
from app.models import (  # noqa: F401
    chat,
    config,
    festival,
    grocery,
    meal_plan,
    notification,
    recipe,
    recipe_rule,
    stats,
    user,
)

# Test database URL (use SQLite for testing)
TEST_DATABASE_URL = "sqlite+aiosqlite:///./test.db"

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
    """Create test database engine."""
    engine = create_async_engine(
        TEST_DATABASE_URL,
        poolclass=NullPool,
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


@pytest_asyncio.fixture(scope="function")
async def client(db_session: AsyncSession, test_user: User) -> AsyncGenerator[AsyncClient, None]:
    """Create test HTTP client with database and auth overrides."""
    from app.api.deps import get_current_user
    from app.core.security import create_access_token
    from datetime import timedelta

    async def override_get_db():
        yield db_session

    async def override_get_current_user():
        return test_user

    # Override dependencies
    app.dependency_overrides[get_db] = override_get_db
    app.dependency_overrides[get_current_user] = override_get_current_user

    # Patch UserRepository to use test session
    def mock_session_maker():
        return _test_session_maker()

    with patch('app.repositories.user_repository.async_session_maker', mock_session_maker):
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as ac:
            yield ac

    app.dependency_overrides.clear()


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

    with patch('app.repositories.user_repository.async_session_maker', mock_session_maker):
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
            headers={"Authorization": f"Bearer {auth_token}"},
        ) as ac:
            yield ac

    app.dependency_overrides.clear()
