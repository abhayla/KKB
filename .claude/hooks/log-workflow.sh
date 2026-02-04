#!/bin/bash
# =============================================================================
# Claude Code Workflow Logging Hook
# =============================================================================
# Purpose: Lightweight logging for workflow events, debugging, and auditing.
#
# Usage: Can be called from other hooks or directly
#        .claude/hooks/log-workflow.sh <EVENT_TYPE> [KEY=VALUE...]
#
# Examples:
#        .claude/hooks/log-workflow.sh STEP_COMPLETE step=1 issue=47
#        .claude/hooks/log-workflow.sh COMMIT_BLOCKED reason="tests_failing"
# =============================================================================

LOG_FILE=".claude/logs/workflow-sessions.log"
EVENT_TYPE="${1:-INFO}"
shift

# Ensure log directory exists
mkdir -p .claude/logs 2>/dev/null

# Get timestamp
TIMESTAMP=$(date -Iseconds 2>/dev/null || date +"%Y-%m-%dT%H:%M:%S")

# Build log entry
LOG_ENTRY="[$TIMESTAMP] $EVENT_TYPE"

# Add any additional key=value pairs
for arg in "$@"; do
    LOG_ENTRY="$LOG_ENTRY | $arg"
done

# Write to log file
echo "$LOG_ENTRY" >> "$LOG_FILE"

# Also output for immediate feedback if verbose mode
if [ "${CLAUDE_HOOK_VERBOSE:-}" = "true" ]; then
    echo "[LOG] $LOG_ENTRY"
fi
