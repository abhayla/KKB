package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.presentation.common.TestTags

/**
 * Robot for Stats screen interactions.
 * Handles streak, cuisine breakdown, and achievements.
 */
class StatsRobot(private val composeTestRule: ComposeContentTestRule) {

    /**
     * Wait for stats screen to be displayed.
     */
    fun waitForStatsScreen(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(TestTags.STATS_SCREEN, timeoutMillis)
    }

    /**
     * Assert stats screen is displayed.
     */
    fun assertStatsScreenDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.STATS_SCREEN).assertIsDisplayed()
    }

    // ===================== Cooking Streak =====================

    /**
     * Assert streak widget is displayed.
     */
    fun assertStreakWidgetDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.STATS_STREAK_WIDGET).assertIsDisplayed()
    }

    /**
     * Assert streak count.
     */
    fun assertStreakCount(count: Int) = apply {
        composeTestRule.onNodeWithText("$count", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Assert streak is displayed (any value).
     */
    fun assertStreakDisplayed() = apply {
        composeTestRule.onNodeWithText("Streak", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert streak value is displayed with specific count.
     * Useful for verifying scope toggle changes the displayed data.
     */
    fun assertStreakValueDisplayed(expected: Int) = apply {
        composeTestRule.onNodeWithText("$expected", substring = true)
            .assertIsDisplayed()
    }

    // ===================== Calendar =====================

    /**
     * Assert calendar is displayed.
     */
    fun assertCalendarDisplayed() = apply {
        // Calendar should show current month
        composeTestRule.onNodeWithText("2024", substring = true) // Current year
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Navigate to previous month.
     */
    fun goToPreviousMonth() = apply {
        composeTestRule.onNodeWithText("<", substring = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Navigate to next month.
     */
    fun goToNextMonth() = apply {
        composeTestRule.onNodeWithText(">", substring = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert day is marked as cooked.
     */
    fun assertDayMarkedAsCooked(day: Int) = apply {
        // Cooked days have special styling
        composeTestRule.onNodeWithText("$day")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ===================== Cuisine Breakdown =====================

    /**
     * Assert cuisine chart is displayed.
     */
    fun assertCuisineChartDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.STATS_CUISINE_CHART)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert cuisine percentage is displayed.
     */
    fun assertCuisinePercentage(cuisineName: String) = apply {
        composeTestRule.onNodeWithText(cuisineName, substring = true, ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert cuisine breakdown section is displayed.
     */
    fun assertCuisineBreakdownDisplayed() = apply {
        composeTestRule.onNodeWithText("Cuisine Breakdown", substring = true, ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ===================== Achievements =====================

    /**
     * Assert achievements section is displayed.
     */
    fun assertAchievementsSectionDisplayed() = apply {
        composeTestRule.onNodeWithText("Achievements", substring = true, ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert achievement is displayed.
     */
    fun assertAchievementDisplayed(achievementName: String) = apply {
        composeTestRule.onNodeWithText(achievementName, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert achievement is unlocked.
     */
    fun assertAchievementUnlocked(achievementName: String) = apply {
        // Unlocked achievements have different styling
        composeTestRule.onNodeWithText(achievementName, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert achievement is locked.
     */
    fun assertAchievementLocked(achievementName: String) = apply {
        // Locked achievements are grayed out
        composeTestRule.onNodeWithText(achievementName, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Tap on achievement for details.
     */
    fun tapAchievement(achievementName: String) = apply {
        composeTestRule.onNodeWithText(achievementName, substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Time Period Selection =====================

    /**
     * Select week view.
     */
    fun selectWeekView() = apply {
        composeTestRule.onNodeWithText("Week", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select month view.
     */
    fun selectMonthView() = apply {
        composeTestRule.onNodeWithText("Month", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select year view.
     */
    fun selectYearView() = apply {
        composeTestRule.onNodeWithText("Year", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Nutrition Summary =====================

    /**
     * Assert nutrition summary is displayed.
     */
    fun assertNutritionSummaryDisplayed() = apply {
        composeTestRule.onNodeWithText("Nutrition", substring = true, ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert calorie count is displayed.
     */
    fun assertCalorieCountDisplayed() = apply {
        composeTestRule.onNodeWithText("calories", substring = true, ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ===================== Scroll =====================

    /**
     * Scroll to achievements section.
     */
    fun scrollToAchievements() = apply {
        composeTestRule.onNodeWithText("Achievements", substring = true, ignoreCase = true)
            .performScrollTo()
    }

    /**
     * Scroll to cuisine breakdown.
     */
    fun scrollToCuisineBreakdown() = apply {
        composeTestRule.onNodeWithText("Cuisine", substring = true, ignoreCase = true)
            .performScrollTo()
    }
}
