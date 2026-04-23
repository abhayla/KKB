# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
./gradlew lint                   # Android Lint (configured in app/build.gradle.kts)
```

### Backend (run from `backend/` — details in `backend/CLAUDE.md`)

```bash
PYTHONPATH=. pytest              # All tests
PYTHONPATH=. pytest tests/test_auth.py -v                    # Single file
PYTHONPATH=. pytest tests/test_preference_service.py::test_add_include_rule -v  # Single test
uvicorn app.main:app --reload    # Dev server (DEBUG=true for /docs)
ruff check app/ tests/           # Lint
black app/ tests/                # Format
```

### Environment

**Backend `.env`:** `DATABASE_URL`, `FIREBASE_CREDENTIALS_PATH`, `ANTHROPIC_API_KEY`, `GOOGLE_AI_API_KEY`, `JWT_SECRET_KEY` (REQUIRED, no default), `DEBUG=true`, `SENTRY_DSN` (optional). Android needs `google-services.json` in `android/app/` and `local.properties` with `sdk.dir`. Personal overrides go in `CLAUDE.local.md` (gitignored).

## Test Fixtures (Backend)

See `backend/tests/CLAUDE.md` for the fixture selection table and multi-user patterns. Enforcement: `.claude/rules/test-fixture-conventions.md`.

## E2E Tests

17 journey suites (J01–J17) running against a real backend via fake phone auth (`http://10.0.2.2:8000`, `DEBUG=true`). Use API 34 emulator (not 36). Details in `.claude/rules/e2e-fake-auth.md`, `.claude/rules/e2e-robot-pattern.md`, `.claude/rules/e2e-journey-numbered-suite-organization.md`, and `docs/testing/E2E-Testing-Prompt.md`.

## Troubleshooting

Android/Gradle issues — `android/CLAUDE.md`. Backend import/env issues — `backend/CLAUDE.md`. SQLAlchemy async pitfalls (MissingGreenlet, UUID↔String(36) comparisons) — `.claude/rules/async-repository-session-maker-pattern.md` and `.claude/rules/backend.md`. Cross-cutting:

| Issue | Fix |
|-------|-----|
| Meal gen timeout | 180s endpoint timeout, ~35s typical. Logs: uvicorn stdout (JSON in prod, text in DEBUG) |
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
