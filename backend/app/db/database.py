"""Database configuration and session management.

This module re-exports from postgres.py for backwards compatibility.
"""

# Re-export from postgres module
from app.db.postgres import (
    async_session_maker,
    close_db,
    create_tables,
    drop_tables,
    engine,
    get_db,
    init_db,
)

__all__ = [
    "engine",
    "async_session_maker",
    "get_db",
    "init_db",
    "create_tables",
    "drop_tables",
    "close_db",
]
