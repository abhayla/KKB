package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.GroceryRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.util.JourneyStepLogger
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * J05: Weekly Grocery Shopping (single Activity session)
 *
 * Scenario: User views meal plan, navigates to grocery list, checks items off.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J05_WeeklyGroceryShoppingJourney
 * ```
 */
@HiltAndroidTest
class J05_WeeklyGroceryShoppingJourney : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var groceryRobot: GroceryRobot
    private val logger = JourneyStepLogger("J05")

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedState()
        homeRobot = HomeRobot(composeTestRule)
        groceryRobot = GroceryRobot(composeTestRule)
    }

    @Test
    fun weeklyGroceryShoppingJourney() {
        val totalSteps = 4

        try {
            logger.step(1, totalSteps, "Home screen loads") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
            }

            logger.step(2, totalSteps, "Navigate to Grocery") {
                homeRobot.navigateToGrocery()
                groceryRobot.waitForGroceryScreen()
                groceryRobot.assertGroceryScreenDisplayed()
            }

            logger.step(3, totalSteps, "Browse grocery categories") {
                groceryRobot.assertCommonCategoriesDisplayed()
            }

            logger.step(4, totalSteps, "Return to Home") {
                homeRobot.navigateToHome()
                homeRobot.assertHomeScreenDisplayed()
            }
        } finally {
            logger.printSummary()
        }
    }
}
