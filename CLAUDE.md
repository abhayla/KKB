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

## Project Rules for Claude

1. **Document Output**: Generated documents → `docs/claude-docs/`. Test screenshots → `docs/testing/screenshots/` (gitignored).

2. **Screenshots (CRITICAL)**:
   - **ALL screenshots MUST be saved to `docs/testing/screenshots/`** — no exceptions
   - This includes: Playwright screenshots, emulator screenshots, UI test captures, debugging screenshots
   - NEVER save screenshots to the project root, `.playwright-mcp/`, or any other location
   - The folder is gitignored — screenshots are temporary debugging artifacts
   - Use descriptive filenames: `{feature}_{context}.png` (e.g., `home_after_login.png`)
   - PNG format, limit to 1280x720, avoid `fullPage: true` on long pages. Hook auto-resizes >1800px.

3. **Bug & Feature Tracking**:
   - **Before starting work**: Check GitHub Issues for related bugs/features with `gh issue list`
   - **Finding TODOs**: When you find `/* TODO: ... */` comments, consider creating a GitHub Issue
   - **After fixing**: Reference issue number in commit: `Fix #123: description`
   - **Use `/fix-issue <number>`**: To implement a fix for a specific GitHub Issue
   - **Labels**: `bug`, `enhancement`, `not-implemented`, `home-screen`, etc.
   - **Issue Templates**: `.github/ISSUE_TEMPLATE/` has templates for bug reports and feature requests

4. **Functional Requirements**: See `docs/testing/Functional-Requirement-Rule.md` for the traceability matrix and workflow (FR-001 through FR-023).

5. **Development Workflow**: 7-step workflow enforced by hooks. See `.claude/rules/workflow.md`.

6. **Session History Analysis**: Run `/skill-factory scan` then `/skill-factory propose` to detect and classify workflow patterns.

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

## Rules for Claude

1. **Bug Fixing**: Use `/fix-loop` or `/fix-issue`. Write a test that reproduces the bug first, then fix and prove with a passing test.
2. **Path-scoped rules** in `.claude/rules/` auto-load when working on matching files (47 rules covering Android, backend, testing, database, Compose, etc.).
3. **Skills** in `.claude/skills/` provide multi-step workflows: `/fix-issue`, `/fix-loop`, `/implement`, `/run-backend-tests`, `/skill-factory`, etc.
