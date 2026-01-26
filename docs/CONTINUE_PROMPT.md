# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Current Status

**All 13 screens implemented.** Backend integration complete for core repositories.

| Component | Status |
|-----------|--------|
| UI Screens | ✅ Complete (13 screens) |
| Auth Integration | ✅ Complete (Firebase + Backend JWT) |
| API Layer | ✅ Complete (Retrofit + AuthInterceptor) |
| DTO Mappers | ✅ Complete (API → Domain) |
| Entity Mappers | ✅ Complete (Room ↔ Domain) |
| MealPlan Repository | ✅ Complete (offline-first) |
| Recipe Repository | ✅ Complete (offline-first) |
| Grocery Repository | ✅ Complete (offline-first) |
| Firebase Auth Flow | ✅ Verified (unit tests pass) |
| Other Repositories | ⏳ Fake (Favorites, Chat, Stats, etc.) |

## Firebase Setup Status

| Item | Status |
|------|--------|
| google-services.json | ✅ Present |
| Web Client ID | ✅ Configured in BuildConfig |
| Unit Tests | ✅ Passing |
| Debug SHA-1 | ✅ Available (see below) |

**Debug SHA-1 Fingerprint** (add to Firebase Console):
```
0D:1C:9D:5D:36:70:91:06:7E:16:C8:D8:EC:5F:AF:C1:6C:39:1D:6E
```

## IMMEDIATE NEXT STEPS

**Choose based on priority:**

### Option 1: Complete Firebase Console Setup (if not done)
1. Go to Firebase Console → Project Settings → Your apps
2. Add SHA-1 fingerprint: `0D:1C:9D:5D:36:70:91:06:7E:16:C8:D8:EC:5F:AF:C1:6C:39:1D:6E`
3. Enable Google Sign-In in Authentication → Sign-in method
4. Test on device: `cd android && ./gradlew installDebug`

### Option 2: Implement Remaining Repositories
- `FavoritesRepositoryImpl` - follows same offline-first pattern
- `ChatRepositoryImpl` - may need real-time updates
- `StatsRepositoryImpl` - analytics and streak tracking
- `SettingsRepositoryImpl` - user preferences sync

### Option 3: Backend API Development
- Set up Python/FastAPI backend
- Implement JWT token exchange endpoint
- Create meal plan generation API with Claude

## Architecture (Offline-First)

```
┌─────────────────────────────────────────────────────────────┐
│  UI (Compose) → ViewModel → UseCase → Repository            │
│                                           ↓                 │
│                              ┌────────────┴────────────┐    │
│                              ↓                         ↓    │
│                         Room (Local)            Retrofit    │
│                         Source of Truth         (Remote)    │
│                              ↓                         ↓    │
│                         EntityMappers           DtoMappers  │
│                              └──────────┬──────────────┘    │
│                                         ↓                   │
│                                   Domain Models             │
└─────────────────────────────────────────────────────────────┘
```

## Key Files Reference

| Purpose | File |
|---------|------|
| Project Context | `CLAUDE.md` |
| Auth Flow | `data/repository/AuthRepositoryImpl.kt` |
| Auth ViewModel | `app/presentation/auth/AuthViewModel.kt` |
| Google Sign-In | `app/presentation/auth/GoogleAuthClient.kt` |
| MealPlan Repo | `data/repository/MealPlanRepositoryImpl.kt` |
| Recipe Repo | `data/repository/RecipeRepositoryImpl.kt` |
| Grocery Repo | `data/repository/GroceryRepositoryImpl.kt` |
| Entity Mappers | `data/local/mapper/EntityMappers.kt` |
| DTO Mappers | `data/remote/mapper/DtoMappers.kt` |
| DI Module | `data/di/DataModule.kt` |
| API Service | `data/remote/api/RasoiApiService.kt` |

## Environment

- Working directory: `D:/Abhay/VibeCoding/KKB`
- Build: `cd android && ./gradlew assembleDebug`
- Test: `cd android && ./gradlew test`
- Install: `cd android && ./gradlew installDebug`
- SHA-1: `cd android && ./gradlew signingReport`

## Real Repositories Implemented

All follow offline-first pattern with:
- Room as single source of truth
- API fetch when online
- Local cache updates
- Sync support for offline mutations

| Repository | Key Features |
|------------|--------------|
| Auth | Firebase → Backend JWT exchange, token storage |
| MealPlan | Generate, swap meals, lock state, sync |
| Recipe | Search, scale, favorites, cache |
| Grocery | Generate from meal plan, toggle purchased, custom items |

Continue from here based on the immediate next step you choose.
```

---

## QUICK REFERENCE

### Build Commands:
```bash
cd "D:/Abhay/VibeCoding/KKB/android"
./gradlew assembleDebug   # Build APK
./gradlew test            # Run tests
./gradlew build           # Full build + tests
./gradlew installDebug    # Install on device
./gradlew signingReport   # Get SHA-1 fingerprint
```

### Test on Device:
```bash
# Install and launch
./gradlew installDebug
adb shell am start -n com.rasoiai.app/.MainActivity

# View auth logs
adb logcat -s RasoiAI:* | grep -E "(Firebase|Auth|JWT)"
```

### Key Patterns:

**ViewModel Pattern:**
```kotlin
data class FeatureUiState(val isLoading: Boolean = true, ...)
sealed class FeatureNavigationEvent { ... }

@HiltViewModel
class FeatureViewModel @Inject constructor(...) : ViewModel() {
    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()
}
```

**Repository Pattern (Offline-First):**
```kotlin
@Singleton
class RepositoryImpl @Inject constructor(
    private val apiService: RasoiApiService,
    private val dao: FeatureDao,
    private val networkMonitor: NetworkMonitor
) : Repository {
    // 1. Return from local DB (single source of truth)
    // 2. Fetch from API if online
    // 3. Cache to local DB
    // 4. Queue mutations for sync if offline
}
```

---

## PREVIOUS SESSIONS SUMMARY

### Sessions 1-10: Core UI Implementation
- All 13 core screens implemented
- ViewModel pattern with StateFlow
- Hilt DI, Navigation Compose setup

### Session 11: Wireframe Review & Recipe Rules Design
- Redesigned Home with 3-level locking
- Added lock indicator to Recipe Detail
- Created Recipe Rules wireframe

### Session 12: Recipe Rules Implementation
- Recipe Rules screen with 4 tabs
- Domain models (RecipeRule, NutritionGoal)
- FakeRecipeRulesRepository

### Session 13: Wireframe Updates & Lock Icon Fixes
- Split wireframes into 16 files
- Lock icons show state (locked/unlocked) not action
- AddRecipeSheet/SwapRecipeSheet 2-column grids
- RecipeLockState tri-state enum
- fromMealPlan navigation parameter

### Session 14: Backend Integration Phase 1
- Auth token storage in DataStore
- AuthInterceptor for API requests
- DTO and Entity mappers
- AuthRepositoryImpl (Firebase → Backend JWT)
- MealPlanRepositoryImpl (offline-first)
- Updated Room entities with full data

### Session 15: MealPlanRepositoryImpl Wiring
- Wired MealPlanRepositoryImpl in DataModule.kt
- Added MealPlanDao provider
- Build verified successful

### Session 16: RecipeRepositoryImpl Implementation
- Added Recipe Entity mappers in EntityMappers.kt
- Created RecipeRepositoryImpl.kt with offline-first pattern
- Wired in DataModule.kt with RecipeDao, FavoriteDao providers

### Session 17: GroceryRepositoryImpl Implementation
- Added Grocery Entity mappers in EntityMappers.kt
- Created GroceryRepositoryImpl.kt with offline-first pattern
- Features: generate from meal plan, toggle purchased, custom items
- Wired in DataModule.kt with GroceryDao provider

### Session 18: Firebase Auth Flow Verification
- Verified google-services.json configuration
- Confirmed Web Client ID in BuildConfig
- Extracted debug SHA-1 fingerprint
- All unit tests passing
- Documented Firebase Console setup steps
- **Next: Complete Firebase Console setup or implement more repositories**

---

## FILES CREATED/MODIFIED IN BACKEND INTEGRATION

### New Files:
```
data/remote/interceptor/AuthInterceptor.kt
data/remote/mapper/DtoMappers.kt
data/local/mapper/EntityMappers.kt
data/repository/AuthRepositoryImpl.kt
data/repository/MealPlanRepositoryImpl.kt
data/repository/RecipeRepositoryImpl.kt
data/repository/GroceryRepositoryImpl.kt
```

### Modified Files:
```
data/local/datastore/UserPreferencesDataStore.kt
data/local/entity/MealPlanEntity.kt
data/local/dao/MealPlanDao.kt
data/local/RasoiDatabase.kt
data/di/DataModule.kt
app/presentation/auth/AuthViewModel.kt
app/build.gradle.kts (WEB_CLIENT_ID)
```

---

*Last Updated: January 2026*
*Backend integration Phase 1 complete. MealPlan, Recipe, Grocery repositories implemented with offline-first pattern. Firebase auth flow verified.*
