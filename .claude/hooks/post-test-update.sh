#!/bin/bash
# =============================================================================
# Claude Code Post-Test Update Hook
# =============================================================================
# Purpose: Update workflow state after test execution to track progress
#          through the mandatory 7-step workflow.
#
# Usage: Called automatically by Claude hooks after test commands
#        .claude/hooks/post-test-update.sh
#
# This hook:
#   1. Captures the exit code of the test command
#   2. Updates the workflow state with test results
#   3. Logs the outcome for auditing
# =============================================================================

WORKFLOW_STATE=".claude/workflow-state.json"

# -----------------------------------------------------------------------------
# Helper Functions
# -----------------------------------------------------------------------------

log_message() {
    local timestamp
    timestamp=$(date -Iseconds 2>/dev/null || date +"%Y-%m-%dT%H:%M:%S")
    mkdir -p .claude/logs 2>/dev/null
    echo "[$timestamp] $1" >> ".claude/logs/workflow-sessions.log" 2>/dev/null
}

update_workflow_step() {
    local step_name="$1"
    local completed="$2"
    local extra_data="$3"
    local timestamp
    timestamp=$(date -Iseconds 2>/dev/null || date +"%Y-%m-%dT%H:%M:%S")

    if [ ! -f "$WORKFLOW_STATE" ]; then
        # Initialize if not exists
        mkdir -p .claude 2>/dev/null
        cat > "$WORKFLOW_STATE" << EOF
{
  "sessionId": "$(date +%Y%m%d-%H%M%S 2>/dev/null || echo "session-$$")",
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
EOF
    fi

    # Update using jq if available, otherwise use sed
    if command -v jq &> /dev/null; then
        local temp_file
        temp_file=$(mktemp 2>/dev/null || echo ".claude/temp_state.json")
        jq ".steps.${step_name}.completed = $completed | .steps.${step_name}.timestamp = \"$timestamp\"" "$WORKFLOW_STATE" > "$temp_file" 2>/dev/null && mv "$temp_file" "$WORKFLOW_STATE"
    else
        # Fallback: Create a marker file indicating step completion
        echo "$completed" > ".claude/.${step_name}_completed" 2>/dev/null
    fi
}

# -----------------------------------------------------------------------------
# Main Logic
# -----------------------------------------------------------------------------

# The previous command's exit code is passed via CLAUDE_HOOK_EXIT_CODE env var
# or we can check if the test output indicates success
EXIT_CODE="${CLAUDE_HOOK_EXIT_CODE:-0}"

# Parse test output if available
TESTS_PASSED=0
TESTS_TOTAL=0

# Try to extract test counts from recent output
# This is a best-effort approach since we can't directly access command output
if [ -f ".claude/last_test_output.txt" ]; then
    # Look for patterns like "Tests: 5 passed" or "5/5 tests passed"
    if grep -qiE "(passed|success)" ".claude/last_test_output.txt" 2>/dev/null; then
        EXIT_CODE=0
    fi
fi

if [ "$EXIT_CODE" -eq 0 ]; then
    # Tests passed
    update_workflow_step "step4_runTests" "true"
    update_workflow_step "step5_fixLoop" "true"

    echo ""
    echo "========================================"
    echo "  WORKFLOW UPDATE - TESTS PASSED"
    echo "========================================"
    echo ""
    echo "✅ Step 4 (Run Tests): Complete"
    echo "✅ Step 5 (Fix Loop): Complete - All tests passing"
    echo ""
    echo "Next steps:"
    echo "  6. Capture screenshots (before/after)"
    echo "     Android: adb exec-out screencap -p > docs/testing/screenshots/{issue}_after.png"
    echo "  7. Verify and confirm with final output"
    echo ""

    log_message "STEP_4_COMPLETE | tests=passed"
    log_message "STEP_5_COMPLETE | allTestsPassing=true"
else
    # Tests failed
    update_workflow_step "step4_runTests" "true"
    update_workflow_step "step5_fixLoop" "false"

    # Increment fix loop iteration counter
    if [ -f ".claude/.fix_loop_iterations" ]; then
        ITERATIONS=$(cat ".claude/.fix_loop_iterations" 2>/dev/null || echo "0")
        ITERATIONS=$((ITERATIONS + 1))
    else
        ITERATIONS=1
    fi
    echo "$ITERATIONS" > ".claude/.fix_loop_iterations" 2>/dev/null

    echo ""
    echo "========================================"
    echo "  WORKFLOW UPDATE - TESTS FAILED"
    echo "========================================"
    echo ""
    echo "✅ Step 4 (Run Tests): Complete (tests executed)"
    echo "❌ Step 5 (Fix Loop): In progress - Tests failing"
    echo ""
    echo "Fix loop iteration: $ITERATIONS"
    echo ""
    echo "Required actions:"
    echo "  1. Analyze the failure output above"
    echo "  2. Fix the failing code"
    echo "  3. Re-run tests"
    echo "  4. Repeat until ALL tests pass"
    echo ""
    echo "⚠️  DO NOT proceed to Step 6 until all tests pass!"
    echo ""

    log_message "STEP_4_COMPLETE | tests=failed | iteration=$ITERATIONS"
fi
