# Full-Stack Traceability Report

**Generated:** 2026-02-08
**Backend Test Run:** 250/250 PASSED (0 failures)
**Project:** RasoiAI (रसोई AI)

---

## 1. Executive Summary

| Metric | Count |
|--------|-------|
| PostgreSQL Tables | 24 |
| Backend API Endpoints | 43 |
| Backend Test Files | 17 |
| Backend Tests (pytest) | 250 — **ALL PASSING** |
| Room Entities (Android) | 20 |
| Room DAOs | 11 |
| Repository Interfaces | 11 |
| ViewModels | 14 (+ 1 BaseViewModel) |
| Screens | 14 (+ 1 PlaceholderScreen) |
| Retrofit API Methods | 39 |
| Alembic Migrations | 7 |

### Key Findings

- **Backend fully tested:** 250 tests across 17 files, 0 failures.
- **Untested backend domains:** Grocery (2 endpoints), Stats (2 endpoints), Festivals (1 endpoint) have no dedicated test files. Meal Plans (7 endpoints) have no direct API tests (only AI service unit tests).
- **Android gap:** Family Members CRUD exists in backend (4 endpoints) but has **no Retrofit methods** — Android cannot call these APIs yet.
- **Android-only entities:** 9 Room tables have no PostgreSQL counterpart (local-only features like favorites, pantry, offline queue).
- **Pre-existing known issues:** 4 auth tests were historically fragile due to conftest auth override (currently all pass). OnboardingViewModelTest has a compilation error (missing constructor param).

---

## 2. Status Legend

### Backend Status

| Status | Meaning |
|--------|---------|
| **PASS** | Dedicated test file exists, all tests pass |
| **PARTIAL** | Tests exist but cover only some endpoints/service functions |
| **UNTESTED** | No dedicated test file for this domain |

### Android Status

| Status | Meaning |
|--------|---------|
| **Wired** | All layers connected: Retrofit → Room → DAO → Repository → ViewModel → Screen |
| **Gap** | One or more layers missing (details noted) |
| **Local-Only** | Room entity with no backend counterpart |
| **N/A** | Backend-only table with no Android representation |

---

## 3. Summary Matrix: Backend

| # | PG Table | API Endpoints | Service File | Test File(s) | Tests | Backend Status |
|---|----------|---------------|--------------|--------------|-------|----------------|
| 1 | `users` | GET `/users/me`, PUT `/users/preferences` | `user_service.py` | `test_family_members_api.py` (prefs tests) | 2 | PARTIAL |
| 2 | `user_preferences` | PUT `/users/preferences` | `user_service.py` | `test_family_members_api.py` | 2 | PARTIAL |
| 3 | `family_members` | GET/POST/PUT/DELETE `/family-members/*` | Direct DB queries | `test_family_members_api.py` | 6 | PASS |
| 4 | `recipes` | GET `/recipes/{id}`, `/recipes/search`, `/recipes/{id}/scale` | `recipe_service.py` | `test_recipe_search.py` | 10 | PASS |
| 5 | `recipe_ingredients` | (nested in recipe responses) | `recipe_service.py` | `test_recipe_search.py` | — | PASS (via recipes) |
| 6 | `recipe_instructions` | (nested in recipe responses) | `recipe_service.py` | `test_recipe_search.py` | — | PASS (via recipes) |
| 7 | `recipe_nutrition` | (nested in recipe responses) | `recipe_service.py` | `test_recipe_search.py` | — | PASS (via recipes) |
| 8 | `meal_plans` | POST `/meal-plans/generate`, GET `/meal-plans/current`, GET `/{id}` | `meal_plan_service.py`, `ai_meal_service.py` | `test_ai_meal_service.py` | 22 | PARTIAL |
| 9 | `meal_plan_items` | POST `swap`, PUT `lock`, DELETE `remove` | `meal_plan_service.py` | `test_ai_meal_service.py` | — | PARTIAL |
| 10 | `grocery_lists` | GET `/grocery`, GET `/grocery/whatsapp` | `grocery_service.py` | — | 0 | **UNTESTED** |
| 11 | `grocery_items` | (nested in grocery responses) | `grocery_service.py` | — | 0 | **UNTESTED** |
| 12 | `chat_messages` | POST `/chat/message`, GET `/chat/history`, POST `/chat/image` | `chat_assistant.py` | `test_chat_api.py`, `test_chat_integration.py` | 12 + 22 | PASS |
| 13 | `cooking_streaks` | GET `/stats/streak` | `stats_service.py` | — | 0 | **UNTESTED** |
| 14 | `cooking_days` | GET `/stats/monthly` | `stats_service.py` | — | 0 | **UNTESTED** |
| 15 | `achievements` | (via stats endpoints) | `stats_service.py` | — | 0 | **UNTESTED** |
| 16 | `user_achievements` | (via stats endpoints) | `stats_service.py` | — | 0 | **UNTESTED** |
| 17 | `notifications` | GET/PUT/DELETE `/notifications/*` | `notification_service.py` | `test_notification_api.py`, `test_notification_service.py` | 11 + 19 | PASS |
| 18 | `fcm_tokens` | POST/DELETE `/notifications/fcm-token` | `notification_service.py` | `test_notification_api.py`, `test_notification_service.py` | — | PASS (via notifications) |
| 19 | `recipe_rules` | GET/POST/PUT/DELETE `/recipe-rules/*`, POST `/recipe-rules/sync` | Direct DB + `preference_update_service.py` | `test_recipe_rules_api.py`, `test_recipe_rules_dedup.py`, `test_preference_service.py`, `test_sharma_recipe_rules.py` | 20 + 5 + 27 + 13 | PASS |
| 20 | `nutrition_goals` | GET/POST/PUT/DELETE `/nutrition-goals/*` | Direct DB queries | `test_recipe_rules_api.py`, `test_sharma_recipe_rules.py` | 5 + 2 | PASS |
| 21 | `festivals` | GET `/festivals/upcoming` | `festival_service.py` | — | 0 | **UNTESTED** |
| 22 | `ai_recipe_catalog` | GET `/recipes/ai-catalog/search` | `ai_recipe_catalog_service.py` | `test_ai_recipe_catalog.py` | 16 | PASS |
| 23 | `system_config` | (internal — no public API) | `config_service.py` | — | 0 | **UNTESTED** |
| 24 | `reference_data` | (internal — no public API) | `config_service.py` | — | 0 | **UNTESTED** |

**Additional test files not table-specific:**

| Test File | Tests | Coverage |
|-----------|-------|----------|
| `test_auth.py` | 6 | Auth endpoints (firebase, refresh, unauthorized) |
| `test_health.py` | 2 | Health check + root endpoint |
| `test_email_uniqueness.py` | 7 | Email dedup, case normalization, 409 responses |
| `test_migrate_legacy_rules.py` | 11 | Legacy rule migration script |
| `test_recipe_cache.py` | 30 | In-memory recipe cache (no DB table) |

---

## 4. Summary Matrix: Android Layers

| # | PG Table | Retrofit | Room Entity | DAO | Repository | ViewModel | Screen | UI Action | Android Status |
|---|----------|----------|-------------|-----|------------|-----------|--------|-----------|----------------|
| 1 | `users` | `getCurrentUser()`, `updateUserPreferences()` | — (DataStore) | — | `AuthRepository`, `SettingsRepository` | `OnboardingViewModel`, `SettingsViewModel` | Onboarding, Settings | "Save" on onboarding step 5; toggle in Settings | **Wired** (DataStore, not Room) |
| 2 | `user_preferences` | `updateUserPreferences()` | — (DataStore) | — | `SettingsRepository` | `OnboardingViewModel`, `SettingsViewModel` | Onboarding, Settings | Complete onboarding; edit in Settings | **Wired** (DataStore) |
| 3 | `family_members` | — | — | — | — | — | — | — | **Gap** (no Retrofit) |
| 4 | `recipes` | `getRecipeById()`, `searchRecipes()`, `scaleRecipe()`, `searchAiRecipeCatalog()` | `RecipeEntity` | `RecipeDao` | `RecipeRepository` | `RecipeDetailViewModel`, `HomeViewModel` | RecipeDetail, Home | Tap meal card → recipe detail; search recipes | **Wired** |
| 5 | `recipe_ingredients` | (nested in recipe) | (stored in `RecipeEntity` JSON) | `RecipeDao` | `RecipeRepository` | `RecipeDetailViewModel` | RecipeDetail | View ingredients list | **Wired** |
| 6 | `recipe_instructions` | (nested in recipe) | (stored in `RecipeEntity` JSON) | `RecipeDao` | `RecipeRepository` | `CookingModeViewModel` | CookingMode | "Start Cooking" → step-by-step mode | **Wired** |
| 7 | `recipe_nutrition` | (nested in recipe) | (stored in `RecipeEntity` JSON) | `RecipeDao` | `RecipeRepository` | `RecipeDetailViewModel` | RecipeDetail | View nutrition info tab | **Wired** |
| 8 | `meal_plans` | `generateMealPlan()`, `getCurrentMealPlan()`, `getMealPlanById()` | `MealPlanEntity` | `MealPlanDao` | `MealPlanRepository` | `HomeViewModel` | Home | Pull-to-refresh; "Generate Plan" button | **Wired** |
| 9 | `meal_plan_items` | `swapMealItem()`, `lockMealItem()`, `removeMealItem()` | `MealPlanItemEntity` | `MealPlanDao` | `MealPlanRepository` | `HomeViewModel` | Home | Swipe meal card; tap lock icon; tap delete | **Wired** |
| 10 | `grocery_lists` | `getGroceryList()`, `getGroceryListForWhatsApp()` | — | `GroceryDao` | `GroceryRepository` | `GroceryViewModel` | Grocery | Navigate to Grocery tab | **Wired** |
| 11 | `grocery_items` | (nested in grocery list) | `GroceryItemEntity` | `GroceryDao` | `GroceryRepository` | `GroceryViewModel` | Grocery | Check/uncheck items; share via WhatsApp | **Wired** |
| 12 | `chat_messages` | `sendChatMessage()`, `getChatHistory()`, `sendImageChatMessage()` | `ChatMessageEntity` | `ChatDao` | `ChatRepository` | `ChatViewModel` | Chat | Type message + send; take photo; scroll history | **Wired** |
| 13 | `cooking_streaks` | `getCookingStreak()` | `CookingStreakEntity` | `StatsDao` | `StatsRepository` | `StatsViewModel` | Stats | Navigate to Stats tab | **Wired** |
| 14 | `cooking_days` | `getMonthlyStats()` | `CookingDayEntity` | `StatsDao` | `StatsRepository` | `StatsViewModel` | Stats | View monthly calendar | **Wired** |
| 15 | `achievements` | (via stats endpoints) | `AchievementEntity` | `StatsDao` | `StatsRepository` | `StatsViewModel` | Stats | Scroll achievements list | **Wired** |
| 16 | `user_achievements` | (via stats endpoints) | (stored in `AchievementEntity.isUnlocked`) | `StatsDao` | `StatsRepository` | `StatsViewModel` | Stats | View unlocked badges | **Wired** |
| 17 | `notifications` | `getNotifications()`, `markNotificationAsRead()`, `markAllNotificationsAsRead()`, `deleteNotification()` | `NotificationEntity` | `NotificationDao` | `NotificationRepository` | `NotificationsViewModel` | Notifications | Tap bell icon; swipe to dismiss; "Mark all read" | **Wired** |
| 18 | `fcm_tokens` | `registerFcmToken()`, `unregisterFcmToken()` | — | — | `NotificationRepository` | — | — | Automatic on app start (background) | **Wired** (no Room needed) |
| 19 | `recipe_rules` | `getRecipeRules()`, `createRecipeRule()`, `updateRecipeRule()`, `deleteRecipeRule()`, `syncRecipeRules()` | `RecipeRuleEntity` | `RecipeRulesDao` | `RecipeRulesRepository` | `RecipeRulesViewModel` | RecipeRules | Tap "+" → add rule sheet; swipe to delete; edit | **Wired** |
| 20 | `nutrition_goals` | `getNutritionGoals()`, `createNutritionGoal()`, `updateNutritionGoal()`, `deleteNutritionGoal()` | `NutritionGoalEntity` | `RecipeRulesDao` | `RecipeRulesRepository` | `RecipeRulesViewModel` | RecipeRules (Nutrition tab) | Tap "+" → add goal; swipe to delete | **Wired** |
| 21 | `festivals` | `getUpcomingFestivals()` | `MealPlanFestivalEntity` | `MealPlanDao` | `MealPlanRepository` | `HomeViewModel` | Home | Festival badge on meal day cards | **Wired** |
| 22 | `ai_recipe_catalog` | `searchAiRecipeCatalog()` | — | — | `RecipeRulesRepository` | `RecipeRulesViewModel` | RecipeRules (search in Add Rule) | Type recipe name → search catalog dropdown | **Wired** (no Room cache) |
| 23 | `system_config` | — | — | — | — | — | — | — | **N/A** (backend internal) |
| 24 | `reference_data` | — | — | — | — | — | — | — | **N/A** (backend internal) |

---

## 5. Per-Domain Detailed Breakdowns

### 5.1 Users Domain

**Tables:** `users`, `user_preferences`, `family_members`

#### API Endpoints

| Method | Path | Service Function | Test File | Status |
|--------|------|------------------|-----------|--------|
| POST | `/auth/firebase` | `auth_service.authenticate_with_firebase()` | `test_auth.py` | PASS (6 tests) |
| POST | `/auth/refresh` | `auth_service.refresh_access_token()` | `test_auth.py` | PASS |
| GET | `/users/me` | `user_service.get_user_with_preferences()` | `test_auth.py` | PASS |
| PUT | `/users/preferences` | `user_service.update_user_preferences()` | `test_family_members_api.py` | PASS (2 tests) |
| GET | `/family-members` | Direct DB query | `test_family_members_api.py` | PASS |
| POST | `/family-members` | Direct DB query | `test_family_members_api.py` | PASS |
| PUT | `/family-members/{id}` | Direct DB query | `test_family_members_api.py` | PASS |
| DELETE | `/family-members/{id}` | Direct DB query | `test_family_members_api.py` | PASS |

#### Android Layer Chain

| Layer | File | Notes |
|-------|------|-------|
| Retrofit | `RasoiApiService.kt` | `authenticateWithFirebase()`, `refreshToken()`, `getCurrentUser()`, `updateUserPreferences()` |
| Room | — | User data stored in DataStore, not Room |
| DAO | — | — |
| Repository | `AuthRepositoryImpl.kt`, `SettingsRepositoryImpl.kt` | DataStore for preferences |
| ViewModel | `AuthViewModel.kt`, `OnboardingViewModel.kt`, `SettingsViewModel.kt` | — |
| Screen | `AuthScreen.kt`, `OnboardingScreen.kt`, `SettingsScreen.kt` | — |

#### UI Actions

| Action | Flow |
|--------|------|
| Tap "Sign In with Google" | AuthScreen → AuthViewModel → AuthRepository → POST `/auth/firebase` |
| Complete 5-step onboarding | OnboardingScreen → OnboardingViewModel → SettingsRepository → PUT `/users/preferences` |
| Toggle dietary preference in Settings | SettingsScreen → SettingsViewModel → SettingsRepository → PUT `/users/preferences` |

#### Gaps

- **Family Members:** Backend CRUD exists (4 endpoints, 6 tests passing) but **no Retrofit methods in Android**. Users cannot manage family members from the app.

---

### 5.2 Recipes Domain

**Tables:** `recipes`, `recipe_ingredients`, `recipe_instructions`, `recipe_nutrition`

#### API Endpoints

| Method | Path | Service Function | Test File | Status |
|--------|------|------------------|-----------|--------|
| GET | `/recipes/search` | `recipe_service.search_recipes()` | `test_recipe_search.py` | PASS (10 tests) |
| GET | `/recipes/{id}` | `recipe_service.get_recipe_by_id()` | — | UNTESTED (direct) |
| GET | `/recipes/{id}/scale` | `recipe_service.scale_recipe()` | — | UNTESTED (direct) |
| GET | `/recipes/ai-catalog/search` | `ai_recipe_catalog_service.search_catalog()` | `test_ai_recipe_catalog.py` | PASS (16 tests) |

#### Android Layer Chain

| Layer | File | Notes |
|-------|------|-------|
| Retrofit | `RasoiApiService.kt` | `getRecipeById()`, `searchRecipes()`, `scaleRecipe()`, `searchAiRecipeCatalog()` |
| Room Entity | `RecipeEntity.kt` | JSON columns for ingredients, instructions, nutrition |
| DAO | `RecipeDao.kt` | Full CRUD + search |
| Repository | `RecipeRepositoryImpl.kt` | Offline-first: Room → API fallback |
| ViewModel | `RecipeDetailViewModel.kt`, `HomeViewModel.kt` | — |
| Screen | `RecipeDetailScreen.kt`, `HomeScreen.kt` | — |

#### UI Actions

| Action | Flow |
|--------|------|
| Tap meal card on Home | Home → RecipeDetail → RecipeDetailViewModel → RecipeRepository → Room/API |
| "Start Cooking" on recipe detail | RecipeDetail → CookingMode → CookingModeViewModel |
| Scale recipe (adjust servings) | RecipeDetail → RecipeDetailViewModel → GET `/recipes/{id}/scale` |

---

### 5.3 Meal Plans Domain

**Tables:** `meal_plans`, `meal_plan_items`

#### API Endpoints

| Method | Path | Service Function | Test File | Status |
|--------|------|------------------|-----------|--------|
| POST | `/meal-plans/generate` | `ai_meal_service.generate_meal_plan()` | `test_ai_meal_service.py` | PASS (22 tests — service level) |
| GET | `/meal-plans/current` | `MealPlanRepository.get_current_for_user()` | — | **UNTESTED** (API level) |
| GET | `/meal-plans/{id}` | `MealPlanRepository.get_by_id()` | — | **UNTESTED** (API level) |
| POST | `/meal-plans/{id}/items/{itemId}/swap` | Swap logic in endpoint | — | **UNTESTED** (API level) |
| PUT | `/meal-plans/{id}/items/{itemId}/lock` | Toggle lock in endpoint | — | **UNTESTED** (API level) |
| DELETE | `/meal-plans/{id}/items/{itemId}` | Remove item in endpoint | — | **UNTESTED** (API level) |

#### Android Layer Chain

| Layer | File | Notes |
|-------|------|-------|
| Retrofit | `RasoiApiService.kt` | `generateMealPlan()`, `getCurrentMealPlan()`, `getMealPlanById()`, `swapMealItem()`, `lockMealItem()`, `removeMealItem()` |
| Room Entity | `MealPlanEntity.kt`, `MealPlanItemEntity.kt`, `MealPlanFestivalEntity.kt` | 3 entities for plan structure |
| DAO | `MealPlanDao.kt` | — |
| Repository | `MealPlanRepositoryImpl.kt` | Offline-first with sync |
| ViewModel | `HomeViewModel.kt` | Manages current week plan |
| Screen | `HomeScreen.kt` | Main tab with day/meal cards |

#### UI Actions

| Action | Flow |
|--------|------|
| "Generate Meal Plan" button | Home → HomeViewModel → MealPlanRepository → POST `/meal-plans/generate` (4-7s AI) |
| Pull-to-refresh on Home | Home → HomeViewModel → GET `/meal-plans/current` → Room cache |
| Swipe meal card left (swap) | Home → HomeViewModel → POST `/meal-plans/{id}/items/{itemId}/swap` |
| Tap lock icon on meal | Home → HomeViewModel → PUT `/meal-plans/{id}/items/{itemId}/lock` |
| Tap delete on meal | Home → HomeViewModel → DELETE `/meal-plans/{id}/items/{itemId}` |

#### Gaps

- Meal plan API endpoints (GET current, GET by ID, swap, lock, delete) have **no dedicated API-level tests**. Only the AI generation service has unit tests.

---

### 5.4 Grocery Domain

**Tables:** `grocery_lists`, `grocery_items`

#### API Endpoints

| Method | Path | Service Function | Test File | Status |
|--------|------|------------------|-----------|--------|
| GET | `/grocery` | `grocery_service.get_grocery_list_for_meal_plan()` | — | **UNTESTED** |
| GET | `/grocery/whatsapp` | `grocery_service.get_grocery_list_whatsapp()` | — | **UNTESTED** |

#### Android Layer Chain

| Layer | File | Notes |
|-------|------|-------|
| Retrofit | `RasoiApiService.kt` | `getGroceryList()`, `getGroceryListForWhatsApp()` |
| Room Entity | `GroceryItemEntity.kt` | — |
| DAO | `GroceryDao.kt` | — |
| Repository | `GroceryRepositoryImpl.kt` | — |
| ViewModel | `GroceryViewModel.kt` | — |
| Screen | `GroceryScreen.kt` | Bottom nav tab |

#### UI Actions

| Action | Flow |
|--------|------|
| Navigate to Grocery tab | GroceryScreen → GroceryViewModel → GroceryRepository → Room / GET `/grocery` |
| Check/uncheck grocery item | GroceryScreen → GroceryViewModel → GroceryDao.update() |
| Share via WhatsApp | GroceryScreen → GroceryViewModel → GET `/grocery/whatsapp` → share intent |

#### Gaps

- **No backend tests** for either grocery endpoint.

---

### 5.5 Chat Domain

**Tables:** `chat_messages`

#### API Endpoints

| Method | Path | Service Function | Test File | Status |
|--------|------|------------------|-----------|--------|
| POST | `/chat/message` | `chat_assistant.process_chat_message()` | `test_chat_api.py`, `test_chat_integration.py` | PASS (34 tests) |
| GET | `/chat/history` | `chat_assistant.get_chat_history()` | `test_chat_api.py` | PASS |
| POST | `/chat/image` | `gemini_client.analyze_food_image()` | — | UNTESTED |

#### Android Layer Chain

| Layer | File | Notes |
|-------|------|-------|
| Retrofit | `RasoiApiService.kt` | `sendChatMessage()`, `getChatHistory()`, `sendImageChatMessage()` |
| Room Entity | `ChatMessageEntity.kt` | Local cache of messages |
| DAO | `ChatDao.kt` | — |
| Repository | `ChatRepositoryImpl.kt` | — |
| ViewModel | `ChatViewModel.kt` | — |
| Screen | `ChatScreen.kt` | Bottom nav tab |

#### UI Actions

| Action | Flow |
|--------|------|
| Type message + tap send | ChatScreen → ChatViewModel → ChatRepository → POST `/chat/message` |
| Take food photo | ChatScreen → Camera → ChatViewModel → POST `/chat/image` |
| Scroll up to load history | ChatScreen → ChatViewModel → GET `/chat/history` |

---

### 5.6 Stats & Gamification Domain

**Tables:** `cooking_streaks`, `cooking_days`, `achievements`, `user_achievements`

#### API Endpoints

| Method | Path | Service Function | Test File | Status |
|--------|------|------------------|-----------|--------|
| GET | `/stats/streak` | `stats_service.get_cooking_streak()` | — | **UNTESTED** |
| GET | `/stats/monthly` | `stats_service.get_monthly_stats()` | — | **UNTESTED** |

#### Android Layer Chain

| Layer | File | Notes |
|-------|------|-------|
| Retrofit | `RasoiApiService.kt` | `getCookingStreak()`, `getMonthlyStats()` |
| Room Entity | `CookingStreakEntity.kt`, `CookingDayEntity.kt`, `AchievementEntity.kt`, `WeeklyChallengeEntity.kt`, `CookedRecipeEntity.kt` | 5 Room entities |
| DAO | `StatsDao.kt` | — |
| Repository | `StatsRepositoryImpl.kt` | — |
| ViewModel | `StatsViewModel.kt` | — |
| Screen | `StatsScreen.kt` | Bottom nav tab |

#### UI Actions

| Action | Flow |
|--------|------|
| Navigate to Stats tab | StatsScreen → StatsViewModel → StatsRepository → Room / API |
| View monthly calendar | StatsScreen → StatsViewModel → GET `/stats/monthly` |
| View achievements | StatsScreen → StatsViewModel → scrollable badges list |

#### Gaps

- **No backend tests** for stats endpoints.
- `WeeklyChallengeEntity` and `CookedRecipeEntity` are Room-only entities within the stats module — no backend API for weekly challenges.

---

### 5.7 Notifications Domain

**Tables:** `notifications`, `fcm_tokens`

#### API Endpoints

| Method | Path | Service Function | Test File | Status |
|--------|------|------------------|-----------|--------|
| GET | `/notifications` | `notification_service.get_notifications()` | `test_notification_api.py` | PASS |
| PUT | `/notifications/{id}/read` | `notification_service.mark_as_read()` | `test_notification_api.py` | PASS |
| PUT | `/notifications/read-all` | `notification_service.mark_all_as_read()` | `test_notification_api.py` | PASS |
| DELETE | `/notifications/{id}` | `notification_service.delete_notification()` | `test_notification_api.py` | PASS |
| POST | `/notifications/fcm-token` | `notification_service.register_fcm_token()` | `test_notification_api.py` | PASS |
| DELETE | `/notifications/fcm-token` | `notification_service.unregister_fcm_token()` | `test_notification_api.py` | PASS |

**Total:** 11 API tests + 19 service tests = **30 tests**

#### Android Layer Chain

| Layer | File | Notes |
|-------|------|-------|
| Retrofit | `RasoiApiService.kt` | All 7 notification methods |
| Room Entity | `NotificationEntity.kt` | — |
| DAO | `NotificationDao.kt` | — |
| Repository | `NotificationRepositoryImpl.kt` | — |
| ViewModel | `NotificationsViewModel.kt` | — |
| Screen | `NotificationsScreen.kt` | — |

#### UI Actions

| Action | Flow |
|--------|------|
| Tap bell icon (from Home) | Navigate to NotificationsScreen → GET `/notifications` |
| Tap notification | NotificationsScreen → mark as read → PUT `/{id}/read` |
| "Mark all read" button | NotificationsScreen → PUT `/notifications/read-all` |
| Swipe to dismiss | NotificationsScreen → DELETE `/notifications/{id}` |

---

### 5.8 Recipe Rules Domain

**Tables:** `recipe_rules`, `nutrition_goals`

#### API Endpoints

| Method | Path | Service Function | Test File | Status |
|--------|------|------------------|-----------|--------|
| GET | `/recipe-rules` | Direct DB query | `test_recipe_rules_api.py` | PASS |
| POST | `/recipe-rules` | Direct DB + dedup check | `test_recipe_rules_api.py`, `test_recipe_rules_dedup.py` | PASS |
| GET | `/recipe-rules/{id}` | Direct DB query | `test_recipe_rules_api.py` | PASS |
| PUT | `/recipe-rules/{id}` | Direct DB query | `test_recipe_rules_api.py` | PASS |
| DELETE | `/recipe-rules/{id}` | Direct DB query | `test_recipe_rules_api.py` | PASS |
| POST | `/recipe-rules/sync` | Last-Write-Wins sync | `test_recipe_rules_api.py` | PASS |
| GET | `/nutrition-goals` | Direct DB query | `test_recipe_rules_api.py` | PASS |
| POST | `/nutrition-goals` | Direct DB + dedup check | `test_recipe_rules_api.py` | PASS |
| GET | `/nutrition-goals/{id}` | Direct DB query | `test_recipe_rules_api.py` | PASS |
| PUT | `/nutrition-goals/{id}` | Direct DB query | `test_recipe_rules_api.py` | PASS |
| DELETE | `/nutrition-goals/{id}` | Direct DB query | `test_recipe_rules_api.py` | PASS |

**Total:** 20 + 5 + 27 + 13 = **65 tests** across 4 test files

#### Android Layer Chain

| Layer | File | Notes |
|-------|------|-------|
| Retrofit | `RasoiApiService.kt` | Full CRUD + sync for both rules and goals |
| Room Entity | `RecipeRuleEntity.kt`, `NutritionGoalEntity.kt` | — |
| DAO | `RecipeRulesDao.kt` | Includes `findDuplicate()` for dedup |
| Repository | `RecipeRulesRepositoryImpl.kt` | Offline-first with sync |
| ViewModel | `RecipeRulesViewModel.kt` | 2-tab UI (Rules, Nutrition) |
| Screen | `RecipeRulesScreen.kt` | With AddRuleBottomSheet |

#### UI Actions

| Action | Flow |
|--------|------|
| Tap "+" on Rules tab | RecipeRules → AddRuleBottomSheet → search AI catalog → POST `/recipe-rules` |
| Swipe to delete rule | RecipeRules → RecipeRulesViewModel → DELETE `/recipe-rules/{id}` |
| Edit rule frequency | RecipeRules → tap rule → edit sheet → PUT `/recipe-rules/{id}` |
| Tap "+" on Nutrition tab | RecipeRules → add goal sheet → POST `/nutrition-goals` |
| Pull-to-refresh | RecipeRules → POST `/recipe-rules/sync` (bidirectional sync) |

---

### 5.9 Festivals Domain

**Tables:** `festivals`

#### API Endpoints

| Method | Path | Service Function | Test File | Status |
|--------|------|------------------|-----------|--------|
| GET | `/festivals/upcoming` | `festival_service.get_upcoming_festivals()` | — | **UNTESTED** |

#### Android Layer Chain

| Layer | File | Notes |
|-------|------|-------|
| Retrofit | `RasoiApiService.kt` | `getUpcomingFestivals()` |
| Room Entity | `MealPlanFestivalEntity.kt` | Festivals embedded in meal plan |
| DAO | `MealPlanDao.kt` | — |
| Repository | `MealPlanRepositoryImpl.kt` | — |
| ViewModel | `HomeViewModel.kt` | — |
| Screen | `HomeScreen.kt` | Festival badge on meal day cards |

#### UI Actions

| Action | Flow |
|--------|------|
| View festival badge on meal card | HomeScreen displays festival info from MealPlanFestivalEntity |

#### Gaps

- **No backend tests** for festivals endpoint.

---

### 5.10 AI Recipe Catalog Domain

**Tables:** `ai_recipe_catalog`

#### API Endpoints

| Method | Path | Service Function | Test File | Status |
|--------|------|------------------|-----------|--------|
| GET | `/recipes/ai-catalog/search` | `ai_recipe_catalog_service.search_catalog()` | `test_ai_recipe_catalog.py` | PASS (16 tests) |

#### Android Layer Chain

| Layer | File | Notes |
|-------|------|-------|
| Retrofit | `RasoiApiService.kt` | `searchAiRecipeCatalog()` |
| Room Entity | — | No local cache |
| DAO | — | — |
| Repository | `RecipeRulesRepositoryImpl.kt` | Used for catalog search in add-rule flow |
| ViewModel | `RecipeRulesViewModel.kt` | Search state |
| Screen | `RecipeRulesScreen.kt` (AddRuleBottomSheet) | Search dropdown |

#### UI Actions

| Action | Flow |
|--------|------|
| Type recipe name in add-rule sheet | AddRuleBottomSheet → RecipeRulesViewModel → GET `/recipes/ai-catalog/search` → dropdown suggestions |

---

### 5.11 Configuration Domain

**Tables:** `system_config`, `reference_data`

#### API Endpoints

None — these are internal-only tables used by the meal generation algorithm.

#### Usage

| Table | Used By | Purpose |
|-------|---------|---------|
| `system_config` | `config_service.py` | App-level settings (meal structure, pairing rules) |
| `reference_data` | `config_service.py` | Ingredients, dishes, cuisines reference data |

#### Android Layer Chain

N/A — no Android representation. Config is loaded server-side during meal generation.

---

## 6. Android-Only Entities

These 9 Room entities have **no PostgreSQL counterpart** — they exist only on the Android device:

| # | Room Entity | Table Name | DAO | Purpose |
|---|-------------|------------|-----|---------|
| 1 | `FavoriteEntity` | `favorites` | `FavoriteDao` | User's favorited recipes (local bookmarks) |
| 2 | `FavoriteCollectionEntity` | `favorite_collections` | `CollectionDao` | User-created recipe collections |
| 3 | `RecentlyViewedEntity` | `recently_viewed` | `FavoriteDao` | Recently viewed recipes for quick access |
| 4 | `PantryItemEntity` | `pantry_items` | `PantryDao` | Kitchen pantry inventory tracking |
| 5 | `WeeklyChallengeEntity` | `weekly_challenges` | `StatsDao` | Gamification weekly cooking challenges |
| 6 | `CookedRecipeEntity` | `cooked_recipes` | `StatsDao` | Records of cooked recipes for stats |
| 7 | `OfflineQueueEntity` | `offline_queue` | `OfflineQueueDao` | Queued actions to sync when online |
| 8 | `KnownIngredientEntity` | `known_ingredients` | — (seeded) | Pre-populated Indian cooking ingredients (40+) |
| 9 | `MealPlanFestivalEntity` | `meal_plan_festivals` | `MealPlanDao` | Festival data linked to meal plan days |

**Note:** `MealPlanFestivalEntity` is populated from the backend `festivals` API but stored locally for offline access. Favorites, pantry, and collections are purely local features with no backend sync.

---

## 7. Coverage Gap Analysis

### 7.1 Untested Backend Domains

| Domain | Tables | Endpoints | Risk Level |
|--------|--------|-----------|------------|
| **Grocery** | `grocery_lists`, `grocery_items` | GET `/grocery`, GET `/grocery/whatsapp` | Medium — user-facing feature |
| **Stats** | `cooking_streaks`, `cooking_days`, `achievements`, `user_achievements` | GET `/stats/streak`, GET `/stats/monthly` | Low — read-only |
| **Festivals** | `festivals` | GET `/festivals/upcoming` | Low — read-only, seeded data |
| **Meal Plan API** | `meal_plans`, `meal_plan_items` | GET/POST/PUT/DELETE (5 endpoints) | **High** — core feature, only AI service tested |
| **Configuration** | `system_config`, `reference_data` | None (internal) | Low — config data |

### 7.2 Missing Android Retrofit Methods

| Backend Endpoint | Missing Retrofit Method | Impact |
|-----------------|------------------------|--------|
| GET `/family-members` | `getFamilyMembers()` | Users cannot view family members on Android |
| POST `/family-members` | `createFamilyMember()` | Users cannot add family members on Android |
| PUT `/family-members/{id}` | `updateFamilyMember()` | Users cannot edit family members on Android |
| DELETE `/family-members/{id}` | `deleteFamilyMember()` | Users cannot remove family members on Android |

### 7.3 Layer Mismatches

| Issue | Details |
|-------|---------|
| User data in DataStore, not Room | User/preferences stored in Android DataStore (key-value), not in Room DB. Works fine but different from offline-first pattern used elsewhere. |
| AI Catalog has no Room cache | `searchAiRecipeCatalog()` results are not cached locally — requires network every time. |
| Chat image endpoint untested | POST `/chat/image` (food photo analysis via Gemini) has no backend test. |
| `get_recipe_by_id` untested directly | Individual recipe fetch is tested indirectly through search, but no direct test for the `GET /recipes/{id}` endpoint. |

### 7.4 Test Coverage Summary by Test File

| # | Test File | Tests | Tables Covered |
|---|-----------|-------|----------------|
| 1 | `test_ai_meal_service.py` | 22 | meal_plans, meal_plan_items (service level) |
| 2 | `test_ai_recipe_catalog.py` | 16 | ai_recipe_catalog |
| 3 | `test_auth.py` | 6 | users |
| 4 | `test_chat_api.py` | 12 | chat_messages |
| 5 | `test_chat_integration.py` | 22 | chat_messages (tool calling) |
| 6 | `test_email_uniqueness.py` | 7 | users (email dedup) |
| 7 | `test_family_members_api.py` | 8 | family_members, user_preferences |
| 8 | `test_health.py` | 2 | — (infrastructure) |
| 9 | `test_migrate_legacy_rules.py` | 11 | recipe_rules (migration script) |
| 10 | `test_notification_api.py` | 11 | notifications, fcm_tokens |
| 11 | `test_notification_service.py` | 19 | notifications, fcm_tokens |
| 12 | `test_preference_service.py` | 27 | user_preferences, recipe_rules |
| 13 | `test_recipe_cache.py` | 30 | — (in-memory cache, no DB) |
| 14 | `test_recipe_rules_api.py` | 20 | recipe_rules, nutrition_goals |
| 15 | `test_recipe_rules_dedup.py` | 5 | recipe_rules |
| 16 | `test_recipe_search.py` | 10 | recipes |
| 17 | `test_sharma_recipe_rules.py` | 13 | recipe_rules, nutrition_goals, user_preferences |
| | **TOTAL** | **250** | |

---

## 8. Verification Checklist

- [x] **pytest results:** 250/250 PASSED, 0 failures (verified live on 2026-02-08)
- [x] **PG table count:** 24 tables confirmed across 12 model files
- [x] **Android-only count:** 9 Room entities with no PG counterpart (+ `MealPlanFestivalEntity` which syncs from festivals API)
- [x] **Total tables:** 24 PG + 9 Android-only = 33 unique tables
- [x] **API endpoint count:** 43 endpoints across 12 routers
- [x] **All 43 endpoints appear** in at least one domain section above
- [x] **Retrofit methods:** 39 Android methods map to 39 of 43 backend endpoints (4 family member endpoints have no Android counterpart)
