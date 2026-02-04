# Claude Code Enforced Workflow Rules

> **Version:** 1.0
> **Last Updated:** 2026-02-04
> **Purpose:** Mandatory 7-step development workflow for all code-related tasks

## Overview

This document defines the enforced development workflow that Claude Code must follow for all code-related tasks in the RasoiAI project. The workflow ensures proper documentation, test coverage, and verification before any code changes are committed.

## Enforcement Mechanism

The workflow is enforced through a hybrid system:

| Layer | Mechanism | Purpose |
|-------|-----------|---------|
| **Instructions** | CLAUDE.md Rule #7 | Claude follows these rules at session start |
| **Automation** | Claude hooks (`.claude/hooks/`) | Automated gates that block non-compliant actions |
| **Tracking** | Workflow state (`.claude/workflow-state.json`) | Session-specific progress tracking |

---

## Trigger Conditions

### APPLIES TO (Must Follow Workflow):
- Implementing a new feature
- Fixing a bug
- Refactoring code
- Making any code change (`.kt`, `.py`, `.xml` files)
- Any task that modifies functional behavior

### DOES NOT APPLY (Skip Workflow):
- Answering questions (no code changes)
- Documentation-only changes (no code)
- Research/exploration tasks
- Reading/reviewing code without modifications

---

## The 7-Step Mandatory Workflow

```
┌─────────────────────────────────────────────────────────────┐
│                    MANDATORY WORKFLOW                        │
├─────────────────────────────────────────────────────────────┤
│  1. UPDATE REQUIREMENTS  ────────────────────────────────►  │
│     • GitHub Issue (create or link)                         │
│     • docs/requirements/screens/*.md                        │
│     • Functional-Requirement-Rule.md                        │
│                                                             │
│  2. CREATE/UPDATE TESTS  ────────────────────────────────►  │
│     • E2E test file with acceptance criteria                │
│     • KDoc header linking to issue                          │
│                                                             │
│  3. IMPLEMENT FEATURE    ────────────────────────────────►  │
│     • Write code to make tests pass                         │
│                                                             │
│  4. RUN TESTS            ────────────────────────────────►  │
│     • Execute E2E/unit tests                                │
│                                                             │
│  5. FIX LOOP             ────────────────────────────────►  │
│     • If fail → fix → retest                                │
│     • Repeat until ALL pass                                 │
│                                                             │
│  6. CAPTURE SCREENSHOTS  ────────────────────────────────►  │
│     • Before: pre-implementation state                      │
│     • After: post-implementation state                      │
│                                                             │
│  7. VERIFY & CONFIRM     ────────────────────────────────►  │
│     • Compare screenshots                                   │
│     • Confirm visible change                                │
│     • Commit with issue reference                           │
└─────────────────────────────────────────────────────────────┘
```

---

## Step-by-Step Details

### STEP 1: Update Requirement Documentation

Before writing ANY code:

1. **Check for existing GitHub Issue:**
   ```bash
   gh issue list --search "keyword"
   ```

2. **Create Issue if none exists:**
   ```bash
   gh issue create --title "Feature: Description" --body "..."
   ```

3. **Add requirement to screen document:**
   - Location: `docs/requirements/screens/*.md`
   - Format: BDD-style (Given/When/Then)

4. **Add traceability entry:**
   - Location: `docs/testing/Functional-Requirement-Rule.md`
   - Link Issue → Requirement ID → Test file

**Required Output Format:**
```
✅ Step 1 Complete:
- GitHub Issue: #XX (created/existing)
- Requirement ID: HOME-XXX (or appropriate screen prefix)
- Traceability: Added to Functional-Requirement-Rule.md
```

---

### STEP 2: Create/Update Tests

Based on the requirement's acceptance criteria:

1. **Create E2E test file:**
   - Location: `app/src/androidTest/java/com/rasoiai/app/e2e/flows/`
   - Naming: `{Feature}FlowTest.kt`

2. **Add KDoc header:**
   ```kotlin
   /**
    * Requirement: #XX - Description
    *
    * Tests the acceptance criteria defined in the issue.
    */
   ```

3. **Write test methods matching acceptance criteria**

**Required Output Format:**
```
✅ Step 2 Complete:
- Test file: XXXFlowTest.kt
- Test methods: [list of test method names]
```

---

### STEP 3: Implement the Feature

Write the minimum code necessary to make tests pass.

**Guidelines:**
- Follow existing patterns in CLAUDE.md
- Use project architecture (Hilt, StateFlow, Room offline-first)
- Make minimal, focused changes

**Required Output Format:**
```
✅ Step 3 Complete:
- Files modified: [list]
- Key changes: [brief description]
```

---

### STEP 4: Run Functional Tests

Execute the tests:

**Android:**
```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.YourTestClass
```

**Backend:**
```bash
PYTHONPATH=. pytest tests/test_xxx.py -v
```

**Required Output Format:**
```
✅ Step 4 Complete:
- Tests run: X
- Tests passed: X
- Tests failed: X
- Output: [summary or link]
```

---

### STEP 5: Fix Loop

IF tests fail:
1. Analyze failure output
2. Fix the code
3. Re-run tests
4. Repeat until ALL tests pass

**CRITICAL:** Do NOT proceed to Step 6 until ALL tests pass.

**Required Output Format:**
```
✅ Step 5 Complete:
- Iterations: X
- Final result: ALL TESTS PASSING (X/X)
```

---

### STEP 6: Capture Screenshots

Platform-specific capture to `docs/testing/screenshots/`:

**Android (ADB):**
```bash
adb exec-out screencap -p > docs/testing/screenshots/{issue}_{feature}_{state}.png
```

**Web (Playwright):**
```javascript
await browser_take_screenshot({
  filename: "docs/testing/screenshots/{issue}_{feature}_{state}.png",
  type: "png"
})
```

**Required Captures:**
- `{issue}_before.png` - Pre-implementation state
- `{issue}_after.png` - Post-implementation state

**Required Output Format:**
```
✅ Step 6 Complete:
- Before: docs/testing/screenshots/XX_before.png
- After: docs/testing/screenshots/XX_after.png
```

---

### STEP 7: Verify and Confirm

1. **Read both screenshots** using the Read tool
2. **Describe the visible difference**
3. **Confirm feature implementation**
4. **Commit with proper message**

**Commit Format:**
```bash
git commit -m "$(cat <<'EOF'
Fix #XX: Brief description

- Change 1
- Change 2

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
EOF
)"
```

**Required Output Format:**
```
✅ WORKFLOW COMPLETE:
- GitHub Issue: #XX
- Requirement: SCREEN-XXX
- Tests: X/X passed
- Screenshots:
  - Before: docs/testing/screenshots/XX_before.png
  - After: docs/testing/screenshots/XX_after.png
- Verification: [describe visible change]

The feature has been implemented and all tests pass.
```

---

## Self-Enforcement Gates

Claude MUST answer these questions in its response before proceeding:

### Pre-Implementation Gate (Before Step 3):
```
□ Pre-Implementation Gate:
  - "Did I complete Step 1 (Requirements)?" → [YES / NO - STOP]
  - "Did I complete Step 2 (Tests)?" → [YES / NO - STOP]
  - "Did I capture a BEFORE screenshot?" → [YES with path / NO - STOP]
  - "Did I note the issue number?" → [YES: #___ / NO - STOP]
```

### Pre-Commit Gate (Before Step 7 commit):
```
□ Pre-Commit Gate:
  - "Did I complete Steps 4-5 (Tests passing)?" → [YES: X/X passed / NO - STOP]
  - "Did I capture an AFTER screenshot?" → [YES with path / NO - STOP]
  - "Did I compare before/after?" → [YES: difference is ___ / NO - STOP]
  - "Are ALL tests passing?" → [YES: X/X passed / NO - STOP]
```

---

## Hook Configuration

The following hooks enforce the workflow:

### Pre-Tool-Use Hooks

| Trigger | Hook | Purpose |
|---------|------|---------|
| `Write(*)` | `validate-workflow-step.sh` | Block test creation before Step 1 |
| `Edit(*)` | `validate-workflow-step.sh` | Block code edits before Step 2 |
| `Bash(git commit*)` | `validate-workflow-step.sh` | Block commits before Steps 1-7 |

### Post-Tool-Use Hooks

| Trigger | Hook | Purpose |
|---------|------|---------|
| `Bash(*gradlew*test*)` | `post-test-update.sh` | Update workflow state after tests |
| `Bash(*pytest*)` | `post-test-update.sh` | Update workflow state after tests |

---

## Workflow State Schema

File: `.claude/workflow-state.json` (auto-generated, gitignored)

```json
{
  "sessionId": "2026-02-04-abc123",
  "issueNumber": null,
  "requirementId": null,
  "steps": {
    "step1_requirements": { "completed": false, "timestamp": null, "artifacts": [] },
    "step2_tests": { "completed": false, "timestamp": null, "testFile": null },
    "step3_implement": { "completed": false, "timestamp": null, "filesChanged": [] },
    "step4_runTests": { "completed": false, "timestamp": null, "testsPassed": null, "testsTotal": null },
    "step5_fixLoop": { "completed": false, "iterations": 0, "allTestsPassing": false },
    "step6_screenshots": { "completed": false, "before": null, "after": null },
    "step7_verify": { "completed": false, "verification": null }
  },
  "blocked": false,
  "blockedReason": null
}
```

---

## Critical Rules - No Exceptions

1. **No Partial Test Passes**: Do NOT rationalize "2 out of 3 is good enough"
2. **No @Ignore Bypasses**: Do NOT mark failing tests as `@Ignore`
3. **No "Fix Later" Excuses**: Do NOT create "fix later" issues to bypass failures
4. **No Step Skipping**: Each step must complete before the next begins
5. **No Commits Without Tests**: Tests MUST pass before any commit
6. **No Screenshot Skipping**: Screenshots are MANDATORY for Steps 6-7, even when:
   - "Documenting existing behavior" - STILL REQUIRES SCREENSHOTS
   - "No code changes made" - STILL REQUIRES SCREENSHOTS
   - "Tests already pass" - STILL REQUIRES SCREENSHOTS
   - ADB/screenshot tools fail - MUST troubleshoot and retry, never skip

**VIOLATION = PROCESS FAILURE. No exceptions. No "I'll do it later."**

---

## Using the /implement Skill

Users can invoke the workflow with:
```
/implement <description>
```

Example:
```
/implement add logout button to settings screen
```

This triggers Claude to follow the full 7-step workflow automatically.

---

## Session Logging

Workflow progress is logged to `.claude/logs/workflow-sessions.log`:

```
[2026-02-04T10:30:00] SESSION_START | id=20260204-103000
[2026-02-04T10:31:15] STEP_1_COMPLETE | issue=#47 | requirement=HOME-043
[2026-02-04T10:35:22] STEP_2_COMPLETE | testFile=AutoFavoriteFlowTest.kt
[2026-02-04T10:45:00] STEP_4_COMPLETE | tests=3/3 passed
[2026-02-04T10:48:30] STEP_6_COMPLETE | before=47_before.png | after=47_after.png
[2026-02-04T10:50:00] WORKFLOW_COMPLETE | issue=#47 | duration=20m
```

---

## Related Documents

| Document | Location |
|----------|----------|
| CLAUDE.md Rule #7 | `CLAUDE.md` (Rules for Claude section) |
| Hook Scripts | `.claude/hooks/` |
| Implement Skill | `.claude/commands/implement.md` |
| Functional Requirements | `docs/testing/Functional-Requirement-Rule.md` |
| Screen Requirements | `docs/requirements/screens/` |
