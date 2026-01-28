# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**RasoiAI** (रसोई AI) is an AI-powered meal planning application for Indian families. It generates personalized weekly meal plans based on family preferences, dietary restrictions, regional cuisines, and cultural considerations including festivals and fasting days.

| Attribute | Details |
|-----------|---------|
| **Platform** | Android Native (Kotlin 1.9.22 + Jetpack Compose BOM 2024.02) |
| **Backend** | Python (FastAPI) |
| **Target SDK** | 34 (Min SDK 24 / Android 7.0) |
| **Target Market** | Pan-India (Tier 1, 2, 3 cities) |

## Current Project State

See `docs/CONTINUE_PROMPT.md` for session context and active work.

**Test Coverage (last verified: Jan 28, 2026):**
- ~265 UI tests across 13 screens (Compose UI Testing)
- All major screens have UI tests: Auth, Onboarding, Home, RecipeDetail, Grocery, Chat, Favorites, Stats, Settings, Pantry, RecipeRules, CookingMode
- Remaining: GenerationScreen, integration tests, offline/edge case tests

## Quick Start

```bash
cd android
./gradlew build          # Build
./gradlew test           # Run tests
./gradlew installDebug   # Install on device/emulator
```

**Prerequisites:** Android Studio, JDK 17+, Android SDK 34

**Firebase Setup:** Add `google-services.json` to `android/app/` from Firebase Console, or create a placeholder for build-only testing:
```bash
echo '{"project_info":{"project_number":"000000000000","project_id":"rasoiai-debug","storage_bucket":"rasoiai-debug.appspot.com"},"client":[{"client_info":{"mobilesdk_app_id":"1:000000000000:android:0000000000000000000000","android_client_info":{"package_name":"com.rasoiai.app.debug"}},"oauth_client":[],"api_key":[{"current_key":"fake-api-key-for-ci"}],"services":{}}],"configuration_version":"1"}' > app/google-services.json
```

## Key Architecture Decisions

| Area | Decision |
|------|----------|
| Dependency Injection | Hilt |
| Annotation Processing | KSP |
| State Management | StateFlow + Single UiState Data Class |
| Navigation | Navigation Compose |
| Build Configuration | Kotlin DSL + Version Catalog (TOML) |
| Minimum SDK | API 24 (Android 7.0) |
| Testing Strategy | 70% Unit / 20% Integration / 10% UI |
| Modularization | By-Layer (app, core, data, domain) → Hybrid later |

## ViewModel Pattern

All ViewModels follow this structure:

```kotlin
// 1. UiState data class - all screen state in one place
data class FeatureUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    // ... computed properties via get() for derived state
)

// 2. NavigationEvent sealed class - one-time navigation events
sealed class FeatureNavigationEvent {
    data class NavigateToDetail(val id: String) : FeatureNavigationEvent()
    data object NavigateToSettings : FeatureNavigationEvent()
}

// 3. ViewModel with Hilt injection
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {
    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

    // Use Channel for one-time navigation events (prevents replay on config change)
    private val _navigationEvent = Channel<FeatureNavigationEvent>()
    val navigationEvent: Flow<FeatureNavigationEvent> = _navigationEvent.receiveAsFlow()

    // Update state with copy()
    fun doSomething() {
        _uiState.update { it.copy(isLoading = true) }
    }

    // Send navigation event (use trySend for non-suspend context)
    fun navigateToDetail(id: String) {
        _navigationEvent.trySend(FeatureNavigationEvent.NavigateToDetail(id))
    }
}
```

## Repository Pattern (Offline-First)

Real repository implementations follow this architecture:

```kotlin
@Singleton
class FeatureRepositoryImpl @Inject constructor(
    private val apiService: RasoiApiService,
    private val featureDao: FeatureDao,
    private val networkMonitor: NetworkMonitor
) : FeatureRepository {

    // Always return from local DB (single source of truth)
    override fun getData(): Flow<Data?> {
        return featureDao.getData().map { entity ->
            if (entity != null) {
                entity.toDomain()
            } else {
                // Fetch from API if online
                if (networkMonitor.isOnline.first()) {
                    fetchAndCache()
                }
                null
            }
        }
    }

    // Mutations: update local immediately, sync to server when online
    override suspend fun updateData(data: Data): Result<Unit> {
        featureDao.update(data.toEntity())
        if (networkMonitor.isOnline.first()) {
            apiService.update(data.toDto())
        } else {
            featureDao.markUnsynced(data.id)
        }
        return Result.success(Unit)
    }
}
```

**Data flow:**
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

## Navigation Routes

Routes with arguments use `createRoute()` helper pattern:

```kotlin
// In Screen.kt
data object RecipeDetail : Screen("recipe/{recipeId}?isLocked={isLocked}&fromMealPlan={fromMealPlan}") {
    fun createRoute(recipeId: String, isLocked: Boolean = false, fromMealPlan: Boolean = false) =
        "recipe/$recipeId?isLocked=$isLocked&fromMealPlan=$fromMealPlan"
    const val ARG_RECIPE_ID = "recipeId"
    const val ARG_IS_LOCKED = "isLocked"
    const val ARG_FROM_MEAL_PLAN = "fromMealPlan"
}

// Usage in navigation
navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
navController.navigate(Screen.RecipeDetail.createRoute(recipeId, isLocked = true, fromMealPlan = true))
```

## Bottom Navigation Integration

Screens with bottom navigation use `RasoiBottomNavigation` component from `home/components/`:

```kotlin
// In any bottom nav screen composable:
Scaffold(
    bottomBar = {
        RasoiBottomNavigation(
            selectedItem = NavigationItem.GROCERY,  // Current screen
            onItemSelected = { item ->
                when (item) {
                    NavigationItem.HOME -> onNavigateToHome()
                    NavigationItem.GROCERY -> { /* Already here */ }
                    NavigationItem.CHAT -> onNavigateToChat()
                    NavigationItem.FAVORITES -> onNavigateToFavorites()
                    NavigationItem.STATS -> onNavigateToStats()
                }
            }
        )
    }
) { paddingValues -> /* Screen content */ }
```

**Bottom nav items:** HOME, GROCERY, CHAT, FAVORITES, STATS

## Key Domain Models

Located in `domain/src/main/java/com/rasoiai/domain/model/`:

| Model | Key Fields |
|-------|-----------|
| `Recipe` | id, name, cuisineType, dietaryTags, prepTimeMinutes, cookTimeMinutes, ingredients, instructions, nutrition |
| `Ingredient` | id, name, quantity, unit, category (IngredientCategory), isOptional |
| `Instruction` | stepNumber, instruction, durationMinutes, timerRequired, tips |
| `MealPlan` | id, weekStartDate, weekEndDate, days |
| `MealPlanDay` | date, breakfast, lunch, dinner, snacks, festival |
| `MealItem` | recipeId, recipeName, isLocked, servings |
| `Nutrition` | calories, proteinGrams, carbohydratesGrams, fatGrams, fiberGrams |
| `RecipeRule` | id, type, action, targetId, targetName, frequency, enforcement, mealSlot, isActive |
| `NutritionGoal` | id, foodCategory, weeklyTarget, currentProgress, isActive |

**Key Enums in Recipe.kt:**
- `IngredientCategory`: VEGETABLES, FRUITS, DAIRY, GRAINS, PULSES, SPICES, OILS, MEAT, SEAFOOD, NUTS, SWEETENERS, OTHER
- `DietaryTag`: VEGETARIAN, NON_VEGETARIAN, VEGAN, JAIN, SATTVIC, HALAL, EGGETARIAN
- `CuisineType`: NORTH, SOUTH, EAST, WEST
- `MealType`: BREAKFAST, LUNCH, DINNER, SNACKS (in MealPlan.kt)

**Key Enums in RecipeRule.kt:**
- `RuleType`: RECIPE, INGREDIENT, MEAL_SLOT, NUTRITION
- `RuleAction`: INCLUDE, EXCLUDE
- `RuleEnforcement`: REQUIRED, PREFERRED
- `FrequencyType`: DAILY, TIMES_PER_WEEK, SPECIFIC_DAYS, NEVER
- `FoodCategory`: GREEN_LEAFY, CITRUS_VITAMIN_C, IRON_RICH, HIGH_PROTEIN, CALCIUM_RICH, FIBER_RICH, OMEGA_3, ANTIOXIDANT

## Design System

| Element | Light | Dark |
|---------|-------|------|
| Primary | `#FF6838` (Orange) | `#FFB59C` |
| Secondary | `#5A822B` (Green) | `#A8D475` |
| Background | `#FDFAF4` (Cream) | `#1C1B1F` |
| Surface | `#FFFFFF` | `#2B2930` |

| Token | Value |
|-------|-------|
| Typography | Roboto (System Default) |
| Spacing | 8dp grid (4, 8, 16, 24, 32, 48dp) |
| Shapes | Rounded corners (8dp small, 16dp medium, 24dp large) |
| Dark Mode | System-follow (auto-switch) |

## Module Structure

Four-layer architecture under `android/`:

| Module | Package | Purpose |
|--------|---------|---------|
| `app` | `com.rasoiai.app.*` | Screens, ViewModels, Hilt modules, navigation |
| `domain` | `com.rasoiai.domain.*` | Models, repository interfaces, use cases |
| `data` | `com.rasoiai.data.*` | Room (local), Retrofit (remote), repository impls |
| `core` | `com.rasoiai.core.*` | Shared UI components, utilities, NetworkMonitor |

**Key locations:**
- Screens: `app/presentation/{feature}/` (e.g., `home/`, `recipedetail/`)
- Domain models: `domain/model/`
- Mappers: `data/local/mapper/EntityMappers.kt`, `data/remote/mapper/DtoMappers.kt`
- Dependencies: `gradle/libs.versions.toml`

**Module Dependencies:**
```
app ─────┬──────> core
         │
         ├──────> domain  <────┐
         │                     │
         └──────> data ────────┴──────> core
```

## Technical Stack

See `android/gradle/libs.versions.toml` for exact dependency versions.

| Layer | Key Technologies |
|-------|-----------------|
| Android | Kotlin 1.9.22, Jetpack Compose, Hilt, Room, Retrofit, Navigation Compose |
| Backend | Python, FastAPI, Firebase Firestore |
| Auth | Firebase Auth (Google OAuth only) |
| Database | Firebase Firestore (backend), Room (Android local cache) |
| LLM | Google Gemini API (primary), Claude API (fallback) |
| Testing | JUnit5, MockK, Turbine, Compose UI Testing |

## Key Design Decisions

1. **Offline-First**: Room DB (Android) caches meal plans, recipes, grocery lists. Firestore (backend) is source of truth.
2. **Database Flexibility**: Backend uses repository pattern with Firestore - can swap to PostgreSQL/MongoDB later without Android changes.
3. **LLM Cost Optimization**: Cache meal plans by preference hash (60-70% savings), store generated recipes for reuse.
4. **Auth**: Google OAuth only (Phone OTP removed for MVP simplicity). Backend accepts `fake-firebase-token` in debug mode for testing.
5. **Festival Intelligence**: 30+ festivals with fasting modes and auto-suggested menus.

## India-Specific Domain Knowledge

| Aspect | Details |
|--------|---------|
| **Dietary Tags** | `VEGETARIAN`, `VEGAN`, `JAIN` (no root vegetables), `SATTVIC` (no onion/garlic), `HALAL`, `EGGETARIAN` |
| **Cuisine Zones** | `NORTH`, `SOUTH`, `EAST`, `WEST` with distinct ingredients |
| **Measurements** | Support both metric and traditional: katori (bowl), chammach (spoon), glass |
| **Family Size** | 3-8 members, multi-generational support |
| **Connectivity** | Offline-first essential (tier 2-3 cities) |

## Development Commands

### Android App (run from `android/` folder)
```bash
cd android

# Build & Test
./gradlew build                  # Full build with tests
./gradlew assembleDebug          # Quick compilation (no tests)
./gradlew test                   # All unit tests
./gradlew :domain:test           # Module-specific tests
./gradlew :data:test
./gradlew :app:test

# Single test class or method
./gradlew test --tests "com.rasoiai.app.ClassName"
./gradlew test --tests "com.rasoiai.app.ClassName.testMethodName"
./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest"

# Compose UI Tests (requires emulator/device - use API 34, not 36)
./gradlew :app:connectedDebugAndroidTest          # All instrumented tests
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.presentation.auth.AuthScreenTest  # Single class
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.rasoiai.app.presentation  # All presentation tests

# Lint
./gradlew lint                   # Report: app/build/reports/lint-results-debug.html

# Install & Launch
./gradlew installDebug
"$ANDROID_HOME/platform-tools/adb" shell am start -n com.rasoiai.app/com.rasoiai.app.MainActivity

# Clean build (when you encounter strange build issues)
./gradlew clean && ./gradlew assembleDebug
```

### Backend (Python + Firestore) - run from `backend/` folder
```bash
cd backend

# Setup virtual environment
python -m venv venv
source venv/bin/activate         # Linux/Mac/Git Bash
.\venv\Scripts\activate          # Windows PowerShell (use this instead)

# Install dependencies
pip install -r requirements.txt

# Configure Firebase (required)
# Option 1: Service account file (recommended)
export FIREBASE_CREDENTIALS_PATH=./rasoiai-firebase-service-account.json

# Option 2: Firebase Emulator (for offline development)
# export FIRESTORE_EMULATOR_HOST=localhost:8080
# firebase emulators:start --only firestore

# Start server
uvicorn app.main:app --reload    # Server at http://localhost:8000/docs

# Seed Firestore with initial data
PYTHONPATH=. python scripts/seed_firestore.py

# Testing
pytest                           # All tests
pytest --cov=app                 # With coverage
pytest tests/test_auth.py -v     # Single file
```

**Firebase Setup:**
1. Get service account key from Firebase Console → Project Settings → Service Accounts
2. Save as `backend/rasoiai-firebase-service-account.json`
3. Add to `.gitignore` (never commit credentials)

## Test Structure

```
android/
├── app/src/test/                    # Unit tests (JUnit5, MockK)
│   └── java/com/rasoiai/app/
│       └── presentation/            # ViewModel tests (*ViewModelTest.kt)
├── app/src/androidTest/             # Instrumented tests (Compose UI Testing)
│   └── java/com/rasoiai/app/
│       └── *Test.kt                 # UI/E2E tests
├── domain/src/test/                 # UseCase tests
└── data/src/test/                   # Repository tests
```

**Test naming:** `ClassNameTest.kt` for unit tests, `ScreenNameTest.kt` for UI tests

## UI Testing (Compose UI Testing)

Tests use **Compose UI Testing** (not Espresso) for native Compose support. Located in `app/src/androidTest/`.

**Current coverage:** ~265 tests across 13 screens:
- Auth (18), Onboarding (41), Home (22), RecipeDetail (26), Grocery (21)
- Chat (17), Favorites (17), Stats (21), Settings (15), Pantry (18)
- RecipeRules (22), CookingMode (27)

| Test Type | Pattern | Purpose |
|-----------|---------|---------|
| UI Tests | `*ScreenTest.kt` | Test wrapper composable with mock UiState, no ViewModel |
| Integration Tests | `*IntegrationTest.kt` | Full app with Hilt DI + FakeGoogleAuthClient |

```kotlin
// UI Test pattern - fast, isolated
class FeatureScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun screen_displaysElement_whenCondition() {
        composeTestRule.setContent { FeatureTestContent(uiState = testState) }
        composeTestRule.onNodeWithTag(TestTags.FEATURE_ELEMENT).assertIsDisplayed()
    }
}

// Integration Test pattern - tests navigation flows
@HiltAndroidTest
class FeatureIntegrationTest : BaseE2ETest() {
    @Inject lateinit var fakeGoogleAuthClient: FakeGoogleAuthClient
    // Uses real navigation, fake auth
}
```

**Key test files:**
- `e2e/base/BaseE2ETest.kt` - Base class with Hilt setup, waitUntil helpers
- `e2e/di/FakeGoogleAuthClient.kt` - Fake auth (configurable success/failure)
- `presentation/common/TestTags.kt` - All semantic test tags

**Important:** Use API 34 emulator (API 36 has Espresso compatibility issues).

See `docs/testing/E2E-Testing-Prompt.md` for the full testing guide.

## CI/CD

GitHub Actions runs on push/PR to `main`/`develop` branches (see `.github/workflows/android-ci.yml`):
- Lint → Unit Tests → Build Debug APK
- Instrumented tests run on PRs only (uses Android emulator)

Artifacts uploaded: lint results, test results, debug APK.

## Troubleshooting

**Gradle sync fails:** Ensure JDK 17+ and `JAVA_HOME` set. Kotlin/KSP must be 1.9.22 / 1.9.22-1.0.17 (see `gradle/libs.versions.toml`). AGP 8.13.2 requires Android Studio Ladybug or newer.

**Emulator not detected:** Set `ANDROID_HOME` and verify with `adb devices`.

**Memory leaks:** LeakCanary is included in debug builds. Check logcat for leak traces during development.

**Gradle daemon issues (Windows):** If builds hang or fail mysteriously, stop existing daemons:
```bash
./gradlew --stop
```

**KSP/Hilt errors after code changes:** Clean and rebuild:
```bash
./gradlew clean :app:kspDebugKotlin
```

## Rules for Claude

1. **Bash Syntax**: Use forward slashes `/` (not `\`), use `./gradlew` (not `.\gradlew`), quote paths with spaces. Shell is Unix-style bash.

2. **Document Output**:
   - Generated documents → `docs/claude-docs/`
   - Test screenshots/artifacts → `docs/testing/screenshots/` (gitignored)
   - Audit reports → `docs/claude-docs/`

3. **Offline-First**: All features must use Room as source of truth with offline support.

## Reference Implementations

Use these as patterns for new screens:

| Pattern | Reference | Key Features |
|---------|-----------|--------------|
| Tabs + Bottom Sheets | `presentation/reciperules/` | 4-tab layout, modal sheets |
| Form-based Settings | `presentation/settings/` | Sections, toggles, navigation |
| Bottom Navigation | `presentation/home/` | RasoiBottomNavigation integration |
| List with Filtering | `presentation/favorites/` | Tab/list pattern |
| Camera Integration | `presentation/pantry/` | ScanResultsSheet component |
| Charts/Gamification | `presentation/stats/` | CuisineBreakdownSection |

## Backend API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/firebase` | Exchange Firebase token for JWT |
| GET | `/api/v1/users/me` | Get current user |
| PUT | `/api/v1/users/preferences` | Update preferences |
| POST | `/api/v1/meal-plans/generate` | Generate meal plan (AI) |
| GET | `/api/v1/meal-plans/current` | Get current week's plan |
| POST | `/api/v1/meal-plans/{planId}/items/{itemId}/swap` | Swap meal |
| GET | `/api/v1/recipes/{id}` | Get recipe details |
| GET | `/api/v1/grocery` | Get grocery list |
| GET | `/api/v1/grocery/whatsapp` | WhatsApp formatted list |
| GET | `/api/v1/festivals/upcoming` | Upcoming festivals |
| POST | `/api/v1/chat/message` | AI chat |
| GET | `/api/v1/stats/streak` | Cooking streak |

API docs available at `http://localhost:8000/docs` when backend is running.

## Key Documentation

| Document | Location |
|----------|----------|
| Requirements | `docs/requirements/RasoiAI Requirements.md` |
| Technical Design | `docs/design/RasoiAI Technical Design.md` |
| Architecture Decisions | `docs/design/Android Architecture Decisions.md` |
| Wireframes | `docs/design/wireframes/` |
| Audit Checklist | `docs/claude-docs/Android-Best-Practices-Audit-Guide.md` |
| E2E Testing Guide | `docs/testing/E2E-Testing-Prompt.md` |
| Session Context | `docs/CONTINUE_PROMPT.md` |
