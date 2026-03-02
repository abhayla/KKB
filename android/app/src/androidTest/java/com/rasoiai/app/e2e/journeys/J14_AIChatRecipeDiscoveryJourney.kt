package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.base.TestDataFactory
import com.rasoiai.app.e2e.robots.ChatRobot
import com.rasoiai.app.e2e.robots.FavoritesRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.util.JourneyStepLogger
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * J14: AI Chat & Recipe Discovery (single Activity session)
 *
 * Scenario: User opens Chat, asks recipe questions, then checks Favorites.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J14_AIChatRecipeDiscoveryJourney
 * ```
 */
@HiltAndroidTest
class J14_AIChatRecipeDiscoveryJourney : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var chatRobot: ChatRobot
    private lateinit var favoritesRobot: FavoritesRobot
    private val logger = JourneyStepLogger("J14")

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedState()
        homeRobot = HomeRobot(composeTestRule)
        chatRobot = ChatRobot(composeTestRule)
        favoritesRobot = FavoritesRobot(composeTestRule)
    }

    @Test
    fun aiChatRecipeDiscoveryJourney() {
        val totalSteps = 6

        try {
            logger.step(1, totalSteps, "Home screen") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
            }

            logger.step(2, totalSteps, "Navigate to Chat") {
                homeRobot.navigateToChat()
                chatRobot.waitForChatScreen()
                chatRobot.assertChatScreenDisplayed()
            }

            logger.step(3, totalSteps, "Send a message") {
                chatRobot.assertInputFieldDisplayed()
                chatRobot.sendMessage(TestDataFactory.ChatMessages.DINNER_SUGGESTION)
                chatRobot.assertUserMessageDisplayed(TestDataFactory.ChatMessages.DINNER_SUGGESTION)
            }

            logger.step(4, totalSteps, "Wait for AI response") {
                chatRobot.waitForAIResponse(LONG_TIMEOUT)
            }

            logger.step(5, totalSteps, "Navigate to Favorites") {
                homeRobot.navigateToFavorites()
                favoritesRobot.waitForFavoritesScreen()
                favoritesRobot.assertFavoritesScreenDisplayed()
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
