package com.rasoiai.app.e2e.flows

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.base.TestDataFactory
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.RecipeRulesRobot
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * Phase 11: Recipe Rules Screen Testing
 *
 * Tests:
 * 11.1 Include Rules Tab
 * 11.2 Exclude Rules Tab
 * 11.3 Nutrition Goals Tab
 */
@HiltAndroidTest
class RecipeRulesFlowTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var recipeRulesRobot: RecipeRulesRobot

    @Before
    override fun setUp() {
        super.setUp()
        // Set up authenticated and onboarded user state
        setUpAuthenticatedState()

        homeRobot = HomeRobot(composeTestRule)
        recipeRulesRobot = RecipeRulesRobot(composeTestRule)

        // Navigate to recipe rules screen
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
        // Recipe rules navigation path depends on UI implementation
    }

    /**
     * Test 11.1: Include Rules Tab
     *
     * Steps:
     * 1. Navigate to Recipe Rules screen
     * 2. Verify 4 tabs: Include, Exclude, Nutrition, Settings
     * 3. On Include tab, add rule:
     *    - Type: RECIPE
     *    - Target: "Dal Tadka"
     *    - Frequency: 2 times per week
     *    - Meal Slot: Lunch/Dinner
     *    - Enforcement: PREFERRED
     * 4. Save rule
     */
    @Test
    fun test_11_1_includeRulesTab() {
        recipeRulesRobot.waitForRecipeRulesScreen()
        recipeRulesRobot.assertRecipeRulesScreenDisplayed()

        // Add include rule
        recipeRulesRobot.addIncludeRule(TestDataFactory.RecipeRules.includeDalTadka)

        // Verify rule card is displayed
        recipeRulesRobot.assertRuleCardDisplayed("Dal Tadka")
        recipeRulesRobot.assertRuleWithFrequency("Dal Tadka", "2")
    }

    /**
     * Test 11.2: Exclude Rules Tab
     *
     * Steps:
     * 1. Switch to Exclude tab
     * 2. Add ingredient exclusion:
     *    - Type: INGREDIENT
     *    - Target: "Paneer"
     *    - Frequency: NEVER
     *    - Enforcement: REQUIRED
     * 3. Save rule
     */
    @Test
    fun test_11_2_excludeRulesTab() {
        recipeRulesRobot.waitForRecipeRulesScreen()

        // Add exclude rule
        recipeRulesRobot.addExcludeRule(TestDataFactory.RecipeRules.excludePaneer)

        // Verify rule card is displayed
        recipeRulesRobot.selectExcludeTab()
        recipeRulesRobot.assertRuleCardDisplayed("Paneer")
    }

    /**
     * Test 11.3: Nutrition Goals Tab
     *
     * Steps:
     * 1. Switch to Nutrition tab
     * 2. Add goal:
     *    - Food Category: GREEN_LEAFY
     *    - Weekly Target: 5 servings
     *    - Enforcement: PREFERRED
     * 3. Save goal
     */
    @Test
    fun test_11_3_nutritionGoalsTab() {
        recipeRulesRobot.waitForRecipeRulesScreen()

        // Add nutrition goal
        recipeRulesRobot.addNutritionGoal(TestDataFactory.RecipeRules.greenLeafyGoal)

        // Verify goal is displayed
        recipeRulesRobot.selectNutritionTab()
        recipeRulesRobot.assertRuleCardDisplayed("Green Leafy")
    }

    /**
     * Test: Tab navigation
     */
    @Test
    fun tabNavigation_works() {
        recipeRulesRobot.waitForRecipeRulesScreen()

        recipeRulesRobot.selectIncludeTab()
        recipeRulesRobot.selectExcludeTab()
        recipeRulesRobot.selectNutritionTab()
        recipeRulesRobot.selectSettingsTab()
    }

    /**
     * Test: Delete rule
     */
    @Test
    fun deleteRule() {
        recipeRulesRobot.waitForRecipeRulesScreen()

        // Add a rule first
        recipeRulesRobot.addIncludeRule(TestDataFactory.RecipeRules.includeDalTadka)
        recipeRulesRobot.assertRuleCardDisplayed("Dal Tadka")

        // Delete the rule
        recipeRulesRobot.deleteRule("Dal Tadka")

        // Verify empty state
        recipeRulesRobot.assertEmptyStateDisplayed()
    }

    /**
     * Test: Toggle rule active state
     */
    @Test
    fun toggleRuleActive() {
        recipeRulesRobot.waitForRecipeRulesScreen()

        // Add a rule
        recipeRulesRobot.addIncludeRule(TestDataFactory.RecipeRules.includeDalTadka)

        // Toggle active state
        recipeRulesRobot.toggleRuleActive("Dal Tadka")
    }

    /**
     * Test: Edit existing rule
     */
    @Test
    fun editExistingRule() {
        recipeRulesRobot.waitForRecipeRulesScreen()

        // Add a rule
        recipeRulesRobot.addIncludeRule(TestDataFactory.RecipeRules.includeDalTadka)

        // Tap to edit
        recipeRulesRobot.tapRuleCard("Dal Tadka")

        // Edit sheet should open
        waitFor(ANIMATION_DURATION)
    }

    /**
     * Test: Nutrition goal progress
     */
    @Test
    fun nutritionGoalProgress() {
        recipeRulesRobot.waitForRecipeRulesScreen()

        // Add a nutrition goal
        recipeRulesRobot.addNutritionGoal(TestDataFactory.RecipeRules.greenLeafyGoal)

        // Check progress display
        recipeRulesRobot.selectNutritionTab()
        recipeRulesRobot.assertNutritionGoalProgress("Green Leafy", "0/5")
    }
}
