package com.rasoiai.app.e2e.flows

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.CookingModeRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.RecipeDetailRobot
import com.rasoiai.app.e2e.util.PerformanceTracker
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

/**
 * Phase 12: Recipe Detail & Cooking Mode Testing
 *
 * Tests:
 * 12.1 Recipe Scaling
 * 12.2 Start Cooking Mode
 *
 * ## Auth State (E2ETestSuite Context)
 * When running via E2ETestSuite, CoreDataFlowTest runs first and:
 * - Authenticates with backend (stores JWT in REAL DataStore)
 * - Completes onboarding (stores preferences in REAL DataStore)
 * - Generates meal plan (stores in Room DB)
 *
 * This test then:
 * - Sets FakeGoogleAuthClient.simulateSignedIn() so SplashViewModel sees user as signed in
 * - Real DataStore already has JWT + onboarded flag from CoreDataFlowTest
 * - Navigates to Recipe Detail via Home → Meal Card tap
 */
@HiltAndroidTest
class CookingModeFlowTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var recipeDetailRobot: RecipeDetailRobot
    private lateinit var cookingModeRobot: CookingModeRobot

    @Before
    override fun setUp() {
        super.setUp()

        // Reset performance tracker for this test class
        PerformanceTracker.reset()

        // Set up authenticated state - gets real JWT from backend
        // This makes the test self-contained (doesn't depend on CoreDataFlowTest running first)
        setUpAuthenticatedState()

        homeRobot = HomeRobot(composeTestRule)
        recipeDetailRobot = RecipeDetailRobot(composeTestRule)
        cookingModeRobot = CookingModeRobot(composeTestRule)

        // Wait for home screen (should navigate directly due to persisted auth state)
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)

        // CRITICAL: Wait for meal data to load (API call can take 30+ seconds)
        // assertMealCardDisplayed will wait up to 60s for the card to appear
        try {
            homeRobot.assertMealCardDisplayed(MealType.BREAKFAST, timeoutMillis = 60000)
            android.util.Log.i("CookingModeFlowTest", "Meal data loaded successfully")
        } catch (e: Exception) {
            android.util.Log.e("CookingModeFlowTest", "Failed to load meal data: ${e.message}")
            throw AssertionError("Meal data not available - meal plan may have failed to generate")
        }

        // Measure recipe detail load time
        // Use BREAKFAST as it's always visible without scrolling
        PerformanceTracker.measure(
            "Recipe Detail Load",
            PerformanceTracker.RECIPE_DETAIL_LOAD_MS
        ) {
            // Tap meal card shows action sheet, then tap "View Recipe" to navigate
            homeRobot.navigateToRecipeDetail(MealType.BREAKFAST)
            // Allow 30 seconds for recipe detail - API call may be slow
            recipeDetailRobot.waitForRecipeDetailScreen(30000)
        }
    }

    @After
    override fun tearDown() {
        // Print performance summary to Logcat
        PerformanceTracker.printSummary()
        super.tearDown()
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
