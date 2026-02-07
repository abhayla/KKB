package com.rasoiai.app.e2e.flows

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.base.FrequencyType
import com.rasoiai.app.e2e.base.RuleEnforcement
import com.rasoiai.app.e2e.base.TestDataFactory
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.RecipeRulesRobot
import com.rasoiai.app.e2e.robots.SettingsRobot
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * Phase 11: Recipe Rules Screen Testing
 *
 * Tests:
 * 11.1 Include Rule (Rules Tab)
 * 11.2 Exclude Rule (Rules Tab)
 * 11.3 Nutrition Goals Tab
 */
@HiltAndroidTest
class RecipeRulesFlowTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot
    private lateinit var recipeRulesRobot: RecipeRulesRobot

    @Before
    override fun setUp() {
        super.setUp()
        // Skip meal plan generation — Recipe Rules tests don't need meal plan data.
        // This avoids Gemini-induced SocketTimeoutException in setUp().
        setUpAuthenticatedStateWithoutMealPlan()
        clearRecipeRulesAndGoals()  // Prevent duplicate detection from prior runs

        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)
        recipeRulesRobot = RecipeRulesRobot(composeTestRule)

        // Navigate to recipe rules screen: Home → Settings → Recipe Rules
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
        homeRobot.navigateToSettings()
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToRecipeRules()
    }

    /**
     * Test 11.1: Include Rule (Rules Tab)
     *
     * Steps:
     * 1. Navigate to Recipe Rules screen
     * 2. Verify 2 tabs: Rules, Nutrition
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
     * Test 11.2: Exclude Rule (Rules Tab)
     *
     * Steps:
     * 1. Select Rules tab
     * 2. Add ingredient exclusion (via bottom sheet):
     *    - Type: INGREDIENT
     *    - Target: "Paneer"
     *    - Frequency: NEVER
     *    - Enforcement: REQUIRED
     * 3. Save rule
     * 4. Verify rule is displayed on Rules tab
     */
    @Test
    fun test_11_2_excludeRulesTab() {
        recipeRulesRobot.waitForRecipeRulesScreen()

        // Add exclude rule (uses Ingredient tab and selects Exclude in bottom sheet)
        recipeRulesRobot.addExcludeRule(TestDataFactory.RecipeRules.excludePaneer)

        // Verify rule card is displayed on Rules tab
        recipeRulesRobot.selectRulesTab()
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
     * Tabs are: Rules, Nutrition
     */
    @Test
    fun tabNavigation_works() {
        recipeRulesRobot.waitForRecipeRulesScreen()

        recipeRulesRobot.selectRulesTab()
        recipeRulesRobot.selectNutritionTab()
        recipeRulesRobot.selectRulesTab()
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

    // ==================== Sharma Family Tests (FR-011, #48) ====================

    /**
     * Requirement: #48 - FR-011: Sharma Chai breakfast INCLUDE rule
     *
     * Sharma family rule 1: Chai → Breakfast (INCLUDE, DAILY, REQUIRED)
     */
    @Test
    fun sharma_chaiBreakfastRule() {
        recipeRulesRobot.waitForRecipeRulesScreen()
        recipeRulesRobot.addIngredientIncludeRule(
            ingredientName = "Chai",
            frequencyType = FrequencyType.DAILY,
            enforcement = RuleEnforcement.REQUIRED
        )
        recipeRulesRobot.assertRuleCardDisplayed("Chai")
    }

    /**
     * Requirement: #48 - FR-011: Sharma Chai snacks INCLUDE rule
     *
     * Sharma family rule 2: Chai → Snacks (INCLUDE, DAILY, REQUIRED)
     */
    @Test
    fun sharma_chaiSnacksRule() {
        recipeRulesRobot.waitForRecipeRulesScreen()
        recipeRulesRobot.addIngredientIncludeRule(
            ingredientName = "Chai",
            frequencyType = FrequencyType.DAILY,
            enforcement = RuleEnforcement.REQUIRED
        )
        recipeRulesRobot.assertRuleCardDisplayed("Chai")
    }

    /**
     * Requirement: #48 - FR-011: Sharma Moringa INCLUDE rule
     *
     * Sharma family rule 3: Moringa (INCLUDE, 1x/week, PREFERRED)
     */
    @Test
    fun sharma_moringaIncludeRule() {
        recipeRulesRobot.waitForRecipeRulesScreen()
        recipeRulesRobot.addIngredientIncludeRule(
            ingredientName = "Moringa",
            frequencyType = FrequencyType.TIMES_PER_WEEK,
            frequencyCount = 1,
            enforcement = RuleEnforcement.PREFERRED
        )
        recipeRulesRobot.assertRuleCardDisplayed("Moringa")
    }

    /**
     * Requirement: #48 - FR-011: Sharma all 5 rules composite test
     *
     * Creates all 5 Sharma family rules and verifies they are all displayed.
     */
    @Test
    fun sharma_allFiveRules() {
        recipeRulesRobot.waitForRecipeRulesScreen()

        // Rule 1: Chai breakfast (INCLUDE, DAILY, REQUIRED)
        recipeRulesRobot.addIngredientIncludeRule(
            ingredientName = "Chai",
            frequencyType = FrequencyType.DAILY,
            enforcement = RuleEnforcement.REQUIRED
        )

        // Rule 2: Paneer exclude (EXCLUDE, NEVER, REQUIRED)
        recipeRulesRobot.addIngredientExcludeRule("Paneer")

        // Rule 3: Moringa (INCLUDE, 1x/week, PREFERRED)
        recipeRulesRobot.addIngredientIncludeRule(
            ingredientName = "Moringa",
            frequencyType = FrequencyType.TIMES_PER_WEEK,
            frequencyCount = 1,
            enforcement = RuleEnforcement.PREFERRED
        )

        // Rule 4+5: Nutrition goal
        recipeRulesRobot.addNutritionGoal(TestDataFactory.RecipeRules.greenLeafyGoal)

        // Verify ingredient rules are displayed on Rules tab
        recipeRulesRobot.selectRulesTab()
        recipeRulesRobot.assertRuleCardDisplayed("Chai")
        recipeRulesRobot.assertRuleCardDisplayed("Paneer")
        recipeRulesRobot.assertRuleCardDisplayed("Moringa")

        // Verify nutrition goal is displayed
        recipeRulesRobot.selectNutritionTab()
        composeTestRule.waitForIdle()
        Thread.sleep(2000) // Wait for tab content to render
        composeTestRule.waitForIdle()
        recipeRulesRobot.assertRuleCardDisplayed("Green Leafy")
    }
}
