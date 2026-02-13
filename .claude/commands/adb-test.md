# ADB Manual E2E Testing

Test app screens via ADB (uiautomator dump, screencap, input tap) — fully autonomous with self-healing fix loops.

**Target screen:** $ARGUMENTS

If `$ARGUMENTS` is empty, test ALL 12 screens sequentially. If a screen name is provided (e.g., `home`, `grocery`), test only that screen. If a flow name is provided (e.g., `new-user-journey`), test that user journey flow.

**Valid screen names:** `auth-flow`, `home`, `grocery`, `chat`, `favorites`, `stats`, `settings`, `notifications`, `recipe-detail`, `cooking-mode`, `pantry`, `recipe-rules`

**Valid flow names:** `new-user-journey`, `existing-user`, `recipe-interaction`, `chat-ai`, `grocery-management`, `offline-mode`, `edge-cases`, `dark-mode`, `pantry-rules-crud`, `stats-tracking`

**Special arguments:** `all-flows` (run all 10 flows sequentially), `all-flows-from <name>` (run from specified flow onwards)

**Argument detection:**
1. If `$ARGUMENTS` matches a valid screen name → run screen test protocol (Sections E-F)
2. If `$ARGUMENTS` matches a valid flow name → run flow execution protocol (Section G)
3. If `$ARGUMENTS` is `all-flows` → run all 10 flows sequentially (flow01 → flow10)
4. If `$ARGUMENTS` is `all-flows-from <name>` → run flows from the specified flow onwards
5. If `$ARGUMENTS` is empty → run all 12 screen tests

---

## AUTO-PROCEED RULES (MANDATORY)

- Do NOT ask for any confirmations before, during, or after testing
- Do NOT ask "Should I proceed?", "Ready to continue?", or similar
- Automatically handle all prerequisites (emulator, backend, build, auth, onboarding)
- If build fails, auto-fix compilation errors and rebuild — do not ask
- If emulator is not running, auto-start it and wait for boot — do not ask
- If backend is not running, auto-start it in the background and wait — do not ask
- If app is on auth screen, auto-authenticate via ADB taps — do not ask
- If app is on onboarding, auto-complete all 5 steps via ADB taps — do not ask
- If no meal plan exists, auto-generate via backend API — do not ask
- Proceed through all screens without pausing for user input
- Never stop for user input. If a screen exhausts 12 fix iterations (3 per issue), skip it and continue.

---

## ADB CONSTANTS & PATTERNS

```
ADB = C:/Users/itsab/AppData/Local/Android/Sdk/platform-tools/adb.exe
EMULATOR = C:/Users/itsab/AppData/Local/Android/Sdk/emulator/emulator.exe
SCREENSHOT_DIR = docs/testing/screenshots
LOG_DIR = .claude/logs/adb-test
APP_PACKAGE = com.rasoiai.app
APP_ACTIVITY = com.rasoiai.app.MainActivity
```

**Read `docs/testing/adb-patterns.md` for the 13 reusable ADB interaction patterns** (UI dump, screenshot, tap, text input, back press, parse bounds, find element, scroll/redump, crash/ANR detection, keyboard dismiss, system dialog detection, screenshot validation, logcat capture).

### CRITICAL: Compose testTag() is NOT visible in uiautomator XML

Jetpack Compose `testTag()` values do NOT appear in uiautomator XML dumps. All element searches must use:
- **`text`** attribute — visible text on screen
- **`content-desc`** attribute — accessibility labels
- **`resource-id`** attribute — Android resource IDs (rare in Compose)
- **`class`** attribute — widget type
- **Bounds position** — relative screen position (bottom nav y > 90%, top bar y < 15%)

---

## PREREQUISITES — Run These First (Every Time)

### D1. Check Emulator

```bash
$ADB devices
```

- If a device is listed as `device` → emulator is ready, continue.
- If no device or only `offline` entries:
  1. Start the emulator: `$EMULATOR -avd Pixel_6 &`
  2. Wait for boot: `$ADB wait-for-device` then loop `$ADB shell getprop sys.boot_completed` until `1` (max 120 seconds).

### D2. Check Backend

```bash
curl -s http://localhost:8000/health
```

- If healthy → continue.
- If connection refused → auto-start:
  1. `cd D:/Abhay/VibeCoding/KKB/backend && source venv/bin/activate && uvicorn app.main:app --reload &`
  2. Poll every 3s for 30s: `for i in {1..10}; do curl -sf http://localhost:8000/health && break || sleep 3; done`
  3. If still unhealthy → log warning, continue (screens not requiring backend may still work).

### D3. Build & Install App

```bash
cd D:/Abhay/VibeCoding/KKB/android && ./gradlew assembleDebug
$ADB install -r D:/Abhay/VibeCoding/KKB/android/app/build/outputs/apk/debug/app-debug.apk
```

If build fails, auto-fix compilation errors and rebuild (max 3 attempts).

### D4. Clean Test Data

```bash
cd D:/Abhay/VibeCoding/KKB/backend && PYTHONPATH=. python scripts/cleanup_user.py
```

### D5. Launch App & Detect Current Screen

```bash
$ADB shell am force-stop $APP_PACKAGE
$ADB shell am start -n $APP_PACKAGE/$APP_ACTIVITY
```

Wait 3 seconds, dump UI, detect which screen:

| Screen | Detection |
|--------|-----------|
| Splash | text="RasoiAI" or loading indicator |
| Auth | text="Sign in with Google" or text="Welcome" |
| Onboarding | text="Tell us about your household" or progress bar |
| Home | text="This Week's Menu" or text="BREAKFAST" |

### D6. Auto-Complete Auth & Onboarding (if needed)

**If on Auth screen:**
1. Find "Sign in with Google" button, compute center from bounds, tap via ADB
2. Wait up to 10 seconds for auth to complete (fake-firebase-token auto-authenticates)
3. If onboarding appears next, proceed to onboarding steps below

**If on Onboarding — complete all 5 steps:**

| Step | Actions |
|------|---------|
| 1. Household | Find "Next", tap household size dropdown if shown, tap to proceed |
| 2. Dietary | Tap a diet preference (e.g., text="Vegetarian"), tap "Next" |
| 3. Cuisine | Tap a cuisine (e.g., text="North Indian"), tap "Next" |
| 4. Dislikes | Tap "Next" (skip dislikes for speed) |
| 5. Cooking Time | Tap "Next" or "Get Started" or "Generate" |

After each step, wait 1-2s and dump UI to verify progression. If generation screen appears, wait up to 90s for meal plan generation.

**If no meal plan data on Home screen:** Generate via backend API:
```bash
curl -X POST http://localhost:8000/api/v1/meal-plans/generate \
  -H "Authorization: Bearer $(curl -s -X POST http://localhost:8000/api/v1/auth/firebase -H 'Content-Type: application/json' -d '{"firebase_token":"fake-firebase-token"}' | python -c 'import sys,json;print(json.load(sys.stdin).get(\"access_token\",\"\"))')" \
  -H "Content-Type: application/json"
```
Wait up to 90s, then force-stop and restart app.

### D7. Clear Logcat Buffer

```bash
$ADB logcat -c
```

---

## SCREEN TESTING PROTOCOL

### Test Definitions Reference

Read `docs/testing/adb-test-definitions.md` for per-screen test checklists (navigation path, primary identifier, required elements, interactive elements, data validation, known issues).

### Execution Order

1. `auth-flow` → 2. `home` → 3. `grocery` → 4. `chat` → 5. `favorites` → 6. `stats` → 7. `settings` → 8. `notifications` → 9. `recipe-detail` → 10. `cooking-mode` → 11. `pantry` → 12. `recipe-rules`

### Per-Screen Protocol (8 Steps)

**E0. Pre-Screen Checks**

**E0a. Backend Health** (only for: `auth-flow`, `home`, `chat`, `recipe-detail`, `recipe-rules`):
```bash
curl -sf http://localhost:8000/health --max-time 5
```
If unhealthy: kill existing uvicorn, restart, poll 30s. If still down → BLOCKED.
Track: `backend_health_checks += 1`, `backend_restarts += 1` if restarted.

**E0b. System Dialog Check:** Run Pattern 11 to detect/dismiss any permission/battery/system dialogs. Re-dump XML after dismissal.

**E1. Navigate to Screen**

Follow navigation path from `adb-test-definitions.md`. Use ADB taps. Wait 1-2s after each tap for animation.

**E2. Verify Arrival (with Crash Detection)**

**E2a. Crash/ANR Check:** Dump UI. Run Pattern 9. If crash detected:
1. Capture logcat (Pattern 13), screenshot, dismiss dialog
2. Relaunch app, re-navigate (repeat E1), re-dump XML
3. Log as CRITICAL issue

**E2b. Primary Identifier Check:** Search XML for screen's primary identifier. If not found after 3 dump attempts (2s gaps), classify as BLOCKED.

**E3. Element Checklist (Two-Phase Scroll Protocol)**

**Phase 1 — Initial Scan:** Parse UI dump, check each required element. Record: FOUND (with bounds) or PENDING_SCROLL (below fold) or MISSING (not below-fold and not found).

**Phase 2 — Scroll Search** (only if PENDING_SCROLL elements exist): Use Pattern 8 — max 3 scroll attempts. Re-check after each scroll. Scroll back to top when done.

**Final Status:** FOUND, FOUND_AFTER_SCROLL, or MISSING.

**ISSUE threshold:** >50% MISSING elements is an ISSUE.

**E4. Screenshot + AI Visual Analysis**

Capture: `$ADB exec-out screencap -p > $SCREENSHOT_DIR/adb-test_{screen}_{timestamp}.png`

Validate with Pattern 12. If BLANK_SUSPECT: wake device, retry (max 2 retries). If still blank → `visual_verified=false`.

If `visual_verified=true`: Read screenshot with Read tool, analyze layout, alignment, data, colors, empty states.

If `visual_verified=false`: Skip visual analysis, log warning. Screen CANNOT be PASS unless ALL elements verified via XML AND all interactions pass.

**E5. Interactive Testing**

For each interactive element: find in XML → compute center → tap → wait → dump → verify expected result → screenshot if meaningful → navigate back. Dismiss keyboard (Pattern 10) after text input.

**E5.5. Logcat Pre-Check**

Capture app-level logcat before classification:
```bash
$ADB logcat -d -t 50 --pid=$($ADB shell pidof $APP_PACKAGE) > $LOG_DIR/{session}/logcat_{screen}_precheck.txt
```
Scan for errors (` E `, `FATAL`, `Exception`). Record `app_error_count`.

**E5.7. Pre-Classification Gate (MANDATORY)**

Copy and fill in ALL 6 questions before classifying:
```
□ Pre-Classification Gate for [{screen_name}]:
  1. "All required elements found (FOUND or FOUND_AFTER_SCROLL)?" → [YES: N/N / NO: N missing — ISSUE_FOUND]
  2. "All interactive tests passed?" → [YES: N/N / NO: N failed — ISSUE_FOUND]
  3. "Screenshot visually verified?" → [YES / NO (blank/GPU issue)]
  4. "Zero crashes/ANRs detected?" → [YES / NO — ISSUE_FOUND]
  5. "Logcat shows zero app errors?" → [YES / NO: N errors — ISSUE_FOUND]
  6. "Any observations that indicate unexpected behavior?" → [NO / YES: {list} — ISSUE_FOUND]

  GATE RESULT: [PASS_ELIGIBLE / ISSUE_FOUND]
```

Rules:
- If `visual_verified=false`: Question 3 is NO. Screen CAN still pass if 1,2,4,5,6 are YES.
- An "observation" IS an issue — no category for "noted behavior".
- You MUST NOT proceed to E6 without completing this gate.

**E6. Classify Screen Result**

| Classification | Criteria |
|----------------|----------|
| **PASS** | Gate PASS_ELIGIBLE: all 6 questions YES, zero missing/failed/crashes |
| **ISSUE_FOUND** | Gate ISSUE_FOUND: any question NO |
| **BLOCKED** | Cannot reach screen after 3 attempts |

**No "PASS with observations".** Any deviation = ISSUE_FOUND.

**E6.5. Post-Classification Logcat**

| Result | Capture |
|--------|---------|
| PASS | E5.5 pre-check sufficient |
| ISSUE_FOUND | `$ADB logcat -d -t 200 *:E > $LOG_DIR/{session}/logcat_{screen}_errors.txt` |
| BLOCKED | `$ADB logcat -d -t 100 AndroidRuntime:E *:S > $LOG_DIR/{session}/logcat_{screen}_crash.txt` |

Then clear: `$ADB logcat -c`

If **PASS** → next screen. If **ISSUE_FOUND** → Fix Loop (Section F). If **BLOCKED** → log, next screen.

> **REMINDER: When ISSUE_FOUND, you MUST use the Skill tool to invoke `/fix-loop`. Do NOT fix issues inline. Do NOT read fix-loop.md and follow it manually.**

---

## FIX LOOP (when ISSUE_FOUND)

**MANDATORY — NO EXCEPTIONS.** Every ISSUE_FOUND enters this loop regardless of whether the issue is known, pre-existing, or seems architectural. Budget: **3 attempts per issue, 12 max total iterations per screen.**

Issues are enumerated from E2a/E3/E4/E5 and processed in severity order: CRASH → MISSING → FAILED → VISUAL.

### Fix Loop Steps

For each issue, for each attempt (up to 3 per issue, 12 total per screen):

**Step F1: Pre-Fix Decision — Code vs. Definition**

Before assuming a code bug, check if the test definition is outdated:
1. Cross-reference the screenshot — does the element exist with different text?
2. Search XML for partial/semantic matches
3. **Decision:**
   - Partial match found → update `docs/testing/adb-test-definitions.md`. Track: `definition_updates += 1`. Re-verify E3 with updated definition. If still fails → revert, proceed to F2.
   - No match → proceed to F2 (fix via Skill)
   - **Limit:** Max 2 definition updates per screen.

**Step F2: Invoke /fix-loop Skill (Single Fix mode)**

**MANDATORY: Use the Skill tool** to invoke `/fix-loop`. Do NOT read fix-loop.md and follow it inline — you MUST invoke it as a Skill.

Invoke: `skill: "fix-loop"` with these arguments:
```
failure_output:             {issue description + XML evidence + screenshot path + logcat}
failure_context:            "ADB test: screen={screen_name}, issue={issue_id} ({severity})"
files_of_interest:          {relevant source files for this screen}
build_command:              "./gradlew assembleDebug"
install_command:            "$ADB install -r android/app/build/outputs/apk/debug/app-debug.apk"
attempt_number:             {current attempt for this issue}
previous_attempts_summary:  {summary of prior attempts from iteration logs}
prohibited_actions:         ["Delete UI elements", "Weaken checklist", "Skip testing", "Mark PASS with issues", "Fix-later issues", "Downgrade issues to observations", "Skip Pre-Classification Gate", "Classify PASS with visual_verified=false AND missing/failed elements"]
fix_target:                 "production"
log_dir:                    ".claude/logs/adb-test/"
session_id:                 {current session id}
```
Budget rationale: Single Fix mode (no `retest_command`) — caller retests via ADB. 3 attempts per issue allows escalation through normal → thinkhard → ultrathink (see fix-loop.md Thinking Escalation).

Collect output: if `fix_applied` → append to `all_fixes[]`. If `revert_applied` or `fix_applied == false` → log, proceed to F5.

**Step F3: Relaunch App & Navigate** (caller responsibility)
```bash
$ADB shell am force-stop $APP_PACKAGE
$ADB shell am start -n $APP_PACKAGE/$APP_ACTIVITY
```
Wait for Home, navigate back to screen under test.

**Step F4: Retest** (caller responsibility)

Repeat full screen protocol (E1-E6.5) for this screen.

**Step F5: Per-Issue Increment**
- Issue RESOLVED and others remain → next OPEN issue (reset attempt counter)
- Issue exhausted (3 attempts) → UNRESOLVED, next OPEN issue
- ALL issues resolved → exit with PASS
- Screen total >= 12 → exit, classify remaining issues
- **PARTIAL** — some resolved, some not

---

## REGRESSION TESTING (after Fix Loop)

**Trigger:** `total_fixes > 0` AND at least one screen PASSED.

For each previously-passed screen (in execution order):

**R1. Navigate** — E1 path. **R2. Verify Arrival** — E2 (including crash detection). If crash → REGRESSED. **R3. Element Spot-Check** — E3 only (skip E5 for speed). **R4. Classify:** All present → REGRESSION_PASS. Missing/crash → REGRESSED.

Track: `regression_screens_tested`, `regression_passes`, `regressions_found`.

Regressions are logged for manual review — NOT auto-fixed.

---

## LOGGING

### Session Directory

Create at start: `.claude/logs/adb-test/{YYYYMMDD_HHMMSS}/`

### Per-Iteration Log

File: `iteration-{NNN}-{screen}.md` — Contains metadata, UI dump analysis, screenshot analysis, interaction results, issues found, root cause, fix applied, retest result.

### Per-Screen Summary

File: `screen-{name}-summary.md` — Final status, total iterations, fix history, remaining issues, screenshots.

---

> **REMINDER: After all tests complete, if fixes were applied, use the Skill tool to invoke `/post-fix-pipeline`. Do NOT commit manually.**

---

## TRACKING VARIABLES

```
// Screen tracking
screens_tested = 0; screens_passed = 0; screens_failed = 0; screens_blocked = 0; screens_unresolved = 0
per_screen_results = {}   // screen_name → { status, iterations, fixes[], issues[] }
per_screen_issues = {}    // screen_name → [ { id, severity, description, status, attempts } ]

// Fix tracking
total_fixes = 0; all_fixes = []; total_iterations = 0; definition_updates = 0

// Fix-loop metrics (accumulated from Skill outputs)
fix_loop_metrics = { debugger_invocations: 0, code_reviews: 0, code_reviews_approved: 0, code_reviews_flagged: 0, review_issues: [], build_failures: 0, reverts: 0 }

// Backend, logcat, visual, regression, pipeline tracking
backend_health_checks = 0; backend_restarts = 0; logcat_captures = 0
blank_screenshots = 0; visual_verified_screens = {}
regression_screens_tested = 0; regression_passes = 0; regressions_found = 0
pipeline_status = "NOT_RUN"; test_suite_gate = "NOT_RUN"; commit_hash = ""; commit_message = ""
start_time = now(); per_screen_times = {}
```

---

## SKILL INTEGRATION

| Skill | Trigger | Purpose |
|-------|---------|---------|
| `/fix-loop` | Step F2, for each issue (Single Fix mode) | Analyze root cause, apply fix, code review gate, rebuild |
| `/post-fix-pipeline` | Post-run, if any fixes were applied | Test suite verification + documentation + git commit |

**How Skills are invoked:** Via the **Skill tool** — NOT by reading the .md file. See Step F2 and Post-Run Pipeline below.

### Post-Run Pipeline

After all screens complete (or single screen), check if fixes were applied.

**If `len(all_fixes) == 0`**: skip — no changes to commit.

**If `len(all_fixes) > 0`**: **Use the Skill tool** to invoke `/post-fix-pipeline`:

Invoke: `skill: "post-fix-pipeline"` with these arguments:
```
fixes_applied:            {all_fixes from tracking}
files_changed:            {all modified file paths}
session_summary:          "ADB test run: {N} fixes across {screens}"
regression_commands:      []   (ADB regression R1-R4 runs inline, not delegated)
test_suite_commands:      [
  { name: "backend", command: "cd backend && PYTHONPATH=. pytest --tb=short -q", timeout: 300 },
  { name: "android-unit", command: "cd android && ./gradlew test --console=plain", timeout: 600 }
]
test_suite_max_fix_attempts: 2
docs_instructions:        "Update docs/CONTINUE_PROMPT.md with session summary"
commit_format:            "fix(adb-test): {summary}"
commit_scope:             "adb-test"
push:                     false
```

**Do NOT commit manually. Do NOT read post-fix-pipeline.md and follow it inline.**

Collect pipeline output for the final report: `test_suite_gate`, commit hash/message.

---

## FINAL REPORT

```
====================================================================
  ADB MANUAL E2E TEST REPORT
====================================================================
Screen  1: auth-flow        -> PASS (0 fixes)
Screen  2: home             -> PASS (1 fix, 2 iterations)
...
Screen 12: recipe-rules     -> PASS (0 fixes)
--------------------------------------------------------------------
TOTAL: X/12 passed | X fixes | X iterations | X blocked | X unresolved
====================================================================

Fixes Applied:
  1. [file:line] - {root cause and fix description}

Definition Updates (if any):
  1. [screen] — Updated expected value: "{old}" → "{new}" in adb-test-definitions.md

Unresolved Issues:
  - [screen] — {description} (3 attempts per issue or 12 total exhausted)

Blocked Screens:
  - [screen] — {reason}

Backend Health:
  - Health checks: X, Restarts: X

Regression Testing (if fixes applied):
  - Screens retested: X, Passes: X, Regressions: X

Test Suite Verification (from /post-fix-pipeline):
  - Gate: PASSED | PASSED_AFTER_FIX | FAILED | NOT_RUN

Skill Activity (from /fix-loop + /post-fix-pipeline):
  - Fix-loop invocations: X
  - Debugger invocations: X
  - Code reviews: X (Y approved, Z flagged)
  - Pipeline status: COMPLETED | BLOCKED_BY_TEST_SUITE | NOT_RUN
  - Commit: [hash] — [message]

Session logs: .claude/logs/adb-test/{session}/
Duration: X minutes Y seconds
```

---

## QUICK REFERENCE

| Screen | Nav From | Backend? | Key Interactions |
|--------|---------|----------|-----------------|
| `auth-flow` | App launch | Yes | Sign-in, 5 onboarding steps |
| `home` | Post-auth | Yes | Day tabs, meal cards, lock, refresh, add, bottom nav |
| `grocery` | Bottom nav | Room | Categories, checkboxes, WhatsApp share |
| `chat` | Bottom nav | Yes (AI) | Type message, send, wait for response |
| `favorites` | Bottom nav | Room | Tabs, recipe cards (or empty state) |
| `stats` | Bottom nav | Room | Time tabs, streak, chart |
| `settings` | Profile icon | No | Toggles, sign-out, links to Pantry/Rules |
| `notifications` | Bell icon | Room | Filters, mark all read (or empty) |
| `recipe-detail` | Meal card tap | Yes | Favorite, servings, start cooking |
| `cooking-mode` | Recipe detail | No | Step navigation, complete |
| `pantry` | Settings link | Room | Add item (or empty) |
| `recipe-rules` | Settings link | Yes | Tabs, add rule, delete |

---

## SECTION G: FLOW EXECUTION PROTOCOL

When `$ARGUMENTS` matches a flow name, execute the corresponding flow definition file.

### G1. Load Flow Definition

```
FLOW_DIR=docs/testing/flows
```

| Flow Name | File |
|-----------|------|
| `new-user-journey` | `flow01-new-user-journey.md` |
| `existing-user` | `flow02-existing-user.md` |
| `recipe-interaction` | `flow03-recipe-interaction.md` |
| `chat-ai` | `flow04-chat-ai.md` |
| `grocery-management` | `flow05-grocery-management.md` |
| `offline-mode` | `flow06-offline-mode.md` |
| `edge-cases` | `flow07-edge-cases.md` |
| `dark-mode` | `flow08-dark-mode.md` |
| `pantry-rules-crud` | `flow09-pantry-rules-crud.md` |
| `stats-tracking` | `flow10-stats-tracking.md` |

Read the flow definition file: `$FLOW_DIR/{flow-file}.md`

### G2. Check Flow Prerequisites

Each flow has a **Prerequisites** section. Verify:
1. Standard D1-D7 prerequisites (same as screen tests)
2. Flow-specific prerequisites (e.g., Flow 2 requires Flow 1 state — do NOT clean test data)
3. **Depends On:** If the flow depends on another flow, verify that flow's state exists

Special handling:
- "Do NOT run cleanup_user.py" → skip D4
- "needs existing user" → skip D4 and D6

### G3. Execute Steps

Follow the flow's **Steps** section sequentially using the 13 ADB patterns from `docs/testing/adb-patterns.md`.

Per-step execution:
1. Read step's **Action** column
2. Execute using appropriate ADB pattern
3. Dump UI and verify **Expected** column
4. If **Screenshot** column has filename → capture to `$SCREENSHOT_DIR/{filename}`
5. If **Validation** column specifies check → run it (G4)
6. Run crash/ANR detection (Pattern 9) after major navigation

Between phases: `✅ Phase {X} Complete: {N}/{total} steps passed`

### G4. Run Validation

When a step's Validation says "V4a-V4k":
```bash
cd D:/Abhay/VibeCoding/KKB
python scripts/validate_meal_plan.py --jwt "$JWT" {args from flow's Validation Checkpoints}
```

JWT acquisition:
```bash
JWT=$(curl -s -X POST http://localhost:8000/api/v1/auth/firebase \
  -H 'Content-Type: application/json' \
  -d '{"firebase_token":"fake-firebase-token"}' | \
  python -c 'import sys,json;print(json.load(sys.stdin).get("access_token",""))')
```

Result handling: exit 0 → pass, exit 1 → HARD failure (log, continue), exit 2 → SOFT warnings (log, continue).

### G5. Per-Step Issue Detection

A step **fails** when:
- Expected text/element not found in XML
- Expected behavior did not occur
- Crash/ANR detected
- Validation exit code 1
- Any behavioral deviation from Expected column
- Screenshot blank AND expected result unverifiable via XML

**"Behavioral deviation" includes:** AI not performing expected actions, missing conflict detection, data not persisting as expected.

> **REMINDER: When a flow step fails, you MUST use the Skill tool to invoke `/fix-loop`. Do NOT fix issues inline. No exceptions.**

When a step fails, **use the Skill tool** to invoke `/fix-loop`:

Invoke: `skill: "fix-loop"` with arguments:
```
failure_output:             {step failure description + XML evidence + screenshot + logcat}
failure_context:            "ADB flow test: flow={flow_name}, step={step_id}"
files_of_interest:          {from the flow's Fix Strategy section}
build_command:              "./gradlew assembleDebug"
install_command:            "$ADB install -r android/app/build/outputs/apk/debug/app-debug.apk"
attempt_number:             {current attempt for this step}
previous_attempts_summary:  {summary of prior attempts}
prohibited_actions:         ["Delete UI elements", "Weaken checklist", "Skip testing", "Mark PASS with issues"]
fix_target:                 "production"
log_dir:                    ".claude/logs/adb-test/"
session_id:                 {current session id}
```

**Max 3 attempts per step.** After fix, re-execute the failed step (not entire flow). If still fails after 3 → UNRESOLVED, continue.

**No "PASS with observation" for flow steps.** Expected X, observed Y = FAILED.

### G6. Flow Report

```
====================================================================
  ADB FLOW TEST REPORT: {flow-name}
====================================================================
Phase A: {name}          -> {N}/{total} steps PASS
Phase B: {name}          -> {N}/{total} steps PASS
...
--------------------------------------------------------------------
Validation Checkpoints:
  Checkpoint 1: {V4a-V4k results}
Contradictions Tested:
  C{N}: {description} -> {PASS/FAIL/WARNING}
--------------------------------------------------------------------
TOTAL: {passed}/{total} steps | {fixes} fixes | {screenshots} screenshots
Duration: X minutes Y seconds
Session logs: .claude/logs/adb-test/{session}/
====================================================================
```

### G7. All-Flows Mode

**`all-flows`:** Run flow01 → flow10 in order. Do NOT run `cleanup_user.py` between flows. Only D4 before flow01. Combined report at end.

**`all-flows-from <name>`:** Find flow number, run from there onwards. Assume prior state exists.

---

## FLOW QUICK REFERENCE

| # | Flow | Screens | Contradictions | Duration | Key Feature |
|---|------|---------|----------------|----------|-------------|
| 1 | `new-user-journey` | 13 | C1-C5 | 15-25 min | Full onboarding + 2 meal plans |
| 2 | `existing-user` | 4 | — | 8-12 min | Persistence + plan #3 |
| 3 | `recipe-interaction` | 4 | — | 5-8 min | Favorite, cook, unfavorite |
| 4 | `chat-ai` | 3 | C6-C12 | 8-15 min | Chat + tool calling |
| 5 | `grocery-management` | 2 | C13 | 4-6 min | Categories, checkboxes, share |
| 6 | `offline-mode` | 5 | C14-C15 | 6-10 min | WiFi off, Room cache |
| 7 | `edge-cases` | 10 | C16-C21 | 5-8 min | Rapid nav, back stack |
| 8 | `dark-mode` | 6 | — | 4-6 min | Theme toggle + visual |
| 9 | `pantry-rules-crud` | 3 | C22-C27 | 8-12 min | CRUD + duplicate prevention |
| 10 | `stats-tracking` | 1 | — | 3-5 min | Streak, chart, tabs |
