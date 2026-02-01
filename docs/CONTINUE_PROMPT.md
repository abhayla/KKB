# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Current State: Comprehensive Home Screen E2E Tests Complete

Backend running on PostgreSQL with SQLAlchemy async ORM. Android app with Compose UI, Hilt DI, Room DB. Full E2E tests passing with real API calls.

**Latest Session (Session 38): Home Screen E2E Tests**
- Added 2 new test classes: `HomeScreenLockingTest` (8 tests), `HomeScreenActionsTest` (10 tests)
- Extended `HomeRobot` with 25+ new methods for interactions
- Added 18 new test tags in `TestTags.kt` for Home screen elements
- Fixed `tapMealCard()` to click inside card bounds (recipe items are clickable)
- Fixed `RecipeDetailRobot.goBack()` to use contentDescription
- **24 Home screen tests passing** (6 + 8 + 10)

**Previous Session (Session 37): E2E Test Reliability Phase 2**
- Added meal plan generation to test setup (fixes meal card tests)
- `BackendTestHelper.generateMealPlan()` - calls `/api/v1/meal-plans/generate` with retry
- `setUpAuthenticatedState()` now generates meal plan after auth + onboarding setup
- 30-second read timeout for AI generation (typically 4-7 seconds)

**Test Results Summary:**
| Phase | Test Suite | Tests | API Calls | Status |
|-------|------------|-------|-----------|--------|
| 1 | Backend pytest (all tests) | 170 | 0 | PASS |
| 2 | Home Screen Tests (Locking, Actions, Navigation) | 24 | 1 | PASS |
| 3 | CoreDataFlowTest (Auth→Onboarding→Generation→Home) | 1 | **1** | PASS |
| 4 | DatabaseVerificationTest (Room DB verification) | 18 | 0 | PASS |
| 5 | RecipeConstraintTest (Constraint validation) | 22 | 0 | PASS |
| **Total** | | **235** | **2** | PASS |

**Backend Status:**
- Database: PostgreSQL (asyncpg + SQLAlchemy)
- **3,580 recipes** (imported from khanakyabanega)
- 12 festivals seeded
- Auth accepts `fake-firebase-token` for testing
- **170 backend tests** (all passing)

**Android Status:**
- Compose UI with 15 screens
- Hilt DI with fake modules for testing
- Room DB for offline-first architecture
- **~400 UI tests** across all screens
- **65+ E2E flow tests** with FakeGoogleAuthClient

**Key Documentation:**
| Document | Path |
|----------|------|
| Architecture | `CLAUDE.md` |
| Meal Generation Algorithm | `docs/design/Meal-Generation-Algorithm.md` |
| Meal Generation Config | `docs/design/Meal-Generation-Config-Architecture.md` |
| E2E Test Plan | `docs/testing/E2E-Test-Plan.md` |

**To start backend:**
```bash
cd backend
.\venv\Scripts\activate          # Windows
# source venv/bin/activate       # Linux/Mac

# Ensure DATABASE_URL is set in .env file
uvicorn app.main:app --reload --port 8000
```

**To run Android E2E tests:**
```bash
# Start emulator (API 34, NOT 36)
$ANDROID_HOME/emulator/emulator -avd Pixel_6_API_34

# Backend must be running on port 8000

cd android

# All Home screen tests (24 tests)
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.HomeScreenTest,com.rasoiai.app.e2e.flows.HomeScreenLockingTest,com.rasoiai.app.e2e.flows.HomeScreenActionsTest

# Single E2E flow (1 API call - generates meal plan)
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.CoreDataFlowTest

# Database verification (no API calls - uses cached data)
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.database.DatabaseVerificationTest
```

### Session 38 Completed Work: Home Screen E2E Tests

**New Test Classes Created:**

| Test Class | Tests | Features Tested |
|------------|-------|-----------------|
| `HomeScreenLockingTest` | 8 | Day lock, meal lock, recipe lock hierarchy |
| `HomeScreenActionsTest` | 10 | Action sheets, swap, refresh, remove |

**HomeScreenLockingTest (8 tests):**
- `dayLock_locksDay_whenTapped` - Day lock button locks entire day
- `dayLock_unlocksDay_whenTappedAgain` - Day lock toggles off
- `dayLock_disablesMealLockButtons` - When day locked, meal locks disabled
- `mealLock_locksMeal_whenTapped` - Individual meal lock works
- `mealLock_unlocksMeal_whenTappedAgain` - Meal lock toggles off
- `mealLock_disabled_whenDayIsLocked` - Meal lock respects day lock
- `recipeLock_locksRecipe_viaActionSheet` - Lock via action sheet
- `recipeLock_disabled_whenMealIsLocked` - Shows "Unlock meal first"

**HomeScreenActionsTest (10 tests):**
- `recipeActionSheet_displaysAllOptions` - View, Swap, Lock, Remove
- `recipeActionSheet_viewRecipe_navigates` - Navigate to Recipe Detail
- `recipeActionSheet_swapRecipe_opensSwapSheet` - Opens swap sheet
- `swapSheet_displaysSuggestions` - Swap sheet displays
- `swapSheet_searchFilters_suggestions` - Search filters
- `swapSheet_selectRecipe_closesSheet` - Dismiss works
- `removeRecipe_closesSheet` - Remove action works
- `refreshButton_opensRefreshSheet` - Day/Week options
- `regenerateDay_triggersRegeneration` - Triggers API
- `mealItemContent_displaysContent` - Recipe name, time, calories

**Test Infrastructure Updates:**
| File | Changes |
|------|---------|
| `TestTags.kt` | +18 new test tags for Home screen elements |
| `HomeScreen.kt` | Applied testTags to day lock, refresh, action sheets |
| `HomeRobot.kt` | +25 new methods for Home screen interactions |
| `RecipeDetailRobot.kt` | Fixed `goBack()` to use contentDescription |

**Key Fixes:**
1. `tapMealCard()` - Now clicks inside card bounds where recipe items are clickable (MealSection container isn't clickable)
2. `goBack()` - Uses `onNodeWithContentDescription("Back")` instead of `onNodeWithText("Back")`
3. Recipe navigation tests - Gracefully handle missing recipes in database

**Test Infrastructure Files:**
| File | Purpose |
|------|---------|
| `e2e/E2ETestSuite.kt` | JUnit Suite for ordered test execution |
| `e2e/flows/CoreDataFlowTest.kt` | Main E2E flow (Auth→Home) - clears state first |
| `e2e/flows/HomeScreenTest.kt` | Home screen navigation (6 tests) |
| `e2e/flows/HomeScreenLockingTest.kt` | **NEW** Locking functionality (8 tests) |
| `e2e/flows/HomeScreenActionsTest.kt` | **NEW** Action sheets, swap, refresh (10 tests) |
| `e2e/flows/GroceryFlowTest.kt` | Grocery screen tests |
| `e2e/flows/CookingModeFlowTest.kt` | Recipe detail + cooking mode tests |
| `e2e/base/BaseE2ETest.kt` | Base class with REAL DataStore + meal plan generation |
| `e2e/util/BackendTestHelper.kt` | Backend API calls with retry |
| `e2e/util/RetryUtils.kt` | Retry logic for flaky operations |
| `e2e/robots/HomeRobot.kt` | Home screen robot (70+ methods) |
| `e2e/robots/RecipeDetailRobot.kt` | Recipe detail robot |
| `e2e/robots/GroceryRobot.kt` | Grocery screen robot |
| `e2e/di/FakeGoogleAuthClient.kt` | Fake auth (returns fake-firebase-token) |
| `e2e/di/FakeAuthModule.kt` | Replaces AuthModule with fake |

### Running All Tests

**Backend Tests (170 total):**
```bash
cd backend
source venv/Scripts/activate  # Windows
PYTHONPATH=. pytest tests/ -q
# 170 passed
```

**Android Unit Tests:**
```bash
cd android
./gradlew test
```

**Android UI Tests (requires emulator):**
```bash
./gradlew :app:connectedDebugAndroidTest
# ~400 tests across 15 screens
```

### Remaining Work

**High Priority:**
1. Add Grocery screen E2E tests (similar to Home screen coverage)
2. Add Favorites screen E2E tests
3. Add Settings navigation E2E tests

**Medium Priority:**
4. Implement offline mode testing with FakeNetworkMonitor
5. Performance benchmarks for meal generation
6. Edge case testing (empty meal plan, network errors)

**Future Scope:**
- CI/CD integration for instrumented tests
- Multi-device testing matrix
- Stress testing with large meal plans
- Screenshot testing for visual regression

### Key Files Reference

**Android E2E Testing:**
- Test Base: `app/src/androidTest/java/com/rasoiai/app/e2e/base/`
- Test DI: `app/src/androidTest/java/com/rasoiai/app/e2e/di/`
- Flow Tests: `app/src/androidTest/java/com/rasoiai/app/e2e/flows/`
- Robots: `app/src/androidTest/java/com/rasoiai/app/e2e/robots/`
- Utilities: `app/src/androidTest/java/com/rasoiai/app/e2e/util/`
- DB Tests: `app/src/androidTest/java/com/rasoiai/app/e2e/database/`
- Validation: `app/src/androidTest/java/com/rasoiai/app/e2e/validation/`
- Test Tags: `app/src/main/java/com/rasoiai/app/presentation/common/TestTags.kt`

**Backend Service:**
- Meal Generation: `backend/app/services/meal_generation_service.py`
- Recipe Repository: `backend/app/repositories/recipe_repository.py`

**Config Files:**
- Meal Generation: `backend/config/meal_generation.yaml`
- Dishes Reference: `backend/config/reference_data/dishes.yaml`
```

---

## IMPLEMENTATION STATUS (MVP)

| Feature | Status | Notes |
|---------|--------|-------|
| PostgreSQL migration | DONE | SQLAlchemy async ORM |
| 2-item pairing logic | DONE | Default 2 items per slot |
| INCLUDE/EXCLUDE rules | DONE | Full tracking across week |
| Allergy exclusion | DONE | Peanut, dairy, gluten variants |
| Android Compose UI | DONE | 15 screens implemented |
| Hilt DI | DONE | With test fakes |
| Room DB | DONE | Offline-first architecture |
| Navigation Compose | DONE | Full navigation graph |
| FakeAuth for testing | DONE | E2E tests use fake tokens |
| E2E Test Suite | DONE | 65+ tests passing |
| UI Tests | DONE | ~400 tests |
| Home Screen E2E | DONE | 24 tests (locking, actions, navigation) |

---

## TEST SUMMARY

### Backend Tests (170 total)

| Test File | Tests | Database | Purpose |
|-----------|-------|----------|---------|
| `test_health.py` | 2 | No | Health check endpoints |
| `test_auth.py` | 3 | No | Firebase authentication |
| `test_preference_service.py` | 26 | No | PreferenceUpdateService |
| `test_chat_integration.py` | 27 | No | Chat tool calling |
| `test_meal_generation.py` | 22 | No | Data structures |
| `test_meal_generation_integration.py` | 29 | No | Rule enforcement |
| `test_meal_generation_e2e.py` | 14 | **PostgreSQL** | Real database E2E |
| `test_chat_api.py` | 12 | No | Chat API endpoints |
| `test_recipe_cache.py` | 35 | No | Recipe cache operations |
| **TOTAL** | **170** | | All passing |

### Android E2E Tests (65+ total)

| Test Class | Tests | API Calls | Purpose |
|------------|-------|-----------|---------|
| `HomeScreenTest` | 6 | 1 | Core navigation, display |
| `HomeScreenLockingTest` | 8 | 1 | Day/meal/recipe locking |
| `HomeScreenActionsTest` | 10 | 1 | Action sheets, swap, refresh |
| `CoreDataFlowTest` | 1 | **1** | Full flow Auth→Home |
| `GroceryFlowTest` | ~6 | 0 | Grocery screen |
| `CookingModeFlowTest` | ~6 | 0 | Recipe detail + cooking |
| `DatabaseVerificationTest` | 18 | 0 | Room DB verification |
| `RecipeConstraintTest` | 22 | 0 | Constraint validation |
| **TOTAL** | **65+** | **~4** | All passing |

### Android UI Tests (~400 total)

| Screen | Tests | Notes |
|--------|-------|-------|
| Auth | 18 | Sign-in states |
| Onboarding | 41 | 5-step flow |
| Home | 22 | Meal cards, navigation |
| RecipeDetail | 26 | Ingredients, instructions |
| Grocery | 21 | List management |
| Chat | 17 | Message flow |
| Favorites | 17 | Collection management |
| Stats | 21 | Charts, achievements |
| Settings | 15 | Preferences |
| Pantry | 18 | Inventory management |
| RecipeRules | 22 | 4-tab layout |
| CookingMode | 27 | Step-by-step cooking |
| Theme | - | Light/dark mode |
| Components | - | Shared UI components |

---

## ENVIRONMENT SETUP

**Required Environment Variables** (in `backend/.env`):
```
DATABASE_URL=postgresql+asyncpg://user:password@host:5432/rasoiai
FIREBASE_CREDENTIALS_PATH=./rasoiai-firebase-service-account.json
ANTHROPIC_API_KEY=sk-ant-...
JWT_SECRET_KEY=your-secret-key
DEBUG=true
```

**Android Emulator:**
- Use API 34 (NOT API 36 - has Espresso issues)
- Recommended: Pixel_6_API_34

**Database Setup:**
```sql
CREATE DATABASE rasoiai;
CREATE USER rasoiai_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE rasoiai TO rasoiai_user;
```

**Seed Data:**
```bash
cd backend
PYTHONPATH=. python scripts/seed_festivals.py
PYTHONPATH=. python scripts/seed_achievements.py
PYTHONPATH=. python scripts/sync_config_postgres.py
PYTHONPATH=. python scripts/import_recipes_postgres.py --all
```

---

## PREVIOUS SESSIONS SUMMARY

### Sessions 1-10: Core UI Implementation
- All 13 core screens implemented
- ViewModel pattern with StateFlow
- Hilt DI, Navigation Compose setup

### Sessions 11-13: Wireframe Review & Recipe Rules
- Redesigned Home with 3-level locking
- Recipe Rules screen with 4 tabs

### Sessions 14-18: Android Backend Integration
- Auth token storage, interceptors
- DTO and Entity mappers
- Repository implementations

### Session 19: Python Backend Implementation
- FastAPI backend structure
- Firebase Admin SDK, JWT auth
- Claude AI client

### Sessions 20-25: E2E & UI Tests
- Compose UI Testing framework
- ~400 UI tests across 15 screens

### Session 26: Recipe Import
- 3,580 recipes from khanakyabanega

### Sessions 27-30: Meal Generation Config
- Config YAML files, ConfigService
- MealGenerationService with pairing
- Chat tool calling (6 tools)

### Session 31: Algorithm Design Review
- 7 Key Design Decisions approved
- Comprehensive documentation

### Session 32: Algorithm Implementation
- Variable items per meal implemented
- Generic suggestions fallback added
- 29 integration tests + 15 E2E tests

### Session 33: PostgreSQL Migration
- Migrated from Firestore to PostgreSQL
- All repositories updated for SQLAlchemy
- 3,580 recipes successfully imported

### Session 34: Backend E2E Test Fixes
- Fixed SQLAlchemy eager loading (MissingGreenlet error)
- Fixed breakfast pairing (category search by name)
- Fixed INCLUDE rule satisfaction (name preference, fallbacks)
- All 14 backend E2E tests passing

### Session 35: Android E2E Test Execution
- Executed full E2E test plan with minimal API calls
- Fixed CoreDataFlowTest timeouts and scroll issues
- Verified DatabaseVerificationTest (18 tests)
- Verified RecipeConstraintTest (22 tests)
- Total: 92 tests passing with 1 API call

### Session 37: E2E Test Reliability Phase 2
- Added `BackendTestHelper.generateMealPlan()` with 30s timeout for AI generation
- Updated `setUpAuthenticatedState()` to call meal plan generation
- Tests using `setUpAuthenticatedState()` now have meal data available
- Fixes HomeScreenTest, GroceryFlowTest, CookingModeFlowTest meal card tests

### Session 38: Home Screen E2E Tests (Current)
- Created `HomeScreenLockingTest` (8 tests for day/meal/recipe locking)
- Created `HomeScreenActionsTest` (10 tests for action sheets, swap, refresh)
- Added 18 new test tags in `TestTags.kt`
- Extended `HomeRobot` with 25+ new methods
- Fixed `tapMealCard()` to click inside card bounds
- Fixed `RecipeDetailRobot.goBack()` to use contentDescription
- **24 Home screen tests passing** (total)

---

## ARCHITECTURE DIAGRAM

```
+-------------------------------------------------------------+
|  ANDROID APP                                                 |
|  UI (Compose) -> ViewModel -> UseCase -> Repository          |
|                                            |                 |
|                              +-------------+-------------+   |
|                              |                           |   |
|                         Room (Local)              Retrofit   |
|                         (Cache)                   (Remote)   |
+-------------------------------------------------------------+
                                                      |
+-------------------------------------------------------------+
|  PYTHON BACKEND (FastAPI)                                    |
|  Endpoints -> Services -> Repositories -> PostgreSQL         |
|                                                              |
|  Database: PostgreSQL (asyncpg + SQLAlchemy async ORM)       |
|  Recipes: 3,580 (imported from khanakyabanega)               |
|  Auth: Accepts "fake-firebase-token" in debug mode           |
+-------------------------------------------------------------+

E2E TEST FLOW:
+-------------------------------------------------------------+
|  1. FakeGoogleAuthClient returns fake-firebase-token         |
|  2. AuthViewModel calls /api/v1/auth/firebase                |
|  3. Backend returns JWT, saved to DataStore                  |
|  4. User completes 5-step onboarding                         |
|  5. OnboardingViewModel saves preferences                    |
|  6. BackendTestHelper.generateMealPlan() called              |
|  7. Backend generates meal plan (4-7 seconds)                |
|  8. Navigation to Home screen                                |
|  9. Response cached to Room DB                               |
| 10. UI displays meal cards (Breakfast, Lunch, Dinner)        |
+-------------------------------------------------------------+

ANDROID TEST DI STRUCTURE:
+-------------------------------------------------------------+
|  FakeAuthModule (replaces AuthModule)                        |
|    -> FakeGoogleAuthClient (returns fake-firebase-token)     |
|                                                              |
|  Real DataStore (persists between tests)                     |
|    -> UserPreferencesDataStore (disk storage)                |
|                                                              |
|  Real modules still used:                                    |
|    -> AuthRepositoryImpl (calls real backend)                |
|    -> MealPlanRepositoryImpl (calls real backend)            |
|    -> Room Database (real local storage)                     |
+-------------------------------------------------------------+

HOME SCREEN TEST COVERAGE:
+-------------------------------------------------------------+
|  HomeScreenTest (6 tests)                                    |
|    - Week view display                                       |
|    - Meal sections display                                   |
|    - Recipe detail navigation                                |
|    - Bottom navigation                                       |
|    - Day selection                                           |
|                                                              |
|  HomeScreenLockingTest (8 tests)                             |
|    - Day lock toggle (on/off)                                |
|    - Day lock disables meal locks                            |
|    - Meal lock toggle (on/off)                               |
|    - Meal lock respects day lock                             |
|    - Recipe lock via action sheet                            |
|    - Recipe lock disabled when meal locked                   |
|                                                              |
|  HomeScreenActionsTest (10 tests)                            |
|    - Recipe action sheet (View, Swap, Lock, Remove)          |
|    - View Recipe navigation                                  |
|    - Swap recipe sheet                                       |
|    - Search filters suggestions                              |
|    - Remove recipe action                                    |
|    - Refresh button (Day/Week options)                       |
|    - Regenerate day                                          |
|    - Meal content display                                    |
+-------------------------------------------------------------+
```

---

*Last Updated: February 1, 2026*
*Session 38: Home Screen E2E Tests*
*3,580 recipes. 170 backend tests. 65+ Android E2E tests. ~400 UI tests.*
