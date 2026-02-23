#!/bin/bash
# =============================================================================
# Claude Code Screenshot Validation Hook (PostToolUse)
# =============================================================================
# Non-blocking (exit 0 always). Records screenshot metadata in workflow state.
# Triggers on: Bash (screencap commands) and Playwright screenshot tool.
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/hook-utils.sh"
parse_hook_input

if [ -z "$HOOK_TOOL_NAME" ]; then exit 0; fi
if [ ! -f "$WORKFLOW_STATE_FILE" ]; then exit 0; fi

SCREENSHOT_PATH=""
SCREENSHOT_SOURCE=""

case "$HOOK_TOOL_NAME" in
    "Bash")
        CMD=$(extract_input_field "command")
        if ! is_screenshot_command "$CMD"; then exit 0; fi
        SCREENSHOT_PATH=$(extract_screenshot_path "$CMD")
        SCREENSHOT_SOURCE="adb"
        ;;
    "mcp__playwright__browser_take_screenshot")
        SCREENSHOT_PATH=$(extract_input_field "filename")
        SCREENSHOT_SOURCE="playwright"
        ;;
    *)
        exit 0
        ;;
esac

if [ -z "$SCREENSHOT_PATH" ]; then exit 0; fi

# Determine screenshot type from filename
SCREENSHOT_TYPE="unknown"
if printf '%s' "$SCREENSHOT_PATH" | grep -qi "_before"; then
    SCREENSHOT_TYPE="before"
elif printf '%s' "$SCREENSHOT_PATH" | grep -qi "_after"; then
    SCREENSHOT_TYPE="after"
fi

# Validate file exists and has content
FILE_SIZE=0
FILE_VALID="false"
if [ -f "$SCREENSHOT_PATH" ]; then
    FILE_SIZE=$(wc -c < "$SCREENSHOT_PATH" 2>/dev/null | tr -d ' ')
    if [ "${FILE_SIZE:-0}" -gt 100 ]; then
        FILE_VALID="true"
    fi
fi

# Record metadata in workflow state
TIMESTAMP=$(date -Iseconds 2>/dev/null || date +"%Y-%m-%dT%H:%M:%S")
python -c "
import json, os, tempfile
sf = '$WORKFLOW_STATE_FILE'
if not os.path.exists(sf):
    exit(0)
with open(sf) as f:
    d = json.load(f)

# Ensure screenshotsCaptured list exists
if 'screenshotsCaptured' not in d:
    d['screenshotsCaptured'] = []

# Append screenshot metadata
d['screenshotsCaptured'].append({
    'path': '$SCREENSHOT_PATH',
    'timestamp': '$TIMESTAMP',
    'source': '$SCREENSHOT_SOURCE',
    'type': '$SCREENSHOT_TYPE',
    'validated': $FILE_VALID,
    'fileSize': $FILE_SIZE
})

# Update step6_screenshots before/after paths
steps = d.get('steps', {})
s6 = steps.get('step6_screenshots', {})
if '$SCREENSHOT_TYPE' == 'before':
    s6['before'] = '$SCREENSHOT_PATH'
elif '$SCREENSHOT_TYPE' == 'after':
    s6['after'] = '$SCREENSHOT_PATH'
steps['step6_screenshots'] = s6
d['steps'] = steps

fd, tmp = tempfile.mkstemp(dir='.claude')
with os.fdopen(fd, 'w') as f:
    json.dump(d, f, indent=2)
os.replace(tmp, sf)
" 2>/dev/null

# Print status message
if [ "$FILE_VALID" = "true" ]; then
    echo "Screenshot recorded: $SCREENSHOT_PATH (${FILE_SIZE} bytes, type=$SCREENSHOT_TYPE, source=$SCREENSHOT_SOURCE)"
else
    echo "WARNING: Screenshot validation failed for $SCREENSHOT_PATH"
    if [ ! -f "$SCREENSHOT_PATH" ]; then
        echo "  File does not exist"
    elif [ "${FILE_SIZE:-0}" -le 100 ]; then
        echo "  File too small (${FILE_SIZE} bytes) - may be empty or corrupt"
    fi
fi

log_event "SCREENSHOT_CAPTURED" "path=$SCREENSHOT_PATH" "source=$SCREENSHOT_SOURCE" "type=$SCREENSHOT_TYPE" "valid=$FILE_VALID" "size=$FILE_SIZE"

exit 0
