#!/bin/bash
# =============================================================================
# Claude Code Post-Screenshot Resize Hook
# =============================================================================
# Purpose: Automatically resize screenshots after capture to prevent Claude API
#          400 errors from images exceeding 2000px per dimension.
#
# Triggers: PostToolUse on browser_take_screenshot and Bash (screencap commands)
#
# The hook reads JSON from stdin (Claude Code hook protocol), checks if the
# tool call was screenshot-related, and runs resize_screenshot.py if needed.
# For non-screenshot Bash commands, exits immediately (< 5ms overhead).
# =============================================================================

# Source shared utilities for consistent stdin parsing
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/hook-utils.sh"
parse_hook_input

TOOL_NAME="$HOOK_TOOL_NAME"
TOOL_INPUT="$HOOK_TOOL_INPUT"

case "$TOOL_NAME" in
  *browser_take_screenshot*)
    # Playwright screenshot - extract filename from tool input
    FILENAME=$(echo "$TOOL_INPUT" | python -c "import sys,json; print(json.load(sys.stdin).get('filename',''))" 2>/dev/null)
    if [ -n "$FILENAME" ] && [ -f "$FILENAME" ]; then
      python "$SCRIPT_DIR/resize_screenshot.py" "$FILENAME"
    fi
    ;;
  Bash)
    # Bash command - check if it's a screenshot command
    COMMAND=$(echo "$TOOL_INPUT" | python -c "import sys,json; print(json.load(sys.stdin).get('command',''))" 2>/dev/null)
    if echo "$COMMAND" | grep -qE "screencap|screenshot"; then
      # Extract actual output file path from redirect (> path.png)
      OUTPUT_FILE=$(echo "$COMMAND" | grep -oE '>\s*[^ ]+\.png' | sed 's/^>\s*//')
      if [ -n "$OUTPUT_FILE" ] && [ -f "$OUTPUT_FILE" ]; then
        # Process the specific file
        python "$SCRIPT_DIR/resize_screenshot.py" "$OUTPUT_FILE"
        # Check if file is still invalid (< 1KB = failed capture)
        FILE_SIZE=$(wc -c < "$OUTPUT_FILE" 2>/dev/null | tr -d ' ')
        if [ "${FILE_SIZE:-0}" -lt 1000 ]; then
          # Extract ADB path from command, re-capture without -d flag
          ADB_PATH=$(echo "$COMMAND" | grep -oE '[^ ]*adb[^ ]*' | head -1)
          if [ -n "$ADB_PATH" ]; then
            "$ADB_PATH" exec-out screencap -p > "$OUTPUT_FILE" 2>/dev/null
            python "$SCRIPT_DIR/resize_screenshot.py" "$OUTPUT_FILE"
          fi
        fi
      else
        python "$SCRIPT_DIR/resize_screenshot.py" --recent
      fi
    fi
    ;;
esac

exit 0
