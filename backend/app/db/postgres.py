"""PostgreSQL database configuration and session management.

This module replaces Firestore as the primary database.
Uses SQLAlchemy async engine with connection pooling for optimal performance.
"""

import logging
from typing import AsyncGenerator

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import AsyncAdaptedQueuePool

from app.config import settings

logger = logging.getLogger(__name__)

# Create async engine with connection pooling
# Using AsyncAdaptedQueuePool for better connection reuse
engine = create_async_engine(
    settings.database_url,
    echo=settings.sql_echo,
    pool_size=10,  # Number of connections to keep in pool
    max_overflow=20,  # Additional connections allowed when pool is full
    pool_timeout=30,  # Seconds to wait for a connection from pool
    pool_recycle=1800,  # Recycle connections after 30 minutes
    pool_pre_ping=True,  # Verify connections before using
    poolclass=AsyncAdaptedQueuePool,
    connect_args={
        "server_settings": {"statement_timeout": "30000"}
    },  # 30s query timeout
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
    """Initialize database connection.

    Call this at application startup to verify connection.
    For production, use Alembic migrations for schema management.
    """

    # Import all models to register them with SQLAlchemy
    from app.models import (  # noqa: F401
        ai_recipe_catalog,
        chat,
        config,
        festival,
        grocery,
        household,
        meal_plan,
        notification,
        recipe,
        recipe_rule,
        refresh_token,
        stats,
        usage_log,
        user,
    )

    # Test connection
    async with engine.begin() as conn:
        # Just verify we can connect
        await conn.run_sync(lambda _: None)
        logger.info("PostgreSQL connection verified")


async def create_tables() -> None:
    """Create all database tables.

    WARNING: This should only be used for development/testing.
    For production, use Alembic migrations.
    """
    from app.db.base import Base
    from app.models import (  # noqa: F401
        ai_recipe_catalog,
        chat,
        config,
        festival,
        grocery,
        household,
        meal_plan,
        notification,
        recipe,
        recipe_rule,
        refresh_token,
        stats,
        usage_log,
        user,
    )

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
        logger.info("Database tables created")


async def drop_tables() -> None:
    """Drop all database tables.

    WARNING: This will delete all data! Use with extreme caution.
    """
    from app.db.base import Base
    from app.models import (  # noqa: F401
        ai_recipe_catalog,
        chat,
        config,
        festival,
        grocery,
        household,
        meal_plan,
        notification,
        recipe,
        recipe_rule,
        refresh_token,
        stats,
        usage_log,
        user,
    )

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
        logger.warning("All database tables dropped")


async def close_db() -> None:
    """Close database connection pool.

    Call this at application shutdown.
    """
    await engine.dispose()
    logger.info("PostgreSQL connection pool closed")
