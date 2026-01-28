# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Current Task: E2E Testing with Firestore Backend

Backend has been migrated from SQLite to Firebase Firestore. E2E tests use FakeGoogleAuthClient with real backend API calls.

**Backend Status:**
- Firestore database configured (project: `rasoiai-6dcdd`)
- Auth endpoint accepts `fake-firebase-token` for testing
- Seeded with 10 recipes + 12 festivals

**To run E2E tests:**
```bash
# 1. Start backend
cd backend
uvicorn app.main:app --reload --port 8000

# 2. Run Android tests
cd android
./gradlew :app:connectedDebugAndroidTest
```

### Session 23 Completed Work

**Test Files Created:**
| Test File | Tests | Status |
|-----------|-------|--------|
| `AuthScreenTest.kt` | 18 UI tests | ✅ All Passing |
| `AuthIntegrationTest.kt` | 9 integration tests | ✅ All Passing |
| `OnboardingScreenTest.kt` | 41 UI tests | ✅ All Passing |
| `HomeScreenTest.kt` | 22 UI tests | ✅ All Passing |
| `GroceryScreenTest.kt` | 21 UI tests | ✅ Created |
| `ChatScreenTest.kt` | 17 UI tests | ✅ Created |
| `FavoritesScreenTest.kt` | 17 UI tests | ✅ Created |
| `StatsScreenTest.kt` | 21 UI tests | ✅ Created |
| `SettingsScreenTest.kt` | 15 UI tests | ✅ Created |
| `PantryScreenTest.kt` | 18 UI tests | ✅ Created |

### Key Testing Decisions Made

1. **Framework**: Compose UI Testing (NOT Espresso)
   - Native Compose support with semantic queries
   - Espresso only for: system dialogs, intent verification

2. **Test Patterns**:
   - **UI Tests** (`*ScreenTest.kt`): Test wrapper composable with mock UiState, no ViewModel
   - **Integration Tests** (`*IntegrationTest.kt`): Full app with Hilt DI + FakeGoogleAuthClient

3. **Screen Content Composables**: Made `internal` for testing:
   - `GroceryScreenContent`, `ChatScreenContent`, `FavoritesScreenContent`
   - `StatsScreenContent`, `SettingsScreenContent`, `PantryScreenContent`
   - `RecipeRulesScreenContent`, `CookingModeContent`

4. **API Compatibility**: Use API 34 emulator (API 36 has Espresso issues)

### Test Coverage Status

| Phase | Screen | UI Tests | Status |
|-------|--------|----------|--------|
| 1 | Auth | 18 ✅ | **DONE** |
| 2 | Onboarding | 41 ✅ | **DONE** |
| 3 | Generation | - | ❌ TODO |
| 4 | Home | 22 ✅ | **DONE** |
| 5 | Grocery | 21 ✅ | **DONE** |
| 6 | Chat | 17 ✅ | **DONE** |
| 7 | Favorites | 17 ✅ | **DONE** |
| 8 | Stats | 21 ✅ | **DONE** |
| 9 | Settings | 15 ✅ | **DONE** |
| 10 | Pantry | 18 ✅ | **DONE** |
| 11 | Recipe Rules | - | ❌ TODO |
| 12 | Cooking Mode | - | ❌ TODO |
| 13 | Offline | - | ❌ TODO |
| 14 | Edge Cases | - | ❌ TODO |
| 15 | Performance | - | ❌ TODO |

**Total: ~190 UI tests created**

### Remaining Work

1. **RecipeRulesScreenTest.kt** - Phase 11 (Recipe Rules screen with 4 tabs)
2. **CookingModeScreenTest.kt** - Phase 12 (Cooking Mode with timer)
3. **RecipeDetailScreenTest.kt** - Phase 4/12 (Recipe details and scaling)
4. Integration tests for navigation flows

### Running Tests

```bash
cd android

# Run all UI tests
./gradlew :app:connectedDebugAndroidTest

# Run specific screen test
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.presentation.grocery.GroceryScreenTest

# Run all presentation tests
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.rasoiai.app.presentation
```

### Test File Template

```kotlin
class {Screen}ScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // Test data factory
    private fun createTestUiState(...): {Screen}UiState { ... }

    // Tests grouped by category
    @Test fun screen_displaysElement_whenCondition() { ... }
    @Test fun screen_action_triggersCallback() { ... }
}

// Test wrapper composable (mirrors actual screen)
@Composable
private fun {Screen}TestContent(
    uiState: {Screen}UiState,
    onAction: () -> Unit = {},
) { /* Mirror screen structure */ }
```

Continue by running all tests and creating tests for Recipe Rules and Cooking Mode screens.
```

---

## TEST FILES CREATED (Sessions 21-23)

```
android/app/src/androidTest/java/com/rasoiai/app/presentation/
├── auth/
│   ├── AuthScreenTest.kt           # 18 UI tests ✅
│   └── AuthIntegrationTest.kt      # 9 integration tests ✅
├── chat/
│   └── ChatScreenTest.kt           # 17 UI tests ✅
├── favorites/
│   └── FavoritesScreenTest.kt      # 17 UI tests ✅
├── grocery/
│   └── GroceryScreenTest.kt        # 21 UI tests ✅
├── home/
│   └── HomeScreenTest.kt           # 22 UI tests ✅
├── onboarding/
│   └── OnboardingScreenTest.kt     # 41 UI tests ✅
├── pantry/
│   └── PantryScreenTest.kt         # 18 UI tests ✅
├── settings/
│   └── SettingsScreenTest.kt       # 15 UI tests ✅
└── stats/
    └── StatsScreenTest.kt          # 21 UI tests ✅
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
- Split wireframes into 16 files

### Sessions 14-18: Android Backend Integration
- Auth token storage in DataStore
- AuthInterceptor for API requests
- DTO and Entity mappers
- AuthRepositoryImpl, MealPlanRepositoryImpl, RecipeRepositoryImpl, GroceryRepositoryImpl
- Firebase auth flow verified

### Session 19: Python Backend Implementation
- Created complete FastAPI backend structure
- 17 SQLAlchemy models (SQLite compatible) → *Migrated to Firestore in Session 24*
- 18 API endpoints matching Android DTOs
- Firebase Admin SDK integration
- JWT authentication
- Claude AI client for meal planning and chat
- Seed scripts: 17 recipes, 23 festivals

### Session 20: E2E Espresso Test Framework (Initial)
- Robot pattern framework created
- 14 flow test classes (phases 1-14)
- Test DI modules

### Session 21: Compose UI Tests - HomeScreenTest
- Pivoted from Espresso to Compose UI Testing
- Created HomeScreenTest.kt with 22 passing tests
- Established test wrapper composable pattern

### Session 22: Auth Tests + Onboarding Tests
- **AuthScreenTest.kt**: 18 UI tests (all passing)
- **AuthIntegrationTest.kt**: 9 integration tests with FakeGoogleAuthClient (all passing)
- **OnboardingScreenTest.kt**: 40+ tests created

### Session 23: Bulk Screen Tests
- Fixed OnboardingScreenTest (41 tests passing)
- Created tests for 6 additional screens:
  - GroceryScreenTest.kt (21 tests)
  - ChatScreenTest.kt (17 tests)
  - FavoritesScreenTest.kt (17 tests)
  - StatsScreenTest.kt (21 tests)
  - SettingsScreenTest.kt (15 tests)
  - PantryScreenTest.kt (18 tests)
- Made screen content composables `internal` for testing

### Session 24: Backend Migration to Firestore
- **Replaced SQLite/SQLAlchemy with Firebase Firestore**
- Created Firestore repositories:
  - `app/repositories/user_repository.py`
  - `app/repositories/recipe_repository.py`
  - `app/repositories/meal_plan_repository.py`
  - `app/repositories/festival_repository.py`
- Updated `app/db/firestore.py` - Firestore client utilities
- Updated `app/services/auth_service.py` - Uses Firestore UserRepository
- Updated `app/core/firebase.py` - Accepts `fake-firebase-token` for E2E testing
- Created `scripts/seed_firestore.py` - Seeds 10 recipes + 12 festivals
- Verified auth flow: `fake-firebase-token` → Backend → JWT returned
- Updated E2E-Testing-Prompt.md with Firestore architecture

---

## ARCHITECTURE REMINDER

```
┌─────────────────────────────────────────────────────────────┐
│  ANDROID APP                                                │
│  UI (Compose) → ViewModel → UseCase → Repository            │
│                                           ↓                 │
│                              ┌────────────┴────────────┐    │
│                              ↓                         ↓    │
│                         Room (Local)            Retrofit    │
│                         (Cache)                 (Remote)    │
└─────────────────────────────────────────────────────────────┘
                                                      ↓
┌─────────────────────────────────────────────────────────────┐
│  PYTHON BACKEND (FastAPI)                                   │
│  Endpoints → Services → Repositories → Firestore            │
│                                                             │
│  Auth: Accepts "fake-firebase-token" in debug mode          │
│  Database: Firebase Firestore (project: rasoiai-6dcdd)      │
└─────────────────────────────────────────────────────────────┘

TEST LAYERS:
┌─────────────────────────────────────────────────────────────┐
│  UI Tests (*ScreenTest.kt)                                  │
│  - Test wrapper composable with mock UiState                │
│  - No ViewModel, no Hilt                                    │
│  - Fast, isolated, reliable                                 │
├─────────────────────────────────────────────────────────────┤
│  Integration Tests (*IntegrationTest.kt)                    │
│  - Full app with Hilt DI + FakeGoogleAuthClient             │
│  - Real backend API calls (Firestore)                       │
│  - Tests navigation flows end-to-end                        │
│  - Requires: emulator + backend running                     │
└─────────────────────────────────────────────────────────────┘
```

---

*Last Updated: January 28, 2026*
*Backend migrated to Firestore. ~190 UI tests across 10 screens. E2E tests ready with FakeGoogleAuthClient + real Firestore backend.*
