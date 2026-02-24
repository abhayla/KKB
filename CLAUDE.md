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

**Context compaction:** When compacting, always preserve: the 5-location model update rule, test fixture choices (client vs unauthenticated_client vs authenticated_client), all file paths that were modified, any test commands that were run, the current workflow step number, and any GitHub Issue numbers being worked on.

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
| Auth | Firebase Auth (Google OAuth only) |
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

### Navigation Routes

Routes with arguments use `createRoute()` helper in `presentation/navigation/Screen.kt`:

| Group | Screens |
|-------|---------|
| Auth flow | Splash, Auth, Onboarding |
| Main (bottom nav) | Home, Grocery, Chat (`?context`), Favorites, Stats |
| Main (other) | Settings, Notifications |
| Detail | RecipeDetail (`{recipeId}`), CookingMode (`{recipeId}`) |
| Feature | Pantry, RecipeRules, Achievements |
| Settings sub-screens | DietaryRestrictions, DislikedIngredients, CuisinePreferences, SpiceLevelSettings, CookingTimeSettings, FamilyMembersSettings, NotificationSettings, UnitsSettings, EditProfile, FriendsLeaderboard, ConnectedAccounts |

### Bottom Navigation

Use `RasoiBottomNavigation` from `home/components/` in `Scaffold(bottomBar = ...)`. Items: HOME, GROCERY, CHAT, FAVORITES, STATS.

## Development Commands

### Android (run from `android/`)

```bash
# Build & Test
./gradlew build                  # Full build with tests
./gradlew assembleDebug          # Quick compilation (no tests)
./gradlew test                   # All unit tests
./gradlew :app:test              # App module tests only

# Single test
./gradlew test --tests "com.rasoiai.app.ClassName"
./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest"

# UI Tests (requires emulator API 34, NOT 36)
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.presentation.auth.AuthScreenTest

# Lint & Install
./gradlew lint
./gradlew installDebug

# Clean build (for strange build issues)
./gradlew clean && ./gradlew assembleDebug
```

### Backend (run from `backend/`)

```bash
# Setup
python -m venv venv
source venv/bin/activate         # Linux/Mac/Git Bash
# .\venv\Scripts\activate        # Windows PowerShell
pip install -r requirements.txt

# Start server
uvicorn app.main:app --reload    # → http://localhost:8000/docs

# Testing
PYTHONPATH=. pytest              # All tests
PYTHONPATH=. pytest --cov=app    # With coverage
PYTHONPATH=. pytest tests/test_auth.py -v                    # Single file
PYTHONPATH=. pytest tests/test_preference_service.py::test_add_include_rule -v  # Single test

# Database
alembic upgrade head             # Run migrations
alembic revision --autogenerate -m "description"  # New migration
PYTHONPATH=. python scripts/seed_festivals.py
PYTHONPATH=. python scripts/seed_achievements.py
PYTHONPATH=. python scripts/import_recipes_postgres.py
PYTHONPATH=. python scripts/sync_config_postgres.py
PYTHONPATH=. python scripts/backfill_ai_recipe_catalog.py  # Backfill catalog from historical meal plans
PYTHONPATH=. python scripts/cleanup_user.py               # Remove test user data (E2E test isolation)
PYTHONPATH=. python scripts/migrate_legacy_rules.py       # Migrate old preferences to recipe rules
```

### Performance Testing (run from `backend/`)

```bash
bash tests/performance/run_perf_tests.sh smoke     # 1 user, 60s (~$0.002)
bash tests/performance/run_perf_tests.sh crud      # 50 users, 5m, no AI ($0)
bash tests/performance/run_perf_tests.sh load      # 20 users, 10m (~$0.03)
bash tests/performance/run_perf_tests.sh web       # Interactive Locust UI at :8089
```

Three user classes in `tests/performance/locustfile.py`: `RasoiAIUser` (realistic mix), `MealGenHeavyUser` (AI-only), `CRUDOnlyUser` (no AI). Reports go to `tests/performance/reports/` (gitignored).

### Prerequisites

| Tool | Version |
|------|---------|
| JDK | 17+ (`JAVA_HOME` must be set) |
| Python | 3.11+ |
| PostgreSQL | 12+ |
| Android SDK | API 34 (Min 24) |

**Dependency versions:** See `backend/requirements.txt` and `android/gradle/libs.versions.toml`.

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
WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
```
The build will fail with `GradleException` if `WEB_CLIENT_ID` is missing. The build also checks `System.getenv("WEB_CLIENT_ID")` as fallback, enabling CI builds without `local.properties`. Also requires `google-services.json` in `android/app/` (from Firebase Console).

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

## Testing

### Test Distribution

| Platform | Tests (approx.) | Framework |
|----------|-----------------|-----------|
| Backend | ~538 | pytest |
| Android Unit | ~580 | JUnit + MockK |
| Android UI | ~750+ | Compose UI Testing |
| Android E2E | ~67+ | Compose UI Testing + Hilt + Real API |

*Counts grow frequently. Run `PYTHONPATH=. pytest --collect-only -q` (backend) or `./gradlew test` (Android) for current totals.*

### Backend Tests

All in `backend/tests/`, named `test_{feature}.py` (43 test files). Run `PYTHONPATH=. pytest --collect-only` to list all. Tests use SQLite in-memory via conftest fixtures (see Backend Test Fixtures below). Some files use class-based test organization (e.g., `test_ai_meal_service.py`, `test_chat_api.py`, `test_preference_service.py`).

### Android UI Tests

Tests use **Compose UI Testing** (not Espresso). Located in `app/src/androidTest/`. Uses custom `com.rasoiai.app.HiltTestRunner`.

| Test Type | Pattern | Purpose |
|-----------|---------|---------|
| UI Tests | `*ScreenTest.kt` | Test composable with mock UiState |
| Integration Tests | `*IntegrationTest.kt` | Full app with Hilt DI |
| E2E Tests | `*FlowTest.kt` | Complete user flows |

### ViewModel Unit Tests

Located in `app/src/test/java/com/rasoiai/app/presentation/`. Use `TestDispatcherRule` (replaces Dispatchers.Main) and `Fake*Repository` classes (in-memory implementations).

### E2E Test Infrastructure

**Key files** in `app/src/androidTest/java/com/rasoiai/app/e2e/`:

| File | Purpose |
|------|---------|
| `base/BaseE2ETest.kt` | Base class with Hilt setup, meal plan generation |
| `base/ComposeTestExtensions.kt` | Compose test extension functions |
| `base/TestDataFactory.kt` | Test data creation helpers |
| `util/BackendTestHelper.kt` | Backend API calls with retry |
| `di/FakeGoogleAuthClient.kt` | Fake auth (returns `fake-firebase-token`) |
| `robots/` | Robot pattern classes (HomeRobot, GroceryRobot, etc.) |
| `rules/RetryRule.kt` | JUnit rule for retrying flaky tests |
| `E2ETestSuite.kt` | Test suite runner (runs CoreDataFlowTest first) |
| `presentation/common/TestTags.kt` | All semantic test tags |

**E2E backend URL:** Tests use `http://10.0.2.2:8000` (Android emulator maps `10.0.2.2` → host `localhost`).

**E2E debug auth bypass:** `FakeGoogleAuthClient` sends `"fake-firebase-token"` to the backend (`DEBUG=true` required). See `android/app/src/androidTest/CLAUDE.md` for full auth flow, execution order, and state setup details.

### Backend Test Fixtures

Defined in `backend/tests/conftest.py`. Key fixtures: `client` (most tests, pre-authenticated), `unauthenticated_client` (testing 401s), `authenticated_client` (JWT verification), `db_session` (direct service tests). When adding new models, import them in `conftest.py` so SQLite creates tables. See `backend/tests/CLAUDE.md` for full fixture guide.

## Domain Models

Located in `domain/src/main/java/com/rasoiai/domain/model/`:

| Model | Key Fields |
|-------|-----------|
| `Recipe` | id, name, cuisineType, dietaryTags, prepTimeMinutes, ingredients, instructions |
| `MealPlan` | id, weekStartDate, weekEndDate, days |
| `MealPlanDay` | date, breakfast, lunch, dinner, snacks, festival |
| `RecipeRule` | id, type, action, targetId, frequency, enforcement, mealSlot |
| `NutritionGoal` | id, foodCategory (FoodCategory enum), weeklyTarget, currentProgress, enforcement (RuleEnforcement), isActive |
| `FamilyMember` | id, name, type (MemberType enum), age, specialNeeds (List\<SpecialDietaryNeed\>) |
| `AiRecipeCatalog` | id, display_name, normalized_name, dietary_tags, cuisine, usage_count |

**Key Enums:**
- `DietaryTag` (Recipe): VEGETARIAN, NON_VEGETARIAN, VEGAN, JAIN, SATTVIC, HALAL, EGGETARIAN
- `PrimaryDiet` (User): VEGETARIAN, EGGETARIAN, NON_VEGETARIAN
- `CuisineType`: NORTH, SOUTH, EAST, WEST
- `MealType`: BREAKFAST, LUNCH, SNACKS, DINNER
- `RuleAction`: INCLUDE, EXCLUDE
- `RuleEnforcement`: REQUIRED, PREFERRED
- `MemberType`: ADULT, CHILD, SENIOR
- `SpecialDietaryNeed`: DIABETIC, LOW_OIL, NO_SPICY, SOFT_FOOD, LOW_SALT, HIGH_PROTEIN, LOW_CARB
- `FoodCategory`: GREEN_LEAFY, CITRUS_VITAMIN_C, IRON_RICH, HIGH_PROTEIN, CALCIUM_RICH, FIBER_RICH, OMEGA_3, ANTIOXIDANT

**Room-only entities** (no domain model counterpart):
`KnownIngredientEntity`, `OfflineQueueEntity`, `CookedRecipeEntity`, `RecentlyViewedEntity`

## Backend API

12 router files in `app/api/v1/endpoints/`: auth, chat, family_members, festivals, grocery, meal_plans, notifications, photos, recipe_rules, recipes, stats, users. Note: nutrition_goals endpoints are in `recipe_rules.py` (separate router, registered separately in `router.py` — 13 routers total). Run `PYTHONPATH=. pytest --collect-only -q` or visit `http://localhost:8000/docs` for current counts (~44 endpoints).

**Full interactive docs:** `http://localhost:8000/docs` (Swagger UI, only available when `DEBUG=true`)

**Key backend files with gotchas:**

| File | Why it matters |
|------|----------------|
| `app/db/postgres.py` | Has 3 model import blocks (init_db, create_tables, drop_tables) — must update all 3 when adding models |
| `app/db/database.py` | `get_db()` dependency — imports from postgres.py |
| `app/config.py` | Pydantic Settings — env vars, CORS (default `[]`), JWT secret (no default — required), sql_echo, usage limits, Sentry DSN |
| `app/ai/chat_assistant.py` | Tool calling orchestration (`MAX_TOOL_ITERATIONS=5`) — ties Claude API to preference/rule services |
| `app/ai/claude_client.py` | Anthropic Claude client wrapper |
| `app/ai/gemini_client.py` | Google Gemini client wrapper (google-genai SDK). `MODEL_NAME = "gemini-2.5-flash"` — change it here only |
| `app/ai/prompts/` | Prompt templates (`chat_prompt.py`, `meal_plan_prompt.py`) |
| `app/ai/tools/` | Chat tool definitions (`preference_tools.py`). `ALL_CHAT_TOOLS` — add new tools here AND in `chat_assistant.py` |
| `app/cache/recipe_cache.py` | In-memory recipe cache, warmed on startup via `warm_recipe_cache()` (non-fatal) |
| `app/repositories/` | Data access layer (one per model); called by services, wraps SQLAlchemy queries |
| `app/services/` | 20 service files, one per domain area; all follow same async pattern with `db: AsyncSession` param |
| `app/main.py` | SecurityHeadersMiddleware (X-Content-Type-Options, X-Frame-Options, HSTS, X-API-Version), rate limiting (slowapi) |

**Router gotchas:**
- `recipe_rules.py` defines **two routers**: one for recipe rules, one for nutrition goals. Don't create a separate `nutrition_goals.py`.
- `family_members.py` is registered under `/users` prefix — full path is `/api/v1/users/family-members/`.

**Service patterns:**
- Services take `db: AsyncSession` as parameter (DI from endpoint). They do NOT create their own sessions (except `auth_service.py` which uses `async_session_maker` directly for token rotation/logout — must be patched in test fixtures).
- `family_constraints.py` is a shared module imported by BOTH `ai_meal_service.py` AND the `recipe_rules.py` endpoint — changes affect meal generation AND rule validation simultaneously.
- `ai_meal_service.py` defines a local `UserPreferences` dataclass that shadows `app.models.user.UserPreferences` — don't confuse them.
- Chat context is limited to last 6 messages via `ChatRepository.get_context_for_claude(limit=6)`.

**AI SDK note:** Uses `google-genai` SDK (NOT old `google-generativeai`) with native async `client.aio`. Do NOT revert — the old SDK blocked uvicorn's event loop.

**Startup sequence:** `main.py` lifespan handler: Sentry init (`send_default_pii=False`) → `init_db()` → `warm_recipe_cache()` (non-fatal) → app ready.

**Rate limiting:** Uses `slowapi` with per-endpoint decorators. Endpoints accepting rate limiting must have `request: Request` as first parameter. Key limits: auth 10/min, chat 30/min, meal generation 5/hr, photo analysis 10/hr.

**Security headers:** `SecurityHeadersMiddleware` adds `X-Content-Type-Options`, `X-Frame-Options`, `X-XSS-Protection`, `Referrer-Policy`, `X-API-Version`, and `Strict-Transport-Security` (non-debug only).

**SQLAlchemy async rules:**
- Use `selectinload()` for eager loading — `joinedload` and lazy loading raise `MissingGreenlet`.
- `expire_on_commit=False` is set in session maker — required for async; without it, post-commit attribute access fails.

## Database Schema

### Room (Android) — Version 11

20 entities in `RasoiDatabase.kt`. Migrations: `MIGRATION_7_8` (notifications + offline queue), `MIGRATION_8_9` (recipe rules refactor + cooked recipes), `MIGRATION_9_10` (known ingredients), `MIGRATION_10_11` (recreate `meal_plan_items` with proper `id` PK instead of composite PK). Fresh installs seed `known_ingredients` with 40+ popular Indian cooking ingredients.

11 DAOs: MealPlan, Recipe, Grocery, Favorite, Collection, Pantry, Stats, RecipeRules, Chat, Notification, OfflineQueue.

### PostgreSQL (Backend) — Alembic Migrations

8 migrations in `backend/alembic/versions/` (initial schema → meal generation settings → notifications/FCM → recipe rules → AI recipe catalog → recipe rules dedup → email uniqueness → pre-prod hardening). Run `alembic upgrade head` to apply.

13 model files in `backend/app/models/` (plus `__init__.py` re-exporting all models). Note: `FamilyMember` is defined in `user.py`, not a separate file; `AiRecipeCatalog` in `ai_recipe_catalog.py`; `NutritionGoal` in `recipe_rule.py`; `UsageLog` in `usage_log.py`; `RefreshToken` in `refresh_token.py`. All 3 `postgres.py` import blocks, `conftest.py`, and `models/__init__.py` must import all models. When adding new models, update all 5 locations.

## Meal Generation

AI-powered meal planning using Google Gemini (`gemini-2.5-flash` via `google-genai` SDK, native async with `client.aio.models.generate_content()`), with YAML config for pairing guidance.

**Config files** in `backend/config/`:
- `meal_generation.yaml` - Pairing rules, meal structure
- `reference_data/ingredients.yaml` - Ingredient aliases
- `reference_data/dishes.yaml` - Common dishes with pairings
- `reference_data/cuisines.yaml` - Regional cuisine definitions

**Key concepts:**
| Concept | Description |
|---------|-------------|
| Items per slot | 2 minimum (e.g., Dal + Rice) |
| INCLUDE rule | Forces item into meal slot, paired with complementary item |
| EXCLUDE rule | Replaces only excluded item, keeps its pair |

**Chat Tool Calling:**
| Tool | Description |
|------|-------------|
| `update_recipe_rule` | ADD/REMOVE INCLUDE/EXCLUDE rules |
| `update_allergy` | Manage food allergies |
| `update_dislike` | Manage disliked ingredients |
| `update_preference` | Cooking time, dietary tags, cuisine |

## Design System

| Element | Light | Dark |
|---------|-------|------|
| Primary | `#FF6838` (Orange) | `#FFB59C` |
| Secondary | `#5A822B` (Green) | `#A8D475` |
| Background | `#FDFAF4` (Cream) | `#1C1B1F` |

| Token | Value |
|-------|-------|
| Spacing | 8dp grid (4, 8, 16, 24, 32, 48dp) |
| Shapes | Rounded corners (8dp small, 16dp medium, 24dp large) |

## India-Specific Domain Knowledge

| Aspect | Details |
|--------|---------|
| **Dietary Tags** | JAIN (no root vegetables), SATTVIC (no onion/garlic) |
| **Cuisine Zones** | NORTH, SOUTH, EAST, WEST with distinct ingredients |
| **Measurements** | Support metric and traditional: katori (bowl), chammach (spoon) |
| **Family Size** | 3-8 members, multi-generational support |

## Reference Implementations

| Pattern | Reference | Key Features |
|---------|-----------|--------------|
| Tabs + Bottom Sheets | `presentation/reciperules/` | 2-tab layout (Rules, Nutrition), modal sheets |
| Form-based Settings | `presentation/settings/` | Sections, toggles |
| Bottom Navigation | `presentation/home/` | RasoiBottomNavigation |
| List with Filtering | `presentation/favorites/` | Tab/list pattern |

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
| `post-screenshot-resize.sh` | PostToolUse (Bash/Playwright) | Auto-resize screenshots >1800px |
| `post-screenshot-validate.sh` | PostToolUse (Bash/Playwright) | Records screenshot metadata; validates file exists and non-zero; updates `screenshotsCaptured[]` in workflow state |
| `auto-fix-pattern-scan.sh` | PostToolUse | Scans for common fix patterns after tool use |
| `auto-format.sh` | PostToolUse | Auto-formats code after edits |
| `post-skill-learning.sh` | PostToolUse (Skill) | Records skill outcomes for learning system |

Workflow state is tracked in `.claude/workflow-state.json` (extended schema with `testFailuresPending`, `fixLoopInvestigating`, `visualIssuesPending`, `screenshotsCaptured`, `backendChecks`, `skillInvocations`, `evidence`, `agentDelegations`). The full hook system and enforcement logic is documented in `docs/rules/Claude Code Enforced Workflow Rules.md`.

## Claude Code Configuration

The `.claude/` directory contains Claude Code customization:

| Directory | Contents |
|-----------|----------|
| `agents/` | 9 agent definitions: api-tester, code-reviewer, database-admin, debugger, docs-manager, git-manager, performance-profiler, planner-researcher, tester |
| `knowledge.db` | SQLite pattern library — known errors, fix strategies, test mappings (used by auto-verify) |
| `skills/` | 12 slash commands (see table below) |
| `hooks/` | 13 workflow enforcement hooks + `resize_screenshot.py` (see Workflow Enforcement Hooks above) |
| `rules/` | 5 path-scoped rule files: android.md, backend.md, compose-ui.md, database.md, testing.md |
| `logs/` | Workflow session logs |

**Skills (slash commands):**

| Skill | Purpose |
|-------|---------|
| `/adb-test` | Manual E2E via ADB (12 screens + 21 flows) |
| `/auto-verify` | Post-change verification with KB-driven diagnosis |
| `/fix-issue <N>` | Implement fix for GitHub Issue |
| `/fix-loop` | Iterative fix cycle (analyze → fix → review → retest) |
| `/implement` | Implement feature with 7-step workflow |
| `/post-fix-pipeline` | Post-fix verification (regression → test suite → docs → commit) |
| `/reflect` | Learning system analysis & self-modification |
| `/run-e2e` | Run Android E2E tests by feature group |
| `/db-migrate` | Database migration management |
| `/deploy` | Deployment workflow |
| `/sync-check` | Verify offline/online sync consistency |
| `/verify-screenshots` | Deep screenshot + backend verification |

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
| **Requirements Index** | `docs/requirements/README.md` |
| Screen Requirements | `docs/requirements/screens/` (12 files) |
| API Requirements | `docs/requirements/api/backend-api.md` |
| Technical Design | `docs/design/RasoiAI Technical Design.md` |
| Android Architecture Decisions | `docs/design/Android Architecture Decisions.md` |
| Data Flow Diagram | `docs/design/Data-Flow-Diagram.md` |
| Meal Generation Algorithm | `docs/design/Meal-Generation-Algorithm.md` |
| Meal Generation Config | `docs/design/Meal-Generation-Config-Architecture.md` |
| Design System | `docs/design/RasoiAI Design System.md` |
| **Workflow Rules** | `docs/rules/Claude Code Enforced Workflow Rules.md` |
| E2E Testing Guide | `docs/testing/E2E-Testing-Prompt.md` |
| E2E Phase Details | `docs/testing/E2E-Phase-Details.md` |
| E2E Test Plan | `docs/testing/E2E-Test-Plan.md` |
| Functional Requirement Traceability | `docs/testing/Functional-Requirement-Rule.md` |
| Recipe Rule Test Plan | `docs/testing/Recipe-Rule-Test-Plan.md` |
| ADB Test Definitions | `docs/testing/adb-test-definitions.md` |
| ADB Flow Definitions | `docs/testing/flows/` (21 flow files) |
| Meal Plan Validator | `scripts/validate_meal_plan.py` |
| Session Context | `docs/CONTINUE_PROMPT.md` |
