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
| Testing | JUnit5, MockK, Turbine, Espresso (UI/E2E) |

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

# Instrumented/Espresso tests (requires emulator/device)
./gradlew connectedAndroidTest                    # All instrumented tests
./gradlew :app:connectedDebugAndroidTest          # App module only
./gradlew connectedCheck --tests "*.HomeScreenTest"  # Single test class

# Lint
./gradlew lint                   # Report: app/build/reports/lint-results-debug.html

# Install & Launch
./gradlew installDebug
"$ANDROID_HOME/platform-tools/adb" shell am start -n com.rasoiai.app/com.rasoiai.app.MainActivity

# Clean build (when you encounter strange build issues)
./gradlew clean && ./gradlew assembleDebug
```

### Backend (Python)
```bash
pip install -r requirements.txt
uvicorn app.main:app --reload    # Run server
pytest                           # Run tests
pytest tests/test_meal_plans.py -v
alembic upgrade head             # Database migrations
ruff check .                     # Lint
```

## Test Structure

```
android/
├── app/src/test/                    # Unit tests (JUnit5, MockK)
│   └── java/com/rasoiai/app/
│       └── presentation/            # ViewModel tests (*ViewModelTest.kt)
├── app/src/androidTest/             # Instrumented tests (Espresso)
│   └── java/com/rasoiai/app/
│       └── *Test.kt                 # UI/E2E tests
├── domain/src/test/                 # UseCase tests
└── data/src/test/                   # Repository tests
```

**Test naming:** `ClassNameTest.kt` for unit tests, `ScreenNameTest.kt` for UI tests

## CI/CD

GitHub Actions runs on push/PR to `main`/`develop` branches (see `.github/workflows/android-ci.yml`):
- Lint → Unit Tests → Build Debug APK
- Instrumented tests run on PRs only (uses Android emulator)

Artifacts uploaded: lint results, test results, debug APK.

## Troubleshooting

**Gradle sync fails:** Ensure JDK 17+ and `JAVA_HOME` set. Kotlin/KSP must be 1.9.22 / 1.9.22-1.0.17 (see `gradle/libs.versions.toml`).

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

## Key Documentation

| Document | Location |
|----------|----------|
| Requirements | `docs/requirements/RasoiAI Requirements.md` |
| Technical Design | `docs/design/RasoiAI Technical Design.md` |
| Architecture Decisions | `docs/design/Android Architecture Decisions.md` |
| Wireframes | `docs/design/wireframes/` |
| Audit Checklist | `docs/claude-docs/Android-Best-Practices-Audit-Guide.md` |
| E2E Testing Guide | `docs/testing/E2E-Testing-Prompt.md` |
