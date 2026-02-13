# Run Android E2E Tests by Feature Group

Run Android E2E tests sequentially by feature group with automatic failure investigation and fixing.

**Target group:** $ARGUMENTS

If `$ARGUMENTS` is empty, run ALL groups sequentially. If a group name is provided (e.g., `auth`, `home`, `recipe-rules`), run only that group.

---

## AUTO-PROCEED RULES (MANDATORY)

- Do NOT ask for any confirmations before, during, or after test execution
- Do NOT ask "Should I proceed?", "Ready to continue?", or similar
- Automatically handle all prerequisites (emulator, build, backend check)
- If build fails, auto-fix compilation errors and rebuild — do not ask
- If emulator is not running, auto-start it and wait for boot — do not ask
- If backend is not running, auto-start it in the background and wait for health check — do not ask
- Proceed through all groups/tests without pausing for user input
- Never stop for user input. If a test fails 10 times, skip it and continue with remaining groups.

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
- If connection refused or error → **auto-start the backend**:
  1. Start in background:
     ```bash
     cd D:/Abhay/VibeCoding/KKB/backend && source venv/bin/activate && uvicorn app.main:app --reload &
     ```
  2. Poll health check every 3 seconds, up to 30 seconds:
     ```bash
     for i in {1..10}; do curl -sf http://localhost:8000/health && break || sleep 3; done
     ```
  3. If healthy after polling → log `✅ Backend auto-started` and continue.
  4. If still unhealthy after 30 seconds → log `⚠️ Backend failed to start after 30s. Continuing without backend — groups requiring backend will likely fail.` Continue execution (do not stop).

### 3. Quick Build Check

```bash
cd android && ./gradlew assembleDebug assembleDebugAndroidTest 2>&1 | tail -5
```

If build fails, fix compilation errors before proceeding.

---

## FEATURE GROUPS

There are **14 feature groups**. When `$ARGUMENTS` is empty, run them in this order (1→14). When a group name is given, run only that group.

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
com.rasoiai.app.e2e.flows.MealPlanGenerationFlowTest,
com.rasoiai.app.e2e.flows.MealPlanAIVerificationTest
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

### Group 14: `full-journey` — Full User Journey (Auth → Rules → Regeneration)

```
com.rasoiai.app.e2e.flows.FullJourneyFlowTest
```

---

## EXECUTION PROTOCOL — Per-Test Individual Execution

For each group, run test classes **one at a time**. On failure, fix and restart the group from the beginning.

### Step 1: Initialize Group

```
test_classes = [list of classes in group, in order]
test_index = 0
fail_counts = {}   // map of class_name → failure count
group_restarts = 0
skipped_tests = [] // tests that hit 10-failure limit and were skipped
```

### Step 2: Run Current Test

Run ONLY `test_classes[test_index]` as a single Gradle invocation:

```bash
cd android && ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=<single_class>
```

Use a 10-minute timeout for groups with AI calls (meal-generation, home, chat, recipe-rules, cross-cutting, full-journey). Use 5-minute timeout for others.

### Step 3: If PASSED

- Log: `✅ test_classes[test_index] passed`
- Increment `test_index`
- If `test_index == len(test_classes)`: **GROUP COMPLETE** → move to next group
- Else: go to **Step 2**

### Step 4: If FAILED — Fix and Restart Group

- Increment `fail_counts[current_class]`
- Increment `group_restarts`
- If `fail_counts[current_class] >= 10`:
    Log: `❌ Test [class_name] has failed 10 times — skipping remaining tests in this group.`
    Record all 10 attempted fixes in `skipped_tests[]`.
    **Skip the rest of this group** and move to the next group. Do NOT stop execution.
- Else:

  #### 4a. Invoke /fix-loop Skill

  **MANDATORY: Use the Skill tool** to invoke `/fix-loop` in Full Loop mode. Do NOT read fix-loop.md and follow it inline — you MUST invoke it as a Skill.

  Invoke: `skill: "fix-loop"` with arguments:
  ```
  failure_output:         {raw Gradle test failure output}
  failure_context:        "E2E test {current_class} failed in group {group_name}"
  files_of_interest:      {test class file + related production files}
  build_command:          "cd android && ./gradlew assembleDebug assembleDebugAndroidTest"
  retest_command:         "cd android && ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class={current_class}"
  retest_timeout:         {600 for AI groups (meal-generation, home, chat, recipe-rules, cross-cutting, full-journey), 300 for others}
  max_iterations:         1
  max_attempts_per_issue: 1
  prohibited_actions:     ["@Ignore", "delete test", "weaken assertion", "Thread.sleep()", "skip group", "fix-later issue"]
  fix_target:             "either"
  force_thinking_level:   {computed from fail_counts: 1→"normal", 2-3→"thinkhard", 4-5→"thinkhard", 6+→"ultrathink"}
  log_dir:                ".claude/logs/fix-loop/"
  ```
  Budget rationale: `max_iterations: 1` because the group restart loop is the outer retry — each fix-loop invocation gets one attempt, but the group restarts up to 10 times total.

  **NO EXCEPTIONS** — invoke for ALL failures including known or pre-existing issues.

  Collect Skill output:
  - Append fixes to `all_fixes[]`
  - Accumulate metrics into tracking variables

  #### 4b. Restart group

    5. **Reset `test_index = 0`** (restart group from first test)
    6. Go to **Step 2**

> **REMINDER: Always use the Skill tool for /fix-loop. Do NOT fix test failures inline.**

#### When a Fix Touches Shared Code:

If a fix modifies code that earlier groups also test, note it but do NOT re-run earlier groups mid-run. The final summary will flag this for the user.

---

## SKILL INTEGRATION

This workflow invokes 2 Skills via the Skill tool, and those Skills delegate to read-only Agents via the Task tool:

| Skill | Trigger | Purpose |
|-------|---------|---------|
| `/fix-loop` | Step 4a, on test failure | Iterative fix cycle with thinking escalation, debugger Agent delegation, and code review |
| `/post-fix-pipeline` | Post-run, if any fixes were applied | Test suite verification + documentation via docs-manager Agent + git commit via git-manager Agent |

> **REMINDER: Always use the Skill tool to invoke /fix-loop and /post-fix-pipeline. Do NOT read the .md files and follow them inline.**

### Tracking variables

Initialize these alongside the Step 1 group variables:

```
// Agent tracking (persists across all groups)
all_fixes = []              // { file, line, description } collected from fix-loop
skipped_tests = []          // { class_name, fail_count } for 10x failures
fix_loop_metrics = {        // accumulated from fix-loop outputs
  debugger_invocations: 0,
  code_reviews: 0,
  code_reviews_approved: 0,
  code_reviews_flagged: 0,
  review_issues: []
}
```

---

## POST-RUN PIPELINE

After all groups complete (or the single requested group), check if any fixes were applied.

**If `len(all_fixes) == 0`**: skip this section — no agents needed.

**If `len(all_fixes) > 0`**: **Use the Skill tool** to invoke `/post-fix-pipeline`. Do NOT read post-fix-pipeline.md and follow it inline.

Invoke: `skill: "post-fix-pipeline"` with arguments:
```
fixes_applied:            {all_fixes from tracking}
files_changed:            {all modified file paths}
session_summary:          "E2E test run: {N} fixes applied across {groups}"
test_suite_commands:      [
  { name: "backend", command: "cd backend && PYTHONPATH=. pytest --tb=short -q", timeout: 300 },
  { name: "android-unit", command: "cd android && ./gradlew test --console=plain", timeout: 600 }
]
test_suite_max_fix_attempts: 2
docs_instructions:        "Update docs/testing/Functional-Requirement-Rule.md if test files changed. Update docs/CONTINUE_PROMPT.md with session summary. Update test counts in CLAUDE.md (non-protected sections only)."
commit_format:            "fix(e2e): {summary}"
commit_scope:             "e2e"
push:                     false
```

Collect Skill output for the final report.

---

## FINAL SUMMARY

After all groups complete (or after the single requested group), produce this report:

```
══════════════════════════════════════════════════════
  E2E TEST REPORT
══════════════════════════════════════════════════════

Group  1: ui-screens              → 17/17 passed (0 fixes)
Group  2: database                →  1/1  passed (0 fixes)
Group  3: validation              →  1/1  passed (0 fixes)
Group  4: auth                    →  1/1  passed (0 fixes)
Group  5: onboarding              →  3/3  passed (1 fix: OnboardingFlowTest fixed 1x — group restarted 1 time)
Group  6: meal-generation         →  1/1  passed (0 fixes)
Group  7: home                    →  6/6  passed (2 fixes: HomeScreenTest fixed 2x, HomeScreenActionsTest fixed 1x — group restarted 3 times)
Group  8: grocery                 →  1/1  passed (0 fixes)
Group  9: chat                    →  1/1  passed (0 fixes)
Group 10: favorites               →  2/2  passed (0 fixes)
Group 11: recipe-rules            →  3/3  passed (0 fixes)
Group 12: cooking-stats-settings  →  5/5  passed (0 fixes)
Group 13: cross-cutting           →  6/6  passed (1 fix: OfflineFlowTest fixed 1x — group restarted 1 time)

──────────────────────────────────────────────────────
TOTAL: XXX/XXX passed | X root causes fixed | X group restarts | X skipped (10x limit)
══════════════════════════════════════════════════════

Skipped Tests (10x failure limit):
  - [class_name] — 10 attempts exhausted. Fix attempts: [brief list]
  - Recommend manual investigation for these tests.
  (If none skipped, omit this section.)

Fixes Applied:
  1. [File:line] — [Brief description of root cause and fix]
  2. [File:line] — [Brief description of root cause and fix]

Skill Activity (from /fix-loop + /post-fix-pipeline):
  - Fix-loop iterations: X
  - Debugger Agent invocations: X
  - Code reviews: X (Y approved, Z flagged)
  - Docs updated: [list of files, or "none — no fixes applied"]
  - Commit: [hash] — [message] (or "none — no fixes applied")
  - Pipeline status: COMPLETED | BLOCKED_BY_TEST_SUITE | NO_FIXES

Review Issues (if any):
  - [severity] [file:line] — [description from code-reviewer Agent]

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
| `full-journey` | 14 | FullJourneyFlowTest | Yes (AI x2) |
