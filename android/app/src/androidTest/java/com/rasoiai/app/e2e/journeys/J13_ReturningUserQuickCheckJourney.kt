package com.rasoiai.app.e2e.journeys

import android.util.Log
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.GroceryRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.util.JourneyStepLogger
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
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

    companion object {
        private const val TAG = "J13_ReturningUser"
    }

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
        val totalSteps = 7

        try {
            val journeyStartTime = System.currentTimeMillis()

            logger.step(1, totalSteps, "Home loads fast") {
                val homeLoadStart = System.currentTimeMillis()
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
                val homeLoadTime = System.currentTimeMillis() - homeLoadStart
                Log.i(TAG, "Home screen load time: ${homeLoadTime}ms")
                assertTrue(
                    "Home screen should load within 5s (took ${homeLoadTime}ms)",
                    homeLoadTime < 5_000
                )
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

            logger.step(5, totalSteps, "Navigate to Notifications and back") {
                homeRobot.tapNotificationsButton()
                composeTestRule.waitForIdle()
                Thread.sleep(500) // Wait for navigation
                Log.i(TAG, "Navigated to Notifications screen")

                // Go back to Home
                composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
                composeTestRule.waitForIdle()
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                Log.i(TAG, "Returned to Home from Notifications")
            }

            logger.step(6, totalSteps, "Open refresh options sheet and dismiss") {
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
                homeRobot.tapRefreshButton()
                homeRobot.assertRefreshSheetDisplayed()
                Log.i(TAG, "Refresh options sheet displayed")

                homeRobot.dismissRefreshSheet()
                Log.i(TAG, "Refresh options sheet dismissed")
            }

            logger.step(7, totalSteps, "Verify bottom navigation tabs") {
                homeRobot.assertBottomNavDisplayed()
                homeRobot.assertHomeNavSelected()
                Log.i(TAG, "Bottom navigation verified — Home tab selected")
            }

            // Performance guardrail
            val totalDuration = System.currentTimeMillis() - journeyStartTime
            Log.i(TAG, "Total journey time: ${totalDuration}ms")
            assertTrue(
                "J13 journey should complete within 30s (took ${totalDuration}ms)",
                totalDuration < 30_000
            )
        } finally {
            logger.printSummary()
        }
    }
}
