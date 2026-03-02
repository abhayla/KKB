package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.RecipeDetailRobot
import com.rasoiai.app.e2e.util.JourneyStepLogger
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

/**
 * J04: Daily Meal Planning (single Activity session)
 *
 * Scenario: Returning user checks today's meals, browses days, views a recipe.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J04_DailyMealPlanningJourney
 * ```
 */
@HiltAndroidTest
class J04_DailyMealPlanningJourney : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var recipeDetailRobot: RecipeDetailRobot
    private val logger = JourneyStepLogger("J04")

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedState()
        homeRobot = HomeRobot(composeTestRule)
        recipeDetailRobot = RecipeDetailRobot(composeTestRule)
    }

    @Test
    fun dailyMealPlanningJourney() {
        val totalSteps = 5

        try {
            logger.step(1, totalSteps, "Home screen with meals") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
                homeRobot.assertAllMealCardsDisplayed()
            }

            logger.step(2, totalSteps, "Week selector navigation") {
                homeRobot.assertWeekSelectorDisplayed()
                homeRobot.selectDay(DayOfWeek.MONDAY)
                homeRobot.selectDay(DayOfWeek.WEDNESDAY)
                homeRobot.selectDay(DayOfWeek.SATURDAY)
            }

            logger.step(3, totalSteps, "Verify meal types") {
                homeRobot.assertMealCardDisplayed(MealType.BREAKFAST)
                homeRobot.assertMealCardDisplayed(MealType.LUNCH)
                homeRobot.assertMealCardDisplayed(MealType.DINNER)
                homeRobot.assertMealCardDisplayed(MealType.SNACKS)
            }

            logger.step(4, totalSteps, "Open recipe detail") {
                homeRobot.navigateToRecipeDetail(MealType.LUNCH)
                recipeDetailRobot.waitForRecipeDetailScreen()
                recipeDetailRobot.assertRecipeDetailScreenDisplayed()
            }

            logger.step(5, totalSteps, "View recipe info") {
                recipeDetailRobot.assertIngredientsListDisplayed()
                recipeDetailRobot.assertStartCookingDisplayed()
            }
        } finally {
            logger.printSummary()
        }
    }
}
