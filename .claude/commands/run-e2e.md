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

  #### 4a. Diagnose the failure

  **If `fail_counts[current_class] == 1` (first failure)** — lightweight manual analysis:
    1. **Read the failure output** — identify exception/assertion message
    2. **Read the failing test code** — understand what the test expects
    3. **Read the production code** — understand what the code actually does
    4. **Identify the ROOT CAUSE** — not symptoms. Common root causes:
       - Missing Hilt bindings or module registrations
       - Null safety issues (uninitialized lateinit, null returns)
       - Race conditions (UI not settled before assertion)
       - Missing test tags in Composables
       - API contract changes (backend response format changed)
       - Room schema mismatches

  **If `fail_counts[current_class] >= 2 and < 4` (2nd-3rd failure)** — escalate to `debugger` agent:
    - Launch the `debugger` agent (via Task tool, subagent_type `general-purpose` using the debugger agent prompt) with:
      - The test class name and full failure output
      - Description of the previous fix attempt(s) and why they didn't work
      - Instruction: "Perform deep root cause analysis. Correlate the error with test code, production code, Hilt modules, Room schemas, and any system-level behavior. Return a diagnosis and a specific recommended fix with file paths and code changes."
    - Use the debugger's diagnosis to guide the fix
    - Track: `debugger_invocations += 1`, append test class name to `debugger_tests[]`

  **If `fail_counts[current_class] >= 4 and < 6` (4th-5th failure)** — `thinkhard` escalation:
    - Launch the `debugger` agent with all previous context PLUS:
      - All previous fix attempts and their failure reasons
      - Instruction: "Use extended thinking (thinkhard). Systematically enumerate ALL possible root causes before proposing a fix. Consider: thread timing, async race conditions, state leaks between tests, emulator-specific quirks, Hilt graph issues, and implicit dependencies. Return a ranked list of hypotheses with the most likely fix."
    - Track: `debugger_invocations += 1`

  **If `fail_counts[current_class] >= 6` (6th+ failure)** — `thinkUltrahard` escalation:
    - Launch the `debugger` agent with all previous context PLUS:
      - Complete history of all fix attempts and failure reasons
      - Instruction: "Use maximum thinking depth (thinkUltrahard). Re-examine every assumption from scratch. Consider architectural issues, cross-module interactions, non-obvious failure modes, and whether the test itself has a fundamental design flaw. Explore unconventional fixes. Return a comprehensive analysis with the recommended fix."
    - Track: `debugger_invocations += 1`

  #### 4b. Apply the fix

    5. **Fix the production code** (or the test if the test itself is wrong)

  #### 4c. Code review gate (after every fix)

    6. **Launch the `code-reviewer` agent** (via Task tool) with:
       - The diff of changes made (output of `git diff`)
       - The test that failed and the failure reason
       - Instruction: "Review this fix for: weakened assertions, `@Ignore` additions, regressions to other tests, security issues (OWASP Top 10), and `Thread.sleep()` usage. Categorize any issues as Critical / High / Medium / Low. Return a verdict: APPROVED or FLAGGED with details."
    - **If code-reviewer returns Critical issue**: revert the fix (`git checkout -- <files>`), log the issue, and re-attempt the fix from Step 4a
    - **If code-reviewer returns High/Medium/Low issues**: log them in `review_issues[]` but proceed
    - **If code-reviewer approves**: proceed
    - Track: `code_reviews += 1`, `code_reviews_approved += 1` or `code_reviews_flagged += 1`

  #### 4d. Restart group

    7. **Reset `test_index = 0`** (restart group from first test)
    8. Go to **Step 2**

### NEVER Do These During Fix Loop:

- `@Ignore` a failing test
- Delete or comment out a failing test
- Weaken an assertion (e.g., changing `assertEquals` to `assertTrue`)
- Add `Thread.sleep()` as a fix (use `waitUntil {}` or `ComposeTestRule.waitForIdle()` instead)
- Skip the group and move on
- Create a "fix later" GitHub issue to bypass the failure

#### When a Fix Touches Shared Code:

If your fix modifies code that earlier groups also test (e.g., fixing a ViewModel used by both `ui-screens` and `home`), note it but do NOT re-run earlier groups mid-run. The final summary will flag this for the user.

---

## AGENT INTEGRATION

This workflow uses 4 custom agents from `.claude/agents/` at specific trigger points:

| Agent | Trigger | Purpose |
|-------|---------|---------|
| `debugger` | Step 4a, on 2nd+ failure of the same test (thinkhard at 4th, thinkUltrahard at 6th) | Deep root cause analysis with log correlation and system behavior tracing |
| `code-reviewer` | Step 4c, after every fix is applied | Inline quality gate — catches weakened assertions, regressions, security issues |
| `docs-manager` | Post-run, if any fixes were applied | Updates test docs (Functional-Requirement-Rule.md, CONTINUE_PROMPT.md, test counts) |
| `git-manager` | Post-run, after docs-manager completes | Commits all changes (fixes + doc updates) with conventional commit format |

### How agents are launched

All agents are launched via the **Task tool** with `subagent_type: "general-purpose"`. Include in the prompt:
- The agent's name and role context (e.g., "You are acting as the `debugger` agent")
- All relevant inputs (error output, diff, file paths)
- A clear instruction of what to return (diagnosis, verdict, file list, etc.)

### Tracking variables

Initialize these alongside the Step 1 group variables:

```
// Agent tracking (persists across all groups)
debugger_invocations = 0
debugger_tests = []         // test class names that triggered debugger
code_reviews = 0
code_reviews_approved = 0
code_reviews_flagged = 0
review_issues = []          // logged High/Medium/Low issues from code-reviewer
all_fixes = []              // { file, line, description } for each fix applied
skipped_tests = []          // { class_name, fail_count, fix_attempts[] } for 10x failures
```

---

## POST-RUN AGENT PIPELINE

After all groups complete (or the single requested group), check if any fixes were applied during the run.

**If `len(all_fixes) == 0`**: skip this section entirely — no agents needed.

**If `len(all_fixes) > 0`**: run the following agents sequentially:

### Step A: docs-manager agent

Launch the `docs-manager` agent (via Task tool) with:
- The list of all fixes applied: `all_fixes[]` (file, line, description for each)
- The list of test files that were modified (if any)
- Instructions:
  - Update `docs/testing/Functional-Requirement-Rule.md` if any test files were added or modified
  - Update `docs/CONTINUE_PROMPT.md` with a session summary of what was fixed
  - Update test counts in `CLAUDE.md` (non-protected sections only) if test counts changed
  - Do NOT modify the "Rules for Claude" protected section in CLAUDE.md
  - Return the list of documentation files that were updated

### Step B: git-manager agent (runs after Step A completes)

Launch the `git-manager` agent (via Task tool) with:
- Instructions:
  - Stage only the relevant files: the fix files + any doc files updated by docs-manager
  - Do NOT stage `.env`, build artifacts, or files in `.gitignore`
  - Create a conventional commit: `fix(e2e): [concise summary of fixes applied]`
  - Include the list of fixes in the commit body
  - Do NOT push unless the user explicitly requested it
  - Return the commit hash and message

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

Agent Activity:
  - Debugger invocations: X (for tests: [list of class names])
  - Code reviews: X (Y approved, Z flagged issues)
  - Docs updated: [list of files, or "none — no fixes applied"]
  - Commit: [hash] — [message] (or "none — no fixes applied")

Review Issues (if any):
  - [severity] [file:line] — [description from code-reviewer]

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
