package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.AuthRobot
import com.rasoiai.app.e2e.robots.ChatRobot
import com.rasoiai.app.e2e.robots.GroceryRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.OnboardingRobot
import com.rasoiai.app.e2e.util.JourneyStepLogger
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * J03: Complete End-to-End Journey (single Activity session)
 *
 * Scenario: Auth → Onboarding → Generation → Home → Grocery → Chat — all in one Activity.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J03_CompleteE2EJourney
 * ```
 */
@HiltAndroidTest
class J03_CompleteE2EJourney : BaseE2ETest() {

    private lateinit var authRobot: AuthRobot
    private lateinit var onboardingRobot: OnboardingRobot
    private lateinit var homeRobot: HomeRobot
    private lateinit var groceryRobot: GroceryRobot
    private lateinit var chatRobot: ChatRobot
    private val logger = JourneyStepLogger("J03")

    @Before
    override fun setUp() {
        super.setUp()
        clearAllState()
        authRobot = AuthRobot(composeTestRule)
        onboardingRobot = OnboardingRobot(composeTestRule)
        homeRobot = HomeRobot(composeTestRule)
        groceryRobot = GroceryRobot(composeTestRule)
        chatRobot = ChatRobot(composeTestRule)
    }

    @Test
    fun completeEndToEndJourney() {
        val profile = activeProfile
        val totalSteps = 7

        try {
            logger.step(1, totalSteps, "Auth flow") {
                authRobot.waitForAuthScreen()
                authRobot.enterPhoneNumber()
                authRobot.tapSendOtp()
                authRobot.assertNavigatedToOnboarding()
            }

            logger.step(2, totalSteps, "Full onboarding") {
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

            logger.step(3, totalSteps, "Meal plan generation") {
                onboardingRobot.waitForGeneratingScreen(timeoutMillis = GENERATION_TIMEOUT_MS)
            }

            logger.step(4, totalSteps, "Home screen with meal data") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
                homeRobot.assertAllMealCardsDisplayed()
            }

            logger.step(5, totalSteps, "Navigate to Grocery") {
                homeRobot.navigateToGrocery()
                groceryRobot.waitForGroceryScreen()
                groceryRobot.assertGroceryScreenDisplayed()
            }

            logger.step(6, totalSteps, "Navigate to Chat") {
                homeRobot.navigateToChat()
                chatRobot.waitForChatScreen()
                chatRobot.assertChatScreenDisplayed()
                chatRobot.assertInputFieldDisplayed()
            }

            logger.step(7, totalSteps, "Return to Home") {
                homeRobot.navigateToHome()
                homeRobot.assertHomeScreenDisplayed()
            }
        } finally {
            logger.printSummary()
        }
    }
}
