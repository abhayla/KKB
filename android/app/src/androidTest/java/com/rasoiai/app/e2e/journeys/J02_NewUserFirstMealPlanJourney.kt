package com.rasoiai.app.e2e.journeys

import android.util.Log
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.AuthRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.OnboardingRobot
import com.rasoiai.app.e2e.util.JourneyStepLogger
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * J02: New User First Meal Plan (single Activity session)
 *
 * Scenario: New user signs up, onboards, and sees their first meal plan on Home.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J02_NewUserFirstMealPlanJourney
 * ```
 */
@HiltAndroidTest
class J02_NewUserFirstMealPlanJourney : BaseE2ETest() {

    companion object {
        private const val TAG = "J02_NewUserMealPlan"
    }

    private lateinit var authRobot: AuthRobot
    private lateinit var onboardingRobot: OnboardingRobot
    private lateinit var homeRobot: HomeRobot
    private val logger = JourneyStepLogger("J02")

    @Before
    override fun setUp() {
        super.setUp()
        clearAllState()
        authRobot = AuthRobot(composeTestRule)
        onboardingRobot = OnboardingRobot(composeTestRule)
        homeRobot = HomeRobot(composeTestRule)
    }

    @Test
    fun newUserFirstMealPlanJourney() {
        val profile = activeProfile
        val totalSteps = 6

        try {
            val journeyStartTime = System.currentTimeMillis()

            logger.step(1, totalSteps, "Sign up via phone auth") {
                authRobot.waitForAuthScreen()
                authRobot.enterPhoneNumber()
                authRobot.tapSendOtp()
                authRobot.assertNavigatedToOnboarding()
            }

            logger.step(2, totalSteps, "Complete onboarding") {
                onboardingRobot.selectHouseholdSize(profile.householdSize)
                for (member in profile.members) {
                    onboardingRobot.addFamilyMember(member)
                }
                onboardingRobot.tapNext()
                composeTestRule.waitForIdle()

                for (restriction in profile.dietaryRestrictions) {
                    onboardingRobot.selectDietaryRestriction(restriction)
                }
                onboardingRobot.tapNext()
                composeTestRule.waitForIdle()

                for (cuisine in profile.cuisines) {
                    onboardingRobot.selectCuisine(cuisine)
                }
                onboardingRobot.selectSpiceLevel(profile.spiceLevel)
                onboardingRobot.tapNext()
                composeTestRule.waitForIdle()

                for (ingredient in profile.dislikedIngredients) {
                    onboardingRobot.selectDislikedIngredient(ingredient)
                }
                onboardingRobot.tapNext()
                composeTestRule.waitForIdle()

                onboardingRobot.setWeekdayCookingTime(profile.weekdayCookingTime)
                onboardingRobot.setWeekendCookingTime(profile.weekendCookingTime)
                for (day in profile.busyDays) {
                    onboardingRobot.selectBusyDay(day)
                }
                onboardingRobot.tapCreateMealPlan()
            }

            logger.step(3, totalSteps, "Wait for meal plan generation") {
                onboardingRobot.waitForGeneratingScreen(timeoutMillis = GENERATION_TIMEOUT_MS)
            }

            logger.step(4, totalSteps, "Home screen loads") {
                val homeLoadStart = System.currentTimeMillis()
                // Gemini generation can take 30-90s; wait up to full timeout
                homeRobot.waitForHomeScreen(GEMINI_FULL_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                val homeLoadTime = System.currentTimeMillis() - homeLoadStart
                Log.i(TAG, "Home screen load time after generation: ${homeLoadTime}ms")
            }

            logger.step(5, totalSteps, "Week selector visible") {
                homeRobot.assertWeekSelectorDisplayed()
            }

            logger.step(6, totalSteps, "Meal cards displayed") {
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
                homeRobot.assertAllMealCardsDisplayed()
            }
            // Performance guardrail
            val totalDuration = System.currentTimeMillis() - journeyStartTime
            Log.i(TAG, "Total journey time: ${totalDuration}ms")
            assertTrue(
                "J02 journey should complete within 300s (took ${totalDuration}ms)",
                totalDuration < 300_000
            )
        } finally {
            logger.printSummary()
        }
    }
}
