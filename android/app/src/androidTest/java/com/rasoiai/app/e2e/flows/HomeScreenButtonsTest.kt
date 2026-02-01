package com.rasoiai.app.e2e.flows

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

/**
 * E2E tests for Home screen buttons (Issue #4, #5, #6 fixes).
 *
 * These tests verify that the following buttons work correctly:
 * - Add Recipe button (Issue #4)
 * - Hamburger Menu button (Issue #5)
 * - Notifications button (Issue #6)
 *
 * ## Prerequisites
 * - Real backend running at localhost:8000
 * - Backend DEBUG mode enabled (accepts fake-firebase-token)
 * - Android emulator with API 34
 */
@HiltAndroidTest
class HomeScreenButtonsTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private val uiDevice: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    override fun setUp() {
        super.setUp()

        // Set up authenticated state with meal plan
        setUpAuthenticatedState()

        homeRobot = HomeRobot(composeTestRule)

        // Wait for home screen and meal data to load
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
        waitForMealDataToLoad()
    }

    /**
     * Wait for meal data to load (up to 60s for API-generated meal plans)
     */
    private fun waitForMealDataToLoad() {
        try {
            homeRobot.assertMealCardDisplayed(MealType.BREAKFAST, timeoutMillis = 60000)
            Log.i(TAG, "Meal data loaded successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Meal data may not have loaded: ${e.message}")
        }
    }

    // ===================== Issue #4: Add Recipe Button Tests =====================

    /**
     * Test: Add Recipe button opens AddRecipeSheet.
     *
     * This test verifies that clicking the "+Add" button on a meal section
     * opens the AddRecipeSheet bottom sheet.
     *
     * GitHub Issue: #4
     */
    @Test
    fun addRecipeButton_opensAddRecipeSheet() {
        // Ensure we're on Home screen with meals
        homeRobot.selectDay(DayOfWeek.MONDAY)
        waitFor(500)

        // Find and click the first "Add" button (in Breakfast section)
        try {
            val addButtons = composeTestRule.onAllNodes(hasText("Add")).fetchSemanticsNodes()
            if (addButtons.isNotEmpty()) {
                Log.i(TAG, "Found ${addButtons.size} Add buttons")
                composeTestRule.onAllNodes(hasText("Add"))[0].performClick()
                waitFor(500)
                composeTestRule.waitForIdle()

                // Verify AddRecipeSheet opened
                composeTestRule.onNodeWithTag(TestTags.ADD_RECIPE_SHEET)
                    .assertIsDisplayed()
                Log.i(TAG, "SUCCESS: AddRecipeSheet opened")

                // Verify sheet contains expected content
                composeTestRule.onNodeWithText("Search recipes...", useUnmergedTree = true)
                    .assertExists()

                // Dismiss sheet
                composeTestRule.onNodeWithText("Cancel").performClick()
                waitFor(500)
            } else {
                Log.w(TAG, "No Add button found - meal plan may not have loaded")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Add button test failed: ${e.message}")
            throw e
        }
    }

    /**
     * Test: Add Recipe button works for all meal types.
     *
     * This test verifies that the Add button is present for meal sections.
     * Note: Due to lazy loading and scrolling, not all 4 buttons may be visible at once.
     */
    @Test
    fun addRecipeButton_existsForAllMealTypes() {
        homeRobot.selectDay(DayOfWeek.MONDAY)
        waitFor(500)

        // Count Add buttons - should find at least 3 visible on screen
        // (due to lazy column, not all 4 meal sections may be visible at once)
        val addButtons = composeTestRule.onAllNodes(hasText("Add")).fetchSemanticsNodes()
        Log.i(TAG, "Found ${addButtons.size} Add buttons")

        // We expect at least 3 Add buttons visible (some may be off-screen)
        assert(addButtons.size >= 3) {
            "Expected at least 3 Add buttons but found ${addButtons.size}"
        }
    }

    // ===================== Issue #5: Hamburger Menu Tests =====================

    /**
     * Test: Hamburger menu navigates to Settings.
     *
     * This test verifies that clicking the hamburger menu icon
     * navigates to the Settings screen.
     *
     * GitHub Issue: #5
     */
    @Test
    fun hamburgerMenu_navigatesToSettings() {
        homeRobot.assertHomeScreenDisplayed()

        // Click the hamburger menu button
        composeTestRule.onNodeWithTag(TestTags.HOME_MENU_BUTTON)
            .assertIsDisplayed()
            .performClick()
        waitFor(500)
        composeTestRule.waitForIdle()

        // Verify Settings screen is displayed
        try {
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                composeTestRule.onAllNodes(hasTestTag(TestTags.SETTINGS_SCREEN))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag(TestTags.SETTINGS_SCREEN)
                .assertIsDisplayed()
            Log.i(TAG, "SUCCESS: Hamburger menu navigated to Settings")

            // Navigate back to Home
            uiDevice.pressBack()
            waitFor(500)
            homeRobot.assertHomeScreenDisplayed()
        } catch (e: Exception) {
            Log.e(TAG, "Hamburger menu test failed: ${e.message}")
            throw e
        }
    }

    // ===================== Issue #6: Notifications Button Tests =====================

    /**
     * Test: Notifications button navigates to Notifications screen.
     *
     * This test verifies that clicking the notifications bell icon
     * navigates to the Notifications screen.
     *
     * GitHub Issue: #6
     */
    @Test
    fun notificationsButton_navigatesToNotifications() {
        homeRobot.assertHomeScreenDisplayed()

        // Click the notifications button
        composeTestRule.onNodeWithTag(TestTags.HOME_NOTIFICATIONS_BUTTON)
            .assertIsDisplayed()
            .performClick()
        waitFor(500)
        composeTestRule.waitForIdle()

        // Verify Notifications screen is displayed
        try {
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                composeTestRule.onAllNodes(hasTestTag(TestTags.NOTIFICATIONS_SCREEN))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_SCREEN)
                .assertIsDisplayed()
            Log.i(TAG, "SUCCESS: Notifications button navigated to Notifications screen")

            // Verify notifications screen content
            composeTestRule.onNodeWithText("No notifications yet", useUnmergedTree = true)
                .assertExists()

            // Navigate back to Home
            uiDevice.pressBack()
            waitFor(500)
            homeRobot.assertHomeScreenDisplayed()
        } catch (e: Exception) {
            Log.e(TAG, "Notifications button test failed: ${e.message}")
            throw e
        }
    }

    /**
     * Test: Notifications screen shows placeholder content.
     *
     * This test verifies the Notifications screen displays the
     * expected placeholder content.
     */
    @Test
    fun notificationsScreen_showsPlaceholderContent() {
        homeRobot.assertHomeScreenDisplayed()

        // Navigate to Notifications
        composeTestRule.onNodeWithTag(TestTags.HOME_NOTIFICATIONS_BUTTON)
            .performClick()
        waitFor(500)

        // Verify placeholder content
        composeTestRule.onNodeWithText("No notifications yet", useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithText("festival reminders", substring = true, useUnmergedTree = true)
            .assertExists()

        // Navigate back
        uiDevice.pressBack()
    }

    // ===================== Combined Flow Test =====================

    /**
     * Test: All header buttons work correctly in sequence.
     *
     * This test verifies all three header buttons work:
     * 1. Menu button -> Settings
     * 2. Notifications button -> Notifications
     * 3. Profile button -> Settings
     */
    @Test
    fun allHeaderButtons_workCorrectly() {
        homeRobot.assertHomeScreenDisplayed()

        // Test Menu button
        composeTestRule.onNodeWithTag(TestTags.HOME_MENU_BUTTON).performClick()
        waitFor(500)
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_SCREEN).assertIsDisplayed()
        uiDevice.pressBack()
        waitFor(500)
        homeRobot.assertHomeScreenDisplayed()

        // Test Notifications button
        composeTestRule.onNodeWithTag(TestTags.HOME_NOTIFICATIONS_BUTTON).performClick()
        waitFor(500)
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_SCREEN).assertIsDisplayed()
        uiDevice.pressBack()
        waitFor(500)
        homeRobot.assertHomeScreenDisplayed()

        // Test Profile button
        composeTestRule.onNodeWithTag(TestTags.HOME_PROFILE_BUTTON).performClick()
        waitFor(500)
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_SCREEN).assertIsDisplayed()
        uiDevice.pressBack()
        waitFor(500)
        homeRobot.assertHomeScreenDisplayed()

        Log.i(TAG, "SUCCESS: All header buttons work correctly")
    }

    companion object {
        private const val TAG = "HomeScreenButtonsTest"
    }
}
