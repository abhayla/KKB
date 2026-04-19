---
description: >
  End-to-end tests live under android/app/src/androidTest/.../e2e/journeys/
  organized as numbered suites J01..J17. Each journey ships as paired files
  (JNN_FeatureJourney.kt + JNN_FeatureSuite.kt), uses a JourneyStepLogger,
  and references TestTags constants.
globs: ["android/app/src/androidTest/java/com/rasoiai/app/e2e/**/*.kt", "android/app/src/main/java/com/rasoiai/app/presentation/common/TestTags.kt"]
synthesized: true
private: false
version: "1.0.0"
---

# E2E Journey — Numbered Suite Organization

RasoiAI's E2E test suite is organized into 17 numbered journeys (J01–J17).
Each journey is a real user flow — first-time-user, add-household,
meal-plan-generation, recipe-customization, etc. This rule encodes the
naming and structure so new journeys slot in cleanly.

## Filename contract

Every journey MUST produce EXACTLY two files under
`android/app/src/androidTest/java/com/rasoiai/app/e2e/journeys/`:

| File | Role |
|------|------|
| `JNN_FeatureJourney.kt` | The test class containing `@Test` methods (the step-by-step flow) |
| `JNN_FeatureSuite.kt` | A JUnit `Suite` runner that groups the journey's tests with related `*FlowTest` tests |

Both files MUST share the same `JNN_Feature` prefix. The NN is a zero-padded
2-digit number (`J01`, not `J1`). Numbers are assigned sequentially — do NOT
reuse a number after a journey is removed (mark the slot retired in
`journey_results.txt` instead).

Current numbering (for reference, not enforcement — read
`android/app/src/androidTest/java/com/rasoiai/app/e2e/journeys/` for the
up-to-date list):

- J01 FirstTimeUser, J02 NewUserFirstMealPlan, J03 AddHousehold,
  J04 EditPreferences, J05 GenerateMealPlan, J06 RecipeDetail, ..., J17 ...

## Suite file shape

```kotlin
@RunWith(Suite::class)
@Suite.SuiteClasses(
    AuthFlowTest::class,
    OnboardingFlowTest::class,
    OnboardingNavigationTest::class
)
class J01_FirstTimeUserSuite
```

The Suite MUST list related `*FlowTest` classes, not just the journey itself —
this is how the journey collects its prerequisite flow tests for batch
execution via `./gradlew :app:connectedDebugAndroidTest --tests "*J01_*"`.

## Journey file shape

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class J01_FirstTimeUserJourney {
    private val log = JourneyStepLogger("J01")

    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun happyPath() {
        log.step("Open app") { /* ... */ }
        log.step("Sign in with fake phone auth") {
            AuthRobot(composeRule).signInWithFakeOtp()
        }
        log.step("Complete onboarding") {
            OnboardingRobot(composeRule).submitDefaultPreferences()
        }
        log.step("Verify home screen") {
            HomeRobot(composeRule).assertMealPlanLoaded()
        }
    }
}
```

Required elements:

- `JourneyStepLogger("JNN")` — constructed with the journey's number.
  Its `step(name) { block }` produces the structured `journey_JNN_output.txt`
  log that CI parses.
- Robots (`AuthRobot`, `HomeRobot`, etc.) encapsulate UI interactions.
  Journeys MUST call robots — NEVER `composeRule.onNodeWithText(...)` inline.
  See the companion rule `e2e-robot-pattern.md`.
- `TestTags` from `presentation/common/TestTags.kt` — all `composeRule`
  lookups inside robots go through `onNodeWithTag(TestTags.SOMETHING)`, not
  text lookups. Text changes with copy; tags don't.

## Robots

Robots live in `e2e/robots/` and are named `FeatureRobot`. A robot MUST:

- Accept the `ComposeTestRule` in its constructor.
- Expose verb-phrase methods (`signInWithFakeOtp`, `submitDefaultPreferences`,
  `assertMealPlanLoaded`) — one method per user-visible action or assertion.
- NEVER assert on implementation details (internal state, repository counts).
  Journeys test what the user sees.

## CRITICAL constraints

- MUST NOT create a journey file without its matching suite file (or vice
  versa). They are paired — a missing suite means CI won't run the journey
  alongside its dependencies; a missing journey means the suite is a
  pointless container.
- MUST NOT reuse a retired JNN number. Append sequentially. The number is an
  identifier, not a sort order.
- MUST NOT inline UI lookups in a journey. Always route through a robot. This
  keeps journey files readable as prose ("open app → sign in → verify") and
  makes selector changes a one-file edit.
- MUST NOT run journeys on API 36 emulators. Use API 34 (matches the target
  SDK). CI defaults to API 29 as a lowest-common-denominator regression
  check — that discrepancy is intentional, not a bug.
- Every journey MUST produce a `journey_JNN_output.txt` artifact.
  `JourneyStepLogger` writes this to the instrumented app's external files
  dir — `run_journeys.sh` pulls it off-device after the run.
