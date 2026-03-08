# RasoiAI End-to-End Testing Guide

This document provides test scenarios and verification criteria for the RasoiAI Android app.

**Detailed phase scenarios:** See [E2E-Phase-Details.md](./E2E-Phase-Details.md) for step-by-step test checklists (Phases 1-15), the Sharma Family test profile, and the Test Results Template.

---

## How to Run Tests

**All testing uses Compose UI Testing (instrumented tests):**

```bash
cd android

# Run all instrumented tests (~400 tests)
./gradlew connectedDebugAndroidTest

# Run specific test class
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.presentation.auth.AuthScreenTest

# Run specific package
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.rasoiai.app.presentation.home

# Run full E2E journey test
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.FullUserJourneyTest

# Run all E2E flow tests (~100 tests)
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.rasoiai.app.e2e.flows

# View results
start app/build/reports/androidTests/connected/index.html
```

**Prerequisites:**
1. Emulator running (API 34 - not API 36 due to Espresso issues)
2. Backend running: `cd backend && uvicorn app.main:app --reload --port 8000`
3. App installed: `./gradlew installDebug`

**Auth Bypass:** Tests use `FakePhoneAuthClient` which returns `fake-firebase-token`. The backend accepts this token in debug mode (`DEBUG=true`).

---

## Functional Requirements Workflow

When implementing or testing features that affect user-facing functionality, follow this workflow to ensure requirements are properly tracked and tested.

### 1. Before Implementation

```bash
# List functional requirements issues
gh issue list --label "functional-requirement"

# Create new requirement if needed
# Go to GitHub Issues → New Issue → "Functional Requirement" template
# Assign next FR-XXX number (check docs/testing/Functional-Requirement-Rule.md)
```

### 2. After Implementation

**Create E2E test with requirement reference:**
```kotlin
/**
 * Requirement: #XX - FR-XXX: Brief description
 * @see docs/testing/Functional-Requirement-Rule.md
 */
@HiltAndroidTest
class YourFeatureTest : BaseE2ETest() {
    // Test implementation
}
```

**Create backend test (if API involved):**
```python
"""
Requirement: #XX - FR-XXX: Brief description
Tests the API endpoint for this feature.
"""
@pytest.mark.asyncio
async def test_your_feature():
    # Test implementation
```

### 3. Update Traceability

Add entry to `docs/testing/Functional-Requirement-Rule.md`:

| ID | Requirement | GitHub Issue | Android E2E Test | Backend Test | Status |
|----|-------------|--------------|------------------|--------------|--------|
| FR-XXX | Your requirement | #XX | `YourFeatureTest.kt` | `test_your_feature.py` | ✅ |

### 4. Verify Tests Pass

```bash
# Android E2E test
cd android
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.YourFeatureTest

# Backend test
cd backend
PYTHONPATH=. pytest tests/test_your_feature.py -v
```

---

## E2E Test Execution Flow

The tests follow a **16-phase sequence** that mimics a complete user journey:

```
Phase 1: AUTH ──► Phase 2: ONBOARDING (5 steps) ──► Phase 3: GENERATION
   │                                                        │
   └─ FakePhoneAuthClient                                 ▼
      "fake-firebase-token"                     Phase 4: HOME ──► Phase 5: GROCERY
                                                   │                    │
                                                   ▼                    ▼
                                        Phase 7: FAVORITES    Phase 6: CHAT
                                                   │
                                                   ▼
                         Phase 8: STATS ──► Phase 9: SETTINGS ──► Phase 10: PANTRY
                                                                       │
                                                                       ▼
                              Phase 11: RECIPE RULES ──► Phase 12: COOKING MODE
                                                                       │
                                                                       ▼
                    Phase 13: OFFLINE ──► Phase 14: EDGE CASES ──► Phase 15: PERFORMANCE
                                                                       │
                                                                       ▼
                                                              Phase 16: HOUSEHOLD
```

### Phase Summary

| Phase | Name | Tests | Key Validations |
|:-----:|------|:-----:|-----------------|
| 1 | Auth | 18 | FakePhoneAuthClient → Backend accepts fake token → JWT returned |
| 2 | Onboarding | 41 | 5 steps with Sharma Family profile data |
| 3 | Generation | 17 | 4-step progress, API calls, 28 meal items created |
| 4 | Home | 22 | Meal cards, lock/swap, day navigation |
| 5 | Grocery | 21 | Items derived from recipes, no allergens |
| 6 | Chat | 17 | AI context awareness, recipe suggestions |
| 7 | Favorites | 17 | Add/remove, persistence in Room DB |
| 8 | Stats | 21 | Streak, calendar, achievements |
| 9 | Settings | 15 | Preferences match onboarding input |
| 10 | Pantry | 18 | Expiry tracking, smart suggestions |
| 11 | Recipe Rules | 22 | Include/Exclude rules, nutrition goals |
| 12 | Cooking Mode | 27 | Step-by-step, timers, scaling |
| 13 | Offline | 7 | Cached data, local mutations, sync |
| 14 | Edge Cases | 11 | Error handling, validation, session |
| 15 | Performance | 6 | Cold start, FPS, memory |
| 16 | Household | ~45 | Household CRUD, member management, invite codes, scope toggle, shared meal plan, notifications |

**Total: ~445 tests**

**For detailed test steps, expected results, and verification SQL for each phase, see [E2E-Phase-Details.md](./E2E-Phase-Details.md).**

---

## Testing Framework

| Layer | Framework | Purpose |
|-------|-----------|---------|
| Unit Tests | JUnit5 + MockK | ViewModel, Repository, UseCase |
| UI Screen Tests | Compose UI Testing | Screen composables with mock UiState |
| Integration Tests | Hilt + FakePhoneAuthClient | Full navigation flows with phone auth bypass |
| Flow Testing | Turbine | StateFlow/Channel in ViewModels |

**Note:** All tests run via `./gradlew connectedDebugAndroidTest`. ADB commands are for debugging/setup only.

### Current UI Test Coverage (~400 tests across 15 screens)

| Screen | Test File | Tests |
|--------|-----------|-------|
| Auth | `AuthScreenTest.kt` | 18 |
| Auth (Integration) | `AuthIntegrationTest.kt` | 9 |
| Onboarding | `OnboardingScreenTest.kt` | 41 |
| **Generation** | `GenerationScreenTest.kt` | **17** |
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

**Additional Test Categories:**

| Category | Test File | Tests |
|----------|-----------|-------|
| Database Verification | `DatabaseVerificationTest.kt` | 18 |
| Recipe Constraints | `RecipeConstraintTest.kt` | 22 |
| Full Journey (Deep) | `FullJourneyFlowTest.kt` | 1 |
| E2E Flow Tests | `*FlowTest.kt` (23 files) | ~125 |
| Performance Tests | `PerformanceTest.kt` | 6 |
| Edge Cases | `EdgeCasesTest.kt` | 11 |
| Offline Tests | `OfflineFlowTest.kt` | 7 |

**Test Locations:**
- UI Screen Tests: `android/app/src/androidTest/java/com/rasoiai/app/presentation/`
- E2E Flow Tests: `android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/`
- Database Tests: `android/app/src/androidTest/java/com/rasoiai/app/e2e/database/`
- Validation Tests: `android/app/src/androidTest/java/com/rasoiai/app/e2e/validation/`

---

## Test Architecture: Real Backend + Fake Auth

The E2E tests use **Real Backend + Fake Phone Auth Only**:
- Firebase Phone Auth is bypassed using `FakePhoneAuthClient`
- All API calls go to the real Python backend
- Database is PostgreSQL (real, not emulated)

### What's Fake vs Real

| Component | Status | Details |
|-----------|--------|---------|
| Phone Auth | **FAKE** | `FakePhoneAuthClient` bypasses real Firebase Phone Auth |
| Firebase Token | **FAKE** | Hardcoded `"fake-firebase-token"` |
| Backend API | **REAL** | Python FastAPI at `localhost:8000` |
| JWT Tokens | **REAL** | Backend generates real JWTs |
| Database | **REAL** | PostgreSQL |
| All Repositories | **REAL** | Real implementations calling real APIs |

### Auth Flow

1. User enters phone number and taps "Send OTP"
2. `FakePhoneAuthClient` auto-verifies and returns `{ userId: "fake-user-id", phoneNumber: "+911111111111", firebaseIdToken: "fake-firebase-token" }`
3. `AuthViewModel` calls `POST http://10.0.2.2:8000/api/v1/auth/firebase` with `{ "firebase_token": "fake-firebase-token" }`
4. Backend (`DEBUG=true`) accepts fake token, creates/finds user in PostgreSQL, returns real JWT
5. App stores JWT in DataStore, navigates to Onboarding (new user) or Home (returning user)

### Key Test Files

| File | Purpose |
|------|---------|
| `e2e/di/FakePhoneAuthClient.kt` | Bypasses Firebase Phone Auth, returns fake credentials |
| `e2e/di/FakeAuthModule.kt` | Hilt module that replaces real PhoneAuthClient |
| `e2e/base/BaseE2ETest.kt` | Base test class with Hilt setup |
| `presentation/common/TestTags.kt` | All semantic test tags for UI elements |
| `presentation/*ScreenTest.kt` | UI tests for each screen (13 files) |

---

## Pre-Test Setup

### Emulator Configuration

```bash
# Required: API 34 emulator (API 36 has Espresso issues)
$ANDROID_HOME/emulator/emulator -avd Pixel_6_API_34 -no-snapshot-load
adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'
```

### App Installation

```bash
cd android
./gradlew clean assembleDebug && ./gradlew installDebug
adb shell pm clear com.rasoiai.app.debug  # Clear data for fresh start
```

### Backend Setup

**Recipe Database:** 3,580 recipes (3,124 North, 358 South, 85 West, 23 East; 3,482 Vegetarian, 1,347 Vegan).

```bash
cd backend
source venv/bin/activate
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
curl http://localhost:8000/health  # Verify API running
```

### Logging

```bash
adb logcat -s RasoiAI:V                           # RasoiAI logs only
adb logcat -s RasoiAI:V > test_$(date +%Y%m%d).log  # Save to file
```

---

## Test User Profile: Sharma Family (Summary)

| Attribute | Value |
|-----------|-------|
| Household | 3 members: Ramesh (45, DIABETIC/LOW_OIL), Sunita (42, LOW_SALT), Aarav (12, NO_SPICY) |
| Primary Diet | VEGETARIAN + SATTVIC (no onion/garlic) |
| Cuisines | NORTH, SOUTH; Spice: MEDIUM |
| Allergies | Peanuts (SEVERE), Cashews (MILD) |
| Dislikes | Karela, Baingan, Mushroom |
| Cooking Time | Weekday: 30 min, Weekend: 60 min |
| Busy Days | Monday, Wednesday, Friday |

**Full profile with expected constraints and recipe rules:** See [E2E-Phase-Details.md](./E2E-Phase-Details.md#test-data-profile-sharma-family).

---

## ADB Quick Reference

```bash
# App management
adb shell pm clear com.rasoiai.app.debug          # Clear data
adb shell am force-stop com.rasoiai.app.debug     # Force stop
adb shell am start -n com.rasoiai.app.debug/com.rasoiai.app.MainActivity  # Launch

# Screenshots (MUST save to docs/testing/screenshots/)
adb exec-out screencap -p > docs/testing/screenshots/screen_name.png

# UI hierarchy
adb shell uiautomator dump /data/local/tmp/ui.xml && adb pull /data/local/tmp/ui.xml

# Room database inspection
adb exec-out run-as com.rasoiai.app.debug cat databases/rasoiai_database > local.db

# Network toggle (offline testing)
adb shell settings put global airplane_mode_on 1 && adb shell am broadcast -a android.intent.action.AIRPLANE_MODE
adb shell settings put global airplane_mode_on 0 && adb shell am broadcast -a android.intent.action.AIRPLANE_MODE
```

---

## Customer Journey Test Suites

17 journey-based suites group the 28 E2E test files into realistic user scenarios. Each journey is a runnable JUnit `@Suite` class in `e2e/journeys/`.

**Full documentation:** [Customer-Journey-Test-Suites.md](./Customer-Journey-Test-Suites.md)

```bash
# Run a specific journey
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J01_FirstTimeUserSuite

# Run all journeys
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.rasoiai.app.e2e.journeys
```

| Suite | Journey | Tests |
|-------|---------|:-----:|
| J01 | First-Time User Gets Started | 3 |
| J02 | New User First Meal Plan | 2 |
| J03 | Complete End-to-End Journey | 1 |
| J04 | Daily Meal Planning | 2 |
| J05 | Weekly Grocery Shopping | 2 |
| J06 | Cooking a Meal | 3 |
| J07 | Managing Dietary Preferences | 3 |
| J08 | AI Meal Plan Quality Assurance | 2 |
| J09 | Family Profile Management | 2 |
| J10 | Exploring App Features | 4 |
| J11 | Customizing App Settings | 3 |
| J12 | Offline and Error Resilience | 2 |
| J13 | Returning User Quick Check | 3 |
| J14 | AI Chat and Recipe Discovery | 3 |
| J15 | Household Setup & Member Management | 2 |
| J16 | Household Meal Collaboration | 2 |
| J17 | Household Notifications & Awareness | 2 |

---

## Known Issues

1. **Compose Dropdown Selection:** `ExposedDropdownMenu` items aren't captured by UI Automator. Use tap coordinates for manual testing.
2. **Firebase Phone Auth in Emulator:** Not needed for instrumented tests — `FakePhoneAuthClient` bypasses real phone auth entirely.
3. **API 36 Espresso Issues:** Use API 34 for tests. API 36 has compatibility problems.

---

*Last Updated: March 8, 2026*
*Recipe Database: 3,580 recipes (imported from khanakyabanega)*
