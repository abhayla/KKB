package com.rasoiai.app.e2e.journeys

import android.util.Log
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.AuthRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.OnboardingRobot
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.app.e2e.util.JourneyStepLogger
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * J01: First-Time User Gets Started (single Activity session)
 *
 * Scenario: Brand new user signs up and completes onboarding.
 * The Activity stays alive for the entire journey — no restarts.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J01_FirstTimeUserJourney
 * ```
 */
@HiltAndroidTest
class J01_FirstTimeUserJourney : BaseE2ETest() {

    companion object {
        private const val TAG = "J01_FirstTimeUser"
    }

    private lateinit var authRobot: AuthRobot
    private lateinit var onboardingRobot: OnboardingRobot
    private lateinit var homeRobot: HomeRobot
    private val logger = JourneyStepLogger("J01")

    @Before
    override fun setUp() {
        super.setUp()
        setUpNewUserState()
        authRobot = AuthRobot(composeTestRule)
        onboardingRobot = OnboardingRobot(composeTestRule)
        homeRobot = HomeRobot(composeTestRule)
    }

    @Test
    fun firstTimeUserJourney() {
        val profile = activeProfile
        val totalSteps = 8

        try {
            val journeyStartTime = System.currentTimeMillis()

            logger.step(1, totalSteps, "Auth screen displayed") {
                authRobot.waitForAuthScreen()
                authRobot.assertAuthScreenDisplayed()
                authRobot.assertWelcomeTextDisplayed()
            }

            logger.step(2, totalSteps, "Phone auth sign-up") {
                authRobot.enterPhoneNumber()
                authRobot.tapSendOtp()
                authRobot.assertNavigatedToOnboarding()
            }

            logger.step(3, totalSteps, "Onboarding Step 1-2: Household & Diet") {
                onboardingRobot.selectHouseholdSize(profile.householdSize)
                for (member in profile.members) {
                    onboardingRobot.addFamilyMember(member)
                }
                onboardingRobot.tapNext()
                composeTestRule.waitForIdle()

                // Step 2: Diet
                for (restriction in profile.dietaryRestrictions) {
                    onboardingRobot.selectDietaryRestriction(restriction)
                }
                onboardingRobot.tapNext()
                composeTestRule.waitForIdle()
            }

            logger.step(4, totalSteps, "Onboarding Step 3-4: Cuisine & Dislikes") {
                // Step 3: Cuisine
                for (cuisine in profile.cuisines) {
                    onboardingRobot.selectCuisine(cuisine)
                }
                onboardingRobot.selectSpiceLevel(profile.spiceLevel)
                onboardingRobot.tapNext()
                composeTestRule.waitForIdle()

                // Step 4: Dislikes
                for (ingredient in profile.dislikedIngredients) {
                    onboardingRobot.selectDislikedIngredient(ingredient)
                }
                onboardingRobot.tapNext()
                composeTestRule.waitForIdle()
            }

            logger.step(5, totalSteps, "Onboarding Step 5: Cooking time & submit") {
                onboardingRobot.setWeekdayCookingTime(profile.weekdayCookingTime)
                onboardingRobot.setWeekendCookingTime(profile.weekendCookingTime)
                for (day in profile.busyDays) {
                    onboardingRobot.selectBusyDay(day)
                }
                onboardingRobot.tapCreateMealPlan()
                onboardingRobot.waitForGeneratingScreen()
            }

            logger.step(6, totalSteps, "Home screen appears after generation") {
                val homeLoadStart = System.currentTimeMillis()
                // Gemini generation can take 30-90s; wait up to full timeout
                homeRobot.waitForHomeScreen(GEMINI_FULL_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                val homeLoadTime = System.currentTimeMillis() - homeLoadStart
                Log.i(TAG, "Home screen load time after generation: ${homeLoadTime}ms")
                // Don't assert on load time — Gemini latency varies widely (4-90s)
            }

            logger.step(7, totalSteps, "Backend has meal plan") {
                val apiStart = System.currentTimeMillis()
                val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
                assertNotNull("Auth token should be available after sign-up", authToken)
                val mealPlanJson = BackendTestHelper.getCurrentMealPlan(BACKEND_BASE_URL, authToken!!)
                assertNotNull("Backend should have a meal plan after generation", mealPlanJson)
                val apiTime = System.currentTimeMillis() - apiStart
                Log.i(TAG, "Backend meal plan verification: ${apiTime}ms")
                assertTrue(
                    "Backend API call should complete within 5s (took ${apiTime}ms)",
                    apiTime < 5_000
                )
            }

            logger.step(8, totalSteps, "Room DB has meal plan") {
                val today = java.time.LocalDate.now().toString()
                val hasMealPlan = runBlocking { mealPlanDao.hasMealPlanForDate(today) }
                assertTrue("Room DB should have a meal plan for today ($today)", hasMealPlan)
            }
            // Performance guardrail — relaxed because Gemini gen can take 30-90s
            val totalDuration = System.currentTimeMillis() - journeyStartTime
            Log.i(TAG, "Total journey time: ${totalDuration}ms")
            assertTrue(
                "J01 journey should complete within 300s (took ${totalDuration}ms)",
                totalDuration < 300_000
            )
        } finally {
            logger.printSummary()
        }
    }
}
