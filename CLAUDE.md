# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Session context:** Check @docs/CONTINUE_PROMPT.md for active work between sessions.

**Imported references** (loaded on demand via `@` syntax):
- Technical design: @docs/design/RasoiAI Technical Design.md
- Data flow: @docs/design/Data-Flow-Diagram.md
- Meal generation: @docs/design/Meal-Generation-Algorithm.md
- Workflow rules: @docs/rules/Claude Code Enforced Workflow Rules.md
- E2E testing guide: @docs/testing/E2E-Testing-Prompt.md
- Functional requirements: @docs/testing/Functional-Requirement-Rule.md

**Context compaction:** When compacting, always preserve: the 5-location model import rule, test fixture choices (client vs unauthenticated_client vs authenticated_client), all file paths that were modified, any test commands that were run, the current workflow step number, and any GitHub Issue numbers being worked on.

## Project Overview

**RasoiAI** (रसोई AI) is an AI-powered meal planning application for Indian families. It generates personalized weekly meal plans based on family preferences, dietary restrictions, regional cuisines, and cultural considerations including festivals and fasting days.

| Attribute | Details |
|-----------|---------|
| **Platform** | Android Native (Kotlin 2.2.10 + Jetpack Compose BOM 2024.02.00) |
| **Backend** | Python (FastAPI + PostgreSQL + SQLAlchemy async) |
| **Target SDK** | 34 (Min SDK 24 / Android 7.0) |
| **Build Tools** | AGP 9.0.1, KSP 2.3.2, Hilt 2.56.1, Room 2.8.1 |
| **Target Market** | Pan-India (Tier 1, 2, 3 cities) |

## Architecture

### Module Structure

Four-layer architecture under `android/`:

| Module | Package | Purpose |
|--------|---------|---------|
| `app` | `com.rasoiai.app.*` | Screens, ViewModels, Hilt modules, navigation |
| `domain` | `com.rasoiai.domain.*` | Models, repository interfaces, use cases |
| `data` | `com.rasoiai.data.*` | Room (local), Retrofit (remote), repository impls |
| `core` | `com.rasoiai.core.*` | Shared UI components, utilities, NetworkMonitor |

```
app ─────┬──────> core
         ├──────> domain  <────┐
         └──────> data ────────┴──────> core
```

### Key Architecture Decisions

| Area | Decision |
|------|----------|
| Dependency Injection | Hilt |
| State Management | StateFlow + Single UiState Data Class |
| Navigation | Navigation Compose |
| Database | Room (Android cache), PostgreSQL (backend source of truth) |
| Auth | Firebase Auth (Phone OTP) |
| LLM | Claude API (chat tool calling), Gemini `gemini-2.5-flash` via `google-genai` SDK (meal generation + food photo analysis) |
| Offline Support | Room as source of truth with sync to backend |

### Data Flow

```
UI → ViewModel → UseCase → Repository
                              ↓
                 ┌────────────┴────────────┐
                 ↓                         ↓
            Room (Local)            Retrofit (Remote)
            Source of Truth
                 ↓                         ↓
            EntityMappers              DtoMappers
                 └──────────┬──────────────┘
                            ↓
                      Domain Models
```

**Key mapper files:**
- `data/local/mapper/EntityMappers.kt` - Room Entity ↔ Domain
- `data/remote/mapper/DtoMappers.kt` - API DTO → Domain & Entity

### Backend Structure

```
backend/app/
├── ai/            # Claude chat (tool calling) + Gemini meal generation + photo analysis
├── api/v1/        # 13 routers across 12 files (~44 endpoints). Swagger at /docs (DEBUG=true only)
├── cache/         # recipe_cache.py — warmed on startup, non-fatal
├── config.py      # Pydantic Settings (env vars, JWT secret required, usage limits)
├── core/          # firebase.py (auth + DEBUG bypass), security.py, exceptions.py
├── db/            # postgres.py (3 model import blocks), database.py
├── main.py        # FastAPI app, Sentry init, SecurityHeadersMiddleware, rate limiting
├── models/        # SQLAlchemy ORM (13 files)
├── repositories/  # Data access (5 files)
├── schemas/       # Pydantic request/response DTOs
└── services/      # Business logic (20 files)
```

**5-location model import rule (CRITICAL):** When adding a new SQLAlchemy model, ALL of these must be updated or tests/migrations silently fail:
1. `app/models/your_model.py` — define the model
2. `app/models/__init__.py` — re-export
3. `app/db/postgres.py` — import in `init_db()`, `create_tables()`, AND `drop_tables()` (3 blocks)
4. `tests/conftest.py` — import so SQLite creates the table
5. Generate Alembic migration

## Patterns

### ViewModel Pattern

ViewModels extend `BaseViewModel<T : BaseUiState>` (`presentation/common/BaseViewModel.kt`), providing `updateState`/`setState` helpers. Key elements:
- `BaseUiState` interface: requires `isLoading: Boolean` and `error: String?`
- UiState data class implements `BaseUiState`, uses `copy()` for updates
- One-time events via `Channel` + `receiveAsFlow()`
- `@HiltViewModel` with `@Inject constructor`
- `Resource<T>` sealed class (`Success`/`Error`/`Loading`) in `presentation/common/UiState.kt`

### Repository Pattern (Offline-First)

Repositories read from Room (source of truth), fetch from API when online. Mutations update local DB first, then sync to server if online, or mark as unsynced for later.

## Development Commands

### Android (run from `android/`)

```bash
./gradlew build                  # Full build with tests
./gradlew assembleDebug          # Quick compilation (no tests)
./gradlew test                   # All unit tests
./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest"  # Single test
./gradlew :app:connectedDebugAndroidTest  # All instrumented tests (emulator API 34)
./gradlew lint
./gradlew installDebug
./gradlew clean && ./gradlew assembleDebug  # Fix strange build issues

# E2E tests (requires running emulator + backend at localhost:8000 with DEBUG=true)
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.FullUserJourneyTest  # Single E2E
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.rasoiai.app.e2e.flows  # All E2E flows

# Customer journey suites (14 suites: J01-J14, group E2E tests by user scenario)
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J01_FirstTimeUserSuite
```

### Backend (run from `backend/`)

```bash
source venv/bin/activate         # Linux/Mac/Git Bash
uvicorn app.main:app --reload    # Dev server → http://localhost:8000/docs
PYTHONPATH=. pytest              # All tests
PYTHONPATH=. pytest tests/test_auth.py -v                    # Single file
PYTHONPATH=. pytest tests/test_preference_service.py::test_add_include_rule -v  # Single test
alembic upgrade head             # Run migrations
alembic revision --autogenerate -m "description"  # New migration
PYTHONPATH=. pytest --collect-only -q             # Count all tests

# After migrations — seed reference data (first-time setup)
PYTHONPATH=. python scripts/seed_festivals.py               # Festival calendar
PYTHONPATH=. python scripts/seed_achievements.py            # Achievement definitions
PYTHONPATH=. python scripts/import_recipes_postgres.py      # 3,580 recipes from KKB dataset
PYTHONPATH=. python scripts/sync_config_postgres.py         # Meal gen YAML config → PostgreSQL
```

### Prerequisites

| Tool | Version |
|------|---------|
| JDK | 17+ (`JAVA_HOME` must be set) |
| Python | 3.11+ |
| PostgreSQL | 12+ |
| Android SDK | API 34 (Min 24) |

**Dependency versions:** See `backend/requirements.txt` and `android/gradle/libs.versions.toml`.

### Code Formatting

Auto-formatting runs via the `auto-format.sh` hook after edits. Manual commands:

```bash
# Python (from backend/)
black app/ tests/                    # Format code
ruff check app/ tests/ --fix        # Lint + auto-fix

# Kotlin — no manual formatter configured; Compose stability rules in android/app/compose-stability.conf
```

### Environment Setup

**Backend `.env` file:**
```
DATABASE_URL=postgresql+asyncpg://rasoiai_user:password@localhost:5432/rasoiai
FIREBASE_CREDENTIALS_PATH=./rasoiai-firebase-service-account.json
ANTHROPIC_API_KEY=sk-ant-...
GOOGLE_AI_API_KEY=your-gemini-api-key
JWT_SECRET_KEY=your-secret-key         # REQUIRED — no default, crashes on startup if missing
DEBUG=true                             # Default is false — must opt-in for debug mode
SENTRY_DSN=https://...@sentry.io/...   # optional, enables error monitoring
CORS_ORIGINS=["http://localhost:3000"] # Default is [] — Android app doesn't need CORS
```

**Android `local.properties`** (required — see `local.properties.example`):
```properties
sdk.dir=/path/to/Android/sdk
```
Also requires `google-services.json` in `android/app/` (from Firebase Console, with Phone Auth enabled). No `WEB_CLIENT_ID` needed — Phone Auth doesn't use it.

**PostgreSQL:**
```sql
CREATE DATABASE rasoiai;
CREATE USER rasoiai_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE rasoiai TO rasoiai_user;
```

### CI/CD Pipeline

Four GitHub Actions workflows in `.github/workflows/`:
- **`android-ci.yml`** — Lint, unit tests, build APK on push; emulator tests (API 29) on PRs
- **`backend-ci.yml`** — Backend pytest suite on push/PR when `backend/` changes (Python 3.11, Ubuntu)
- **`claude.yml`** — Claude Code agent triggered by `@claude` mentions in issues/PRs (read-only permissions)
- **`claude-code-review.yml`** — Automatic code review on all PR opens/updates

## Testing, Domain Models, Backend API, Database

Detailed content is in path-scoped rules (auto-loaded when working on matching files):

| Topic | Rule File |
|-------|-----------|
| Test distribution, fixtures, E2E infrastructure | `.claude/rules/testing.md` |
| Domain models, enums, navigation routes | `.claude/rules/android.md` |
| API endpoints, service patterns, meal generation | `.claude/rules/backend.md` |
| Room & PostgreSQL schema, migrations | `.claude/rules/database.md` |
| Design system, Compose patterns | `.claude/rules/compose-ui.md` |

Key test commands: `PYTHONPATH=. pytest` (backend) | `./gradlew test` (Android) | `./gradlew :app:connectedDebugAndroidTest` (E2E)

### Backend Test Fixtures (Quick Reference)

All standard fixtures live in `backend/tests/conftest.py`. Do NOT duplicate in subdirectories.

| Fixture | Auth | Use when |
|---------|------|----------|
| `client` | Pre-authenticated (`get_current_user` → `test_user`) | Most tests — authenticated endpoint calls |
| `unauthenticated_client` | No auth override | Testing 401 responses |
| `authenticated_client` | Real JWT in `Authorization: Bearer` header | Testing actual JWT verification flow |
| `db_session` | N/A | Direct service/repository unit tests |

For custom setups (e.g., two users): use `make_api_client()` from `tests/api/conftest.py`. Services that call `async_session_maker` directly (e.g., `auth_service.py`) must also be patched — see `backend/tests/CLAUDE.md` for details.

### E2E Test Architecture

E2E tests use **real backend + fake phone auth only**. `FakePhoneAuthClient` sends `"fake-firebase-token"` → backend (`DEBUG=true`) accepts it → returns real JWT. All API calls hit real PostgreSQL.

| File | Purpose |
|------|---------|
| `e2e/base/BaseE2ETest.kt` | Base class with Hilt setup, auth state helpers |
| `e2e/di/FakePhoneAuthClient.kt` | Bypasses Firebase Phone Auth |
| `e2e/robots/` | Robot pattern (HomeRobot, GroceryRobot, etc.) |
| `e2e/journeys/` | 14 customer journey suites (J01-J14) grouping 23 test files |
| `presentation/common/TestTags.kt` | All semantic test tags for UI elements |

**E2E backend URL:** `http://10.0.2.2:8000` (Android emulator maps `10.0.2.2` → host `localhost`).

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Gradle sync fails | Ensure JDK 17+, `JAVA_HOME` set. Check `gradle/libs.versions.toml` for versions. |
| API 36 emulator issues | Use API 34 locally - API 36 has Espresso compatibility issues. CI uses API 29. |
| Gradle daemon hangs (Windows) | Run `./gradlew --stop` |
| KSP/Hilt errors | Run `./gradlew clean :app:kspDebugKotlin` |
| Backend import errors | Run from backend dir: `cd backend && PYTHONPATH=. pytest` |
| MissingGreenlet errors | SQLAlchemy async requires `selectinload()` for eager loading |
| Meal generation timeout | Migrated to `google-genai` SDK with native async (`client.aio`). AI takes 4-7 seconds; E2E tests use 30-second timeout. Old `google-generativeai` SDK blocked uvicorn event loop. |
| Room DB not found | Run `./gradlew clean` then rebuild - schema may have changed |
| Test flakiness | Use `waitUntil {}` in E2E tests; check `RetryUtils.kt` for patterns |
| Screenshot processing errors | Use PNG, limit to 1280x720, avoid `fullPage` on long pages. Auto-resized by `post-screenshot-resize.sh` hook (max 1800px). Batch fix: `python .claude/hooks/resize_screenshot.py --all` |
| ADB screenshot corrupted | Do NOT use `-d 0`. Hook auto-strips `[Warning] Multiple displays...` text and retries. Manual fix: `python .claude/hooks/resize_screenshot.py --all` |
| 1 auth test fails | Pre-existing: `conftest.py` globally overrides auth dependency. Not a regression. |
| Enum case in Room tests | Room stores MealType as uppercase (`BREAKFAST`, `LUNCH`, `DINNER`, `SNACKS`). Tests must match. |
| Backend changes not reflected | Stale `.pyc` cache: `find backend -name "*.pyc" -delete && find backend -name "__pycache__" -type d -exec rm -rf {} +` |
| Recipe ID 500 errors in PostgreSQL | Always compare recipe IDs as strings in queries — `uuid.UUID` vs `String(36)` column type mismatch causes silent 500s |
| JUnit Platform Launcher (Gradle 9) | All modules need `testRuntimeOnly(libs.junit.platform.launcher)` — Gradle 9.x won't load JUnit 5 without it |
| Hilt + KSP2 classloader error | Hilt 2.50 doesn't support KSP 2.x. Use Hilt 2.51+ (currently 2.56.1) |
| Room "unexpected jvm signature V" | Room 2.6.1 doesn't support KSP2. Use Room 2.8.1+ |
| Test Orchestrator breaks E2E | Intentionally disabled — re-enabling clears DataStore between tests, breaking `BaseE2ETest.backendMealPlanGenerated` state sharing |
| Auth race condition in E2E | SplashViewModel has 2-second delay before checking `phoneAuthClient.isSignedIn`. Tokens must be in DataStore before this fires. `FakePhoneAuthClient` handles this. |

## VPS Deployment

This project's development VPS is **544934-ABHAYVPS** (Windows Server 2022, IP `103.118.16.189`). All deployment happens from `C:\Apps\`. VPS documentation lives at `C:\Apps\shared\docs\` — **do NOT modify files in `C:\Apps\shared\`**.

| Component | Version |
|-----------|---------|
| Node.js | v24.1.0 |
| PM2 | 6.0.13 |
| Nginx | 1.26.2 |
| PostgreSQL | 16.8 |
| Redis | Port 6379 |

**Architecture:** Internet → Cloudflare (HTTPS) → Nginx (port 80, reverse proxy) → PM2 apps (ports 3001-3004, 8000).

**Key VPS commands (PowerShell):**
```powershell
pm2 ls                                    # List all apps
pm2 logs <app-name> --lines 100           # View logs
pm2 restart <app-name> && pm2 save        # Restart + persist state
cd C:\Apps\nginx && .\nginx.exe -t        # Test Nginx config
cd C:\Apps\nginx && .\nginx.exe -s reload # Reload Nginx (zero-downtime)
netstat -ano | findstr "3001 3002 3003 3004 8000"  # Check ports
```

**VPS documentation index:** `C:\Apps\shared\docs\README.md` — covers setup, PM2, Nginx, Cloudflare, CI/CD, monitoring, troubleshooting.

## Rules for Claude

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

   > **Full Reference:** See `docs/rules/Claude Code Enforced Workflow Rules.md` for complete documentation.

   **TRIGGER - This workflow applies when user asks to:**
   - Implement a feature
   - Fix a bug
   - Refactor code
   - Make any code change (`.kt`, `.py`, `.xml` files)

   **DOES NOT APPLY to:**
   - Answering questions (no code changes)
   - Documentation-only changes (no code)
   - Research/exploration tasks

   ---

   ### THE 7-STEP WORKFLOW

   **STEP 1: Update Requirement Documentation**

   Before writing ANY code:
   ```bash
   # a) Check for existing issue
   gh issue list --search "keyword"

   # b) Create issue if none exists
   gh issue create --title "Feature: Description"
   ```
   - Add requirement to `docs/requirements/screens/*.md` (BDD format)
   - Add traceability row to `docs/testing/Functional-Requirement-Rule.md`

   **Output:**
   ```
   ✅ Step 1 Complete:
   - GitHub Issue: #XX (created/existing)
   - Requirement ID: SCREEN-XXX
   - Traceability: Added to Functional-Requirement-Rule.md
   ```

   **STEP 2: Create/Update Tests**

   - Create E2E test in `app/src/androidTest/java/com/rasoiai/app/e2e/flows/`
   - Add KDoc header: `/** Requirement: #XX - Description */`
   - Write test methods matching acceptance criteria

   **Output:**
   ```
   ✅ Step 2 Complete:
   - Test file: XXXFlowTest.kt
   - Test methods: [list]
   ```

   **STEP 3: Implement the Feature**

   Write the minimum code to make tests pass.

   **STEP 4: Run Functional Tests**

   ```bash
   # Android
   ./gradlew :app:connectedDebugAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.YourTestClass

   # Backend
   PYTHONPATH=. pytest tests/test_xxx.py -v
   ```

   **STEP 5: Fix Loop**

   IF tests fail → fix code → re-run tests → repeat until ALL pass.

   ⚠️ **DO NOT proceed to Step 6 until ALL tests pass**

   **STEP 6: Capture Screenshots**

   Platform-specific capture to `docs/testing/screenshots/`:
   ```bash
   # Android (ADB)
   adb exec-out screencap -p > docs/testing/screenshots/{issue}_{feature}_before.png
   adb exec-out screencap -p > docs/testing/screenshots/{issue}_{feature}_after.png
   ```
   ```javascript
   // Web (Playwright)
   await browser_take_screenshot({
     filename: "docs/testing/screenshots/{issue}_{feature}_{state}.png",
     type: "png"
   })
   ```

   **STEP 7: Verify and Confirm**

   1. Read both screenshots using Read tool
   2. Describe the visible difference
   3. Commit with issue reference

   **Final Output:**
   ```
   ✅ WORKFLOW COMPLETE:
   - GitHub Issue: #XX
   - Requirement: SCREEN-XXX
   - Tests: X/X passed
   - Screenshots:
     - Before: docs/testing/screenshots/XX_before.png
     - After: docs/testing/screenshots/XX_after.png
   - Verification: [describe visible change]

   The feature has been implemented and all tests pass.
   ```

   ---

   ### SELF-ENFORCEMENT GATES (MANDATORY)

   **Claude MUST answer these in each response:**

   **Pre-Implementation Gate (Before Step 3):**
   ```
   □ Pre-Implementation Gate:
     - "Step 1 complete (Requirements)?" → [YES / NO - STOP]
     - "Step 2 complete (Tests created)?" → [YES / NO - STOP]
     - "BEFORE screenshot captured?" → [YES: path / NO - STOP]
     - "Issue number noted?" → [YES: #___ / NO - STOP]
   ```

   **Pre-Commit Gate (Before Step 7 commit):**
   ```
   □ Pre-Commit Gate:
     - "ALL tests passing?" → [YES: X/X passed / NO - STOP]
     - "AFTER screenshot captured?" → [YES: path / NO - STOP]
     - "Before/after compared?" → [YES: difference is ___ / NO - STOP]
   ```

   ---

   ### CRITICAL RULES - NO EXCEPTIONS

   - **No partial test passes**: "2 out of 3 is good enough" = VIOLATION
   - **No @Ignore bypasses**: Marking failing tests as `@Ignore` = VIOLATION
   - **No "fix later" excuses**: Creating issues to bypass failures = VIOLATION
   - **No step skipping**: Each step must complete before the next
   - **No commits without tests**: Tests MUST pass before any commit
   - **No screenshot skipping**: Screenshots are MANDATORY for Steps 6-7, even when:
     - "Documenting existing behavior" - STILL REQUIRES SCREENSHOTS
     - "No code changes made" - STILL REQUIRES SCREENSHOTS
     - "Tests already pass" - STILL REQUIRES SCREENSHOTS
     - ADB/screenshot tools fail - MUST troubleshoot and retry, never skip

   **VIOLATION = PROCESS FAILURE. No exceptions. No "I'll do it later."**

   ---

   ### QUICK REFERENCE

   | Step | Action | Output Required |
   |------|--------|-----------------|
   | 1 | Update requirements | Issue #, Requirement ID |
   | 2 | Create tests | Test file, methods |
   | 3 | Implement | Code changes |
   | 4 | Run tests | Pass/fail count |
   | 5 | Fix loop | All tests passing |
   | 6 | Screenshots | Before/after paths |
   | 7 | Verify & commit | Visual diff, commit hash |

## Workflow Enforcement Hooks

The 7-step workflow (Rule #7) is enforced by shell hooks in `.claude/hooks/`. All hooks source `hook-utils.sh` for shared stdin JSON parsing and state management.

| Hook | Trigger | Purpose |
|------|---------|---------|
| `hook-utils.sh` | (sourced) | Shared library — stdin parsing, state mgmt, test detection, evidence writing |
| `validate-workflow-step.sh` | PreToolUse (Write/Edit/Bash) | Blocks actions if prior workflow steps are incomplete; blocks commits without pipeline; **blocks code edits when testFailuresPending** |
| `pre-skill-fixloop-unblock.sh` | PreToolUse (Skill) | Sets `fixLoopInvestigating=true` when fix-loop is invoked, unblocking code edits during investigation |
| `verify-evidence-artifacts.sh` | PreToolUse (Bash) | Blocks `git commit` when required evidence is missing (Skill invocations not tracked) |
| `post-test-update.sh` | PostToolUse (Bash) | Records test results in workflow state and evidence files; **sets/clears testFailuresPending flag** |
| `verify-test-rerun.sh` | PostToolUse (Bash) | Re-runs same test independently; **blocks** if claimed PASS but re-run FAIL |
| `log-workflow.sh` | PostToolUse (Bash/Skill/Write/Edit) | Logs events; **tracks Skill invocations**; **clears fixLoopInvestigating on fix-loop completion** |
| `post-anr-detection.sh` | PostToolUse (Bash) | Detects ANR patterns in Bash output; sets `testFailuresPending=true` and logs to `adb-test/anr-events.log` |
| `post-screenshot.sh` | PostToolUse (Bash/Playwright) | Combined: auto-resize screenshots >1800px + validate file + record metadata in workflow state |
| `auto-fix-pattern-scan.sh` | PostToolUse | Scans for common fix patterns after tool use |
| `auto-format.sh` | PostToolUse | Auto-formats code after edits |
| `post-skill-learning.sh` | PostToolUse (Skill) | Records skill outcomes for learning system |

Workflow state is tracked in `.claude/workflow-state.json` (extended schema with `testFailuresPending`, `fixLoopInvestigating`, `visualIssuesPending`, `screenshotsCaptured`, `backendChecks`, `skillInvocations`, `evidence`, `agentDelegations`). The full hook system and enforcement logic is documented in `docs/rules/Claude Code Enforced Workflow Rules.md`.

## Claude Code Configuration

The `.claude/` directory contains: `agents/` (11 agents), `knowledge.db` (pattern library), `skills/` (12 slash commands), `hooks/` (12 hooks — see table above), `rules/` (5 path-scoped rule files), `logs/` (session logs).

**Skills:** `/adb-test`, `/auto-verify`, `/fix-issue`, `/fix-loop`, `/implement`, `/post-fix-pipeline`, `/reflect`, `/run-e2e`, `/db-migrate`, `/deploy`, `/sync-check`, `/verify-screenshots`

**MCP Servers** (`.mcp.json`): Playwright (`@anthropic-ai/mcp-server-playwright`) for web screenshots, ADB (`adb-mcp`) for Android emulator automation.

### Sub-directory CLAUDE.md Files

Path-specific context files loaded automatically when working in these directories:

| File | Scope |
|------|-------|
| `android/CLAUDE.md` | Build commands, module structure, key build config gotchas |
| `backend/CLAUDE.md` | Backend structure, 5-location model rule, AI module, service patterns |
| `backend/tests/CLAUDE.md` | Test fixture selection guide, SQLite vs PostgreSQL differences, known issues |
| `android/app/src/androidTest/CLAUDE.md` | E2E test architecture, fake auth flow, robot pattern, state sharing |

## Key Documentation

| Document | Location |
|----------|----------|
| Requirements Index | `docs/requirements/README.md` |
| Technical Design | `docs/design/RasoiAI Technical Design.md` |
| Workflow Rules | `docs/rules/Claude Code Enforced Workflow Rules.md` |
| E2E Testing Guide | `docs/testing/E2E-Testing-Prompt.md` |
| Customer Journey Suites | `docs/testing/Customer-Journey-Test-Suites.md` |
| Functional Requirements | `docs/testing/Functional-Requirement-Rule.md` |
| Meal Generation Config | `docs/design/Meal-Generation-Config-Architecture.md` |
| Session Context | `docs/CONTINUE_PROMPT.md` |
