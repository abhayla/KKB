# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Session continuity:** Use `/save-session` before ending work, `/start-session` to resume. Session files in `.claude/sessions/`.

**Context compaction:** When compacting, always preserve: the 5-location model import rule, test fixture choices (client vs unauthenticated_client vs authenticated_client), all file paths that were modified, any test commands that were run, the current workflow step number, and any GitHub Issue numbers being worked on.

## Project Overview

**RasoiAI** (रसोई AI) is an AI-powered meal planning application for Indian families. Generates personalized weekly meal plans based on family preferences, dietary restrictions, regional cuisines, and festivals/fasting days.

| Attribute | Details |
|-----------|---------|
| **Android** | Kotlin 2.2.10, Jetpack Compose BOM 2024.02.00, Hilt 2.56.1, Room 2.8.1, KSP 2.3.2, AGP 9.0.1, Target SDK 34 (Min 24) |
| **Backend** | Python 3.11+, FastAPI, PostgreSQL 12+, SQLAlchemy async |
| **AI** | Gemini `gemini-2.5-flash` via `google-genai` SDK (meal gen + photo analysis), Claude API (chat) |

## Architecture

Four Android modules: `app` (presentation) -> `domain` (pure Kotlin interfaces/models) <- `data` (Room + Retrofit impls) -> `core` (shared utilities). Domain has zero Android dependencies. Offline-first: Room is source of truth, API syncs in background. Mappers centralized in `EntityMappers.kt` and `DtoMappers.kt`. Backend services are standalone async functions (not classes).

**5-location model import rule (CRITICAL):** New SQLAlchemy models require updates in: (1) `models/your_model.py`, (2) `models/__init__.py`, (3) `db/postgres.py` (3 blocks: init_db, create_tables, drop_tables), (4) `tests/conftest.py`, (5) Alembic migration. Missing any causes silent test/migration failures.

**Detailed conventions** auto-load via path-scoped rules in `.claude/rules/` (44+ rules covering Android, backend, testing, database, Compose, etc.).

## Development Commands

### Android (run from `android/` — details in `android/CLAUDE.md`)

```bash
./gradlew assembleDebug          # Quick build
./gradlew test                   # All unit tests
./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest"  # Single test
./gradlew :app:connectedDebugAndroidTest  # All instrumented tests (emulator API 34)
```

### Backend (run from `backend/` — details in `backend/CLAUDE.md`)

```bash
PYTHONPATH=. pytest              # All tests
PYTHONPATH=. pytest tests/test_auth.py -v                    # Single file
PYTHONPATH=. pytest tests/test_preference_service.py::test_add_include_rule -v  # Single test
uvicorn app.main:app --reload    # Dev server (DEBUG=true for /docs)
```

### Environment

**Backend `.env`:** `DATABASE_URL`, `FIREBASE_CREDENTIALS_PATH`, `ANTHROPIC_API_KEY`, `GOOGLE_AI_API_KEY`, `JWT_SECRET_KEY` (REQUIRED, no default), `DEBUG=true`, `SENTRY_DSN` (optional). Android needs `google-services.json` in `android/app/` and `local.properties` with `sdk.dir`.

## Test Fixtures (Backend)

All in `backend/tests/conftest.py` — do NOT duplicate. Details in `backend/tests/CLAUDE.md`.

| Fixture | Auth | Use when |
|---------|------|----------|
| `client` | Pre-authenticated | Most tests |
| `unauthenticated_client` | No auth | Testing 401s |
| `authenticated_client` | Real JWT | Testing JWT verification |
| `db_session` | N/A | Direct service/repository tests |

Multi-user: `make_api_client()` from `tests/api/conftest.py`.

## E2E Tests

Real backend + fake phone auth. `FakePhoneAuthClient` -> `"fake-firebase-token"` -> backend (DEBUG=true) accepts -> real JWT -> real PostgreSQL. URL: `http://10.0.2.2:8000`. API 34 emulator (not 36). 17 journey suites (J01-J17), robot pattern in `e2e/robots/`, TestTags in `presentation/common/TestTags.kt`. Guide: `docs/testing/E2E-Testing-Prompt.md`.

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Gradle sync fails | JDK 17+, `JAVA_HOME` set, check `libs.versions.toml` |
| API 36 emulator | Use API 34. CI uses API 29 |
| KSP/Hilt errors | `./gradlew clean :app:kspDebugKotlin` |
| Backend import errors | `cd backend && PYTHONPATH=. pytest` |
| MissingGreenlet | Use `selectinload()` for eager loading |
| Meal gen timeout | 180s endpoint timeout, ~35s typical. Logs: `logs/MEAL_PLAN-*.json` |
| Recipe ID 500s | Compare recipe IDs as strings — `uuid.UUID` vs `String(36)` mismatch |

## VPS Deployment

See `docs/VPS-Deployment.md`. Do NOT modify files in `C:\Apps\shared\`.

## Key Documentation

| Resource | Location |
|----------|----------|
| Session checkpoints | `.claude/sessions/` (via `/save-session` + `/start-session`) |
| Sub-directory CLAUDE.md | `android/CLAUDE.md`, `backend/CLAUDE.md`, `backend/tests/CLAUDE.md` |
| E2E testing guide | `docs/testing/E2E-Testing-Prompt.md` |
| Technical design | `docs/design/RasoiAI Technical Design.md` |
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

The `.claude/` directory contains 123 skills, 33 agents, and 47 rules for Claude Code.

<!-- hub:best-practices:end -->
