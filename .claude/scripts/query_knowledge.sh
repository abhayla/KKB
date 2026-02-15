#!/bin/bash
# query_knowledge.sh — Quick KB query wrapper for skills
#
# Usage: bash .claude/scripts/query_knowledge.sh "ERROR_TYPE" "error_message" "file/path"
# Returns: ranked strategies or "UNKNOWN PATTERN"
# 5-second timeout, fail-open (exit 0 on any error)

set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KB_SCRIPT="$SCRIPT_DIR/knowledge_db.py"
DB_PATH="$SCRIPT_DIR/../knowledge.db"

# Fail-open: if anything goes wrong, exit 0 with "UNKNOWN PATTERN"
fail_open() {
    echo "UNKNOWN PATTERN"
    exit 0
}
trap fail_open ERR

# Check prerequisites
if [ ! -f "$KB_SCRIPT" ]; then
    fail_open
fi

if [ ! -f "$DB_PATH" ]; then
    fail_open
fi

ERROR_TYPE="${1:-}"
ERROR_MSG="${2:-}"
FILE_PATH="${3:-}"

if [ -z "$ERROR_MSG" ]; then
    fail_open
fi

# Combine error type and message for matching
COMBINED_ERROR="$ERROR_TYPE: $ERROR_MSG"

# Query with 5-second timeout
RESULT=$(timeout 5 python "$KB_SCRIPT" get-strategies --error "$COMBINED_ERROR" 2>/dev/null) || fail_open

if [ -z "$RESULT" ] || echo "$RESULT" | grep -q "UNKNOWN PATTERN"; then
    # Try with just the error message (without type prefix)
    RESULT=$(timeout 5 python "$KB_SCRIPT" get-strategies --error "$ERROR_MSG" 2>/dev/null) || fail_open
fi

if [ -z "$RESULT" ] || echo "$RESULT" | grep -q "UNKNOWN PATTERN"; then
    echo "UNKNOWN PATTERN"
else
    echo "$RESULT"
fi

exit 0
