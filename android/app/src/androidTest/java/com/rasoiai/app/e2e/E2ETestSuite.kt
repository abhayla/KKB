package com.rasoiai.app.e2e

import com.rasoiai.app.e2e.flows.CookingModeFlowTest
import com.rasoiai.app.e2e.flows.CoreDataFlowTest
import com.rasoiai.app.e2e.flows.GroceryFlowTest
import com.rasoiai.app.e2e.flows.HomeScreenComprehensiveTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * E2E Test Suite - runs tests in specific order with persistent state.
 *
 * ## Architecture
 * This suite uses REAL UserPreferencesDataStore (not fake) which persists to disk.
 * Tests run in a single app instance and share persistent state:
 *
 * 1. CoreDataFlowTest runs first:
 *    - Clears DataStore for fresh start
 *    - Authenticates with backend (fake-firebase-token → real JWT)
 *    - Completes onboarding (persists preferences to real DataStore)
 *    - Generates meal plan (saved to Room DB)
 *    - Verifies Home screen
 *
 * 2. Subsequent tests inherit persisted state:
 *    - FakePhoneAuthClient.simulateSignedIn() sets Phone auth state
 *    - Real DataStore already has JWT + onboarded flag from step 1
 *    - App navigates directly to Home screen via SplashViewModel
 *
 * ## Run Command
 * ```bash
 * cd android
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.E2ETestSuite
 * ```
 *
 * ## Prerequisites
 * - Backend running on port 8000: `uvicorn app.main:app --reload --host 0.0.0.0`
 * - Android emulator running (API 34 recommended, NOT API 36)
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    CoreDataFlowTest::class,      // 1. Auth → Onboarding → Generate → Home (clears state first)
    HomeScreenComprehensiveTest::class, // 2. Verify Home (uses persisted state)
    GroceryFlowTest::class,        // 3. Verify Grocery (uses persisted state)
    CookingModeFlowTest::class     // 4. Recipe Detail + Cooking Mode (uses persisted state)
)
class E2ETestSuite
