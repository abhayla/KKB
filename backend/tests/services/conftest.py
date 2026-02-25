"""Service test fixtures — inherits db fixtures from root conftest."""

# Service tests primarily use db_session directly (no HTTP client needed).
# All fixtures from backend/tests/conftest.py (db_engine, db_session,
# test_user, cleanup_production_engine) are inherited automatically.
