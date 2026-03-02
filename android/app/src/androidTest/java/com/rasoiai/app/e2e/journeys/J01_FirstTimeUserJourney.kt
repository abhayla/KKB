package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.AuthRobot
import com.rasoiai.app.e2e.robots.OnboardingRobot
import com.rasoiai.app.e2e.util.JourneyStepLogger
import dagger.hilt.android.testing.HiltAndroidTest
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

    private lateinit var authRobot: AuthRobot
    private lateinit var onboardingRobot: OnboardingRobot
    private val logger = JourneyStepLogger("J01")

    @Before
    override fun setUp() {
        super.setUp()
        setUpNewUserState()
        authRobot = AuthRobot(composeTestRule)
        onboardingRobot = OnboardingRobot(composeTestRule)
    }

    @Test
    fun firstTimeUserJourney() {
        val profile = activeProfile
        val totalSteps = 5

        try {
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
        } finally {
            logger.printSummary()
        }
    }
}
