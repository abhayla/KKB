#!/bin/bash
# =============================================================================
# Claude Code Workflow Validation Hook
# =============================================================================
# Purpose: Enforce the mandatory 7-step development workflow by blocking
#          actions that skip required steps.
#
# Usage: Called by Claude hooks system with tool name and arguments
#        .claude/hooks/validate-workflow-step.sh <TOOL_NAME> [TOOL_ARGS]
#
# Exit Codes:
#   0 - Validation passed, allow action
#   1 - Validation failed, block action
# =============================================================================

WORKFLOW_STATE=".claude/workflow-state.json"
TOOL_NAME="${1:-}"
TOOL_ARGS="${2:-}"

# -----------------------------------------------------------------------------
# Helper Functions
# -----------------------------------------------------------------------------

log_message() {
    local timestamp
    timestamp=$(date -Iseconds 2>/dev/null || date +"%Y-%m-%dT%H:%M:%S")
    echo "[$timestamp] $1" >> ".claude/logs/workflow-sessions.log" 2>/dev/null
}

init_workflow_state() {
    local session_id
    session_id=$(date +%Y%m%d-%H%M%S 2>/dev/null || echo "session-$$")

    mkdir -p .claude/logs 2>/dev/null

    cat > "$WORKFLOW_STATE" << EOF
{
  "sessionId": "$session_id",
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
    log_message "SESSION_START | id=$session_id"
}

get_step_status() {
    local step_name="$1"
    if [ -f "$WORKFLOW_STATE" ]; then
        # Use grep/sed as fallback if jq not available
        if command -v jq &> /dev/null; then
            jq -r ".steps.${step_name}.completed" "$WORKFLOW_STATE" 2>/dev/null || echo "false"
        else
            grep -o "\"${step_name}\".*\"completed\":[^,}]*" "$WORKFLOW_STATE" 2>/dev/null | grep -o "true\|false" | head -1 || echo "false"
        fi
    else
        echo "false"
    fi
}

check_all_steps_complete() {
    local steps=("step1_requirements" "step2_tests" "step3_implement" "step4_runTests" "step5_fixLoop" "step6_screenshots" "step7_verify")
    for step in "${steps[@]}"; do
        local status
        status=$(get_step_status "$step")
        if [ "$status" != "true" ]; then
            echo "$step"
            return 1
        fi
    done
    echo "all"
    return 0
}

is_test_file() {
    local file_path="$1"
    if echo "$file_path" | grep -qE "(androidTest|test_|Test\.kt|_test\.py)"; then
        return 0
    fi
    return 1
}

is_code_file() {
    local file_path="$1"
    if echo "$file_path" | grep -qE "\.(kt|py|java|xml)$"; then
        # Exclude test files
        if is_test_file "$file_path"; then
            return 1
        fi
        return 0
    fi
    return 1
}

is_requirement_file() {
    local file_path="$1"
    if echo "$file_path" | grep -qE "(docs/requirements|Functional-Requirement)"; then
        return 0
    fi
    return 1
}

# -----------------------------------------------------------------------------
# Main Validation Logic
# -----------------------------------------------------------------------------

# Initialize workflow state if it doesn't exist
if [ ! -f "$WORKFLOW_STATE" ]; then
    init_workflow_state
fi

case "$TOOL_NAME" in
    "Write"|"Edit")
        # Determine what type of file is being modified
        FILE_PATH="$TOOL_ARGS"

        # Allow requirement documentation updates (Step 1)
        if is_requirement_file "$FILE_PATH"; then
            log_message "STEP_1_PROGRESS | file=$FILE_PATH"
            exit 0
        fi

        # Writing test files requires Step 1 to be complete
        if is_test_file "$FILE_PATH"; then
            STEP1_STATUS=$(get_step_status "step1_requirements")
            if [ "$STEP1_STATUS" != "true" ]; then
                echo ""
                echo "========================================"
                echo "  WORKFLOW VALIDATION - STEP BLOCKED"
                echo "========================================"
                echo ""
                echo "Cannot create/modify tests before completing Step 1."
                echo ""
                echo "Required actions:"
                echo "  1. Check/create GitHub Issue: gh issue list --search \"keyword\""
                echo "  2. Add requirement to docs/requirements/screens/*.md"
                echo "  3. Add entry to docs/testing/Functional-Requirement-Rule.md"
                echo ""
                echo "After completing Step 1, output:"
                echo "  ✅ Step 1 Complete:"
                echo "  - GitHub Issue: #XX"
                echo "  - Requirement ID: SCREEN-XXX"
                echo ""
                log_message "BLOCKED | reason=step2_before_step1 | file=$FILE_PATH"
                exit 1
            fi
            log_message "STEP_2_PROGRESS | testFile=$FILE_PATH"
            exit 0
        fi

        # Writing code files requires Step 2 to be complete
        if is_code_file "$FILE_PATH"; then
            STEP2_STATUS=$(get_step_status "step2_tests")
            if [ "$STEP2_STATUS" != "true" ]; then
                echo ""
                echo "========================================"
                echo "  WORKFLOW VALIDATION - STEP BLOCKED"
                echo "========================================"
                echo ""
                echo "Cannot implement code before completing Step 2."
                echo ""
                echo "Required actions:"
                echo "  1. Create test file in app/src/androidTest/java/com/rasoiai/app/e2e/flows/"
                echo "  2. Add KDoc header: /** Requirement: #XX - Description */"
                echo "  3. Write test methods matching acceptance criteria"
                echo ""
                echo "After completing Step 2, output:"
                echo "  ✅ Step 2 Complete:"
                echo "  - Test file: XXXFlowTest.kt"
                echo "  - Test methods: [list]"
                echo ""
                log_message "BLOCKED | reason=step3_before_step2 | file=$FILE_PATH"
                exit 1
            fi
            log_message "STEP_3_PROGRESS | codeFile=$FILE_PATH"
            exit 0
        fi

        # Allow other file types (documentation, config, etc.)
        exit 0
        ;;

    "Bash")
        if [ "$TOOL_ARGS" = "git-commit" ]; then
            # Check if all steps are complete
            INCOMPLETE_STEP=$(check_all_steps_complete)
            if [ "$INCOMPLETE_STEP" != "all" ]; then
                echo ""
                echo "========================================"
                echo "  WORKFLOW VALIDATION - COMMIT BLOCKED"
                echo "========================================"
                echo ""
                echo "Cannot commit. Workflow incomplete."
                echo ""
                echo "Incomplete step: $INCOMPLETE_STEP"
                echo ""
                echo "Complete all 7 steps before committing:"
                echo "  1. Update requirement documentation"
                echo "  2. Create/update tests"
                echo "  3. Implement the feature"
                echo "  4. Run functional tests"
                echo "  5. Fix loop (until all tests pass)"
                echo "  6. Capture screenshots (before/after)"
                echo "  7. Verify and confirm"
                echo ""
                echo "See: docs/rules/Claude Code Enforced Workflow Rules.md"
                echo ""
                log_message "BLOCKED | reason=commit_workflow_incomplete | missing=$INCOMPLETE_STEP"
                exit 1
            fi
            log_message "COMMIT_ALLOWED | all_steps_complete"
            exit 0
        fi
        ;;
esac

# Default: allow action
exit 0
