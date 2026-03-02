package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.GroceryRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.util.JourneyStepLogger
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * J13: Returning User Quick Check (single Activity session)
 *
 * Scenario: Returning user quickly checks today's meals and grocery list.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J13_ReturningUserQuickCheckJourney
 * ```
 */
@HiltAndroidTest
class J13_ReturningUserQuickCheckJourney : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var groceryRobot: GroceryRobot
    private val logger = JourneyStepLogger("J13")

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedState()
        homeRobot = HomeRobot(composeTestRule)
        groceryRobot = GroceryRobot(composeTestRule)
    }

    @Test
    fun returningUserQuickCheckJourney() {
        val totalSteps = 4

        try {
            logger.step(1, totalSteps, "Home loads fast") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
            }

            logger.step(2, totalSteps, "Review today's meals") {
                homeRobot.assertAllMealCardsDisplayed()
                homeRobot.assertMealCardDisplayed(MealType.BREAKFAST)
                homeRobot.assertMealCardDisplayed(MealType.DINNER)
            }

            logger.step(3, totalSteps, "Check grocery list") {
                homeRobot.navigateToGrocery()
                groceryRobot.waitForGroceryScreen()
                groceryRobot.assertGroceryScreenDisplayed()
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
