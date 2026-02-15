#!/usr/bin/env python3
"""
Knowledge Database for KKB fix strategies.

Tracks error patterns, fix strategies with time-decayed success scores,
and fix attempt history. Stdlib only — no external dependencies.

Usage:
    python .claude/scripts/knowledge_db.py record-error --category backend --error "IntegrityError: ..."
    python .claude/scripts/knowledge_db.py get-strategies --error "IntegrityError: ..."
    python .claude/scripts/knowledge_db.py record-attempt --error-id 5 --strategy-id 3 --outcome success
    python .claude/scripts/knowledge_db.py create-strategy --error-id 5 --name "add_upsert" --description "..."
    python .claude/scripts/knowledge_db.py import-failure-index
    python .claude/scripts/knowledge_db.py top-errors --limit 10
    python .claude/scripts/knowledge_db.py self-test
"""

import argparse
import json
import os
import re
import sqlite3
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent.parent
DB_PATH = SCRIPT_DIR.parent / "knowledge.db"
FAILURE_INDEX_PATH = SCRIPT_DIR.parent / "logs" / "learning" / "failure-index.json"

SCHEMA = """
CREATE TABLE IF NOT EXISTS error_patterns (
    id INTEGER PRIMARY KEY,
    category TEXT NOT NULL,
    component TEXT,
    file_path TEXT,
    error_signature TEXT NOT NULL,
    first_seen TEXT NOT NULL,
    last_seen TEXT NOT NULL,
    occurrence_count INTEGER DEFAULT 1,
    UNIQUE(category, error_signature)
);

CREATE TABLE IF NOT EXISTS fix_strategies (
    id INTEGER PRIMARY KEY,
    error_pattern_id INTEGER REFERENCES error_patterns(id),
    name TEXT NOT NULL,
    description TEXT,
    success_rate REAL DEFAULT 0.5,
    attempt_count INTEGER DEFAULT 0,
    last_used TEXT,
    UNIQUE(error_pattern_id, name)
);

CREATE TABLE IF NOT EXISTS fix_attempts (
    id INTEGER PRIMARY KEY,
    error_pattern_id INTEGER REFERENCES error_patterns(id),
    strategy_id INTEGER REFERENCES fix_strategies(id),
    session_id TEXT,
    outcome TEXT NOT NULL,
    fix_description TEXT,
    timestamp TEXT NOT NULL
);
"""


def normalize_error(error_text: str) -> str:
    """Strip line numbers, timestamps, hex addresses, and UUIDs for matching."""
    s = error_text.strip()
    s = re.sub(r'line \d+', 'line N', s)
    s = re.sub(r'\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}[.\d]*Z?', 'TIMESTAMP', s)
    s = re.sub(r'0x[0-9a-fA-F]+', '0xADDR', s)
    s = re.sub(r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}', 'UUID', s, flags=re.I)
    s = re.sub(r'\s+', ' ', s)
    return s[:500]


def get_db(db_path: str = None) -> sqlite3.Connection:
    """Open (or create) the knowledge database."""
    path = db_path or str(DB_PATH)
    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    conn.executescript(SCHEMA)
    return conn


def apply_weekly_decay(conn: sqlite3.Connection):
    """Apply 0.95 weekly decay to all strategies not used in the last 7 days."""
    cutoff = (datetime.now(timezone.utc) - timedelta(days=7)).isoformat()
    conn.execute("""
        UPDATE fix_strategies
        SET success_rate = MAX(0.0, MIN(1.0, success_rate * 0.95))
        WHERE last_used IS NOT NULL AND last_used < ?
    """, (cutoff,))
    conn.commit()


def record_error(conn, category, error_text, component=None, file_path=None):
    """Record an error pattern. Returns the error_pattern id."""
    sig = normalize_error(error_text)
    now = datetime.now(timezone.utc).isoformat()
    row = conn.execute(
        "SELECT id, occurrence_count FROM error_patterns WHERE category = ? AND error_signature = ?",
        (category, sig)
    ).fetchone()
    if row:
        conn.execute(
            "UPDATE error_patterns SET last_seen = ?, occurrence_count = occurrence_count + 1, "
            "component = COALESCE(?, component), file_path = COALESCE(?, file_path) WHERE id = ?",
            (now, component, file_path, row['id'])
        )
        conn.commit()
        return row['id']
    else:
        cur = conn.execute(
            "INSERT INTO error_patterns (category, component, file_path, error_signature, first_seen, last_seen) "
            "VALUES (?, ?, ?, ?, ?, ?)",
            (category, component, file_path, sig, now, now)
        )
        conn.commit()
        return cur.lastrowid


def get_strategies(conn, error_text=None, error_id=None):
    """Get strategies for an error, ranked by success_rate descending."""
    if error_id:
        eid = error_id
    elif error_text:
        sig = normalize_error(error_text)
        row = conn.execute(
            "SELECT id FROM error_patterns WHERE error_signature = ?", (sig,)
        ).fetchone()
        if not row:
            return []
        eid = row['id']
    else:
        return []
    rows = conn.execute(
        "SELECT s.*, e.category, e.error_signature FROM fix_strategies s "
        "JOIN error_patterns e ON s.error_pattern_id = e.id "
        "WHERE s.error_pattern_id = ? ORDER BY s.success_rate DESC",
        (eid,)
    ).fetchall()
    return [dict(r) for r in rows]


def create_strategy(conn, error_id, name, description=None):
    """Create a new fix strategy for an error pattern."""
    now = datetime.now(timezone.utc).isoformat()
    try:
        cur = conn.execute(
            "INSERT INTO fix_strategies (error_pattern_id, name, description, last_used) VALUES (?, ?, ?, ?)",
            (error_id, name, description, now)
        )
        conn.commit()
        return cur.lastrowid
    except sqlite3.IntegrityError:
        row = conn.execute(
            "SELECT id FROM fix_strategies WHERE error_pattern_id = ? AND name = ?",
            (error_id, name)
        ).fetchone()
        return row['id'] if row else None


def record_attempt(conn, error_id, strategy_id, outcome, session_id=None, description=None):
    """Record a fix attempt and update strategy score."""
    now = datetime.now(timezone.utc).isoformat()
    conn.execute(
        "INSERT INTO fix_attempts (error_pattern_id, strategy_id, session_id, outcome, fix_description, timestamp) "
        "VALUES (?, ?, ?, ?, ?, ?)",
        (error_id, strategy_id, session_id, outcome, description, now)
    )
    # Update strategy score
    delta = {'success': 0.1, 'failure': -0.05, 'partial': 0.03}.get(outcome, 0)
    conn.execute(
        "UPDATE fix_strategies SET success_rate = MAX(0.0, MIN(1.0, success_rate + ?)), "
        "attempt_count = attempt_count + 1, last_used = ? WHERE id = ?",
        (delta, now, strategy_id)
    )
    conn.commit()


def top_errors(conn, limit=10):
    """Get top errors by occurrence count."""
    rows = conn.execute(
        "SELECT e.*, "
        "(SELECT s.name FROM fix_strategies s WHERE s.error_pattern_id = e.id "
        " ORDER BY s.success_rate DESC LIMIT 1) AS best_strategy, "
        "(SELECT MAX(s.success_rate) FROM fix_strategies s WHERE s.error_pattern_id = e.id) AS best_score "
        "FROM error_patterns e ORDER BY e.occurrence_count DESC LIMIT ?",
        (limit,)
    ).fetchall()
    return [dict(r) for r in rows]


def import_failure_index(conn):
    """Import existing failure-index.json into the knowledge DB."""
    if not FAILURE_INDEX_PATH.exists():
        print(f"No failure-index.json found at {FAILURE_INDEX_PATH}")
        return 0

    with open(FAILURE_INDEX_PATH) as f:
        data = json.load(f)

    imported = 0
    for entry in data.get('entries', []):
        category = 'android-e2e'  # failure-index is from adb-test
        component = entry.get('component', '')
        error_sig = f"{entry.get('issue_type', 'unknown')}: {entry.get('known_workaround', '')}"
        first_seen = entry.get('first_seen', datetime.now(timezone.utc).isoformat())

        eid = record_error(conn, category, error_sig, component=component)

        # Import workaround as a strategy
        workaround = entry.get('known_workaround')
        if workaround:
            # Set initial score based on threshold_reached
            sid = create_strategy(conn, eid, 'known_workaround', workaround)
            if sid and entry.get('threshold_reached'):
                conn.execute(
                    "UPDATE fix_strategies SET success_rate = 0.7 WHERE id = ?", (sid,)
                )

        # Import each occurrence
        for occ in entry.get('occurrences', []):
            occ_workaround = occ.get('workaround_used')
            if occ_workaround:
                occ_sid = create_strategy(conn, eid, occ_workaround, occ.get('root_cause'))
                if occ_sid:
                    outcome = 'success' if occ.get('outcome') == 'partial_success' else 'failure'
                    record_attempt(conn, eid, occ_sid, outcome,
                                   session_id=occ.get('session'),
                                   description=occ.get('root_cause'))
        imported += 1

    conn.commit()
    print(f"Imported {imported} entries from failure-index.json")
    return imported


def self_test(db_path=":memory:"):
    """Run self-test to verify all operations work."""
    print("Running knowledge_db self-test...")
    conn = get_db(db_path)
    errors = []

    # Test 1: record_error
    eid = record_error(conn, 'backend', 'IntegrityError: duplicate key value', component='auth_service')
    assert eid is not None, "record_error returned None"
    print(f"  [PASS] record_error -> id={eid}")

    # Test 2: duplicate error increments count
    eid2 = record_error(conn, 'backend', 'IntegrityError: duplicate key value')
    assert eid == eid2, f"Expected same id, got {eid} vs {eid2}"
    row = conn.execute("SELECT occurrence_count FROM error_patterns WHERE id = ?", (eid,)).fetchone()
    assert row['occurrence_count'] == 2, f"Expected count=2, got {row['occurrence_count']}"
    print(f"  [PASS] duplicate error -> count=2")

    # Test 3: create_strategy
    sid = create_strategy(conn, eid, 'add_upsert', 'Use INSERT ON CONFLICT instead of plain INSERT')
    assert sid is not None, "create_strategy returned None"
    print(f"  [PASS] create_strategy -> id={sid}")

    # Test 4: duplicate strategy returns existing id
    sid2 = create_strategy(conn, eid, 'add_upsert', 'different description')
    assert sid == sid2, f"Expected same id, got {sid} vs {sid2}"
    print(f"  [PASS] duplicate strategy -> same id")

    # Test 5: get_strategies
    strategies = get_strategies(conn, error_text='IntegrityError: duplicate key value')
    assert len(strategies) == 1, f"Expected 1 strategy, got {len(strategies)}"
    assert strategies[0]['name'] == 'add_upsert'
    print(f"  [PASS] get_strategies -> {len(strategies)} strategy")

    # Test 6: record_attempt (success)
    record_attempt(conn, eid, sid, 'success', session_id='test-session', description='Added upsert')
    strat = conn.execute("SELECT success_rate, attempt_count FROM fix_strategies WHERE id = ?", (sid,)).fetchone()
    assert abs(strat['success_rate'] - 0.6) < 0.01, f"Expected ~0.6, got {strat['success_rate']}"
    assert strat['attempt_count'] == 1
    print(f"  [PASS] record_attempt success -> rate={strat['success_rate']:.2f}")

    # Test 7: record_attempt (failure)
    record_attempt(conn, eid, sid, 'failure', session_id='test-session-2')
    strat = conn.execute("SELECT success_rate FROM fix_strategies WHERE id = ?", (sid,)).fetchone()
    assert abs(strat['success_rate'] - 0.55) < 0.01, f"Expected ~0.55, got {strat['success_rate']}"
    print(f"  [PASS] record_attempt failure -> rate={strat['success_rate']:.2f}")

    # Test 8: top_errors
    top = top_errors(conn, limit=5)
    assert len(top) == 1
    assert top[0]['best_strategy'] == 'add_upsert'
    print(f"  [PASS] top_errors -> {len(top)} error(s)")

    # Test 9: normalize_error strips dynamic parts
    assert 'line N' in normalize_error('line 42')
    assert 'UUID' in normalize_error('key=550e8400-e29b-41d4-a716-446655440000')
    assert '0xADDR' in normalize_error('at 0x7fff5fbff8c0')
    print(f"  [PASS] normalize_error strips dynamic parts")

    # Test 10: score clamping
    for _ in range(50):
        record_attempt(conn, eid, sid, 'success')
    strat = conn.execute("SELECT success_rate FROM fix_strategies WHERE id = ?", (sid,)).fetchone()
    assert strat['success_rate'] <= 1.0, f"Score exceeded 1.0: {strat['success_rate']}"
    print(f"  [PASS] score clamped at 1.0 -> {strat['success_rate']:.2f}")

    conn.close()
    print(f"\nAll 10 tests PASSED.")
    return True


def main():
    parser = argparse.ArgumentParser(description='KKB Knowledge Database')
    sub = parser.add_subparsers(dest='command', help='Command')

    # record-error
    p = sub.add_parser('record-error', help='Record an error pattern')
    p.add_argument('--category', required=True, choices=['backend', 'android-unit', 'android-ui', 'android-e2e', 'build'])
    p.add_argument('--error', required=True, help='Error message/signature')
    p.add_argument('--component', help='Component name')
    p.add_argument('--file', help='File path')

    # get-strategies
    p = sub.add_parser('get-strategies', help='Get strategies for an error')
    p.add_argument('--error', help='Error message to match')
    p.add_argument('--error-id', type=int, help='Error pattern ID')

    # create-strategy
    p = sub.add_parser('create-strategy', help='Create a fix strategy')
    p.add_argument('--error-id', type=int, required=True, help='Error pattern ID')
    p.add_argument('--name', required=True, help='Strategy name')
    p.add_argument('--description', help='Strategy description')

    # record-attempt
    p = sub.add_parser('record-attempt', help='Record a fix attempt')
    p.add_argument('--error-id', type=int, required=True)
    p.add_argument('--strategy-id', type=int, required=True)
    p.add_argument('--outcome', required=True, choices=['success', 'failure', 'partial'])
    p.add_argument('--session', help='Session ID')
    p.add_argument('--description', help='Fix description')

    # import-failure-index
    sub.add_parser('import-failure-index', help='Import failure-index.json')

    # top-errors
    p = sub.add_parser('top-errors', help='Show top errors')
    p.add_argument('--limit', type=int, default=10)

    # self-test
    sub.add_parser('self-test', help='Run self-test')

    args = parser.parse_args()

    if args.command == 'self-test':
        success = self_test()
        sys.exit(0 if success else 1)

    if not args.command:
        parser.print_help()
        sys.exit(1)

    conn = get_db()
    apply_weekly_decay(conn)

    try:
        if args.command == 'record-error':
            eid = record_error(conn, args.category, args.error, args.component, args.file)
            print(json.dumps({"error_pattern_id": eid}))

        elif args.command == 'get-strategies':
            strategies = get_strategies(conn, error_text=args.error, error_id=getattr(args, 'error_id', None))
            if strategies:
                for s in strategies:
                    print(f"  [{s['success_rate']:.2f}] #{s['id']} {s['name']}: {s.get('description', '')[:80]}")
            else:
                print("UNKNOWN PATTERN")

        elif args.command == 'create-strategy':
            sid = create_strategy(conn, args.error_id, args.name, args.description)
            print(json.dumps({"strategy_id": sid}))

        elif args.command == 'record-attempt':
            record_attempt(conn, args.error_id, args.strategy_id, args.outcome,
                           session_id=args.session, description=args.description)
            print(json.dumps({"recorded": True}))

        elif args.command == 'import-failure-index':
            import_failure_index(conn)

        elif args.command == 'top-errors':
            errors = top_errors(conn, args.limit)
            if errors:
                print(f"{'ID':>4} {'Cat':<12} {'Count':>5} {'Best Strategy':<25} {'Score':>5} {'Signature':<50}")
                print("-" * 105)
                for e in errors:
                    score_str = f"{e['best_score']:.2f}" if e['best_score'] else "-"
                    strat_str = e['best_strategy'] or '-'
                    print(f"{e['id']:>4} {e['category']:<12} {e['occurrence_count']:>5} "
                          f"{strat_str:<25} {score_str:>5} "
                          f"{e['error_signature'][:50]}")
            else:
                print("No errors recorded.")
    finally:
        conn.close()


if __name__ == '__main__':
    main()
