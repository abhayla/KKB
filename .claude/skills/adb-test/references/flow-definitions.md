# Flow Execution Protocol

Detailed protocol for running user journey flows via ADB. Referenced from the main `adb-test` SKILL.md.

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
| `settings-deep-dive` | `flow11-settings-deep-dive.md` |
| `multi-family-medical` | `flow12-multi-family-medical.md` |
| `festival-meals` | `flow13-festival-meals.md` |
| `nutrition-goals` | `flow14-nutrition-goals.md` |
| `notifications-lifecycle` | `flow15-notifications.md` |
| `achievement-earning` | `flow16-achievements.md` |
| `pantry-suggestions` | `flow17-pantry-suggestions.md` |
| `photo-analysis` | `flow18-photo-analysis.md` |
| `multi-week-history` | `flow19-multi-week-history.md` |
| `recipe-scaling` | `flow20-recipe-scaling.md` |
| `recipe-rules-comprehensive` | `flow21-recipe-rules-comprehensive.md` |

Read the flow definition file: `$FLOW_DIR/{flow-file}.md`

### G2. Check Flow Prerequisites

Each flow has a **Prerequisites** section. Verify:
1. Standard D1-D7 prerequisites (same as screen tests)
2. Flow-specific prerequisites (e.g., Flow 2 requires Flow 1 state — do NOT clean test data)
3. **Depends On:** If the flow depends on another flow, verify that flow's state exists

Special handling:
- "Do NOT run cleanup_user.py" -> skip D4
- "needs existing user" -> skip D4 and D6

### G3. Execute Steps

Follow the flow's **Steps** section sequentially using the 13 ADB patterns from `docs/testing/adb-patterns.md`.

Per-step execution:
1. Read step's **Action** column
2. Execute using appropriate ADB pattern
3. Dump UI and verify **Expected** column
4. If **Screenshot** column has filename -> capture to `$SCREENSHOT_DIR/{filename}`
5. If **Validation** column specifies check -> run it (G4)
6. Run crash/ANR detection (Pattern 9) after major navigation

Between phases: `Phase {X} Complete: {N}/{total} steps passed`

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

Result handling: exit 0 -> pass, exit 1 -> HARD failure (log, continue), exit 2 -> SOFT warnings (log, continue).

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

**Max 3 attempts per step.** After fix, re-execute the failed step (not entire flow). If still fails after 3 -> UNRESOLVED, continue.

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

**`all-flows`:** Run flow01 -> flow11 in order. Do NOT run `cleanup_user.py` between flows. Only D4 before flow01. Combined report at end.

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
| 11 | `settings-deep-dive` | 13 | — | 15-20 min | 12 sub-screens, CRUD, preferences |
