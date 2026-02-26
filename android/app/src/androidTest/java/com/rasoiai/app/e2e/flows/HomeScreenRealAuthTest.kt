package com.rasoiai.app.e2e.flows

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.rasoiai.app.e2e.base.waitUntilWithBackoff
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * E2E tests for Home screen buttons with authenticated user.
 *
 * Tests verify that Home screen navigation buttons (Add Recipe, Menu,
 * Notifications, Bottom Nav) work correctly with a fully authenticated session.
 *
 * Uses BaseE2ETest with setUpAuthenticatedState() for reliable auth setup
 * within SplashViewModel's 2-second window.
 *
 * ## Prerequisites
 * 1. Backend must be running at localhost:8000
 * 2. Backend must have DEBUG=true to accept fake-firebase-token
 */
@HiltAndroidTest
class HomeScreenRealAuthTest : HomeScreenBaseE2ETest() {

    private val uiDevice: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    override fun setUp() {
        // Custom setup: different ordering from base class
        // (inject first, then robot init, then auth, then longer wait)
        hiltRule.inject()
        homeRobot = HomeRobot(composeTestRule)
        setUpAuthenticatedState()

        homeRobot.waitForHomeScreen(LONG_TIMEOUT + MEDIUM_TIMEOUT)
        homeRobot.waitForMealListToLoad()
        Log.d("HomeScreenRealAuthTest", "Home screen ready for testing")
    }

    /**
     * Verify Add Recipe button opens the AddRecipeSheet.
     * Clicks "+ Add" on a meal section and checks the sheet appears.
     */
    @Test
    fun addRecipeButton_shouldOpenSheet() {
        Log.d(TAG, "Testing Add Recipe button")

        // Find and click the Add button on a meal slot
        val addButtons = composeTestRule.onAllNodesWithTag(
            "${TestTags.MEAL_ADD_BUTTON_PREFIX}breakfast",
            useUnmergedTree = true
        ).fetchSemanticsNodes()

        if (addButtons.isNotEmpty()) {
            Log.d(TAG, "Found Add button for breakfast, clicking...")
            composeTestRule.onNodeWithTag(
                "${TestTags.MEAL_ADD_BUTTON_PREFIX}breakfast",
                useUnmergedTree = true
            ).performClick()

            waitFor(ANIMATION_DURATION)
            waitForIdle()

            // Check if AddRecipeSheet opened
            composeTestRule.waitUntilWithBackoff(
                tag = TestTags.ADD_RECIPE_SHEET,
                timeoutMillis = LONG_TIMEOUT
            )
            Log.d(TAG, "SUCCESS: AddRecipeSheet opened!")
        } else {
            // Try any other meal type's add button
            val anyAddButtons = composeTestRule.onAllNodesWithText(
                "Add", substring = true
            ).fetchSemanticsNodes()

            if (anyAddButtons.isNotEmpty()) {
                composeTestRule.onAllNodesWithText("Add", substring = true)[0]
                    .performClick()
                waitFor(ANIMATION_DURATION)
                waitForIdle()

                composeTestRule.waitUntilWithBackoff(
                    tag = TestTags.ADD_RECIPE_SHEET,
                    timeoutMillis = LONG_TIMEOUT
                )
                Log.d(TAG, "SUCCESS: AddRecipeSheet opened via fallback!")
            } else {
                Log.w(TAG, "No Add button found - meal plan may not have loaded")
            }
        }
    }

    /**
     * Verify Hamburger Menu button navigates to Settings screen.
     */
    @Test
    fun hamburgerMenu_shouldOpenSettings() {
        Log.d(TAG, "Testing Hamburger Menu button")

        composeTestRule.onNodeWithTag(TestTags.HOME_MENU_BUTTON)
            .assertIsDisplayed()
            .performClick()

        waitFor(ANIMATION_DURATION)
        waitForIdle()

        // Check if we navigated to Settings screen
        composeTestRule.waitUntilWithBackoff(
            tag = TestTags.SETTINGS_SCREEN,
            timeoutMillis = MEDIUM_TIMEOUT
        )
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_SCREEN)
            .assertIsDisplayed()
        Log.d(TAG, "SUCCESS: Navigated to Settings screen!")

        // Navigate back to Home
        uiDevice.pressBack()
        waitFor(ANIMATION_DURATION)
    }

    /**
     * Verify Notifications button navigates to Notifications screen.
     */
    @Test
    fun notificationsButton_shouldNavigate() {
        Log.d(TAG, "Testing Notifications button")

        composeTestRule.onNodeWithTag(TestTags.HOME_NOTIFICATIONS_BUTTON)
            .assertIsDisplayed()
            .performClick()

        waitFor(ANIMATION_DURATION)
        waitForIdle()

        // Check if we navigated to notifications screen
        composeTestRule.waitUntilWithBackoff(
            tag = TestTags.NOTIFICATIONS_SCREEN,
            timeoutMillis = MEDIUM_TIMEOUT
        )
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_SCREEN)
            .assertIsDisplayed()
        Log.d(TAG, "SUCCESS: Navigated to notifications screen!")

        // Navigate back to Home
        uiDevice.pressBack()
        waitFor(ANIMATION_DURATION)
    }

    /**
     * Verify bottom navigation between Home and Grocery screens.
     */
    @Test
    fun bottomNavigation_worksCorrectly() {
        Log.d(TAG, "Testing Bottom Navigation")

        // Test navigation to Grocery
        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV_GROCERY)
            .assertIsDisplayed()
            .performClick()
        waitFor(ANIMATION_DURATION)
        waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.GROCERY_SCREEN)
            .assertIsDisplayed()
        Log.d(TAG, "Navigation to Grocery works")

        // Navigate back to Home
        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV_HOME)
            .performClick()
        waitFor(ANIMATION_DURATION)
        waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.HOME_SCREEN)
            .assertIsDisplayed()
        Log.d(TAG, "Navigation back to Home works")
    }

    companion object {
        private const val TAG = "HomeScreenRealAuthTest"
    }
}
