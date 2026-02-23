#!/bin/bash
# =============================================================================
# Auto-Fix Pattern Detection — PostToolUse Hook
# =============================================================================
# Fires after Bash commands containing "pytest" or "gradlew test".
# Scans fix-patterns.md for "Auto-fix eligible: Yes" entries and checks
# if the affected files still contain the bug pattern.
# Non-blocking (always exits 0) — logs warnings to workflow state.
# =============================================================================

source "$(dirname "$0")/hook-utils.sh"
parse_hook_input

# Only trigger on Bash tool
if [ "$HOOK_TOOL_NAME" != "Bash" ]; then exit 0; fi

# Extract command from tool input
CMD=$(extract_input_field "command")
if [ -z "$CMD" ]; then exit 0; fi

# Only trigger on test commands or skill-related commands
if ! echo "$CMD" | grep -qiE "(pytest|gradlew.*(test|Test)|fix-loop|reflect)"; then
    exit 0
fi

FIX_PATTERNS_FILE=$(ls -1 "$HOME"/.claude/projects/*VibeCoding-KKB/memory/fix-patterns.md 2>/dev/null | head -1)
if [ ! -f "$FIX_PATTERNS_FILE" ]; then exit 0; fi

# Scan for auto-fix eligible entries and check if fixes have been applied
python -c "
import re, json, os

fix_patterns_file = '$FIX_PATTERNS_FILE'
workflow_state_file = '.claude/workflow-state.json'

with open(fix_patterns_file, 'r', encoding='utf-8') as f:
    content = f.read()

# Parse entries: find sections with 'Auto-fix eligible: Yes'
sections = re.split(r'(?=^### )', content, flags=re.MULTILINE)
pending_fixes = []

for section in sections:
    if 'Auto-fix eligible: Yes' not in section:
        continue
    # Skip entries already marked FIXED
    title_match = re.match(r'### (.+)', section)
    if not title_match:
        continue
    title = title_match.group(1).strip()
    if title.endswith('FIXED'):
        continue

    # Extract files
    files_match = re.search(r'\*\*Files?:\*\*\s*(.+)', section)
    files = []
    if files_match:
        files_str = files_match.group(1)
        files = [f.strip().strip('\`') for f in re.split(r'[,;]|\band\b', files_str) if f.strip()]

    # Extract fix description
    fix_match = re.search(r'\*\*Fix(?:\s+applied)?:\*\*\s*(.+)', section)
    fix_desc = fix_match.group(1).strip() if fix_match else 'Unknown fix'

    pending_fixes.append({
        'pattern_name': title,
        'files': files,
        'fix_description': fix_desc
    })

if not pending_fixes:
    exit(0)

# Log to workflow state
if os.path.exists(workflow_state_file):
    try:
        with open(workflow_state_file) as f:
            state = json.load(f)
        state['pendingAutoFixes'] = pending_fixes
        import tempfile
        fd, tmp = tempfile.mkstemp(dir='.claude')
        with os.fdopen(fd, 'w') as f:
            json.dump(state, f, indent=2)
        os.replace(tmp, workflow_state_file)
    except Exception:
        pass

count = len(pending_fixes)
names = ', '.join(p['pattern_name'] for p in pending_fixes)
print(f'Auto-fix scan: {count} unfixed auto-fix-eligible pattern(s) found: {names}')
" 2>/dev/null

# Always non-blocking
exit 0
