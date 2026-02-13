# Implement Feature/Fix

Implement the requested feature or fix following the **mandatory 7-step workflow**.

**Request:** $ARGUMENTS

---

## STEP 0: Initialize Workflow State

Before any other action, initialize the workflow tracking state so hooks can enforce the pipeline:

```bash
# Hooks will auto-initialize on first tool use, but explicitly set activeCommand
python -c "
import json, os
state_file = '.claude/workflow-state.json'
if os.path.exists(state_file):
    with open(state_file) as f:
        d = json.load(f)
    d['activeCommand'] = 'implement'
    with open(state_file, 'w') as f:
        json.dump(d, f, indent=2)
"
```

This marks the session as an `implement` workflow. Hooks will:
- Track all Skill invocations (fix-loop, post-fix-pipeline)
- Independently verify test results via re-run
- Block commits if required evidence artifacts are missing

---

## MANDATORY WORKFLOW

You MUST follow these 7 steps in order. Do NOT skip any step.

### STEP 1: Update Requirement Documentation

1. Search for existing issue:
   ```bash
   gh issue list --search "$ARGUMENTS"
   ```

2. If no issue exists, create one:
   ```bash
   gh issue create --title "Feature: $ARGUMENTS" --body "## Description

   [Describe the feature/fix]

   ## Acceptance Criteria

   - [ ] [Criterion 1]
   - [ ] [Criterion 2]
   "
   ```

3. Add requirement to the appropriate screen document in `docs/requirements/screens/` using BDD format:
   ```markdown
   ### SCREEN-XXX: [Feature Name]

   **Given** [precondition]
   **When** [action]
   **Then** [expected result]
   ```

4. Add traceability entry to `docs/testing/Functional-Requirement-Rule.md`

**Output Required:**
```
✅ Step 1 Complete:
- GitHub Issue: #XX (created/existing)
- Requirement ID: SCREEN-XXX
- Traceability: Added to Functional-Requirement-Rule.md
```

---

### STEP 2: Create/Update Tests

Based on the acceptance criteria:

1. Create E2E test file in `app/src/androidTest/java/com/rasoiai/app/e2e/flows/`
2. Add KDoc header:
   ```kotlin
   /**
    * Requirement: #XX - [Description from issue]
    *
    * Tests the acceptance criteria defined in the issue.
    */
   class [Feature]FlowTest : BaseE2ETest() {
       // Test methods matching each acceptance criterion
   }
   ```

**Output Required:**
```
✅ Step 2 Complete:
- Test file: [Name]FlowTest.kt
- Test methods: [list of methods]
```

---

### STEP 3: Implement the Feature

Write the minimum code necessary to make the tests pass.

Follow patterns from CLAUDE.md:
- Hilt for dependency injection
- StateFlow for state management
- Room for local storage (offline-first)

**Output Required:**
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
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.[TestClass]
```

**Backend (if applicable):**
```bash
PYTHONPATH=. pytest tests/test_[feature].py -v
```

**Output Required:**
```
✅ Step 4 Complete:
- Tests run: X
- Tests passed: X
- Tests failed: X
```

---

### STEP 5: Fix Loop (via /fix-loop Skill)

> **ENFORCEMENT GATE:** Hooks track whether you invoke `/fix-loop` via the Skill tool. If test failures were detected and you fix issues inline without using the Skill tool, the `verify-evidence-artifacts.sh` hook will **block your commit**. You MUST use `Skill("fix-loop")`.

IF any tests fail — **regardless of whether the failure is known or pre-existing** — **use the Skill tool** to invoke `/fix-loop` in Full Loop mode. Do NOT read fix-loop.md and follow it inline.

Invoke: `skill: "fix-loop"` with arguments:
```
failure_output:         {raw test failure output from Step 4}
failure_context:        {what was tested and what was expected}
files_of_interest:      {files modified in Step 3}
build_command:          {same build command used in Step 4, if applicable}
retest_command:         {same test command used in Step 4}
retest_timeout:         300
max_iterations:         10
max_attempts_per_issue: 3
prohibited_actions:     ["@Ignore", "weaken assertions", "delete tests", "fix-later issues"]
fix_target:             "either"
log_dir:                ".claude/logs/fix-loop/"
```
Budget rationale: `max_iterations: 10` with `max_attempts_per_issue: 3` — generous budget for feature implementation where multiple issues may surface.

The /fix-loop Skill will iterate until all tests pass or budget is exhausted.

**CRITICAL:** Do NOT proceed to Step 6 until the fix-loop process returns status **RESOLVED** (all tests passing).

If it returns UNRESOLVED or MAX_ITERATIONS_EXCEEDED, report the failure and STOP.

**Output Required:**
```
✅ Step 5 Complete:
- Fix-loop status: RESOLVED
- Iterations: X
- Issues fixed: X
- Final result: ALL TESTS PASSING (X/X)
```

---

### STEP 6: Capture Screenshots

Capture before and after screenshots to `docs/testing/screenshots/`:

**Android:**
```bash
# Before (should have been captured in Step 1)
adb exec-out screencap -p > docs/testing/screenshots/[issue]_[feature]_before.png

# After
adb exec-out screencap -p > docs/testing/screenshots/[issue]_[feature]_after.png
```

**Web:**
```javascript
await browser_take_screenshot({
  filename: "docs/testing/screenshots/[issue]_[feature]_after.png",
  type: "png"
})
```

**Output Required:**
```
✅ Step 6 Complete:
- Before: docs/testing/screenshots/XX_before.png
- After: docs/testing/screenshots/XX_after.png
```

---

### STEP 7: Verify and Post-Fix Pipeline

> **ENFORCEMENT GATE:** Hooks track whether you invoke `/post-fix-pipeline` via the Skill tool. If tests were run and you attempt to commit without invoking the pipeline, the `verify-evidence-artifacts.sh` hook will **block your commit**. You MUST use `Skill("post-fix-pipeline")`.

1. Read both screenshots using the Read tool
2. Describe the visible difference between before and after
3. **Use the Skill tool** to invoke `/post-fix-pipeline`. Do NOT commit manually.

Invoke: `skill: "post-fix-pipeline"` with arguments:
```
fixes_applied:            {list of changes from Steps 3+5}
files_changed:            {all modified file paths}
session_summary:          "Implement: #{issue_number} - {description}"
test_suite_commands:      [
  { name: "backend", command: "cd backend && PYTHONPATH=. pytest --tb=short -q", timeout: 300 },
  { name: "android-unit", command: "cd android && ./gradlew test --console=plain", timeout: 600 }
]
test_suite_max_fix_attempts: 2
docs_instructions:        "Update docs/testing/Functional-Requirement-Rule.md with test links. Update docs/CONTINUE_PROMPT.md with session summary."
commit_format:            "feat({scope}): {summary}\n\nFix #{issue_number}"
commit_scope:             "{feature-area}"
push:                     false
```

The /post-fix-pipeline Skill handles: test suite verification gate, documentation updates, and git commit with Co-Authored-By tag.

**Final Output Required:**
```
✅ WORKFLOW COMPLETE:
- GitHub Issue: #XX
- Requirement: SCREEN-XXX
- Tests: X/X passed
- Screenshots:
  - Before: docs/testing/screenshots/XX_before.png
  - After: docs/testing/screenshots/XX_after.png
- Verification: [describe visible change]
- Pipeline: COMPLETED | BLOCKED_BY_TEST_SUITE
- Commit: [hash] — [message]

The feature has been implemented and all tests pass.
```

---

## SELF-ENFORCEMENT GATES

Before proceeding past each major phase, answer these questions:

**Pre-Implementation Gate (Before Step 3):**
```
□ Step 1 complete (Requirements)? → [YES / NO - STOP]
□ Step 2 complete (Tests created)? → [YES / NO - STOP]
□ BEFORE screenshot captured? → [YES: path / NO - capture now]
□ Issue number noted? → [YES: #___ / NO - STOP]
```

**Pre-Commit Gate (Before Step 7 commit):**
```
□ ALL tests passing? → [YES: X/X passed / NO - STOP]
□ AFTER screenshot captured? → [YES: path / NO - STOP]
□ Before/after compared? → [YES: difference is ___ / NO - STOP]
```

---

## VIOLATIONS

The following are violations of the workflow:

- Skipping any step
- Committing with failing tests
- Using @Ignore to bypass test failures
- Creating "fix later" issues instead of fixing
- Proceeding without screenshots

**VIOLATION = PROCESS FAILURE. No exceptions.**
