# Fix Loop

Iterative fix cycle that analyzes failures, applies minimal fixes, runs code review gates, and optionally retests. Supports two modes — Full Loop (with retest) and Single Fix (one pass). Uses thinking escalation, debugger delegation, and structured iteration logging. Fully project-agnostic.

**Arguments:** $ARGUMENTS

Read and follow this process using the parameters passed by the caller (via `$ARGUMENTS` or inline in the calling command).

**MANDATORY — NO EXCEPTIONS.** This process runs for ALL issues including known, pre-existing, or seemingly architectural ones. The budget limits (`max_iterations`, `max_attempts_per_issue`) are the ONLY valid exit conditions. Do NOT skip iterations or short-circuit based on your own judgment about issue complexity or familiarity.

---

## Execution Modes

You operate in one of two modes based on the presence of `retest_command`:

| Mode | Trigger | Behavior |
|------|---------|----------|
| **Full Loop** | `retest_command` is provided | Run full analyze → fix → review → build → retest cycle, iterating until resolved or budget exhausted |
| **Single Fix** | `retest_command` is absent | Run ONE analyze → fix → review → build pass, then return results for the caller to retest externally |

---

## Input Parameters

### Core Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `failure_output` | Yes | Raw failure output (test errors, stack traces, assertion messages) |
| `failure_context` | Yes | What was tested and what was expected to happen |
| `files_of_interest` | Yes | File paths to read for understanding the code under test |
| `build_command` | No | Rebuild command after fix (null = skip rebuild) |
| `install_command` | No | Deploy/install command after build (e.g., install APK) |
| `retest_command` | No | Retest command. **Present = Full Loop mode, absent = Single Fix mode** |
| `retest_timeout` | No | Retest timeout in seconds (default: 300) |

### Budget Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `max_iterations` | 10 | Maximum total fix-build-test cycles |
| `max_attempts_per_issue` | 3 | Maximum attempts to fix a single discrete issue |
| `max_build_retries` | 3 | Build failures allowed before reverting the fix |

### Escalation Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `force_thinking_level` | null | Override thinking level: `"normal"` / `"thinkhard"` / `"ultrathink"` |
| `thinking_escalation` | auto | Auto-escalation: attempt 1 → normal, 2-3 → thinkhard, 4+ → ultrathink |
| `debugger_agent_name` | `"debugger"` | Agent name for deep root cause analysis |
| `code_reviewer_agent_name` | `"code-reviewer"` | Agent name for quality gate reviews |

### Behavioral Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `prohibited_actions` | `[]` | Actions you must NEVER take (e.g., `["@Ignore", "weaken assertions"]`) |
| `fix_target` | `"production"` | What to fix: `"production"` (source code only), `"test"` (test code only), `"either"` (whichever is wrong) |
| `revert_on_critical_review` | `true` | Whether to revert fixes that receive Critical code review findings |

### Logging Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `log_dir` | `".claude/logs/fix-loop/"` | Directory for iteration log files |
| `session_id` | auto-generated | Session identifier for log subdirectory |

### Single Fix Mode Extras

| Parameter | Description |
|-----------|-------------|
| `attempt_number` | Current attempt number for this issue (used for thinking escalation) |
| `previous_attempts_summary` | Summary of what was tried in prior attempts and why it failed |

---

## Algorithm — Full Loop Mode

```
INITIALIZE:
  Create {log_dir}/{session_id}/ directory
  Parse failure_output into discrete issues
  Sort issues by severity (crashes > assertion failures > warnings)
  total_iterations = 0
  results = { issues_found: N, issues_resolved: 0, fixes: [], unresolved: [] }
  metrics = { debugger_invocations: 0, code_reviews: 0, approved: 0, flagged: 0, build_failures: 0, reverts: 0 }

FOR each issue (while total_iterations < max_iterations):
  FOR attempt = 1 to max_attempts_per_issue:
    total_iterations++
    if total_iterations > max_iterations: break with MAX_ITERATIONS_EXCEEDED

    STEP 1: READ PREVIOUS LOGS
      Read all iteration-*.md files from {log_dir}/{session_id}/
      Understand what was tried, what worked, what didn't

    STEP 2: ANALYZE ROOT CAUSE
      Determine thinking level:
        - If force_thinking_level is set: use it
        - Else auto-escalate: attempt 1 → normal, 2-3 → thinkhard, 4+ → ultrathink

      normal: Analyze directly — read failure, trace to code, identify root cause
      thinkhard: Launch debugger agent (read-only) with extended thinking instruction and all prior attempt logs
      ultrathink: Launch debugger agent (read-only) with maximum depth instruction and complete history

      The debugger agent returns a root cause analysis report.
      YOU (main Claude) then apply fixes based on that analysis.

      Respect fix_target:
        - "production": only fix source/production code
        - "test": only fix test code
        - "either": fix whichever is actually wrong

    STEP 3: APPLY MINIMAL FIX
      Before applying, verify the fix does NOT involve any prohibited_actions
      Make the smallest change that addresses the root cause
      Record: { file, line, root_cause, change_description }

    STEP 4: CODE REVIEW GATE
      Launch code-reviewer agent (read-only) with:
        - The git diff of changes
        - The failure context and root cause
        - Instruction: "Review for regressions, weakened assertions, prohibited patterns, security issues. Categorize findings as Critical/High/Medium/Low. Return APPROVED or FLAGGED."

      The code-reviewer agent returns a review report.
      YOU handle the result:
        - FLAGGED with Critical finding + revert_on_critical_review:
          → Revert fix (git checkout -- <files>)
          → Log the rejection reason
          → Re-attempt with rejection context (same attempt counter)
          → metrics.reverts++
        - FLAGGED with non-Critical findings:
          → Log findings, proceed
          → metrics.flagged++
        - APPROVED:
          → Proceed
          → metrics.approved++
      metrics.code_reviews++

    STEP 5: REBUILD (if build_command provided)
      Run build_command
      If build fails:
        build_retry_count++
        If build_retry_count >= max_build_retries:
          → Revert fix, mark issue as FAILED_BUILD, break to next issue
          → metrics.build_failures++, metrics.reverts++
        Else:
          → Analyze build error, apply build fix, retry
      If build succeeds AND install_command provided:
        Run install_command

    STEP 6: RETEST (Full Loop only)
      Run retest_command with retest_timeout
      If exit code 0 → issue RESOLVED → break to next issue
      If non-zero → analyze new failure output, continue to next attempt
      If timeout → treat as failure

    STEP 7: LOG ITERATION
      Write iteration-{NNN}.md to {log_dir}/{session_id}/ with:
        - Metadata (session, iteration, issue, attempt, thinking_level, timestamp)
        - Previous iterations summary
        - Failure analysis (raw output, root cause, file, line)
        - Fix applied (file, change, diff summary)
        - Code review result (verdict, findings)
        - Build result (status, attempts)
        - Retest result (PASSED / FAILED / PENDING_CALLER_RETEST)
  END attempt loop

  If issue not resolved after max_attempts_per_issue:
    Mark as UNRESOLVED, add to results.unresolved
END issue loop

Determine overall status:
  - All issues resolved → RESOLVED
  - Some resolved, some not → PARTIALLY_RESOLVED
  - None resolved → UNRESOLVED
  - Budget exhausted with remaining issues → MAX_ITERATIONS_EXCEEDED

RETURN structured results (see Output section)
```

---

## Algorithm — Single Fix Mode

Runs exactly ONE iteration (Steps 1-5 + 7, no Step 6 retest).

Uses `attempt_number` and `previous_attempts_summary` from the caller to determine thinking level and build on prior context.

Returns the fix details for the caller to evaluate and retest externally.

---

## Edge Cases

| Edge Case | Handling |
|-----------|----------|
| No `build_command` | Skip Step 5 rebuild entirely |
| Build fails `max_build_retries` times | Revert fix, mark FAILED_BUILD, move to next issue |
| Code reviewer returns Critical | Revert fix, re-attempt with rejection context |
| `max_iterations` exceeded | Stop all processing, return MAX_ITERATIONS_EXCEEDED |
| Fix creates a NEW issue | Add the new issue to the issue queue |
| Retest times out | Treat as failure, proceed to next attempt |
| No `files_of_interest` | Infer relevant files via Grep/Glob on error messages |
| Debugger agent returns nothing actionable | Fall back to direct analysis |
| All `prohibited_actions` violated by only fix | Mark issue UNRESOLVED, log reason |

---

## Output — Full Loop Mode

Return a structured report:

```markdown
## Fix Loop Results

### Status
- **Overall:** RESOLVED | PARTIALLY_RESOLVED | UNRESOLVED | MAX_ITERATIONS_EXCEEDED
- **Iterations used:** N / max_iterations
- **Issues found:** N
- **Issues resolved:** N
- **Issues unresolved:** N

### Fixes Applied
1. [{file}:{line}] — Root cause: {description} → Fix: {change}
2. [{file}:{line}] — Root cause: {description} → Fix: {change}

### Unresolved Issues
1. {issue description} — Attempts: N, Last error: {message}
(Omit section if all resolved)

### Tracking Metrics
- Debugger invocations: N
- Code reviews: N (approved: N, flagged: N)
- Build failures: N
- Reverts: N

### Files Changed
- {file_path_1}
- {file_path_2}

### Iteration Log Directory
{log_dir}/{session_id}/
```

---

## Output — Single Fix Mode

Return a structured report:

```markdown
## Single Fix Result

### Status
- **Fix applied:** true | false
- **Review verdict:** APPROVED | FLAGGED (details)
- **Build status:** PASSED | FAILED | SKIPPED (no build_command)
- **Revert applied:** true | false

### Fix Details
- **File:** {path}
- **Line:** {line}
- **Root cause:** {description}
- **Change:** {description}
- **Thinking level:** normal | thinkhard | ultrathink

### Code Review
- **Verdict:** {verdict}
- **Findings:** {list or "none"}

### Metrics
- Debugger invocations: N
- Code reviews: 1
- Build retries: N

### Iteration Log
{log_dir}/{session_id}/iteration-{NNN}.md
```

---

## Iteration Log Format

Each iteration is written to disk at `{log_dir}/{session_id}/iteration-{NNN}.md`:

```markdown
# Iteration {NNN}

## Metadata
- Session: {session_id}
- Iteration: {NNN} / {max_iterations}
- Issue: {issue_description}
- Attempt: {M} / {max_attempts_per_issue}
- Thinking level: {normal | thinkhard | ultrathink}
- Mode: {full_loop | single_fix}
- Timestamp: {ISO 8601}

## Previous Iterations Summary
{2-3 line summary of each prior iteration}

## Failure Analysis
- Raw failure: {truncated failure output}
- Root cause: {description}
- File: {path}
- Line: {line_number}

## Fix Applied
- File: {path}
- Change: {description}
- Diff summary: {brief diff}

## Code Review
- Verdict: {APPROVED | FLAGGED}
- Findings: {list or "none"}

## Build Result
- Status: {PASSED | FAILED | SKIPPED}
- Attempts: {N}

## Retest Result
- Status: {PASSED | FAILED | TIMEOUT | PENDING_CALLER_RETEST}
```

---

## Prohibited Actions Enforcement

Before applying ANY fix, verify it does not involve any action in the `prohibited_actions` list. Common prohibited actions include:
- Adding `@Ignore` or `@Disabled` annotations to tests
- Weakening assertions (e.g., changing `assertEquals` to `assertTrue`, removing assertions)
- Deleting or commenting out test methods
- Adding `Thread.sleep()` as a timing fix
- Creating "fix later" issues to bypass failures
- Skipping test groups or suites
- Classifying a screen/step as PASS when any issue was detected
- Downgrading issues to "observations", "findings", or "notes" to avoid fix-loop
- Skipping visual verification without setting visual_verified=false
- Bypassing the Pre-Classification Gate (E5.7)

If the ONLY viable fix would violate a prohibited action, mark the issue as UNRESOLVED with the reason "Only available fix violates prohibited action: {action}".

---

## Thinking Escalation Details

| Level | When | Approach |
|-------|------|----------|
| **normal** | Attempt 1 (or forced) | Analyze directly — read failure output, trace to source code, identify root cause |
| **thinkhard** | Attempt 2-3 (or forced) | Launch debugger agent (read-only) with instruction to use extended thinking, all prior attempt logs, and systematic root cause enumeration |
| **ultrathink** | Attempt 4+ (or forced) | Launch debugger agent (read-only) with maximum thinking depth, complete history, instruction to re-examine all assumptions and explore unconventional fixes |

When launching the debugger agent, always include:
- Complete failure output
- All files of interest
- Summary of all previous fix attempts and why they failed
- The specific thinking level instruction

The debugger agent returns analysis only — YOU apply the fixes directly.
