# RasoiAI End-to-End Testing Guide

This document provides a comprehensive testing guide for the RasoiAI Android app. Use this prompt to perform complete E2E testing on an Android emulator.

---

## TESTING PROMPT

```
I need you to perform complete End-to-End testing of the RasoiAI Android app on an emulator.

## Backend Status
- Firestore database: `rasoiai-6dcdd`
- **3,590 recipes** (imported from khanakyabanega)
- 12 festivals seeded
- Auth accepts `fake-firebase-token` for testing

## Test Objective
Test the complete user journey as a NEW user with a 3-member family, validating:
- All screen interactions and UI states
- Data flow between screens (meal plan → grocery, favorites, stats)
- API endpoint responses and error handling
- Room database state after each operation
- Offline-first behavior and sync

## Test User Profile: "Sharma Family"

### Account
- Login: Google OAuth (test.sharma@gmail.com)
- New user (clear app data before testing)

### Household (3 Members)
| Name | Type | Age | Health Conditions |
|------|------|-----|-------------------|
| Ramesh | ADULT | 45 | DIABETIC, LOW_OIL |
| Sunita | ADULT | 42 | LOW_SALT |
| Aarav | CHILD | 12 | NO_SPICY |

### Dietary Preferences
- Primary Diet: VEGETARIAN
- Restrictions: SATTVIC (no onion/garlic)
- Cuisines: NORTH, SOUTH
- Spice Level: MEDIUM

### Allergies & Dislikes
- Karela (Bitter Gourd) - DISLIKE
- Baingan (Eggplant) - DISLIKE
- Mushroom - DISLIKE
- Peanuts - ALLERGY (severe)
- Cashews - ALLERGY (mild)

### Cooking Preferences
- Weekday Time: 30 minutes
- Weekend Time: 60 minutes
- Busy Days: Monday, Wednesday, Friday

## Test Execution Steps

1. **Setup**: Start emulator (API 34), clear app data, install debug build
2. **Auth**: Complete Google OAuth login as new user
3. **Onboarding**: Enter ALL test data across 5 steps
4. **Generate**: Create meal plan and verify generation
5. **Validate**: Cross-check data on ALL screens:
   - Home: Meal cards match preferences (no SATTVIC violations)
   - Grocery: Items derived from meal plan recipes
   - Favorites: Add/remove recipes, verify persistence
   - Stats: New user state (0 streak, empty charts)
   - Settings: Preferences match onboarding input
   - Pantry: Add items, test expiry alerts
   - Recipe Rules: Create include/exclude rules
   - Chat: AI responses use context
6. **API/DB**: Verify endpoints and database after each action
7. **Combinations**: Test all state combinations (see matrix below)

## Test All Combinations

For each screen, test these states:
- Loading state
- Empty state
- Populated state
- Error state (simulate API failure)
- Offline state (airplane mode)

Cross-screen data validation:
- Meal plan recipes appear in Favorites after favoriting
- Grocery list updates when meal plan changes
- Stats reflect actual cooking history
- Recipe Rules affect next meal plan generation

## Output Required
1. Test execution report with pass/fail for each phase
2. Screenshots of failures
3. Database state verification
4. API response logs
5. List of bugs found with reproduction steps
```

---

## Testing Framework

| Layer | Framework | Purpose |
|-------|-----------|---------|
| Unit Tests | JUnit5 + MockK | ViewModel, Repository, UseCase |
| UI Tests | Compose UI Testing | Screen composables with mock state |
| Integration Tests | Hilt + FakeGoogleAuthClient | Full navigation with real backend |
| E2E Tests | ADB + Manual/Automated | Complete user journeys |
| Flow Testing | Turbine | StateFlow/Channel in ViewModels |

### Current UI Test Coverage (~265 tests across 13 screens)

| Screen | Test File | Tests |
|--------|-----------|-------|
| Auth | `AuthScreenTest.kt` | 18 |
| Auth (Integration) | `AuthIntegrationTest.kt` | 9 |
| Onboarding | `OnboardingScreenTest.kt` | 41 |
| Home | `HomeScreenTest.kt` | 22 |
| Recipe Detail | `RecipeDetailScreenTest.kt` | 26 |
| Grocery | `GroceryScreenTest.kt` | 21 |
| Chat | `ChatScreenTest.kt` | 17 |
| Favorites | `FavoritesScreenTest.kt` | 17 |
| Stats | `StatsScreenTest.kt` | 21 |
| Settings | `SettingsScreenTest.kt` | 15 |
| Pantry | `PantryScreenTest.kt` | 18 |
| Recipe Rules | `RecipeRulesScreenTest.kt` | 22 |
| Cooking Mode | `CookingModeScreenTest.kt` | 27 |

All test files are in `android/app/src/androidTest/java/com/rasoiai/app/presentation/`.

---

## Test Architecture: Real Backend + Fake Auth

The E2E tests use **Option B: Real Backend + Fake Google Auth Only**. This means:
- Google OAuth is bypassed using `FakeGoogleAuthClient`
- All API calls go to the real Python backend
- Database is Firebase Firestore (real, not emulated)

### Flow Diagram

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
│  3. AuthViewModel calls AuthRepository.signInWithGoogle("fake-firebase-token")│
│                    ↓                                                        │
│  4. REAL AuthRepositoryImpl calls backend API:                              │
│     POST http://10.0.2.2:8000/api/v1/auth/firebase                          │
│     Body: { "firebase_token": "fake-firebase-token" }                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                     ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PYTHON BACKEND (Firestore)                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  5. Backend receives "fake-firebase-token"                                  │
│     firebase.py detects: settings.debug=True AND token=="fake-firebase-token"│
│     Returns mock user: { uid: "fake-user-id",                               │
│                          email: "test@example.com",                         │
│                          name: "Test User" }                                │
│                    ↓                                                        │
│  6. Creates/finds user in Firestore                                         │
│                    ↓                                                        │
│  7. Returns REAL JWT tokens:                                                │
│     { access_token: "eyJ...", refresh_token: "eyJ...", user: {...} }        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                     ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                      ANDROID APP (authenticated)                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  8. App stores JWT in DataStore                                             │
│  9. Navigation: Auth → Onboarding (if new user) → Home                      │
│  10. All subsequent API calls use REAL JWT to Firestore backend             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### What's Fake vs Real

| Component | Status | Details |
|-----------|--------|---------|
| Google OAuth | **FAKE** | `FakeGoogleAuthClient` bypasses real Google sign-in |
| Firebase Token | **FAKE** | Hardcoded `"fake-firebase-token"` |
| Backend API | **REAL** | Python FastAPI at `localhost:8000` |
| JWT Tokens | **REAL** | Backend generates real JWTs |
| Database | **REAL** | Firebase Firestore |
| All Repositories | **REAL** | Real implementations calling real APIs |

### Key Test Files

| File | Purpose |
|------|---------|
| `e2e/di/FakeGoogleAuthClient.kt` | Bypasses Google OAuth, returns fake credentials |
| `e2e/di/FakeAuthModule.kt` | Hilt module that replaces real GoogleAuthClient |
| `e2e/base/BaseE2ETest.kt` | Base test class with Hilt setup |
| `presentation/common/TestTags.kt` | All semantic test tags for UI elements |
| `presentation/*ScreenTest.kt` | UI tests for each screen (13 files) |

---

## Pre-Test Setup

### Emulator Configuration

```bash
# Required: API 34 emulator (API 36 has Espresso issues)
# Recommended: Pixel 6 or similar with 1080x2400 resolution

# Start emulator without snapshot for clean state
$ANDROID_HOME/emulator/emulator -avd Pixel_6_API_34 -no-snapshot-load

# Verify emulator is running
adb devices

# Wait for boot completion
adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'
```

### App Installation

```bash
cd android

# Clean build
./gradlew clean assembleDebug

# Install
./gradlew installDebug

# Clear app data for fresh start (REQUIRED for new user test)
adb shell pm clear com.rasoiai.app.debug

# Verify installation
adb shell pm list packages | grep rasoiai
```

### Backend Setup (Firestore)

**Recipe Database Status:** 3,590 recipes already imported from khanakyabanega.

| Category | Count |
|----------|-------|
| North Indian | 3,124 |
| South Indian | 358 |
| West Indian | 85 |
| East Indian | 23 |
| Vegetarian | 3,482 |
| Vegan | 1,347 |

```bash
cd backend

# Activate virtual environment
source venv/bin/activate  # Linux/Mac
# .\venv\Scripts\activate  # Windows

# Install dependencies (first time only)
pip install -r requirements.txt

# Configure Firebase (choose one):
# Option 1: Service Account (recommended for testing)
export FIREBASE_CREDENTIALS_PATH="./rasoiai-firebase-service-account.json"

# Option 2: Firebase Emulator (for local development)
# export FIRESTORE_EMULATOR_HOST="localhost:8080"
# firebase emulators:start --only firestore

# Start backend server
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Verify API is running
curl http://localhost:8000/health

# Verify recipe database (already seeded with 3,590 recipes)
python scripts/verify_recipe_import.py

# Only if you need to re-seed (normally not required):
# PYTHONPATH=. python scripts/seed_firestore.py
```

### Firebase Service Account Setup

1. Go to Firebase Console → Project Settings → Service Accounts
2. Click "Generate new private key"
3. Save as `backend/rasoiai-firebase-service-account.json`
4. Add to `.gitignore` (never commit credentials)

### Logging

```bash
# Filter RasoiAI logs
adb logcat -s RasoiAI:V

# Full verbose logging
adb logcat -v time | grep -E "(RasoiAI|rasoiai)"

# Save logs to file
adb logcat -s RasoiAI:V > test_logs_$(date +%Y%m%d_%H%M%S).txt
```

---

## Test Data Profile: Sharma Family

### Complete Profile Summary

| Category | Attribute | Value |
|----------|-----------|-------|
| **Account** | Email | test.sharma@gmail.com |
| **Account** | Auth Method | Google OAuth |
| **Household** | Size | 3 |
| **Diet** | Primary | VEGETARIAN |
| **Diet** | Restrictions | SATTVIC |
| **Cuisine** | Preferences | NORTH, SOUTH |
| **Cuisine** | Spice Level | MEDIUM |
| **Cooking** | Weekday Time | 30 min |
| **Cooking** | Weekend Time | 60 min |
| **Cooking** | Busy Days | Mon, Wed, Fri |

### Family Members Detail

| # | Name | Type | Age | Conditions | Notes |
|---|------|------|-----|------------|-------|
| 1 | Ramesh | ADULT | 45 | DIABETIC, LOW_OIL | Family head, controls sugar |
| 2 | Sunita | ADULT | 42 | LOW_SALT | Heart-healthy diet |
| 3 | Aarav | CHILD | 12 | NO_SPICY | School-going, needs mild food |

### Allergies (Food Safety Critical)

| Ingredient | Severity | Action |
|------------|----------|--------|
| Peanuts | SEVERE | Must never appear in any recipe |
| Cashews | MILD | Warn but allow with confirmation |

### Dislikes (Preference-Based)

| Ingredient | Hindi Name | Reason |
|------------|------------|--------|
| Karela | करेला (Bitter Gourd) | Taste preference |
| Baingan | बैंगन (Eggplant) | Texture dislike |
| Mushroom | मशरूम | Family preference |

### Expected Meal Plan Constraints

Based on this profile, generated meal plans MUST:
1. Be 100% VEGETARIAN (no meat, fish, eggs) - **3,482 vegetarian recipes available**
2. Exclude onion and garlic (SATTVIC)
3. Never contain peanuts (ALLERGY)
4. Avoid karela, baingan, mushroom recipes
5. Include NORTH and SOUTH Indian cuisines - **3,124 North + 358 South available**
6. Have ≤30 min prep on weekdays
7. Have ≤60 min prep on weekends
8. Suggest quick meals on Mon/Wed/Fri (busy days)
9. Consider DIABETIC needs for Ramesh (low sugar)
10. Consider LOW_SALT for Sunita
11. Consider NO_SPICY for Aarav (mild preparations)

**Recipe Rules to Create (Phase 11):**
| Rule | Type | Frequency | Enforcement |
|------|------|-----------|-------------|
| Chai → Breakfast | Recipe/Meal-Slot | Daily | REQUIRED |
| Chai → Snacks | Recipe/Meal-Slot | Daily | REQUIRED |
| Moringa/Drumstick | Ingredient Include | 1x/week | PREFERRED |
| Paneer | Ingredient Exclude | NEVER | REQUIRED |
| Green Leafy | Nutrition Goal | 5x/week | PREFERRED |

**Sample Valid Recipes (from 3,590 database):**
- Aloo Gobi, Dal Tadka, Vegetable Pulao (North)
- Masala Dosa, Idli Sambar, Drumstick Sambar (South)
- Khichdi, Poha, Upma, Chai (Breakfast options)
- Sahjan ki Sabzi, Moringa Dal (Moringa recipes)

---

## Phase 1: Authentication Testing

### Test 1.1: Fresh App Launch

**Precondition:** App data cleared, emulator connected

**Steps:**
1. Launch app: `adb shell am start -n com.rasoiai.app.debug/com.rasoiai.app.MainActivity`
2. Observe splash screen
3. Wait for navigation

**Expected Results:**
- [ ] Splash screen displays RasoiAI logo
- [ ] Tagline "AI-Powered Meal Planning" visible
- [ ] Animation completes in 2-3 seconds
- [ ] Navigates to Auth screen (not Home - new user)
- [ ] No crash or ANR

**Verify Database:**
```sql
-- DataStore should be empty/default
-- is_onboarded = false
-- auth_token = null
```

### Test 1.2: Google OAuth Login

**Steps:**
1. Tap "Continue with Google" button
2. Select test Google account from picker
3. Complete OAuth consent flow
4. Wait for redirect

**Expected Results:**
- [ ] Google One Tap or account picker appears
- [ ] Correct test account selectable
- [ ] Loading indicator during auth
- [ ] Successful authentication (no error)
- [ ] Redirects to Onboarding Step 1 (NOT Home)

**Verify API:**
```
POST /api/v1/auth/firebase
Request: { "idToken": "<firebase_id_token>" }
Response: 200 OK {
  "accessToken": "eyJ...",
  "refreshToken": "...",
  "expiresIn": 3600
}
```

**Verify Database:**
```sql
-- After successful auth
-- auth_token should be set (encrypted)
-- user_id should be set
-- is_onboarded should still be false
```

**Test Variations:**
- [ ] Cancel OAuth flow → should stay on Auth screen
- [ ] Select wrong account → should allow retry
- [ ] Network timeout → should show error with retry

---

## Phase 2: Onboarding Testing (5 Steps)

### Test 2.1: Step 1 - Household Setup

**UI State Verification:**
- [ ] Progress indicator shows 1/5 (20%)
- [ ] "Household Size" label visible
- [ ] Dropdown shows "Select size" placeholder
- [ ] "Add Family Member" button visible but members section empty
- [ ] "Next" button disabled initially

**Steps:**
1. Tap household size dropdown
2. Select "3 people"
3. Tap "Add Family Member"
4. Fill member 1: Ramesh, ADULT, 45, DIABETIC + LOW_OIL
5. Save member 1
6. Add member 2: Sunita, ADULT, 42, LOW_SALT
7. Add member 3: Aarav, CHILD, 12, NO_SPICY
8. Verify all 3 members displayed
9. Tap "Next"

**Expected Results:**
- [ ] Dropdown expands with options 1-8+
- [ ] Selection updates dropdown display
- [ ] Bottom sheet opens for member entry
- [ ] Name field accepts text input
- [ ] Age field is numeric only
- [ ] Type selector (ADULT/CHILD/SENIOR) works
- [ ] Health conditions are multi-select checkboxes
- [ ] Save adds member to list
- [ ] Member cards show name, age, conditions
- [ ] Edit/Delete icons on each card
- [ ] "Next" enables when members added

**Member Entry Form Fields:**
| Field | Type | Validation |
|-------|------|------------|
| Name | Text | Required, 2-50 chars |
| Type | Select | ADULT, CHILD, SENIOR |
| Age | Number | 1-120, required |
| Conditions | Multi-select | DIABETIC, LOW_OIL, LOW_SALT, NO_SPICY, GLUTEN_FREE, LACTOSE_FREE |

**Test Variations:**
- [ ] Edit existing member (change Ramesh's age to 46)
- [ ] Delete member and re-add
- [ ] Try "Next" with 0 members → should be blocked
- [ ] Add 8+ members (max household) → should allow or limit
- [ ] Add member with same name → should allow

### Test 2.2: Step 2 - Dietary Preferences

**UI State Verification:**
- [ ] Progress shows 2/5 (40%)
- [ ] Primary diet options visible (radio buttons)
- [ ] VEGETARIAN pre-selected (most common in India)
- [ ] Dietary restrictions section (checkboxes)
- [ ] "Next" enabled by default

**Steps:**
1. Verify VEGETARIAN is pre-selected
2. Check restriction: SATTVIC
3. Verify SATTVIC description shows "No onion, garlic"
4. Tap "Next"

**Expected Results:**
- [ ] Radio buttons are mutually exclusive
- [ ] VEGETARIAN shows description
- [ ] SATTVIC checkbox toggles
- [ ] Multiple restrictions can be selected
- [ ] Visual feedback on selection

**Diet Options:**
| Option | Description | Excludes |
|--------|-------------|----------|
| VEGETARIAN | No meat, fish, eggs | Meat, fish, eggs |
| NON_VEGETARIAN | Includes all foods | Nothing |
| EGGETARIAN | Vegetarian + eggs | Meat, fish |
| VEGAN | No animal products | All animal products |

**Restriction Options:**
| Option | Description | Additional Excludes |
|--------|-------------|---------------------|
| SATTVIC | Pure/spiritual diet | Onion, garlic |
| JAIN | No root vegetables | Onion, garlic, potato, carrot |
| HALAL | Islamic dietary laws | Non-halal meat |
| GLUTEN_FREE | Celiac-safe | Wheat, barley, rye |

**Test Variations:**
- [ ] Select NON_VEGETARIAN, then switch back to VEGETARIAN
- [ ] Select JAIN + SATTVIC together
- [ ] Proceed without any restrictions → should allow

### Test 2.3: Step 3 - Cuisine Preferences

**UI State Verification:**
- [ ] Progress shows 3/5 (60%)
- [ ] 4 cuisine cards in 2x2 grid
- [ ] Each card has emoji, name, description
- [ ] Spice level dropdown visible
- [ ] "Next" disabled until cuisine selected

**Steps:**
1. Tap NORTH Indian card → verify selection
2. Tap SOUTH Indian card → verify multi-select
3. Select spice level: MEDIUM from dropdown
4. Tap "Next"

**Expected Results:**
- [ ] Cards show selection state (border/checkmark)
- [ ] Multiple cuisines can be selected
- [ ] At least 1 cuisine required to proceed
- [ ] Spice dropdown has 4 options
- [ ] Selection persists if navigating back

**Cuisine Cards:**
| Cuisine | Emoji | Key Dishes |
|---------|-------|------------|
| NORTH | 🍛 | Dal Makhani, Paneer, Roti |
| SOUTH | 🥘 | Dosa, Sambar, Idli |
| EAST | 🍲 | Fish curry, Rasgulla, Mishti |
| WEST | 🥗 | Dhokla, Thepla, Pav Bhaji |

**Spice Levels:**
| Level | Description |
|-------|-------------|
| MILD | Very little spice, suitable for children |
| MEDIUM | Balanced spice, most popular |
| SPICY | Noticeable heat |
| VERY_SPICY | For spice lovers |

**Test Variations:**
- [ ] Try "Next" with no cuisine → should be blocked
- [ ] Select all 4 cuisines → should allow
- [ ] Change spice level multiple times
- [ ] Deselect a cuisine after selecting

### Test 2.4: Step 4 - Dislikes & Allergies

**UI State Verification:**
- [ ] Progress shows 4/5 (80%)
- [ ] "Allergies" section with severity options
- [ ] "Dislikes" section with common ingredients
- [ ] Search bar for ingredients
- [ ] Custom ingredient input
- [ ] "Next" enabled (optional step)

**Steps:**
1. **Allergies Section:**
   - Add "Peanuts" with SEVERE
   - Add "Cashews" with MILD
2. **Dislikes Section:**
   - Select Karela from common list
   - Select Baingan from common list
   - Select Mushroom from common list
3. Search "Jack" → verify Jackfruit appears
4. Add custom: type "Raw Papaya" → add
5. Remove custom ingredient by tapping X
6. Tap "Next"

**Expected Results:**
- [ ] Allergy items show severity badge
- [ ] Dislikes show as filter chips
- [ ] Selected items have checkmark
- [ ] Search filters ingredient list in real-time
- [ ] Custom ingredients appear separately
- [ ] Can remove custom ingredients
- [ ] Bilingual names (English + Hindi)

**Common Dislike Ingredients:**
| English | Hindi | Category |
|---------|-------|----------|
| Karela | करेला | Vegetables |
| Baingan | बैंगन | Vegetables |
| Lauki | लौकी | Vegetables |
| Tori | तोरी | Vegetables |
| Parwal | परवल | Vegetables |
| Mushroom | मशरूम | Vegetables |
| Capsicum | शिमला मिर्च | Vegetables |
| Cabbage | पत्ता गोभी | Vegetables |

**Test Variations:**
- [ ] Proceed with no allergies/dislikes → should allow
- [ ] Add 10+ dislikes → verify scrolling
- [ ] Search with no results → show empty state
- [ ] Add same ingredient twice → should prevent

### Test 2.5: Step 5 - Cooking Schedule

**UI State Verification:**
- [ ] Progress shows 5/5 (100%)
- [ ] Weekday cooking time dropdown
- [ ] Weekend cooking time dropdown
- [ ] Busy days as day chips (Mon-Sun)
- [ ] Button text: "Create My Meal Plan"

**Steps:**
1. Set weekday cooking time: 30 minutes
2. Set weekend cooking time: 60 minutes
3. Select busy days: MON, WED, FRI
4. Tap "Create My Meal Plan"

**Expected Results:**
- [ ] Dropdowns show time options (15-90 min)
- [ ] Day chips are multi-select
- [ ] Selected days show different state
- [ ] Button triggers meal plan generation

**Cooking Time Options:**
| Option | Description |
|--------|-------------|
| 15 min | Quick meals only |
| 30 min | Standard cooking |
| 45 min | Moderate cooking |
| 60 min | Elaborate meals |
| 90 min | Complex recipes |

**Test Variations:**
- [ ] Set minimum times (15 min both)
- [ ] Set maximum times (90 min both)
- [ ] Select all 7 days as busy
- [ ] Select no busy days

---

## Phase 3: Meal Plan Generation

### Test 3.1: Generation Progress

**Trigger:** "Create My Meal Plan" button tapped

**Expected Animation Sequence:**
| Step | Message | Duration | State |
|------|---------|----------|-------|
| 1 | "Analyzing your preferences..." | 0.8s | Spinner → ✓ |
| 2 | "Checking upcoming festivals..." | 0.6s | Spinner → ✓ |
| 3 | "Generating personalized recipes..." | 1.2s | Spinner → ✓ |
| 4 | "Building your grocery list..." | 0.6s | Spinner → ✓ |

**Expected Results:**
- [ ] Each step shows progress
- [ ] Smooth spinner animation
- [ ] Checkmark on completion
- [ ] Total time ~3.2 seconds (or API response time)
- [ ] Auto-navigates to Home screen

**Verify API Request:**
```json
POST /api/v1/meal-plans/generate
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
```

**Verify API Response:**
```json
Response: 201 Created
{
  "mealPlanId": "mp_abc123",
  "weekStartDate": "2026-01-26",
  "weekEndDate": "2026-02-01",
  "days": [
    {
      "date": "2026-01-26",
      "dayName": "Monday",
      "isBusyDay": true,
      "festival": null,
      "meals": {
        "breakfast": {"recipeId": "r1", "recipeName": "Poha", "prepTime": 20},
        "lunch": {"recipeId": "r2", "recipeName": "Dal Chawal", "prepTime": 25},
        "dinner": {"recipeId": "r3", "recipeName": "Khichdi", "prepTime": 30},
        "snacks": {"recipeId": "r4", "recipeName": "Fruit Chaat", "prepTime": 10}
      }
    }
    // ... 6 more days
  ],
  "totalRecipes": 28,
  "groceryItemCount": 45
}
```

**Verify Database After Generation:**
```sql
-- Meal Plan created
SELECT id, week_start_date, week_end_date, created_at
FROM meal_plans
ORDER BY created_at DESC LIMIT 1;

-- Meal items created (28 = 4 meals x 7 days)
SELECT COUNT(*) FROM meal_plan_items
WHERE meal_plan_id = '<new_plan_id>';
-- Expected: 28

-- Grocery items generated
SELECT category, COUNT(*) as count
FROM grocery_items
WHERE meal_plan_id = '<new_plan_id>'
GROUP BY category;

-- Verify NO peanuts in any recipe (ALLERGY)
SELECT mpi.* FROM meal_plan_items mpi
JOIN recipe_ingredients ri ON mpi.recipe_id = ri.recipe_id
WHERE ri.ingredient_name ILIKE '%peanut%';
-- Expected: 0 rows

-- Verify NO onion/garlic (SATTVIC)
SELECT mpi.* FROM meal_plan_items mpi
JOIN recipe_ingredients ri ON mpi.recipe_id = ri.recipe_id
WHERE ri.ingredient_name ILIKE '%onion%' OR ri.ingredient_name ILIKE '%garlic%';
-- Expected: 0 rows

-- Verify cooking times on busy days ≤30 min
SELECT date, meal_type, recipe_name, prep_time_minutes
FROM meal_plan_items
WHERE EXTRACT(DOW FROM date) IN (1, 3, 5) -- Mon, Wed, Fri
AND prep_time_minutes > 30;
-- Expected: 0 rows
```

---

## Phase 4: Home Screen Testing

### Test 4.1: Initial Home Load

**Precondition:** Meal plan generated successfully

**UI State Verification:**
- [ ] Week header shows current week dates
- [ ] 7 day tabs/cards in horizontal scroll
- [ ] Current day is highlighted/selected
- [ ] 4 meal cards visible (Breakfast, Lunch, Dinner, Snacks)
- [ ] Bottom navigation shows 5 items
- [ ] HOME is selected in bottom nav

**Expected Results:**
- [ ] Meal cards show recipe name, prep time, image
- [ ] No empty meal slots
- [ ] Festival badge if applicable (check date)
- [ ] Swipe gesture works for day navigation

**Verify Data Matches Meal Plan:**
```sql
-- Get today's meals
SELECT meal_type, recipe_name, prep_time_minutes, is_locked
FROM meal_plan_items
WHERE meal_plan_id = '<current_plan_id>'
AND date = CURRENT_DATE
ORDER BY
  CASE meal_type
    WHEN 'breakfast' THEN 1
    WHEN 'lunch' THEN 2
    WHEN 'dinner' THEN 3
    WHEN 'snacks' THEN 4
  END;
```

### Test 4.2: Day Navigation

**Steps:**
1. Note current day's meals
2. Swipe left to next day
3. Verify different meals displayed
4. Tap on specific day (e.g., Friday)
5. Verify Friday's meals
6. Navigate to Sunday (weekend)
7. Verify longer prep time recipes (weekend = 60 min limit)

**Expected Results:**
- [ ] Each day shows different recipes
- [ ] Busy days (Mon/Wed/Fri) have quick recipes
- [ ] Weekend days may have elaborate recipes
- [ ] Day selection updates meal cards

### Test 4.3: Meal Card Actions

**Test Lock/Unlock:**
1. Long-press on Monday Breakfast
2. Verify action menu appears (Lock, Swap)
3. Tap Lock
4. Verify lock icon on meal card
5. Tap unlock
6. Verify lock removed

**Test Swap:**
1. Tap Swap on any unlocked meal
2. Verify suggestion bottom sheet appears
3. Verify alternatives match dietary constraints
4. Select an alternative
5. Verify meal card updates

**Expected Results:**
- [ ] Lock persists after app restart
- [ ] Locked meals cannot be swapped
- [ ] Swap suggestions are VEGETARIAN + SATTVIC
- [ ] Swap updates database immediately

**Verify Lock API:**
```
PUT /api/v1/meal-plans/{planId}/items/{itemId}/lock
Request: { "isLocked": true }
Response: 200 OK
```

**Verify Swap API:**
```
POST /api/v1/meal-plans/{planId}/items/{itemId}/swap
Request: { "newRecipeId": "r_xyz" }
Response: 200 OK {
  "updatedMealItem": {...},
  "affectedGroceryItems": [...]
}
```

### Test 4.4: Recipe Detail Navigation

**Steps:**
1. Tap on any meal card
2. Verify navigation to Recipe Detail screen
3. Verify recipe content:
   - Name and image
   - Prep time, cook time, difficulty
   - Ingredients list with quantities
   - Step-by-step instructions
   - Nutrition information
4. Tap back to return to Home

**Expected Results:**
- [ ] Full recipe loads
- [ ] Ingredients grouped by category
- [ ] Quantities scale-appropriate (3 servings)
- [ ] Instructions numbered with tips
- [ ] Nutrition shows: calories, protein, carbs, fat, fiber

**Verify Recipe API:**
```
GET /api/v1/recipes/{recipeId}
Response: 200 OK {
  "id": "r_abc",
  "name": "Vegetable Pulao",
  "cuisineType": "NORTH",
  "dietaryTags": ["VEGETARIAN", "SATTVIC"],
  "prepTimeMinutes": 15,
  "cookTimeMinutes": 25,
  "difficulty": "MEDIUM",
  "servings": 4,
  "ingredients": [
    {"name": "Basmati Rice", "quantity": 2, "unit": "cups", "category": "GRAINS"},
    {"name": "Mixed Vegetables", "quantity": 1, "unit": "cup", "category": "VEGETABLES"}
  ],
  "instructions": [
    {"step": 1, "instruction": "Wash and soak rice for 30 minutes", "duration": 30}
  ],
  "nutrition": {
    "calories": 350,
    "proteinGrams": 8,
    "carbohydratesGrams": 65,
    "fatGrams": 5,
    "fiberGrams": 3
  }
}
```

---

## Phase 5: Grocery Screen Testing

### Test 5.1: Grocery List Validation

**Navigation:** Tap GROCERY in bottom nav

**UI State Verification:**
- [ ] Grocery items grouped by category
- [ ] Each item shows: name, quantity, unit
- [ ] Unchecked state by default
- [ ] Category headers collapsible
- [ ] Search bar at top
- [ ] WhatsApp share button visible

**Expected Results:**
- [ ] Items derived from current meal plan
- [ ] Quantities aggregated across recipes
- [ ] No peanuts present (ALLERGY)
- [ ] No onion/garlic (SATTVIC)

**Verify Grocery Data:**
```sql
SELECT
  category,
  ingredient_name,
  SUM(quantity) as total_qty,
  unit
FROM grocery_items
WHERE meal_plan_id = '<current_plan_id>'
GROUP BY category, ingredient_name, unit
ORDER BY category, ingredient_name;

-- Verify no allergens
SELECT * FROM grocery_items
WHERE ingredient_name ILIKE '%peanut%';
-- Expected: 0 rows
```

### Test 5.2: Check/Uncheck Workflow

**Steps:**
1. Tap checkbox on first item (e.g., Rice)
2. Verify checked state (strikethrough/dimmed)
3. Scroll and check 3 more items
4. Verify "Checked: 4 of X" counter updates
5. Uncheck one item
6. Use "Mark All" menu option
7. Use "Clear Checked" option

**Expected Results:**
- [ ] Individual checkboxes work
- [ ] Counter updates in real-time
- [ ] Mark All checks everything
- [ ] Clear Checked resets all
- [ ] State persists after leaving screen

**Verify Database:**
```sql
SELECT COUNT(*) as checked FROM grocery_items WHERE is_checked = 1;
SELECT COUNT(*) as unchecked FROM grocery_items WHERE is_checked = 0;
```

### Test 5.3: WhatsApp Share

**Steps:**
1. Tap WhatsApp share button
2. Verify share sheet appears
3. Verify formatted text preview
4. Select WhatsApp (or cancel)

**Expected Results:**
- [ ] Share intent fires
- [ ] Text is formatted with emojis
- [ ] Organized by category
- [ ] Quantities included

**Verify API:**
```
GET /api/v1/grocery/whatsapp?mealPlanId={id}
Response: 200 OK {
  "formattedText": "🛒 *Grocery List - Week of Jan 26*\n\n*Vegetables*\n- Tomato: 500g\n..."
}
```

### Test 5.4: Grocery After Meal Swap

**Steps:**
1. Go to Home, swap a meal
2. Return to Grocery
3. Verify list updated with new ingredients
4. Verify old recipe ingredients removed (if not used elsewhere)

**Expected Results:**
- [ ] Real-time update after swap
- [ ] Quantities recalculated
- [ ] No stale ingredients

---

## Phase 6: Chat Screen Testing

### Test 6.1: Initial Chat State

**Navigation:** Tap CHAT in bottom nav

**UI State Verification:**
- [ ] Welcome message from AI assistant
- [ ] Suggestion chips visible
- [ ] Text input field at bottom
- [ ] Send button (disabled when empty)

### Test 6.2: Recipe Query

**Steps:**
1. Type: "What can I make for dinner tonight?"
2. Tap send
3. Observe typing indicator
4. Verify AI response

**Expected Results:**
- [ ] Message appears in chat
- [ ] Typing indicator shows
- [ ] Response arrives within 5 seconds
- [ ] Response mentions VEGETARIAN + SATTVIC options
- [ ] Recipe cards may be embedded

**Verify API:**
```
POST /api/v1/chat/message
Request: {
  "content": "What can I make for dinner tonight?",
  "context": {
    "currentMealPlan": "<plan_id>",
    "userPreferences": {...}
  }
}
Response: 200 OK {
  "response": "Based on your preferences, here are some dinner ideas...",
  "suggestions": ["Dal Tadka", "Vegetable Pulao"],
  "recipeCards": [...]
}
```

### Test 6.3: Context Awareness

**Test queries that require user context:**
1. "What's for lunch today?" → Should check meal plan
2. "My son doesn't like spicy food" → Should remember Aarav is NO_SPICY
3. "Can you suggest something without paneer?" → Should exclude
4. "Is there anything with less oil?" → Should consider Ramesh's LOW_OIL

**Expected Results:**
- [ ] AI uses meal plan context
- [ ] AI knows family member preferences
- [ ] Personalized suggestions

### Test 6.4: Recipe Navigation from Chat

**Steps:**
1. Ask for a recipe suggestion
2. Tap on recipe card in response
3. Verify navigation to Recipe Detail

**Expected Results:**
- [ ] Recipe cards are tappable
- [ ] Navigation works from chat

---

## Phase 7: Favorites Screen Testing

### Test 7.1: Empty State

**Navigation:** Tap FAVORITES in bottom nav

**UI State for New User:**
- [ ] Empty state illustration
- [ ] "No favorites yet" message
- [ ] "Browse recipes" CTA

### Test 7.2: Add to Favorites

**Steps:**
1. Navigate to any Recipe Detail
2. Tap heart/favorite icon
3. Verify icon fills (selected state)
4. Navigate to Favorites screen
5. Verify recipe appears in list

**Repeat for 3 recipes:**
- Recipe 1: Breakfast recipe
- Recipe 2: Lunch recipe
- Recipe 3: Dinner recipe

**Expected Results:**
- [ ] Heart icon toggles
- [ ] Favorites list updates immediately
- [ ] Recipe cards show image, name, time
- [ ] Tap navigates to detail

**Verify Database:**
```sql
SELECT recipe_id, added_at FROM favorites ORDER BY added_at DESC;
-- Expected: 3 rows
```

### Test 7.3: Remove from Favorites

**Steps:**
1. On Favorites screen, swipe left on a recipe
2. Tap delete/unfavorite
3. Verify removed from list

**Alternative:**
1. Go to Recipe Detail of favorited recipe
2. Tap filled heart to unfavorite
3. Return to Favorites
4. Verify removed

**Expected Results:**
- [ ] Swipe reveals delete action
- [ ] Confirmation if needed
- [ ] List updates immediately
- [ ] Can re-add the same recipe

---

## Phase 8: Stats Screen Testing

### Test 8.1: New User State

**Navigation:** Tap STATS in bottom nav

**Expected for New User:**
- [ ] Cooking streak: 0 days
- [ ] Calendar shows current month
- [ ] No days marked as "cooked"
- [ ] Cuisine breakdown: empty or meal plan projection
- [ ] Achievements section visible

### Test 8.2: Simulate Cooking

**Steps:**
1. Go to Home
2. Find "Mark as Cooked" action on a meal
3. Mark today's breakfast as cooked
4. Return to Stats

**Expected Results:**
- [ ] Streak increments to 1
- [ ] Today marked on calendar
- [ ] Cuisine breakdown updates

**Verify Database:**
```sql
SELECT * FROM cooking_streak;
SELECT * FROM cooking_days ORDER BY date DESC LIMIT 7;
```

### Test 8.3: Achievements

**UI Verification:**
- [ ] Achievement cards displayed
- [ ] Locked achievements grayed out
- [ ] Unlocked have color/animation
- [ ] Tap shows details

**New User Achievements:**
| Achievement | Status | Criteria |
|-------------|--------|----------|
| First Step | Unlocked | Completed onboarding |
| Meal Planner | Unlocked | Generated first plan |
| Streak Starter | Locked | 3-day streak |
| Cuisine Explorer | Locked | Cook 3 cuisines |

---

## Phase 9: Settings Screen Testing

### Test 9.1: Profile Display

**Navigation:** Gear icon from Home header

**UI Verification:**
- [ ] Google profile image
- [ ] User name
- [ ] Email address
- [ ] "Edit Profile" option

### Test 9.2: Preference Verification

**Check each section matches onboarding input:**

| Section | Expected Value |
|---------|---------------|
| Household Size | 3 |
| Family Members | Ramesh, Sunita, Aarav |
| Primary Diet | VEGETARIAN |
| Restrictions | SATTVIC |
| Cuisines | NORTH, SOUTH |
| Spice Level | MEDIUM |
| Allergies | Peanuts (Severe), Cashews (Mild) |
| Dislikes | Karela, Baingan, Mushroom |
| Weekday Time | 30 min |
| Weekend Time | 60 min |
| Busy Days | Mon, Wed, Fri |

### Test 9.3: Update Preferences

**Steps:**
1. Change spice level to SPICY
2. Save changes
3. Verify confirmation
4. Verify change persists after restart

**Expected Results:**
- [ ] Edit mode enables
- [ ] Changes save to database
- [ ] API syncs changes
- [ ] UI updates immediately

**Verify API:**
```
PUT /api/v1/users/preferences
Request: { "spiceLevel": "SPICY" }
Response: 200 OK
```

### Test 9.4: Notifications

**Steps:**
1. Toggle meal reminders ON
2. Toggle shopping reminder ON
3. Set shopping day to Saturday
4. Verify toggles persist

**Expected Results:**
- [ ] Toggles work
- [ ] Settings saved locally
- [ ] Notification permissions requested if needed

---

## Phase 10: Pantry Screen Testing

### Test 10.1: Empty State

**Navigation:** Settings → Pantry (or direct nav)

**UI State:**
- [ ] Empty state illustration
- [ ] "Your pantry is empty" message
- [ ] "Add Item" FAB

### Test 10.2: Add Pantry Items

**Steps:**
1. Tap "Add Item"
2. Add: Rice, GRAINS, 2 kg, no expiry
3. Add: Atta (Flour), GRAINS, 5 kg, expiry in 30 days
4. Add: Milk, DAIRY, 500 ml, expiry in 3 days
5. Add: Yogurt, DAIRY, 200 g, expiry yesterday (to test expired)

**Expected Results:**
- [ ] Items appear in list
- [ ] Grouped by category
- [ ] Expiry dates shown
- [ ] Expired items highlighted

**Verify Database:**
```sql
SELECT * FROM pantry_items ORDER BY category, name;
```

### Test 10.3: Expiry Sections

**UI Verification:**
- [ ] "Expiring Soon" section shows Milk (3 days)
- [ ] "Expired" section shows Yogurt
- [ ] Regular items in main list

### Test 10.4: Smart Suggestions

**Expected Behavior:**
- [ ] When grocery has item already in pantry, show "In Pantry" badge
- [ ] Suggest using expiring items in meal plan

---

## Phase 11: Recipe Rules Screen Testing

**Navigation:** Settings → Recipe Rules

**Screen Structure:** 4 tabs - Recipe, Ingredient, Meal-Slot, Nutrition

### Test 11.1: Chai Daily Rule (Recipe + Meal-Slot)

**Objective:** Ensure Chai appears daily at Breakfast AND Snacks

**Steps:**
1. Navigate to Recipe Rules screen
2. On "Recipe" tab, tap "+ ADD RECIPE RULE"
3. Create first rule:
   - Action: INCLUDE
   - Search & select: "Chai" (from 3,590 recipes)
   - Frequency: Daily
   - Meal Slot: Breakfast
   - Enforcement: REQUIRED
4. Save rule
5. Add second rule:
   - Action: INCLUDE
   - Target: "Chai"
   - Frequency: Daily
   - Meal Slot: Snacks
   - Enforcement: REQUIRED
6. Save rule

**Expected Results:**
- [ ] Two Chai rules appear in list
- [ ] First shows: "📖 Chai • Every day • Breakfast • Required"
- [ ] Second shows: "📖 Chai • Every day • Snacks • Required"
- [ ] Toggle switches are ON (active)

### Test 11.2: Moringa Weekly Rule (Ingredient Include)

**Objective:** Include Moringa (Drumstick/Sahjan) at least once per week

**Steps:**
1. Switch to "Ingredient" tab
2. Tap "+ ADD INGREDIENT RULE"
3. Create rule:
   - Action: INCLUDE
   - Search & select: "Moringa" or "Drumstick" or "Sahjan"
   - Frequency: 1 time per week (minimum)
   - Enforcement: PREFERRED
4. Save rule

**Expected Results:**
- [ ] Rule appears: "🥕 Moringa • 1x per week • Preferred"
- [ ] Recipes with moringa/drumstick will be prioritized

**Note:** Moringa-containing recipes in database include:
- Drumstick Sambar (South Indian)
- Sahjan ki Sabzi (North Indian)
- Moringa Dal

### Test 11.3: Exclude Rules Tab

**Steps:**
1. Switch to "Ingredient" tab (for excludes)
2. Tap "+ ADD INGREDIENT RULE"
3. Create rule:
   - Action: EXCLUDE
   - Target: "Paneer"
   - Frequency: NEVER
   - Enforcement: REQUIRED
4. Save rule

**Expected Results:**
- [ ] Rule shows: "🚫 Paneer • Never • Required"
- [ ] All paneer recipes excluded from meal plans

### Test 11.4: Nutrition Goals Tab

**Steps:**
1. Switch to "Nutrition" tab
2. Tap "+ ADD NUTRITION GOAL"
3. Add goal:
   - Category: GREEN_LEAFY (🥬)
   - Weekly Target: 5 servings
4. Save goal

**Expected Results:**
- [ ] Goal card appears with progress bar
- [ ] Shows "🥬 Green leafy vegetables"
- [ ] Shows "0/5 times" (new goal)
- [ ] Progress bar at 0%

### Test 11.5: Rules Applied to New Plan

**Setup - Active Rules:**
| # | Tab | Rule | Expected Impact |
|---|-----|------|-----------------|
| 1 | Recipe | Chai → Breakfast, Daily, REQUIRED | 7 breakfast slots have Chai |
| 2 | Recipe | Chai → Snacks, Daily, REQUIRED | 7 snack slots have Chai |
| 3 | Ingredient | Moringa → 1x/week, PREFERRED | At least 1 moringa recipe |
| 4 | Ingredient | Paneer → NEVER, REQUIRED | 0 paneer recipes |
| 5 | Nutrition | GREEN_LEAFY → 5x/week | 5+ green leafy recipes |

**Steps:**
1. Ensure all rules above are created and active
2. Go to Settings → "Regenerate Meal Plan"
3. Wait for generation
4. Navigate to Home screen
5. Check all 7 days

**Verification Checklist:**
- [ ] **Chai at Breakfast:** All 7 days show Chai at breakfast slot
- [ ] **Chai at Snacks:** All 7 days show Chai at snacks slot
- [ ] **Moringa:** At least 1 recipe contains moringa/drumstick (check recipe details)
- [ ] **No Paneer:** Zero recipes contain paneer (Palak Paneer, Shahi Paneer, etc. excluded)
- [ ] **Green Leafy:** 5+ recipes have spinach, methi, sarson, palak, etc.

**Database Verification:**
```sql
-- Verify Chai appears 14 times (7 breakfast + 7 snacks)
SELECT COUNT(*) FROM meal_plan_items
WHERE recipe_name ILIKE '%chai%';
-- Expected: 14

-- Verify Moringa appears at least 1x
SELECT COUNT(*) FROM meal_plan_items mpi
JOIN recipes r ON mpi.recipe_id = r.id
WHERE r.ingredients ILIKE '%moringa%'
   OR r.ingredients ILIKE '%drumstick%'
   OR r.ingredients ILIKE '%sahjan%';
-- Expected: >= 1

-- Verify NO paneer recipes
SELECT COUNT(*) FROM meal_plan_items mpi
JOIN recipes r ON mpi.recipe_id = r.id
WHERE r.ingredients ILIKE '%paneer%';
-- Expected: 0
```

### Test 11.6: Rule CRUD Operations

**Edit Rule:**
1. Tap on Moringa rule card
2. Change frequency from 1x/week → 2x/week
3. Save
4. Verify card updates to "2x per week"

**Toggle Rule:**
1. Tap toggle switch on Chai breakfast rule
2. Rule becomes inactive (grayed out)
3. Regenerate meal plan
4. Verify Chai no longer required at breakfast
5. Re-enable rule

**Delete Rule:**
1. Tap delete icon on Paneer exclude rule
2. Confirmation dialog: "Delete rule for Paneer?"
3. Tap DELETE
4. Rule removed from list

---

## Phase 12: Recipe Detail & Cooking Mode

### Test 12.1: Recipe Scaling

**Steps:**
1. Open any recipe (default 3 servings for household)
2. Change servings to 6
3. Verify ingredients double
4. Change to 1 serving
5. Verify ingredients scaled down

**Expected Results:**
- [ ] Quantities update mathematically
- [ ] UI reflects new amounts
- [ ] Fractional amounts handled (0.5, 1/4)

### Test 12.2: Start Cooking Mode

**Steps:**
1. Tap "Start Cooking"
2. Verify cooking mode UI:
   - Large step text
   - Step indicator (1 of N)
   - Timer button on timed steps
   - Previous/Next navigation
3. Navigate through all steps
4. Start a timer, verify countdown
5. Complete cooking

**Expected Results:**
- [ ] Large, readable text
- [ ] Swipe navigation works
- [ ] Timer counts down
- [ ] Timer notification if app backgrounded
- [ ] Completion prompts "Mark as Cooked"

### Test 12.3: Timer Functionality

**Steps:**
1. Find step with timer (e.g., "Cook for 10 minutes")
2. Tap timer button
3. Verify countdown starts
4. Background the app
5. Verify notification when timer ends

**Expected Results:**
- [ ] Timer accurate
- [ ] Notification fires
- [ ] Sound/vibration alert

---

## Phase 13: Offline Testing

### Test 13.1: Offline Meal Plan Access

**Steps:**
1. Enable airplane mode
2. Kill and restart app
3. Navigate to Home
4. View meal plan
5. Navigate through days
6. Open recipe detail

**Expected Results:**
- [ ] App launches without crash
- [ ] Cached meal plan displays
- [ ] All 7 days accessible
- [ ] Recipe details load from cache
- [ ] Offline indicator shown

### Test 13.2: Offline Modifications

**Steps:**
1. While offline:
   - Check a grocery item
   - Lock a meal
   - Add to favorites
2. Re-enable network
3. Verify sync occurs

**Expected Results:**
- [ ] Local changes saved to Room
- [ ] Changes queued for sync
- [ ] Auto-sync when online
- [ ] No data loss

**Verify Sync Queue:**
```sql
SELECT * FROM sync_queue WHERE synced = 0;
-- Should have pending actions when offline
```

### Test 13.3: Offline Error Handling

**Steps:**
1. While offline, try to:
   - Regenerate meal plan → Should show error
   - Send chat message → Should queue or error
   - Swap meal → Should work locally

**Expected Results:**
- [ ] Clear error messages
- [ ] No crashes
- [ ] Graceful degradation

---

## Phase 14: Edge Cases & Error Handling

### Test 14.1: Network Errors

**Test Scenarios:**
| Scenario | Expected Behavior |
|----------|-------------------|
| Timeout on meal plan generate | Retry button, cached fallback |
| 500 error on API | Error snackbar, retry option |
| 401 unauthorized | Redirect to login |
| Network change mid-operation | Resume or retry |

### Test 14.2: Data Validation

**Test Invalid Inputs:**
| Input | Location | Expected |
|-------|----------|----------|
| Age: 0 | Family member | Validation error |
| Age: 200 | Family member | Validation error |
| Empty name | Family member | Required field error |
| Household: 0 | Onboarding | Must be ≥1 |

### Test 14.3: Session Expiry

**Steps:**
1. Clear auth token: `adb shell run-as com.rasoiai.app.debug rm -rf shared_prefs/auth*`
2. Try to make API call
3. Verify redirect to login
4. Login again
5. Verify data preserved

### Test 14.4: App Kill and Restore

**Steps:**
1. Mid-onboarding (step 3), kill app
2. Relaunch
3. Verify state:
   - Returns to onboarding
   - Previous steps data may be lost (expected)
4. Post-meal-plan, kill app
5. Relaunch
6. Verify goes to Home with cached data

---

## Phase 15: Performance Testing

### Test 15.1: Cold Start

**Steps:**
1. `adb shell am force-stop com.rasoiai.app.debug`
2. `adb shell am start -W com.rasoiai.app.debug/.MainActivity`
3. Note TotalTime in output

**Expected:**
- Cold start < 3000ms
- Warm start < 1500ms

### Test 15.2: Frame Rate

**Steps:**
1. Enable GPU profiling: Settings → Developer → Profile GPU
2. Navigate between all screens rapidly
3. Check for jank (frame drops)

**Expected:**
- 60 FPS maintained
- No visible stuttering

### Test 15.3: Memory

**Steps:**
1. `adb shell dumpsys meminfo com.rasoiai.app.debug`
2. Navigate all screens, open 10 recipes
3. Check memory again
4. Force GC, check for leaks

**Expected:**
- Memory < 150MB typical usage
- No LeakCanary alerts
- Memory stable after GC

---

## Test Combinations Matrix

For thorough testing, verify each screen in these states:

| Screen | Loading | Empty | Populated | Error | Offline |
|--------|---------|-------|-----------|-------|---------|
| Home | ⬜ | N/A | ⬜ | ⬜ | ⬜ |
| Grocery | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Chat | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Favorites | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Stats | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Settings | ⬜ | N/A | ⬜ | ⬜ | ⬜ |
| Pantry | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Recipe Rules | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| Recipe Detail | ⬜ | N/A | ⬜ | ⬜ | ⬜ |
| Cooking Mode | ⬜ | N/A | ⬜ | ⬜ | ⬜ |

**Legend:** ⬜ = To Test, ✅ = Passed, ❌ = Failed

### Cross-Screen Data Flow Tests

| Test | Source | Target | Verification |
|------|--------|--------|--------------|
| Meal Plan → Grocery | Home meals | Grocery list | Ingredients match recipes |
| Favorite → Favorites | Recipe Detail | Favorites screen | Recipe appears in list |
| Swap → Grocery | Home swap | Grocery list | Items update correctly |
| Rule → Meal Plan | Recipe Rules | Home (after regen) | Rules applied |
| Cook → Stats | Cooking Mode | Stats screen | Streak increments |
| Pantry → Grocery | Pantry items | Grocery list | "In Pantry" badges |

---

## Test Results Template

```markdown
# E2E Test Execution Report

**Date:** YYYY-MM-DD
**Tester:** Claude Code
**App Version:** X.X.X (Build XXX)
**Emulator:** Pixel_6_API_34 (1080x2400)
**Backend:** localhost:8000 / Mock

## Summary

| Phase | Tests | Passed | Failed | Blocked |
|-------|-------|--------|--------|---------|
| 1. Auth | 2 | | | |
| 2. Onboarding | 5 | | | |
| 3. Generation | 1 | | | |
| 4. Home | 4 | | | |
| 5. Grocery | 4 | | | |
| 6. Chat | 4 | | | |
| 7. Favorites | 3 | | | |
| 8. Stats | 3 | | | |
| 9. Settings | 4 | | | |
| 10. Pantry | 4 | | | |
| 11. Rules | 4 | | | |
| 12. Cooking | 3 | | | |
| 13. Offline | 3 | | | |
| 14. Edge Cases | 4 | | | |
| 15. Performance | 3 | | | |
| **TOTAL** | 51 | | | |

## Test User Profile Verification

| Attribute | Expected | Actual | Match |
|-----------|----------|--------|-------|
| Household Size | 3 | | |
| Primary Diet | VEGETARIAN | | |
| Restrictions | SATTVIC | | |
| Cuisines | NORTH, SOUTH | | |
| Allergies | Peanuts, Cashews | | |
| Dislikes | Karela, Baingan, Mushroom | | |

## Database Verification

| Entity | Expected Count | Actual | Status |
|--------|----------------|--------|--------|
| recipes (Firestore) | 3,590 | | |
| meal_plans | 1 | | |
| meal_plan_items | 28 | | |
| grocery_items | 40+ | | |
| favorites | 3 | | |
| recipe_rules | 3 | | |
| pantry_items | 4 | | |

## API Verification

| Endpoint | Method | Expected | Status | Time |
|----------|--------|----------|--------|------|
| /auth/firebase | POST | 200 OK | | |
| /meal-plans/generate | POST | 201 Created | | |
| /meal-plans/current | GET | 200 OK | | |
| /recipes | GET | 3,590 recipes | | |
| /recipes/{id} | GET | Recipe detail | | |
| /grocery | GET | 40+ items | | |
| /chat/message | POST | AI response | | |
| /users/preferences | PUT | 200 OK | | |

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

## ADB Commands Reference

### App Management
```bash
# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Clear data
adb shell pm clear com.rasoiai.app.debug

# Force stop
adb shell am force-stop com.rasoiai.app.debug

# Launch
adb shell am start -n com.rasoiai.app.debug/com.rasoiai.app.MainActivity

# Uninstall
adb uninstall com.rasoiai.app.debug
```

### Screenshots & UI
```bash
# Screenshot
adb exec-out screencap -p > screenshot_$(date +%H%M%S).png

# UI hierarchy
adb shell uiautomator dump /data/local/tmp/ui.xml && adb pull /data/local/tmp/ui.xml

# Tap coordinates
adb shell input tap X Y

# Type text
adb shell input text "Hello"

# Press back
adb shell input keyevent KEYCODE_BACK
```

### Local Room Database
```bash
# Pull Room database from device
adb exec-out run-as com.rasoiai.app.debug cat databases/rasoiai_database > local.db

# Open with SQLite (for local cache inspection)
sqlite3 local.db ".tables"
sqlite3 local.db "SELECT * FROM meal_plans;"
```

### Firestore Database (Backend)
```bash
# Use Firebase Console or Firebase CLI to verify data
# https://console.firebase.google.com/project/rasoiai-6dcdd/firestore

# Or use verify script to check recipe counts
cd backend
python scripts/verify_recipe_import.py

# Expected output:
# Total recipes: 3,590
# North Indian: 3,124
# South Indian: 358
# West Indian: 85
# East Indian: 23
# Vegetarian: 3,482
# Vegan: 1,347

# Sample recipes for test verification:
# - Aloo Gobi (North, Vegetarian)
# - Masala Dosa (South, Vegan)
# - Dhokla (West, Vegetarian)
# - Dal Tadka (North, Vegan)
# - Paneer Butter Masala (North, Vegetarian)
```

### Network
```bash
# Enable airplane mode
adb shell settings put global airplane_mode_on 1
adb shell am broadcast -a android.intent.action.AIRPLANE_MODE

# Disable airplane mode
adb shell settings put global airplane_mode_on 0
adb shell am broadcast -a android.intent.action.AIRPLANE_MODE
```

### Logs
```bash
# RasoiAI logs only
adb logcat -s RasoiAI:V

# Clear logs
adb logcat -c

# Save to file
adb logcat -s RasoiAI:V > test_$(date +%Y%m%d_%H%M%S).log
```

---

## API Testing with cURL

```bash
BASE_URL="http://localhost:8000/api/v1"
TOKEN="<auth_token>"

# Auth
curl -X POST "$BASE_URL/auth/firebase" \
  -H "Content-Type: application/json" \
  -d '{"idToken": "test_firebase_token"}'

# Generate meal plan
curl -X POST "$BASE_URL/meal-plans/generate" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "householdSize": 3,
    "primaryDiet": "VEGETARIAN",
    "dietaryRestrictions": ["SATTVIC"],
    "cuisinePreferences": ["NORTH", "SOUTH"]
  }'

# Get current meal plan
curl -X GET "$BASE_URL/meal-plans/current" \
  -H "Authorization: Bearer $TOKEN"

# Get recipe
curl -X GET "$BASE_URL/recipes/r_abc123" \
  -H "Authorization: Bearer $TOKEN"

# Get grocery list
curl -X GET "$BASE_URL/grocery?mealPlanId=mp_abc123" \
  -H "Authorization: Bearer $TOKEN"

# Chat
curl -X POST "$BASE_URL/chat/message" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content": "What should I cook today?"}'
```

---

## Known Issues & Workarounds

### 1. Compose Dropdown Selection
Compose `ExposedDropdownMenu` items aren't captured by UI Automator. Use tap coordinates:
```bash
# Open dropdown at (540, 643)
adb shell input tap 540 643
sleep 0.3
# Select 3rd item at (100, 990)
adb shell input tap 100 990
```

### 2. Google OAuth in Emulator
Requires Google Play Services. Use:
- Emulator with Google APIs (not vanilla AOSP)
- Google account signed into emulator
- Web client ID configured in `local.properties`

### 3. API 36 Espresso Issues
Use API 34 for Espresso tests. API 36 has compatibility problems.

---

*Last Updated: January 28, 2026*
*Recipe Database: 3,590 recipes (imported from khanakyabanega)*
