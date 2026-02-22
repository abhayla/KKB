# Android Instrumented & E2E Tests

Compose UI Testing with Hilt DI. Custom `HiltTestRunner` configured in `app/build.gradle.kts`.

## Running Tests

```bash
# From android/
./gradlew :app:connectedDebugAndroidTest    # All instrumented tests

# Single test class
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.HomeScreenFlowTest
```

Requires an emulator running **API 34** (not 36 — Espresso compatibility issues). CI uses API 29.

## Test Types

| Type | Location | Pattern | Base class |
|------|----------|---------|------------|
| UI tests | `presentation/*ScreenTest.kt` | `createComposeRule()` + mock UiState | None |
| E2E tests | `e2e/flows/*FlowTest.kt` | Real backend + Hilt DI | `BaseE2ETest` |

## E2E Test Architecture

### Execution Order (Critical)

`E2ETestSuite.kt` controls the order. **`CoreDataFlowTest` must run first** — it clears state, performs full auth + onboarding, and persists state to DataStore. All subsequent tests inherit this persisted state.

### Backend Connection

Tests connect to `http://10.0.2.2:8000` (Android emulator → host localhost). Backend must be running with `DEBUG=true` for fake auth to work.

### Fake Auth Flow

1. `FakeGoogleAuthClient` returns `"fake-firebase-token"` (injected via `FakeAuthModule` with `@TestInstallIn`)
2. Backend (`DEBUG=true`) accepts fake token in `firebase.py`, returns JWT
3. JWT is saved to DataStore
4. `SplashViewModel` checks `isSignedIn` after a **2-second delay**

**Timing gotcha**: `createAndroidComposeRule` launches `TestActivity` BEFORE `@Before` runs. Auth tokens must be stored before the 2-second Splash check fires.

### Setting Up Test State

| Method | Use when |
|--------|----------|
| `setUpAuthenticatedState()` | Test needs Home screen with meal plan data |
| `setUpAuthenticatedStateWithoutMealPlan()` | Test doesn't need meal cards (Settings, Recipe Rules) — avoids triggering Gemini AI call |

### Static State Sharing

`backendMealPlanGenerated` is a `companion object` field on `BaseE2ETest` — it persists across test instances in the same JVM run. This avoids redundant meal plan generation calls (4-7 seconds each).

## Robot Pattern

Robot classes in `e2e/robots/` wrap Compose test API into a readable DSL:

```kotlin
homeRobot.verifyMealCardsDisplayed()
groceryRobot.tapGenerateList()
```

One robot per major screen (12 robots total).

## Key Gotchas

- **RetryRule is disabled** (`maxRetries=0`) — Hilt doesn't support re-injection on retry. Use `RetryUtils` for action-level retries instead.
- **Test Orchestrator is disabled** — `clearPackageData` is off. Tests share DataStore state intentionally (suite depends on it).
- **TestTags**: All semantic tags are in `presentation/common/TestTags.kt`. UI tests break if tags are renamed or missing.
- **TestActivity** is different from `MainActivity` — declared in test manifest, not the main one.
- **`FakeNetworkModule`** replaces `NetworkModule` via `@TestInstallIn` — it provides an always-online `NetworkMonitor`.
