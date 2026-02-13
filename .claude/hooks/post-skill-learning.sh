#!/bin/bash
# =============================================================================
# Post-Skill Learning Capture Hook
# =============================================================================
# Trigger: PostToolUse for Skill tool (runs after log-workflow.sh)
# Timeout: 30s | Exit: Always 0 (never blocks)
# Purpose: Capture skill outcomes into structured learning logs and memory topics
# =============================================================================

source .claude/hooks/hook-utils.sh 2>/dev/null || exit 0
parse_hook_input

# Only process Skill tool invocations
if [ "$HOOK_TOOL_NAME" != "Skill" ]; then exit 0; fi

# Extract skill name from tool_input
SKILL_NAME=$(echo "$HOOK_TOOL_INPUT" | python -c "import sys,json;print(json.load(sys.stdin).get('skill',''))" 2>/dev/null)
if [ -z "$SKILL_NAME" ]; then exit 0; fi

# Self-skip: ignore /reflect to prevent infinite capture loops
if [ "$SKILL_NAME" = "reflect" ]; then exit 0; fi

# Extract skill arguments
SKILL_ARGS=$(echo "$HOOK_TOOL_INPUT" | python -c "import sys,json;print(json.load(sys.stdin).get('args',''))" 2>/dev/null)

# Parse the skill output for structured outcome
TOOL_OUTPUT_TEXT="$HOOK_TOOL_OUTPUT"
if [ -z "$TOOL_OUTPUT_TEXT" ] || [ ${#TOOL_OUTPUT_TEXT} -lt 10 ]; then exit 0; fi

parse_skill_outcome "$TOOL_OUTPUT_TEXT"

# Prepare capture directory
TODAY=$(date +%Y%m%d 2>/dev/null || echo "unknown")
CAPTURE_DIR="$LEARNING_LOG_DIR/$TODAY"
mkdir -p "$CAPTURE_DIR" 2>/dev/null || exit 0

# Generate capture ID and timestamp
TIMESTAMP=$(date +"%Y-%m-%dT%H:%M:%S" 2>/dev/null || date +"%Y-%m-%dT%H:%M:%S")
CAPTURE_TS=$(date +%H%M%S 2>/dev/null || echo "000000")
CAPTURE_ID="cap-${TODAY}-${CAPTURE_TS}"

# Write structured capture JSON
python -c "
import json, os

capture = {
    'captureId': '$CAPTURE_ID',
    'timestamp': '$TIMESTAMP',
    'skillName': '$SKILL_NAME',
    'skillArgs': '''$SKILL_ARGS'''[:500],
    'outcome': '$SKILL_OUTCOME',
    'issuesFound': $SKILL_ISSUES_FOUND,
    'issuesResolved': $SKILL_ISSUES_RESOLVED,
    'fixesApplied': json.loads('''$SKILL_FIXES''' or '[]'),
    'unresolvedItems': json.loads('''$SKILL_UNRESOLVED''' or '[]')
}

capture_file = '$CAPTURE_DIR/capture-${CAPTURE_TS}.json'
with open(capture_file, 'w') as f:
    json.dump(capture, f, indent=2)
" 2>/dev/null

# Append lightweight summary to memory topic files
SUMMARY="- **Skill:** \`/$SKILL_NAME\` | **Outcome:** $SKILL_OUTCOME | **Issues:** $SKILL_ISSUES_FOUND found, $SKILL_ISSUES_RESOLVED resolved"

# Route to appropriate topic file based on skill name
case "$SKILL_NAME" in
    adb-test|run-e2e)
        append_memory_topic "testing-lessons.md" "$SUMMARY"
        ;;
    fix-loop)
        append_memory_topic "fix-patterns.md" "$SUMMARY"
        if [ "$SKILL_OUTCOME" = "RESOLVED" ] && [ "$SKILL_ISSUES_RESOLVED" -gt 0 ] 2>/dev/null; then
            # Extract fix pattern details for the catalog
            FIX_DETAIL=$(echo "$TOOL_OUTPUT_TEXT" | head -c 500 | grep -oE '\[[^\]]+:\d+\].*' | head -3)
            if [ -n "$FIX_DETAIL" ]; then
                append_memory_topic "fix-patterns.md" "$FIX_DETAIL"
            fi
        fi
        ;;
    implement|fix-issue|post-fix-pipeline)
        append_memory_topic "testing-lessons.md" "$SUMMARY"
        ;;
esac

# If UNRESOLVED: note in skill-gaps.md
if [ "$SKILL_OUTCOME" = "UNRESOLVED" ] || [ "$SKILL_OUTCOME" = "PARTIALLY_RESOLVED" ]; then
    UNRESOLVED_TEXT=$(echo "$SKILL_UNRESOLVED" | python -c "
import sys, json
items = json.loads(sys.stdin.read() or '[]')
for item in items[:5]:
    print(f'- {item}')
" 2>/dev/null)
    GAP_ENTRY="- **Skill:** \`/$SKILL_NAME\` | **Gap:** $SKILL_OUTCOME\n$UNRESOLVED_TEXT"
    append_memory_topic "skill-gaps.md" "$GAP_ENTRY"
fi

# Log the learning capture event
log_event "LEARNING_CAPTURE" "skill=$SKILL_NAME" "outcome=$SKILL_OUTCOME" "capture=$CAPTURE_ID"

exit 0
