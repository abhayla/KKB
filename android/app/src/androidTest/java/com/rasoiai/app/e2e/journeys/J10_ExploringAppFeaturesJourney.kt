package com.rasoiai.app.e2e.journeys

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.ChatRobot
import com.rasoiai.app.e2e.robots.FavoritesRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.PantryRobot
import com.rasoiai.app.e2e.robots.StatsRobot
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.app.e2e.util.JourneyStepLogger
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * J10: Exploring App Features (single Activity session)
 *
 * Scenario: User browses Home, Favorites, Chat, Stats, and Pantry (via Settings).
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J10_ExploringAppFeaturesJourney
 * ```
 */
@HiltAndroidTest
class J10_ExploringAppFeaturesJourney : BaseE2ETest() {

    companion object {
        private const val TAG = "J10_ExploringFeatures"
    }

    private lateinit var homeRobot: HomeRobot
    private lateinit var favoritesRobot: FavoritesRobot
    private lateinit var chatRobot: ChatRobot
    private lateinit var statsRobot: StatsRobot
    private lateinit var pantryRobot: PantryRobot
    private val logger = JourneyStepLogger("J10")

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedState()
        homeRobot = HomeRobot(composeTestRule)
        favoritesRobot = FavoritesRobot(composeTestRule)
        chatRobot = ChatRobot(composeTestRule)
        statsRobot = StatsRobot(composeTestRule)
        pantryRobot = PantryRobot(composeTestRule)
    }

    @Test
    fun exploringAppFeaturesJourney() {
        val totalSteps = 13

        try {
            val journeyStartTime = System.currentTimeMillis()

            logger.step(1, totalSteps, "Home screen") {
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

            logger.step(2, totalSteps, "Navigate to Favorites") {
                homeRobot.navigateToFavorites()
                favoritesRobot.waitForFavoritesScreen()
                favoritesRobot.assertFavoritesScreenDisplayed()
            }

            logger.step(3, totalSteps, "Scope toggle on Favorites (soft)") {
                try {
                    composeTestRule.waitUntil(5_000) {
                        composeTestRule.onAllNodes(hasTestTag(TestTags.SCOPE_TOGGLE))
                            .fetchSemanticsNodes().isNotEmpty()
                    }
                    composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE).assertIsDisplayed()
                    Log.i(TAG, "Scope toggle found on Favorites — testing Family/Personal switch")

                    composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_FAMILY).performClick()
                    composeTestRule.waitForIdle()
                    Thread.sleep(500)
                    favoritesRobot.assertFavoritesScreenDisplayed()
                    Log.i(TAG, "Switched to Family scope on Favorites — screen stable")

                    composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_PERSONAL).performClick()
                    composeTestRule.waitForIdle()
                    Thread.sleep(500)
                    favoritesRobot.assertFavoritesScreenDisplayed()
                    Log.i(TAG, "Switched back to Personal scope on Favorites — screen stable")
                } catch (e: Throwable) {
                    Log.w(TAG, "Scope toggle not available on Favorites (no household?): ${e.message}")
                }
            }

            logger.step(4, totalSteps, "Navigate to Chat") {
                homeRobot.navigateToChat()
                chatRobot.waitForChatScreen()
                chatRobot.assertChatScreenDisplayed()
                chatRobot.assertInputFieldDisplayed()
            }

            logger.step(5, totalSteps, "Navigate to Stats") {
                homeRobot.navigateToStats()
                statsRobot.waitForStatsScreen()
                statsRobot.assertStatsScreenDisplayed()
            }

            logger.step(6, totalSteps, "Browse stats widgets") {
                statsRobot.assertStreakWidgetDisplayed()
                statsRobot.assertCalendarDisplayed()
            }

            logger.step(7, totalSteps, "Scroll to achievements section on Stats") {
                statsRobot.scrollToAchievements()
                statsRobot.assertAchievementsSectionDisplayed()
                Log.i(TAG, "Achievements section is displayed on Stats screen")
            }

            logger.step(8, totalSteps, "Browse cuisine breakdown on Stats") {
                statsRobot.scrollToCuisineBreakdown()
                statsRobot.assertCuisineChartDisplayed()
                Log.i(TAG, "Cuisine chart is displayed on Stats screen")
            }

            logger.step(9, totalSteps, "Scope toggle on Stats (soft)") {
                try {
                    composeTestRule.waitUntil(5_000) {
                        composeTestRule.onAllNodes(hasTestTag(TestTags.SCOPE_TOGGLE))
                            .fetchSemanticsNodes().isNotEmpty()
                    }
                    composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE).assertIsDisplayed()
                    Log.i(TAG, "Scope toggle found on Stats — testing Family/Personal switch")

                    composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_FAMILY).performClick()
                    composeTestRule.waitForIdle()
                    Thread.sleep(1_000)
                    statsRobot.assertStatsScreenDisplayed()
                    Log.i(TAG, "Switched to Family scope on Stats — screen stable")

                    composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_PERSONAL).performClick()
                    composeTestRule.waitForIdle()
                    Thread.sleep(1_000)
                    statsRobot.assertStatsScreenDisplayed()
                    Log.i(TAG, "Switched back to Personal scope on Stats — screen stable")
                } catch (e: Throwable) {
                    Log.w(TAG, "Scope toggle not available on Stats (no household?): ${e.message}")
                }
            }

            logger.step(10, totalSteps, "Navigate to Pantry via Settings") {
                homeRobot.navigateToHome()
                homeRobot.assertHomeScreenDisplayed()
                homeRobot.navigateToSettings()
                composeTestRule.waitForIdle()
                Thread.sleep(500)
                composeTestRule.onNodeWithText("Pantry", ignoreCase = true)
                    .performScrollTo()
                    .performClick()
                composeTestRule.waitForIdle()
                pantryRobot.waitForPantryScreen()
                pantryRobot.assertPantryScreenDisplayed()
                Log.d(TAG, "Pantry screen displayed via Settings navigation")
                composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                composeTestRule.waitForIdle()
            }

            logger.step(11, totalSteps, "Return to Home") {
                homeRobot.navigateToHome()
                homeRobot.assertHomeScreenDisplayed()
            }

            logger.step(12, totalSteps, "Verify backend stats endpoint returns data") {
                val apiStart = System.currentTimeMillis()
                val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
                assertNotNull("Auth token should be available", authToken)
                val statsResponse = BackendTestHelper.getWithRetry(
                    BACKEND_BASE_URL, "/api/v1/stats", authToken
                )
                assertNotNull("Backend stats endpoint should return data", statsResponse)
                Log.d(TAG, "Stats response: ${statsResponse?.take(200)}")
                val apiTime = System.currentTimeMillis() - apiStart
                Log.i(TAG, "Backend stats verification: ${apiTime}ms")
                assertTrue(
                    "Backend API call should complete within 5s (took ${apiTime}ms)",
                    apiTime < 5_000
                )
            }

            logger.step(13, totalSteps, "Verify backend favorites endpoint works") {
                val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
                assertNotNull("Auth token should be available", authToken)
                val favoritesResponse = BackendTestHelper.getWithRetry(
                    BACKEND_BASE_URL, "/api/v1/favorites", authToken
                )
                assertNotNull("Backend favorites endpoint should return valid response", favoritesResponse)
                Log.d(TAG, "Favorites response: ${favoritesResponse?.take(200)}")
            }

            // Performance guardrail
            val totalDuration = System.currentTimeMillis() - journeyStartTime
            Log.i(TAG, "Total journey time: ${totalDuration}ms")
            assertTrue(
                "J10 journey should complete within 45s (took ${totalDuration}ms)",
                totalDuration < 45_000
            )
        } finally {
            logger.printSummary()
        }
    }
}
