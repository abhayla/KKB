#!/bin/bash
# =============================================================================
# Claude Code Independent Test Verification Hook (PostToolUse)
# =============================================================================
# Re-runs the same test independently. Blocks if claimed PASS but re-run FAIL.
# Exit 0 = allow, Exit 2 = BLOCK (inconsistent).
# Skips: non-test, full suite, E2E, multiple targets, infrastructure failure.
# Timeout: 300s.
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/hook-utils.sh"
parse_hook_input

if [ "$HOOK_TOOL_NAME" != "Bash" ]; then exit 0; fi

CMD=$(extract_input_field "command")
if ! is_test_command "$CMD"; then exit 0; fi

IS_E2E=false
RERUN_TIMEOUT=300
if printf '%s' "$CMD" | grep -qE "connectedDebugAndroidTest|connectedAndroidTest"; then
    IS_E2E=true
    RERUN_TIMEOUT=600
    # Skip full E2E suite (no specific class target) — too expensive
    if ! printf '%s' "$CMD" | grep -qE "class="; then exit 0; fi
fi

TARGET=$(extract_test_target "$CMD")
if [ -z "$TARGET" ]; then exit 0; fi
if printf '%s' "$TARGET" | grep -qE "[, ]"; then exit 0; fi

CLAIMED=$(detect_test_result "$HOOK_TOOL_OUTPUT")
if [ "$CLAIMED" = "unknown" ]; then exit 0; fi

# Infrastructure pre-check
if printf '%s' "$CMD" | grep -qiE "pytest"; then
    if ! command -v python &>/dev/null; then
        TS=$(date +%Y%m%d-%H%M%S 2>/dev/null || echo "$$")
        EJ='{"event":"rerun_infra_warn","reason":"python_not_found","target":"'"$TARGET"'","timestamp":"'"$(date -Iseconds 2>/dev/null)"'"}'
        write_evidence "$EVIDENCE_DIR" "rerun-infra-warn-${TS}.json" "$EJ" >/dev/null
        log_event "VERIFY_RERUN_INFRA_MISSING" "reason=python_not_found" "target=$TARGET"
        exit 0
    fi
elif printf '%s' "$CMD" | grep -qiE "gradlew"; then
    if [ ! -f "android/gradlew" ]; then
        TS=$(date +%Y%m%d-%H%M%S 2>/dev/null || echo "$$")
        EJ='{"event":"rerun_infra_warn","reason":"gradlew_not_found","target":"'"$TARGET"'","timestamp":"'"$(date -Iseconds 2>/dev/null)"'"}'
        write_evidence "$EVIDENCE_DIR" "rerun-infra-warn-${TS}.json" "$EJ" >/dev/null
        log_event "VERIFY_RERUN_INFRA_MISSING" "reason=gradlew_not_found" "target=$TARGET"
        exit 0
    fi
fi

# Build re-run command
RERUN_CMD=""
if printf '%s' "$CMD" | grep -qiE "pytest"; then
    if printf '%s' "$TARGET" | grep -qE "^tests/"; then
        RERUN_CMD="cd backend && PYTHONPATH=. python -m pytest $TARGET --tb=short -q 2>&1"
    else
        RERUN_CMD="cd backend && PYTHONPATH=. python -m pytest tests/$TARGET --tb=short -q 2>&1"
    fi
elif [ "$IS_E2E" = "true" ]; then
    # E2E re-run with specific class target
    RERUN_CMD="cd android && ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=$TARGET --console=plain 2>&1"
elif printf '%s' "$CMD" | grep -qiE "gradlew.*test"; then
    if printf '%s' "$TARGET" | grep -qE "^com\.|^\*\."; then
        RERUN_CMD="cd android && ./gradlew :app:testDebugUnitTest --tests \"$TARGET\" --console=plain 2>&1"
    else
        exit 0
    fi
fi
if [ -z "$RERUN_CMD" ]; then exit 0; fi

# Re-run with timeout
RERUN_OUTPUT=$(timeout $RERUN_TIMEOUT bash -c "$RERUN_CMD" 2>&1) || true
RC=$?
if [ "$RC" -eq 124 ]; then
    log_event "VERIFY_RERUN_TIMEOUT" "target=$TARGET"; exit 0
fi

RERUN_RESULT=$(detect_test_result "$RERUN_OUTPUT")
if [ "$RERUN_RESULT" = "unknown" ]; then exit 0; fi

# Write evidence
TS=$(date +%Y%m%d-%H%M%S 2>/dev/null || echo "$$")
CONSISTENT="false"
if [ "$CLAIMED" = "$RERUN_RESULT" ] || { [ "$CLAIMED" = "fail" ] && [ "$RERUN_RESULT" = "pass" ]; }; then
    CONSISTENT="true"
fi
# Pass TARGET via env var to avoid shell injection in JSON
export _HK_TARGET="$TARGET"
EJ=$(python -c "
import json, os
consistent = True if '$CONSISTENT' == 'true' else False
print(json.dumps({'target':os.environ.get('_HK_TARGET',''),'claimed':'$CLAIMED','rerun':'$RERUN_RESULT','consistent':consistent}))
" 2>/dev/null)
unset _HK_TARGET
if [ -n "$EJ" ]; then write_evidence "$EVIDENCE_DIR" "rerun-${TS}.json" "$EJ" >/dev/null; fi

# Update workflow state
if [ -f "$WORKFLOW_STATE_FILE" ]; then
    python -c "
import json, os, tempfile
with open('$WORKFLOW_STATE_FILE') as f:
    d = json.load(f)
runs = d.get('evidence', {}).get('testRuns', [])
if runs:
    runs[-1]['independentVerification'] = {'rerunResult': '$RERUN_RESULT', 'consistent': $CONSISTENT}
fd, tmp = tempfile.mkstemp(dir='.claude')
with os.fdopen(fd, 'w') as f:
    json.dump(d, f, indent=2)
os.replace(tmp, '$WORKFLOW_STATE_FILE')
" 2>/dev/null
fi

# Decision matrix
if [ "$RERUN_RESULT" = "fail" ] && [ "$CLAIMED" = "pass" ]; then
    echo ""; echo "INDEPENDENT VERIFICATION FAILED"
    echo "Claimed: PASS, Re-run: FAIL for $TARGET"
    echo "$(echo "$RERUN_OUTPUT" | tail -10)"; echo ""
    log_event "VERIFY_INCONSISTENT" "target=$TARGET" "claimed=pass" "rerun=fail"
    exit 2
fi

if [ "$RERUN_RESULT" = "pass" ] && [ "$CLAIMED" = "fail" ]; then
    echo "WARNING: Flaky test? $TARGET (claimed FAIL, re-run PASS)"
    log_event "VERIFY_FLAKY" "target=$TARGET"
fi

log_event "VERIFY_CONSISTENT" "target=$TARGET" "result=$CLAIMED"
exit 0
