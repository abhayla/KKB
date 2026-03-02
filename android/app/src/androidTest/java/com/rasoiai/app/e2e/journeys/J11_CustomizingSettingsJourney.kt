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
 * J11: Customizing App Settings (single Activity session)
 *
 * Scenario: User navigates Settings sub-screens: theme, notifications, recipe rules, meal gen.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J11_CustomizingSettingsJourney
 * ```
 */
@HiltAndroidTest
class J11_CustomizingSettingsJourney : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot
    private lateinit var recipeRulesRobot: RecipeRulesRobot
    private val logger = JourneyStepLogger("J11")

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedStateWithoutMealPlan()
        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)
        recipeRulesRobot = RecipeRulesRobot(composeTestRule)
    }

    @Test
    fun customizingSettingsJourney() {
        val totalSteps = 6

        try {
            logger.step(1, totalSteps, "Navigate to Settings") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.navigateToSettings()
                settingsRobot.waitForSettingsScreen()
                settingsRobot.assertSettingsScreenDisplayed()
            }

            logger.step(2, totalSteps, "Theme settings") {
                settingsRobot.navigateToTheme()
                settingsRobot.selectDarkTheme()
                settingsRobot.selectSystemTheme()
            }

            logger.step(3, totalSteps, "Notification settings") {
                settingsRobot.navigateToNotifications()
                settingsRobot.toggleMealReminders()
                waitFor(500)
            }

            logger.step(4, totalSteps, "Back to Settings") {
                composeTestRule.activityRule.scenario.onActivity { activity ->
                    activity.onBackPressedDispatcher.onBackPressed()
                }
                waitFor(500)
                settingsRobot.waitForSettingsScreen()
            }

            logger.step(5, totalSteps, "Meal generation section") {
                settingsRobot.assertMealGenerationSectionDisplayed()
                settingsRobot.assertItemsPerMealValue("2 items")
            }

            logger.step(6, totalSteps, "Recipe rules") {
                settingsRobot.navigateToRecipeRules()
                recipeRulesRobot.waitForRecipeRulesScreen()
                recipeRulesRobot.assertRecipeRulesScreenDisplayed()
                recipeRulesRobot.selectRulesTab()
                recipeRulesRobot.selectNutritionTab()
            }
        } finally {
            logger.printSummary()
        }
    }
}
