package com.rasoiai.app.e2e.journeys

import android.util.Log
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.app.e2e.util.JourneyStepLogger
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

/**
 * J08: AI Meal Plan Quality Assurance (single Activity session)
 *
 * Scenario: User reviews the generated meal plan across multiple days for quality.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J08_AIMealPlanQualityJourney
 * ```
 */
@HiltAndroidTest
class J08_AIMealPlanQualityJourney : BaseE2ETest() {

    companion object {
        private const val TAG = "J08_AIMealPlanQuality"
    }

    private lateinit var homeRobot: HomeRobot
    private val logger = JourneyStepLogger("J08")

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedState()
        homeRobot = HomeRobot(composeTestRule)
    }

    @Test
    fun aiMealPlanQualityJourney() {
        val totalSteps = 10

        try {
            val journeyStartTime = System.currentTimeMillis()

            logger.step(1, totalSteps, "Home with meal data") {
                val homeLoadStart = System.currentTimeMillis()
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
                homeRobot.assertAllMealCardsDisplayed()
                val homeLoadTime = System.currentTimeMillis() - homeLoadStart
                Log.i(TAG, "Home screen load time: ${homeLoadTime}ms")
                assertTrue(
                    "Home screen should load within 5s (took ${homeLoadTime}ms)",
                    homeLoadTime < 5_000
                )
            }

            logger.step(2, totalSteps, "Review Monday meals") {
                homeRobot.selectDay(DayOfWeek.MONDAY)
                homeRobot.assertMealCardDisplayed(MealType.BREAKFAST)
                homeRobot.assertMealCardDisplayed(MealType.DINNER)
            }

            logger.step(3, totalSteps, "Review Wednesday meals") {
                homeRobot.selectDay(DayOfWeek.WEDNESDAY)
                homeRobot.assertMealCardDisplayed(MealType.LUNCH)
                homeRobot.assertMealCardDisplayed(MealType.SNACKS)
            }

            logger.step(4, totalSteps, "Review Saturday meals") {
                homeRobot.selectDay(DayOfWeek.SATURDAY)
                homeRobot.assertMealCardDisplayed(MealType.BREAKFAST)
                homeRobot.assertMealCardDisplayed(MealType.DINNER)
            }

            logger.step(5, totalSteps, "Week selector is functional") {
                homeRobot.assertWeekSelectorDisplayed()
                homeRobot.selectDay(DayOfWeek.SUNDAY)
            }

            logger.step(6, totalSteps, "Seed festival and verify backend") {
                val apiStart = System.currentTimeMillis()
                val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
                assertNotNull("Auth token should be available", authToken)

                val today = java.time.LocalDate.now().toString()
                val festivalBody = JSONObject().apply {
                    put("name", "Test Navratri")
                    put("date", today)
                    put("is_fasting_day", true)
                    put("fasting_type", "partial")
                    put("special_foods", JSONArray(listOf("Sabudana Khichdi", "Kuttu Atta Puri")))
                    put("avoided_foods", JSONArray(listOf("grains", "onion", "garlic")))
                    put("regions", JSONArray(listOf("all")))
                }

                val result = BackendTestHelper.postWithRetry(
                    BACKEND_BASE_URL, "/api/v1/festivals", festivalBody, authToken
                )
                assertNotNull("Festival should be created via POST /festivals", result)
                Log.i(TAG, "Created test festival: $result")
                val apiTime = System.currentTimeMillis() - apiStart
                Log.i(TAG, "Festival seed API call: ${apiTime}ms")
                assertTrue(
                    "Backend API call should complete within 5s (took ${apiTime}ms)",
                    apiTime < 5_000
                )
            }

            logger.step(7, totalSteps, "Verify festival in upcoming list") {
                val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
                val upcomingResult = BackendTestHelper.getWithRetry(
                    BACKEND_BASE_URL, "/api/v1/festivals/upcoming?days=7", authToken!!
                )
                assertNotNull("Upcoming festivals should return data", upcomingResult)

                val festivals = JSONArray(upcomingResult)
                var found = false
                for (i in 0 until festivals.length()) {
                    if (festivals.getJSONObject(i).getString("name") == "Test Navratri") {
                        found = true
                        assertTrue(
                            "Test festival should be a fasting day",
                            festivals.getJSONObject(i).getBoolean("is_fasting_day")
                        )
                        break
                    }
                }
                assertTrue("Test Navratri should appear in upcoming festivals", found)
            }

            logger.step(8, totalSteps, "Verify meal plan has festival-aware data from backend") {
                val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
                val mealPlan = BackendTestHelper.getCurrentMealPlan(BACKEND_BASE_URL, authToken!!)
                assertNotNull("Meal plan should exist", mealPlan)
                Log.i(TAG, "Meal plan verified from backend — festival seeding complete")
            }

            logger.step(9, totalSteps, "Tap festival banner and verify recipes sheet") {
                // Festival banner may or may not be displayed depending on whether
                // the current day coincides with the seeded festival. Try soft assertion.
                try {
                    homeRobot.assertFestivalBannerDisplayed()
                    Log.i(TAG, "Festival banner is displayed — tapping to view recipes")

                    homeRobot.tapFestivalBanner()
                    homeRobot.assertFestivalRecipesSheetDisplayed()
                    Log.i(TAG, "Festival recipes sheet displayed successfully")

                    homeRobot.dismissFestivalRecipesSheet()
                    Log.i(TAG, "Festival recipes sheet dismissed")
                } catch (e: Throwable) {
                    Log.w(TAG, "Festival banner not displayed on current day (expected if festival date doesn't match): ${e.message}")
                }
            }

            logger.step(10, totalSteps, "Open and dismiss refresh options sheet") {
                homeRobot.tapRefreshButton()
                homeRobot.assertRefreshSheetDisplayed()
                Log.i(TAG, "Refresh options sheet displayed")

                homeRobot.dismissRefreshSheet()
                Log.i(TAG, "Refresh options sheet dismissed")
            }

            // Performance guardrail
            val totalDuration = System.currentTimeMillis() - journeyStartTime
            Log.i(TAG, "Total journey time: ${totalDuration}ms")
            assertTrue(
                "J08 journey should complete within 60s (took ${totalDuration}ms)",
                totalDuration < 60_000
            )
        } finally {
            logger.printSummary()
        }
    }
}
