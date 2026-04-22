# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Session continuity:** Use `/save-session` before ending, `/start-session` to resume. On compaction, preserve modified file paths, test commands run, workflow step, and any GitHub Issue numbers.

## Project Overview

**RasoiAI** (रसोई AI) is an AI-powered meal planning application for Indian families. Generates personalized weekly meal plans based on family preferences, dietary restrictions, regional cuisines, and festivals/fasting days.

| Attribute | Details |
|-----------|---------|
| **Android** | Kotlin 2.2.10, Jetpack Compose BOM 2024.02.00, Hilt 2.56.1, Room 2.8.1, KSP 2.3.2, AGP 9.0.1, Target SDK 34 (Min 24) |
| **Backend** | Python 3.11+, FastAPI, PostgreSQL 12+, SQLAlchemy async |
| **AI** | Gemini `gemini-2.5-flash` via `google-genai` SDK (meal gen + photo analysis), Claude API (chat) |

## Architecture

Four Android modules: `app` (presentation) -> `domain` (pure Kotlin interfaces/models) <- `data` (Room + Retrofit impls) -> `core` (shared utilities). Domain has zero Android dependencies. Offline-first: Room is source of truth, API syncs in background. Mappers centralized — see `.claude/rules/centralized-mapper-convention.md`. Backend services are standalone async functions (not classes).

**5-location model import rule (CRITICAL):** Adding a SQLAlchemy model requires updates in 5 files — see `.claude/rules/model-import-5-locations.md`. Partial updates cause silent test/migration failures.

**Detailed conventions** auto-load via path-scoped rules in `.claude/rules/`.

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

**Backend `.env`:** `DATABASE_URL`, `FIREBASE_CREDENTIALS_PATH`, `ANTHROPIC_API_KEY`, `GOOGLE_AI_API_KEY`, `JWT_SECRET_KEY` (REQUIRED, no default), `DEBUG=true`, `SENTRY_DSN` (optional). Android needs `google-services.json` in `android/app/` and `local.properties` with `sdk.dir`. Personal overrides go in `CLAUDE.local.md` (gitignored).

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

Android/Gradle issues — see `android/CLAUDE.md`. Backend import/env issues — see `backend/CLAUDE.md`. Cross-cutting gotchas below:

| Issue | Fix |
|-------|-----|
| MissingGreenlet | Use `selectinload()` for eager loading |
| Meal gen timeout | 180s endpoint timeout, ~35s typical. Logs: uvicorn stdout (JSON in prod, text in DEBUG) |
| Recipe ID 500s | Compare recipe IDs as strings — `uuid.UUID` vs `String(36)` mismatch |
| E2E flakes on API 36 emulator | Use API 34 (API 36 has Espresso issues) |

## Key Documentation

| Resource | Location |
|----------|----------|
| Sub-directory CLAUDE.md | `android/CLAUDE.md`, `android/app/src/androidTest/CLAUDE.md`, `backend/CLAUDE.md`, `backend/tests/CLAUDE.md` |
| E2E testing guide | `docs/testing/E2E-Testing-Prompt.md` |
| Technical design | `docs/design/RasoiAI Technical Design.md` |
| VPS deployment | `docs/VPS-Deployment.md` — do NOT modify `C:\Apps\shared\` |
| MCP Servers | `.mcp.json` — Playwright (web screenshots), ADB (emulator automation) |

<!-- hub:best-practices:start -->

<!-- PROTECTED SECTION — managed by claude-best-practices hub. -->
<!-- Do NOT condense, rewrite, reorganize, or remove.          -->
<!-- Any /init or optimization request must SKIP this section.  -->

## Rules for Claude

1. **Bug Fixing**: Use `/fix-loop` or `/fix-issue`. Start by writing a test that reproduces the bug, then fix and prove with a passing test.
2. **Rules**: Path-scoped rules live in `.claude/rules/` and auto-load via `globs:` frontmatter when matching files are opened. Browse with `ls .claude/rules/` — enumerating each rule here would cost ~4k tokens per session for zero enforcement benefit.

## Claude Code Configuration

The `.claude/` directory contains 123 skills, 35 agents, and 56 rules for Claude Code.

<!-- hub:best-practices:end -->
