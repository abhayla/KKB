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
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Household Meal Plan Flow Tests - View shared meal plans, update item status
 * (cooked/skipped/ordered out), and view monthly stats.
 *
 * Tests ensure household exists via API before UI navigation.
 *
 * Navigation path: Home → Settings → "My Household" → Meal Plan section
 */
@HiltAndroidTest
class HouseholdMealPlanFlowTest : BaseE2ETest() {

    companion object {
        private const val TAG = "HouseholdMealPlanFlowTest"
        private const val TEST_HOUSEHOLD_NAME = "Sharma Family"
    }

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedStateWithoutMealPlan()

        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)

        // Ensure household exists
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            BackendTestHelper.ensureHouseholdExists(BACKEND_BASE_URL, authToken, TEST_HOUSEHOLD_NAME)
        }
    }

    // ===================== Navigation helper =====================

    private fun navigateToHouseholdMealPlan() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToSettings()
        settingsRobot.waitForSettingsScreen()
        settingsRobot.tapSettingItem("My Household")

        composeTestRule.onNodeWithText("Meal Plan", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasTestTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

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
    fun testViewSharedMealPlan() {
        navigateToHouseholdMealPlan()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN).assertIsDisplayed()

        // At least one meal item status row should be visible (status = PENDING/COOKED/etc.)
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_MEAL_ITEM_STATUS_PREFIX}0")
            .assertIsDisplayed()

        Log.i(TAG, "testViewSharedMealPlan: shared meal plan screen loaded with items")
    }

    @Test
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
    fun testMarkMealItemCooked() {
        navigateToHouseholdMealPlan()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_COOKED).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_COOKED).assertIsDisplayed()

        Log.i(TAG, "testMarkMealItemCooked: first meal item marked as cooked")
    }

    @Test
    fun testMarkMealItemSkipped() {
        navigateToHouseholdMealPlan()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_SKIPPED).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_SKIPPED).assertIsDisplayed()

        Log.i(TAG, "testMarkMealItemSkipped: first meal item marked as skipped")
    }

    @Test
    fun testMarkMealItemOrderedOut() {
        navigateToHouseholdMealPlan()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_ORDERED_OUT).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_ORDERED_OUT).assertIsDisplayed()

        Log.i(TAG, "testMarkMealItemOrderedOut: first meal item marked as ordered out")
    }

    @Test
    fun testNoEditAccessStatusButtonsDisabled() {
        navigateToHouseholdMealPlan()

        // This test assumes the current user is a member WITHOUT edit access
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_COOKED).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_SKIPPED).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEAL_ITEM_ORDERED_OUT)
            .assertIsNotEnabled()

        Log.i(TAG, "testNoEditAccessStatusButtonsDisabled: status buttons disabled without edit access")
    }

    @Test
    fun testViewMonthlyStats() {
        navigateToHouseholdStats()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_STATS_SCREEN).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_STATS_TOTAL_MEALS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_STATS_COOKED_COUNT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_STATS_SKIPPED_COUNT).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_STATS_MONTH_SELECTOR).assertIsDisplayed()

        Log.i(TAG, "testViewMonthlyStats: all household stats widgets visible")
    }

    @Test
    fun testMonthlyStatsEmpty() {
        navigateToHouseholdStats()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_STATS_SCREEN).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_STATS_MONTH_SELECTOR).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(">", substring = false).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("0", substring = true).assertIsDisplayed()

        Log.i(TAG, "testMonthlyStatsEmpty: empty month shows zero stats")
    }
}
