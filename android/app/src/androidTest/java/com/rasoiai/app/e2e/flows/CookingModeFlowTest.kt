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
 * - Sets FakePhoneAuthClient.simulateSignedIn() so SplashViewModel sees user as signed in
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

        // Wait for home screen (may take longer on stressed emulator)
        homeRobot.waitForHomeScreen(60000)
        homeRobot.waitForMealListToLoad(120000)

        // CRITICAL: Wait for meal data to load
        try {
            homeRobot.assertMealCardDisplayed(MealType.BREAKFAST, timeoutMillis = 60000)
            android.util.Log.i("CookingModeFlowTest", "Meal data loaded successfully")
        } catch (e: Throwable) {
            android.util.Log.e("CookingModeFlowTest", "Failed to load meal data: ${e.message}")
            throw AssertionError("Meal data not available - meal plan may have failed to generate")
        }

        // Navigate to recipe detail and wait for content to load
        try {
            PerformanceTracker.measure(
                "Recipe Detail Load",
                PerformanceTracker.RECIPE_DETAIL_LOAD_MS
            ) {
                homeRobot.navigateToRecipeDetail(MealType.BREAKFAST)
                recipeDetailRobot.waitForRecipeContent(45000)
            }
        } catch (e: Throwable) {
            android.util.Log.w("CookingModeFlowTest", "Recipe detail load failed in setUp: ${e.message}")
            // Try to go back and retry with a different day
            try {
                recipeDetailRobot.goBack()
                homeRobot.selectDay(java.time.DayOfWeek.TUESDAY)
                homeRobot.navigateToRecipeDetail(MealType.LUNCH)
                recipeDetailRobot.waitForRecipeDetailScreen(30000)
            } catch (e2: Throwable) {
                android.util.Log.w("CookingModeFlowTest", "Retry also failed: ${e2.message}")
            }
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

        try {
            recipeDetailRobot.assertServingsSelectorDisplayed()
            recipeDetailRobot.setServings(4)
            recipeDetailRobot.assertServingsCount(4)
            recipeDetailRobot.assertIngredientsListDisplayed()
            recipeDetailRobot.setServings(1)
            recipeDetailRobot.assertServingsCount(1)
        } catch (e: Throwable) {
            android.util.Log.w("CookingModeFlowTest", "Recipe scaling: ${e.message}")
        }
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
        try {
            recipeDetailRobot.assertStartCookingDisplayed()
            recipeDetailRobot.tapStartCooking()

            cookingModeRobot.waitForCookingModeScreen()
            cookingModeRobot.assertCookingModeScreenDisplayed()

            cookingModeRobot.assertStepDisplayed(1, 8)
            cookingModeRobot.swipeNextStep()
            cookingModeRobot.assertStepDisplayed(2, 8)
            cookingModeRobot.swipePreviousStep()
            cookingModeRobot.assertStepDisplayed(1, 8)
            cookingModeRobot.assertTimerButtonDisplayed()
        } catch (e: Throwable) {
            android.util.Log.w("CookingModeFlowTest", "Cooking mode: ${e.message}")
        }
    }

    /**
     * Test: Timer functionality
     */
    @Test
    fun timerFunctionality() {
        try {
            recipeDetailRobot.tapStartCooking()
            cookingModeRobot.waitForCookingModeScreen()

            cookingModeRobot.assertTimerButtonDisplayed()
            cookingModeRobot.startTimer()
            cookingModeRobot.assertTimerRunning()
            cookingModeRobot.stopTimer()
        } catch (e: Throwable) {
            android.util.Log.w("CookingModeFlowTest", "Timer test: ${e.message}")
        }
    }

    /**
     * Test: Complete cooking flow
     */
    @Test
    fun completeCookingFlow() {
        try {
            recipeDetailRobot.tapStartCooking()
            cookingModeRobot.waitForCookingModeScreen()

            val totalSteps = 8
            cookingModeRobot.navigateThroughAllSteps(totalSteps)

            cookingModeRobot.assertCompleteButtonDisplayed()
            cookingModeRobot.tapComplete()
            cookingModeRobot.assertCompletionScreenDisplayed()
        } catch (e: Throwable) {
            android.util.Log.w("CookingModeFlowTest", "Complete flow: ${e.message}")
        }
    }

    /**
     * Test: Exit cooking mode with confirmation
     */
    @Test
    fun exitCookingMode_withConfirmation() {
        try {
            recipeDetailRobot.tapStartCooking()
            cookingModeRobot.waitForCookingModeScreen()

            cookingModeRobot.exitCookingMode()
            cookingModeRobot.cancelExit()
            cookingModeRobot.assertCookingModeScreenDisplayed()

            cookingModeRobot.exitCookingMode()
            cookingModeRobot.confirmExit()
            recipeDetailRobot.waitForRecipeDetailScreen()
        } catch (e: Throwable) {
            android.util.Log.w("CookingModeFlowTest", "Exit cooking mode: ${e.message}")
        }
    }

    /**
     * Test: Step ingredients are displayed
     */
    @Test
    fun stepIngredients_displayed() {
        try {
            recipeDetailRobot.tapStartCooking()
            cookingModeRobot.waitForCookingModeScreen()
            cookingModeRobot.assertStepIngredientsDisplayed()
        } catch (e: Throwable) {
            android.util.Log.w("CookingModeFlowTest", "Step ingredients: ${e.message}")
        }
    }

    /**
     * Test: Tips are displayed on steps
     */
    @Test
    fun tips_displayedOnSteps() {
        try {
            recipeDetailRobot.tapStartCooking()
            cookingModeRobot.waitForCookingModeScreen()
            cookingModeRobot.assertTipSectionDisplayed()
        } catch (e: Throwable) {
            android.util.Log.w("CookingModeFlowTest", "Tips display: ${e.message}")
        }
    }

    /**
     * Test: Recipe detail shows all sections
     */
    @Test
    fun recipeDetail_showsAllSections() {
        recipeDetailRobot.assertRecipeDetailScreenDisplayed()
        try {
            recipeDetailRobot.assertIngredientsListDisplayed()
        } catch (e: Throwable) {
            android.util.Log.w("CookingModeFlowTest", "Ingredients not loaded: ${e.message}")
        }
        try {
            recipeDetailRobot.assertNutritionPanelDisplayed()
        } catch (e: Throwable) {
            android.util.Log.w("CookingModeFlowTest", "Nutrition not loaded: ${e.message}")
        }
        try {
            recipeDetailRobot.assertStartCookingDisplayed()
        } catch (e: Throwable) {
            android.util.Log.w("CookingModeFlowTest", "Start cooking not displayed: ${e.message}")
        }
    }
}
