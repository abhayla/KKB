---
name: run-e2e-journeys
description: >
  Run the Android E2E journey test suites (J01-J17) sequentially by journey number.
  Each journey tests a complete user flow with its own Suite and Journey file pair.
  Invokes /systematic-debugging + /fix-loop on failure and /learn-n-improve on success.
type: workflow
allowed-tools: "Bash Read Grep Glob Skill"
argument-hint: "[J01|J02|...|J17|all]"
version: "2.0.0"
synthesized: true
private: false
---

# Run E2E Journey Tests

Run the numbered journey test suites that test complete user flows end-to-end.

**Arguments:** $ARGUMENTS — e.g., `J01`, `J05`, or `all`

## STEP 1: Identify Target Journeys

The project has 17 journey suites in `android/app/src/androidTest/java/com/rasoiai/app/e2e/journeys/`:

| Journey | Suite File | Flow File | Tests |
|---------|-----------|-----------|-------|
| J01 | J01_FirstTimeUserSuite | J01_FirstTimeUserJourney | First-time onboarding |
| J02 | J02_NewUserFirstMealPlanSuite | J02_NewUserFirstMealPlanJourney | First meal plan generation |
| J03 | J03_CompleteE2EJourneySuite | J03_CompleteE2EJourney | Full app walkthrough |
| J04 | J04_DailyMealPlanningSuite | J04_DailyMealPlanningJourney | Daily meal plan interactions |
| J05 | J05_WeeklyGroceryShoppingSuite | J05_WeeklyGroceryShoppingJourney | Grocery list flow |
| J06 | J06_CookingAMealSuite | J06_CookingAMealJourney | Cooking mode flow |
| J07 | J07_ManagingDietaryPrefsSuite | J07_ManagingDietaryPrefsJourney | Dietary preferences |
| J08 | J08_AIMealPlanQualitySuite | J08_AIMealPlanQualityJourney | AI generation quality |
| J09 | J09_FamilyProfileMgmtSuite | J09_FamilyProfileMgmtJourney | Family profile management |
| J10 | J10_ExploringAppFeaturesSuite | J10_ExploringAppFeaturesJourney | Feature exploration |
| J11 | J11_CustomizingSettingsSuite | J11_CustomizingSettingsJourney | Settings customization |
| J12 | J12_OfflineErrorResilienceSuite | J12_OfflineResilienceJourney | Offline/error handling |
| J13 | J13_ReturningUserQuickCheckSuite | J13_ReturningUserQuickCheckJourney | Returning user baseline |
| J14 | J14_AIChatRecipeDiscoverySuite | J14_AIChatRecipeDiscoveryJourney | AI chat multi-turn |
| J15 | J15_HouseholdSetupSuite | J15_HouseholdSetupJourney | Household creation & members |
| J16 | J16_HouseholdMealCollaborationSuite | J16_HouseholdMealCollaborationJourney | Household collaboration |
| J17 | J17_HouseholdNotificationsSuite | J17_HouseholdNotificationsJourney | Household notifications |

Each Suite class runs the Journey methods in order. Base test infrastructure is in `e2e/base/`.

## STEP 2: Run the Target Journey

If a specific journey is requested (e.g., `J05`):

```bash
cd android
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J05_WeeklyGroceryShoppingSuite
```

If `all` is requested, run sequentially from J01 to J12:

```bash
cd android
for j in 01 02 03 04 05 06 07 08 09 10 11 12; do
  echo "=== Running J${j} ==="
  ./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J${j}_*Suite \
    2>&1 | tail -5
  if [ $? -ne 0 ]; then
    echo "FAILED at J${j} — stopping"
    break
  fi
done
```

## STEP 3: Analyze Results

If a journey fails:
1. Read the Suite file to understand what methods are called in order
2. Read the Journey file to find the specific failing step
3. Check `e2e/base/BaseE2ETest.kt` and `ComposeTestExtensions.kt` for test utilities
4. Check `e2e/di/Fake*.kt` files for test doubles configuration

## STEP 4: Auto-Fix and Learn (On Failure Only)

If any journey failed in STEP 2, automatically invoke the fix-and-learn pipeline. Do NOT just document failures — fix them.

### 4a. Diagnose First (E2E-Specific)

Per Rule 15 in `claude-behavior.md`, E2E failures go through `/systematic-debugging` first because environment issues masquerade as code bugs:

```
Skill("systematic-debugging", args="E2E journey {JXX} failed: <failure_output>")
```

### 4b. Invoke Fix-Loop

Once root cause is isolated by systematic debugging:

```
Skill("fix-loop", args="<diagnosed_failure>\n\nretest_command: cd android && ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.{JXX_Suite}")
```

### 4c. Capture Learnings (On Fix Success)

If `/fix-loop` reports success:

```
Skill("learn-n-improve", args="session")
```

### 4d. Batch Processing

When running `all` journeys and multiple fail:
- Group failures by root cause (e.g., J01-J03 share same onboarding timeout)
- Fix each root cause once, then re-run affected journeys
- Do NOT invoke `/fix-loop` separately for each journey that shares the same root cause

### Skip Conditions

Do NOT auto-invoke fix pipeline if:
- No emulator connected
- Backend not running at localhost:8000
- Build failed before tests ran (Gradle compilation error)

---

## CRITICAL RULES

- MUST run from `android/` directory using `./gradlew` (Unix syntax)
- MUST use the Suite class (not the Journey class) as the test target — Suites control execution order
- Run journeys sequentially, not in parallel — they share emulator state
- J01-J03 are prerequisite flows — if they fail, later journeys will also fail
- Use `HiltTestRunner` (configured in `app/build.gradle.kts`) — never override the test runner
