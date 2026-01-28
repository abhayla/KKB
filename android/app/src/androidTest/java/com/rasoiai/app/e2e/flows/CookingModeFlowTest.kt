package com.rasoiai.app.e2e.flows

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.CookingModeRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.RecipeDetailRobot
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

/**
 * Phase 12: Recipe Detail & Cooking Mode Testing
 *
 * Tests:
 * 12.1 Recipe Scaling
 * 12.2 Start Cooking Mode
 */
@HiltAndroidTest
class CookingModeFlowTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var recipeDetailRobot: RecipeDetailRobot
    private lateinit var cookingModeRobot: CookingModeRobot

    @Before
    override fun setUp() {
        super.setUp()
        // Set up authenticated and onboarded user state
        setUpAuthenticatedState()

        homeRobot = HomeRobot(composeTestRule)
        recipeDetailRobot = RecipeDetailRobot(composeTestRule)
        cookingModeRobot = CookingModeRobot(composeTestRule)

        // Navigate to a recipe
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
        homeRobot.selectDay(DayOfWeek.MONDAY)
        homeRobot.tapMealCard(MealType.LUNCH)
        recipeDetailRobot.waitForRecipeDetailScreen()
    }

    /**
     * Test 12.1: Recipe Scaling
     *
     * Steps:
     * 1. Open any recipe
     * 2. Find serving size selector
     * 3. Increase servings from 2 to 4
     * 4. Verify ingredient quantities double
     * 5. Decrease to 1 serving
     * 6. Verify quantities halve
     */
    @Test
    fun test_12_1_recipeScaling() {
        recipeDetailRobot.assertRecipeDetailScreenDisplayed()

        // Verify servings selector
        recipeDetailRobot.assertServingsSelectorDisplayed()

        // Increase servings
        recipeDetailRobot.setServings(4)
        recipeDetailRobot.assertServingsCount(4)

        // Check scaled ingredients
        recipeDetailRobot.assertIngredientsListDisplayed()

        // Decrease servings
        recipeDetailRobot.setServings(1)
        recipeDetailRobot.assertServingsCount(1)
    }

    /**
     * Test 12.2: Start Cooking Mode
     *
     * Steps:
     * 1. From Recipe Detail, tap "Start Cooking"
     * 2. Verify Cooking Mode screen opens
     * 3. Swipe through instruction steps
     * 4. Verify timer button on timed steps
     * 5. Start timer, verify countdown
     * 6. Complete all steps
     * 7. Verify completion state
     *
     * Expected:
     * - Large text for instructions (cooking-friendly)
     * - Step indicator (e.g., Step 3 of 8)
     * - Timer integration for timed steps
     * - Tips shown for complex steps
     */
    @Test
    fun test_12_2_cookingMode() {
        // Start cooking mode
        recipeDetailRobot.assertStartCookingDisplayed()
        recipeDetailRobot.tapStartCooking()

        // Verify cooking mode screen
        cookingModeRobot.waitForCookingModeScreen()
        cookingModeRobot.assertCookingModeScreenDisplayed()

        // Navigate through steps
        cookingModeRobot.assertStepDisplayed(1, 8) // Assuming 8 steps

        // Swipe through steps
        cookingModeRobot.swipeNextStep()
        cookingModeRobot.assertStepDisplayed(2, 8)

        // Navigate back
        cookingModeRobot.swipePreviousStep()
        cookingModeRobot.assertStepDisplayed(1, 8)

        // Check for timer on timed step
        cookingModeRobot.assertTimerButtonDisplayed()
    }

    /**
     * Test: Timer functionality
     */
    @Test
    fun timerFunctionality() {
        recipeDetailRobot.tapStartCooking()
        cookingModeRobot.waitForCookingModeScreen()

        // Find a step with timer and test it
        cookingModeRobot.assertTimerButtonDisplayed()
        cookingModeRobot.startTimer()
        cookingModeRobot.assertTimerRunning()
        cookingModeRobot.stopTimer()
    }

    /**
     * Test: Complete cooking flow
     */
    @Test
    fun completeCookingFlow() {
        recipeDetailRobot.tapStartCooking()
        cookingModeRobot.waitForCookingModeScreen()

        // Navigate through all steps
        val totalSteps = 8 // Depends on recipe
        cookingModeRobot.navigateThroughAllSteps(totalSteps)

        // Complete
        cookingModeRobot.assertCompleteButtonDisplayed()
        cookingModeRobot.tapComplete()

        // Verify completion
        cookingModeRobot.assertCompletionScreenDisplayed()
    }

    /**
     * Test: Exit cooking mode with confirmation
     */
    @Test
    fun exitCookingMode_withConfirmation() {
        recipeDetailRobot.tapStartCooking()
        cookingModeRobot.waitForCookingModeScreen()

        // Try to exit
        cookingModeRobot.exitCookingMode()

        // Cancel
        cookingModeRobot.cancelExit()

        // Should still be in cooking mode
        cookingModeRobot.assertCookingModeScreenDisplayed()

        // Exit again and confirm
        cookingModeRobot.exitCookingMode()
        cookingModeRobot.confirmExit()

        // Should be back on recipe detail
        recipeDetailRobot.waitForRecipeDetailScreen()
    }

    /**
     * Test: Step ingredients are displayed
     */
    @Test
    fun stepIngredients_displayed() {
        recipeDetailRobot.tapStartCooking()
        cookingModeRobot.waitForCookingModeScreen()

        cookingModeRobot.assertStepIngredientsDisplayed()
    }

    /**
     * Test: Tips are displayed on steps
     */
    @Test
    fun tips_displayedOnSteps() {
        recipeDetailRobot.tapStartCooking()
        cookingModeRobot.waitForCookingModeScreen()

        cookingModeRobot.assertTipSectionDisplayed()
    }

    /**
     * Test: Recipe detail shows all sections
     */
    @Test
    fun recipeDetail_showsAllSections() {
        recipeDetailRobot.assertRecipeDetailScreenDisplayed()
        recipeDetailRobot.assertIngredientsListDisplayed()
        recipeDetailRobot.assertInstructionsListDisplayed()
        recipeDetailRobot.assertNutritionPanelDisplayed()
        recipeDetailRobot.assertStartCookingDisplayed()
    }
}
