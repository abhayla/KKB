#!/bin/bash
# =============================================================================
# Claude Code Workflow Validation Hook (PreToolUse)
# =============================================================================
# Reads JSON from stdin. Exit 0 = allow, Exit 2 = block with message.
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/hook-utils.sh"
parse_hook_input

if [ -z "$HOOK_TOOL_NAME" ]; then exit 0; fi

get_step_status() {
    local step_name="$1"
    if [ -f "$WORKFLOW_STATE_FILE" ]; then
        if command -v jq &>/dev/null; then
            jq -r ".steps.${step_name}.completed" "$WORKFLOW_STATE_FILE" 2>/dev/null || echo "false"
        else
            grep -o "\"${step_name}\".*\"completed\":[^,}]*" "$WORKFLOW_STATE_FILE" 2>/dev/null | grep -o "true\|false" | head -1 || echo "false"
        fi
    else
        echo "false"
    fi
}

check_all_steps_complete() {
    local steps=("step1_requirements" "step2_tests" "step3_implement" "step4_runTests" "step5_fixLoop" "step6_screenshots" "step7_verify")
    for step in "${steps[@]}"; do
        local status; status=$(get_step_status "$step")
        if [ "$status" != "true" ]; then echo "$step"; return 1; fi
    done
    echo "all"; return 0
}

is_test_file() { echo "$1" | grep -qE "(androidTest|test_|Test\.kt|_test\.py)"; }
is_code_file() { echo "$1" | grep -qE "\.(kt|py|java|xml)$" && ! is_test_file "$1"; }
is_requirement_file() { echo "$1" | grep -qE "(docs/requirements|Functional-Requirement)"; }

# Graceful init: if no state file, create it and allow
if [ ! -f "$WORKFLOW_STATE_FILE" ]; then
    init_workflow_state "null"
    exit 0
fi

case "$HOOK_TOOL_NAME" in
    "Write"|"Edit")
        FILE_PATH=$(extract_input_field "file_path")
        if [ -z "$FILE_PATH" ]; then exit 0; fi
        if is_requirement_file "$FILE_PATH"; then log_event "STEP_1_PROGRESS" "file=$FILE_PATH"; exit 0; fi
        if echo "$FILE_PATH" | grep -qE "(\.claude/|\.github/|docs/rules|docs/design|CLAUDE\.md)"; then exit 0; fi
        if is_test_file "$FILE_PATH"; then
            if [ "$(get_step_status step1_requirements)" != "true" ]; then
                echo ""; echo "WORKFLOW BLOCKED: Cannot create tests before Step 1 (requirements)."
                echo "Run: gh issue list --search \"keyword\""; echo ""
                log_event "BLOCKED" "reason=step2_before_step1" "file=$FILE_PATH"; exit 2
            fi
            exit 0
        fi
        if is_code_file "$FILE_PATH"; then
            if [ "$(get_step_status step2_tests)" != "true" ]; then
                echo ""; echo "WORKFLOW BLOCKED: Cannot implement code before Step 2 (tests)."
                echo "Create test file first."; echo ""
                log_event "BLOCKED" "reason=step3_before_step2" "file=$FILE_PATH"; exit 2
            fi
            exit 0
        fi
        exit 0
        ;;
    "Bash")
        CMD=$(extract_input_field "command")
        if echo "$CMD" | grep -qE "git commit"; then
            INCOMPLETE=$(check_all_steps_complete)
            if [ "$INCOMPLETE" != "all" ]; then
                echo ""; echo "WORKFLOW BLOCKED: Cannot commit. Incomplete step: $INCOMPLETE"
                echo "Complete all 7 steps. See: docs/rules/Claude Code Enforced Workflow Rules.md"; echo ""
                log_event "BLOCKED" "reason=commit_incomplete" "missing=$INCOMPLETE"; exit 2
            fi
            ACTIVE_CMD=$(get_state_field ".activeCommand")
            if [ -n "$ACTIVE_CMD" ] && [ "$ACTIVE_CMD" != "null" ] && [ "$ACTIVE_CMD" != "None" ]; then
                TC=$(python -c "import json;print(len(json.load(open('$WORKFLOW_STATE_FILE')).get('evidence',{}).get('testRuns',[])))" 2>/dev/null)
                if [ "${TC:-0}" -gt 0 ]; then
                    PI=$(get_state_field ".skillInvocations.postFixPipelineInvoked")
                    if [ "$PI" != "true" ] && [ "$PI" != "True" ]; then
                        echo ""; echo "COMMIT BLOCKED: /post-fix-pipeline was not invoked."
                        echo "Use Skill tool to invoke /post-fix-pipeline first."; echo ""
                        log_event "BLOCKED" "reason=pipeline_not_invoked"; exit 2
                    fi
                fi
            fi
            log_event "COMMIT_ALLOWED"; exit 0
        fi
        ;;
esac

exit 0
