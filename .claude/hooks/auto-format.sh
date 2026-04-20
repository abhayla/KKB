#!/bin/bash
# auto-format.sh — PostToolUse hook for Write|Edit
# Auto-formats files after Claude writes or edits them.
#
# Customize the formatters below to match your project tooling.
# Only formatters that are installed will run — missing ones are silently skipped.
#
# Configuration:
#   Event: PostToolUse
#   Matcher: Write|Edit
#   Exit codes: 0 = success (always non-blocking)
#
# Settings.json entry:
#   {
#     "hooks": {
#       "PostToolUse": [
#         {
#           "matcher": "Write|Edit",
#           "command": ".claude/hooks/auto-format.sh"
#         }
#       ]
#     }
#   }

TOOL_INPUT=$(cat)
if command -v jq >/dev/null 2>&1; then
  FILE=$(echo "$TOOL_INPUT" | jq -r '.tool_input.file_path // .tool_input.path // .file_path // .path // empty' 2>/dev/null)
else
  FILE=$(echo "$TOOL_INPUT" | python -c "import sys,json
try:
  d=json.load(sys.stdin); ti=d.get('tool_input',d) if isinstance(d,dict) else {}
  print(ti.get('file_path') or ti.get('path') or d.get('file_path') or d.get('path') or '')
except Exception: pass" 2>/dev/null)
fi
if [[ -z "$FILE" ]] || [[ ! -f "$FILE" ]]; then exit 0; fi

case "$FILE" in
  # Python
  *.py)
    command -v black >/dev/null 2>&1 && black --quiet "$FILE" 2>/dev/null
    command -v ruff >/dev/null 2>&1 && ruff format --quiet "$FILE" 2>/dev/null
    ;;
  # JavaScript / TypeScript
  *.js|*.jsx|*.ts|*.tsx)
    command -v npx >/dev/null 2>&1 && npx prettier --write "$FILE" 2>/dev/null
    ;;
  # JSON / YAML / Markdown (prettier handles these too)
  *.json|*.yml|*.yaml|*.css|*.scss|*.html)
    command -v npx >/dev/null 2>&1 && npx prettier --write "$FILE" 2>/dev/null
    ;;
  # Kotlin
  *.kt|*.kts)
    command -v ktfmt >/dev/null 2>&1 && ktfmt "$FILE" 2>/dev/null
    ;;
  # Go
  *.go)
    command -v gofmt >/dev/null 2>&1 && gofmt -w "$FILE" 2>/dev/null
    ;;
  # Rust
  *.rs)
    command -v rustfmt >/dev/null 2>&1 && rustfmt "$FILE" 2>/dev/null
    ;;
  # Swift
  *.swift)
    command -v swift-format >/dev/null 2>&1 && swift-format format -i "$FILE" 2>/dev/null
    ;;
  # Shell
  *.sh|*.bash)
    command -v shfmt >/dev/null 2>&1 && shfmt -w "$FILE" 2>/dev/null
    ;;
esac

# Always exit 0 — formatting failures should not block the workflow
exit 0
