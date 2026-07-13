# Per-Test Report — run 2026-07-13T03-13-08Z_fb299b8

Scope: backend pytest suite only (Android app SKIPPED by user directive).
Config: bundled plugin default (no project `.claude/config/test-pipeline.yml`).

## Failing tests (1)

| test_id | Lanes run | Verdict | Error |
|---------|-----------|---------|-------|
| `backend/tests/api/test_health.py::test_health_check` | functional, api | FAIL in both | `assert 503 == 200` — `GET /health` returned 503 Service Unavailable |

## Unexercised tests

None — 862/862 manifest tests executed. Lane ledgers (JSONL) match lane verdicts.

## Lane summaries

| Lane | Manifest | Executed | Passed | Failed | Skipped | Gate |
|------|----------|----------|--------|--------|---------|------|
| functional | 862 | 862 | 861 | 1 | 0 | FAILED |
| api | 550 | 550 | 549 | 1 | 0 | FAILED |
| ui | 0 | 0 | — | — | — | SKIP (empty queue; backend-only run) |

Notes:
- API-lane contract-test/integration-test sub-skills skipped: no Pact/OpenAPI contract files exist in the project.
- Android unit/instrumented/E2E suites: SKIPPED (out of scope for this run per user).
