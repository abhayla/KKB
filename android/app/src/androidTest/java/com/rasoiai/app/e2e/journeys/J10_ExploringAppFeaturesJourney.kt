package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.ChatRobot
import com.rasoiai.app.e2e.robots.FavoritesRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.StatsRobot
import com.rasoiai.app.e2e.util.JourneyStepLogger
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * J10: Exploring App Features (single Activity session)
 *
 * Scenario: User browses Home, Favorites, Chat, and Stats via bottom nav.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J10_ExploringAppFeaturesJourney
 * ```
 */
@HiltAndroidTest
class J10_ExploringAppFeaturesJourney : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var favoritesRobot: FavoritesRobot
    private lateinit var chatRobot: ChatRobot
    private lateinit var statsRobot: StatsRobot
    private val logger = JourneyStepLogger("J10")

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedState()
        homeRobot = HomeRobot(composeTestRule)
        favoritesRobot = FavoritesRobot(composeTestRule)
        chatRobot = ChatRobot(composeTestRule)
        statsRobot = StatsRobot(composeTestRule)
    }

    @Test
    fun exploringAppFeaturesJourney() {
        val totalSteps = 6

        try {
            logger.step(1, totalSteps, "Home screen") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
            }

            logger.step(2, totalSteps, "Navigate to Favorites") {
                homeRobot.navigateToFavorites()
                favoritesRobot.waitForFavoritesScreen()
                favoritesRobot.assertFavoritesScreenDisplayed()
            }

            logger.step(3, totalSteps, "Navigate to Chat") {
                homeRobot.navigateToChat()
                chatRobot.waitForChatScreen()
                chatRobot.assertChatScreenDisplayed()
                chatRobot.assertInputFieldDisplayed()
            }

            logger.step(4, totalSteps, "Navigate to Stats") {
                homeRobot.navigateToStats()
                statsRobot.waitForStatsScreen()
                statsRobot.assertStatsScreenDisplayed()
            }

            logger.step(5, totalSteps, "Browse stats widgets") {
                statsRobot.assertStreakWidgetDisplayed()
                statsRobot.assertCalendarDisplayed()
            }

            logger.step(6, totalSteps, "Return to Home") {
                homeRobot.navigateToHome()
                homeRobot.assertHomeScreenDisplayed()
            }
        } finally {
            logger.printSummary()
        }
    }
}
