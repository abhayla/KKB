# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Project Status

All 13 screens implemented and working. Wireframes split into individual files. UI aligned with wireframe specifications including lock icon updates and 2-column grid sheets.

| Phase | Status | Document |
|-------|--------|----------|
| Requirements | ✅ Complete | `docs/requirements/RasoiAI Requirements.md` |
| Technical Design | ✅ Complete | `docs/design/RasoiAI Technical Design.md` |
| Architecture Decisions | ✅ Complete | `docs/design/Android Architecture Decisions.md` |
| Design System | ✅ Complete | `docs/design/RasoiAI Design System.md` |
| Screen Wireframes | ✅ Complete | `docs/design/wireframes/` (16 files, v3.1) |
| Android Project Setup | ✅ Complete | `android/` folder |
| All 13 Screens | ✅ Complete | See list below |
| **Backend Integration** | ⏳ **Next Step** | API connections, Firebase Auth |

### Implemented Screens (all complete):
1. Splash (`presentation/splash/`)
2. Auth (`presentation/auth/`)
3. Onboarding (`presentation/onboarding/`)
4. Home (`presentation/home/`)
5. Recipe Detail (`presentation/recipedetail/`)
6. Cooking Mode (`presentation/cookingmode/`)
7. Grocery List (`presentation/grocery/`)
8. Favorites (`presentation/favorites/`)
9. Chat (`presentation/chat/`)
10. Pantry Scan (`presentation/pantry/`)
11. Stats (`presentation/stats/`)
12. Settings (`presentation/settings/`)
13. Recipe Rules (`presentation/reciperules/`)

## Recent Updates (Last Session)

### Wireframe Changes Applied to Code:
1. **Lock icons show current state** (not action):
   - 🔒 = item IS locked
   - 🔓 = item IS unlocked
   - Removed "Locked" text labels

2. **AddRecipeSheet** - New component with 2-column grid layout:
   - `home/components/AddRecipeSheet.kt`
   - `home/components/RecipeSelectionGridItem.kt`
   - Search bar, Suggestions/Favorites tabs

3. **SwapRecipeSheet** - Updated with 2-column grid layout:
   - Search functionality
   - Similar recipes grid display

4. **RecipeHeader tri-state lock indicator**:
   - `RecipeLockState` enum: LOCKED, UNLOCKED, NO_CONTEXT
   - Shows 🔒 when locked from meal plan
   - Shows 🔓 when unlocked from meal plan
   - No icon when not from meal plan context (favorites, search, chat)
   - Added `fromMealPlan` navigation parameter

5. **Wireframes split into 16 files**:
   - `docs/design/wireframes/00-overview.md` through `13-recipe-rules.md`
   - `docs/design/wireframes/99-common-components.md`

## App Verified Working

- ✅ App builds successfully (`./gradlew build`)
- ✅ All tests pass (`./gradlew test`)
- ✅ Full navigation flow works
- ✅ Bottom navigation working (Home, Grocery, Chat, Favs, Stats)
- ✅ All 13 screens implemented and working
- ✅ Lock icons display correctly at all levels (day/meal/recipe)
- ✅ 2-column grid sheets working

## Your Task

**Begin Backend Integration Phase**:

### Priority Tasks:
1. **Firebase Auth Setup**
   - Add real `google-services.json` from Firebase Console
   - Implement actual Google Sign-In flow in `AuthViewModel`
   - Store user session in DataStore

2. **API Layer Setup**
   - Create Retrofit service interfaces
   - Add API DTOs in `data/remote/dto/`
   - Implement network interceptors for auth tokens

3. **Repository Implementation**
   - Replace Fake repositories with real implementations
   - Add Room database for offline caching
   - Implement sync logic between local and remote

4. **Key API Endpoints to Implement**:
   - `POST /auth/google` - Google OAuth
   - `GET /meal-plans/{date}` - Get meal plan for date
   - `POST /meal-plans/generate` - Generate new meal plan
   - `GET /recipes/{id}` - Get recipe details
   - `POST /recipes/{id}/favorite` - Toggle favorite
   - `GET /grocery-list` - Get grocery list
   - `POST /chat/message` - Send chat message

### Files to Create:
```
data/remote/
├── api/
│   ├── RasoiAIApi.kt           # Retrofit interface
│   ├── AuthApi.kt              # Auth endpoints
│   └── interceptors/
│       └── AuthInterceptor.kt  # Add auth token to requests
├── dto/
│   ├── MealPlanDto.kt
│   ├── RecipeDto.kt
│   └── UserDto.kt
└── mapper/
    └── DtoMappers.kt           # DTO to domain model mappers

data/local/
├── database/
│   ├── RasoiDatabase.kt        # Room database
│   └── dao/
│       ├── MealPlanDao.kt
│       ├── RecipeDao.kt
│       └── GroceryDao.kt
└── entity/
    ├── MealPlanEntity.kt
    └── RecipeEntity.kt
```

### Update Files:
- `data/di/DataModule.kt` - Add Retrofit, Room bindings
- `data/repository/*` - Replace fake implementations
- `app/presentation/auth/AuthViewModel.kt` - Real Google Sign-In

## Reference Files

| File | Path | Why |
|------|------|-----|
| Wireframes | `docs/design/wireframes/` | All 16 screen wireframes |
| Home Wireframe | `docs/design/wireframes/04-home.md` | Lock icon specs, Add/Swap sheets |
| Recipe Detail | `docs/design/wireframes/05-recipe-detail.md` | Lock indicator specs |
| Technical Design | `docs/design/RasoiAI Technical Design.md` | API specs, data models |
| CLAUDE.md | `CLAUDE.md` | Full project context and patterns |
| Home Screen | `app/presentation/home/HomeScreen.kt` | Lock icons, sheets implementation |
| RecipeHeader | `app/presentation/recipedetail/components/RecipeHeader.kt` | Tri-state lock |
| Screen.kt | `app/presentation/navigation/Screen.kt` | Navigation with fromMealPlan param |
| Fake Repos | `data/repository/Fake*.kt` | Current mock implementations |

## Working Directory
Project root: `D:/Abhay/VibeCoding/KKB`

Start by reading:
1. `CLAUDE.md` for full project context
2. `docs/design/RasoiAI Technical Design.md` for API specs
3. Current fake repository implementations in `data/repository/`

Then begin implementing the backend integration layer.
```

---

## QUICK REFERENCE

### Build Commands (use forward slashes `/` in bash):
```bash
cd "D:/Abhay/VibeCoding/KKB/android"
./gradlew build           # Build
./gradlew test            # Run tests
./gradlew installDebug    # Install on device
./gradlew assembleDebug   # Build APK only
```

### Run App:
```bash
# Check device
"/c/Users/itsab/AppData/Local/Android/Sdk/platform-tools/adb" devices

# Install and launch
./gradlew installDebug
"/c/Users/itsab/AppData/Local/Android/Sdk/platform-tools/adb" shell am start -n com.rasoiai.app/com.rasoiai.app.MainActivity
```

### Key Architecture:
- **ViewModel Pattern**: `UiState` data class + `NavigationEvent` sealed class
- **DI**: Hilt with `@HiltViewModel` and `@Inject constructor`
- **State**: `MutableStateFlow` with `.update { it.copy(...) }`
- **Navigation**: Callbacks passed to Screen, ViewModel emits events
- **Lock State**: `RecipeLockState` enum (LOCKED, UNLOCKED, NO_CONTEXT)

### Design System:
| Element | Light | Dark |
|---------|-------|------|
| Primary | `#FF6838` | `#FFB59C` |
| Secondary | `#5A822B` | `#A8D475` |
| Background | `#FDFAF4` | `#1C1B1F` |

| Token | Usage |
|-------|-------|
| Spacing | `spacing.xs` (4dp), `spacing.sm` (8dp), `spacing.md` (16dp), `spacing.lg` (24dp) |
| Corners | 8dp (small), 16dp (medium), 24dp (large) |

### Wireframe Files:
```
docs/design/wireframes/
├── 00-overview.md
├── 01-splash.md
├── 02-auth.md
├── 03-onboarding.md
├── 04-home.md              # Lock icons, Add/Swap sheets
├── 05-recipe-detail.md     # Lock indicator specs
├── 06-cooking-mode.md
├── 07-grocery-list.md
├── 08-favorites.md
├── 09-chat.md
├── 10-pantry-scan.md
├── 11-stats.md
├── 12-settings.md
├── 13-recipe-rules.md
└── 99-common-components.md
```

---

## PREVIOUS SESSIONS SUMMARY

### Sessions 1-10: Core UI Implementation
- Implemented all 12 core screens
- Established ViewModel pattern with StateFlow
- Set up Hilt DI, Navigation Compose
- All screens follow wireframe designs

### Session 11: Wireframe Review & Recipe Rules Design
- Reviewed all 12 wireframes with user approval
- Redesigned Screen 4 (Home) with 3-level locking system
- Added lock indicator to Screen 5 (Recipe Detail)
- Created Screen 13 (Recipe Rules) wireframe

### Session 12: Recipe Rules Implementation
- Implemented Recipe Rules screen with 4 tabs
- Added domain models (RecipeRule, NutritionGoal, FoodCategory)
- Created FakeRecipeRulesRepository with mock data
- Full CRUD operations for rules

### Session 13: Wireframe Updates & Lock Icon Fixes
- Split large wireframes doc into 16 individual files
- Updated lock icons to show current state (🔒/🔓) not action
- Created AddRecipeSheet with 2-column grid layout
- Updated SwapRecipeSheet with 2-column grid layout
- Added RecipeLockState tri-state enum
- Added fromMealPlan navigation parameter
- Fixed duplicate lock icons at meal type level

### Next Phase: Backend Integration
- Firebase Auth with real Google Sign-In
- Retrofit API layer
- Room database for offline caching
- Replace fake repositories with real implementations

---

*Last Updated: January 2025*
*All 13 screens complete, ready for backend integration*
