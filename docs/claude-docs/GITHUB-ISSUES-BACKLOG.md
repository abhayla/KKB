# RasoiAI GitHub Issues Backlog

**Generated:** February 2, 2026
**Total Issues:** 32
**Source:** Codebase analysis and technical debt audit

---

## Quick Import Guide

Each issue below is formatted for GitHub. To create issues:
1. Go to https://github.com/your-repo/issues/new
2. Copy the title and body for each issue
3. Apply the suggested labels

---

## Issue Categories

| Category | Count | Labels |
|----------|-------|--------|
| Missing Implementation | 4 | `not-implemented`, `enhancement` |
| Partial Implementation | 5 | `not-implemented`, `enhancement` |
| TODO - Presentation | 14 | `todo`, `android` |
| TODO - Data Layer | 4 | `todo`, `android` |
| TODO - Build/Config | 2 | `todo`, `infrastructure` |
| Missing Tests | 3 | `testing`, `enhancement` |

---

# MISSING IMPLEMENTATIONS (Critical)

## Issue #1: Implement NotificationsScreen ViewModel and functionality

**Labels:** `not-implemented`, `enhancement`, `high-priority`, `android`

### Summary
The Notifications screen is currently a placeholder/stub with no ViewModel or functionality. It only displays a static "No notifications yet" message.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/notifications/NotificationsScreen.kt`
- **Status:** Placeholder UI only
- **ViewModel:** Missing
- **Tests:** None

### Required Implementation
1. Create `NotificationsViewModel` with proper UiState
2. Implement notification data loading from repository
3. Create `NotificationRepository` interface and implementation
4. Add Room entity for notifications
5. Add backend API endpoint for notifications
6. Display notification types:
   - Festival reminders
   - Meal plan updates
   - Shopping list reminders
   - Recipe suggestions
   - Cooking streak milestones

### Acceptance Criteria
- [ ] NotificationsViewModel created with StateFlow<UiState>
- [ ] NotificationEntity and NotificationDao created
- [ ] Notifications loaded from local Room DB
- [ ] Notifications synced from backend
- [ ] UI displays notification list with proper categorization
- [ ] Mark as read functionality
- [ ] Clear all notifications
- [ ] Unit tests for ViewModel (minimum 10 tests)
- [ ] UI tests for NotificationsScreen

### Code Location
- Screen: `presentation/notifications/NotificationsScreen.kt`
- ViewModel: `presentation/notifications/NotificationsViewModel.kt` (to create)

### Priority
**High** - Feature completeness for MVP

---

## Issue #2: Implement SyncWorker for offline data synchronization

**Labels:** `not-implemented`, `enhancement`, `high-priority`, `android`

### Summary
The SyncWorker class exists but is completely stubbed with TODO comments. Offline sync functionality is not working.

### Current State
- **File:** `android/data/src/main/java/com/rasoiai/data/sync/SyncWorker.kt`
- **Lines:** 28, 37
- **Status:** Empty implementation with TODO comments

### Required Implementation
```kotlin
// Currently stubbed methods:
private suspend fun syncOfflineQueue() { /* TODO */ }
private suspend fun syncMealPlans() { /* TODO */ }
private suspend fun syncFavorites() { /* TODO */ }
private suspend fun syncGroceryList() { /* TODO */ }
```

1. Implement `syncOfflineQueue()` - Process queued offline operations
2. Implement `syncMealPlans()` - Sync local meal plan changes to server
3. Implement `syncFavorites()` - Sync favorite recipes
4. Implement `syncGroceryList()` - Sync grocery list changes
5. Add conflict resolution logic
6. Add retry with exponential backoff
7. Schedule periodic sync via WorkManager

### Acceptance Criteria
- [ ] All sync methods implemented
- [ ] Conflict resolution for concurrent edits
- [ ] Retry logic with exponential backoff
- [ ] WorkManager periodic sync (every 15 minutes when online)
- [ ] Manual sync trigger from Settings
- [ ] Sync status indicator in UI
- [ ] Unit tests for sync logic
- [ ] Integration tests with mock server

### Code Location
- `data/sync/SyncWorker.kt`
- `data/repository/*RepositoryImpl.kt` (add unsynced tracking)

### Priority
**High** - Critical for offline-first experience

---

## Issue #3: Add Firebase Cloud Messaging Service for push notifications

**Labels:** `not-implemented`, `enhancement`, `high-priority`, `android`

### Summary
Push notification support is not implemented. The Firebase Messaging Service needs to be added to receive and handle push notifications.

### Current State
- **File:** `android/app/src/main/AndroidManifest.xml`
- **Line:** 53
- **Status:** TODO comment, no FCM service registered

### Required Implementation
1. Create `RasoiFcmService` extending `FirebaseMessagingService`
2. Handle token refresh
3. Handle incoming messages
4. Create notification channels
5. Display notifications with proper intents
6. Backend: Add FCM token storage and notification sending

### Acceptance Criteria
- [ ] `RasoiFcmService` created and registered in manifest
- [ ] FCM token sent to backend on refresh
- [ ] Notification channels created (meals, reminders, social)
- [ ] Notifications display correctly with app icon
- [ ] Tapping notification navigates to correct screen
- [ ] Backend can send targeted notifications
- [ ] Token invalidation handled

### Code Location
- Service: `app/src/main/java/com/rasoiai/app/service/RasoiFcmService.kt` (to create)
- Manifest: `app/src/main/AndroidManifest.xml`

### Priority
**High** - Required for user engagement

---

## Issue #4: Configure release signing for Play Store deployment

**Labels:** `not-implemented`, `infrastructure`, `high-priority`

### Summary
Release signing configuration is not set up, preventing Play Store deployment.

### Current State
- **File:** `android/app/build.gradle.kts`
- **Line:** 52
- **Status:** TODO comment

### Required Implementation
1. Generate release keystore
2. Configure signing config in build.gradle.kts
3. Store keystore credentials securely (not in repo)
4. Set up CI/CD signing (GitHub Secrets)

### Acceptance Criteria
- [ ] Release keystore generated
- [ ] Signing config added to build.gradle.kts
- [ ] Keystore password stored in local.properties (gitignored)
- [ ] CI/CD can sign release builds
- [ ] Release APK/AAB builds successfully
- [ ] Document keystore backup procedure

### Code Location
- `app/build.gradle.kts`
- `local.properties` (gitignored)

### Priority
**High** - Required for release

---

# PARTIAL IMPLEMENTATIONS (Coming Soon Features)

## Issue #5: Implement voice input for Chat screen

**Labels:** `not-implemented`, `enhancement`, `medium-priority`, `android`

### Summary
Voice input button in Chat shows "Voice input coming soon!" error message instead of actual functionality.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/chat/ChatViewModel.kt`
- **Line:** 219
- **Status:** Shows error message placeholder

### Required Implementation
1. Integrate Android Speech-to-Text API
2. Request RECORD_AUDIO permission
3. Convert speech to text
4. Send transcribed text as chat message
5. Show recording indicator during input

### Acceptance Criteria
- [ ] Microphone permission requested and handled
- [ ] Speech recognition starts on button tap
- [ ] Recording indicator shown during input
- [ ] Transcribed text appears in input field
- [ ] User can edit before sending
- [ ] Error handling for no speech detected
- [ ] Works offline (on-device recognition)

### Code Location
- `presentation/chat/ChatViewModel.kt:219`
- `presentation/chat/ChatScreen.kt`

### Priority
**Medium** - Enhanced UX feature

---

## Issue #6: Implement photo attachment for Chat screen

**Labels:** `not-implemented`, `enhancement`, `medium-priority`, `android`

### Summary
Photo attachment button in Chat shows "Photo attachment coming soon!" error message.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/chat/ChatViewModel.kt`
- **Line:** 223
- **Status:** Shows error message placeholder

### Required Implementation
1. Add camera and gallery permissions
2. Implement image picker (camera or gallery)
3. Compress and upload image to backend
4. Display image in chat message
5. AI can analyze food images for suggestions

### Acceptance Criteria
- [ ] Camera permission requested
- [ ] Gallery picker implemented
- [ ] Image compression before upload
- [ ] Image preview before sending
- [ ] Backend accepts image uploads
- [ ] AI can process food images
- [ ] Images displayed in chat history

### Code Location
- `presentation/chat/ChatViewModel.kt:223`
- `presentation/chat/ChatScreen.kt`

### Priority
**Medium** - Enhanced UX feature

---

## Issue #7: Implement camera capture for Pantry screen

**Labels:** `not-implemented`, `enhancement`, `medium-priority`, `android`

### Summary
Camera capture in Pantry shows "coming soon" error instead of scanning pantry items.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/pantry/PantryViewModel.kt`
- **Line:** 188
- **Status:** Shows error message placeholder

### Required Implementation
1. Add camera permission handling
2. Implement CameraX for capture
3. OCR for ingredient label reading (ML Kit)
4. Auto-populate pantry item from scan
5. Barcode scanning for packaged items

### Acceptance Criteria
- [ ] Camera permission requested
- [ ] CameraX preview implemented
- [ ] ML Kit OCR integration
- [ ] Barcode scanning with ML Kit
- [ ] Scanned items auto-populate form
- [ ] Manual correction of scanned data
- [ ] Works offline (on-device ML)

### Code Location
- `presentation/pantry/PantryViewModel.kt:188`
- `presentation/pantry/PantryScreen.kt`

### Priority
**Medium** - Enhanced UX feature

---

## Issue #8: Implement gallery selection for Pantry screen

**Labels:** `not-implemented`, `enhancement`, `medium-priority`, `android`

### Summary
Gallery selection in Pantry shows "coming soon" error instead of allowing image selection.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/pantry/PantryViewModel.kt`
- **Line:** 193
- **Status:** Shows error message placeholder

### Required Implementation
1. Implement photo picker
2. Process selected image for OCR
3. Extract ingredient information
4. Auto-populate pantry item

### Acceptance Criteria
- [ ] Photo picker implemented
- [ ] Selected image processed with ML Kit
- [ ] Extracted text parsed for ingredients
- [ ] Quantity and unit detection
- [ ] Manual correction UI

### Code Location
- `presentation/pantry/PantryViewModel.kt:193`
- `presentation/pantry/PantryScreen.kt`

### Priority
**Medium** - Enhanced UX feature

---

## Issue #9: Implement items per meal selection dialog in Settings

**Labels:** `not-implemented`, `enhancement`, `medium-priority`, `android`

### Summary
Items per meal setting shows "Coming soon" error instead of selection dialog.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/settings/SettingsViewModel.kt`
- **Line:** 289
- **Status:** Shows error message placeholder

### Required Implementation
1. Create selection dialog (1-4 items)
2. Update user preferences
3. Sync to backend
4. Affect meal plan generation

### Acceptance Criteria
- [ ] Dialog with 1-4 selection options
- [ ] Current value highlighted
- [ ] Updates local preferences
- [ ] Syncs to backend
- [ ] Next meal generation uses new value
- [ ] Visual feedback on save

### Code Location
- `presentation/settings/SettingsViewModel.kt:289`
- `presentation/settings/SettingsScreen.kt`

### Priority
**Medium** - Feature completeness

---

# TODO - PRESENTATION LAYER

## Issue #10: Implement Terms of Service URL navigation

**Labels:** `todo`, `android`, `low-priority`

### Summary
Terms of Service link in Auth screen is a no-op callback.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/auth/AuthScreen.kt`
- **Line:** 146
- **Status:** Empty callback

### Required Implementation
1. Create Terms of Service page (or host externally)
2. Open URL in browser or WebView
3. Handle back navigation

### Acceptance Criteria
- [ ] Terms URL opens in browser
- [ ] Or: In-app WebView with back button
- [ ] URL configurable via BuildConfig

### Code Location
- `presentation/auth/AuthScreen.kt:146`

### Priority
**Low** - Legal compliance

---

## Issue #11: Implement Privacy Policy URL navigation

**Labels:** `todo`, `android`, `low-priority`

### Summary
Privacy Policy link in Auth screen is a no-op callback.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/auth/AuthScreen.kt`
- **Line:** 147
- **Status:** Empty callback

### Required Implementation
1. Create Privacy Policy page (or host externally)
2. Open URL in browser or WebView

### Acceptance Criteria
- [ ] Privacy URL opens in browser
- [ ] URL configurable via BuildConfig

### Code Location
- `presentation/auth/AuthScreen.kt:147`

### Priority
**Low** - Legal compliance

---

## Issue #12: Implement navigate to settings from Chat menu

**Labels:** `todo`, `android`, `low-priority`

### Summary
Chat menu has settings option but navigation is not implemented.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/chat/ChatScreen.kt`
- **Line:** 191
- **Status:** TODO comment

### Required Implementation
Navigate to Settings screen when settings menu item tapped.

### Acceptance Criteria
- [ ] Tapping settings navigates to SettingsScreen
- [ ] Back button returns to Chat

### Code Location
- `presentation/chat/ChatScreen.kt:191`

### Priority
**Low** - UX improvement

---

## Issue #13: Implement meal plan integration in Chat

**Labels:** `todo`, `android`, `medium-priority`

### Summary
Chat should be able to reference and modify the current meal plan, but integration is placeholder.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/chat/ChatViewModel.kt`
- **Line:** 199
- **Status:** TODO - uses placeholder instead of MealPlanRepository

### Required Implementation
1. Inject MealPlanRepository into ChatViewModel
2. Load current meal plan context
3. Allow AI to reference specific meals
4. Allow meal modifications via chat

### Acceptance Criteria
- [ ] Chat can display current meal plan
- [ ] User can ask "What's for dinner today?"
- [ ] AI responses reference actual meals
- [ ] Modifications sync to meal plan

### Code Location
- `presentation/chat/ChatViewModel.kt:199`

### Priority
**Medium** - Feature enhancement

---

## Issue #14: Implement recipe rating submission

**Labels:** `todo`, `android`, `medium-priority`

### Summary
After cooking mode completion, rating is collected but not submitted to repository.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/cookingmode/CookingModeViewModel.kt`
- **Line:** 329
- **Status:** TODO - rating not saved

### Required Implementation
1. Create rating endpoint in backend
2. Add rating to RecipeRepository
3. Submit rating after cooking completion
4. Display average ratings on recipes

### Acceptance Criteria
- [ ] Rating submitted to backend
- [ ] Rating stored locally
- [ ] Average rating displayed on recipe
- [ ] User's own rating shown if exists
- [ ] Stats track recipes cooked

### Code Location
- `presentation/cookingmode/CookingModeViewModel.kt:329`

### Priority
**Medium** - Data collection for improvements

---

## Issue #15: Implement festival recipes navigation

**Labels:** `todo`, `android`, `low-priority`

### Summary
Tapping on festival info should navigate to festival-specific recipes.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/home/HomeScreen.kt`
- **Line:** 290
- **Status:** TODO comment

### Required Implementation
1. Create FestivalRecipesScreen (or filter existing recipes)
2. Navigate with festival ID
3. Display suggested dishes for festival

### Acceptance Criteria
- [ ] Tapping festival navigates to recipes
- [ ] Recipes filtered by festival
- [ ] Suggested dishes highlighted
- [ ] Can add festival recipe to meal plan

### Code Location
- `presentation/home/HomeScreen.kt:290`

### Priority
**Low** - Feature enhancement

---

## Issue #16: Implement recipe selection in AddRecipe sheet

**Labels:** `todo`, `android`, `medium-priority`

### Summary
AddRecipe bottom sheet callback for recipe selection is not fully implemented.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/home/HomeScreen.kt`
- **Line:** 449
- **Status:** TODO - callback incomplete

### Required Implementation
1. Complete recipe search in sheet
2. Select recipe adds to meal slot
3. Handle duplicate recipes in same meal

### Acceptance Criteria
- [ ] Recipe search works in sheet
- [ ] Selecting recipe adds to meal
- [ ] Sheet closes after selection
- [ ] Meal plan updates immediately

### Code Location
- `presentation/home/HomeScreen.kt:449`

### Priority
**Medium** - Core feature completion

---

## Issue #17: Pass context to Chat screen for contextual assistance

**Labels:** `todo`, `android`, `low-priority`

### Summary
Chat screen should receive context about where user navigated from.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/navigation/RasoiNavHost.kt`
- **Line:** 145
- **Status:** TODO comment

### Required Implementation
1. Pass navigation context to ChatScreen
2. AI can provide contextual help
3. Example: From RecipeDetail, AI knows which recipe

### Acceptance Criteria
- [ ] Context parameter added to Chat route
- [ ] AI receives context in system prompt
- [ ] Contextual responses provided

### Code Location
- `presentation/navigation/RasoiNavHost.kt:145`

### Priority
**Low** - UX enhancement

---

## Issue #18: Implement more options menu in Recipe Detail

**Labels:** `todo`, `android`, `low-priority`

### Summary
More options menu button in Recipe Detail is a no-op.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/recipedetail/RecipeDetailScreen.kt`
- **Line:** 90
- **Status:** Stable no-op callback

### Required Implementation
1. Create options menu dropdown
2. Options: Share, Add to Collection, Report Issue, Print

### Acceptance Criteria
- [ ] Dropdown menu appears on tap
- [ ] Share recipe functionality
- [ ] Add to collection
- [ ] Other actions as needed

### Code Location
- `presentation/recipedetail/RecipeDetailScreen.kt:90`

### Priority
**Low** - UX enhancement

---

## Issue #19: Implement grocery list integration from Recipe Detail

**Labels:** `todo`, `android`, `medium-priority`

### Summary
Button to add recipe ingredients to grocery list is not implemented.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/recipedetail/RecipeDetailViewModel.kt`
- **Line:** 270
- **Status:** TODO comment

### Required Implementation
1. Add "Add to Grocery" button
2. Add selected/all ingredients to grocery list
3. Handle duplicates (increase quantity)

### Acceptance Criteria
- [ ] Button visible on recipe detail
- [ ] Adds ingredients to grocery list
- [ ] Duplicates increase quantity
- [ ] Success feedback shown
- [ ] Navigate to grocery option

### Code Location
- `presentation/recipedetail/RecipeDetailViewModel.kt:270`

### Priority
**Medium** - Feature enhancement

---

## Issue #20: Implement full achievements screen navigation

**Labels:** `todo`, `android`, `low-priority`

### Summary
Stats screen should navigate to full achievements screen.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/stats/StatsScreen.kt`
- **Line:** 75
- **Status:** TODO comment

### Required Implementation
1. Create AchievementsScreen with all achievements
2. Navigate from Stats "View All" button
3. Show locked/unlocked achievements with progress

### Acceptance Criteria
- [ ] AchievementsScreen created
- [ ] Navigation from Stats works
- [ ] All achievements displayed
- [ ] Progress toward locked achievements shown

### Code Location
- `presentation/stats/StatsScreen.kt:75`

### Priority
**Low** - Feature enhancement

---

## Issue #21: Implement full leaderboard screen navigation

**Labels:** `todo`, `android`, `low-priority`

### Summary
Stats screen should navigate to full leaderboard screen.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/stats/StatsScreen.kt`
- **Line:** 76
- **Status:** TODO comment

### Required Implementation
1. Create LeaderboardScreen with full rankings
2. Navigate from Stats "View All" button
3. Show user's position highlighted

### Acceptance Criteria
- [ ] LeaderboardScreen created
- [ ] Navigation from Stats works
- [ ] Full leaderboard displayed
- [ ] Current user highlighted

### Code Location
- `presentation/stats/StatsScreen.kt:76`

### Priority
**Low** - Feature enhancement

---

## Issue #22: Load cuisine breakdown from repository instead of mock data

**Labels:** `todo`, `android`, `medium-priority`

### Summary
Stats screen cuisine breakdown uses hardcoded mock data instead of real statistics.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/stats/StatsViewModel.kt`
- **Lines:** 109-114
- **Status:** Mock data: North 40%, South 27%, East 13%, West 20%

### Required Implementation
1. Calculate cuisine breakdown from cooked recipes
2. Store in StatsRepository
3. Load from repository in ViewModel

### Acceptance Criteria
- [ ] Cuisine breakdown calculated from history
- [ ] Stored in Room/backend
- [ ] Loaded dynamically
- [ ] Updates as user cooks more recipes

### Code Location
- `presentation/stats/StatsViewModel.kt:109`

### Priority
**Medium** - Data accuracy

---

## Issue #23: Implement Android share intent for Stats sharing

**Labels:** `todo`, `android`, `low-priority`

### Summary
Share achievement button shows error instead of opening share sheet.

### Current State
- **File:** `android/app/src/main/java/com/rasoiai/app/presentation/stats/StatsViewModel.kt`
- **Line:** 236
- **Status:** Shows error message placeholder

### Required Implementation
1. Create share intent with achievement text
2. Include optional image
3. Open Android share sheet

### Acceptance Criteria
- [ ] Share intent created
- [ ] Achievement text formatted nicely
- [ ] Share sheet opens with apps
- [ ] Sharing completes successfully

### Code Location
- `presentation/stats/StatsViewModel.kt:236`

### Priority
**Low** - Social feature

---

# TODO - DATA LAYER

## Issue #24: Use BuildConfig for API base URL

**Labels:** `todo`, `android`, `medium-priority`, `infrastructure`

### Summary
API base URL is hardcoded instead of using BuildConfig for environment switching.

### Current State
- **File:** `android/data/src/main/java/com/rasoiai/data/di/DataModule.kt`
- **Line:** 36-38
- **Status:** Hardcoded as "http://10.0.2.2:8000/"

### Required Implementation
1. Add BASE_URL to build.gradle.kts buildConfigField
2. Different URLs for debug/release
3. Read from DataModule

### Acceptance Criteria
- [ ] BuildConfig.BASE_URL used
- [ ] Debug: localhost/emulator URL
- [ ] Release: Production API URL
- [ ] Staging variant if needed

### Code Location
- `data/di/DataModule.kt:36`
- `app/build.gradle.kts`

### Priority
**Medium** - Required for release

---

## Issue #25: Implement refresh token endpoint integration

**Labels:** `todo`, `android`, `medium-priority`

### Summary
Auth token refresh is not implemented, causing re-login on token expiry.

### Current State
- **File:** `android/data/src/main/java/com/rasoiai/data/repository/AuthRepositoryImpl.kt`
- **Line:** 96
- **Status:** TODO - refresh not implemented

### Required Implementation
1. Backend: Add refresh token endpoint
2. Android: Call refresh before token expires
3. Handle refresh failure (logout)

### Acceptance Criteria
- [ ] Backend refresh endpoint created
- [ ] Android calls refresh automatically
- [ ] Token updated in DataStore
- [ ] Graceful logout on refresh failure

### Code Location
- `data/repository/AuthRepositoryImpl.kt:96`

### Priority
**Medium** - User experience

---

## Issue #26: Inject repositories into SyncWorker

**Labels:** `todo`, `android`, `medium-priority`

### Summary
SyncWorker has TODO for repository injection.

### Current State
- **File:** `android/data/src/main/java/com/rasoiai/data/sync/SyncWorker.kt`
- **Line:** 28
- **Status:** TODO comment

### Required Implementation
1. Use HiltWorker annotation
2. Inject required repositories
3. Enable proper dependency injection

### Acceptance Criteria
- [ ] @HiltWorker annotation added
- [ ] Repositories injected via @AssistedInject
- [ ] WorkerFactory configured

### Code Location
- `data/sync/SyncWorker.kt:28`

### Priority
**Medium** - Blocked by Issue #2

---

# TODO - BUILD/CONFIG

## Issue #27: Fix broad exception handling in repositories

**Labels:** `todo`, `android`, `code-quality`, `medium-priority`

### Summary
Multiple repositories use broad `catch (e: Exception)` which can hide bugs.

### Current State
Multiple files with untyped exception catches:
- `data/repository/RecipeRepositoryImpl.kt` - 7 instances
- `data/repository/FavoritesRepositoryImpl.kt` - 7 instances
- `data/repository/FakeMealPlanRepository.kt` - 4 instances
- `data/repository/FakePantryRepository.kt` - 2 instances

### Required Implementation
1. Identify specific exceptions to catch
2. Replace broad catches with specific types
3. Add proper error logging
4. Consider Result type for error handling

### Acceptance Criteria
- [ ] No broad `catch (e: Exception)` in production code
- [ ] Specific exceptions caught (IOException, HttpException, etc.)
- [ ] Errors logged with Timber
- [ ] Unexpected exceptions still crash (for debugging)

### Code Location
- `data/repository/*RepositoryImpl.kt`

### Priority
**Medium** - Code quality

---

# MISSING TESTS

## Issue #28: Add backend tests for untested services

**Labels:** `testing`, `enhancement`, `backend`, `medium-priority`

### Summary
Several backend services have no dedicated test files.

### Current State
Untested services:
- `app/services/grocery_service.py`
- `app/services/stats_service.py`
- `app/services/festival_service.py`
- `app/services/user_service.py`

### Required Implementation
Create test files with comprehensive coverage:
1. `tests/test_grocery_service.py`
2. `tests/test_stats_service.py`
3. `tests/test_festival_service.py`
4. `tests/test_user_service.py`

### Acceptance Criteria
- [ ] 80%+ coverage for each service
- [ ] Happy path tests
- [ ] Edge case tests
- [ ] Error handling tests
- [ ] All tests passing

### Code Location
- `backend/tests/`

### Priority
**Medium** - Test coverage

---

## Issue #29: Add UI tests for NotificationsScreen

**Labels:** `testing`, `enhancement`, `android`, `low-priority`

### Summary
NotificationsScreen has no UI tests.

### Current State
- **File:** Missing `NotificationsScreenTest.kt`
- **Status:** No tests

### Required Implementation
1. Create `NotificationsScreenTest.kt`
2. Test empty state
3. Test notification list display
4. Test interactions

### Acceptance Criteria
- [ ] Test file created
- [ ] Empty state test
- [ ] List display tests
- [ ] Interaction tests
- [ ] Minimum 10 tests

### Code Location
- `app/src/androidTest/java/com/rasoiai/app/presentation/notifications/`

### Priority
**Low** - Blocked by Issue #1

---

## Issue #30: Add UI tests for SplashScreen

**Labels:** `testing`, `enhancement`, `android`, `low-priority`

### Summary
SplashScreen has no UI tests.

### Current State
- **File:** Missing `SplashScreenTest.kt`
- **Status:** No tests

### Required Implementation
1. Create `SplashScreenTest.kt`
2. Test splash display
3. Test navigation after delay

### Acceptance Criteria
- [ ] Test file created
- [ ] Logo/branding displayed test
- [ ] Navigation tests
- [ ] Minimum 5 tests

### Code Location
- `app/src/androidTest/java/com/rasoiai/app/presentation/splash/`

### Priority
**Low** - Minor coverage gap

---

## Issue #31: Add more domain layer use case tests

**Labels:** `testing`, `enhancement`, `android`, `low-priority`

### Summary
Domain layer has minimal test coverage with only 1 use case tested.

### Current State
- Only `GetCurrentMealPlanUseCaseTest.kt` exists (2 tests)
- Many use cases untested

### Required Implementation
Add tests for use cases in `domain/src/main/java/com/rasoiai/domain/usecase/`

### Acceptance Criteria
- [ ] All use cases have test files
- [ ] Business logic validated
- [ ] Edge cases covered

### Code Location
- `domain/src/test/java/com/rasoiai/domain/usecase/`

### Priority
**Low** - Architecture purity

---

## Issue #32: Add E2E tests for Grocery, Favorites, and Settings flows

**Labels:** `testing`, `enhancement`, `android`, `medium-priority`

### Summary
E2E test coverage for Grocery, Favorites, and Settings screens is basic.

### Current State
- GroceryFlowTest: 5 tests (basic)
- FavoritesFlowTest: 5 tests (basic)
- SettingsFlowTest: 12 tests (moderate)

### Required Implementation
Expand E2E coverage similar to HomeScreen (24 tests):
1. Grocery: Add/edit/delete items, categories, WhatsApp share
2. Favorites: Collections, search, filters
3. Settings: All preference changes, sign out flow

### Acceptance Criteria
- [ ] Grocery: 15+ E2E tests
- [ ] Favorites: 15+ E2E tests
- [ ] Settings: 20+ E2E tests
- [ ] All flows tested end-to-end

### Code Location
- `app/src/androidTest/java/com/rasoiai/app/e2e/flows/`

### Priority
**Medium** - Test coverage

---

# Summary by Priority

## High Priority (4 issues)
1. Issue #1: NotificationsScreen implementation
2. Issue #2: SyncWorker implementation
3. Issue #3: FCM Service for push notifications
4. Issue #4: Release signing configuration

## Medium Priority (14 issues)
5. Issue #5: Voice input for Chat
6. Issue #6: Photo attachment for Chat
7. Issue #7: Camera capture for Pantry
8. Issue #8: Gallery selection for Pantry
9. Issue #9: Items per meal dialog
13. Issue #13: Meal plan integration in Chat
14. Issue #14: Recipe rating submission
16. Issue #16: Recipe selection in AddRecipe
19. Issue #19: Grocery list integration
22. Issue #22: Cuisine breakdown from repository
24. Issue #24: BuildConfig for API URL
25. Issue #25: Refresh token endpoint
27. Issue #27: Fix broad exception handling
28. Issue #28: Backend service tests
32. Issue #32: E2E test expansion

## Low Priority (14 issues)
10. Issue #10: Terms URL
11. Issue #11: Privacy URL
12. Issue #12: Settings from Chat
15. Issue #15: Festival recipes navigation
17. Issue #17: Chat context passing
18. Issue #18: Recipe Detail options menu
20. Issue #20: Achievements screen
21. Issue #21: Leaderboard screen
23. Issue #23: Share intent
26. Issue #26: SyncWorker DI
29. Issue #29: Notifications UI tests
30. Issue #30: Splash UI tests
31. Issue #31: Use case tests

---

# Labels Reference

Create these labels in your GitHub repository:

| Label | Color | Description |
|-------|-------|-------------|
| `not-implemented` | `#d73a4a` | Feature not yet implemented |
| `enhancement` | `#a2eeef` | New feature or request |
| `todo` | `#fbca04` | TODO comment in code |
| `testing` | `#0e8a16` | Testing related |
| `android` | `#3DDC84` | Android app related |
| `backend` | `#1d76db` | Backend related |
| `infrastructure` | `#5319e7` | Build/CI/CD related |
| `code-quality` | `#c5def5` | Code quality improvement |
| `high-priority` | `#b60205` | High priority |
| `medium-priority` | `#fbca04` | Medium priority |
| `low-priority` | `#0e8a16` | Low priority |

---

*Generated by Claude Code - February 2, 2026*
*Total: 32 issues covering all missing, partial, and TODO items*
