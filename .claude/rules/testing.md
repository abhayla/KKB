---
paths:
  - "backend/tests/**/*.py"
  - "android/app/src/test/**/*.kt"
  - "android/app/src/androidTest/**/*.kt"
---

# Testing Rules

## General Principles

1. **Test Isolation** — Each test should be independent; no shared mutable state
2. **Descriptive Names** — Test names should describe the scenario and expected outcome
3. **Arrange-Act-Assert** — Structure tests clearly with setup, action, and verification
4. **One Assertion Focus** — Each test should verify one behavior (multiple asserts OK if related)
5. **No Test Interdependence** — Tests must pass in any order

## Test Categories

| Category | Purpose | Speed |
|----------|---------|-------|
| Unit | Individual functions/methods | Fast (ms) |
| Integration | Component interactions | Medium (seconds) |
| E2E | Full user flows | Slow (minutes) |

## Running Tests

- Run targeted tests first (faster feedback)
- Run full suite before committing
- Use `-x` flag to stop on first failure when debugging

## Fixtures & Test Data

- Use factories/fixtures for test data — don't hardcode
- Clean up test data in teardown
- Use in-memory databases for unit tests where possible
- Mock external services (APIs, email, file systems)

## Handling Failures

- Distinguish real failures from flaky tests
- Fix flaky tests immediately — they erode confidence
- Never `@skip` or `@ignore` a test without a tracking issue
- Re-run flaky tests 2-3 times before investigating

## Flaky Test Prevention

- **Use auto-wait over timeouts** — Never use `sleep(2)` or fixed delays. Use framework-provided waiters: `waitFor`, `eventually`, `toBeVisible`. Fixed delays are the #1 cause of flaky tests.
- **Isolate test state** — Each test gets its own database transaction, browser context, or temp directory. Never share mutable state across tests. Use `beforeEach` setup, not `beforeAll`.
- **Mock external dependencies** — Network calls to third-party APIs, email services, payment providers MUST be mocked. External service flakiness should not fail your tests.
- **Use deterministic test data** — No `random()`, no `Date.now()` in assertions, no reliance on database auto-increment order. Use factories with fixed seeds.
- **Control time** — Use clock mocking (`jest.useFakeTimers`, `freezegun`, `timecop`) for time-dependent logic. Never assert against wall clock time.
- **Avoid order dependence** — Tests MUST pass when run individually, in reverse order, or in random order. Use `--randomize` flag to verify.

## Documentation

- Add brief comments explaining non-obvious test setups
- Document test fixtures and their purpose
- Keep test helper functions close to where they're used


---

## Project-Specific Testing

## Test Distribution

| Platform | Tests (approx.) | Framework |
|----------|-----------------|-----------|
| Backend | ~538 | pytest |
| Android Unit | ~580 | JUnit + MockK |
| Android UI | ~750+ | Compose UI Testing |
| Android E2E | ~67+ | Compose UI Testing + Hilt + Real API |

*Run `PYTHONPATH=. pytest --collect-only -q` (backend) or `./gradlew test` (Android) for current totals.*

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
- All backend tests in `backend/tests/`, named `test_{feature}.py` (43 test files)
- Some files use class-based test organization (e.g., `test_ai_meal_service.py`, `test_chat_api.py`)

## Android Unit Tests
- Use `TestDispatcherRule` for coroutine tests
- Use `Fake*Repository` classes (in-memory implementations)
- JUnit 5 with `useJUnitPlatform()`
- Located in `app/src/test/java/com/rasoiai/app/presentation/`

## Android UI Tests

Tests use **Compose UI Testing** (not Espresso). Located in `app/src/androidTest/`.

| Test Type | Pattern | Purpose |
|-----------|---------|---------|
| UI Tests | `*ScreenTest.kt` | Test composable with mock UiState |
| Integration Tests | `*IntegrationTest.kt` | Full app with Hilt DI |
| E2E Tests | `*FlowTest.kt` | Complete user flows |

## E2E Test Infrastructure

**Key files** in `app/src/androidTest/java/com/rasoiai/app/e2e/`:

| File | Purpose |
|------|---------|
| `base/BaseE2ETest.kt` | Base class with Hilt setup, meal plan generation |
| `base/ComposeTestExtensions.kt` | Compose test extension functions |
| `base/TestDataFactory.kt` | Test data creation helpers |
| `util/BackendTestHelper.kt` | Backend API calls with retry |
| `di/FakePhoneAuthClient.kt` | Fake auth (returns `fake-firebase-token`) |
| `robots/` | Robot pattern classes (HomeRobot, GroceryRobot, etc.) |
| `rules/RetryRule.kt` | JUnit rule for retrying flaky tests |
| `E2ETestSuite.kt` | Test suite runner (runs CoreDataFlowTest first) |
| `presentation/common/TestTags.kt` | All semantic test tags |

**E2E backend URL:** `http://10.0.2.2:8000` (Android emulator maps `10.0.2.2` → host `localhost`).

**E2E auth bypass:** `FakePhoneAuthClient` sends `"fake-firebase-token"` to backend (`DEBUG=true` required). See `android/app/src/androidTest/CLAUDE.md` for full auth flow details.

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
