package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.RecipeRulesRobot
import com.rasoiai.app.e2e.robots.SettingsRobot
import com.rasoiai.app.e2e.util.JourneyStepLogger
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * J07: Managing Dietary Preferences (single Activity session)
 *
 * Scenario: User navigates to Settings, updates dietary prefs, manages recipe rules.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J07_ManagingDietaryPrefsJourney
 * ```
 */
@HiltAndroidTest
class J07_ManagingDietaryPrefsJourney : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot
    private lateinit var recipeRulesRobot: RecipeRulesRobot
    private val logger = JourneyStepLogger("J07")

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedStateWithoutMealPlan()
        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)
        recipeRulesRobot = RecipeRulesRobot(composeTestRule)
    }

    @Test
    fun managingDietaryPrefsJourney() {
        val totalSteps = 7

        try {
            logger.step(1, totalSteps, "Wait for Home") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
            }

            logger.step(2, totalSteps, "Navigate to Settings") {
                homeRobot.navigateToSettings()
                settingsRobot.waitForSettingsScreen()
                settingsRobot.assertSettingsScreenDisplayed()
            }

            logger.step(3, totalSteps, "Open dietary preferences") {
                settingsRobot.navigateToDietaryPreferences()
                waitFor(1000)
            }

            logger.step(4, totalSteps, "Go back to Settings") {
                composeTestRule.activityRule.scenario.onActivity { activity ->
                    activity.onBackPressedDispatcher.onBackPressed()
                }
                waitFor(500)
                settingsRobot.waitForSettingsScreen()
            }

            logger.step(5, totalSteps, "Open disliked ingredients") {
                settingsRobot.navigateToDislikedIngredients()
                waitFor(1000)
            }

            logger.step(6, totalSteps, "Go back and open recipe rules") {
                composeTestRule.activityRule.scenario.onActivity { activity ->
                    activity.onBackPressedDispatcher.onBackPressed()
                }
                waitFor(500)
                settingsRobot.navigateToRecipeRules()
                recipeRulesRobot.waitForRecipeRulesScreen()
                recipeRulesRobot.assertRecipeRulesScreenDisplayed()
            }

            logger.step(7, totalSteps, "Browse recipe rules tabs") {
                recipeRulesRobot.selectRulesTab()
                recipeRulesRobot.selectNutritionTab()
            }
        } finally {
            logger.printSummary()
        }
    }
}
