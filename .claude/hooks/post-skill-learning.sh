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
SKILL_NAME=$(printf '%s' "$HOOK_TOOL_INPUT" | python -c "import sys,json;print(json.load(sys.stdin).get('skill',''))" 2>/dev/null)
if [ -z "$SKILL_NAME" ]; then exit 0; fi

# Self-skip: ignore /reflect to prevent infinite capture loops
if [ "$SKILL_NAME" = "reflect" ]; then exit 0; fi

# Extract skill arguments
SKILL_ARGS=$(printf '%s' "$HOOK_TOOL_INPUT" | python -c "import sys,json;print(json.load(sys.stdin).get('args',''))" 2>/dev/null)

# Parse the skill output for structured outcome (extract from raw input)
TOOL_OUTPUT_TEXT=$(get_tool_output)
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
            FIX_DETAIL=$(printf '%s' "$TOOL_OUTPUT_TEXT" | head -c 500 | grep -oE '\[[^\]]+:\d+\].*' | head -3)
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
    UNRESOLVED_TEXT=$(printf '%s' "$SKILL_UNRESOLVED" | python -c "
import sys, json
items = json.loads(sys.stdin.read() or '[]')
for item in items[:5]:
    print(f'- {item}')
" 2>/dev/null)
    GAP_ENTRY="- **Skill:** \`/$SKILL_NAME\` | **Gap:** $SKILL_OUTCOME\n$UNRESOLVED_TEXT"
    append_memory_topic "skill-gaps.md" "$GAP_ENTRY"
fi

# Update failure index for non-PASSED outcomes
if [ "$SKILL_OUTCOME" != "PASSED" ] && [ "$SKILL_OUTCOME" != "RESOLVED" ]; then
    # Determine issue type from skill args or output
    ISSUE_TYPE=$(printf '%s' "$SKILL_ARGS" | python -c "
import sys
args = sys.stdin.read()
if 'dropdown' in args.lower(): print('dropdown_interaction')
elif 'crash' in args.lower() or 'anr' in args.lower(): print('crash_anr')
elif 'navigation' in args.lower(): print('navigation_failure')
elif 'timeout' in args.lower(): print('timeout')
else: print('general_failure')
" 2>/dev/null)
    ISSUE_TYPE="${ISSUE_TYPE:-general_failure}"

    # Check if fix-patterns.md has a matching entry with file paths (auto-fix eligible)
    AUTO_FIX="false"
    if [ -f "$MEMORY_DIR/fix-patterns.md" ]; then
        FIX_MATCH=$(grep -i -A3 "$ISSUE_TYPE\|$SKILL_NAME" "$MEMORY_DIR/fix-patterns.md" 2>/dev/null | grep -i "auto-fix eligible.*yes" 2>/dev/null)
        if [ -n "$FIX_MATCH" ]; then
            AUTO_FIX="true"
        fi
    fi

    update_failure_index "$SKILL_NAME" "$ISSUE_TYPE" "$SKILL_OUTCOME" "" "" "$AUTO_FIX"
fi

# Detect "test failures ignored" meta-pattern (ENFORCEMENT_GAP detection)
# If test failures are pending and we're NOT in a fix-loop investigation,
# then the system is running skills while ignoring test failures.
if [ -f "$WORKFLOW_STATE_FILE" ]; then
    IGNORED=$(python -c "
import json
with open('$WORKFLOW_STATE_FILE') as f:
    d = json.load(f)
tfp = d.get('testFailuresPending', False)
fli = d.get('fixLoopInvestigating', False)
if tfp and not fli:
    details = d.get('testFailurePendingDetails') or {}
    print(f\"IGNORED: {details.get('platform','?')} failures from {details.get('command','?')[:80]}\")
" 2>/dev/null)
    if [ -n "$IGNORED" ]; then
        update_failure_index "system" "test_failure_bypass" "ENFORCEMENT_GAP" "hooks" "" "true"
        append_memory_topic "skill-gaps.md" "- **ENFORCEMENT GAP:** $IGNORED (detected during /$SKILL_NAME execution)"
        log_event "ENFORCEMENT_GAP" "skill=$SKILL_NAME" "detail=$IGNORED"
    fi
fi

# Auto-threshold escalation: if same (skill, issue_type) fails 5+ times, auto-file GitHub issue
if [ "$SKILL_OUTCOME" = "UNRESOLVED" ]; then
    python -c "
import json, subprocess, os
try:
    with open('$FAILURE_INDEX') as f:
        d = json.load(f)
    for e in d.get('entries', []):
        if e.get('skill') == '$SKILL_NAME' and len(e.get('occurrences', [])) >= 5:
            if not e.get('auto_escalation_filed'):
                desc = f\"Recurring failure in /{e['skill']}: {e['issue_type']} ({len(e['occurrences'])} occurrences)\"
                # Check for duplicate before filing
                result = subprocess.run(
                    ['gh', 'issue', 'list', '--search', f\"{e['skill']} {e['issue_type']}\", '--state', 'open', '--limit', '3'],
                    capture_output=True, text=True, timeout=10
                )
                if e['issue_type'] not in result.stdout:
                    subprocess.run(
                        ['gh', 'issue', 'create', '--title', f\"Recurring: {e['skill']} - {e['issue_type']}\",
                         '--body', desc, '--label', 'bug,recurring-failure,auto-filed'],
                        capture_output=True, text=True, timeout=15
                    )
                    e['auto_escalation_filed'] = True
                    import tempfile
                    fd, tmp = tempfile.mkstemp(dir='.claude/logs/learning')
                    with os.fdopen(fd, 'w') as f2:
                        json.dump(d, f2, indent=2)
                    os.replace(tmp, '$FAILURE_INDEX')
except Exception:
    pass
" 2>/dev/null
fi

# Log the learning capture event
log_event "LEARNING_CAPTURE" "skill=$SKILL_NAME" "outcome=$SKILL_OUTCOME" "capture=$CAPTURE_ID"

exit 0
