# Workflow Gap Analysis: Command Invocation Chain

**Date:** 2026-02-13
**Scope:** Full audit of 6 `.claude/commands/` files for invocation gaps, inconsistencies, and best-practice violations.

---

## Summary

| Severity | Count | Fixed |
|----------|-------|-------|
| CRITICAL | 3 | GAP-01, GAP-02, GAP-03 |
| HIGH | 4 | GAP-04, GAP-05, GAP-06, GAP-07 |
| MEDIUM | 5 | GAP-08, GAP-09, GAP-10, GAP-11, GAP-12 |
| LOW | 2 | GAP-13, GAP-14 |

---

## Gap Details

### CRITICAL

| ID | Gap | Where | Impact | Fix |
|----|-----|-------|--------|-----|
| GAP-01 | "Read and follow" invocation language | adb-test.md, run-e2e.md, implement.md, fix-issue.md | fix-loop and post-fix-pipeline never formally invoked as Skills. Claude inlines behavior, losing iteration tracking, thinking escalation, code review gate, budget enforcement. | Replaced with explicit `skill: "fix-loop"` / `skill: "post-fix-pipeline"` Skill tool invocation blocks in all 4 callers. |
| GAP-02 | adb-test.md exceeds reliable prompt length (1,114 lines) | adb-test.md | Instructions from later sections lost due to context compression. Industry best practice: <500 lines. | Extracted 13 ADB patterns to `docs/testing/adb-patterns.md`. Removed fix-loop duplication. Reduced to ~700 lines. |
| GAP-03 | implement.md and fix-issue.md skip post-fix-pipeline | implement.md Step 7, fix-issue.md Step 7 | Manual commits without test suite gate, docs update, or standardized commit format. | Added post-fix-pipeline Skill invocation to both commands' commit steps. |

### HIGH

| ID | Gap | Where | Impact | Fix |
|----|-----|-------|--------|-----|
| GAP-04 | No explicit Skill tool reference | All 6 command files | None mention "Skill tool" or the invocation mechanism. | Added explicit `skill: "fix-loop"` and `skill: "post-fix-pipeline"` invocation patterns with "Do NOT read and follow inline" warnings. |
| GAP-05 | No reinforcement anchors in long commands | adb-test.md, run-e2e.md | Long commands need periodic reminders to survive context compression. | Added 3 reinforcement anchors in adb-test.md, 2 in run-e2e.md. |
| GAP-06 | Duplicated fix-loop instructions | adb-test.md:585-661 duplicates fix-loop.md | Inline version drifts from canonical fix-loop.md; Claude uses inline instead of invoking Skill. | Removed duplicated logic from adb-test.md Section F. Replaced with Skill invocation block. |
| GAP-07 | Flow execution (Section G) has weaker fix integration than screen testing | adb-test.md:1050-1056 | Only 4-line reference vs screen testing's 76-line Section F. | Added explicit Skill invocation block with full parameter template for flow step failures. |

### MEDIUM

| ID | Gap | Where | Impact | Fix |
|----|-----|-------|--------|-----|
| GAP-08 | Inconsistent budget parameters | All callers | Different budgets with no documented rationale. | Added budget rationale comments in each caller. Documented defaults in fix-loop.md parameter table. |
| GAP-09 | Thinking escalation defined in 3 places | adb-test.md, fix-loop.md, run-e2e.md | Three different escalation tables. | Removed from callers. fix-loop.md is now the single source of truth. Callers reference it. |
| GAP-10 | Agent vs Command vs Skill terminology confusion | All 6 files | "Agent", "command", "skill" used interchangeably. | Standardized: Skills = `.claude/commands/*.md` (Skill tool), Agents = `.claude/agents/*.md` (Task tool). Renamed "Agent Integration" sections to "Skill Integration". |
| GAP-11 | post-fix-pipeline test_suite_commands empty in run-e2e.md | run-e2e.md:316 | Skips test suite gate entirely, defeating the purpose. | Added actual test suite commands (backend pytest + android unit tests). |
| GAP-12 | Co-Authored-By tag outdated ("Opus 4.5") | implement.md:203, fix-issue.md:89 | Indicates staleness. | Updated to "Opus 4.6" in both files. |

### LOW

| ID | Gap | Where | Impact | Fix |
|----|-----|-------|--------|-----|
| GAP-13 | No verification that Skill was actually invoked | All callers | Silent failure when Claude inlines instead of using Skill tool. | Added explicit "Do NOT read fix-loop.md and follow it inline" warnings in all Skill invocation blocks. |
| GAP-14 | fix-loop.md parameters described in prose, not structured | fix-loop.md | Callers copy-paste inconsistently. | Reorganized into Required vs Optional tables with defaults and valid ranges. |

---

## Best Practices Applied

### BP-01: Explicit Skill Invocation
Skills must be invoked via the Skill tool, not by reading the file and following inline. Pattern: `Use the Skill tool to invoke /fix-loop with arguments: ...`

### BP-02: Prompt Length
LLM prompt documents should stay under ~500 lines (ideal) or ~700 lines (acceptable). Mitigation: modular decomposition, reinforcement anchors, front-loaded rules, reference docs via Read.

### BP-03: Unified Quality Gate
All paths to a commit route through post-fix-pipeline. No manual commit bypasses.

### BP-04: DRY for Command Systems
Define behavior once (in the callee), reference from callers. Removed duplicated fix-loop logic from adb-test.md.

### BP-05: Consistent Terminology
- **Skill** = `.claude/commands/*.md` invoked via Skill tool
- **Agent** = `.claude/agents/*.md` launched via Task tool
- **Hook** = `.claude/hooks/*.sh` triggered by events

### BP-06: Reinforcement Anchors
Bold reminders of critical rules every ~200 lines in long prompts.

### BP-07: Parameter Standardization
fix-loop.md documents Required vs Optional parameters with defaults and valid ranges.

---

## Files Modified

| File | Change Type |
|------|------------|
| `.claude/commands/adb-test.md` | Restructured (extracted ADB patterns, Skill invocations, anchors) |
| `.claude/commands/run-e2e.md` | Skill invocations, anchors, test_suite_commands fix |
| `.claude/commands/implement.md` | Skill invocations, post-fix-pipeline Step 7, Co-Authored-By |
| `.claude/commands/fix-issue.md` | Skill invocations, post-fix-pipeline Step 7, Co-Authored-By |
| `.claude/commands/fix-loop.md` | Parameter docs, canonical escalation, terminology |
| `.claude/commands/post-fix-pipeline.md` | Caller contexts, terminology, validation |
| `docs/testing/adb-patterns.md` | NEW: 13 ADB patterns reference doc |
| `docs/claude-docs/workflow-gap-analysis.md` | NEW: This document |
