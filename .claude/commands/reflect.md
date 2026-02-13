# Reflect — Learning System Analysis & Self-Modification

Analyze skill outcomes, update memory, and optionally self-modify skills/hooks to close gaps.

**Arguments:** $ARGUMENTS

---

## MODE SELECTION

| Mode | Trigger | Modifies files? | Time |
|------|---------|-----------------|------|
| `session` (default) | Auto-invoked after skills, or `/reflect` with no args | Memory topic files only | <60s |
| `deep` | `/reflect deep` | Memory + Skills + Hooks | <120s |
| `meta` | `/reflect meta` | Memory only | <60s |
| `test-run` | `/reflect test-run` | No (dry-run analysis) | <90s |

Parse `$ARGUMENTS`:
- Empty or `session` → session mode
- Contains `deep` → deep mode
- Contains `meta` → meta mode
- Contains `test-run` → test-run mode
- `--depth N` → set recursion depth (used internally by auto-invocation)

---

## SELF-SKIP RULE

This skill MUST NOT invoke itself. The `post-skill-learning.sh` hook already skips capturing `/reflect` invocations. If during recursion (deep mode) a re-run triggers `/reflect`, it must detect and break the cycle.

---

## STEP 1: GATHER

Read the following data sources (adjust scope by mode):

### All Modes
1. **Learning captures** — Read all JSON files from `.claude/logs/learning/` (most recent 7 days):
   ```bash
   find .claude/logs/learning/ -name "capture-*.json" -newer .claude/logs/learning/ -mtime -7 2>/dev/null | sort -r | head -50
   ```
   Parse each capture for: skillName, outcome, issuesFound, issuesResolved, fixesApplied, unresolvedItems.

2. **Memory topic files** — Read all 4 topic files:
   - `memory/testing-lessons.md`
   - `memory/fix-patterns.md`
   - `memory/skill-gaps.md`
   - `memory/meta-reflections.md`

3. **Workflow session log** — Read last 200 lines of `.claude/logs/workflow-sessions.log`

4. **Modification history** — Read `.claude/logs/learning/modifications.json`

### Deep & Test-Run Modes (additional)
5. **Skill definitions** — Read all `.claude/commands/*.md` files
6. **Hook definitions** — Read all `.claude/hooks/*.sh` files
7. **Recursion state** — Read `.claude/logs/learning/recursion-state.json`

---

## STEP 2: ANALYZE

Produce a structured analysis table:

### Per-Skill Success Rates
```
| Skill | Invocations | Resolved | Partial | Unresolved | Success Rate |
|-------|-------------|----------|---------|------------|--------------|
| adb-test | 5 | 3 | 1 | 1 | 60% |
| fix-loop | 12 | 9 | 2 | 1 | 75% |
| ...
```

### Recurring Root Causes
Identify root causes that appear 2+ times across captures:
```
| Root Cause | Occurrences | Skills Affected | Last Seen |
|------------|-------------|-----------------|-----------|
| HTTP 500 recipe not found | 3 | adb-test, fix-loop | 2026-02-13 |
| ...
```

### Fix Pattern Frequency
Which fix patterns are applied most often:
```
| Pattern | Count | Example File | Success After Fix |
|---------|-------|-------------|-------------------|
| Missing null check | 4 | RecipeRepository.kt | 100% |
| ...
```

### Persistent Gaps
Issues in `skill-gaps.md` that remain open across 2+ sessions:
```
| Gap | Sessions Open | Skill | Severity |
|-----|---------------|-------|----------|
| No auto-file for UNRESOLVED | 3 | adb-test | High |
| ...
```

### Duration Trends
Average skill execution time trending up or down.

---

## STEP 3: UPDATE MEMORY

Based on the analysis, update memory topic files:

1. **testing-lessons.md** — Append new patterns discovered (e.g., "HTTP 500 on recipe detail is a backend data issue, not Android")
2. **fix-patterns.md** — Add newly confirmed fix patterns with success rate
3. **skill-gaps.md** — Update gap statuses (mark resolved gaps, add new ones)
4. **meta-reflections.md** — (meta mode only) Append meta-insight

**MEMORY.md** — Only touch for critical cross-session insights that should be in the system prompt. Keep under 200 lines. Check current line count before editing.

Use the `append_memory_topic()` pattern: timestamped entries, 500-line limit per topic file.

---

## STEP 4: PROPOSE MODIFICATIONS (deep & test-run modes only)

For each identified gap or recurring failure, propose a specific modification:

```
| # | Target File | Reason | Change Description | Lines | Risk |
|---|-------------|--------|--------------------|-------|------|
| 1 | .claude/commands/adb-test.md | Missing auto-file for UNRESOLVED | Add F5.5 section for gh issue create | +25 | Low |
| 2 | .claude/hooks/post-test-update.sh | Test result not captured for backend | Add pytest output parsing | +10 | Low |
| ...
```

**Prioritization:** Impact (high → low) then Risk (low → high).

**test-run mode:** Output the table and STOP. Do not apply modifications.

---

## STEP 5: APPLY MODIFICATIONS (deep mode only)

### Safety Protocol (MANDATORY)

Before applying ANY modification:

1. **Git stash** — Save current uncommitted changes:
   ```bash
   STASH_REF=$(git stash push -m "reflect-deep-$(date +%Y%m%d%H%M%S)" 2>&1)
   ```
   Record `stashRef` in recursion-state.json.

2. **Deny list check** — NEVER modify these files:
   - `CLAUDE.md` (protected section or any part)
   - `backend/tests/conftest.py`
   - `android/build.gradle.kts` or `android/app/build.gradle.kts`
   - `.claude/settings.json` (hook registration is separate from skill content)
   - Any `*.env` file
   - Any file in `backend/alembic/versions/`

   If a proposed modification targets a deny-listed file → SKIP it.

3. **Uncommitted changes check** — For each target file:
   ```bash
   git diff --name-only | grep -q "{file}"
   ```
   If the file has uncommitted changes → SKIP it to avoid conflict.

4. **Limits:**
   - Maximum 5 files per session
   - Maximum 50 lines changed per file
   - If a modification exceeds these limits → SKIP it, log reason

### Apply Each Modification

For each approved modification (up to 5):
1. Read the target file
2. Apply the edit using Edit tool
3. Validate:
   - `.sh` files: `bash -n {file}` — must exit 0
   - `.md` files: check heading structure preserved, file size < 50KB
   - `.json` files: `python -c "import json; json.load(open('{file}'))"` — must succeed
4. If validation fails → `git checkout -- {file}` and skip

### Record Modifications

Append to `.claude/logs/learning/modifications.json`:
```json
{
  "sessionId": "reflect-{timestamp}",
  "timestamp": "ISO8601",
  "file": "{path}",
  "reason": "{gap description}",
  "linesAdded": N,
  "linesRemoved": N,
  "validated": true,
  "result": "APPLIED|REVERTED|SKIPPED"
}
```

---

## STEP 6: RECURSE (deep mode, if modifications applied)

### Recursion Protocol

1. **Read recursion state:**
   ```bash
   cat .claude/logs/learning/recursion-state.json
   ```

2. **Check depth:**
   - If `currentDepth >= maxDepth` (3) → go to STEP 7 (meta-reflect), do NOT recurse
   - If no modifications were applied → STOP (converged)

3. **Update recursion state:**
   ```json
   {
     "currentDepth": N+1,
     "sessionId": "reflect-{timestamp}",
     "stashRef": "{from step 5}",
     "chain": [ ...previous, { "depth": N, "action": "reflect", "mode": "deep", "modifications_applied": M, "result": "MODIFICATIONS_APPLIED" } ]
   }
   ```

4. **Re-run the most affected skill** — Identify which skill had the worst success rate. Invoke it via Skill tool:
   ```
   Skill("{skill_name}", args="{minimal args to trigger a representative test}")
   ```
   This generates a new capture via post-skill-learning.sh.

5. **Evaluate re-run results:**
   - Read the new capture JSON
   - Compare outcome to the pre-modification baseline
   - Classify: **IMPROVED** (better success rate), **NEUTRAL** (same), **DEGRADED** (worse)

6. **Decision:**
   - **IMPROVED or NEUTRAL:** Keep modifications. Invoke `/reflect session --depth {N+1}` to capture the result.
   - **DEGRADED:** Auto-revert ALL modifications from this session:
     ```bash
     git checkout -- {file1} {file2} ...
     ```
     Log revert in modifications.json. Reset recursion depth.

### Termination Conditions
- `currentDepth >= maxDepth` → go to meta-reflect
- No modifications proposed → converged, stop
- Re-run shows no change → converged, stop
- Re-run shows DEGRADED → revert + stop
- Total elapsed time > 10 minutes → stop
- Consecutive NEUTRAL results >= 2 → converged, stop

---

## STEP 7: META-REFLECT (meta mode, or depth=3 in deep mode)

### Meta-Analysis

1. **Read meta-reflections.md** history
2. **Read modifications.json** — all historical modifications

3. **Analyze:**
   - **Improvement rate:** What % of modifications led to IMPROVED outcomes?
   - **Best modification types:** Which categories of changes (skill updates, hook fixes, memory additions) have highest success?
   - **Convergence:** Is the system improving over time, or oscillating?
   - **Diminishing returns:** Are recent modifications having less impact than earlier ones?

4. **Output meta-insight:**
   ```
   ### Meta-Reflection: {session_id}

   **Improvement rate:** X% of modifications improved outcomes
   **Best strategies:** {list top 3 modification types by effectiveness}
   **Convergence:** IMPROVING | PLATEAUED | OSCILLATING
   **Recommendation:** {next action — e.g., "Focus on backend error handling patterns" or "System is converged, no deep reflect needed"}
   ```

5. **Append to meta-reflections.md** via `append_memory_topic()`

6. **Reset recursion state:**
   ```json
   { "currentDepth": 0, "maxDepth": 3, "sessionId": null, "stashRef": null, "chain": [] }
   ```

---

## OUTPUT FORMAT

### Session Mode
```
## Reflect: Session Analysis

### Captures Analyzed: N (from {date_range})
### Skill Success Rates
{table from Step 2}

### New Insights
- {insight 1}
- {insight 2}

### Memory Updates
- testing-lessons.md: +{N} entries
- fix-patterns.md: +{N} entries
- skill-gaps.md: {N} gaps updated

### Duration: {seconds}s
```

### Deep Mode
```
## Reflect: Deep Analysis & Modification

### Analysis
{tables from Step 2}

### Modifications Applied
| # | File | Change | Lines | Validated | Result |
|---|------|--------|-------|-----------|--------|
{table}

### Recursion
- Depth: {N}/{maxDepth}
- Re-run skill: {name}
- Re-run result: IMPROVED | NEUTRAL | DEGRADED
- Action: KEPT | REVERTED

### Memory Updates
{same as session}

### Duration: {seconds}s
```

### Meta Mode
```
## Reflect: Meta-Analysis

### Historical Modifications: {N total}
### Improvement Rate: {X}%
### Best Strategies
1. {strategy}: {success_rate}%
2. ...

### Convergence: IMPROVING | PLATEAUED | OSCILLATING
### Recommendation: {action}

### Duration: {seconds}s
```

### Test-Run Mode
```
## Reflect: Test-Run (Dry Run)

### Analysis
{tables from Step 2}

### Proposed Modifications (NOT applied)
{table from Step 4}

### Would affect: {N} files, ~{N} lines

### Duration: {seconds}s
```

---

## QUICK REFERENCE

| Mode | Reads | Writes Memory | Modifies Skills/Hooks | Recurses |
|------|-------|---------------|----------------------|----------|
| session | captures, topics, log | Yes | No | No |
| deep | + skill/hook defs | Yes | Yes (with safety) | Yes (max 3) |
| meta | topics, modifications | Yes (meta only) | No | No |
| test-run | + skill/hook defs | No | No (dry-run) | No |

| Safety Guard | Description |
|-------------|-------------|
| Git stash | Before any modification |
| Deny list | CLAUDE.md, conftest.py, build files, .env |
| Uncommitted check | Skip files with local changes |
| Validation | bash -n for .sh, JSON parse for .json, size for .md |
| Limits | 5 files/session, 50 lines/file |
| Auto-revert | If re-run shows DEGRADED results |
| Depth limit | Max 3 recursive levels |
| Time limit | 10 minutes total |
