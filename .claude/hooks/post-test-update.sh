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
if printf '%s' "$CMD" | grep -qiE "pytest|python.*test"; then PLATFORM="backend"
elif printf '%s' "$CMD" | grep -qiE "gradlew.*connectedDebug"; then PLATFORM="android-e2e"
elif printf '%s' "$CMD" | grep -qiE "gradlew.*test"; then PLATFORM="android-unit"
fi

if [ ! -f "$WORKFLOW_STATE_FILE" ]; then init_workflow_state "null"; fi

if [ "$RESULT" = "pass" ]; then
    update_workflow_state '.steps.step4_runTests.completed = true'
    update_workflow_state '.steps.step5_fixLoop.completed = true'
    update_workflow_state '.testFailuresPending = false'
    update_workflow_state '.testFailurePendingDetails = null'
    echo ""; echo "WORKFLOW: Tests PASSED. Steps 4+5 complete. testFailuresPending cleared."
    echo "Next: Step 6 (screenshots), Step 7 (verify+commit)"; echo ""
    log_event "STEP_4_COMPLETE" "tests=passed" "target=$TARGET"
elif [ "$RESULT" = "fail" ]; then
    update_workflow_state '.steps.step4_runTests.completed = true'
    IT=$(get_state_field ".steps.step5_fixLoop.iterations"); IT=${IT:-0}; IT=$((IT + 1))
    update_workflow_state ".steps.step5_fixLoop.iterations = $IT"
    update_workflow_state '.testFailuresPending = true'
    # Store failure details for gate enforcement
    TS_DETAIL=$(date -Iseconds 2>/dev/null || date +"%Y-%m-%dT%H:%M:%S")
    # Write command to temp file to avoid shell expansion issues in Python
    local cmd_tmp
    cmd_tmp=$(mktemp 2>/dev/null || echo ".claude/.tmp_hook_tfp_cmd_$$.txt")
    printf '%s' "$CMD" | head -c 200 > "$cmd_tmp"
    python -c "
import json, os, sys, tempfile
cmd_text = ''
try:
    with open(sys.argv[1]) as f:
        cmd_text = f.read()
except: pass
with open('$WORKFLOW_STATE_FILE') as f:
    d = json.load(f)
d['testFailurePendingDetails'] = {
    'command': cmd_text,
    'platform': '$PLATFORM',
    'target': '$TARGET',
    'timestamp': '$TS_DETAIL'
}
fd, tmp = tempfile.mkstemp(dir='.claude')
with os.fdopen(fd, 'w') as f:
    json.dump(d, f, indent=2)
os.replace(tmp, '$WORKFLOW_STATE_FILE')
" "$cmd_tmp" 2>/dev/null
    rm -f "$cmd_tmp" 2>/dev/null
    echo ""
    echo "WORKFLOW: Tests FAILED (iteration $IT)."
    echo ">>> Write/Edit to code files is BLOCKED until Skill(\"fix-loop\") is invoked. <<<"
    echo ">>> No exceptions for 'pre-existing' or 'unrelated' failures. ALL failures must be investigated. <<<"
    echo "DO NOT proceed to Step 6 until all tests pass."; echo ""
    log_event "STEP_4_COMPLETE" "tests=failed" "target=$TARGET" "iteration=$IT" "testFailuresPending=true"
else
    update_workflow_state '.steps.step4_runTests.completed = true'
    log_event "TEST_RUN" "result=unknown" "target=$TARGET"
fi

append_test_run_evidence "$CMD" "$TARGET" "$RESULT"

TS=$(date +%Y%m%d-%H%M%S 2>/dev/null || echo "$$")
# Use temp file for command text to avoid shell expansion issues
EJ_CMD_TMP=$(mktemp 2>/dev/null || echo ".claude/.tmp_hook_ej_cmd_$$.txt")
printf '%s' "$CMD" | head -c 200 > "$EJ_CMD_TMP"
EJ=$(python -c "
import json, sys
cmd = ''
try:
    with open(sys.argv[1]) as f:
        cmd = f.read()
except: pass
print(json.dumps({'timestamp':'$TS','command':cmd,'target':'$TARGET','claimedResult':'$RESULT','platform':'$PLATFORM'}))
" "$EJ_CMD_TMP" 2>/dev/null)
rm -f "$EJ_CMD_TMP" 2>/dev/null
if [ -n "$EJ" ]; then write_evidence "$EVIDENCE_DIR" "run-${TS}.json" "$EJ" >/dev/null; fi

exit 0
