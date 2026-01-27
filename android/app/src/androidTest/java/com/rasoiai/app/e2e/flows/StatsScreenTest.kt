package com.rasoiai.app.e2e.flows

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.StatsRobot
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * Phase 8: Stats Screen Testing
 *
 * Tests:
 * 8.1 Cooking Streak
 * 8.2 Cuisine Breakdown
 * 8.3 Achievements
 */
@HiltAndroidTest
class StatsScreenTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var statsRobot: StatsRobot

    @Before
    override fun setUp() {
        super.setUp()
        homeRobot = HomeRobot(composeTestRule)
        statsRobot = StatsRobot(composeTestRule)

        // Navigate to stats screen
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
        homeRobot.navigateToStats()
        statsRobot.waitForStatsScreen()
    }

    /**
     * Test 8.1: Cooking Streak
     *
     * Steps:
     * 1. Navigate to Stats screen
     * 2. Verify streak widget shows current streak
     * 3. Check monthly calendar view
     * 4. Verify days marked as "cooked"
     *
     * Expected:
     * - Streak count displayed (may be 0 for new user)
     * - Calendar shows current month
     * - Legend explains day markers
     */
    @Test
    fun test_8_1_cookingStreak() {
        // Verify stats screen is displayed
        statsRobot.assertStatsScreenDisplayed()

        // Verify streak widget
        statsRobot.assertStreakWidgetDisplayed()
        statsRobot.assertStreakDisplayed()

        // Verify calendar
        statsRobot.assertCalendarDisplayed()

        // Navigate between months
        statsRobot.goToPreviousMonth()
        statsRobot.goToNextMonth()
    }

    /**
     * Test 8.2: Cuisine Breakdown
     *
     * Steps:
     * 1. Scroll to cuisine breakdown section
     * 2. Verify pie chart or bar chart
     * 3. Check breakdown matches meal plan cuisines
     *
     * Expected:
     * - Visual chart of cuisine distribution
     * - NORTH and SOUTH should dominate (per preferences)
     */
    @Test
    fun test_8_2_cuisineBreakdown() {
        // Scroll to cuisine section
        statsRobot.scrollToCuisineBreakdown()

        // Verify cuisine breakdown is displayed
        statsRobot.assertCuisineBreakdownDisplayed()
        statsRobot.assertCuisineChartDisplayed()

        // Verify cuisine percentages (based on Sharma family preferences)
        statsRobot.assertCuisinePercentage("North")
        statsRobot.assertCuisinePercentage("South")
    }

    /**
     * Test 8.3: Achievements
     *
     * Steps:
     * 1. Scroll to achievements section
     * 2. Verify locked/unlocked achievements shown
     * 3. Check achievement descriptions
     *
     * Expected:
     * - Achievement cards with emoji, name, description
     * - Locked achievements grayed out
     */
    @Test
    fun test_8_3_achievements() {
        // Scroll to achievements
        statsRobot.scrollToAchievements()

        // Verify achievements section
        statsRobot.assertAchievementsSectionDisplayed()

        // Check for common achievements
        statsRobot.assertAchievementDisplayed("First Meal")
        statsRobot.assertAchievementDisplayed("Week Warrior")
    }

    /**
     * Test: Time period selection
     */
    @Test
    fun timePeriodSelection_works() {
        statsRobot.selectWeekView()
        statsRobot.selectMonthView()
        statsRobot.selectYearView()
    }

    /**
     * Test: Nutrition summary is displayed
     */
    @Test
    fun nutritionSummary_isDisplayed() {
        statsRobot.assertNutritionSummaryDisplayed()
        statsRobot.assertCalorieCountDisplayed()
    }

    /**
     * Test: Achievement details on tap
     */
    @Test
    fun achievementDetails_onTap() {
        statsRobot.scrollToAchievements()
        statsRobot.tapAchievement("First Meal")
        // Details sheet or dialog should appear
    }
}
