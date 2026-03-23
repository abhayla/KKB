package com.rasoiai.app.e2e.journeys

import android.util.Log
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.base.TestDataFactory
import com.rasoiai.app.e2e.robots.ChatRobot
import com.rasoiai.app.e2e.robots.FavoritesRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.app.e2e.util.JourneyStepLogger
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * J14: AI Chat & Recipe Discovery — Multi-Turn Conversation
 *
 * Scenario: User opens Chat, has a 3-message conversation with the AI
 * (dinner → paneer follow-up → breakfast topic change), verifies context
 * continuity, checks Favorites mid-conversation, and validates that all
 * 6 messages (3 user + 3 AI) persist in both Room DB and backend history.
 *
 * Steps:
 * 1-4: Home → Chat → Send dinner question → Wait for AI response
 * 5:   Navigate to Favorites and back to Chat (context preserved)
 * 6:   Send paneer follow-up → Wait for AI response (within 30s)
 * 7:   Send breakfast topic change → Wait for AI response (within 30s)
 * 8:   Verify Room DB has >= 6 messages (3 user + 3 AI)
 * 9:   Verify backend /chat/history has >= 6 messages
 * 10:  Return to Home
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
    private val tag = "J14_AIChatRecipeDiscovery"

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
        val totalSteps = 10
        val AI_RESPONSE_TIMEOUT = 30_000L

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

            logger.step(3, totalSteps, "Send first message — dinner suggestion") {
                chatRobot.assertInputFieldDisplayed()
                chatRobot.sendMessage(TestDataFactory.ChatMessages.DINNER_SUGGESTION)
                chatRobot.assertUserMessageDisplayed(TestDataFactory.ChatMessages.DINNER_SUGGESTION)
            }

            logger.step(4, totalSteps, "Wait for first AI response") {
                val startTime = System.currentTimeMillis()
                chatRobot.waitForAIResponse(AI_RESPONSE_TIMEOUT)
                val elapsed = System.currentTimeMillis() - startTime
                Log.i(tag, "First AI response arrived in ${elapsed}ms")
                assertTrue(
                    "First AI response took ${elapsed}ms — exceeds ${AI_RESPONSE_TIMEOUT}ms limit",
                    elapsed < AI_RESPONSE_TIMEOUT
                )
            }

            logger.step(5, totalSteps, "Navigate to Favorites and back") {
                homeRobot.navigateToFavorites()
                favoritesRobot.waitForFavoritesScreen()
                favoritesRobot.assertFavoritesScreenDisplayed()
                homeRobot.navigateToChat()
                chatRobot.waitForChatScreen()
                chatRobot.assertChatScreenDisplayed()
            }

            logger.step(6, totalSteps, "Send second message — paneer follow-up") {
                val paneerMessage = "What about something with paneer?"
                chatRobot.sendMessage(paneerMessage)
                chatRobot.assertUserMessageDisplayed(paneerMessage)

                val startTime = System.currentTimeMillis()
                chatRobot.waitForAIResponse(AI_RESPONSE_TIMEOUT)
                val elapsed = System.currentTimeMillis() - startTime
                Log.i(tag, "Second AI response arrived in ${elapsed}ms")
                assertTrue(
                    "Second AI response took ${elapsed}ms — exceeds ${AI_RESPONSE_TIMEOUT}ms limit",
                    elapsed < AI_RESPONSE_TIMEOUT
                )
            }

            logger.step(7, totalSteps, "Send third message — breakfast topic change") {
                val breakfastMessage = TestDataFactory.ChatMessages.QUICK_BREAKFAST
                chatRobot.sendMessage(breakfastMessage)
                chatRobot.assertUserMessageDisplayed(breakfastMessage)

                val startTime = System.currentTimeMillis()
                chatRobot.waitForAIResponse(AI_RESPONSE_TIMEOUT)
                val elapsed = System.currentTimeMillis() - startTime
                Log.i(tag, "Third AI response arrived in ${elapsed}ms")
                assertTrue(
                    "Third AI response took ${elapsed}ms — exceeds ${AI_RESPONSE_TIMEOUT}ms limit",
                    elapsed < AI_RESPONSE_TIMEOUT
                )
            }

            logger.step(8, totalSteps, "Verify Room DB has multi-turn messages") {
                val messageCount = runBlocking { chatDao.getMessageCount() }
                Log.i(tag, "Room DB chat message count: $messageCount")
                assertTrue(
                    "Expected at least 6 chat messages in Room (3 user + 3 AI), found $messageCount",
                    messageCount >= 6
                )

                val messages = runBlocking { chatDao.getAllMessages().first() }
                val userMessages = messages.filter { it.isFromUser }
                val aiMessages = messages.filter { !it.isFromUser }
                Log.i(tag, "User messages: ${userMessages.size}, AI messages: ${aiMessages.size}")

                assertTrue(
                    "Expected at least 3 user messages in Room DB, found ${userMessages.size}",
                    userMessages.size >= 3
                )
                assertTrue(
                    "Expected at least 3 AI responses in Room DB, found ${aiMessages.size}",
                    aiMessages.size >= 3
                )

                val dinnerMessage = userMessages.find {
                    it.content.contains(TestDataFactory.ChatMessages.DINNER_SUGGESTION, ignoreCase = true)
                }
                assertTrue(
                    "Expected user message containing '${TestDataFactory.ChatMessages.DINNER_SUGGESTION}' in Room DB",
                    dinnerMessage != null
                )

                val paneerMessage = userMessages.find {
                    it.content.contains("paneer", ignoreCase = true)
                }
                assertTrue(
                    "Expected user message containing 'paneer' in Room DB",
                    paneerMessage != null
                )

                val breakfastMessage = userMessages.find {
                    it.content.contains("breakfast", ignoreCase = true)
                }
                assertTrue(
                    "Expected user message containing 'breakfast' in Room DB",
                    breakfastMessage != null
                )
            }

            logger.step(9, totalSteps, "Verify backend chat history has multi-turn messages") {
                val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
                if (authToken != null) {
                    val response = BackendTestHelper.getWithRetry(
                        BACKEND_BASE_URL,
                        "/api/v1/chat/history",
                        authToken
                    )
                    assertTrue(
                        "Expected non-null response from /api/v1/chat/history",
                        response != null
                    )

                    val historyObj = JSONObject(response!!)
                    val historyArray = historyObj.getJSONArray("messages")
                    val totalCount = historyObj.optInt("total_count", historyArray.length())
                    Log.i(tag, "Backend chat history: ${historyArray.length()} messages, total_count=$totalCount")
                    assertTrue(
                        "Expected at least 6 messages in backend chat history (3 user + 3 AI), found ${historyArray.length()}",
                        historyArray.length() >= 6
                    )
                } else {
                    Log.w(tag, "No auth token available — skipping backend chat history verification")
                    assertTrue("Auth token should be available for backend verification", false)
                }
            }

            logger.step(10, totalSteps, "Return to Home") {
                homeRobot.navigateToHome()
                homeRobot.assertHomeScreenDisplayed()
            }
        } finally {
            logger.printSummary()
        }
    }
}
