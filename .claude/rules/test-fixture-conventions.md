---
description: >
  Backend tests MUST use the correct test fixture (client, unauthenticated_client, authenticated_client,
  db_session) based on what they're testing. Wrong fixture choice causes false passes or obscure failures.
globs: ["backend/tests/**/*.py"]
synthesized: true
private: true
---

# Backend Test Fixture Conventions

All test fixtures are defined in `backend/tests/conftest.py`. MUST NOT duplicate fixtures in subdirectory conftest files.

## Fixture selection guide

| Fixture | Auth behavior | Use when |
|---------|-------------|----------|
| `client` | Pre-authenticated — `get_current_user` dependency is overridden to return `test_user` | Most tests — testing authenticated endpoint behavior |
| `unauthenticated_client` | No auth override — requests have no credentials | Testing 401 responses, auth middleware behavior |
| `authenticated_client` | Real JWT in `Authorization: Bearer` header | Testing actual JWT verification flow (token parsing, expiry) |
| `db_session` | N/A (no HTTP) | Direct service/repository unit tests that bypass the API layer |

## Database isolation

Each test function gets its own in-memory SQLite database via `StaticPool`:

- `db_engine` fixture creates a fresh `create_async_engine` per test
- `StaticPool` ensures all connections share the same in-memory DB within a test
- All models are imported in `conftest.py` so SQLite creates all tables (see 5-location model import rule)
- Production asyncpg engine is disposed after each test via `cleanup_production_engine` fixture

## Custom multi-user setups

For tests requiring multiple authenticated users (e.g., household sharing, permissions):

```python
from tests.api.conftest import make_api_client

async def test_two_users_share_household(db_session):
    client_a = await make_api_client(user_id="user-a", phone="+91-111")
    client_b = await make_api_client(user_id="user-b", phone="+91-222")
    # Both clients hit the same test database
```

## Services that bypass dependency injection

Some services (e.g., `auth_service.py`) call `async_session_maker` directly instead of receiving a session via dependency injection. Tests for these services MUST also patch `async_session_maker` — the standard `db_session` fixture is not sufficient. See `backend/tests/CLAUDE.md` for details.

## MUST NOT

- MUST NOT create conftest.py files in test subdirectories that redefine shared fixtures — all standard fixtures live in `backend/tests/conftest.py`
- MUST NOT use `client` fixture when testing unauthenticated behavior — it has auth pre-overridden, so 401 tests will incorrectly pass
- MUST NOT use `authenticated_client` for normal endpoint tests — it's slower (real JWT creation) and only needed when testing the auth flow itself
- MUST NOT share mutable state across tests — each test gets a fresh in-memory SQLite database
