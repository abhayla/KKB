# Backend Tests

SQLite in-memory via `conftest.py` fixtures. Each test gets a fresh engine with `StaticPool`.

## Running Tests

```bash
# From backend/
PYTHONPATH=. pytest                                    # All tests
PYTHONPATH=. pytest tests/api/test_auth.py -v          # Single file
PYTHONPATH=. pytest tests/services/test_preference_service.py::test_add_include_rule -v  # Single test
PYTHONPATH=. pytest --cov=app                          # With coverage
PYTHONPATH=. pytest --collect-only -q                  # Count tests without running
```

## Fixture Architecture (3-level conftest)

```
tests/conftest.py          ← DB engine, session, test_user, client fixtures
tests/api/conftest.py      ← make_api_client() helper only
tests/integration/conftest.py ← make_firebase_mock_client(), sharma_user
```

**All standard client fixtures live in root `conftest.py`** — they are available to every subdirectory (api/, services/, integration/). Do NOT duplicate fixtures in subdirectory conftest files.

`make_api_client()` lives in `api/conftest.py` and is the single source of truth for test client setup (DB override, auth override, session_maker patches). The root fixtures delegate to it.

## Choosing the Right Fixture

| Fixture | Auth override | DB override | Use when |
|---------|--------------|-------------|----------|
| `client` | Yes (`get_current_user` → `test_user`) | Yes | Most tests — authenticated endpoint calls |
| `unauthenticated_client` | No | Yes | Testing that endpoints return 401 without auth |
| `authenticated_client` | Yes + `Authorization: Bearer` header | Yes | Testing the actual JWT verification flow |
| `db_session` | N/A | Yes | Direct service/repository unit tests |
| `test_user` | N/A | Creates user in DB | When you need a user object |
| `auth_token` | N/A | N/A | When you need a raw JWT string |

For custom client setups (e.g., two users in one test), use `make_api_client()` inline:

```python
from tests.api.conftest import make_api_client

async def test_two_users(db_session, test_user):
    second_user = User(id=str(uuid.uuid4()), ...)
    db_session.add(second_user)
    await db_session.commit()

    async with make_api_client(db_session, second_user) as client2:
        resp = await client2.get("/api/v1/users/me")
```

## Test Directory Structure

```
tests/
├── conftest.py              # Root: DB engine, session, test_user, all client fixtures
├── factories.py             # Shared test data builders (make_user, make_preferences)
├── api/                     # API endpoint tests (21 files)
│   └── conftest.py          # make_api_client() helper
├── services/                # Service/business logic tests (18 files)
│   └── (no conftest.py)     # Uses root fixtures directly
├── integration/             # Cross-cutting tests (4 files)
│   └── conftest.py          # make_firebase_mock_client(), sharma_user
└── performance/             # Performance benchmarks
```

## Known Issues

- **1 `test_health.py` failure**: Health check returns 503 because tests use SQLite, not real PostgreSQL. Not a regression.
- **4 `test_email_uniqueness.py` failures**: Fail in full suite, pass in isolation — test ordering issue with session state. Not a regression.

## Adding New Models

When you add a new SQLAlchemy model, you MUST import it in root `conftest.py` so SQLite creates the table. Without this, tests will fail with "no such table" errors. See the 5-location import rule in `.claude/rules/backend.md`.

## SQLite vs PostgreSQL Differences

- `conftest.py` registers `sqlite3.register_adapter(uuid.UUID, str)` to handle UUID columns — SQLite doesn't support native UUID comparison.
- Some PostgreSQL-specific features (array columns, JSON operators) may behave differently in SQLite tests.

## Session Patching

Both `user_repository.async_session_maker` and `auth_service.async_session_maker` are patched with `mock_session_maker` in `make_api_client()`. The auth service uses `async_session_maker` directly for token rotation/logout operations.

If you add a new service/repository that calls `async_session_maker` directly (instead of using the `db: AsyncSession` parameter), you must also patch it in `make_api_client()`. Some test files (`test_auth_merge.py`, `test_email_uniqueness.py`) have their own client fixtures that also need both patches.

## Rules

- **No fixture duplication**: All standard fixtures (`client`, `unauthenticated_client`, `authenticated_client`, `auth_token`) are in root `conftest.py`. Do NOT re-define them in subdirectory conftest files.
- **Use `make_api_client()` for custom setups**: Import from `tests.api.conftest` — never copy-paste the setup logic.
- **`asyncio_mode = "auto"`**: In `pytest.ini` — async fixtures/tests work without explicit `@pytest.mark.asyncio`.
- **Class-based tests**: Some files use classes for grouping (`test_ai_meal_service.py`, `test_chat_api.py`, `test_preference_service.py`). Either style is acceptable.
- **Naming convention**: Files are `test_{feature}.py`, functions are `test_{scenario}` or `async def test_{scenario}`.
- **`_clear_dependency_overrides`**: Autouse fixture in root conftest clears FastAPI dependency overrides after every test. Do NOT remove — prevents stale overrides leaking between tests.
- **`cleanup_production_engine`**: Autouse fixture disposes asyncpg connections after every test. Do NOT remove — prevents connection pool leaks.
