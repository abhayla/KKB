# Best Practices Hub — Design Document

> **Date:** 2026-03-09
> **Status:** Approved
> **Approach:** Hybrid — GitHub Template Repo + User-Level Sync + Internet Scanning
> **Codename:** Claude Practices Hub

---

## Problem Statement

When starting new projects, best practices, skills, agents, hooks, and rules developed in previous projects are lost or require manual copy-paste. There is no automated way to:
1. Collate reusable patterns from multiple active projects into a central repository
2. Discover best practices from the internet and evaluate them against existing patterns
3. Sync improvements back to projects that use them
4. Bootstrap new projects with proven patterns

## Solution Overview

A GitHub template repository (`claude-best-practices`) that serves three roles simultaneously:

1. **Starter template** — "Use this template" on GitHub to bootstrap new projects with a layered `.claude/` skeleton
2. **Living knowledge base** — GitHub Actions scan project repos and internet sources, proposing new patterns via PRs
3. **Auto-sync system** — When patterns are updated in the hub, PRs are created to registered project repos

All changes go through PR-based review. Nothing auto-merges.

### Sync Architecture

```
┌──────────────┐         ┌──────────────────┐         ┌──────────────┐
│  Your        │────①───>│  claude-best-    │────③───>│  ~/.claude/  │
│  Projects    │<───④────│  practices (Hub) │         │  (Local)     │
│  (N repos)   │         │  (GitHub)        │<───②────│              │
└──────────────┘         └──────────────────┘         └──────────────┘
                                 ▲
                                 │⑤
                         ┌───────┴────────┐
                         │   Internet     │
                         │  (URLs/Search) │
                         └────────────────┘

① Project → Hub:    Weekly cron + manual dispatch. Scans registered repos for new/updated patterns.
② Local → Hub:      /contribute-practice skill. Creates PR from project to hub.
③ Hub → Local:      /update-practices skill. Pulls latest from hub into ~/.claude/ or .claude/.
④ Hub → Projects:   On merge to main. Creates PRs to registered project repos with updates.
⑤ Internet → Hub:   Weekly cron + manual dispatch. Searches topics, fetches URLs, extracts patterns.
```

---

## Section 1: Repository Structure

```
claude-best-practices/
├── core/                                 ← Always included (universal patterns)
│   ├── .claude/
│   │   ├── skills/
│   │   │   ├── fix-loop/SKILL.md
│   │   │   ├── implement/SKILL.md
│   │   │   ├── auto-verify/SKILL.md
│   │   │   ├── update-practices/SKILL.md    ← Pull from hub
│   │   │   ├── contribute-practice/SKILL.md ← Push to hub
│   │   │   ├── scan-url/SKILL.md            ← Trigger internet scan
│   │   │   ├── scan-repo/SKILL.md           ← Trigger project scan
│   │   │   └── ...
│   │   ├── agents/
│   │   │   ├── code-reviewer.md
│   │   │   ├── debugger.md
│   │   │   ├── test-failure-analyzer.md
│   │   │   └── ...
│   │   ├── hooks/
│   │   │   ├── hook-utils.sh
│   │   │   ├── auto-format.sh
│   │   │   ├── validate-workflow-step.sh
│   │   │   └── ...
│   │   └── rules/
│   │       └── workflow.md
│   ├── CLAUDE.md.template                ← Parameterized ({{PROJECT_NAME}}, {{STACK}})
│   └── CLAUDE.local.md.template
│
├── stacks/                               ← Opt-in layers
│   ├── superpowers/                      ← Brainstorming, TDD, debugging, code-review skills
│   │   ├── .claude/skills/
│   │   ├── stack-config.yml
│   │   └── examples/
│   ├── android-compose/
│   │   ├── .claude/skills/               ← run-android-tests, adb-test
│   │   ├── .claude/agents/               ← android-compose agent
│   │   ├── .claude/rules/                ← android.md, compose-ui.md
│   │   ├── stack-config.yml              ← Metadata (name, desc, namespace, dependencies, conflicts)
│   │   └── examples/                     ← Sample CLAUDE.md snippet, sample test, sample hook usage
│   ├── fastapi-python/
│   │   ├── .claude/skills/               ← run-backend-tests, db-migrate
│   │   ├── .claude/rules/                ← backend.md, database.md
│   │   ├── stack-config.yml
│   │   └── examples/
│   ├── ai-gemini/
│   │   ├── .claude/skills/               ← gemini-api, generate-meal
│   │   ├── stack-config.yml
│   │   └── examples/
│   ├── firebase-auth/
│   │   ├── stack-config.yml
│   │   └── examples/
│   └── react-nextjs/                     ← Future stacks
│       ├── stack-config.yml
│       └── examples/
│
├── internet-sources/                     ← Discovered patterns (staging area)
│   ├── pending/                          ← Awaiting review (PRs reference these)
│   └── archived/                         ← Previously reviewed (accepted/rejected)
│
├── registry/
│   ├── patterns.json                     ← Hash-based dedup + provenance + versions + dependencies
│   └── changelog.md                      ← Human-readable history of additions
│
├── config/                               ← Split configuration
│   ├── repos.yml                         ← Project repos to scan
│   ├── topics.yml                        ← Topics to monitor
│   ├── urls.yml                          ← URLs/blogs to watch (+ last_verified, expires_after, trust_level)
│   ├── settings.yml                      ← Frequency, PR labels, notification prefs
│   └── .secretsignore                    ← Patterns to never sync (private/sensitive)
│
├── docs/                                 ← Auto-updated documentation
│   ├── GETTING-STARTED.md                ← Regenerated on push to main
│   ├── STACK-CATALOG.md                  ← Skills/agents/hooks per stack
│   ├── CHANGELOG.md                      ← Pattern addition history
│   ├── DASHBOARD.md                      ← Stats, metrics, sync status (markdown)
│   ├── dashboard.html                    ← Rich interactive dashboard (single self-contained file)
│   └── SYNC-ARCHITECTURE.md             ← Flow diagrams + conflict resolution
│
├── .github/workflows/
│   ├── scan-projects.yml                 ← Weekly + manual dispatch (with repo input)
│   ├── scan-internet.yml                 ← Weekly + manual dispatch (with url/topic input)
│   ├── propose-updates.yml               ← Creates PRs from scan results
│   ├── sync-to-projects.yml              ← On merge to main: propose updates to registered repos
│   ├── update-docs.yml                   ← On push to main: regenerate all docs + dashboard
│   ├── validate-pr.yml                   ← PR quality gate (integrity, deps, secrets, templates)
│   ├── expire-sources.yml                ← Weekly: flag expired internet sources
│   └── test.yml                          ← Unit + integration tests on PR/push
│
├── scripts/
│   ├── collate.py                        ← Extract patterns from project repos
│   ├── scan_web.py                       ← Fetch + parse URLs for best practices
│   ├── dedup_check.py                    ← 3-level dedup (hash, structural, semantic)
│   ├── bootstrap.py                      ← Interactive project setup
│   ├── sync_to_local.py                  ← Update ~/.claude/ from hub
│   ├── sync_to_projects.py              ← Create PRs to registered project repos
│   ├── generate_docs.py                  ← Generate DASHBOARD, STACK-CATALOG, GETTING-STARTED, etc.
│   ├── check_freshness.py               ← Flag expired internet sources
│   └── tests/                            ← Unit test suite
│       ├── test_collate.py
│       ├── test_scan_web.py
│       ├── test_dedup_check.py
│       ├── test_bootstrap.py
│       ├── test_sync_to_local.py
│       ├── test_generate_docs.py
│       ├── conftest.py
│       └── fixtures/                     ← Sample patterns, mock registry, test HTML
│           ├── sample_skill/SKILL.md
│           ├── sample_agent.md
│           ├── sample_hook.sh
│           ├── invalid_skill/SKILL.md
│           ├── duplicate_skill/SKILL.md
│           ├── sample_registry.json
│           ├── sample_urls.yml
│           └── sample_webpage.html
│
├── bootstrap.sh                          ← One-liner setup for new projects
└── README.md
```

### Stack Config Format

```yaml
# stacks/android-compose/stack-config.yml
name: android-compose
description: Android Jetpack Compose with Hilt DI, Room DB, Navigation Compose
namespace: android                        # Prevents file collisions across stacks
conflicts_with: []                        # Stacks that can't be used together
merges_with: [fastapi-python, superpowers] # Known compatible stacks
file_precedence: stack                    # Stack files override core if same name
dependencies: []                          # Required stacks (e.g., "core" is implicit)
```

### Pattern Registry Entry Format

```json
{
  "fix-loop": {
    "hash": "a1b2c3d4e5f6...",
    "type": "skill",
    "category": "core",
    "version": "1.2.0",
    "source": "project:abhayla/KKB",
    "discovered": "2026-02-10",
    "last_updated": "2026-03-05",
    "dependencies": ["hook-utils.sh", "test-failure-analyzer"],
    "visibility": "public",
    "description": "Iterative test-fix cycle with thinking escalation",
    "tags": ["testing", "debugging", "workflow"],
    "changelog": "v1.2: Added thinking escalation"
  }
}
```

### URL Watchlist Format

```yaml
# config/urls.yml
- url: https://code.claude.com/docs/en/skills
  last_verified: 2026-03-09
  expires_after: 90d
  trust_level: high
- url: https://blog.sshh.io/p/claude-code-tips
  last_verified: 2026-03-05
  expires_after: 90d
  trust_level: high
```

### Version Strategy

- Each skill/agent/hook/rule has a `version` field in its frontmatter
- Hub repo uses git tags (`v1.0`, `v1.1`) for release tracking
- Projects can pin to a version via `sync-config.yml`
- `patterns.json` tracks version per pattern for granular updates

---

## Section 2: Sync Flow & Architecture

### Flow 1: Project → Hub (Discovery)

```
Trigger: Weekly cron OR gh workflow run scan-projects.yml -f repo="owner/repo"

scan-projects.yml
  ├─ Clone registered repos (from config/repos.yml)
  ├─ For each repo:
  │   ├─ Extract .claude/ directory
  │   ├─ Diff each skill/agent/hook/rule against hub's registry
  │   │   ├─ Hash content (SHA256) → compare with patterns.json
  │   │   ├─ NEW pattern? → stage in internet-sources/pending/
  │   │   ├─ UPDATED pattern? → stage with diff summary
  │   │   └─ IDENTICAL? → skip
  │   └─ Check dependency integrity
  ├─ Run dedup_check.py (cross-check all staged patterns)
  └─ Create PR to hub with staged patterns + updated patterns.json
```

### Flow 2: Manual Contribution (Local → Hub)

```
Trigger: /contribute-practice <path> from within any project

Skill: contribute-practice/SKILL.md
  ├─ Validate input (valid pattern, has frontmatter, has version)
  ├─ Categorize (core vs stack, auto-detect or user flag)
  ├─ Dependency scan (grep references, check hub has deps)
  ├─ Dedup check against patterns.json
  │   ├─ Duplicate → prompt to update existing
  │   └─ New → proceed
  ├─ Assign version (1.0.0 for new, bump for update)
  └─ Create PR to hub via gh pr create
```

### Flow 3: Hub → Local (Pull)

```
Trigger: /update-practices from any Claude Code session

Skill: update-practices/SKILL.md
  ├─ Fetch hub state via GitHub API
  ├─ Compare versions: hub vs local for each pattern
  │   ├─ UPDATEABLE (hub newer)
  │   ├─ CURRENT (same version)
  │   ├─ CONFLICT (both modified)
  │   └─ AVAILABLE (new in hub)
  ├─ Present summary with changelog per pattern
  ├─ User approves (all / select individually / skip)
  ├─ Copy files, validate dependencies, update sync-config.yml
  └─ Git commit (atomic, revertable)
```

### Flow 4: Hub → Projects (Auto-Propose)

```
Trigger: Push to main on hub (after merging a PR)

sync-to-projects.yml
  ├─ Read config/repos.yml for registered projects
  ├─ For each project:
  │   ├─ Check which stacks the project uses (from project's sync-config.yml)
  │   ├─ Diff hub's core + relevant stacks vs project's .claude/
  │   └─ Create PR to PROJECT repo if updates available
  └─ Update registry with sync status per project
```

### Flow 5: Internet → Hub (Scanning)

```
Trigger: Weekly cron OR gh workflow run scan-internet.yml -f url="..." or -f topic="..."

scan-internet.yml
  ├─ Mode A: Topic search (from config/topics.yml)
  ├─ Mode B: URL watch (from config/urls.yml, compare content hash)
  ├─ Mode C: Direct URL (from workflow_dispatch input)
  ├─ For each fetched page:
  │   ├─ Extract patterns via Claude API (Haiku for cost efficiency)
  │   ├─ Dedup check (all 3 levels)
  │   └─ Stage in internet-sources/pending/
  ├─ Update urls.yml timestamps
  └─ Create PR to hub
```

### Conflict Resolution

| Scenario | Resolution |
|----------|------------|
| Hub and project both modified same pattern | PR shows both versions; human picks |
| Two projects contribute same pattern | Dedup catches it; second PR links to first |
| Internet pattern duplicates existing | Semantic check flags it; PR shows comparison |
| Stack file overlap (two stacks define same file) | `stack-config.yml` namespace prevents collision; `file_precedence` decides |
| Dependency missing | PR flagged with `needs-dependency` label; won't auto-sync |

---

## Section 3: GitHub Actions Pipeline

### 3.1 Workflows

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `scan-projects.yml` | Weekly Monday 9am UTC + `workflow_dispatch(repo)` | Scan registered repos for new/updated patterns |
| `scan-internet.yml` | Weekly Monday 10am UTC + `workflow_dispatch(url, topic)` | Search topics, fetch URLs, extract patterns |
| `sync-to-projects.yml` | Push to main (paths: core/, stacks/, registry/) | Create PRs to registered project repos |
| `update-docs.yml` | Push to main (paths: core/, stacks/, registry/, config/) | Regenerate DASHBOARD.md, dashboard.html, STACK-CATALOG.md, etc. |
| `validate-pr.yml` | Pull request (opened, synchronize) | Quality gate: integrity, deps, secrets, templates |
| `expire-sources.yml` | Weekly Monday 8am UTC | Flag expired internet sources, create maintenance issue |
| `test.yml` | PR + push to main (paths: scripts/) | Unit + integration tests |

### 3.2 Workflow Dispatch Inputs

```yaml
# scan-internet.yml
workflow_dispatch:
  inputs:
    url:
      description: 'Specific URL to scan'
      type: string
      required: false
    topic:
      description: 'Specific topic to search'
      type: string
      required: false

# scan-projects.yml
workflow_dispatch:
  inputs:
    repo:
      description: 'Specific repo (owner/repo)'
      type: string
      required: false
```

### 3.3 Triggering via CLI

```bash
# Scan a specific URL
gh workflow run scan-internet.yml -f url="https://blog.example.com/claude-tips" --repo owner/claude-best-practices

# Scan a specific topic
gh workflow run scan-internet.yml -f topic="Jetpack Compose testing" --repo owner/claude-best-practices

# Scan a specific repo
gh workflow run scan-projects.yml -f repo="owner/my-project" --repo owner/claude-best-practices
```

### 3.4 Secrets Required

| Secret | Purpose |
|--------|---------|
| `PRACTICES_PAT` | GitHub PAT with repo read/write access for cross-repo operations |
| `ANTHROPIC_API_KEY` | Claude API for semantic dedup (Level 3) and pattern extraction |

### 3.5 Cost Estimate (GitHub Free Tier)

| Action | Minutes/run | Runs/month | Total |
|--------|------------|------------|-------|
| scan-projects | ~5 min | 4-5 | ~25 min |
| scan-internet | ~3 min | 4-5 | ~15 min |
| sync-to-projects | ~2 min x N repos | 2-3 | ~12 min |
| update-docs | ~1 min | 4-5 | ~5 min |
| validate-pr | ~1 min | 8-10 | ~10 min |
| expire-sources | ~1 min | 4 | ~4 min |
| **Total** | | | **~71 min/month** |

GitHub free tier: 2,000 min/month. Well within limits.

### 3.6 Auto-Updated Documentation

Both markdown and HTML dashboards are regenerated on every push to main:

**DASHBOARD.md** — GitHub-rendered stats: pattern inventory, project sync status, internet source health, scan history, dependency graph, quick action commands.

**dashboard.html** — Rich single-file HTML with: search/filter across all tables, collapsible sections, SVG dependency graph, color-coded status badges, sortable columns, dark/light mode, inline diff viewer, copy-paste quick actions, responsive design.

---

## Section 4: Skill Specifications

### 4.1 `/update-practices` — Pull from Hub

```yaml
---
name: update-practices
description: >
  Pull latest best practices from the central hub into your local
  ~/.claude/ or project .claude/. Shows changelog, lets you approve
  individually or in bulk.
allowed-tools: "Bash Read Grep Glob Write Edit"
argument-hint: "[--project] [--dry-run]"
---
```

**Flow:** Read sync-config.yml → fetch hub state via GitHub API → compare versions → present summary (updates, new, conflicts) → user approves → copy files → validate dependencies → git commit → update sync-config.yml.

**Flags:**
- `--project` — Sync to `.claude/` (project-level) instead of `~/.claude/` (user-level)
- `--dry-run` — Show what would change without applying

### 4.2 `/contribute-practice` — Push to Hub

```yaml
---
name: contribute-practice
description: >
  Contribute a skill, agent, hook, or rule from this project to the
  central best practices hub. Creates a PR for review.
allowed-tools: "Bash Read Grep Glob"
argument-hint: "<path-to-pattern> [--stack <stack-name>] [--core]"
---
```

**Flow:** Validate input → categorize (core/stack, auto-detect or flag) → dependency scan → dedup check → assign version → create PR to hub.

### 4.3 `/scan-url` — Trigger Internet Scan

```yaml
---
name: scan-url
description: >
  Trigger an internet scan for a specific URL or topic. Dispatches
  the GitHub Action and reports when complete.
allowed-tools: "Bash"
argument-hint: "<url-or-topic> [--add-to-watchlist]"
---
```

**Flow:** Detect input type (URL vs topic) → trigger `scan-internet.yml` via `gh workflow run` → optionally add to `config/urls.yml` watchlist → report status.

### 4.4 `/scan-repo` — Trigger Project Scan

```yaml
---
name: scan-repo
description: >
  Trigger a project scan for a specific repository. Dispatches
  the GitHub Action to extract patterns.
allowed-tools: "Bash"
argument-hint: "<owner/repo-or-url> [--add-to-tracked]"
---
```

**Flow:** Normalize input → trigger `scan-projects.yml` via `gh workflow run` → optionally add to `config/repos.yml` → report status.

### 4.5 Project Sync Config

Each project that connects to the hub has:

```yaml
# .claude/sync-config.yml
hub_repo: owner/claude-best-practices
sync_target: project              # "project" (.claude/) or "user" (~/.claude/)
selected_stacks:
  - android-compose
  - fastapi-python
  - superpowers
last_sync_version: "v1.3"
last_sync_timestamp: "2026-03-09T10:30:00Z"
auto_check_on_session_start: true # Prompt for updates at session start
```

---

## Section 5: Deduplication Logic

### Three-Level Pipeline

```
Incoming Pattern
      │
      ▼
┌─────────────────┐     Match?     ┌──────────────┐
│  Level 1: EXACT │────YES────────>│ DUPLICATE     │
│  SHA256 hash    │                │ Skip/Update   │
└────────┬────────┘                └──────────────┘
         │ NO
         ▼
┌─────────────────┐     Match?     ┌──────────────┐
│  Level 2: STRUCT│────YES────────>│ LIKELY DUP    │
│  Name + Type    │                │ Flag for      │
│  + Category     │                │ human review  │
└────────┬────────┘                └──────────────┘
         │ NO
         ▼
┌─────────────────┐     Match?     ┌──────────────┐
│  Level 3: SEMAN │────YES────────>│ POSSIBLE DUP  │
│  Claude API     │                │ Include in PR │
│  similarity     │                │ with warning  │
└────────┬────────┘                └──────────────┘
         │ NO
         ▼
┌─────────────────┐
│  NEW PATTERN    │
│  Stage for PR   │
└─────────────────┘
```

### Level 1: Exact Hash

SHA256 of normalized content (strip whitespace, lowercase). If hash matches existing `patterns.json` entry, it's an exact duplicate — skip entirely.

### Level 2: Structural Match

Scoring: same name (case-insensitive) = +3, same type = +1, same category = +1, shared dependencies = +1 each. Score >= 3 = likely duplicate, flagged in PR for human review.

### Level 3: Semantic Similarity (Claude API)

Only runs during internet scans (not local). Uses Claude Haiku for cost efficiency (~$0.01 per comparison). Compares incoming pattern description against existing patterns in same category. Similarity >= 70% = flagged. >= 85% = strongly flagged.

### Decision Matrix

| Level 1 | Level 2 | Level 3 | Action |
|:---:|:---:|:---:|--------|
| MATCH | — | — | Skip entirely |
| — | MATCH | — | Flag in PR: "Possible duplicate of X" |
| — | — | >=85% | Flag: "Semantically similar to X. Consider updating X." |
| — | — | 70-84% | Note: "Related to X. May complement or overlap." |
| — | — | <70% | Treat as new pattern |
| — | MATCH | >=70% | Strong signal. PR title prefixed with `[LIKELY DUP]` |

---

## Section 6: Error Handling & Conflict Resolution

### Error Categories

| Category | Examples | Handling |
|----------|----------|----------|
| Network | GitHub API rate limit, timeout | Retry 3x with exponential backoff (1s, 4s, 16s) |
| Auth | Expired PAT, missing secrets | Fail with actionable message |
| Content | Invalid frontmatter, malformed JSON | Validate before PR; reject with specific error |
| Dedup | Claude API failure | Fall back to Level 1+2; note in PR |
| Sync conflict | Hub and project both modified | Create PR with both versions; never auto-resolve |
| Dependency | Missing referenced dependency | Block sync for that pattern; label `needs-dependency` |
| Scan | URL 404, no extractable content | Log, skip, don't create empty PR |
| Bootstrap | Invalid stack, incompatible combo | Fail with available stacks; check `conflicts_with` |

### Conflict Type 1: Same Pattern Modified in Hub AND Project

`/update-practices` detects divergence and presents options:
1. Keep local (skip hub update)
2. Take hub (overwrite local)
3. Merge manually (show diff)
4. Contribute local → hub first, then take hub

### Conflict Type 2: Two Projects Contribute Same Pattern

Dedup catches name collision (Level 2). Second PR includes warning with link to first, and options to rename, merge features, or replace.

### Conflict Type 3: Stack File Overlap

Resolution from `stack-config.yml`:
- Both stacks declare `merges_with` → merge files with separator comments
- `file_precedence` set → use that stack's version
- Otherwise → prompt user during bootstrap

### Rollback Strategy

| Scenario | Method |
|----------|--------|
| Bad sync corrupted local `.claude/` | `git revert <sync-commit>` (each sync = 1 atomic commit) |
| Bad pattern merged to hub | Revert PR; `sync-to-projects.yml` auto-proposes revert |
| Bootstrap created wrong structure | `git reset --hard HEAD~1` or re-run with different stacks |
| Internet pattern is wrong | Close PR without merging |

### Rate Limiting & Cost Protection

| Resource | Limit | Protection |
|----------|-------|------------|
| GitHub API | 5,000 req/hour | Pause if <100 remaining |
| Claude API (semantic dedup) | $5/month budget | Max 50 comparisons per scan; skip Level 3 if exceeded |
| GitHub Actions minutes | 2,000 min/month | ~71 min/month estimated; alert if >500 |
| PR creation | Max 5 PRs per scan run | Batch patterns into single PR |
| URL fetching | Max 20 URLs per scan run | Prioritize by trust_level |

---

## Section 7: Testing Strategy

### Layer 1: Unit Tests

Test each script in `scripts/` independently via `pytest`.

| Test File | What It Tests |
|-----------|---------------|
| `test_collate.py` | Pattern extraction, version detection, empty `.claude/` handling |
| `test_scan_web.py` | URL fetching, HTML parsing, trust_level filtering, 404 handling |
| `test_dedup_check.py` | All 3 dedup levels, hash normalization, scoring thresholds, API fallback |
| `test_bootstrap.py` | Core copy, stack filtering, template rendering, dependency validation, incompatible stack rejection |
| `test_sync_to_local.py` | Version comparison, conflict detection, file copy, atomic commits, dry-run mode |
| `test_generate_docs.py` | All doc outputs valid, stats accurate, HTML well-formed |

### Layer 2: Integration Tests (CI)

Run by `validate-pr.yml` on every PR:
- Pattern integrity (valid frontmatter for all patterns)
- Dependency graph (no circular deps, all referenced deps exist)
- Registry consistency (`patterns.json` matches actual files)
- Stack config validity (required fields, valid `conflicts_with` references)
- Template rendering (CLAUDE.md.template renders for each stack combo)
- Doc generation (runs without errors)
- Secret scan (regex for API keys, tokens, passwords)

### Layer 3: End-to-End Tests (Manual/Monthly)

| E2E Test | Success Criteria |
|----------|-----------------|
| Bootstrap → Use → Contribute | PR created on hub with correct files/labels |
| Internet scan → PR → Merge → Sync | End-to-end from URL to project update PR |
| Project scan → Dedup | PR flags duplicate with correct level |
| Conflict resolution | User sees both versions with options |
| Rollback | `git revert` returns to pre-sync state |
| Dashboard accuracy | All counts and statuses match reality |

### Test Data

Located in `scripts/tests/fixtures/`:
- `sample_skill/SKILL.md` — valid skill
- `sample_agent.md` — valid agent
- `sample_hook.sh` — valid hook
- `invalid_skill/SKILL.md` — missing frontmatter (negative test)
- `duplicate_skill/SKILL.md` — exact copy (dedup test)
- `sample_registry.json` — pre-populated registry
- `sample_urls.yml` — test watchlist
- `sample_webpage.html` — captured HTML (no network needed)

---

## Existing Ecosystem (Partial Coverage)

These existing tools/repos solve parts of the problem but not the full scope:

| Solution | Covers | Missing |
|----------|--------|---------|
| `~/.claude/skills/` (built-in) | User-level skills across all projects | No sync, no discovery |
| `~/.claude/CLAUDE.md` (built-in) | Global rules across projects | Static, no auto-aggregation |
| [claude-code-showcase](https://github.com/ChrisWiles/claude-code-showcase) | Example hooks, skills, Actions | Not a sync system |
| [Daves-Claude-Code-Skills](https://medium.com/@davidroliver/skills-and-hooks-starter-kit-for-claude-code-c867af2ace32) | 37 skills, 12 hooks starter kit | One-time copy, no sync |
| [awesome-claude-code](https://github.com/hesreallyhim/awesome-claude-code) | Curated community skills list | Manual, no integration |
| [claude-code-templates](https://github.com/davila7/claude-code-templates) | CLI for configuring Claude Code | Template-only |
| [chezmoi](https://dotfiles.github.io/) / yadm | Dotfile sync across machines | System configs, not project knowledge |
| [cowork-template](https://github.com/helgejo/cowork-template) | Workspace template with memory | Single project |

None combines: template + living KB + internet scanning + multi-project auto-sync.

---

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| PR-based review for all changes | Nothing auto-merges. User maintains full control. |
| Layered architecture (core + stacks) | Universal patterns always included; stack-specific patterns opt-in |
| 3-level deduplication | Catches exact copies, structural similarities, and semantic overlap |
| GitHub Actions (not local) | Runs independently of local machine; free tier sufficient |
| Single self-contained HTML dashboard | No external dependencies; works offline |
| Version per pattern (not just repo tags) | Granular updates; projects can be behind on one pattern but current on others |
| Atomic git commits for syncs | Clean rollback via `git revert` |
| Claude Haiku for semantic dedup | Cheapest model; sufficient for comparison tasks |

---

*Design approved: 2026-03-09*
*All 7 sections reviewed and confirmed by project owner*
