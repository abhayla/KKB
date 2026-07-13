# Escalation Report — run 2026-07-13T03-13-08Z_fb299b8

## Unresolved failure (1)

`backend/tests/api/test_health.py::test_health_check` — `assert 503 == 200`

- **Category:** INFRASTRUCTURE (HIGH confidence) → RETRY_INFRA
- **Root cause:** `/health` (backend/app/main.py) bypasses FastAPI DI and calls `engine.connect()` on the module-level asyncpg engine from `app.db.postgres`. Test fixtures override `get_db` with in-memory SQLite; no live PostgreSQL exists in the test environment, so the handler returns 503.
- **Retry:** 1 infra retry performed — identical failure (deterministic, not flaky).
- **Known issue:** documented at `backend/tests/CLAUDE.md:70` — "Health check returns 503 because tests use SQLite, not real PostgreSQL. Not a regression."
- **Why no auto-fix / no GitHub issue:** dispatch matrix routes INFRASTRUCTURE to RETRY_INFRA (neither fixer nor issue-manager). The repo explicitly classifies this as an expected environment limitation; changing the endpoint or test would contradict that documentation and the user's "no changes unless a test fails for real" intent.

## Options for the owner (pick one, none applied)

1. Run the suite with a live local PostgreSQL so `/health` can connect (makes the test meaningful end-to-end).
2. Refactor `health_check()` to use the `DbSession` dependency so test overrides apply.
3. Mark the test `xfail`/skip when running against SQLite (encodes the documented known issue in the test itself).
