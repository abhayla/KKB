package com.rasoiai.app.e2e.flows

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.rasoiai.app.e2e.base.RealGoogleAuthE2ETest
import com.rasoiai.app.e2e.util.GoogleAuthTestHelper
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * E2E tests for Home screen with authentication.
 *
 * These tests verify that the Home screen buttons work correctly
 * with an authenticated user session.
 *
 * ## Prerequisites
 * 1. Backend must be running at localhost:8000
 * 2. Backend must have DEBUG=true to accept fake-firebase-token
 *
 * ## Note on Auth
 * Uses FakeGoogleAuthClient which returns fake-firebase-token.
 * Backend accepts this in debug mode for E2E testing.
 */
@HiltAndroidTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class HomeScreenRealAuthTest : RealGoogleAuthE2ETest() {

    companion object {
        private const val TAG = "HomeScreenRealAuthTest"
    }

    /**
     * Test 1: Authentication flow completes successfully
     *
     * This test:
     * 1. Clears any existing auth state
     * 2. Navigates through splash screen
     * 3. Clicks "Sign in with Google" button
     * 4. FakeGoogleAuthClient returns fake-firebase-token
     * 5. Backend accepts token, returns JWT
     * 6. Verifies we reach the onboarding or home screen
     */
    @Test
    fun test_01_signIn_completesSuccessfully() {
        Log.d(TAG, "Starting sign-in test")

        // Clear any existing state
        clearAuthState()
        waitFor(1000)

        // Wait for splash screen to pass
        Log.d(TAG, "Waiting for splash screen...")
        waitFor(SPLASH_DURATION + 1000)
        waitForIdle()

        // Take screenshot to see what screen we're on
        GoogleAuthTestHelper.takeDebugScreenshot("after_splash")

        // Look for sign-in button
        try {
            composeTestRule.onNodeWithTag(TestTags.AUTH_SIGN_IN_BUTTON)
                .assertIsDisplayed()
            Log.d(TAG, "Found sign-in button")
        } catch (e: AssertionError) {
            Log.w(TAG, "Sign-in button not found, might already be signed in")
            GoogleAuthTestHelper.takeDebugScreenshot("no_signin_button")
            return // Already signed in or different state
        }

        // Click sign-in button - FakeGoogleAuthClient handles the rest
        Log.d(TAG, "Clicking sign-in button...")
        composeTestRule.onNodeWithTag(TestTags.AUTH_SIGN_IN_BUTTON)
            .performClick()

        // Wait for auth to complete and navigation to happen
        waitFor(3000)
        waitForIdle()

        GoogleAuthTestHelper.takeDebugScreenshot("after_signin_complete")

        // Verify we're either on onboarding or home
        val isOnOnboarding = try {
            composeTestRule.onNodeWithTag(TestTags.ONBOARDING_SCREEN).assertExists()
            Log.d(TAG, "Navigated to onboarding after sign-in")
            true
        } catch (e: AssertionError) {
            false
        }

        val isOnHome = try {
            composeTestRule.onNodeWithTag(TestTags.HOME_SCREEN).assertExists()
            Log.d(TAG, "Navigated to home after sign-in")
            true
        } catch (e: AssertionError) {
            false
        }

        if (!isOnOnboarding && !isOnHome) {
            GoogleAuthTestHelper.takeDebugScreenshot("signin_failed")
            throw AssertionError("Sign-in did not navigate to expected screen")
        }

        Log.d(TAG, "Sign-in test completed successfully")
    }

    /**
     * Test 2: After sign-in, complete onboarding and reach Home screen
     */
    @Test
    fun test_02_completeOnboarding_reachHomeScreen() {
        Log.d(TAG, "Completing onboarding to reach Home screen")

        waitFor(2000)
        waitForIdle()
        GoogleAuthTestHelper.takeDebugScreenshot("onboarding_start")

        // Check if we're on onboarding or already on home
        val isOnOnboarding = try {
            composeTestRule.onNodeWithTag(TestTags.ONBOARDING_SCREEN).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        if (isOnOnboarding) {
            Log.d(TAG, "On onboarding screen, saving preferences to skip...")
            // Save preferences to skip onboarding
            saveOnboardingPreferences()
            waitFor(1000)
        }

        // Look for Home screen elements
        waitFor(3000)
        waitForIdle()
        GoogleAuthTestHelper.takeDebugScreenshot("home_screen_check")

        // Verify we can see Home screen content
        try {
            composeTestRule.waitUntil(timeoutMillis = 10000) {
                composeTestRule.onAllNodes(hasTestTag(TestTags.HOME_SCREEN))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            Log.d(TAG, "Home screen is displayed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find Home screen: ${e.message}")
            GoogleAuthTestHelper.takeDebugScreenshot("home_not_found")
            GoogleAuthTestHelper.dumpUiHierarchy()
            throw AssertionError("Home screen not displayed after onboarding")
        }
    }

    /**
     * Test 3: Verify Add Recipe button behavior
     *
     * Expected: Clicking "+ Add" should open AddRecipeSheet
     */
    @Test
    fun test_03_addRecipeButton_shouldOpenSheet() {
        Log.d(TAG, "Testing Add Recipe button")

        // Ensure we're on Home screen
        navigateToHomeIfNeeded()
        waitFor(1000)
        GoogleAuthTestHelper.takeDebugScreenshot("before_add_recipe")

        // Find and click the Add button on a meal slot
        try {
            // Look for any Add button in meal sections
            val addButtons = composeTestRule.onAllNodes(
                hasText("Add", substring = true)
            ).fetchSemanticsNodes()

            if (addButtons.isNotEmpty()) {
                Log.d(TAG, "Found ${addButtons.size} Add buttons, clicking first one...")
                composeTestRule.onAllNodes(hasText("Add", substring = true))[0]
                    .performClick()
                waitFor(500)
                waitForIdle()
                GoogleAuthTestHelper.takeDebugScreenshot("after_add_click")

                // Check if AddRecipeSheet opened
                composeTestRule.waitUntil(timeoutMillis = 3000) {
                    composeTestRule.onAllNodes(hasTestTag(TestTags.ADD_RECIPE_SHEET))
                        .fetchSemanticsNodes().isNotEmpty()
                }
                composeTestRule.onNodeWithTag(TestTags.ADD_RECIPE_SHEET)
                    .assertIsDisplayed()
                Log.d(TAG, "SUCCESS: AddRecipeSheet opened!")
            } else {
                Log.w(TAG, "No Add button found - might not have meal plan yet")
                GoogleAuthTestHelper.takeDebugScreenshot("no_add_button")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing Add button: ${e.message}")
            GoogleAuthTestHelper.takeDebugScreenshot("add_button_error")
            throw e
        }
    }

    /**
     * Test 4: Verify Hamburger Menu button navigates to Settings
     *
     * Expected: Clicking hamburger menu should navigate to Settings screen
     */
    @Test
    fun test_04_hamburgerMenu_shouldOpenSettings() {
        Log.d(TAG, "Testing Hamburger Menu button")

        navigateToHomeIfNeeded()
        waitFor(1000)
        GoogleAuthTestHelper.takeDebugScreenshot("before_hamburger")

        // Find and click the hamburger menu icon (usually top-left)
        try {
            composeTestRule.onNodeWithTag(TestTags.HOME_MENU_BUTTON)
                .assertIsDisplayed()
                .performClick()

            waitFor(500)
            waitForIdle()
            GoogleAuthTestHelper.takeDebugScreenshot("after_hamburger_click")

            // Check if we navigated to Settings screen
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                composeTestRule.onAllNodes(hasTestTag(TestTags.SETTINGS_SCREEN))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag(TestTags.SETTINGS_SCREEN)
                .assertIsDisplayed()
            Log.d(TAG, "SUCCESS: Navigated to Settings screen!")

            // Navigate back to Home for next tests
            uiDevice.pressBack()
            waitFor(500)
        } catch (e: Exception) {
            Log.e(TAG, "Hamburger menu test failed: ${e.message}")
            GoogleAuthTestHelper.takeDebugScreenshot("hamburger_failed")
            GoogleAuthTestHelper.dumpUiHierarchy()
            throw e
        }
    }

    /**
     * Test 5: Verify Notifications button behavior
     *
     * Expected: Clicking notifications icon should navigate to notifications screen
     */
    @Test
    fun test_05_notificationsButton_shouldNavigate() {
        Log.d(TAG, "Testing Notifications button")

        navigateToHomeIfNeeded()
        waitFor(1000)
        GoogleAuthTestHelper.takeDebugScreenshot("before_notifications")

        // Find and click the notifications icon (usually top-right)
        try {
            composeTestRule.onNodeWithTag(TestTags.HOME_NOTIFICATIONS_BUTTON)
                .assertIsDisplayed()
                .performClick()

            waitFor(500)
            waitForIdle()
            GoogleAuthTestHelper.takeDebugScreenshot("after_notifications_click")

            // Check if we navigated to notifications screen
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                composeTestRule.onAllNodes(hasTestTag(TestTags.NOTIFICATIONS_SCREEN))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_SCREEN)
                .assertIsDisplayed()
            Log.d(TAG, "SUCCESS: Navigated to notifications screen!")

            // Navigate back to Home for next tests
            uiDevice.pressBack()
            waitFor(500)
        } catch (e: Exception) {
            Log.e(TAG, "Notifications test failed: ${e.message}")
            GoogleAuthTestHelper.takeDebugScreenshot("notifications_failed")
            GoogleAuthTestHelper.dumpUiHierarchy()
            throw e
        }
    }

    /**
     * Test 6: Verify bottom navigation works correctly
     */
    @Test
    fun test_06_bottomNavigation_worksCorrectly() {
        Log.d(TAG, "Testing Bottom Navigation")

        navigateToHomeIfNeeded()
        waitFor(1000)

        // Test navigation to Grocery
        try {
            composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV_GROCERY)
                .assertIsDisplayed()
                .performClick()
            waitFor(500)
            waitForIdle()

            composeTestRule.onNodeWithTag(TestTags.GROCERY_SCREEN)
                .assertIsDisplayed()
            Log.d(TAG, "Navigation to Grocery works")
        } catch (e: Exception) {
            Log.e(TAG, "Bottom nav to Grocery failed: ${e.message}")
            throw e
        }

        // Navigate back to Home
        try {
            composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV_HOME)
                .performClick()
            waitFor(500)
            waitForIdle()

            composeTestRule.onNodeWithTag(TestTags.HOME_SCREEN)
                .assertIsDisplayed()
            Log.d(TAG, "Navigation back to Home works")
        } catch (e: Exception) {
            Log.e(TAG, "Bottom nav to Home failed: ${e.message}")
            throw e
        }
    }

    /**
     * Helper to ensure we're on the Home screen
     */
    private fun navigateToHomeIfNeeded() {
        // Check if already on Home
        val onHome = try {
            composeTestRule.onNodeWithTag(TestTags.HOME_SCREEN).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        if (!onHome) {
            Log.d(TAG, "Not on Home, attempting to navigate...")
            // Try clicking Home in bottom nav
            try {
                composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV_HOME)
                    .performClick()
                waitFor(500)
            } catch (e: Exception) {
                Log.w(TAG, "Could not find bottom nav Home button")
                // Might need to go through auth flow first
                try {
                    // Try signing in first
                    composeTestRule.onNodeWithTag(TestTags.AUTH_SIGN_IN_BUTTON)
                        .performClick()
                    waitFor(3000)
                    // Then skip onboarding
                    saveOnboardingPreferences()
                    waitFor(2000)
                } catch (authE: Exception) {
                    Log.w(TAG, "Auth attempt failed: ${authE.message}")
                }
            }
        }

        waitForIdle()
    }
}
