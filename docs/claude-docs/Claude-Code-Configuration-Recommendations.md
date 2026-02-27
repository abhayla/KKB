# Claude Code Configuration Recommendations for RasoiAI

*Generated: 2026-02-24 | Research-only — no changes implemented*

---

## Table of Contents

1. [Quick Wins (< 30 minutes)](#1-quick-wins--30-minutes)
2. [Medium Effort (1-2 hours)](#2-medium-effort-1-2-hours)
3. [Strategic Changes](#3-strategic-changes)
4. [Appendix: Sources & References](#4-appendix-sources--references)

---

## 1. Quick Wins (< 30 minutes)

### 1.1 Add Context7 MCP Server

**What:** Install the Context7 MCP server for up-to-date library documentation.

**Why:** Your project uses fast-moving libraries (Compose BOM 2024.02.00, Hilt 2.56.1, Room 2.8.1, google-genai SDK). Claude's training data may have stale API knowledge. Context7 fetches version-specific docs on demand.

**Expected impact:** Fewer hallucinated API calls, especially for Compose and google-genai SDK changes.

**Setup:**
```bash
claude mcp add context7 -- npx -y @upstash/context7-mcp@latest
```

**Source:** [upstash/context7](https://github.com/upstash/context7) | [Context7 Claude Code Docs](https://context7.com/docs/clients/claude-code)

---

### 1.2 Add Sentry MCP Server

**What:** Connect Claude directly to your Sentry error monitoring (you already have `SENTRY_DSN` configured).

**Why:** Currently debugging production errors requires manually checking Sentry, then pasting context into Claude. This MCP lets Claude query errors, investigate stack traces, and correlate with code changes directly.

**Expected impact:** Faster production bug diagnosis. Claude can search Sentry issues, trigger AI root cause analysis (Seer), and suggest fixes without context-switching.

**Setup:**
```bash
claude mcp add sentry --transport http https://mcp.sentry.dev/sse
# Then authenticate via OAuth when prompted
```

**Source:** [getsentry/sentry-mcp](https://github.com/getsentry/sentry-mcp) | [Sentry MCP Docs](https://docs.sentry.io/product/sentry-mcp/)

---

### 1.3 Commit the Pending CLAUDE.md Corrections

**What:** Stage and commit the two pending corrections in CLAUDE.md (service count 21→20, two missing hooks in hooks table).

**Why:** These are accurate fixes sitting in the working tree. The current CLAUDE.md gives Claude wrong information about the service count.

**Expected impact:** Minor accuracy improvement.

---

### 1.4 Fix the 5-Location vs 6-7 Location Model Rule Contradiction

**What:** Root CLAUDE.md references "the 5-location model update rule" but `backend/CLAUDE.md` lists up to 7 locations. Align them.

**Why:** This is the single most-referenced backend gotcha. A contradiction here means Claude might miss update locations when adding models.

**Expected impact:** Prevents missed model imports — a category of bugs that silently breaks tests.

**Source:** Found via audit — `backend/CLAUDE.md` lines 38-48 list: `models/__init__.py`, 3× `postgres.py` blocks, `conftest.py`, plus `alembic env.py` and model file itself.

---

### 1.5 Install JetBrains IDE Plugin (if using Android Studio)

**What:** Install the official Claude Code plugin for JetBrains IDEs.

**Why:** Side-by-side diff viewing in Android Studio when Claude proposes Compose UI changes. Selection sharing lets you highlight a Composable and ask Claude about it with full context. Launch with `Ctrl+Esc`.

**Expected impact:** Better Android development workflow integration.

**Source:** [JetBrains IDE Docs](https://code.claude.com/docs/en/jetbrains)

---

## 2. Medium Effort (1-2 hours)

### 2.1 Reduce Root CLAUDE.md to ~150 Lines (HIGHEST IMPACT)

**What:** Move domain-specific content out of root CLAUDE.md into path-scoped locations. Keep only what applies universally.

**Why:** Multiple authoritative sources converge on this:
- Anthropic official: *"Keep it concise. Bloated CLAUDE.md files cause Claude to ignore your actual instructions!"* ([Best Practices](https://code.claude.com/docs/en/best-practices))
- HumanLayer: Frontier LLMs follow ~150-200 instructions reliably. Claude Code's system prompt consumes ~50. Your CLAUDE.md competes for the remaining ~100-150 slots. ([HumanLayer](https://www.humanlayer.dev/blog/writing-a-good-claude-md))
- Builder.io: *"Keep it under 300 lines. Context tokens are precious."* ([Builder.io](https://www.builder.io/blog/claude-md-guide))
- shanraisshan: *"CLAUDE.md should not exceed 150+ lines. If Claude keeps doing something you don't want despite having a rule, the file is probably too long."* ([GitHub](https://github.com/shanraisshan/claude-code-best-practice))

**Current state:** Root CLAUDE.md is ~797 lines (~3,000 tokens). With CONTINUE_PROMPT.md (~1,500 tokens) and sub-directory files (~940 tokens), total auto-loaded context is ~5,440 tokens per message. There's ~24% internal redundancy (~820 wasted tokens).

**What to keep in root CLAUDE.md (~150 lines):**

| Section | Keep? | Rationale |
|---------|-------|-----------|
| Project Overview (11 lines) | YES | Universal context |
| Architecture table + diagram (30 lines) | YES | Core structural knowledge |
| Key Architecture Decisions table (10 lines) | YES | Prevents wrong patterns |
| Development Commands (40 lines) | YES, condensed | Most-used commands only |
| 5-location model update rule (5 lines) | YES | Critical gotcha |
| Compaction instructions (5 lines) | YES | Preserves context on compact |
| Pointers to detailed docs (15 lines) | YES | "See X for details" |
| Protected Rules section | YES | Must not be moved per directive |

**What to move out:**

| Content | Move to | Reason |
|---------|---------|--------|
| Domain Models table (27 lines) | `.claude/rules/android.md` | Only relevant during Android work |
| Full Backend API section (44 lines) | `backend/CLAUDE.md` | Only relevant during backend work |
| Complete navigation routes table | `android/CLAUDE.md` | Only relevant during Android work |
| Database Schema section (13 lines) | `.claude/rules/database.md` | Only relevant during DB work |
| Design System colors/spacing (12 lines) | `.claude/rules/compose-ui.md` | Only relevant during UI work |
| India-Specific Domain Knowledge (8 lines) | Referenced doc | Rarely needed at file level |
| VPS Deployment section (25 lines) | `.claude/skills/deploy/SKILL.md` | Only relevant during deployment |
| Troubleshooting table (22 lines) | Referenced doc | Consulted on-demand |
| Meal Generation details (24 lines) | `backend/CLAUDE.md` | Only relevant during AI work |
| Workflow Enforcement Hooks table (21 lines) | Referenced doc | Implementation detail |
| Claude Code Configuration table (32 lines) | Referenced doc | Meta-information |
| Key Documentation index (21 lines) | Compress to 5 lines | Pointers, not content |

**Expected impact:** ~60% reduction in always-loaded tokens. Instructions in the remaining 150 lines will be followed more reliably. Path-scoped content still loads when Claude works in those directories.

**Source:** [How I Organized My CLAUDE.md in a Monorepo](https://dev.to/anvodev/how-i-organized-my-claudemd-in-a-monorepo-with-too-many-contexts-37k7) — one developer reduced from 47K words to 9K words (80% reduction) by splitting into subdirectory files.

---

### 2.2 Deduplicate Content Between Root and Sub-Directory CLAUDE.md Files

**What:** Remove content that's duplicated between root CLAUDE.md and the 4 sub-directory files.

**Why:** Audit found these exact/near duplications:

| Duplicated Content | Root | Sub-file | Similarity |
|--------------------|------|----------|-----------|
| Backend dev commands | Lines 139-167 | `backend/CLAUDE.md:5-17` | 85% |
| Android build commands | Lines 114-137 | `android/CLAUDE.md:7-21` | 80% |
| Router gotchas | Lines 333-335 | `backend/CLAUDE.md:58-61` | 95% |
| SQLAlchemy async rules | Lines 351-353 | `backend/CLAUDE.md:86-90` | 90% |
| Service patterns | Lines 337-341 | `backend/CLAUDE.md:80-84` | 95% |
| Rate limiting | Line 347 | `backend/CLAUDE.md:103-105` | 85% |
| Security headers | Line 349 | `backend/CLAUDE.md:109-110` | 90% |
| AI SDK note | Line 343 | `backend/CLAUDE.md:76` | 98% |
| API 34 emulator | Lines 127, 430 | `android/CLAUDE.md:58` | 85% |
| E2E backend URL | Line 274 | `androidTest/CLAUDE.md:33` | 100% |

**Principle:** Root CLAUDE.md should have brief pointers ("See `backend/CLAUDE.md` for async rules and service patterns"). Sub-directory files should have the authoritative detail.

**Expected impact:** ~820 tokens saved. Single source of truth per concept — no more contradictions.

---

### 2.3 Slim Down CONTINUE_PROMPT.md

**What:** CONTINUE_PROMPT.md contains ~40% content that's unique (session history, implementation status) and ~60% that duplicates CLAUDE.md (commands, architecture, test counts). Remove the duplicated parts.

**Why:** Both files are auto-loaded into context on every message. The duplicated portions (backend commands, testing commands, architecture diagram) waste ~600 tokens.

**Keep in CONTINUE_PROMPT.md:** Current state summary, implementation status table, session history, environment setup specifics (things that change between sessions).

**Remove from CONTINUE_PROMPT.md:** Backend test command reference, development commands, architecture description, requirement ID format tables (all already in CLAUDE.md or sub-files).

**Expected impact:** ~600 tokens saved per message. Clearer separation: CLAUDE.md = "how the project works", CONTINUE_PROMPT.md = "what we've done and what's next".

---

### 2.4 Add ADB MCP Server

**What:** Install an ADB MCP server for structured Android device interaction.

**Why:** Your `/adb-test` skill runs raw ADB shell commands through Bash (uiautomator dump, screencap, input tap). An MCP server provides structured tools instead of fragile shell parsing. Would also help with the documented screenshot capture issues (ADB warning text, `-d 0` flag problems).

**Setup:**
```bash
# Option 1: watabee's implementation
claude mcp add adb-server npx -- -y mcp-server-adb

# Option 2: AlexGladkov's mobile-focused implementation
claude mcp add adb npx -- -y @anthropic/mcp-server-adb
```

**Source:** [watabee/mcp-server-adb](https://github.com/watabee/mcp-server-adb) | [AlexGladkov/claude-in-mobile](https://github.com/AlexGladkov/claude-in-mobile)

**Expected impact:** More reliable E2E testing via ADB. Structured screenshot capture instead of manual PNG byte parsing.

---

### 2.5 Enrich Path-Scoped Rules with Content from Root

**What:** Your existing 5 rule files (`.claude/rules/`) are good but lean (24-44 lines each). Move relevant detail from root CLAUDE.md into them.

**Current vs. proposed:**

| Rule File | Current Lines | Proposed Additions |
|-----------|:------------:|---------------------|
| `android.md` | 35 | + Navigation routes table, key enums, Room entity list |
| `backend.md` | 44 | + Full API endpoint list, service gotchas, startup sequence |
| `compose-ui.md` | 39 | + Design system tokens, reference implementations table |
| `database.md` | 24 | + Full schema description, migration history, Room v11 details |
| `testing.md` | 38 | + Test distribution table, E2E infrastructure details |

**Why:** Path-scoped rules only load when Claude works on matching files. Content here is "free" — it doesn't consume tokens during unrelated work. This is the correct home for domain-specific detail.

**Source:** [Claude Code Rules Directory - ClaudeFast](https://claudefa.st/blog/guide/mechanics/rules-directory): *"Use CLAUDE.md for what applies everywhere. Use rules for what applies to specific areas."*

**Expected impact:** Same information available, but loaded conditionally rather than always.

---

### 2.6 Audit Hook Performance — Consider Async verify-test-rerun

**What:** Your PostToolUse Bash hook chain has a worst-case of ~360 seconds (6 minutes), primarily due to `verify-test-rerun.sh` (300s timeout) running serially.

**Hook chain timing (worst case):**
```
PostToolUse Bash (serial):
  post-test-update.sh          30s
  verify-test-rerun.sh        300s  ← bottleneck
  post-screenshot-resize.sh    30s
  post-screenshot-validate.sh  10s
  auto-fix-pattern-scan.sh     10s
  post-anr-detection.sh        10s
  log-workflow.sh               5s
                        Total: ~395s
```

**Options:**
- **Option A:** Make `verify-test-rerun.sh` run in background (non-blocking) and report results asynchronously. If rerun fails, set `testFailuresPending=true` via a follow-up check.
- **Option B:** Only trigger rerun on single-test runs (skip for suite runs where flakiness is less impactful). Currently it skips full E2E suites — verify it also skips medium-sized test batches.
- **Option C:** Accept the current timing. If the rerun catches even one false-positive "PASS" per week, the 5-minute cost is worth it.

**Source:** [Anthropic Hooks Guide](https://code.claude.com/docs/en/hooks-guide): *"Hooks are deterministic and guarantee the action happens"* — but they should be fast enough not to impede developer flow.

**Expected impact:** If Option A: typical PostToolUse drops from ~360s to ~60s for test commands.

---

## 3. Strategic Changes

### 3.1 Restructure for Progressive Disclosure (Token Optimization)

**What:** Adopt the "progressive disclosure" pattern: root CLAUDE.md tells Claude *how to find* information rather than *containing* all information.

**Why:** According to one optimization study, progressive disclosure across skills recovered ~15,000 tokens per session — an 82% improvement over loading everything into CLAUDE.md. ([Medium](https://medium.com/@jpranav97/stop-wasting-tokens-how-to-optimize-claude-code-context-by-60-bfad6fd477e5))

**Proposed structure:**
```
CLAUDE.md                          # ~150 lines: overview, key commands, pointers
├── android/CLAUDE.md              # Auto-loads for android/**
├── backend/CLAUDE.md              # Auto-loads for backend/**
├── backend/tests/CLAUDE.md        # Auto-loads for backend/tests/**
├── android/.../androidTest/CLAUDE.md  # Auto-loads for E2E tests
├── .claude/rules/
│   ├── android.md                 # Enriched: routes, enums, entities
│   ├── backend.md                 # Enriched: API list, services, startup
│   ├── compose-ui.md              # Enriched: design system, patterns
│   ├── database.md                # Enriched: full schema, migrations
│   └── testing.md                 # Enriched: distributions, fixtures
├── .claude/skills/
│   ├── implement/                 # 7-step workflow (loaded on /implement)
│   ├── fix-issue/                 # Issue workflow (loaded on /fix-issue)
│   └── deploy/                    # VPS deployment (loaded on /deploy)
└── docs/
    ├── CONTINUE_PROMPT.md         # Session state only (no duplicated reference)
    └── design/                    # @-referenced on demand
```

**Key principle from HumanLayer:** *"Don't include all context in CLAUDE.md. Tell Claude how to find important information so it can load it only when needed."* ([HumanLayer](https://www.humanlayer.dev/blog/writing-a-good-claude-md))

**Expected impact:** ~60% reduction in always-loaded context. Better instruction adherence. Same information depth available on demand.

---

### 3.2 Consider Community Android & FastAPI Skills

**What:** Evaluate these community skill collections for patterns you could adopt:

| Skill Collection | Source | Relevant Content |
|------------------|--------|------------------|
| Android Kotlin Development | [dpconde/claude-android-skill](https://github.com/dpconde/claude-android-skill) | Compose patterns, Hilt DI, offline-first, UDF enforcement |
| Android Agent Skills (15+) | [new-silvermoon/awesome-android-agent-skills](https://github.com/new-silvermoon/awesome-android-agent-skills) | Compose state hoisting, modifier ordering, Room sync, coroutines |
| FastAPI Expert Skill | [jeffallan.github.io](https://jeffallan.github.io/claude-skills/skills/backend/fastapi-expert/) | REST API patterns, async SQLAlchemy, JWT/OAuth2, DI patterns |
| Trail of Bits Security | [awesome-claude-code](https://github.com/hesreallyhim/awesome-claude-code) | 12+ security skills for vulnerability scanning, dependency auditing |

**Why:** Your project already follows many of these patterns (BaseViewModel, UiState, Hilt, Room offline-first). The value is in codifying them as auto-enforced rules rather than hoping Claude remembers from CLAUDE.md.

**Expected impact:** More consistent code generation. Less drift from established patterns over time.

---

### 3.3 Add Firebase MCP Server

**What:** Install Firebase MCP for direct auth user management.

**Why:** Your project uses Firebase Auth with `FakeGoogleAuthClient` for testing. Currently, managing test users (cleanup, inspection, token verification) requires manual Firebase Console access or raw API calls. An MCP server lets Claude manage Firebase Auth users directly — useful for E2E test setup/teardown and debugging auth issues.

**Setup:**
```bash
claude mcp add firebase npx -- -y firebase-tools@latest mcp
```

**Source:** [Firebase MCP Docs](https://firebase.google.com/docs/ai-assistance/mcp-server) | [gannonh/firebase-mcp](https://github.com/gannonh/firebase-mcp)

**Expected impact:** Smoother auth debugging. Automated test user lifecycle management.

---

### 3.4 Consider Specialized Sub-Agents for Android and Meal Generation

**What:** Add 2 focused sub-agents beyond your current 9:

1. **android-compose agent** — Scoped to `android/` with Compose-specific patterns, modifier ordering rules, state management validation, and accessibility checking.

2. **meal-generation agent** — Scoped to `backend/app/ai/` + `backend/config/` with Gemini API knowledge, prompt engineering context, and pairing config understanding.

**Why:** Your current agents are generic (debugger, tester, code-reviewer). Domain-specific agents can carry specialized context without loading it into the main conversation. Community pattern: *"Domain-based routing: spawn parallel agents for frontend, backend, and database work where each agent owns their domain."* ([ClaudeFast](https://claudefa.st/blog/guide/agents/sub-agent-best-practices))

**Cost optimization:** Run main session on Opus, sub-agents on Sonnet. ([PubNub](https://www.pubnub.com/blog/best-practices-for-claude-code-sub-agents/))

**Source:** [VoltAgent/awesome-claude-code-subagents](https://github.com/VoltAgent/awesome-claude-code-subagents) | [pate0304/claude-code-specialized-agents](https://github.com/pate0304/claude-code-specialized-agents)

---

### 3.5 Evaluate Hook Consolidation or Python Migration

**What:** Your 13 hooks total 1,717 lines of bash + Python. Consider:
- Consolidating overlapping hooks (e.g., merge `post-screenshot-resize.sh` and `post-screenshot-validate.sh` into one)
- Migrating complex hooks to Python using the `cchooks` SDK for cleaner JSON handling

**Why:**
- All hooks source `hook-utils.sh` (537 lines) which uses Python subprocesses for JSON parsing. Native Python hooks would eliminate this overhead.
- Community observation: *"Vanilla Claude Code is better than complex workflows with smaller tasks."* ([shanraisshan](https://github.com/shanraisshan/claude-code-best-practice)) — ensure hooks are earning their overhead.
- The 7-hook PostToolUse Bash chain runs serially; fewer hooks = faster response.

**Source:** [cchooks Python SDK](https://github.com/hesreallyhim/awesome-claude-code) | [Claude Hooks Best Practices - PRPM](https://prpm.dev/blog/claude-hooks-best-practices)

**Expected impact:** Faster hook execution. Easier maintenance. Same enforcement guarantees.

---

### 3.6 Add Redis MCP Server (if using Redis caching)

**What:** Your VPS has Redis on port 6379. A Redis MCP would let Claude inspect cache state, debug rate limiting (`slowapi`), and examine the recipe cache (`warm_recipe_cache()`).

**Setup:**
```bash
claude mcp add redis npx -- -y @redis/mcp-redis --url redis://localhost:6379
```

**Source:** [redis/mcp-redis](https://github.com/redis/mcp-redis) | [Redis MCP Docs](https://redis.io/docs/latest/integrate/redis-mcp/)

**Expected impact:** Better debugging of caching and rate limiting issues.

---

## 4. Appendix: Sources & References

### CLAUDE.md Best Practices

| Source | Key Takeaway | URL |
|--------|-------------|-----|
| Anthropic Official | "Keep it concise. Bloated files cause Claude to ignore instructions." | [code.claude.com/docs/en/best-practices](https://code.claude.com/docs/en/best-practices) |
| HumanLayer | "Tell Claude how to find information, don't embed it all." ~150-200 instruction limit. | [humanlayer.dev/blog/writing-a-good-claude-md](https://www.humanlayer.dev/blog/writing-a-good-claude-md) |
| Builder.io | "Keep under 300 lines. Context tokens are precious." | [builder.io/blog/claude-md-guide](https://www.builder.io/blog/claude-md-guide) |
| shanraisshan | "CLAUDE.md should not exceed 150+ lines." Limit MCP servers. | [github.com/shanraisshan/claude-code-best-practice](https://github.com/shanraisshan/claude-code-best-practice) |
| DEV Community | Monorepo: reduced 47K words to 9K via subdirectory splitting. | [dev.to/anvodev/how-i-organized-my-claudemd-in-a-monorepo](https://dev.to/anvodev/how-i-organized-my-claudemd-in-a-monorepo-with-too-many-contexts-37k7) |
| Medium (Token Optimization) | Progressive disclosure recovered ~15K tokens/session (82% improvement). | [medium.com/@jpranav97/stop-wasting-tokens](https://medium.com/@jpranav97/stop-wasting-tokens-how-to-optimize-claude-code-context-by-60-bfad6fd477e5) |
| Anthropic PDF | Internal teams use CLAUDE.md for codebase navigation, not exhaustive docs. | [www-cdn.anthropic.com/58284b19...pdf](https://www-cdn.anthropic.com/58284b19e702b49db9302d5b6f135ad8871e7658.pdf) |

### Stack-Specific Resources

| Stack | Resource | URL |
|-------|----------|-----|
| FastAPI + async SQLAlchemy | MissingGreenlet pitfall | [github.com/sqlalchemy/sqlalchemy/discussions/11258](https://github.com/sqlalchemy/sqlalchemy/discussions/11258) |
| FastAPI + async SQLAlchemy | Session setup guide | [medium.com/@tclaitken/setting-up-a-fastapi-app](https://medium.com/@tclaitken/setting-up-a-fastapi-app-with-async-sqlalchemy-2-0-pydantic-v2-e6c540be4308) |
| Android Compose skill | Claude Code Android patterns | [github.com/dpconde/claude-android-skill](https://github.com/dpconde/claude-android-skill) |
| Android agent skills (15+) | Compose, Hilt, Room, coroutines | [github.com/new-silvermoon/awesome-android-agent-skills](https://github.com/new-silvermoon/awesome-android-agent-skills) |
| Alembic migrations | Safe migration practices | [medium.com/zackary-yen/alembic-a-practical-guide](https://medium.com/zackary-yen/alembic-a-practical-guide-to-safe-database-migrations-with-real-world-insights-474b71a59b3f) |
| Gemini + Claude integration | AI-calling-AI patterns | [gist.github.com/AndrewAltimit/fc5ba068](https://gist.github.com/AndrewAltimit/fc5ba068b73e7002cbe4e9721cebb0f5) |

### MCP Servers

| Server | Source | Relevance |
|--------|--------|-----------|
| Sentry MCP | [getsentry/sentry-mcp](https://github.com/getsentry/sentry-mcp) | Production error monitoring (you have Sentry configured) |
| Context7 | [upstash/context7](https://github.com/upstash/context7) | Up-to-date library docs |
| ADB MCP | [watabee/mcp-server-adb](https://github.com/watabee/mcp-server-adb) | Structured Android device interaction |
| Firebase MCP | [firebase.google.com/docs/ai-assistance/mcp-server](https://firebase.google.com/docs/ai-assistance/mcp-server) | Auth user management |
| Redis MCP | [redis/mcp-redis](https://github.com/redis/mcp-redis) | Cache and rate limit inspection |
| Postgres MCP Pro | [crystaldba/postgres-mcp](https://github.com/crystaldba/postgres-mcp) | Enhanced schema introspection |
| Playwright MCP | [microsoft/playwright-mcp](https://github.com/microsoft/playwright-mcp) | Browser automation for Swagger UI testing |

### Hooks & Agents

| Resource | URL |
|----------|-----|
| Claude Code Hooks Mastery | [github.com/disler/claude-code-hooks-mastery](https://github.com/disler/claude-code-hooks-mastery) |
| Multi-Agent Observability | [github.com/disler/claude-code-hooks-multi-agent-observability](https://github.com/disler/claude-code-hooks-multi-agent-observability) |
| Hook best practices | [prpm.dev/blog/claude-hooks-best-practices](https://prpm.dev/blog/claude-hooks-best-practices) |
| Sub-agent best practices | [claudefa.st/blog/guide/agents/sub-agent-best-practices](https://claudefa.st/blog/guide/agents/sub-agent-best-practices) |
| 100+ sub-agents collection | [github.com/VoltAgent/awesome-claude-code-subagents](https://github.com/VoltAgent/awesome-claude-code-subagents) |
| Awesome Claude Code (master list) | [github.com/hesreallyhim/awesome-claude-code](https://github.com/hesreallyhim/awesome-claude-code) |

### Audit Findings Summary

| Finding | Severity | Section |
|---------|----------|---------|
| Root CLAUDE.md ~797 lines (~3,000 tokens) — 5x over recommended limit | HIGH | §2.1 |
| 24% internal redundancy (~820 wasted tokens) across root + sub-files | HIGH | §2.2 |
| 5-location vs 6-7 location model rule contradiction | HIGH | §1.4 |
| CONTINUE_PROMPT.md ~60% duplicated from CLAUDE.md (~600 wasted tokens) | MEDIUM | §2.3 |
| PostToolUse hook chain worst-case 360s (verify-test-rerun bottleneck) | MEDIUM | §2.6 |
| E2E 2-second Splash timing detail missing from root CLAUDE.md | LOW | §2.5 |
| Missing forward references to backend/CLAUDE.md and android/CLAUDE.md | LOW | §2.2 |
| 1,717 lines of hook bash code — maintenance complexity | LOW | §3.5 |

---

*This document is research-only. No changes have been implemented. Prioritize items in Section 1 (Quick Wins) first, then Section 2 (Medium Effort) based on which pain points you experience most.*
