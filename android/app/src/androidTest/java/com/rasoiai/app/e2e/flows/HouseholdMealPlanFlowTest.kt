package com.rasoiai.app.e2e.flows

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.SettingsRobot
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Household Meal Plan Flow Tests - View shared meal plans, update item status
 * (cooked/skipped/ordered out), and view monthly stats.
 *
 * All tests are @Ignore because they require:
 * - Running backend with household endpoints active
 * - The current user to be a member of a household that has a generated meal plan
 *
 * Navigation path: Home → Settings → "My Household" → Meal Plan section
 */
@HiltAndroidTest
class HouseholdMealPlanFlowTest : BaseE2ETest() {

    companion object {
        private const val TAG = "HouseholdMealPlanFlowTest"
    }

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedStateWithoutMealPlan()

        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)
    }

    // ===================== Navigation helper =====================

    /**
     * Navigate to the household meal plan screen.
     * The household meal plan lives under the household section in Settings,
     * or may be accessible directly from the Home screen under a "Household" tab.
     */
    private fun navigateToHouseholdMealPlan() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToSettings()
        settingsRobot.waitForSettingsScreen()
        settingsRobot.tapSettingItem("My Household")

        // Navigate further into the meal plan sub-section of the household screen
        composeTestRule.onNodeWithText("Meal Plan", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasTestTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Navigate to the household monthly stats screen.
     */
    private fun navigateToHouseholdStats() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToSettings()
        settingsRobot.waitForSettingsScreen()
        settingsRobot.tapSettingItem("My Household")

        composeTestRule.onNodeWithText("Stats", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasTestTag(TestTags.HOUSEHOLD_STATS_SCREEN)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ===================== Tests =====================

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testViewSharedMealPlan() {
        navigateToHouseholdMealPlan()

        // The household meal plan screen should be displayed
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN).assertIsDisplayed()

        // At least one meal item status row should be visible (status = PENDING/COOKED/etc.)
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_MEAL_ITEM_STATUS_PREFIX}0")
            .assertIsDisplayed()

        Log.i(TAG, "testViewSharedMealPlan: shared meal plan screen loaded with items")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testSharedMealPlanEmptyState() {
        navigateToHouseholdMealPlan()

        // When the household has no active meal plan, an empty state message is shown
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN).assertIsDisplayed()

        composeTestRule.onNodeWithText(
            "No meal plan",
            substring = true,
            ignoreCase = true
        ).assertIsDisplayed()

        Log.i(TAG, "testSharedMealPlanEmptyState: empty state shown when no household meal plan")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testMarkMealItemCooked() {
        navigateToHouseholdMealPlan()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN).assertIsDisplayed()

        // Tap the "Cooked" status button for the first meal item
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_COOKED).performClick()
        composeTestRule.waitForIdle()

        // The status should now reflect "Cooked" — typically a filled/active chip
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_COOKED).assertIsDisplayed()

        Log.i(TAG, "testMarkMealItemCooked: first meal item marked as cooked")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testMarkMealItemSkipped() {
        navigateToHouseholdMealPlan()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN).assertIsDisplayed()

        // Tap the "Skipped" status button for the first meal item
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_SKIPPED).performClick()
        composeTestRule.waitForIdle()

        // The status should now reflect "Skipped"
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_SKIPPED).assertIsDisplayed()

        Log.i(TAG, "testMarkMealItemSkipped: first meal item marked as skipped")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testMarkMealItemOrderedOut() {
        navigateToHouseholdMealPlan()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN).assertIsDisplayed()

        // Tap the "Ordered Out" status button for the first meal item
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_ORDERED_OUT).performClick()
        composeTestRule.waitForIdle()

        // The status should now reflect "Ordered Out"
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_ORDERED_OUT).assertIsDisplayed()

        Log.i(TAG, "testMarkMealItemOrderedOut: first meal item marked as ordered out")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testNoEditAccessStatusButtonsDisabled() {
        navigateToHouseholdMealPlan()

        // This test assumes the current user is a member WITHOUT edit access
        // Status buttons (Cooked, Skipped, Ordered Out) should be disabled/not clickable
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_COOKED).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_SKIPPED).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_ORDERED_OUT)
            .assertIsNotEnabled()

        Log.i(TAG, "testNoEditAccessStatusButtonsDisabled: status buttons disabled without edit access")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testViewMonthlyStats() {
        navigateToHouseholdStats()

        // The household stats screen should be displayed
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_STATS_SCREEN).assertIsDisplayed()

        // Total meals, cooked count, and skipped count widgets should be visible
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_STATS_TOTAL_MEALS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_STATS_COOKED_COUNT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_STATS_SKIPPED_COUNT).assertIsDisplayed()

        // Month selector should also be present for navigating between months
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_STATS_MONTH_SELECTOR).assertIsDisplayed()

        Log.i(TAG, "testViewMonthlyStats: all household stats widgets visible")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testMonthlyStatsEmpty() {
        navigateToHouseholdStats()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_STATS_SCREEN).assertIsDisplayed()

        // Navigate to a future month that has no data using the month selector
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_STATS_MONTH_SELECTOR).performClick()
        composeTestRule.waitForIdle()

        // Select the next month (e.g. tap the ">" arrow)
        composeTestRule.onNodeWithText(">", substring = false).performClick()
        composeTestRule.waitForIdle()

        // The stats for an empty month should show zeros or a "no data" message
        composeTestRule.onNodeWithText("0", substring = true).assertIsDisplayed()

        Log.i(TAG, "testMonthlyStatsEmpty: empty month shows zero stats")
    }
}
