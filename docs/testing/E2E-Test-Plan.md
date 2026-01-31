# RasoiAI E2E Test Plan

This document provides an executable test plan for end-to-end testing of the RasoiAI Android app. Follow this step-by-step guide to validate the complete user journey.

---

## Table of Contents

1. [Overview](#overview)
2. [Test Architecture](#test-architecture)
3. [Prerequisites](#prerequisites)
4. [Test Data: Sharma Family Profile](#test-data-sharma-family-profile)
5. [Phase 1: Authentication](#phase-1-authentication)
6. [Phase 2: Onboarding](#phase-2-onboarding)
7. [Phase 3: Meal Plan Generation](#phase-3-meal-plan-generation)
8. [Phase 4: Home Screen](#phase-4-home-screen)
9. [Additional Phases (5-15)](#additional-phases-5-15)
10. [Verification Checklists](#verification-checklists)
11. [Execution Commands](#execution-commands)
12. [Test Results Template](#test-results-template)
13. [Backend Meal Generation E2E Testing](#backend-meal-generation-e2e-testing)
14. [Meal Generation API & Screen Data Flow Tests](#meal-generation-api--screen-data-flow-tests)
15. [Sequential Core Data Flow Test (Real API)](#section-15-sequential-core-data-flow-test-real-api)

---

## Overview

### Purpose

This test plan validates the complete user journey through the RasoiAI app, from first launch through daily usage. It covers:

- User authentication via Google OAuth
- 5-step onboarding with family preferences
- AI-powered meal plan generation
- Core features: Home, Grocery, Chat, Favorites, Stats, Settings
- Advanced features: Pantry, Recipe Rules, Cooking Mode
- Offline behavior and edge cases
- Performance benchmarks

### Scope

| In Scope | Out of Scope |
|----------|--------------|
| Android app (API 34) | iOS app |
| Backend API integration | Direct PostgreSQL manipulation |
| Compose UI Testing framework | Manual exploratory testing |
| Sharma Family test profile | Other test profiles |

### Test Coverage Summary

| Phase | Description | Automated Tests |
|-------|-------------|-----------------|
| 1 | Authentication | ~18 tests |
| 2 | Onboarding | ~41 tests |
| 3 | Generation | ~17 tests |
| 4 | Home | ~22 tests |
| 5-15 | Remaining Features | ~300+ tests |

**Total: ~400 automated tests**

---

## Test Architecture

### Real Backend + Fake Google Auth Only

The E2E tests use a hybrid approach where most components are real, but Google OAuth is faked to enable automated testing.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ANDROID E2E TEST                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. User taps "Sign in with Google"                                         │
│                    ↓                                                        │
│  2. FakeGoogleAuthClient (bypasses real Google OAuth)                       │
│     Returns: { userId: "fake-user-id",                                      │
│                email: "test@example.com",                                   │
│                name: "Test User",                                           │
│                firebaseIdToken: "fake-firebase-token" }                     │
│                    ↓                                                        │
│  3. AuthViewModel calls AuthRepository.signInWithGoogle("fake-firebase-token")
│                    ↓                                                        │
│  4. REAL AuthRepositoryImpl calls backend API:                              │
│     POST http://10.0.2.2:8000/api/v1/auth/firebase                          │
│     Body: { "firebase_token": "fake-firebase-token" }                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                     ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PYTHON BACKEND (PostgreSQL)                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  5. Backend receives "fake-firebase-token"                                  │
│     firebase.py detects: settings.debug=True AND token=="fake-firebase-token"
│     Returns mock user: { uid: "fake-user-id",                               │
│                          email: "test@example.com",                         │
│                          name: "Test User" }                                │
│                    ↓                                                        │
│  6. Creates/finds user in PostgreSQL                                         │
│                    ↓                                                        │
│  7. Returns REAL JWT tokens:                                                │
│     { access_token: "eyJ...", refresh_token: "eyJ...", user: {...} }        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### What's Fake vs Real

| Component | Type | Source File | Notes |
|-----------|------|-------------|-------|
| Google OAuth | **FAKE** | `FakeGoogleAuthClient.kt` | Bypasses real Google sign-in |
| Firebase Token | **FAKE** | Hardcoded `"fake-firebase-token"` | Backend accepts in debug mode |
| DataStore State | **FAKE** | `FakeUserPreferencesDataStore.kt` | Controls onboarding state |
| Network Monitor | **FAKE** | `FakeNetworkMonitor` | For offline tests only |
| Backend API | **REAL** | Python FastAPI at localhost:8000 | All API calls are real |
| JWT Tokens | **REAL** | Backend generates | Actual JWT authentication |
| PostgreSQL Database | **REAL** | 3,580 recipes | Production-like data |
| Repositories | **REAL** | All `*RepositoryImpl` classes | Real data layer |
| ViewModels | **REAL** | All `*ViewModel` classes | Real presentation logic |
| Room Database | **REAL** | Local SQLite cache | Real offline storage |

### Key Fake Module Files

| File | Purpose |
|------|---------|
| `e2e/di/FakeGoogleAuthClient.kt` | Returns fake credentials, skips OAuth flow |
| `e2e/di/FakeAuthModule.kt` | Hilt module replacing real GoogleAuthClient |
| `e2e/di/FakeUserPreferencesDataStore.kt` | Controls navigation state (onboarded/not) |
| `e2e/di/FakeDataStoreModule.kt` | Hilt module for DataStore replacement |

---

## Prerequisites

### 1. Emulator Setup

```bash
# Start API 34 emulator (NOT API 36 - has Espresso issues)
$ANDROID_HOME/emulator/emulator -avd Pixel_6_API_34 -no-snapshot-load

# Verify emulator is running
adb devices

# Wait for boot completion
adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'
```

**Recommended Emulator:**
- Device: Pixel 6 (1080x2400)
- API Level: 34 (Android 14)
- RAM: 2048 MB
- VM Heap: 512 MB

### 2. Backend Setup

```bash
cd backend

# Activate virtual environment
source venv/bin/activate  # Linux/Mac/Git Bash
# .\venv\Scripts\activate  # Windows PowerShell

# Set Firebase credentials
export FIREBASE_CREDENTIALS_PATH="./rasoiai-firebase-service-account.json"

# Start backend
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Verify API is running
curl http://localhost:8000/health
# Expected: {"status": "healthy"}
```

### 3. App Installation

```bash
cd android

# Clean build
./gradlew clean assembleDebug

# Install on emulator
./gradlew installDebug

# Clear app data for fresh state (REQUIRED for new user test)
adb shell pm clear com.rasoiai.app.debug

# Verify installation
adb shell pm list packages | grep rasoiai
# Expected: package:com.rasoiai.app.debug
```

### 4. Verify Recipe Database

```bash
cd backend
source venv/bin/activate
python scripts/verify_recipe_import.py

# Expected output:
# Total recipes: 3,580
# North Indian: 3,124
# South Indian: 358
# West Indian: 85
# East Indian: 23
# Vegetarian: 3,482
# Vegan: 1,347
```

---

## Test Data: Sharma Family Profile

The standard test profile used across all E2E tests.

### Account

| Field | Value |
|-------|-------|
| Email | test@example.com (from FakeGoogleAuthClient) |
| Auth | Google OAuth (faked) |
| User State | New user (first launch) |

### Household (3 Members)

| Name | Type | Age | Health Conditions | Notes |
|------|------|-----|-------------------|-------|
| Ramesh | ADULT | 45 | DIABETIC, LOW_OIL | Family head |
| Sunita | ADULT | 42 | LOW_SALT | Heart-healthy diet |
| Aarav | CHILD | 12 | NO_SPICY | School-going, mild food only |

### Dietary Preferences

| Setting | Value |
|---------|-------|
| Primary Diet | VEGETARIAN |
| Restrictions | SATTVIC (no onion/garlic) |
| Cuisines | NORTH, SOUTH |
| Spice Level | MEDIUM |

### Allergies (Food Safety Critical)

| Allergen | Severity | Action |
|----------|----------|--------|
| Peanuts | SEVERE | Must NEVER appear in any recipe |
| Cashews | MILD | Warn but allow with confirmation |

### Dislikes (Preference-Based)

| English | Hindi | Category |
|---------|-------|----------|
| Karela | करेला (Bitter Gourd) | Vegetables |
| Baingan | बैंगन (Eggplant) | Vegetables |
| Mushroom | मशरूम | Vegetables |

### Cooking Time

| Day Type | Time Limit | Notes |
|----------|------------|-------|
| Weekdays | 30 minutes | Quick meals |
| Weekends | 60 minutes | Elaborate recipes OK |
| Busy Days | Mon, Wed, Fri | Extra-quick meals preferred |

### Expected Meal Plan Constraints

Generated meal plans for this profile MUST:

1. Be 100% VEGETARIAN (no meat, fish, eggs)
2. Exclude onion and garlic (SATTVIC)
3. **Never contain peanuts** (SEVERE ALLERGY)
4. Avoid karela, baingan, mushroom recipes
5. Include NORTH and SOUTH Indian cuisines
6. Have ≤30 min prep on weekdays
7. Have ≤60 min prep on weekends
8. Suggest quick meals on Mon/Wed/Fri (busy days)
9. Consider DIABETIC needs for Ramesh (low sugar)
10. Consider LOW_SALT for Sunita
11. Consider NO_SPICY for Aarav (mild preparations)

---

## Phase 1: Authentication

### Setup

**Fake Module State:**
```kotlin
// In BaseE2ETest, before test starts:
fakeGoogleAuthClient.reset()  // Clear any previous state
fakeGoogleAuthClient.setSignInSuccess()  // Enable successful auth
fakeUserPreferencesDataStore.simulateNewUser()  // Not yet onboarded
```

**Test Files:**
- `e2e/flows/AuthFlowTest.kt`
- `presentation/auth/AuthScreenTest.kt`
- `presentation/auth/AuthIntegrationTest.kt`

### Test 1.1: Fresh App Launch

**Steps:**
1. Clear app data: `adb shell pm clear com.rasoiai.app.debug`
2. Launch app: `adb shell am start -n com.rasoiai.app.debug/com.rasoiai.app.MainActivity`
3. Observe splash screen
4. Wait for auto-navigation

**Expected Results:**
| Check | TestTag | Expected |
|-------|---------|----------|
| Splash displays | - | RasoiAI logo visible |
| Tagline visible | - | "AI-Powered Meal Planning" |
| Animation duration | - | 2-3 seconds |
| Navigation | `AUTH_SCREEN` | Navigates to Auth screen |
| No crash | - | No ANR or crash dialog |

**Verification:**
```kotlin
// In test code:
composeTestRule.onNodeWithTag(TestTags.AUTH_SCREEN).assertIsDisplayed()
composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).assertIsDisplayed()
composeTestRule.onNodeWithTag(TestTags.AUTH_WELCOME_TEXT).assertIsDisplayed()
```

### Test 1.2: Google Sign-In (via FakeGoogleAuthClient)

**Steps:**
1. On Auth screen, tap "Continue with Google" button
2. FakeGoogleAuthClient intercepts and returns fake credentials
3. App sends "fake-firebase-token" to backend
4. Backend accepts token (debug mode) and returns JWT

**Expected Results:**
| Check | Expected |
|-------|----------|
| No Google account picker | Bypassed by FakeGoogleAuthClient |
| Loading indicator | Shows during auth request |
| API call succeeds | Backend returns 200 OK with JWT |
| Navigation | Redirects to Onboarding Step 1 |

**FakeGoogleAuthClient Returns:**
```kotlin
GoogleSignInResult.Success(
    SignInUserData(
        userId = "fake-user-id",
        email = "test@example.com",
        displayName = "Test User",
        photoUrl = null,
        firebaseIdToken = "fake-firebase-token"
    )
)
```

**API Verification:**
```bash
# Expected backend call:
POST /api/v1/auth/firebase
Content-Type: application/json
{"idToken": "fake-firebase-token"}

# Expected response:
HTTP 200 OK
{
  "accessToken": "eyJ...",
  "refreshToken": "...",
  "expiresIn": 3600,
  "user": {
    "id": "fake-user-id",
    "email": "test@example.com",
    "displayName": "Test User"
  }
}
```

### Test 1.3: Auth Error Handling

**Setup:**
```kotlin
fakeGoogleAuthClient.setSignInFailure(Exception("Network error"))
```

**Steps:**
1. Tap "Continue with Google"
2. FakeGoogleAuthClient returns error

**Expected Results:**
- [ ] Error snackbar/message displayed
- [ ] Retry button visible
- [ ] No crash
- [ ] Can retry after fixing (reset fake to success)

---

## Phase 2: Onboarding

### Setup

**Fake Module State:**
```kotlin
// FakeGoogleAuthClient already signed in from Phase 1
// FakeUserPreferencesDataStore.simulateNewUser() already set
```

**Test Files:**
- `e2e/flows/OnboardingFlowTest.kt`
- `presentation/onboarding/OnboardingScreenTest.kt`

### Test 2.1: Step 1 - Household Size

**UI Verification:**
| TestTag | Element | State |
|---------|---------|-------|
| `ONBOARDING_STEP_INDICATOR` | Progress | Shows 1/5 (20%) |
| `HOUSEHOLD_SIZE_DROPDOWN` | Dropdown | "Select size" placeholder |
| `ADD_FAMILY_MEMBER_BUTTON` | Button | Visible |
| `FAMILY_MEMBERS_LIST` | List | Empty |
| `ONBOARDING_NEXT_BUTTON` | Next | Disabled |

**Steps (Sharma Family):**
1. Tap household size dropdown
2. Select "3" from options
3. Tap "Add Family Member"
4. Add Ramesh:
   - Name: "Ramesh"
   - Type: ADULT
   - Age: 45
   - Conditions: DIABETIC, LOW_OIL
   - Save
5. Add Sunita:
   - Name: "Sunita"
   - Type: ADULT
   - Age: 42
   - Conditions: LOW_SALT
   - Save
6. Add Aarav:
   - Name: "Aarav"
   - Type: CHILD
   - Age: 12
   - Conditions: NO_SPICY
   - Save
7. Tap "Next"

**Expected Results:**
- [ ] Dropdown shows 1-8+ options
- [ ] Bottom sheet opens for member entry
- [ ] Member cards show name, age, conditions
- [ ] Edit/Delete icons on each card
- [ ] "Next" enables when members added
- [ ] Navigates to Step 2

**Verification Code:**
```kotlin
// Robot pattern (from FullUserJourneyTest.kt)
onboardingRobot
    .assertStepIndicator(1)
    .selectHouseholdSize(3)
    .addFamilyMember(ramesh)
    .addFamilyMember(sunita)
    .addFamilyMember(aarav)
    .tapNext()
```

### Test 2.2: Step 2 - Dietary Preferences

**UI Verification:**
| TestTag | Element | State |
|---------|---------|-------|
| `ONBOARDING_STEP_INDICATOR` | Progress | Shows 2/5 (40%) |
| `PRIMARY_DIET_VEGETARIAN` | Radio | Selected by default |
| `DIETARY_RESTRICTION_SATTVIC` | Checkbox | Unchecked |
| `ONBOARDING_NEXT_BUTTON` | Next | Enabled |

**Steps:**
1. Verify VEGETARIAN is pre-selected
2. Check SATTVIC restriction
3. Tap "Next"

**Expected Results:**
- [ ] Radio buttons mutually exclusive
- [ ] SATTVIC shows "No onion, garlic" description
- [ ] Multiple restrictions selectable
- [ ] Navigates to Step 3

**Diet Options:**
| Value | Description |
|-------|-------------|
| VEGETARIAN | No meat, fish, eggs |
| NON_VEGETARIAN | All foods |
| EGGETARIAN | Vegetarian + eggs |
| VEGAN | No animal products |

### Test 2.3: Step 3 - Cuisine Preferences

**UI Verification:**
| TestTag | Element | State |
|---------|---------|-------|
| `ONBOARDING_STEP_INDICATOR` | Progress | Shows 3/5 (60%) |
| `CUISINE_CARD_NORTH` | Card | Unselected |
| `CUISINE_CARD_SOUTH` | Card | Unselected |
| `SPICE_LEVEL_DROPDOWN` | Dropdown | Default value |
| `ONBOARDING_NEXT_BUTTON` | Next | Disabled (no cuisine) |

**Steps:**
1. Tap NORTH Indian card
2. Tap SOUTH Indian card
3. Select MEDIUM spice level
4. Tap "Next"

**Expected Results:**
- [ ] Cards show selection state (border/checkmark)
- [ ] Multiple cuisines selectable
- [ ] "Next" enables when ≥1 cuisine selected
- [ ] Navigates to Step 4

### Test 2.4: Step 4 - Dislikes & Allergies

**UI Verification:**
| TestTag | Element | State |
|---------|---------|-------|
| `ONBOARDING_STEP_INDICATOR` | Progress | Shows 4/5 (80%) |
| `INGREDIENT_SEARCH_FIELD` | Search | Empty |
| `INGREDIENT_CHIP_*` | Chips | Common ingredients |
| `ONBOARDING_NEXT_BUTTON` | Next | Enabled (optional) |

**Steps:**
1. **Allergies:**
   - Search "Peanuts" → Add as SEVERE allergy
   - Search "Cashews" → Add as MILD allergy
2. **Dislikes:**
   - Select "Karela" chip
   - Select "Baingan" chip
   - Select "Mushroom" chip
3. Tap "Next"

**Expected Results:**
- [ ] Allergy items show severity badge
- [ ] Dislikes show as filter chips
- [ ] Selected items have checkmark
- [ ] Search filters in real-time
- [ ] Navigates to Step 5

### Test 2.5: Step 5 - Cooking Schedule

**UI Verification:**
| TestTag | Element | State |
|---------|---------|-------|
| `ONBOARDING_STEP_INDICATOR` | Progress | Shows 5/5 (100%) |
| `WEEKDAY_TIME_DROPDOWN` | Dropdown | 30 min default |
| `WEEKEND_TIME_DROPDOWN` | Dropdown | 60 min default |
| `BUSY_DAY_CHIP_*` | Day chips | Unselected |
| `ONBOARDING_NEXT_BUTTON` | Button | "Create My Meal Plan" |

**Steps:**
1. Set weekday time: 30 minutes
2. Set weekend time: 60 minutes
3. Select busy days: MON, WED, FRI
4. Tap "Create My Meal Plan"

**Expected Results:**
- [ ] Time dropdowns show 15-90 min options
- [ ] Day chips are multi-select
- [ ] Triggers meal plan generation
- [ ] Navigates to Generation screen

---

## Phase 3: Meal Plan Generation

### Setup

**Triggered by:** "Create My Meal Plan" button from Onboarding Step 5

**Test Files:**
- `presentation/generation/GenerationScreenTest.kt`
- `e2e/flows/MealPlanGenerationTest.kt`

### Test 3.1: Generation Progress

**UI Verification:**
| TestTag | Element | State |
|---------|---------|-------|
| `GENERATING_SCREEN` | Screen | Displayed |
| `GENERATING_PROGRESS_ANALYZING` | Step 1 | Spinner → ✓ |
| `GENERATING_PROGRESS_FESTIVALS` | Step 2 | Spinner → ✓ |
| `GENERATING_PROGRESS_RECIPES` | Step 3 | Spinner → ✓ |
| `GENERATING_PROGRESS_GROCERY` | Step 4 | Spinner → ✓ |

**Expected Animation Sequence:**
| Step | Message | Duration |
|------|---------|----------|
| 1 | "Analyzing your preferences..." | ~0.8s |
| 2 | "Checking upcoming festivals..." | ~0.6s |
| 3 | "Generating personalized recipes..." | ~1.2s |
| 4 | "Building your grocery list..." | ~0.6s |

**Expected Results:**
- [ ] Each step shows spinner then checkmark
- [ ] Total time ~3-10 seconds (depends on API)
- [ ] Auto-navigates to Home screen on success
- [ ] No error displayed for success case

**API Verification:**
```bash
# Request to backend:
POST /api/v1/meal-plans/generate
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "householdSize": 3,
  "familyMembers": [
    {"name": "Ramesh", "type": "ADULT", "age": 45, "conditions": ["DIABETIC", "LOW_OIL"]},
    {"name": "Sunita", "type": "ADULT", "age": 42, "conditions": ["LOW_SALT"]},
    {"name": "Aarav", "type": "CHILD", "age": 12, "conditions": ["NO_SPICY"]}
  ],
  "primaryDiet": "VEGETARIAN",
  "dietaryRestrictions": ["SATTVIC"],
  "allergies": [
    {"ingredient": "Peanuts", "severity": "SEVERE"},
    {"ingredient": "Cashews", "severity": "MILD"}
  ],
  "cuisinePreferences": ["NORTH", "SOUTH"],
  "spiceLevel": "MEDIUM",
  "dislikedIngredients": ["Karela", "Baingan", "Mushroom"],
  "weekdayCookingTimeMinutes": 30,
  "weekendCookingTimeMinutes": 60,
  "busyDays": ["MONDAY", "WEDNESDAY", "FRIDAY"]
}

# Expected response:
HTTP 201 Created
{
  "mealPlanId": "mp_abc123",
  "weekStartDate": "2026-01-26",
  "weekEndDate": "2026-02-01",
  "days": [...],  # 7 days
  "totalRecipes": 28,  # 4 meals x 7 days
  "groceryItemCount": 45
}
```

### Test 3.2: Generation Error Handling

**Setup:**
- Stop backend server OR inject network error

**Expected Results:**
- [ ] Error message displayed: `GENERATING_ERROR_MESSAGE`
- [ ] Retry button visible: `GENERATING_RETRY_BUTTON`
- [ ] Tap retry → reattempts generation
- [ ] No crash on error

---

## Phase 4: Home Screen

### Setup

**State:** User authenticated, onboarded, meal plan generated

**For isolated Home tests:**
```kotlin
setUpAuthenticatedState()  // Bypasses auth + onboarding
```

**Test Files:**
- `e2e/flows/HomeScreenTest.kt`
- `presentation/home/HomeScreenTest.kt`

### Test 4.1: Initial Home Load

**UI Verification:**
| TestTag | Element | State |
|---------|---------|-------|
| `HOME_SCREEN` | Screen | Displayed |
| `HOME_WEEK_SELECTOR` | Week header | Current week dates |
| `HOME_DAY_TAB_*` | Day tabs | 7 days, today selected |
| `MEAL_CARD_breakfast` | Card | Today's breakfast |
| `MEAL_CARD_lunch` | Card | Today's lunch |
| `MEAL_CARD_dinner` | Card | Today's dinner |
| `BOTTOM_NAV` | Navigation | 5 items, HOME selected |

**Expected Results:**
- [ ] Week dates are correct
- [ ] Current day is highlighted
- [ ] 4 meal cards visible (Breakfast, Lunch, Dinner, Snacks)
- [ ] Meal cards show: recipe name, prep time, image
- [ ] No empty meal slots
- [ ] Bottom nav shows: HOME, GROCERY, CHAT, FAVORITES, STATS

### Test 4.2: Day Navigation

**Steps:**
1. Note today's meals
2. Swipe left to tomorrow
3. Verify different meals displayed
4. Tap on Friday (busy day)
5. Verify quick recipes (≤30 min)
6. Tap on Sunday (weekend)
7. Verify potentially longer recipes allowed

**Expected Results:**
- [ ] Each day shows different recipes
- [ ] Busy days have ≤30 min recipes
- [ ] Weekend may have ≤60 min recipes
- [ ] Day selection updates meal cards

### Test 4.3: Meal Card Actions

**Test Lock:**
1. Long-press on Monday Breakfast
2. Verify action menu (Lock, Swap)
3. Tap Lock
4. Verify lock icon appears: `MEAL_LOCK_BUTTON_breakfast`
5. Tap again to unlock

**Test Swap:**
1. Tap Swap on any unlocked meal
2. Verify suggestion sheet appears
3. Verify alternatives match dietary constraints
4. Select an alternative
5. Verify meal card updates

**Expected Results:**
- [ ] Lock persists after app restart
- [ ] Locked meals cannot be swapped
- [ ] Swap suggestions are VEGETARIAN + SATTVIC
- [ ] Swap updates immediately

### Test 4.4: Recipe Detail Navigation

**Steps:**
1. Tap any meal card
2. Verify navigation to Recipe Detail

**Expected Results:**
- [ ] `RECIPE_DETAIL_SCREEN` displayed
- [ ] Recipe name matches tapped card
- [ ] Ingredients list visible
- [ ] Instructions visible
- [ ] Back button returns to Home

---

## Additional Phases (5-15)

### Phase 5: Grocery Screen

**TestTags:** `GROCERY_SCREEN`, `GROCERY_ITEM_*`, `GROCERY_CATEGORY_*`, `GROCERY_WHATSAPP_BUTTON`

**Key Tests:**
- Items derived from meal plan recipes
- Quantities aggregated across recipes
- Check/uncheck workflow
- WhatsApp share functionality
- Updates after meal swap

### Phase 6: Chat Screen

**TestTags:** `CHAT_SCREEN`, `CHAT_INPUT_FIELD`, `CHAT_SEND_BUTTON`, `CHAT_MESSAGE_*`

**Key Tests:**
- Send message and receive AI response
- Context awareness (uses user preferences)
- Recipe card navigation from chat

### Phase 7: Favorites Screen

**TestTags:** `FAVORITES_SCREEN`, `FAVORITES_LIST`

**Key Tests:**
- Empty state for new user
- Add to favorites from Recipe Detail
- Remove from favorites
- Persistence after app restart

### Phase 8: Stats Screen

**TestTags:** `STATS_SCREEN`, `STATS_STREAK_WIDGET`, `STATS_CUISINE_CHART`

**Key Tests:**
- New user state (0 streak)
- Streak after marking meals cooked
- Achievement unlocks

### Phase 9: Settings Screen

**TestTags:** `SETTINGS_SCREEN`

**Key Tests:**
- Profile display matches auth data
- Preferences match onboarding input
- Preference editing works

### Phase 10: Pantry Screen

**TestTags:** `PANTRY_SCREEN`

**Key Tests:**
- Add pantry items
- Expiry tracking
- Smart suggestions

### Phase 11: Recipe Rules Screen

**TestTags:** `RECIPE_RULES_SCREEN`

**Key Tests:**
- Create include rule (Chai daily)
- Create exclude rule (Paneer never)
- Create nutrition goal (Green leafy 5x/week)
- Rules applied after regeneration

### Phase 12: Cooking Mode Screen

**TestTags:** `COOKING_MODE_SCREEN`

**Key Tests:**
- Recipe scaling
- Step-by-step navigation
- Timer functionality

### Phase 13: Offline Testing

**Key Tests:**
- Cached meal plan access offline
- Local modifications queued
- Sync on reconnect

### Phase 14: Edge Cases

**Key Tests:**
- Network error handling
- Input validation
- Session expiry → login redirect

### Phase 15: Performance

**Key Tests:**
- Cold start < 3 seconds
- 60 FPS maintained
- Memory < 150 MB typical

---

## Verification Checklists

### Constraint Verification (After Generation)

Run these checks on the generated meal plan:

```sql
-- NO PEANUTS (SEVERE ALLERGY)
SELECT COUNT(*) FROM meal_plan_items mpi
JOIN recipe_ingredients ri ON mpi.recipe_id = ri.recipe_id
WHERE ri.ingredient_name ILIKE '%peanut%';
-- MUST BE: 0 rows

-- NO ONION/GARLIC (SATTVIC)
SELECT COUNT(*) FROM meal_plan_items mpi
JOIN recipe_ingredients ri ON mpi.recipe_id = ri.recipe_id
WHERE ri.ingredient_name ILIKE '%onion%'
   OR ri.ingredient_name ILIKE '%garlic%';
-- MUST BE: 0 rows

-- ALL VEGETARIAN
SELECT COUNT(*) FROM meal_plan_items mpi
JOIN recipes r ON mpi.recipe_id = r.id
WHERE NOT (r.dietary_tags @> '["VEGETARIAN"]');
-- MUST BE: 0 rows

-- BUSY DAYS ≤30 MIN
SELECT date, meal_type, recipe_name, total_time_minutes
FROM meal_plan_items
WHERE EXTRACT(DOW FROM date) IN (1, 3, 5)  -- Mon, Wed, Fri
AND total_time_minutes > 30;
-- SHOULD BE: 0 rows (or minimal exceptions)
```

### Recipe Rules Verification

After creating rules and regenerating:

| Rule | Verification |
|------|--------------|
| Chai → Breakfast daily | 7 breakfast slots have chai |
| Chai → Snacks daily | 7 snack slots have chai |
| Moringa 1x/week | ≥1 moringa recipe in plan |
| Paneer excluded | 0 paneer recipes |
| Green leafy 5x/week | ≥5 green leafy recipes |

---

## Execution Commands

### Run All E2E Tests

```bash
cd android
./gradlew connectedDebugAndroidTest
```

### Run Specific Test Class

```bash
# Full User Journey
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.FullUserJourneyTest

# Auth Flow
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.AuthFlowTest

# Onboarding Flow
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.OnboardingFlowTest

# Home Screen Tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.HomeScreenTest
```

### Run by Package

```bash
# All E2E flow tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.rasoiai.app.e2e.flows

# All database verification tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.rasoiai.app.e2e.database

# All constraint validation tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.rasoiai.app.e2e.validation

# All UI screen tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.rasoiai.app.presentation
```

### View Test Results

```bash
# Results location after test run:
# android/app/build/reports/androidTests/connected/index.html

# Open in browser (macOS)
open app/build/reports/androidTests/connected/index.html

# Open in browser (Windows)
start app/build/reports/androidTests/connected/index.html
```

---

## Test Results Template

Use this template to record test execution results:

```markdown
# E2E Test Execution Report

**Date:** YYYY-MM-DD
**Tester:** [Name/Claude Code]
**App Version:** X.X.X (Build XXX)
**Emulator:** Pixel_6_API_34 (1080x2400)
**Backend:** localhost:8000

## Summary

| Phase | Tests | Passed | Failed | Blocked |
|-------|-------|--------|--------|---------|
| 1. Auth | 3 | | | |
| 2. Onboarding | 5 | | | |
| 3. Generation | 2 | | | |
| 4. Home | 4 | | | |
| 5. Grocery | 4 | | | |
| 6. Chat | 3 | | | |
| 7. Favorites | 3 | | | |
| 8. Stats | 3 | | | |
| 9. Settings | 3 | | | |
| 10. Pantry | 3 | | | |
| 11. Rules | 4 | | | |
| 12. Cooking | 3 | | | |
| 13. Offline | 3 | | | |
| 14. Edge Cases | 4 | | | |
| 15. Performance | 3 | | | |
| **TOTAL** | ~50 | | | |

## Sharma Family Profile Verification

| Attribute | Expected | Actual | Match |
|-----------|----------|--------|-------|
| Household Size | 3 | | [ ] |
| Primary Diet | VEGETARIAN | | [ ] |
| Restrictions | SATTVIC | | [ ] |
| Cuisines | NORTH, SOUTH | | [ ] |
| Allergies | Peanuts, Cashews | | [ ] |
| Dislikes | Karela, Baingan, Mushroom | | [ ] |

## Constraint Verification

| Constraint | Check | Status |
|------------|-------|--------|
| No peanuts | 0 recipes with peanuts | [ ] |
| SATTVIC | 0 recipes with onion/garlic | [ ] |
| Vegetarian | 100% vegetarian recipes | [ ] |
| Busy day timing | ≤30 min on Mon/Wed/Fri | [ ] |

## Failed Tests

| ID | Phase | Test | Expected | Actual | Screenshot |
|----|-------|------|----------|--------|------------|
| | | | | | |

## Bugs Found

| # | Severity | Summary | Steps to Reproduce |
|---|----------|---------|-------------------|
| 1 | | | |

## Notes

-
```

---

## Backend Meal Generation E2E Testing

### Overview

The backend has dedicated E2E tests that verify the meal generation algorithm works correctly against the **real PostgreSQL database** (3,580 recipes).

**Test File:** `backend/tests/test_meal_generation_e2e.py`

### Test Categories

| Test Class | Purpose |
|------------|---------|
| `TestMealGenerationE2E` | Sharma Family profile with all rules |
| `TestEastIndianCuisine` | Limited recipes (23) - tests generic fallback |
| `TestSouthIndianCuisine` | South Indian with Idli INCLUDE rule |
| `TestVerificationReport` | Generates printable verification report |

### Sharma Family Verification Checklist

| Check | Expected | Pass Criteria |
|-------|----------|---------------|
| Peanut allergy | 0 peanut/groundnut recipes | CRITICAL - must be 0 |
| Mushroom EXCLUDE | 0 mushroom recipes | Must be 0 |
| Dislikes (karela/lauki/turai) | 0 recipes | Should be 0 |
| Chai DAILY | 7 in breakfast | Count >= 7 |
| Dal 4x/week | 4 in lunch/dinner | Count >= 4 |
| Paneer 2x/week | 2 in lunch/dinner | Count >= 2 |
| 2-item pairing | 28 slots × 2 items | Most slots have 2 |
| No duplicate mains | Unique main recipes | No excessive repeats |

### Prerequisites

```bash
# 1. Firebase credentials must be configured
export FIREBASE_CREDENTIALS_PATH="./rasoiai-firebase-service-account.json"

# 2. Verify recipe database has data
cd backend
PYTHONPATH=. python scripts/verify_recipe_import.py
# Expected: Total recipes: 3,580
```

### Running Backend E2E Tests

```bash
cd backend
source venv/bin/activate  # Linux/Mac/Git Bash
# .\venv\Scripts\activate  # Windows PowerShell

# Run all E2E tests (hits real PostgreSQL)
PYTHONPATH=. pytest tests/test_meal_generation_e2e.py -v -s

# Run just the verification report
PYTHONPATH=. pytest tests/test_meal_generation_e2e.py::TestVerificationReport -v -s

# Run specific test
PYTHONPATH=. pytest tests/test_meal_generation_e2e.py::TestMealGenerationE2E::test_sharma_family_no_peanuts -v -s
```

### Important Notes

**PostgreSQL Quota Limits:**
- Free tier has daily read limits (~50K reads/day)
- Each E2E test run makes ~100-200 PostgreSQL reads
- If you see `429 Quota exceeded`, wait 24 hours or use Blaze plan
- Run E2E tests sparingly (once per day during development)

**Test Output:**
The verification report test prints a detailed summary:

```
================================================================================
SHARMA FAMILY VERIFICATION REPORT
================================================================================

1. PEANUT ALLERGY CHECK:
   Violations: 0
   Status: ✅ PASS

2. DISLIKE CHECK:
   Violations: 0
   Status: ✅ PASS

3. MUSHROOM EXCLUDE CHECK:
   Count: 0
   Status: ✅ PASS

4. INCLUDE RULES CHECK:
   Chai in breakfast: 7/7 ✅
   Dal in lunch/dinner: 5/4 ✅
   Paneer in lunch/dinner: 2/2 ✅

5. 2-ITEM PAIRING CHECK:
   Slots with 2 items: 28/28
   Status: ✅ PASS

6. GENERIC SUGGESTIONS:
   Count: 0
   Status: ✅ All DB recipes

--------------------------------------------------------------------------------
OVERALL: ✅ ALL CRITICAL CHECKS PASS
================================================================================
```

### When to Run E2E Tests

| Scenario | Run E2E? |
|----------|----------|
| After changing meal generation algorithm | ✅ Yes |
| After modifying pairing rules | ✅ Yes |
| After updating allergen variants | ✅ Yes |
| During regular development | ❌ No (use unit tests) |
| Before release | ✅ Yes |
| CI/CD pipeline | ⚠️ Only on merge to main |

### Test Data Files

| File | Purpose |
|------|---------|
| `tests/test_meal_generation.py` | Unit tests (22 tests, no PostgreSQL) |
| `tests/test_meal_generation_integration.py` | Integration tests (29 tests, no PostgreSQL) |
| `tests/test_meal_generation_e2e.py` | E2E tests (15 tests, real PostgreSQL) |

### Total Backend Test Coverage

| Test Type | Count | PostgreSQL |
|-----------|-------|-----------|
| Unit tests | 22 | No |
| Integration tests | 29 | No |
| E2E tests | 15 | **Yes** |
| **Total** | **66** | |

---

## Meal Generation API & Screen Data Flow Tests

This section provides a comprehensive test plan for validating the meal generation API and verifying data flows correctly through downstream screens (Home → Grocery → Recipe Detail).

### Overview

| Component | Tests | Purpose |
|-----------|-------|---------|
| Backend pytest | 65 | Validate API generates correct data |
| Android E2E | ~70 | Verify data flows to screens correctly |
| Content verification | Checklist | Confirm constraints respected in UI |

### Phase 1: Backend pytest Tests (65 tests)

#### Prerequisites

```bash
cd D:\Abhay\VibeCoding\KKB\backend
.\venv\Scripts\activate  # Windows
# source venv/bin/activate  # Linux/Mac

# Verify PostgreSQL connection
PYTHONPATH=. python -c "from app.db.postgres import get_async_session; print('DB OK')"
```

#### Test Files

| File | Tests | Purpose | PostgreSQL |
|------|-------|---------|------------|
| `test_meal_generation.py` | 22 | Data structures, filtering logic | No |
| `test_meal_generation_integration.py` | 29 | Service unit tests, exclude/include logic | No |
| `test_meal_generation_e2e.py` | 14 | Full generation against real DB | **Yes** |

#### Run Commands

```bash
# Run all meal generation tests (65 total)
PYTHONPATH=. pytest tests/test_meal_generation.py tests/test_meal_generation_integration.py tests/test_meal_generation_e2e.py -v

# Run unit tests only (fast, no DB)
PYTHONPATH=. pytest tests/test_meal_generation.py -v

# Run integration tests only (fast, no DB)
PYTHONPATH=. pytest tests/test_meal_generation_integration.py -v

# Run E2E tests only (slow, hits PostgreSQL)
PYTHONPATH=. pytest tests/test_meal_generation_e2e.py -v -s
```

#### Key Constraint Tests

| Test | File | Verifies |
|------|------|----------|
| `test_sharma_family_no_peanuts` | E2E | Peanut allergy enforcement (SEVERE) |
| `test_sharma_family_no_dislikes` | E2E | No karela/lauki/turai |
| `test_sharma_family_no_mushroom` | E2E | EXCLUDE rule enforcement |
| `test_sharma_family_chai_daily` | E2E | INCLUDE rule (7x breakfast) |
| `test_sharma_family_2_items_per_slot` | E2E | 2-item pairing logic |
| `test_allergen_filtering` | Integration | Allergen variants (peanut/groundnut/mungfali) |
| `test_sattvic_filtering` | Integration | No onion/garlic recipes |

#### Expected Results

```
tests/test_meal_generation.py .................... [22 passed]
tests/test_meal_generation_integration.py ......................... [29 passed]
tests/test_meal_generation_e2e.py .............. [14 passed]

==================== 65 passed in X.XXs ====================
```

### Phase 2: Start Backend Server

After backend tests pass, start the server for Android tests:

```bash
cd D:\Abhay\VibeCoding\KKB\backend
.\venv\Scripts\activate
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Verify health endpoint
curl http://localhost:8000/health
# Expected: {"status": "healthy"}
```

### Phase 3: Android E2E Tests

#### Prerequisites

```bash
# Start emulator (API 34, NOT 36)
"C:/Users/itsab/AppData/Local/Android/Sdk/emulator/emulator.exe" -avd Pixel_6 -no-snapshot-load

# Wait for boot
adb wait-for-device shell getprop sys.boot_completed

# Install app
cd D:\Abhay\VibeCoding\KKB\android
./gradlew installDebug
```

#### Core Flow Tests

| Screen | Test File | Tests | Verifies |
|--------|-----------|-------|----------|
| Home | `HomeScreenTest.kt` | 22 | Meal cards, day navigation, lock state |
| Grocery | `GroceryScreenTest.kt` | 21 | Ingredient list, categories, no prohibited items |
| Recipe Detail | `RecipeDetailScreenTest.kt` | 26 | Recipe info, ingredients, instructions |

#### Run Commands

```bash
cd D:\Abhay\VibeCoding\KKB\android

# Run Home screen tests (22 tests)
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.presentation.home.HomeScreenTest

# Run Grocery screen tests (21 tests)
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.presentation.grocery.GroceryScreenTest

# Run Recipe Detail tests (26 tests)
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.presentation.recipedetail.RecipeDetailScreenTest

# Run all core flow tests
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.rasoiai.app.e2e.flows
```

#### E2E Flow Tests

| Test File | Tests | Purpose |
|-----------|-------|---------|
| `MealPlanGenerationTest.kt` | 3 | Progress animation, navigation |
| `FullUserJourneyTest.kt` | 1 | Complete onboarding → generation → home |
| `RecipeConstraintTest.kt` | 22 | SATTVIC, allergy, dislike constraints |

```bash
# Run meal plan generation flow
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.MealPlanGenerationTest

# Run recipe constraint validation
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.validation.RecipeConstraintTest
```

### Phase 4: Content Verification Checklist

After running tests, verify the Sharma Family profile constraints:

#### Critical Constraints (Must Pass)

| Constraint | Backend Test | Android Verification | Pass |
|------------|--------------|---------------------|------|
| No Peanuts (SEVERE ALLERGY) | `test_sharma_family_no_peanuts` | RecipeConstraintTest | [ ] |
| No Onion/Garlic (SATTVIC) | `test_sattvic_filtering` | RecipeConstraintTest | [ ] |
| No Karela/Baingan/Mushroom | `test_sharma_family_no_dislikes` | RecipeConstraintTest | [ ] |
| 100% VEGETARIAN | `test_vegetarian_only` | RecipeConstraintTest | [ ] |

#### Preference Constraints

| Constraint | Backend Test | Android Verification | Pass |
|------------|--------------|---------------------|------|
| Weekday ≤30min | Service logic | Verify prep time in UI | [ ] |
| Weekend ≤60min | Service logic | Verify prep time in UI | [ ] |
| 2 Items Per Slot | `test_sharma_family_2_items_per_slot` | Meal card count | [ ] |
| Chai Daily (Breakfast) | `test_sharma_family_chai_daily` | Verify chai in breakfast | [ ] |

#### UI Verification Points

| Screen | Verification | TestTag |
|--------|--------------|---------|
| Home | 4 meal cards per day | `MEAL_CARD_*` |
| Home | 7 day tabs | `HOME_DAY_TAB_*` |
| Grocery | Items from meal plan | `GROCERY_ITEM_*` |
| Grocery | No prohibited ingredients | Manual check |
| Recipe Detail | Ingredients list | `RECIPE_INGREDIENTS_LIST` |
| Recipe Detail | Instructions list | `RECIPE_INSTRUCTIONS_LIST` |

### Execution Checklist

#### Backend

- [ ] PostgreSQL running with 3,580 recipes
- [ ] `pytest tests/test_meal_generation.py -v` - 22 PASS
- [ ] `pytest tests/test_meal_generation_integration.py -v` - 29 PASS
- [ ] `pytest tests/test_meal_generation_e2e.py -v -s` - 14 PASS
- [ ] Backend server started on port 8000

#### Android

- [ ] Emulator running (API 34)
- [ ] Backend accessible at http://10.0.2.2:8000
- [ ] HomeScreenTest (22) PASS
- [ ] GroceryScreenTest (21) PASS
- [ ] RecipeDetailScreenTest (26) PASS
- [ ] MealPlanGenerationTest (3) PASS
- [ ] RecipeConstraintTest (22) PASS

### Critical Files

| File | Purpose |
|------|---------|
| `backend/tests/test_meal_generation.py` | Unit tests (22) |
| `backend/tests/test_meal_generation_integration.py` | Integration tests (29) |
| `backend/tests/test_meal_generation_e2e.py` | E2E tests (14) |
| `android/.../e2e/validation/RecipeConstraintTest.kt` | Constraint validation |
| `android/.../e2e/flows/MealPlanGenerationTest.kt` | Generation flow |
| `android/.../presentation/home/HomeScreenTest.kt` | Home UI tests |
| `android/.../presentation/grocery/GroceryScreenTest.kt` | Grocery UI tests |
| `android/.../presentation/recipedetail/RecipeDetailScreenTest.kt` | Recipe detail tests |

### Execution Order

1. **Backend Tests First** - Verify API generates correct data
2. **Start Backend Server** - For Android tests
3. **Android UI Tests** - Verify screens render correctly
4. **Android E2E Tests** - Verify data flows through screens
5. **Content Verification** - Confirm constraints respected in UI

---

## Section 15: Sequential Core Data Flow Test (Real API)

This section describes a focused E2E test that validates the complete data flow from authentication through meal plan generation to downstream screens, using the **real backend API**.

### Execution Approach

**IMPORTANT: Use the automated Compose UI test, NOT manual adb tapping.**

| Approach | Speed | Reliability | Use When |
|----------|-------|-------------|----------|
| **Automated Test (Recommended)** | Fast (~30s) | High | Always - primary execution method |
| Manual Screenshot | Slow (~5min) | Low | Only for debugging failed tests |

**Why automated tests are better:**
1. **FakeGoogleAuthClient** auto-succeeds without real OAuth flow
2. **Compose UI Testing** uses semantic TestTags, not coordinate tapping
3. **Fast execution** - no sleep delays or screenshot pulls
4. **Deterministic** - same result every run

### Primary Execution Command

```bash
cd android

# Run the automated sequential test (RECOMMENDED)
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.FullUserJourneyTest

# View results
start app/build/reports/androidTests/connected/index.html
```

### Test Sequence

| Step | Screen | Action | Content Verification |
|------|--------|--------|---------------------|
| 1 | Auth | Google Sign-In (FakeGoogleAuthClient) | Auth screen displayed, sign-in button works |
| 2 | Onboarding | Complete Sharma Family profile (5 steps) | All 5 onboarding steps complete |
| 3 | Generation | Trigger real API POST `/api/v1/meal-plans/generate` | Progress animation, auto-navigation to Home |
| 4 | Home | View generated meal plan | 7 days visible, 2 items per meal slot |
| 5 | RecipeDetail | Tap breakfast meal | Ingredients list, instructions list displayed |
| 6 | Grocery | Navigate via bottom nav | Category grouping (Vegetables, Spices, etc.) |

### Sharma Family Profile (Test Data)

| Attribute | Value |
|-----------|-------|
| Members | 3 (Ramesh 45, Sunita 42, Aarav 12) |
| Diet | VEGETARIAN + SATTVIC |
| Cuisines | NORTH, SOUTH |
| Dislikes | Karela, Baingan |
| Cooking Time | Weekday 30min, Weekend 60min |
| Busy Days | Monday, Wednesday, Friday |

### How the Test Works (FakeGoogleAuthClient)

```kotlin
// The test uses FakeGoogleAuthClient which:
// 1. Intercepts Google sign-in button tap
// 2. Returns fake credentials instantly (no OAuth popup)
// 3. Sends "fake-firebase-token" to backend
// 4. Backend accepts in DEBUG mode and returns real JWT

@HiltAndroidTest
class FullUserJourneyTest : BaseE2ETest() {
    @Inject lateinit var fakeGoogleAuthClient: FakeGoogleAuthClient

    @Before
    fun setup() {
        fakeGoogleAuthClient.setSignInSuccess()  // Auto-succeed
    }

    @Test
    fun completeJourney() {
        // Taps are handled by Compose UI Testing with TestTags
        // NOT by manual adb coordinate tapping
        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).performClick()
        // ... continues through onboarding, generation, home
    }
}
```

### Screenshot Verification Loop

**For each screen, take a screenshot after the test step to verify pass/fail:**

```
FOR each screen in [Auth, Onboarding, Generation, Home, RecipeDetail, Grocery]:
    1. Run automated test step (Compose UI Testing with TestTags)
    2. Take screenshot: adb shell screencap -p /data/local/tmp/step_N.png
    3. Pull screenshot: adb pull /data/local/tmp/step_N.png
    4. Analyze screenshot visually to verify test passed
    5. IF PASS: Move to next screen
    6. IF FAIL:
       a. Identify issue from screenshot
       b. Fix the code/test
       c. Clear app data: adb shell pm clear com.rasoiai.app
       d. Retest from Step 1
       e. Loop until pass
```

**Screenshot Commands:**
```bash
ADB="/c/Users/itsab/AppData/Local/Android/Sdk/platform-tools/adb.exe"

# Take screenshot after each screen test
"$ADB" shell screencap -p /data/local/tmp/screen_name.png
MSYS_NO_PATHCONV=1 "$ADB" pull /data/local/tmp/screen_name.png ./docs/testing/screenshots/e2e-flow/

# Clear app data for retest
"$ADB" shell pm clear com.rasoiai.app
```

**Screenshot Checklist:**

| Step | Screenshot | Pass Criteria |
|------|------------|---------------|
| 1 | `step1_auth.png` | Logo, "Continue with Google" button visible |
| 2 | `step2_onboarding.png` | 5 steps completed, "Create My Meal Plan" tapped |
| 3 | `step3_generation.png` | 4 progress steps with checkmarks, auto-nav to Home |
| 4 | `step4_home.png` | 7 days, meal cards with 2 items per slot |
| 5 | `step5_recipe.png` | Ingredients list, instructions list |
| 6 | `step6_grocery.png` | Category headers, ingredient items |

### Test Assertions (What the Automated Test Verifies)

The `FullUserJourneyTest` uses TestTags to verify each screen:

#### Step 1: Auth Screen
```kotlin
composeTestRule.onNodeWithTag(TestTags.AUTH_SCREEN).assertIsDisplayed()
composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).assertIsDisplayed()
```

#### Step 2: Onboarding (5 sub-steps)
```kotlin
// Step 1: Household
composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_SIZE_DROPDOWN).assertIsDisplayed()
// Step 2: Diet
composeTestRule.onNodeWithTag(TestTags.PRIMARY_DIET_VEGETARIAN).assertIsSelected()
// Step 3: Cuisine
composeTestRule.onNodeWithTag(TestTags.CUISINE_CARD_NORTH).assertIsSelected()
// Step 4: Dislikes
composeTestRule.onNodeWithText("Karela").assertIsDisplayed()
// Step 5: Schedule
composeTestRule.onNodeWithTag(TestTags.WEEKDAY_TIME_DROPDOWN).assertIsDisplayed()
```

#### Step 3: Generation Screen
```kotlin
composeTestRule.onNodeWithTag(TestTags.GENERATING_SCREEN).assertIsDisplayed()
// Waits for auto-navigation to Home
composeTestRule.waitUntil { /* Home screen visible */ }
```

#### Step 4: Home Screen
```kotlin
composeTestRule.onNodeWithTag(TestTags.HOME_SCREEN).assertIsDisplayed()
composeTestRule.onNodeWithTag(TestTags.MEAL_CARD_breakfast).assertIsDisplayed()
composeTestRule.onNodeWithTag(TestTags.MEAL_CARD_lunch).assertIsDisplayed()
composeTestRule.onNodeWithTag(TestTags.MEAL_CARD_dinner).assertIsDisplayed()
```

#### Step 5: RecipeDetail Screen
```kotlin
composeTestRule.onNodeWithTag(TestTags.RECIPE_DETAIL_SCREEN).assertIsDisplayed()
composeTestRule.onNodeWithTag(TestTags.RECIPE_INGREDIENTS_LIST).assertIsDisplayed()
composeTestRule.onNodeWithTag(TestTags.RECIPE_INSTRUCTIONS_LIST).assertIsDisplayed()
```

#### Step 6: Grocery Screen
```kotlin
composeTestRule.onNodeWithTag(TestTags.GROCERY_SCREEN).assertIsDisplayed()
composeTestRule.onNodeWithTag(TestTags.GROCERY_CATEGORY_vegetables).assertIsDisplayed()
```

### Execution Commands

```bash
cd android

# 1. Ensure backend is running (DEBUG=true for fake-firebase-token)
# In another terminal: cd backend && uvicorn app.main:app --reload

# 2. Run the automated E2E test (RECOMMENDED)
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.FullUserJourneyTest

# 3. View HTML report
start app/build/reports/androidTests/connected/index.html
```

### Critical Files

| File | Purpose |
|------|---------|
| `e2e/flows/CoreDataFlowTest.kt` | **Simplified core flow test (RECOMMENDED)** |
| `e2e/flows/FullUserJourneyTest.kt` | Full journey test (more complex) |
| `e2e/base/BaseE2ETest.kt` | Base class with Hilt setup, timeout helpers |
| `e2e/di/FakeGoogleAuthClient.kt` | Auto-succeeds Google sign-in |
| `e2e/di/FakeAuthModule.kt` | Hilt module replacing real GoogleAuthClient |
| `e2e/robots/*.kt` | Robot classes for each screen |
| `presentation/common/TestTags.kt` | All semantic test tags |

### Latest Test Results (Jan 30, 2026)

**CoreDataFlowTest - ✅ PASSED**

| Step | Screen | Status | Notes |
|------|--------|--------|-------|
| 1 | Auth | ✅ PASS | Logo + Google button verified |
| 2 | Sign In → Onboarding | ✅ PASS | FakeGoogleAuthClient worked |
| 3 | Onboarding (5 steps) | ✅ PASS | All 5 steps completed with defaults |
| 4 | Generation | ✅ PASS | GENERATING_SCREEN testTag found |
| 5 | Home | ✅ PASS | HOME_SCREEN and BOTTOM_NAV verified |

**Execution Command:**
```bash
cd android
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.CoreDataFlowTest
```

### Troubleshooting

| Issue | Solution |
|-------|----------|
| Test stuck on Auth | Check `FakeGoogleAuthClient.setSignInSuccess()` in test setup |
| Generation fails | Verify backend running at `http://10.0.2.2:8000` (emulator address) |
| Home shows empty | Check `DEBUG=true` in backend `.env` file |
| Test timeout | Increase timeout in `BaseE2ETest` or use `waitUntil` helper |
| Emulator not found | Run `adb devices` and ensure emulator is running |
| API 36 issues | Use API 34 emulator (API 36 has Espresso compatibility bugs) |

### Common Mistakes to Avoid

| Mistake | Correct Approach |
|---------|------------------|
| Manual `adb input tap` commands | Use Compose UI Testing with `performClick()` |
| Taking screenshots for each step | Run automated test, only screenshot on failure |
| Using real Google OAuth | Use `FakeGoogleAuthClient` which auto-succeeds |
| Coordinate-based tapping | Use semantic `TestTags` for element selection |
| Sleep-based waits | Use `waitUntil {}` with condition checks |

---

*Last Updated: January 30, 2026*
*Document Version: 1.5 - Added CoreDataFlowTest results and simplified test execution*
