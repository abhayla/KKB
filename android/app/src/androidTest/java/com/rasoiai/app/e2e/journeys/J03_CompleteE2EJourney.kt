package com.rasoiai.app.e2e.journeys

import android.util.Log
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.AuthRobot
import com.rasoiai.app.e2e.robots.ChatRobot
import com.rasoiai.app.e2e.robots.GroceryRobot
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

    companion object {
        private const val TAG = "J03_CompleteE2E"
    }

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
        val totalSteps = 9

        try {
            val journeyStartTime = System.currentTimeMillis()

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

            logger.step(8, totalSteps, "Verify backend data persistence") {
                val apiStart = System.currentTimeMillis()
                val authToken = runBlocking {
                    userPreferencesDataStore.accessToken.first()
                }
                assertNotNull("Auth token should be stored in DataStore", authToken)

                val user = BackendTestHelper.getCurrentUser(BACKEND_BASE_URL, authToken!!)
                assertNotNull("Backend should return current user", user)

                val mealPlan = BackendTestHelper.getCurrentMealPlan(BACKEND_BASE_URL, authToken)
                assertNotNull("Backend should have a meal plan", mealPlan)

                val daysArray = mealPlan!!.getJSONArray("days")
                assertTrue(
                    "Meal plan should have at least 1 day",
                    daysArray.length() > 0
                )

                // Verify at least one day has meals for all meal types
                val firstDay = daysArray.getJSONObject(0)
                val meals = firstDay.getJSONObject("meals")
                for (mealType in listOf("breakfast", "lunch", "dinner")) {
                    assertTrue(
                        "Day should have $mealType meals",
                        meals.has(mealType) && meals.getJSONArray(mealType).length() > 0
                    )
                }
                val apiTime = System.currentTimeMillis() - apiStart
                Log.i(TAG, "Backend data verification: ${apiTime}ms")
                assertTrue(
                    "Backend API calls should complete within 5s (took ${apiTime}ms)",
                    apiTime < 5_000
                )
            }

            logger.step(9, totalSteps, "Verify local Room database persistence") {
                val today = java.time.LocalDate.now().toString()

                val hasPlan = runBlocking { mealPlanDao.hasMealPlanForDate(today) }
                assertTrue("Room should have a meal plan for today", hasPlan)

                val localPlan = runBlocking {
                    mealPlanDao.getMealPlanForDate(today).first()
                }
                assertNotNull("Room meal plan entity should not be null", localPlan)

                val localItems = runBlocking {
                    mealPlanDao.getMealPlanItemsSync(localPlan!!.id)
                }
                assertTrue(
                    "Room should have meal plan items (found ${localItems.size})",
                    localItems.isNotEmpty()
                )

                // Verify items cover multiple meal types
                val mealTypes = localItems.map { it.mealType }.distinct()
                assertTrue(
                    "Room items should cover at least 3 meal types (found: $mealTypes)",
                    mealTypes.size >= 3
                )

                // Verify each item has a recipe name
                for (item in localItems) {
                    assertNotNull(
                        "Meal plan item for ${item.mealType} should have a recipe name",
                        item.recipeName
                    )
                    assertTrue(
                        "Recipe name should not be blank for ${item.mealType}",
                        item.recipeName.isNotBlank()
                    )
                }
            }

            // Performance guardrail
            val totalDuration = System.currentTimeMillis() - journeyStartTime
            Log.i(TAG, "Total journey time: ${totalDuration}ms")
            assertTrue(
                "J03 journey should complete within 120s (took ${totalDuration}ms)",
                totalDuration < 120_000
            )
        } finally {
            logger.printSummary()
        }
    }
}
