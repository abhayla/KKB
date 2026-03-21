# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Session context:** Check @docs/CONTINUE_PROMPT.md for active work between sessions.

**Imported references** (loaded on demand via `@` syntax):
- Technical design: @docs/design/RasoiAI Technical Design.md
- Data flow: @docs/design/Data-Flow-Diagram.md
- Meal generation: @docs/design/Meal-Generation-Algorithm.md
- Workflow rules: @.claude/rules/workflow.md
- E2E testing guide: @docs/testing/E2E-Testing-Prompt.md
- Functional requirements: @docs/testing/Functional-Requirement-Rule.md

**Context compaction:** When compacting, always preserve: the 5-location model import rule, test fixture choices (client vs unauthenticated_client vs authenticated_client), all file paths that were modified, any test commands that were run, the current workflow step number, and any GitHub Issue numbers being worked on.

## Project Overview

**RasoiAI** (रसोई AI) is an AI-powered meal planning application for Indian families. Generates personalized weekly meal plans based on family preferences, dietary restrictions, regional cuisines, and festivals/fasting days.

| Attribute | Details |
|-----------|---------|
| **Android** | Kotlin 2.2.10, Jetpack Compose BOM 2024.02.00, Hilt 2.56.1, Room 2.8.1, KSP 2.3.2, AGP 9.0.1, Target SDK 34 (Min 24) |
| **Backend** | Python 3.11+, FastAPI, PostgreSQL 12+, SQLAlchemy async, ~62 endpoints across 13+ routers |
| **AI** | Gemini `gemini-2.5-flash` via `google-genai` SDK (meal gen + photo analysis), Claude API (chat) |

## Architecture

Four Android modules: `app` (presentation) → `domain` (pure Kotlin interfaces/models) ← `data` (Room + Retrofit impls) → `core` (shared utilities). Domain has zero Android dependencies. Offline-first: Room is source of truth, API syncs in background. Mappers centralized in `EntityMappers.kt` and `DtoMappers.kt`. Backend services are standalone async functions (not classes).

**5-location model import rule (CRITICAL):** New SQLAlchemy models require updates in: (1) `models/your_model.py`, (2) `models/__init__.py`, (3) `db/postgres.py` (3 blocks: init_db, create_tables, drop_tables), (4) `tests/conftest.py`, (5) Alembic migration. Missing any causes silent test/migration failures.

**Detailed conventions** auto-load via path-scoped rules in `.claude/rules/` — see `testing.md`, `android.md`, `backend.md`, `database.md`, `compose-ui.md`, and 40+ others.

## Development Commands

### Android (run from `android/`)

```bash
./gradlew build                  # Full build with tests
./gradlew assembleDebug          # Quick compilation (no tests)
./gradlew test                   # All unit tests
./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest"  # Single test
./gradlew :app:connectedDebugAndroidTest  # All instrumented tests (emulator API 34)
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.FullUserJourneyTest  # Single E2E
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J01_FirstTimeUserSuite  # Journey suite
```

### Backend (run from `backend/`)

```bash
source venv/bin/activate && pip install -r requirements.txt
uvicorn app.main:app --reload    # Dev server at http://localhost:8000/docs (DEBUG=true only)
PYTHONPATH=. pytest              # All tests
PYTHONPATH=. pytest tests/test_auth.py -v                    # Single file
PYTHONPATH=. pytest tests/test_preference_service.py::test_add_include_rule -v  # Single test
alembic upgrade head             # Run migrations
alembic revision --autogenerate -m "description"  # New migration
# Seed data (first-time): seed_festivals.py, seed_achievements.py, import_recipes_postgres.py, sync_config_postgres.py
```

### Formatting (auto-runs via hook)

```bash
black app/ tests/ && ruff check app/ tests/ --fix   # Python
# Kotlin: no manual formatter; Compose stability rules in android/app/compose-stability.conf
```

### Environment

**Backend `.env`:** `DATABASE_URL`, `FIREBASE_CREDENTIALS_PATH`, `ANTHROPIC_API_KEY`, `GOOGLE_AI_API_KEY`, `JWT_SECRET_KEY` (REQUIRED, no default), `DEBUG=true`, `SENTRY_DSN` (optional). Android needs `google-services.json` in `android/app/` and `local.properties` with `sdk.dir`.

**CI/CD:** 4 GitHub Actions — `android-ci.yml` (lint/test/build), `backend-ci.yml` (pytest), `claude.yml` (@claude mentions), `claude-code-review.yml` (auto PR review).

## Test Fixtures (Backend)

All in `backend/tests/conftest.py` — do NOT duplicate in subdirectories.

| Fixture | Auth | Use when |
|---------|------|----------|
| `client` | Pre-authenticated | Most tests |
| `unauthenticated_client` | No auth | Testing 401s |
| `authenticated_client` | Real JWT | Testing JWT verification |
| `db_session` | N/A | Direct service/repository tests |

Multi-user: `make_api_client()` from `tests/api/conftest.py`. Services using `async_session_maker` directly need it patched — see `backend/tests/CLAUDE.md`.

## E2E Tests

Real backend + fake phone auth. `FakePhoneAuthClient` → `"fake-firebase-token"` → backend (DEBUG=true) accepts → real JWT → real PostgreSQL. URL: `http://10.0.2.2:8000`. API 34 emulator (not 36). 17 journey suites (J01-J17), robot pattern in `e2e/robots/`, TestTags in `presentation/common/TestTags.kt`.

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Gradle sync fails | JDK 17+, `JAVA_HOME` set, check `libs.versions.toml` |
| API 36 emulator | Use API 34. CI uses API 29 |
| Gradle daemon hangs (Win) | `./gradlew --stop` |
| KSP/Hilt errors | `./gradlew clean :app:kspDebugKotlin` |
| Backend import errors | `cd backend && PYTHONPATH=. pytest` |
| MissingGreenlet | Use `selectinload()` for eager loading |
| Meal gen timeout | 180s endpoint timeout, ~35s typical. Logs: `logs/MEAL_PLAN-*.json` |
| Stale .pyc | `find backend -name "*.pyc" -delete && find backend -name "__pycache__" -type d -exec rm -rf {} +` |
| Recipe ID 500s | Compare recipe IDs as strings — `uuid.UUID` vs `String(36)` mismatch |
| Room DB not found | `./gradlew clean` then rebuild |
| Hilt + KSP2 | Need Hilt 2.51+ (currently 2.56.1) |
| Room + KSP2 | Need Room 2.8.1+ |
| JUnit Platform (Gradle 9) | All modules need `testRuntimeOnly(libs.junit.platform.launcher)` |
| Test Orchestrator | Intentionally disabled — breaks E2E state sharing |

## Patterns We DON'T Use

- No LiveData — StateFlow via BaseViewModel
- No SharedFlow for nav — Channel + receiveAsFlow()
- No per-feature mappers — centralized EntityMappers.kt / DtoMappers.kt
- No @Provides for simple bindings — @Binds on abstract classes
- No classes for backend services — standalone async functions
- No `google-generativeai` SDK — use `google-genai` with native async (`client.aio`)
- No lazy loading in SQLAlchemy — selectinload() always

## VPS Deployment

See `docs/VPS-Deployment.md`. Do NOT modify files in `C:\Apps\shared\`.

## Generic Rules for Claude

<!-- ========================================================== -->
<!-- PROTECTED SECTION - DO NOT MODIFY                          -->
<!-- These rules are carefully crafted and tested.              -->
<!-- Do NOT condense, rewrite, reorganize, or "improve" them.   -->
<!-- Do NOT remove content even if it appears redundant.        -->
<!-- Any /init or optimization request must SKIP this section.  -->
<!-- Authority: Project owner directive (2026-03-08)            -->
<!-- ========================================================== -->

### Session History Analysis & Automation Extraction

When the user asks to analyze their workflow patterns, session history, or identify automation opportunities, follow this structured approach:

**Step 1: Scrape Session Data**
- Scan all Claude Code session transcripts (`.jsonl` files) under `~/.claude/projects/` and `~/.claude/sessions/`
- Parse each session for: tool calls made, skills invoked, commands run, files edited, errors encountered, and user corrections
- Group sessions by project directory and time period

**Step 2: Pattern Classification**
Categorize every recurring pattern into exactly one of these buckets:

| Category | When to Use | Examples |
|----------|-------------|---------|
| **Skill** (`.claude/skills/`) | Repeatable multi-step workflows triggered by user command. Has a clear start/end, requires judgment, runs in the main conversation context. | `/fix-issue`, `/deploy`, `/run-e2e`, test-debug-fix cycles, release workflows |
| **Agent** (`.claude/agents/`) | Autonomous sub-tasks that can run in isolation with their own tool access. Delegatable work that doesn't need continuous user interaction. | Code review, test runner, build validator, lint fixer, dependency auditor |
| **Hook** (`.claude/hooks/`) | Automatic triggers on tool events (PreToolUse/PostToolUse). Enforcement gates, auto-formatting, validation checks. No user invocation needed. | Auto-format on save, block commits without tests, screenshot resize, workflow state tracking |
| **MCP Server/Plugin** | External tool integrations that expose new capabilities via the Model Context Protocol. Bridges to third-party services or local tools. | Database queries, browser automation, Slack notifications, CI/CD status checks |
| **CLAUDE.md Rule** | Static instructions, conventions, or constraints that should always be in context. No logic — just knowledge and directives. | File naming conventions, architecture decisions, env setup, troubleshooting tables, test fixture guides |

**Step 3: Output Format**
Present findings as a prioritized table:

```
| # | Pattern Observed | Frequency | Category | Priority | Suggested Name | Description |
|---|-----------------|-----------|----------|----------|----------------|-------------|
| 1 | User runs tests then fixes failures in a loop | Every session | Skill | HIGH | /fix-loop | Iterative test-fix cycle with escalation |
| 2 | Auto-format Python after edits | Every edit | Hook | HIGH | auto-format.sh | Black + ruff on PostToolUse |
| ... | | | | | | |
```

**Step 4: Conflict Check**
- Before recommending a new skill/agent/hook, check if one already exists (search `.claude/skills/`, `.claude/agents/`, `.claude/hooks/`)
- If an existing automation partially covers the pattern, recommend enhancing it rather than creating a new one
- Flag any patterns that span multiple categories (e.g., "could be a skill OR an agent") and explain the trade-off

**Step 5: Implementation Readiness**
For each HIGH priority recommendation, provide:
- The exact file path where it should be created
- A 2-3 sentence spec of what it should do
- Dependencies on existing infrastructure

## Project Rules for Claude

<!-- ========================================================== -->
<!-- PROTECTED SECTION - DO NOT MODIFY                          -->
<!-- These rules are carefully crafted and tested.              -->
<!-- Do NOT condense, rewrite, reorganize, or "improve" them.   -->
<!-- Do NOT remove content even if it appears redundant.        -->
<!-- Any /init or optimization request must SKIP this section.  -->
<!-- Authority: Project owner directive (2026-02-04)            -->
<!-- ========================================================== -->

1. **Bash Syntax (CRITICAL)**: Use forward slashes `/`, use `./gradlew` (not `.\gradlew`), quote paths with spaces. Shell is Unix-style bash even on Windows.

2. **Document Output**:
   - Generated documents → `docs/claude-docs/`
   - Test screenshots → `docs/testing/screenshots/` (gitignored)

3. **Screenshots (CRITICAL)**:
   - **ALL screenshots MUST be saved to `docs/testing/screenshots/`** - no exceptions
   - This includes: Playwright screenshots, emulator screenshots, UI test captures, debugging screenshots
   - NEVER save screenshots to the project root, `.playwright-mcp/`, or any other location
   - The folder is gitignored - screenshots are temporary debugging artifacts
   - Use descriptive filenames: `{feature}_{context}.png` (e.g., `home_after_login.png`, `onboarding_step2.png`)

   **Best practices:** Use PNG format, limit to 1280x720, avoid `fullPage: true` on long pages. Wait for page stability before capture. Hook auto-resizes >1800px and auto-strips ADB warnings.

   ```bash
   # ADB screenshot
   adb exec-out screencap -p > docs/testing/screenshots/screen_name.png
   ```

4. **Offline-First**: All features must use Room as source of truth with offline support.

5. **Bug & Feature Tracking**:
   - **Before starting work**: Check GitHub Issues for related bugs/features with `gh issue list`
   - **Finding TODOs**: When you find `/* TODO: ... */` comments, consider creating a GitHub Issue
   - **After fixing**: Reference issue number in commit: `Fix #123: description`
   - **Use `/fix-issue <number>`**: To implement a fix for a specific GitHub Issue
   - **Labels**: `bug`, `enhancement`, `not-implemented`, `home-screen`, etc.
   - **Issue Templates**: `.github/ISSUE_TEMPLATE/` has templates for bug reports and feature requests

6. **Functional Requirements Testing**:

   > ⚠️ **STOP**: Complete the Pre-Implementation Checklist in Rule #7 BEFORE writing any code.

   When implementing a new feature or fixing a bug that affects user-facing functionality:

   **Before Implementation:**
   - Check for existing GitHub Issue with acceptance criteria
   - If none exists, create one using the "Functional Requirement" template
   - Reference the Issue ID (e.g., FR-001 = Issue #45)

   **After Implementation:**
   - Create Android E2E test in `app/src/androidTest/java/com/rasoiai/app/e2e/flows/`
   - Create Backend test in `backend/tests/` if API is involved
   - Update `docs/testing/Functional-Requirement-Rule.md` with test links

   **Test Documentation:**
   - Each test file MUST have KDoc/docstring header referencing the GitHub Issue
   - Use format: `/** Requirement: #45 - FR-001: Add Chai to breakfast */`

   **Verification:**
   ```bash
   # Android E2E test
   ./gradlew :app:connectedDebugAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.TestClassName

   # Backend test
   PYTHONPATH=. pytest tests/test_file.py -v
   ```

   **Reference:** See `docs/testing/Functional-Requirement-Rule.md` for the full traceability matrix.

7. **Mandatory Development Workflow (ALL Code Tasks)**:

   7-step workflow: Requirements → Tests → Implement → Run tests → Fix loop → Screenshots → Verify & commit. Enforced by hooks (see below). No step skipping, no partial passes, no @Ignore bypasses, no commits without passing tests.

   > **Full reference:** `.claude/rules/workflow.md`
   >
   > **Applies to:** Feature implementation, bug fixes, refactoring, any code changes (`.kt`, `.py`, `.xml`).
   > **Does NOT apply to:** Questions, documentation-only, research/exploration.

## Workflow Enforcement Hooks

8 shell hooks in `.claude/hooks/` enforce the 7-step workflow (blocking edits, commits, and test claims when gates aren't met). State tracked in `.claude/workflow-state.json`. See `.claude/rules/workflow.md` for full hook documentation.

## Key Documentation & Configuration

| Resource | Location |
|----------|----------|
| Session context | `docs/CONTINUE_PROMPT.md` |
| Workflow rules | `.claude/rules/workflow.md` |
| E2E testing guide | `docs/testing/E2E-Testing-Prompt.md` |
| Functional requirements | `docs/testing/Functional-Requirement-Rule.md` |
| Technical design | `docs/design/RasoiAI Technical Design.md` |
| Sub-directory CLAUDE.md | `android/CLAUDE.md`, `backend/CLAUDE.md`, `backend/tests/CLAUDE.md`, `android/app/src/androidTest/CLAUDE.md` |
| Path-scoped rules (44) | `.claude/rules/` — auto-loaded when working on matching files |
| Skills (25+) | `.claude/skills/` — `/fix-issue`, `/fix-loop`, `/implement`, `/run-backend-tests`, etc. |
| MCP Servers | `.mcp.json` — Playwright (web screenshots), ADB (emulator automation) |

<!-- hub:best-practices:start -->

<!-- PROTECTED SECTION — managed by claude-best-practices hub. -->
<!-- Do NOT condense, rewrite, reorganize, or remove.          -->
<!-- Any /init or optimization request must SKIP this section.  -->

## Rules for Claude

1. **Bug Fixing**: Use `/fix-loop` or `/fix-issue`. Start by writing a test that reproduces the bug, then fix and prove with a passing test.

### Rules Reference

| Rule File | What It Covers |
|-----------|---------------|
| `rules/agent-orchestration.md` | Constraints for multi-agent orchestration patterns in agents and skills. |
| `rules/ai-gemini.md` | Ai Gemini |
| `rules/android.md` | Android development rules for Kotlin + Jetpack Compose projects. |
| `rules/android-dao-query-conventions.md` | Android Dao Query Conventions |
| `rules/android-kotlin.md` | Kotlin language idioms, null safety, scope functions, and KMP-specific patterns for Android projects. |
| `rules/android-naming-conventions.md` | Android Naming Conventions |
| `rules/android-navigation-callbacks.md` | Android Navigation Callbacks |
| `rules/backend.md` | Backend |
| `rules/backend-auth-dependency.md` | Backend Auth Dependency |
| `rules/backend-endpoint-structure.md` | Backend Endpoint Structure |
| `rules/backend-logging-standards.md` | Backend Logging Standards |
| `rules/backend-service-layer.md` | Backend Service Layer |
| `rules/backend-service-pattern.md` | Backend Service Pattern |
| `rules/baseviewmodel-pattern.md` | Baseviewmodel Pattern |
| `rules/centralized-mapper-convention.md` | Centralized Mapper Convention |
| `rules/claude-behavior.md` | Universal behavioral rules for how Claude should approach all tasks. |
| `rules/compose-testtags-convention.md` | Compose Testtags Convention |
| `rules/compose-ui.md` | Compose Ui |
| `rules/configuration-ssot.md` | Scope: global |
| `rules/context-management.md` | Rules for managing context window, token usage, and documentation references. |
| `rules/custom-exception-hierarchy.md` | Custom Exception Hierarchy |
| `rules/database.md` | Database |
| `rules/e2e-fake-auth.md` | E2E Fake Auth |
| `rules/e2e-robot-pattern.md` | E2E Robot Pattern |
| `rules/fastapi-lifespan-pattern.md` | Fastapi Lifespan Pattern |
| `rules/firebase.md` | Firebase Auth, Firestore, and backend token verification patterns. |
| `rules/firebase-auth.md` | Firebase Auth |
| `rules/firestore-vs-postgres-usage.md` | Firestore Vs Postgres Usage |
| `rules/gemini-structured-output.md` | Gemini Structured Output |
| `rules/hilt-di-module-convention.md` | Hilt Di Module Convention |
| `rules/hilt-module-organization.md` | Hilt Module Organization |
| `rules/model-import-5-locations.md` | Model Import 5 Locations |
| `rules/module-dependency-direction.md` | Module Dependency Direction |
| `rules/navigation-screen-pattern.md` | Navigation Screen Pattern |
| `rules/offline-first-repository.md` | Offline First Repository |
| `rules/offline-sync-queue.md` | Offline Sync Queue |
| `rules/prompt-auto-enhance-rule.md` | Auto-enhance every user prompt with project-specific context before acting. Prefix every response with a brief *Enhanced: ...* indicator.
 |
| `rules/pydantic-android-schema-sync.md` | Pydantic Android Schema Sync |
| `rules/rule-writing-meta.md` | Meta-guidance for writing effective CLAUDE.md rules, choosing config file placement, and structuring project instructions. |
| `rules/security-headers-middleware.md` | Security Headers Middleware |
| `rules/settings-configuration-validation.md` | Settings Configuration Validation |
| `rules/superpowers.md` | Superpowers |
| `rules/tdd-rule.md` | Test-driven development workflow rules for red-green-refactor cycle. |
| `rules/test-fixture-conventions.md` | Test Fixture Conventions |
| `rules/testing.md` | Testing conventions and best practices. |
| `rules/viewmodel-navigation-events.md` | Viewmodel Navigation Events |
| `rules/workflow.md` | Development workflow guidelines for structured feature implementation and bug fixes. |

## Claude Code Configuration

The `.claude/` directory contains 122 skills, 33 agents, and 47 rules for Claude Code.

<!-- hub:best-practices:end -->
