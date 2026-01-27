# RasoiAI End-to-End Testing Guide

This document provides a comprehensive testing guide for the RasoiAI Android app using **Espresso** for UI/E2E testing.

## Testing Framework

| Layer | Framework | Purpose |
|-------|-----------|---------|
| Unit Tests | JUnit5 + MockK | ViewModel, Repository, UseCase testing |
| UI/E2E Tests | **Espresso** | Screen interactions, user flows, integration |
| Flow Testing | Turbine | StateFlow/Channel testing in ViewModels |

## Test Objective

Perform complete end-to-end testing of RasoiAI app as a new user with 3 family members, validating all screens, API endpoints, and database state across the full user journey.

---

## Pre-Test Setup

### Emulator Requirements
```bash
# Create/start emulator (API 29+ recommended for best compatibility)
$ANDROID_HOME/emulator/emulator -avd Pixel_6_API_34 -no-snapshot-load

# Verify emulator is running
adb devices
```

### App Installation
```bash
cd android
./gradlew installDebug

# Clear app data for fresh start
adb shell pm clear com.rasoiai.app.debug
```

### Test Environment
- **Backend**: Ensure API server is running or use fake repositories
- **Network**: Online for API testing, test offline scenarios separately
- **Logging**: Enable verbose logging for debugging
```bash
adb logcat -s RasoiAI:V
```

---

## Test Data Profile

### User Profile: "Sharma Family"
| Attribute | Value |
|-----------|-------|
| **Email** | test.sharma@gmail.com |
| **Household Size** | 3 |
| **Primary Diet** | VEGETARIAN |
| **Dietary Restrictions** | SATTVIC (no onion/garlic) |
| **Cuisines** | NORTH, SOUTH |
| **Spice Level** | MEDIUM |

### Family Members

| Member | Name | Type | Age | Special Needs |
|--------|------|------|-----|---------------|
| 1 | Ramesh | ADULT | 45 | DIABETIC, LOW_OIL |
| 2 | Sunita | ADULT | 42 | LOW_SALT |
| 3 | Aarav | CHILD | 12 | NO_SPICY |

### Disliked Ingredients
- Karela (Bitter Gourd)
- Baingan (Eggplant)
- Mushroom

### Cooking Preferences
| Setting | Value |
|---------|-------|
| Weekday Cooking Time | 30 minutes |
| Weekend Cooking Time | 60 minutes |
| Busy Days | Monday, Wednesday, Friday |

---

## Phase 1: Authentication Testing

### Test 1.1: Splash Screen
**Steps:**
1. Launch app
2. Observe splash screen animation (RasoiAI logo + tagline)

**Expected:**
- Splash displays for 2-3 seconds
- Redirects to Auth screen (new user)
- No crash or ANR

**Verify Database:**
```sql
-- UserPreferencesDataStore should be empty
SELECT * FROM user_preferences WHERE key = 'is_onboarded'
-- Expected: null or false
```

### Test 1.2: Google OAuth Login
**Steps:**
1. Tap "Sign in with Google" button
2. Select Google account from picker
3. Complete OAuth flow

**Expected:**
- Google sign-in sheet appears
- Successful authentication
- Redirects to Onboarding screen (not Home)

**Verify API:**
```
POST /api/v1/auth/firebase
Request: { "idToken": "<firebase_token>" }
Response: 200 OK { "accessToken": "...", "refreshToken": "..." }
```

**Verify Database:**
```sql
-- Check UserPreferencesDataStore
-- auth_token should be set
-- is_onboarded should be false
```

---

## Phase 2: Onboarding Testing (5 Steps)

### Test 2.1: Step 1 - Household Size & Family Members

**Steps:**
1. Verify step indicator shows 1/5 (20% progress)
2. Select household size: 3
3. Add family member 1:
   - Tap "Add Family Member"
   - Enter name: "Ramesh"
   - Select type: ADULT
   - Enter age: 45
   - Check: DIABETIC, LOW_OIL
   - Tap "Save"
4. Repeat for member 2 (Sunita, ADULT, 42, LOW_SALT)
5. Repeat for member 3 (Aarav, CHILD, 12, NO_SPICY)
6. Verify all 3 members shown in list
7. Tap "Next"

**Test Variations:**
- [ ] Edit existing member (change age)
- [ ] Delete member and re-add
- [ ] Try to proceed with 0 members (should be blocked)
- [ ] Add member with same name (should allow)

**Expected:**
- Bottom sheet modal for add/edit
- Members displayed as cards with edit/delete icons
- "Next" button enabled only when householdSize > 0

**UI Assertions (Espresso):**
```kotlin
// Espresso assertions
onView(withText("Household Size")).check(matches(isDisplayed()))
onView(withText("Ramesh")).check(matches(isDisplayed()))
onView(withText("DIABETIC")).check(matches(isDisplayed()))
```

### Test 2.2: Step 2 - Dietary Preferences

**Steps:**
1. Verify step indicator shows 2/5 (40% progress)
2. Select primary diet: VEGETARIAN (default, verify pre-selected)
3. Check dietary restriction: SATTVIC
4. Verify other options are available (JAIN, HALAL, VEGAN)
5. Tap "Next"

**Test Variations:**
- [ ] Select NON_VEGETARIAN, then switch to VEGETARIAN
- [ ] Select multiple restrictions (JAIN + SATTVIC)
- [ ] Proceed without selecting restrictions (should allow)

**Expected:**
- Radio buttons for primary diet (mutually exclusive)
- Checkboxes for restrictions (multi-select)
- VEGETARIAN shows "No meat, fish, or eggs" description

### Test 2.3: Step 3 - Cuisine Preferences

**Steps:**
1. Verify step indicator shows 3/5 (60% progress)
2. Select cuisines: NORTH, SOUTH (tap both cards)
3. Verify emoji and regional descriptions shown
4. Select spice level from dropdown: MEDIUM
5. Tap "Next"

**Test Variations:**
- [ ] Try to proceed with no cuisine selected (should be blocked)
- [ ] Select all 4 cuisines
- [ ] Change spice level multiple times

**Expected:**
- 2x2 grid of cuisine cards with selection state
- Dropdown for spice level (MILD, MEDIUM, SPICY, VERY_SPICY)
- "Next" button enabled only when at least 1 cuisine selected

**UI Assertions (Espresso):**
```kotlin
// Verify cuisine selection
onView(withText("North Indian")).check(matches(isSelected()))
onView(withText("South Indian")).check(matches(isSelected()))
onView(withText("East Indian")).check(matches(not(isSelected())))
```

### Test 2.4: Step 4 - Disliked Ingredients

**Steps:**
1. Verify step indicator shows 4/5 (80% progress)
2. Tap disliked ingredients from common list:
   - Karela (Bitter Gourd)
   - Baingan (Eggplant)
   - Mushroom
3. Verify selected chips show checkmark
4. Test search: type "Ca" → verify Cabbage, Cauliflower, Capsicum appear
5. Add custom ingredient: type "Jackfruit" → tap add button
6. Verify custom ingredient appears in separate section
7. Remove custom ingredient by tapping X
8. Tap "Next"

**Test Variations:**
- [ ] Select all 12 common ingredients
- [ ] Add 5 custom ingredients
- [ ] Clear search and verify full list returns
- [ ] Proceed with no dislikes (should allow)

**Expected:**
- FilterChip display with bilingual names
- Search filters ingredient list
- Custom ingredients in separate section with remove option

### Test 2.5: Step 5 - Cooking Time & Busy Days

**Steps:**
1. Verify step indicator shows 5/5 (100% progress)
2. Set weekday cooking time: 30 minutes (default)
3. Set weekend cooking time: 60 minutes
4. Select busy days: MON, WED, FRI
5. Tap "Create My Meal Plan"

**Test Variations:**
- [ ] Set minimum times (15 min weekday, 15 min weekend)
- [ ] Set maximum times (90 min both)
- [ ] Select all 7 days as busy
- [ ] Select no busy days

**Expected:**
- Two dropdown selectors for time
- FilterChip flow row for days (multi-select)
- Button text changes to "Create My Meal Plan"

---

## Phase 3: Meal Plan Generation

### Test 3.1: Generation Progress Screen

**Steps:**
1. After tapping "Create My Meal Plan"
2. Observe 4-step progress animation:
   - Step 1: "Analyzing preferences" (0.8s)
   - Step 2: "Checking festivals" (0.6s)
   - Step 3: "Generating recipes" (1.2s)
   - Step 4: "Building grocery list" (0.6s)
3. Verify auto-navigation to Home screen

**Expected:**
- Each step shows spinner → checkmark transition
- Total duration ~3.2 seconds
- Smooth transition to Home screen

**Verify API:**
```
POST /api/v1/meal-plans/generate
Request: {
  "householdSize": 3,
  "familyMembers": [...],
  "primaryDiet": "VEGETARIAN",
  "dietaryRestrictions": ["SATTVIC"],
  "cuisinePreferences": ["NORTH", "SOUTH"],
  "spiceLevel": "MEDIUM",
  "dislikedIngredients": ["Karela", "Baingan", "Mushroom"],
  "weekdayCookingTimeMinutes": 30,
  "weekendCookingTimeMinutes": 60,
  "busyDays": ["MONDAY", "WEDNESDAY", "FRIDAY"]
}
Response: 201 Created { "mealPlanId": "...", "weekStartDate": "...", ... }
```

**Verify Database:**
```sql
-- MealPlanEntity
SELECT * FROM meal_plans ORDER BY created_at DESC LIMIT 1;
-- Expected: new meal plan with current week dates

-- MealPlanItemEntity
SELECT COUNT(*) FROM meal_plan_items WHERE meal_plan_id = '<new_id>';
-- Expected: 28+ items (4 meals x 7 days)

-- Verify no disliked ingredients in recipes
SELECT DISTINCT recipe_name FROM meal_plan_items WHERE recipe_name LIKE '%Karela%';
-- Expected: 0 rows
```

---

## Phase 4: Home Screen Testing

### Test 4.1: Initial Load & Week View

**Steps:**
1. Verify Home screen displays with bottom navigation
2. Check current week dates shown in header
3. Verify 7-day horizontal scroll with day names
4. Tap each day to view meals

**Expected:**
- Current day is highlighted/selected
- Each day shows: Breakfast, Lunch, Dinner, Snacks
- Festival badge shown if applicable (check date)
- Bottom nav shows HOME selected

**UI Assertions (Espresso):**
```kotlin
onView(withId(R.id.home_week_selector)).check(matches(isDisplayed()))
onView(withId(R.id.meal_card_breakfast)).check(matches(isDisplayed()))
onView(withId(R.id.bottom_nav_home)).check(matches(isSelected()))
```

### Test 4.2: Meal Card Interactions

**Steps:**
1. Long-press on a meal card (e.g., Monday Breakfast)
2. Verify lock icon appears
3. Tap lock to lock the meal
4. Verify meal shows locked state
5. Tap swap icon on another meal
6. Verify swap suggestions sheet appears
7. Select alternative recipe
8. Verify meal card updates

**Test Variations:**
- [ ] Lock all meals for a day
- [ ] Swap and then undo (if available)
- [ ] Lock meal then try to swap (should be blocked)

**Verify API:**
```
PUT /api/v1/meal-plans/{planId}/items/{itemId}/lock
Request: { "isLocked": true }
Response: 200 OK

POST /api/v1/meal-plans/{planId}/items/{itemId}/swap
Request: { "newRecipeId": "..." }
Response: 200 OK { "newMealItem": {...} }
```

**Verify Database:**
```sql
-- After locking
SELECT is_locked FROM meal_plan_items
WHERE meal_plan_id = ? AND date = ? AND meal_type = 'breakfast';
-- Expected: 1 (true)
```

### Test 4.3: Recipe Detail Navigation

**Steps:**
1. Tap on any meal card
2. Verify navigation to Recipe Detail screen
3. Verify recipe info: name, image, time, difficulty
4. Check ingredients list with quantities
5. Check step-by-step instructions
6. Verify nutrition info displayed
7. Tap back to return to Home

**Expected:**
- Full recipe details loaded
- Ingredients grouped by category
- Instructions numbered with tips
- Nutrition panel shows calories, protein, carbs, fat

**Verify API:**
```
GET /api/v1/recipes/{recipeId}
Response: 200 OK { "id": "...", "name": "...", "ingredients": [...], ... }
```

### Test 4.4: Navigation to Other Screens

**Steps:**
1. Tap GROCERY in bottom nav → verify Grocery screen
2. Tap CHAT in bottom nav → verify Chat screen
3. Tap FAVORITES in bottom nav → verify Favorites screen
4. Tap STATS in bottom nav → verify Stats screen
5. Return to HOME

**Expected:**
- Each screen loads without crash
- Bottom nav highlights current selection
- State preserved when switching tabs

---

## Phase 5: Grocery Screen Testing

### Test 5.1: Grocery List Display

**Steps:**
1. Navigate to Grocery screen
2. Verify grocery items grouped by category
3. Check quantities match meal plan servings
4. Verify all ingredients from week's recipes present

**Expected:**
- Categories: Vegetables, Dairy, Grains, Spices, Pulses, etc.
- Items show: name, quantity, unit
- Items are unchecked by default

**Verify Database:**
```sql
SELECT category, COUNT(*) as count
FROM grocery_items
WHERE meal_plan_id = ?
GROUP BY category
ORDER BY count DESC;
-- Expected: multiple categories with items
```

### Test 5.2: Check/Uncheck Items

**Steps:**
1. Tap checkbox on first item
2. Verify item shows checked state (strikethrough or dimmed)
3. Tap again to uncheck
4. Use "Mark All Checked" option
5. Verify all items checked
6. Use "Clear Checked" option

**Test Variations:**
- [ ] Check items from different categories
- [ ] Check all items in one category
- [ ] Verify checked count updates in header

**Verify Database:**
```sql
SELECT COUNT(*) FROM grocery_items WHERE is_checked = 1;
-- Should update after each action
```

### Test 5.3: WhatsApp Share

**Steps:**
1. Tap WhatsApp share button
2. Verify formatted text generated
3. Verify WhatsApp app opens with pre-filled message

**Verify API:**
```
GET /api/v1/grocery/whatsapp?mealPlanId={id}
Response: 200 OK { "formattedText": "🛒 Grocery List\n\n..." }
```

---

## Phase 6: Chat Screen Testing

### Test 6.1: Chat Interface

**Steps:**
1. Navigate to Chat screen
2. Verify empty state or welcome message
3. Type message: "What can I make for dinner tonight?"
4. Send message
5. Verify AI response appears

**Expected:**
- Chat bubbles differentiate user vs AI
- Typing indicator while waiting for response
- Quick action chips may appear in response

**Verify API:**
```
POST /api/v1/chat/message
Request: { "content": "What can I make for dinner tonight?" }
Response: 200 OK { "response": "...", "suggestions": [...] }
```

**Verify Database:**
```sql
SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 2;
-- Expected: user message + AI response
```

### Test 6.2: Recipe Suggestions in Chat

**Steps:**
1. Ask: "Suggest a quick breakfast recipe"
2. Verify recipe cards appear in response
3. Tap on suggested recipe
4. Verify navigation to Recipe Detail

**Expected:**
- Recipe cards show image, name, time
- Cards are tappable for navigation

---

## Phase 7: Favorites Screen Testing

### Test 7.1: Add to Favorites

**Steps:**
1. Navigate to Recipe Detail (any recipe)
2. Tap heart/favorite icon
3. Navigate to Favorites screen
4. Verify recipe appears in favorites list

**Test Variations:**
- [ ] Add 5 recipes to favorites
- [ ] Remove recipe from favorites
- [ ] Add same recipe twice (should toggle)

**Verify Database:**
```sql
SELECT COUNT(*) FROM favorites;
-- Should increase after adding

SELECT recipe_id FROM favorites ORDER BY added_at DESC;
-- Most recent should be the added recipe
```

### Test 7.2: Favorites Collections (if implemented)

**Steps:**
1. Create new collection: "Weekend Specials"
2. Move favorite recipe to collection
3. Verify collection shows recipe
4. Delete collection

---

## Phase 8: Stats Screen Testing

### Test 8.1: Cooking Streak

**Steps:**
1. Navigate to Stats screen
2. Verify streak widget shows current streak
3. Check monthly calendar view
4. Verify days marked as "cooked"

**Expected:**
- Streak count displayed (may be 0 for new user)
- Calendar shows current month
- Legend explains day markers

**Verify Database:**
```sql
SELECT * FROM cooking_streak WHERE id = 'user_streak';
SELECT * FROM cooking_days ORDER BY date DESC LIMIT 7;
```

### Test 8.2: Cuisine Breakdown

**Steps:**
1. Scroll to cuisine breakdown section
2. Verify pie chart or bar chart
3. Check breakdown matches meal plan cuisines

**Expected:**
- Visual chart of cuisine distribution
- NORTH and SOUTH should dominate (per preferences)

### Test 8.3: Achievements

**Steps:**
1. Scroll to achievements section
2. Verify locked/unlocked achievements shown
3. Check achievement descriptions

**Expected:**
- Achievement cards with emoji, name, description
- Locked achievements grayed out

---

## Phase 9: Settings Screen Testing

### Test 9.1: Profile Section

**Steps:**
1. Navigate to Settings (gear icon from Home)
2. Verify user profile displayed
3. Check email matches Google account

**Expected:**
- Profile image (from Google)
- Name and email displayed
- Edit profile option available

### Test 9.2: Preference Updates

**Steps:**
1. Navigate to dietary preferences section
2. Change primary diet to EGGETARIAN
3. Save changes
4. Verify confirmation

**Test Variations:**
- [ ] Update cuisine preferences
- [ ] Update cooking times
- [ ] Update family members

**Verify API:**
```
PUT /api/v1/users/preferences
Request: { "primaryDiet": "EGGETARIAN", ... }
Response: 200 OK
```

### Test 9.3: Notifications Toggle

**Steps:**
1. Find notification settings
2. Toggle meal reminders ON/OFF
3. Toggle shopping day reminder ON/OFF

**Expected:**
- Toggle switches work
- Settings persist after app restart

---

## Phase 10: Pantry Screen Testing

### Test 10.1: Add Pantry Items

**Steps:**
1. Navigate to Pantry screen
2. Tap "Add Item" button
3. Enter: Rice, Grains category, 2 kg
4. Save item
5. Verify item appears in list

**Test Variations:**
- [ ] Add item with expiry date
- [ ] Add multiple items
- [ ] Delete item

**Verify Database:**
```sql
SELECT * FROM pantry_items ORDER BY added_date DESC;
```

### Test 10.2: Expiring Soon Section

**Steps:**
1. Add item with expiry in 2 days
2. Verify item shows in "Expiring Soon" section
3. Add item with past expiry
4. Verify item shows in "Expired" section

---

## Phase 11: Recipe Rules Screen Testing

### Test 11.1: Include Rules Tab

**Steps:**
1. Navigate to Recipe Rules screen
2. Verify 4 tabs: Include, Exclude, Nutrition, Settings
3. On Include tab, add rule:
   - Type: RECIPE
   - Target: "Dal Tadka"
   - Frequency: 2 times per week
   - Meal Slot: Lunch/Dinner
   - Enforcement: PREFERRED
4. Save rule

**Verify Database:**
```sql
SELECT * FROM recipe_rules WHERE action = 'INCLUDE';
```

### Test 11.2: Exclude Rules Tab

**Steps:**
1. Switch to Exclude tab
2. Add ingredient exclusion:
   - Type: INGREDIENT
   - Target: "Paneer"
   - Frequency: NEVER
   - Enforcement: REQUIRED
3. Save rule

### Test 11.3: Nutrition Goals Tab

**Steps:**
1. Switch to Nutrition tab
2. Add goal:
   - Food Category: GREEN_LEAFY
   - Weekly Target: 5 servings
   - Enforcement: PREFERRED
3. Save goal

**Verify Database:**
```sql
SELECT * FROM nutrition_goals WHERE is_active = 1;
```

---

## Phase 12: Recipe Detail & Cooking Mode Testing

### Test 12.1: Recipe Scaling

**Steps:**
1. Open any recipe
2. Find serving size selector
3. Increase servings from 2 to 4
4. Verify ingredient quantities double
5. Decrease to 1 serving
6. Verify quantities halve

**Verify API:**
```
GET /api/v1/recipes/{id}/scale?servings=4
Response: 200 OK { "scaledIngredients": [...] }
```

### Test 12.2: Start Cooking Mode

**Steps:**
1. From Recipe Detail, tap "Start Cooking"
2. Verify Cooking Mode screen opens
3. Swipe through instruction steps
4. Verify timer button on timed steps
5. Start timer, verify countdown
6. Complete all steps
7. Verify completion state

**Expected:**
- Large text for instructions (cooking-friendly)
- Step indicator (e.g., Step 3 of 8)
- Timer integration for timed steps
- Tips shown for complex steps

---

## Phase 13: Offline Testing

### Test 13.1: Offline Meal Plan Access

**Steps:**
1. Turn off network (airplane mode)
2. Open app
3. Navigate to Home
4. Verify meal plan displays from cache
5. Navigate through days
6. Open recipe details

**Expected:**
- All cached data accessible
- No crash or error
- Appropriate offline indicator shown

### Test 13.2: Offline Grocery List

**Steps:**
1. While offline, navigate to Grocery
2. Check/uncheck items
3. Verify state persists
4. Re-enable network
5. Verify sync occurs

**Verify Database:**
```sql
SELECT * FROM grocery_items WHERE meal_plan_id = ? AND is_synced = 0;
-- Should show unsynced items during offline
```

### Test 13.3: Offline Action Queue

**Steps:**
1. While offline, lock a meal
2. While offline, swap a meal
3. Re-enable network
4. Verify actions sync to server

**Verify:**
- SyncManager processes queued actions
- Server reflects offline changes

---

## Phase 14: Edge Cases & Error Handling

### Test 14.1: Network Timeout

**Steps:**
1. Simulate slow network (use Charles Proxy or similar)
2. Generate new meal plan
3. Verify timeout handling
4. Verify retry option shown

### Test 14.2: API Error Responses

**Steps:**
1. Simulate 500 error from server
2. Attempt API operation
3. Verify error snackbar/dialog
4. Verify app doesn't crash

### Test 14.3: Invalid Data

**Steps:**
1. Attempt onboarding with edge values:
   - Age: 1, 100
   - Household: 1, 10
   - All days as busy days
2. Verify validation messages
3. Verify app handles gracefully

### Test 14.4: Session Expiry

**Steps:**
1. Clear auth token manually
2. Attempt API operation
3. Verify redirect to login
4. Verify data preserved after re-login

---

## Phase 15: Performance Testing

### Test 15.1: Cold Start Time

**Steps:**
1. Force stop app
2. Clear from recents
3. Time app launch to Home screen
4. Repeat 3 times

**Expected:**
- Cold start < 3 seconds
- Splash screen smooth

### Test 15.2: Screen Transition Performance

**Steps:**
1. Navigate between all main screens rapidly
2. Check for jank or dropped frames
3. Use Android Profiler to measure frame rate

**Expected:**
- 60 FPS maintained
- No visible jank

### Test 15.3: Memory Usage

**Steps:**
1. Navigate through all screens
2. Open 10+ recipes
3. Check memory usage in profiler
4. Verify no memory leaks (LeakCanary)

**Expected:**
- Memory stable after GC
- No LeakCanary alerts

---

## Test Results Template

```markdown
## Test Execution Report

**Date:** YYYY-MM-DD
**Tester:** Claude Code
**App Version:** X.X.X
**Emulator:** Pixel 6 API 34

### Summary
| Phase | Tests | Passed | Failed | Blocked |
|-------|-------|--------|--------|---------|
| 1. Auth | 2 | | | |
| 2. Onboarding | 5 | | | |
| 3. Generation | 1 | | | |
| 4. Home | 4 | | | |
| 5. Grocery | 3 | | | |
| 6. Chat | 2 | | | |
| 7. Favorites | 2 | | | |
| 8. Stats | 3 | | | |
| 9. Settings | 3 | | | |
| 10. Pantry | 2 | | | |
| 11. Rules | 3 | | | |
| 12. Cooking | 2 | | | |
| 13. Offline | 3 | | | |
| 14. Edge Cases | 4 | | | |
| 15. Performance | 3 | | | |
| **TOTAL** | 42 | | | |

### Failed Tests
| Test ID | Description | Actual Result | Screenshot |
|---------|-------------|---------------|------------|
| | | | |

### Blocked Tests
| Test ID | Description | Blocker |
|---------|-------------|---------|
| | | |

### Database Verification
| Entity | Expected | Actual | Status |
|--------|----------|--------|--------|
| meal_plans | 1 | | |
| meal_plan_items | 28+ | | |
| grocery_items | 50+ | | |
| favorites | 0+ | | |
| recipe_rules | 0+ | | |

### API Verification
| Endpoint | Status | Response Time |
|----------|--------|---------------|
| POST /auth/firebase | | |
| POST /meal-plans/generate | | |
| GET /meal-plans/current | | |
| GET /recipes/{id} | | |
| GET /grocery | | |
| POST /chat/message | | |
```

---

## Automation Commands

### Run All Unit Tests
```bash
cd android
./gradlew test --continue
```

### Run Specific Test Classes
```bash
# ViewModel tests
./gradlew :app:test --tests "com.rasoiai.app.presentation.home.HomeViewModelTest"
./gradlew :app:test --tests "com.rasoiai.app.presentation.onboarding.OnboardingViewModelTest"

# Repository tests
./gradlew :data:test --tests "com.rasoiai.data.repository.MealPlanRepositoryImplTest"

# DAO integration tests (requires emulator)
./gradlew :data:connectedAndroidTest --tests "com.rasoiai.data.local.dao.MealPlanDaoTest"
```

### Run Espresso UI/E2E Tests
```bash
# Run all Espresso tests
./gradlew :app:connectedAndroidTest

# Run specific Espresso test class
./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.OnboardingFlowTest

# Run tests with test orchestrator (isolated execution)
./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.orchestrator=true
```

### Generate Test Report
```bash
./gradlew test jacocoTestReport
# Report: app/build/reports/jacoco/test/html/index.html
```

---

## Database Inspection Commands

```bash
# Pull database from emulator
adb exec-out run-as com.rasoiai.app.debug cat databases/rasoiai_database > rasoiai_local.db

# Open with SQLite
sqlite3 rasoiai_local.db

# Useful queries
.tables
SELECT * FROM meal_plans;
SELECT COUNT(*) FROM meal_plan_items;
SELECT * FROM grocery_items WHERE is_checked = 1;
SELECT * FROM favorites;
```

---

## API Testing with cURL

```bash
# Auth
curl -X POST http://localhost:8000/api/v1/auth/firebase \
  -H "Content-Type: application/json" \
  -d '{"idToken": "test_token"}'

# Get current meal plan
curl -X GET http://localhost:8000/api/v1/meal-plans/current \
  -H "Authorization: Bearer <token>"

# Generate meal plan
curl -X POST http://localhost:8000/api/v1/meal-plans/generate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "householdSize": 3,
    "primaryDiet": "VEGETARIAN",
    "cuisinePreferences": ["NORTH", "SOUTH"]
  }'
```

---

## Critical Configuration Findings (Updated from Testing)

### 1. Google OAuth Setup Requirements

**CRITICAL**: The app will fail Google Sign-In with error `[28444] Developer console is not set up correctly` unless these are configured:

#### local.properties Configuration
```properties
# REQUIRED: Web Client ID from google-services.json (client_type: 3)
# Located in: android/local.properties
WEB_CLIENT_ID=1016523916534-tiop62vjrd3ak3sh91ru76bj8p04v49f.apps.googleusercontent.com
```

#### Google Cloud Console Configuration
1. Navigate to: https://console.cloud.google.com/auth/branding?project=rasoiai-6dcdd
2. **Branding Page**:
   - App name: RasoiAI
   - User support email: Must be filled (e.g., abhayinfosys@gmail.com)
   - Developer contact email: Must be filled (required field!)
3. **Audience Page**:
   - Set to "Testing" mode (not Production) for development
   - Add test users (e.g., abhayinfosys@gmail.com)
4. **Clients Page**:
   - Android client must have correct SHA-1: `0D:1C:9D:5D:36:70:91:06:7E:16:C8:D8:EC:5F:AF:C1:6C:39:1D:6E`
   - Web client ID must match the one in local.properties

#### After Configuration Changes
```bash
# Rebuild app to include new WEB_CLIENT_ID
cd android
./gradlew assembleDebug
./gradlew installDebug
```

### 2. UI Element Coordinates (Emulator 1080x2400)

Based on UI hierarchy dumps, here are the exact tap coordinates:

#### Onboarding Step 1 - Household Screen
| Element | Bounds | Tap Coordinates (x, y) |
|---------|--------|------------------------|
| Household dropdown | [42,570][1038,717] | (540, 643) |
| "Add family member" button | [63,892][1017,1039] | (540, 965) |
| "Next" button | [42,2148][1038,2295] | (540, 2221) |

#### Auth Screen
| Element | Bounds | Tap Coordinates (x, y) |
|---------|--------|------------------------|
| "Continue with Google" button | [42,1609][1038,1756] | (540, 1682) |

### 3. ADB Commands (Windows Compatible)

```bash
# Set ADB path for Windows
ADB="C:/Users/itsab/AppData/Local/Android/Sdk/platform-tools/adb.exe"

# Take screenshot
"$ADB" exec-out screencap -p > screenshot.png

# Tap on screen
"$ADB" shell input tap <x> <y>

# Dump UI hierarchy
"$ADB" shell "uiautomator dump /data/local/tmp/ui.xml && cat /data/local/tmp/ui.xml"

# Launch app
"$ADB" shell am start -n com.rasoiai.app/com.rasoiai.app.MainActivity

# Clear app data (fresh start)
"$ADB" shell pm clear com.rasoiai.app

# View logs for RasoiAI
"$ADB" logcat -s RasoiAI:V
```

### 4. Dropdown Selection - VERIFIED COORDINATES

The Compose ExposedDropdownMenu popup items are NOT captured by uiautomator.
Dropdown items are positioned differently than the visual display suggests.

**VERIFIED tap coordinates for household dropdown (emulator 1080x2400):**

```bash
# Open dropdown first
"$ADB" shell input tap 540 643

# Wait for dropdown animation
sleep 0.3

# VERIFIED dropdown item y-coordinates (x can be 100-400):
# 1 person:  y = 770
# 2 people:  y = 880
# 3 people:  y = 990
# 4 people:  y = 1100
# 5 people:  y = 1210
# ... each item is approximately 110px apart

# Example: Select "3 people"
"$ADB" shell input tap 100 990
```

**Complete command to select 3 people:**
```bash
ADB="C:/Users/itsab/AppData/Local/Android/Sdk/platform-tools/adb.exe"
"$ADB" shell input tap 540 643 && sleep 0.3 && "$ADB" shell input tap 100 990
```

### 5. Known Issues

1. **Dropdown keyboard navigation doesn't work**: DPAD_DOWN/ENTER keypresses don't select items in Compose dropdowns
2. **UI dumps don't capture popups**: Need to use visual coordinates for dropdown items
3. **ADB path expansion**: Use full quoted paths with forward slashes on Windows

---

## Notes for Claude Code Execution

When executing this test plan:

1. **Start with setup**: Ensure emulator is running and app is freshly installed
2. **Use Playwright MCP**: For UI interactions, use browser automation tools
3. **Verify database state**: After key operations, pull and inspect the database
4. **Capture screenshots**: On failures, take screenshots for debugging
5. **Log API calls**: Monitor network requests using logcat or proxy
6. **Report findings**: Use the test results template to document outcomes

The test data profile (Sharma Family) is designed to exercise:
- Multiple family members with varied dietary needs
- Sattvic diet restriction (filters onion/garlic recipes)
- Multi-cuisine preference (tests variety in meal planning)
- Busy days (tests quick meal suggestions)
- Disliked ingredients (tests exclusion logic)
