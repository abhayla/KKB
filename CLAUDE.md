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

## Quick Start

```bash
# 1. Navigate to android folder
cd android

# 2. Build the project
./gradlew build

# 3. Run tests
./gradlew test

# 4. Install on device/emulator
./gradlew installDebug
```

**Prerequisites:** Android Studio, JDK 17+, Android SDK 34

**Android SDK Path:** Set `ANDROID_HOME` environment variable or use Android Studio's default location.

**ADB Command:** Use full path if adb is not in PATH:
```bash
# Windows (Git Bash) - adjust path to your SDK location
"$ANDROID_HOME/platform-tools/adb" devices

# Or use Android Studio's terminal which has adb in PATH
```

## Infrastructure Setup (Complete)

| Category | Status | Details |
|----------|--------|---------|
| CI/CD | ✅ | GitHub Actions (`android-ci.yml`) - build, test, lint on push/PR |
| Firebase | ✅ | Plugins configured (google-services, crashlytics), Analytics & Crashlytics deps |
| Logging | ✅ | Timber integrated in `RasoiAIApplication` |
| Background Sync | ✅ | WorkManager + `SyncWorker` for offline data sync |
| Network Security | ✅ | `network_security_config.xml` (cleartext blocked except localhost) |
| Gradle Wrapper | ✅ | `gradlew` / `gradlew.bat` scripts |
| Test Infrastructure | ✅ | Sample tests in app, domain, data modules |

### Before Starting Feature Development

1. **Firebase Setup** - Create Firebase project and download `google-services.json` to `android/app/`
2. **Release Signing** - Configure keystore in `android/app/build.gradle.kts` (placeholder exists)

### CI/CD Notes

The GitHub Actions workflow (`android-ci.yml`) automatically creates a placeholder `google-services.json` for CI builds. For local development without Firebase:
```bash
# Create placeholder for build-only testing (from android/ folder)
mkdir -p app/src/debug
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

```
android/
├── app/src/main/java/com/rasoiai/app/
│   ├── presentation/             # All screens & ViewModels
│   │   ├── navigation/           # RasoiNavHost.kt, Screen.kt, PlaceholderScreen.kt
│   │   ├── theme/                # Color.kt, Type.kt, Shape.kt, Spacing.kt, Theme.kt
│   │   ├── common/               # UiState.kt, BaseViewModel.kt
│   │   ├── splash/               # SplashScreen.kt, SplashViewModel.kt
│   │   ├── auth/                 # AuthScreen.kt, AuthViewModel.kt, GoogleAuthClient.kt
│   │   ├── onboarding/           # OnboardingScreen.kt, OnboardingViewModel.kt
│   │   ├── home/                 # HomeScreen.kt, HomeViewModel.kt, components/
│   │   ├── recipedetail/         # RecipeDetailScreen.kt, RecipeDetailViewModel.kt, components/
│   │   ├── cookingmode/          # CookingModeScreen.kt, CookingModeViewModel.kt, components/
│   │   ├── grocery/              # GroceryScreen.kt, GroceryViewModel.kt, components/
│   │   ├── favorites/            # FavoritesScreen.kt, FavoritesViewModel.kt, components/
│   │   ├── chat/                 # ChatScreen.kt, ChatViewModel.kt, components/
│   │   ├── pantry/               # PantryScreen.kt, PantryViewModel.kt, components/
│   │   ├── stats/                # StatsScreen.kt, StatsViewModel.kt, components/
│   │   ├── settings/             # SettingsScreen.kt, SettingsViewModel.kt, components/
│   │   └── reciperules/          # RecipeRulesScreen.kt, RecipeRulesViewModel.kt, components/
│   └── di/                       # Hilt modules
├── domain/src/main/java/com/rasoiai/domain/
│   ├── model/                    # Recipe.kt, MealPlan.kt, Festival.kt, User.kt
│   ├── repository/               # MealPlanRepository.kt, RecipeRepository.kt, AuthRepository.kt
│   └── usecase/                  # GetCurrentMealPlanUseCase.kt, GenerateMealPlanUseCase.kt, etc.
├── data/src/main/java/com/rasoiai/data/
│   ├── local/
│   │   ├── dao/                  # Room DAOs (MealPlanDao, etc.)
│   │   ├── entity/               # Room Entities (MealPlanEntity, etc.)
│   │   ├── datastore/            # DataStore (UserPreferencesDataStore - auth tokens)
│   │   └── mapper/               # Entity ↔ Domain mappers (EntityMappers.kt)
│   ├── remote/
│   │   ├── api/                  # Retrofit service (RasoiApiService.kt)
│   │   ├── dto/                  # API DTOs (MealPlanDto, etc.)
│   │   ├── interceptor/          # AuthInterceptor (adds JWT to requests)
│   │   └── mapper/               # DTO → Domain/Entity mappers (DtoMappers.kt)
│   ├── repository/               # Real impls (AuthRepositoryImpl, MealPlanRepositoryImpl) + Fakes
│   ├── di/                       # DataModule.kt (Hilt bindings)
│   └── sync/                     # SyncManager, OfflineQueueManager
├── core/src/main/java/com/rasoiai/core/
│   ├── ui/                       # Theme, shared composables
│   ├── util/                     # Extensions, constants
│   └── network/                  # NetworkMonitor
└── gradle/
    └── libs.versions.toml        # Centralized dependency versions
```

**Package naming:**
- App module: `com.rasoiai.app.*`
- Domain module: `com.rasoiai.domain.*`
- Data module: `com.rasoiai.data.*`
- Core module: `com.rasoiai.core.*`

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
| Backend | Python, FastAPI, SQLAlchemy, PostgreSQL, Redis |
| Auth | Firebase Auth (Google OAuth only) |
| LLM | Claude API (claude-3-sonnet) |
| Testing | JUnit5, MockK, Turbine, Compose Testing |

## Key Design Decisions

1. **Offline-First**: Room DB caches meal plans, recipes, grocery lists. SyncManager handles queued offline actions.
2. **LLM Cost Optimization**: Cache meal plans by preference hash (60-70% savings), store generated recipes for reuse.
3. **Auth**: Google OAuth only (Phone OTP removed for MVP simplicity).
4. **Festival Intelligence**: 30+ festivals with fasting modes and auto-suggested menus.

## India-Specific Domain Knowledge

| Aspect | Details |
|--------|---------|
| **Dietary Tags** | `VEGETARIAN`, `VEGAN`, `JAIN` (no root vegetables), `SATTVIC` (no onion/garlic), `HALAL`, `EGGETARIAN` |
| **Cuisine Zones** | `NORTH`, `SOUTH`, `EAST`, `WEST` with distinct ingredients |
| **Measurements** | Support both metric and traditional: katori (bowl), chammach (spoon), glass |
| **Family Size** | 3-8 members, multi-generational support |
| **Connectivity** | Offline-first essential (tier 2-3 cities) |

## Development Commands

**Important:** Use forward slashes `/` in bash commands (not backslashes `\`). The shell is Unix-style bash, not Windows CMD.

### Android App (run from `android/` folder)
```bash
cd android

# Build
./gradlew build

# Quick compilation check (no tests)
./gradlew assembleDebug

# Run all unit tests
./gradlew test

# Run single test class
./gradlew test --tests "com.rasoiai.app.ClassName"

# Run single test method
./gradlew test --tests "com.rasoiai.app.ClassName.testMethodName"

# Run tests for specific module
./gradlew :domain:test
./gradlew :data:test
./gradlew :app:test

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Lint (Android lint)
./gradlew lint

# Lint report location
# app/build/reports/lint-results-debug.html

# Clean build
./gradlew clean build

# Install on device/emulator
./gradlew installDebug

# Launch app via adb (use full path)
"/c/Users/itsab/AppData/Local/Android/Sdk/platform-tools/adb" shell am start -n com.rasoiai.app/com.rasoiai.app.MainActivity
```

### Backend (Python)
```bash
# Install dependencies
pip install -r requirements.txt

# Run server
uvicorn app.main:app --reload

# Run tests
pytest

# Run single test file
pytest tests/test_meal_plans.py -v

# Database migrations
alembic upgrade head
alembic revision --autogenerate -m "description"

# Lint
ruff check .
```

## Current Status

**All 13 screens implemented.** Overall audit score: **97%** (production-ready).

| Component | Status |
|-----------|--------|
| UI Screens | ✅ Complete (13 screens in `presentation/`) |
| Auth Integration | ✅ Complete (Firebase + Backend JWT) |
| API Layer | ✅ Complete (Retrofit + AuthInterceptor) |
| Mappers | ✅ Complete (DTO & Entity mappers) |
| Core Repositories | ✅ MealPlan, Recipe, Grocery (offline-first) |
| Other Repositories | ⏳ Fake implementations (Favorites, Chat, Stats) |
| Test Coverage | ✅ All ViewModels, DAOs, Mappers tested |

**Before First Run:** Add `google-services.json` to `android/app/` from Firebase Console (see CI/CD Notes for placeholder).

| Documentation | Location |
|--------------|----------|
| Requirements | `docs/requirements/RasoiAI Requirements.md` |
| Technical Design | `docs/design/RasoiAI Technical Design.md` |
| Architecture Decisions | `docs/design/Android Architecture Decisions.md` |
| Audit Report | `docs/claude-docs/RasoiAI-Codebase-Audit-Report.md` |

## Troubleshooting

**Build fails with missing google-services.json:**
Place a valid `google-services.json` in `android/app/` from Firebase Console, or create the placeholder shown in CI/CD Notes section for build-only testing.

**Gradle sync fails with version mismatch:**
Ensure JDK 17+ is installed and `JAVA_HOME` is set correctly. The project requires exact Kotlin/KSP version compatibility (1.9.22 / 1.9.22-1.0.17). Check `gradle/libs.versions.toml` for current versions.

**Emulator not detected:**
Ensure `ANDROID_HOME` is set and the emulator is running. Use `adb devices` to verify connection.

## Rules for Claude

1. **Bash Path Syntax**: Always use forward slashes `/` in bash commands, not backslashes `\`. Use `./gradlew` not `.\gradlew`. Quote paths containing spaces. The shell is Unix-style bash, not Windows CMD.

2. **Document Output Location**: Save generated documents to `docs/claude-docs/` by default.

3. **Offline-First**: Any feature design must account for offline-first behavior with Room as source of truth.

## Key Documentation

**Reference implementations** (use as patterns for new screens):
| Pattern | Reference | Key Features |
|---------|-----------|--------------|
| Tabs + Bottom Sheets | `presentation/reciperules/` | 4-tab layout, modal sheets |
| Form-based Settings | `presentation/settings/` | Sections, toggles, navigation |
| Bottom Navigation | `presentation/home/` | RasoiBottomNavigation integration |
| List with Filtering | `presentation/favorites/` | Tab/list pattern |
| Camera Integration | `presentation/pantry/` | ScanResultsSheet component |
| Charts/Gamification | `presentation/stats/` | CuisineBreakdownSection |

**Key docs:**
- Wireframes: `docs/design/wireframes/` (individual screen specs)
- Architecture: `docs/design/Android Architecture Decisions.md`
- Audit Checklist: `docs/claude-docs/Android-Best-Practices-Audit-Guide.md`
- Resume Context: `docs/CONTINUE_PROMPT.md`
