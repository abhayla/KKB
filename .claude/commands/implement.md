# Implement Feature/Fix

Implement the requested feature or fix following the **mandatory 7-step workflow**.

**Request:** $ARGUMENTS

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

### STEP 5: Fix Loop

IF tests fail:
1. Analyze failure output
2. Fix the code
3. Re-run tests
4. Repeat until ALL tests pass

**CRITICAL:** Do NOT proceed to Step 6 until ALL tests pass.

**Output Required:**
```
✅ Step 5 Complete:
- Iterations: X
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

### STEP 7: Verify and Confirm

1. Read both screenshots using the Read tool
2. Describe the visible difference between before and after
3. Confirm the feature has been implemented correctly
4. Create the commit

**Commit Format:**
```bash
git commit -m "$(cat <<'EOF'
Fix #XX: [Brief description]

- [Change 1]
- [Change 2]

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
EOF
)"
```

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
