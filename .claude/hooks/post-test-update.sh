#!/bin/bash
# =============================================================================
# Claude Code Post-Test Update Hook (PostToolUse)
# =============================================================================
# Records test results in workflow state. Exit 0 always (never blocks).
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/hook-utils.sh"
parse_hook_input

if [ "$HOOK_TOOL_NAME" != "Bash" ]; then exit 0; fi

CMD=$(extract_input_field "command")
if ! is_test_command "$CMD"; then exit 0; fi

TARGET=$(extract_test_target "$CMD")
RESULT=$(detect_test_result "$HOOK_TOOL_OUTPUT")

# Detect test platform for learning system enrichment
PLATFORM="unknown"
if echo "$CMD" | grep -qiE "pytest|python.*test"; then PLATFORM="backend"
elif echo "$CMD" | grep -qiE "gradlew.*connectedDebug"; then PLATFORM="android-e2e"
elif echo "$CMD" | grep -qiE "gradlew.*test"; then PLATFORM="android-unit"
fi

if [ ! -f "$WORKFLOW_STATE_FILE" ]; then init_workflow_state "null"; fi

if [ "$RESULT" = "pass" ]; then
    update_workflow_state '.steps.step4_runTests.completed = true'
    update_workflow_state '.steps.step5_fixLoop.completed = true'
    echo ""; echo "WORKFLOW: Tests PASSED. Steps 4+5 complete."
    echo "Next: Step 6 (screenshots), Step 7 (verify+commit)"; echo ""
    log_event "STEP_4_COMPLETE" "tests=passed" "target=$TARGET"
elif [ "$RESULT" = "fail" ]; then
    update_workflow_state '.steps.step4_runTests.completed = true'
    IT=$(get_state_field ".steps.step5_fixLoop.iterations"); IT=${IT:-0}; IT=$((IT + 1))
    update_workflow_state ".steps.step5_fixLoop.iterations = $IT"
    echo ""; echo "WORKFLOW: Tests FAILED (iteration $IT). Use Skill(\"fix-loop\")."
    echo "DO NOT proceed to Step 6 until all tests pass."; echo ""
    log_event "STEP_4_COMPLETE" "tests=failed" "target=$TARGET" "iteration=$IT"
else
    update_workflow_state '.steps.step4_runTests.completed = true'
    log_event "TEST_RUN" "result=unknown" "target=$TARGET"
fi

append_test_run_evidence "$CMD" "$TARGET" "$RESULT"

TS=$(date +%Y%m%d-%H%M%S 2>/dev/null || echo "$$")
EJ=$(python -c "import json;print(json.dumps({'timestamp':'$TS','command':'$(echo "$CMD"|head -c 200)','target':'$TARGET','claimedResult':'$RESULT','platform':'$PLATFORM'}))" 2>/dev/null)
if [ -n "$EJ" ]; then write_evidence "$EVIDENCE_DIR" "run-${TS}.json" "$EJ" >/dev/null; fi

exit 0
