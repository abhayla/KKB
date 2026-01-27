"""Database configuration and session management."""

from typing import AsyncGenerator

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import NullPool

from app.config import settings

# Create async engine
engine = create_async_engine(
    settings.database_url,
    echo=settings.debug,
    poolclass=NullPool,  # Disable pooling for async
)

# Create async session factory
async_session_maker = async_sessionmaker(
    engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autocommit=False,
    autoflush=False,
)


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    """Dependency that provides a database session.

    Yields:
        AsyncSession: Database session that auto-closes after use
    """
    async with async_session_maker() as session:
        try:
            yield session
        finally:
            await session.close()


async def init_db() -> None:
    """Initialize database tables.

    Call this at application startup to create tables if they don't exist.
    For production, use Alembic migrations instead.
    """
    from app.db.base import Base
    from app.models import (  # noqa: F401 - Import models to register them
        chat,
        festival,
        grocery,
        meal_plan,
        recipe,
        stats,
        user,
    )

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
