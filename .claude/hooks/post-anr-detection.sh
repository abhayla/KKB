#!/bin/bash
# PostToolUse hook: Detect ANR patterns in Bash tool output
# Sets testFailuresPending=true when ANR is detected

# Source shared utilities
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/hook-utils.sh" ]; then
    source "$SCRIPT_DIR/hook-utils.sh"
else
    exit 0
fi
parse_hook_input

# Only process Bash tool outputs
if [ "$HOOK_TOOL_NAME" != "Bash" ]; then
    exit 0
fi

# Get the tool output
OUTPUT="$HOOK_TOOL_OUTPUT"
if [ -z "$OUTPUT" ]; then
    exit 0
fi

# Check for ANR patterns in tool output
ANR_DETECTED=false
if echo "$OUTPUT" | grep -qiE "ANR in|isn't responding|Application Not Responding|Input dispatching timed out"; then
    ANR_DETECTED=true
fi

if [ "$ANR_DETECTED" = "true" ]; then
    # Set testFailuresPending in workflow state
    STATE_FILE=".claude/workflow-state.json"
    if [ -f "$STATE_FILE" ]; then
        python -c "
import json
with open('$STATE_FILE') as f:
    d = json.load(f)
d['testFailuresPending'] = True
d.setdefault('evidence', {})['anr_detected'] = True
with open('$STATE_FILE', 'w') as f:
    json.dump(d, f, indent=2)
print('ANR DETECTED: testFailuresPending set to true. MUST invoke /fix-loop.')
" 2>/dev/null || python -c "
import json
with open('$STATE_FILE') as f:
    d = json.load(f)
d['testFailuresPending'] = True
d.setdefault('evidence', {})['anr_detected'] = True
with open('$STATE_FILE', 'w') as f:
    json.dump(d, f, indent=2)
print('ANR DETECTED: testFailuresPending set to true. MUST invoke /fix-loop.')
" 2>/dev/null
    fi

    # Log the event
    LOG_DIR=".claude/logs/adb-test"
    mkdir -p "$LOG_DIR" 2>/dev/null
    echo "$(date +%Y-%m-%dT%H:%M:%S) ANR_DETECTED tool=Bash" >> "$LOG_DIR/anr-events.log"
fi

exit 0
