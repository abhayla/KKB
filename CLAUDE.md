# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Quick Reference

```bash
# Android (from android/)
./gradlew build                    # Build with tests
./gradlew assembleDebug            # Quick build (no tests)
./gradlew test                     # Unit tests
./gradlew :app:connectedDebugAndroidTest  # UI tests (requires emulator API 34)

# Backend (from backend/)
source venv/bin/activate           # Linux/Mac/Git Bash
# .\venv\Scripts\activate          # Windows PowerShell
uvicorn app.main:app --reload      # Start server → http://localhost:8000/docs
PYTHONPATH=. pytest                # Run all 170 tests
```

**Key numbers:** 3,580 recipes | 170 backend tests | ~400 Android UI tests | 15 screens

**Session context:** Check `docs/CONTINUE_PROMPT.md` for active work between sessions.

## Project Overview

**RasoiAI** (रसोई AI) is an AI-powered meal planning application for Indian families. It generates personalized weekly meal plans based on family preferences, dietary restrictions, regional cuisines, and cultural considerations including festivals and fasting days.

| Attribute | Details |
|-----------|---------|
| **Platform** | Android Native (Kotlin 1.9.22 + Jetpack Compose BOM 2024.02) |
| **Backend** | Python (FastAPI + PostgreSQL) |
| **Target SDK** | 34 (Min SDK 24 / Android 7.0) |
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
| LLM | Claude API (with tool calling for chat) |
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
}

// 3. ViewModel with Hilt injection
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {
    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

    // Use Channel for one-time navigation events
    private val _navigationEvent = Channel<FeatureNavigationEvent>()
    val navigationEvent: Flow<FeatureNavigationEvent> = _navigationEvent.receiveAsFlow()

    fun doSomething() {
        _uiState.update { it.copy(isLoading = true) }
    }
}
```

### Repository Pattern (Offline-First)

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
            entity?.toDomain() ?: run {
                if (networkMonitor.isOnline.first()) fetchAndCache()
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

### Navigation Routes

Routes with arguments use `createRoute()` helper pattern:

```kotlin
data object RecipeDetail : Screen("recipe/{recipeId}?isLocked={isLocked}") {
    fun createRoute(recipeId: String, isLocked: Boolean = false) =
        "recipe/$recipeId?isLocked=$isLocked"
    const val ARG_RECIPE_ID = "recipeId"
}

// Usage
navController.navigate(Screen.RecipeDetail.createRoute(recipeId, isLocked = true))
```

### Bottom Navigation

Screens with bottom navigation use `RasoiBottomNavigation` from `home/components/`:

```kotlin
Scaffold(
    bottomBar = {
        RasoiBottomNavigation(
            selectedItem = NavigationItem.GROCERY,
            onItemSelected = { /* handle navigation */ }
        )
    }
) { /* content */ }
```

**Bottom nav items:** HOME, GROCERY, CHAT, FAVORITES, STATS

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
PYTHONPATH=. python scripts/seed_festivals.py
PYTHONPATH=. python scripts/import_recipes_postgres.py
PYTHONPATH=. python scripts/sync_config_postgres.py
```

### Environment Setup

**Backend `.env` file:**
```
DATABASE_URL=postgresql+asyncpg://rasoiai_user:password@localhost:5432/rasoiai
FIREBASE_CREDENTIALS_PATH=./rasoiai-firebase-service-account.json
ANTHROPIC_API_KEY=sk-ant-...
JWT_SECRET_KEY=your-secret-key
DEBUG=true
```

**PostgreSQL:**
```sql
CREATE DATABASE rasoiai;
CREATE USER rasoiai_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE rasoiai TO rasoiai_user;
```

## Testing

### Test Distribution

| Platform | Tests | Framework |
|----------|-------|-----------|
| Backend | 170 | pytest |
| Android UI | ~400 | Compose UI Testing |
| Android E2E | 65+ | Compose UI Testing + Hilt |

### Backend Tests (170 total)

| Test File | Tests | Purpose |
|-----------|-------|---------|
| `test_health.py` | 2 | Health check endpoints |
| `test_auth.py` | 3 | Firebase authentication |
| `test_preference_service.py` | 26 | PreferenceUpdateService |
| `test_chat_integration.py` | 27 | Chat tool calling flow |
| `test_meal_generation.py` | 22 | Meal generation structures |
| `test_meal_generation_integration.py` | 29 | Rule enforcement |
| `test_meal_generation_e2e.py` | 14 | E2E against PostgreSQL |
| `test_chat_api.py` | 12 | Chat API endpoints |
| `test_recipe_cache.py` | 35 | Recipe cache operations |

### Android UI Tests

Tests use **Compose UI Testing** (not Espresso). Located in `app/src/androidTest/`.

| Test Type | Pattern | Purpose |
|-----------|---------|---------|
| UI Tests | `*ScreenTest.kt` | Test composable with mock UiState |
| Integration Tests | `*IntegrationTest.kt` | Full app with Hilt DI |
| E2E Tests | `*FlowTest.kt` | Complete user flows |

```kotlin
// UI Test pattern
class FeatureScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun screen_displaysElement_whenCondition() {
        composeTestRule.setContent { FeatureTestContent(uiState = testState) }
        composeTestRule.onNodeWithTag(TestTags.FEATURE_ELEMENT).assertIsDisplayed()
    }
}
```

### ViewModel Unit Tests

Located in `app/src/test/java/com/rasoiai/app/presentation/`:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class FeatureViewModelTest {
    @get:Rule val testDispatcherRule = TestDispatcherRule()

    private lateinit var repository: FakeFeatureRepository
    private lateinit var viewModel: FeatureViewModel

    @Before
    fun setup() {
        repository = FakeFeatureRepository()
        viewModel = FeatureViewModel(repository)
    }

    @Test
    fun `initial state is loading`() = runTest {
        assertEquals(true, viewModel.uiState.value.isLoading)
    }
}
```

**Test utilities:**
- `TestDispatcherRule` - Replaces Dispatchers.Main for coroutine tests
- `Fake*Repository` classes - In-memory implementations for testing

### E2E Test Infrastructure

**Key files** in `app/src/androidTest/java/com/rasoiai/app/e2e/`:

| File | Purpose |
|------|---------|
| `base/BaseE2ETest.kt` | Base class with Hilt setup, meal plan generation |
| `util/BackendTestHelper.kt` | Backend API calls with retry |
| `di/FakeGoogleAuthClient.kt` | Fake auth (returns `fake-firebase-token`) |
| `robots/` | Robot pattern classes (HomeRobot, GroceryRobot, etc.) |
| `presentation/common/TestTags.kt` | All semantic test tags |

**E2E Test Flow:**
1. FakeGoogleAuthClient returns fake-firebase-token
2. AuthViewModel calls `/api/v1/auth/firebase` (real API)
3. Backend returns JWT, saved to DataStore
4. User completes 5-step onboarding
5. BackendTestHelper.generateMealPlan() calls AI (4-7 seconds)
6. Home screen displays meal cards
7. Room DB caches meal plan for offline access

## Domain Models

Located in `domain/src/main/java/com/rasoiai/domain/model/`:

| Model | Key Fields |
|-------|-----------|
| `Recipe` | id, name, cuisineType, dietaryTags, prepTimeMinutes, ingredients, instructions |
| `MealPlan` | id, weekStartDate, weekEndDate, days |
| `MealPlanDay` | date, breakfast, lunch, dinner, snacks, festival |
| `RecipeRule` | id, type, action, targetId, frequency, enforcement, mealSlot |

**Key Enums:**
- `DietaryTag`: VEGETARIAN, NON_VEGETARIAN, VEGAN, JAIN, SATTVIC, HALAL, EGGETARIAN
- `CuisineType`: NORTH, SOUTH, EAST, WEST
- `MealType`: BREAKFAST, LUNCH, SNACKS, DINNER
- `RuleAction`: INCLUDE, EXCLUDE

## Backend API

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
| POST | `/api/v1/chat/message` | AI chat with tool calling |

API docs: `http://localhost:8000/docs`

**Backend Key Files:**
| File | Purpose |
|------|---------|
| `app/db/postgres.py` | PostgreSQL connection pool |
| `app/services/meal_generation_service.py` | 2-item pairing logic |
| `app/services/preference_update_service.py` | INCLUDE/EXCLUDE rules |
| `app/ai/chat_assistant.py` | Tool calling orchestration |

## Meal Generation

Configuration-driven meal planning with YAML source of truth synced to PostgreSQL.

**Config files** in `backend/config/`:
- `meal_generation.yaml` - Pairing rules, meal structure
- `reference_data/ingredients.yaml` - Ingredient aliases
- `reference_data/dishes.yaml` - Common dishes with pairings

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
| Tabs + Bottom Sheets | `presentation/reciperules/` | 4-tab layout, modal sheets |
| Form-based Settings | `presentation/settings/` | Sections, toggles |
| Bottom Navigation | `presentation/home/` | RasoiBottomNavigation |
| List with Filtering | `presentation/favorites/` | Tab/list pattern |

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Gradle sync fails | Ensure JDK 17+, `JAVA_HOME` set. Check `gradle/libs.versions.toml` for versions. |
| API 36 emulator issues | Use API 34 - API 36 has Espresso compatibility issues |
| Gradle daemon hangs (Windows) | Run `./gradlew --stop` |
| KSP/Hilt errors | Run `./gradlew clean :app:kspDebugKotlin` |
| Backend import errors | Run from backend dir: `cd backend && PYTHONPATH=. pytest` |
| MissingGreenlet errors | SQLAlchemy async requires `selectinload()` for eager loading |
| Meal generation timeout | AI takes 4-7 seconds; E2E tests use 30-second timeout |

## Rules for Claude

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

4. **Offline-First**: All features must use Room as source of truth with offline support.

4. **Bug & Feature Tracking**:
   - **Before starting work**: Check GitHub Issues for related bugs/features with `gh issue list`
   - **Finding TODOs**: When you find `/* TODO: ... */` comments, consider creating a GitHub Issue
   - **After fixing**: Reference issue number in commit: `Fix #123: description`
   - **Use `/fix-issue <number>`**: To implement a fix for a specific GitHub Issue
   - **Labels**: `bug`, `enhancement`, `not-implemented`, `home-screen`, etc.
   - **Issue Templates**: `.github/ISSUE_TEMPLATE/` has templates for bug reports and feature requests

## Key Documentation

| Document | Location |
|----------|----------|
| Requirements | `docs/requirements/RasoiAI Requirements.md` |
| Technical Design | `docs/design/RasoiAI Technical Design.md` |
| Meal Generation Algorithm | `docs/design/Meal-Generation-Algorithm.md` |
| Meal Generation Config | `docs/design/Meal-Generation-Config-Architecture.md` |
| E2E Testing Guide | `docs/testing/E2E-Testing-Prompt.md` |
| E2E Test Plan | `docs/testing/E2E-Test-Plan.md` |
| Session Context | `docs/CONTINUE_PROMPT.md` |
