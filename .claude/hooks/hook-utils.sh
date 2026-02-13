#!/bin/bash
# =============================================================================
# Claude Code Hook Utilities - Shared Library
# =============================================================================
# Sourced (not executed) by all workflow hooks.
# All functions fail open (return 0 / allow on error).
# =============================================================================

WORKFLOW_STATE_FILE=".claude/workflow-state.json"
WORKFLOW_LOG_FILE=".claude/logs/workflow-sessions.log"
EVIDENCE_DIR=".claude/logs/test-evidence"

HOOK_TOOL_NAME=""
HOOK_TOOL_INPUT=""
HOOK_TOOL_OUTPUT=""
HOOK_RAW_INPUT=""

parse_hook_input() {
    HOOK_RAW_INPUT=$(cat)
    if [ -z "$HOOK_RAW_INPUT" ]; then return 0; fi
    HOOK_TOOL_NAME=$(echo "$HOOK_RAW_INPUT" | python -c "import sys,json;print(json.load(sys.stdin).get('tool_name',''))" 2>/dev/null) || HOOK_TOOL_NAME=""
    HOOK_TOOL_INPUT=$(echo "$HOOK_RAW_INPUT" | python -c "import sys,json;print(json.dumps(json.load(sys.stdin).get('tool_input',{})))" 2>/dev/null) || HOOK_TOOL_INPUT="{}"
    HOOK_TOOL_OUTPUT=$(echo "$HOOK_RAW_INPUT" | python -c "import sys,json;d=json.load(sys.stdin);o=d.get('tool_output','');print(json.dumps(o) if isinstance(o,dict) else str(o)[:50000])" 2>/dev/null) || HOOK_TOOL_OUTPUT=""
}

extract_input_field() {
    local field="$1"
    echo "$HOOK_TOOL_INPUT" | python -c "import sys,json;print(str(json.load(sys.stdin).get('$field','')))" 2>/dev/null
}

get_state_field() {
    local field_path="$1"
    if [ ! -f "$WORKFLOW_STATE_FILE" ]; then echo ""; return 1; fi
    if command -v jq &>/dev/null; then
        jq -r "$field_path // empty" "$WORKFLOW_STATE_FILE" 2>/dev/null
    else
        python -c "
import json
with open('$WORKFLOW_STATE_FILE') as f:
    d = json.load(f)
parts = '$field_path'.lstrip('.').split('.')
v = d
for p in parts:
    v = v.get(p) if isinstance(v, dict) else None
    if v is None: break
if v is not None: print(v)
" 2>/dev/null
    fi
}

update_workflow_state() {
    local jq_expr="$1"
    if [ ! -f "$WORKFLOW_STATE_FILE" ]; then return 1; fi
    if command -v jq &>/dev/null; then
        local tf; tf=$(mktemp 2>/dev/null || echo ".claude/.tmp_ws_$$.json")
        jq "$jq_expr" "$WORKFLOW_STATE_FILE" > "$tf" 2>/dev/null && mv "$tf" "$WORKFLOW_STATE_FILE" || rm -f "$tf" 2>/dev/null
    else
        python -c "
import json, os, tempfile
with open('$WORKFLOW_STATE_FILE') as f:
    d = json.load(f)
expr = r'''$jq_expr'''
if '=' in expr:
    pp, vp = expr.split('=', 1)
    pp = pp.strip().lstrip('.')
    vp = vp.strip()
    if vp=='true': val=True
    elif vp=='false': val=False
    elif vp=='null': val=None
    elif vp.startswith('\"') and vp.endswith('\"'): val=vp[1:-1]
    else:
        try: val=int(vp)
        except:
            try: val=float(vp)
            except: val=vp
    parts = pp.split('.')
    obj = d
    for p in parts[:-1]:
        if p not in obj or not isinstance(obj[p], dict): obj[p] = {}
        obj = obj[p]
    obj[parts[-1]] = val
fd, tmp = tempfile.mkstemp(dir='.claude')
with os.fdopen(fd, 'w') as f:
    json.dump(d, f, indent=2)
os.replace(tmp, '$WORKFLOW_STATE_FILE')
" 2>/dev/null
    fi
}

init_workflow_state() {
    local command_name="${1:-null}"
    local session_id
    session_id=$(date +%Y%m%d-%H%M%S 2>/dev/null || echo "session-$$")
    mkdir -p .claude/logs .claude/logs/test-evidence .claude/logs/fix-loop .claude/logs/post-fix-pipeline 2>/dev/null
    python -c "
import json, os
cn = '$command_name'
state = {
    'sessionId': '$session_id',
    'issueNumber': None, 'requirementId': None,
    'activeCommand': cn if cn != 'null' else None,
    'steps': {
        'step1_requirements': {'completed': False, 'timestamp': None, 'artifacts': []},
        'step2_tests': {'completed': False, 'timestamp': None, 'testFile': None},
        'step3_implement': {'completed': False, 'timestamp': None, 'filesChanged': []},
        'step4_runTests': {'completed': False, 'timestamp': None, 'testsPassed': None, 'testsTotal': None},
        'step5_fixLoop': {'completed': False, 'iterations': 0, 'allTestsPassing': False},
        'step6_screenshots': {'completed': False, 'before': None, 'after': None},
        'step7_verify': {'completed': False, 'verification': None}
    },
    'blocked': False, 'blockedReason': None,
    'skillInvocations': {
        'fixLoopInvoked': False, 'fixLoopCount': 0,
        'fixLoopEvidence': [], 'postFixPipelineInvoked': False,
        'postFixPipelineEvidence': None
    },
    'evidence': {'testRuns': [], 'screenshots': [], 'fixLoopLogs': []},
    'agentDelegations': []
}
os.makedirs('.claude', exist_ok=True)
with open('.claude/workflow-state.json', 'w') as f:
    json.dump(state, f, indent=2)
" 2>/dev/null
    log_event "SESSION_START" "id=$session_id" "command=$command_name"
}

is_test_command() {
    local cmd="$1"
    echo "$cmd" | grep -qiE "(pytest|gradlew.*(test|Test)|connectedDebugAndroidTest)"
}

extract_test_target() {
    local cmd="$1"
    if echo "$cmd" | grep -qiE "pytest"; then
        echo "$cmd" | grep -oE "tests/[^ ]*\.py" | head -1
        return 0
    fi
    if echo "$cmd" | grep -qiE "gradlew"; then
        local t; t=$(echo "$cmd" | grep -oE "class=[^ ]*" | sed 's/class=//')
        if [ -n "$t" ]; then echo "$t"; return 0; fi
        t=$(echo "$cmd" | grep -oE "\-\-tests[= ]+\"?[^ \"]*" | sed 's/--tests[= ]*//' | tr -d '"')
        if [ -n "$t" ]; then echo "$t"; return 0; fi
    fi
    echo ""
}

detect_test_result() {
    local output="$1"
    if echo "$output" | grep -qE "passed.*failed|failed.*passed"; then echo "fail"; return; fi
    if echo "$output" | grep -qE "[0-9]+ passed" && ! echo "$output" | grep -qE "[0-9]+ failed|[0-9]+ error"; then echo "pass"; return; fi
    if echo "$output" | grep -qE "[0-9]+ failed|[0-9]+ error|FAILED|FAILURES"; then echo "fail"; return; fi
    if echo "$output" | grep -qE "BUILD SUCCESSFUL"; then echo "pass"; return; fi
    if echo "$output" | grep -qE "BUILD FAILED|Tests? failed"; then echo "fail"; return; fi
    echo "unknown"
}

write_evidence() {
    local dir="$1"; local filename="$2"; local json_content="$3"
    mkdir -p "$dir" 2>/dev/null
    echo "$json_content" > "$dir/$filename" 2>/dev/null
    echo "$dir/$filename"
}

log_event() {
    local event_type="${1:-INFO}"; shift
    mkdir -p .claude/logs 2>/dev/null
    local ts; ts=$(date -Iseconds 2>/dev/null || date +"%Y-%m-%dT%H:%M:%S")
    local entry="[$ts] $event_type"
    for arg in "$@"; do entry="$entry | $arg"; done
    echo "$entry" >> "$WORKFLOW_LOG_FILE" 2>/dev/null
}

append_test_run_evidence() {
    local cmd="$1"; local target="$2"; local result="$3"
    local ts; ts=$(date -Iseconds 2>/dev/null || date +"%Y-%m-%dT%H:%M:%S")
    if [ ! -f "$WORKFLOW_STATE_FILE" ]; then return 1; fi
    python -c "
import json, os, tempfile
with open('$WORKFLOW_STATE_FILE') as f:
    d = json.load(f)
if 'evidence' not in d:
    d['evidence'] = {'testRuns': [], 'screenshots': [], 'fixLoopLogs': []}
d['evidence']['testRuns'].append({
    'timestamp': '$ts', 'command': '$(echo "$cmd" | head -c 200)',
    'target': '$target', 'claimedResult': '$result', 'independentVerification': None
})
fd, tmp = tempfile.mkstemp(dir='.claude')
with os.fdopen(fd, 'w') as f:
    json.dump(d, f, indent=2)
os.replace(tmp, '$WORKFLOW_STATE_FILE')
" 2>/dev/null
}

detect_skill_success() {
    local output="$1"
    if echo "$output" | grep -qiE "UNRESOLVED|MAX_ITERATIONS_EXCEEDED|MAX_CASCADE_EXCEEDED"; then
        echo "false"; return
    fi
    if echo "$output" | grep -qiE "Traceback|stacktrace|FATAL|panic:"; then
        echo "false"; return
    fi
    if echo "$output" | grep -qiE "RESOLVED|COMPLETED|PASSED"; then
        echo "true"; return
    fi
    echo "unknown"
}

record_skill_invocation() {
    local skill_name="$1"
    local succeeded="${2:-unknown}"
    if [ ! -f "$WORKFLOW_STATE_FILE" ]; then return 1; fi
    python -c "
import json, os, tempfile
with open('$WORKFLOW_STATE_FILE') as f:
    d = json.load(f)
si = d.setdefault('skillInvocations', {
    'fixLoopInvoked': False, 'fixLoopCount': 0, 'fixLoopSucceeded': None,
    'fixLoopEvidence': [], 'postFixPipelineInvoked': False,
    'postFixPipelineEvidence': None
})
s = '$skill_name'
succ = '$succeeded'
if s == 'fix-loop':
    si['fixLoopInvoked'] = True
    si['fixLoopCount'] = si.get('fixLoopCount', 0) + 1
    if succ in ('true', 'false'):
        si['fixLoopSucceeded'] = (succ == 'true')
elif s == 'post-fix-pipeline':
    si['postFixPipelineInvoked'] = True
fd, tmp = tempfile.mkstemp(dir='.claude')
with os.fdopen(fd, 'w') as f:
    json.dump(d, f, indent=2)
os.replace(tmp, '$WORKFLOW_STATE_FILE')
" 2>/dev/null
}
