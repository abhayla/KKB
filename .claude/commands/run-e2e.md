# Run Android E2E Tests by Feature Group

Run Android E2E tests sequentially by feature group with automatic failure investigation and fixing.

**Target group:** $ARGUMENTS

If `$ARGUMENTS` is empty, run ALL groups sequentially. If a group name is provided (e.g., `auth`, `home`, `recipe-rules`), run only that group.

---

## PREREQUISITES — Run These First (Every Time)

### 1. Check Emulator

```bash
C:/Users/itsab/AppData/Local/Android/Sdk/platform-tools/adb.exe devices
```

- If a device is listed as `device` → emulator is ready, continue.
- If no device or only `offline` entries:
  1. Start the emulator:
     ```bash
     C:/Users/itsab/AppData/Local/Android/Sdk/emulator/emulator.exe -avd Pixel_6 &
     ```
  2. Wait for boot:
     ```bash
     C:/Users/itsab/AppData/Local/Android/Sdk/platform-tools/adb.exe wait-for-device
     C:/Users/itsab/AppData/Local/Android/Sdk/platform-tools/adb.exe shell getprop sys.boot_completed
     ```
     Loop until `sys.boot_completed` returns `1`.

### 2. Check Backend

```bash
curl -s http://localhost:8000/health
```

- If healthy response → continue.
- If connection refused or error → **STOP and tell the user**: "Backend is not running. Start it with: `cd backend && source venv/bin/activate && uvicorn app.main:app --reload`"
- Do NOT attempt to start the backend automatically.

### 3. Quick Build Check

```bash
cd android && ./gradlew assembleDebug assembleDebugAndroidTest 2>&1 | tail -5
```

If build fails, fix compilation errors before proceeding.

---

## FEATURE GROUPS

There are **13 feature groups**. When `$ARGUMENTS` is empty, run them in this order (1→13). When a group name is given, run only that group.

### Group 1: `ui-screens` — Presentation UI Tests (no backend needed)

```
com.rasoiai.app.presentation.auth.AuthScreenTest,
com.rasoiai.app.presentation.auth.AuthIntegrationTest,
com.rasoiai.app.presentation.chat.ChatScreenTest,
com.rasoiai.app.presentation.cookingmode.CookingModeScreenTest,
com.rasoiai.app.presentation.favorites.FavoritesScreenTest,
com.rasoiai.app.presentation.generation.GenerationScreenTest,
com.rasoiai.app.presentation.grocery.GroceryScreenTest,
com.rasoiai.app.presentation.home.HomeScreenTest,
com.rasoiai.app.presentation.notifications.NotificationsScreenTest,
com.rasoiai.app.presentation.onboarding.OnboardingScreenTest,
com.rasoiai.app.presentation.pantry.PantryScreenTest,
com.rasoiai.app.presentation.recipedetail.RecipeDetailScreenTest,
com.rasoiai.app.presentation.reciperules.RecipeRulesScreenTest,
com.rasoiai.app.presentation.settings.SettingsScreenTest,
com.rasoiai.app.presentation.stats.StatsScreenTest,
com.rasoiai.app.presentation.theme.ThemeTest,
com.rasoiai.app.presentation.common.ComponentsTest
```

### Group 2: `database` — Database Verification

```
com.rasoiai.app.e2e.database.DatabaseVerificationTest
```

### Group 3: `validation` — Recipe Constraint Validation

```
com.rasoiai.app.e2e.validation.RecipeConstraintTest
```

### Group 4: `auth` — Authentication Flow

```
com.rasoiai.app.e2e.flows.AuthFlowTest
```

### Group 5: `onboarding` — Onboarding Flows

```
com.rasoiai.app.e2e.flows.OnboardingFlowTest,
com.rasoiai.app.e2e.flows.OnboardingNavigationTest,
com.rasoiai.app.e2e.flows.SharmaOnboardingVerificationTest
```

### Group 6: `meal-generation` — Meal Plan Generation

```
com.rasoiai.app.e2e.flows.MealPlanGenerationFlowTest
```

### Group 7: `home` — Home Screen E2E

```
com.rasoiai.app.e2e.flows.HomeScreenTest,
com.rasoiai.app.e2e.flows.HomeScreenActionsTest,
com.rasoiai.app.e2e.flows.HomeScreenButtonsTest,
com.rasoiai.app.e2e.flows.HomeScreenLockingTest,
com.rasoiai.app.e2e.flows.HomeScreenComprehensiveTest,
com.rasoiai.app.e2e.flows.HomeScreenRealAuthTest
```

### Group 8: `grocery` — Grocery Flow

```
com.rasoiai.app.e2e.flows.GroceryFlowTest
```

### Group 9: `chat` — Chat Flow

```
com.rasoiai.app.e2e.flows.ChatFlowTest
```

### Group 10: `favorites` — Favorites Flow

```
com.rasoiai.app.e2e.flows.FavoritesFlowTest,
com.rasoiai.app.e2e.flows.AutoFavoriteOnAddRecipeTest
```

### Group 11: `recipe-rules` — Recipe Rules

```
com.rasoiai.app.e2e.flows.RecipeRulesFlowTest,
com.rasoiai.app.e2e.flows.SharmaRecipeRulesVerificationTest,
com.rasoiai.app.e2e.flows.AddChaiToBreakfastTest
```

### Group 12: `cooking-stats-settings` — Cooking, Stats, Settings, Pantry, Family

```
com.rasoiai.app.e2e.flows.CookingModeFlowTest,
com.rasoiai.app.e2e.flows.StatsScreenTest,
com.rasoiai.app.e2e.flows.SettingsFlowTest,
com.rasoiai.app.e2e.flows.PantryFlowTest,
com.rasoiai.app.e2e.flows.FamilyProfileFlowTest
```

### Group 13: `cross-cutting` — Cross-Feature & Performance

```
com.rasoiai.app.e2e.flows.CoreDataFlowTest,
com.rasoiai.app.e2e.flows.FullUserJourneyTest,
com.rasoiai.app.e2e.flows.OfflineFlowTest,
com.rasoiai.app.e2e.flows.EdgeCasesTest,
com.rasoiai.app.e2e.flows.MealTypeFilterTest,
com.rasoiai.app.e2e.performance.PerformanceTest
```

---

## EXECUTION PROTOCOL — For Each Group

### Step 1: Run the Group

Construct the Gradle command by joining all class names for the group with commas (no spaces, no newlines):

```bash
cd android && ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=CLASS1,CLASS2,CLASS3
```

Use a 10-minute timeout for groups with AI calls (meal-generation, home, chat, recipe-rules, cross-cutting). Use 5-minute timeout for others.

### Step 2: Parse Results

After the Gradle command completes:
1. Check the exit code (0 = all passed)
2. Read the test result XML files if needed:
   ```bash
   find android/app/build/outputs/androidTest-results -name "*.xml" -newer /tmp/e2e-marker 2>/dev/null | head -5
   ```
3. Count: total tests, passed, failed, skipped

### Step 3: If ALL Passed

Log the result and move to the next group:
```
✅ Group N: [group-name] → X/X passed
```

### Step 4: If ANY Failed — Enter Fix Loop

**Max 5 fix iterations per group.** If still failing after 5 attempts, ask the user what to do.

#### Fix Loop Protocol:

1. **Read the failure output** — identify which test methods failed and the exception/assertion message
2. **Read the failing test code** — understand what the test expects
3. **Read the production code** — understand what the code actually does
4. **Identify the ROOT CAUSE** — not symptoms. Common root causes:
   - Missing Hilt bindings or module registrations
   - Null safety issues (uninitialized lateinit, null returns)
   - Race conditions (UI not settled before assertion)
   - Missing test tags in Composables
   - API contract changes (backend response format changed)
   - Room schema mismatches
5. **Fix the production code** (or the test if the test itself is wrong)
6. **Rerun ONLY the current group** — same Gradle command as Step 1
7. **If still failing** → repeat from sub-step 1

#### NEVER Do These During Fix Loop:

- `@Ignore` a failing test
- Delete or comment out a failing test
- Weaken an assertion (e.g., changing `assertEquals` to `assertTrue`)
- Add `Thread.sleep()` as a fix (use `waitUntil {}` or `ComposeTestRule.waitForIdle()` instead)
- Skip the group and move on
- Create a "fix later" GitHub issue to bypass the failure

#### When a Fix Touches Shared Code:

If your fix modifies code that earlier groups also test (e.g., fixing a ViewModel used by both `ui-screens` and `home`), note it but do NOT re-run earlier groups mid-run. The final summary will flag this for the user.

---

## FINAL SUMMARY

After all groups complete (or after the single requested group), produce this report:

```
══════════════════════════════════════════════════════
  E2E TEST REPORT
══════════════════════════════════════════════════════

Group  1: ui-screens              → XXX/XXX passed (0 fixes)
Group  2: database                →  XX/XX  passed (0 fixes)
Group  3: validation              →  XX/XX  passed (0 fixes)
Group  4: auth                    →   X/X   passed (0 fixes)
Group  5: onboarding              →  XX/XX  passed (1 fix)
Group  6: meal-generation         →   X/X   passed (0 fixes)
Group  7: home                    →  XX/XX  passed (2 fixes)
Group  8: grocery                 →  XX/XX  passed (0 fixes)
Group  9: chat                    →   X/X   passed (0 fixes)
Group 10: favorites               →  XX/XX  passed (0 fixes)
Group 11: recipe-rules            →  XX/XX  passed (0 fixes)
Group 12: cooking-stats-settings  →  XX/XX  passed (0 fixes)
Group 13: cross-cutting           →  XX/XX  passed (1 fix)

──────────────────────────────────────────────────────
TOTAL: XXX/XXX passed | X root causes fixed | 0 remaining failures
══════════════════════════════════════════════════════

Fixes Applied:
  1. [File:line] — [Brief description of root cause and fix]
  2. [File:line] — [Brief description of root cause and fix]

Shared Code Warning:
  - [If any fix touched code tested by earlier groups, list here]
  - Recommend re-running: /run-e2e [affected-group]
```

If only a single group was requested, show just that group's line in the report.

---

## QUICK REFERENCE

| Group Name | Shorthand | Classes | Backend Needed? |
|---|---|---|---|
| `ui-screens` | 1 | 17 presentation tests | No |
| `database` | 2 | DatabaseVerificationTest | No |
| `validation` | 3 | RecipeConstraintTest | No |
| `auth` | 4 | AuthFlowTest | Yes |
| `onboarding` | 5 | 3 onboarding tests | Yes |
| `meal-generation` | 6 | 2 meal plan tests | Yes (AI) |
| `home` | 7 | 6 home screen tests | Yes |
| `grocery` | 8 | GroceryFlowTest | Yes |
| `chat` | 9 | ChatFlowTest | Yes (AI) |
| `favorites` | 10 | 2 favorites tests | Yes |
| `recipe-rules` | 11 | 3 recipe rules tests | Yes |
| `cooking-stats-settings` | 12 | 5 flow tests | Yes |
| `cross-cutting` | 13 | 6 cross-feature tests | Yes |
