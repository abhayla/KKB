package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.CookingModeRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.RecipeDetailRobot
import com.rasoiai.app.e2e.util.JourneyStepLogger
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * J06: Cooking a Meal (single Activity session)
 *
 * Scenario: User picks a meal from Home, opens recipe detail, enters cooking mode.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J06_CookingAMealJourney
 * ```
 */
@HiltAndroidTest
class J06_CookingAMealJourney : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var recipeDetailRobot: RecipeDetailRobot
    private lateinit var cookingModeRobot: CookingModeRobot
    private val logger = JourneyStepLogger("J06")

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedState()
        homeRobot = HomeRobot(composeTestRule)
        recipeDetailRobot = RecipeDetailRobot(composeTestRule)
        cookingModeRobot = CookingModeRobot(composeTestRule)
    }

    @Test
    fun cookingAMealJourney() {
        val totalSteps = 5

        try {
            logger.step(1, totalSteps, "Home with meals") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
                homeRobot.assertAllMealCardsDisplayed()
            }

            logger.step(2, totalSteps, "Open dinner recipe") {
                homeRobot.navigateToRecipeDetail(MealType.DINNER)
                recipeDetailRobot.waitForRecipeDetailScreen()
                recipeDetailRobot.assertRecipeDetailScreenDisplayed()
            }

            logger.step(3, totalSteps, "View recipe ingredients") {
                recipeDetailRobot.assertIngredientsListDisplayed()
                recipeDetailRobot.assertServingsSelectorDisplayed()
            }

            logger.step(4, totalSteps, "Enter cooking mode") {
                recipeDetailRobot.tapStartCooking()
                cookingModeRobot.waitForCookingModeScreen()
                cookingModeRobot.assertCookingModeScreenDisplayed()
            }

            logger.step(5, totalSteps, "Navigate cooking steps") {
                cookingModeRobot.assertStepDisplayed(1, -1)
                cookingModeRobot.nextStep()
            }
        } finally {
            logger.printSummary()
        }
    }
}
