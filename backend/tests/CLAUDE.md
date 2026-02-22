# Backend Tests

SQLite in-memory via `conftest.py` fixtures. Each test gets a fresh engine with `StaticPool`.

## Running Tests

```bash
# From backend/
PYTHONPATH=. pytest                                    # All tests
PYTHONPATH=. pytest tests/test_auth.py -v              # Single file
PYTHONPATH=. pytest tests/test_preference_service.py::test_add_include_rule -v  # Single test
PYTHONPATH=. pytest --cov=app                          # With coverage
```

## Choosing the Right Fixture

| Fixture | Auth override | DB override | Use when |
|---------|--------------|-------------|----------|
| `client` | Yes (`get_current_user` → `test_user`) | Yes | Most tests — authenticated endpoint calls |
| `unauthenticated_client` | No | Yes | Testing that endpoints return 401 without auth |
| `authenticated_client` | No (uses real JWT path) | Yes + `Authorization: Bearer` header | Testing the actual JWT verification flow |
| `db_session` | N/A | Yes | Direct service/repository unit tests |
| `test_user` | N/A | Creates user in DB | When you need a user object |

## Known Issues

- **4 pre-existing test failures** in `test_auth.py`: `conftest.py` globally overrides `get_current_user`, which breaks the 4 auth tests that need the real dependency. This is a known issue, not a regression. Do not try to fix it by removing the global override (it would break everything else).

## Adding New Models

When you add a new SQLAlchemy model, you MUST import it in `conftest.py` so SQLite creates the table. Without this, tests will fail with "no such table" errors.

## SQLite vs PostgreSQL Differences

- `conftest.py` registers `sqlite3.register_adapter(uuid.UUID, str)` to handle UUID columns — SQLite doesn't support native UUID comparison.
- Some PostgreSQL-specific features (array columns, JSON operators) may behave differently in SQLite tests.

## Session Patching

`user_repository.async_session_maker` is patched with `mock_session_maker` in all client fixtures. If you add a new repository that calls `async_session_maker` directly (instead of using the `db: AsyncSession` parameter), you must also patch it in the fixtures.

## Test Organization

- Most tests are function-based (`def test_feature():`)
- Some files use class-based organization: `test_ai_meal_service.py`, `test_chat_api.py`, `test_preference_service.py`
- `pytest.ini` has `asyncio_mode = "auto"` — async fixtures/tests work without explicit `@pytest.mark.asyncio`

## Cleanup Fixture

`cleanup_production_engine` is `autouse=True` — it runs after every test to dispose asyncpg connections that were created at import time by `postgres.py`. This prevents connection pool leaks during test runs.
