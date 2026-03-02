package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.util.JourneyStepLogger
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

/**
 * J12: Offline and Error Resilience (single Activity session)
 *
 * Scenario: User starts with cached data, verifies Home loads from Room.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J12_OfflineResilienceJourney
 * ```
 */
@HiltAndroidTest
class J12_OfflineResilienceJourney : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private val logger = JourneyStepLogger("J12")

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedState()
        homeRobot = HomeRobot(composeTestRule)
    }

    @Test
    fun offlineResilienceJourney() {
        val totalSteps = 4

        try {
            logger.step(1, totalSteps, "Home loads with cached data") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
            }

            logger.step(2, totalSteps, "Meal data from Room") {
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
                homeRobot.assertAllMealCardsDisplayed()
            }

            logger.step(3, totalSteps, "Day navigation works") {
                homeRobot.assertWeekSelectorDisplayed()
                homeRobot.selectDay(DayOfWeek.TUESDAY)
                homeRobot.selectDay(DayOfWeek.FRIDAY)
            }

            logger.step(4, totalSteps, "Return to today") {
                homeRobot.navigateToHome()
                homeRobot.assertHomeScreenDisplayed()
            }
        } finally {
            logger.printSummary()
        }
    }
}
