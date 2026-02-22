---
paths:
  - "backend/tests/**/*.py"
  - "android/app/src/test/**/*.kt"
  - "android/app/src/androidTest/**/*.kt"
---

# Testing Rules

## Backend Test Fixtures
| Fixture | Use when |
|---------|----------|
| `client` | Most tests — pre-authenticated endpoint calls |
| `unauthenticated_client` | Testing 401 responses |
| `authenticated_client` | Testing actual JWT verification flow |
| `db_session` | Direct service/repository unit tests |

- 4 pre-existing failures in `test_auth.py` are known — don't try to fix the global auth override
- New models MUST be imported in `conftest.py` or SQLite won't create the table
- `pytest.ini` has `asyncio_mode = "auto"` — async tests work without explicit marks

## Android Unit Tests
- Use `TestDispatcherRule` for coroutine tests
- Use `Fake*Repository` classes (in-memory implementations)
- JUnit 5 with `useJUnitPlatform()`

## Android E2E Tests
- Extend `BaseE2ETest`, use Robot pattern
- `CoreDataFlowTest` runs first in `E2ETestSuite` — sets up auth state for all others
- Backend must run at `http://10.0.2.2:8000` with `DEBUG=true`
- `setUpAuthenticatedState()` for tests needing Home screen
- `setUpAuthenticatedStateWithoutMealPlan()` to skip Gemini AI call
- `RetryRule` is disabled (maxRetries=0) — use `RetryUtils` for action-level retries
- 2-second Splash timing: auth tokens must be in DataStore before SplashViewModel checks

## Test Documentation
- Each test file needs KDoc/docstring header: `/** Requirement: #XX - Description */`
- E2E tests go in `e2e/flows/`, UI tests in `presentation/`
