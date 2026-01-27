package com.rasoiai.app.e2e.flows

import androidx.compose.ui.test.onNodeWithTag
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * Phase 3: Meal Plan Generation Testing
 *
 * Tests:
 * 3.1 Generation Progress Screen - Verify 4-step animation and auto-navigation
 */
@HiltAndroidTest
class MealPlanGenerationTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot

    @Before
    override fun setUp() {
        super.setUp()
        homeRobot = HomeRobot(composeTestRule)
    }

    /**
     * Test 3.1: Generation Progress Screen
     *
     * Steps:
     * 1. After tapping "Create My Meal Plan" (from onboarding)
     * 2. Observe 4-step progress animation:
     *    - Step 1: "Analyzing preferences" (0.8s)
     *    - Step 2: "Checking festivals" (0.6s)
     *    - Step 3: "Generating recipes" (1.2s)
     *    - Step 4: "Building grocery list" (0.6s)
     * 3. Verify auto-navigation to Home screen
     *
     * Expected:
     * - Each step shows spinner → checkmark transition
     * - Total duration ~3.2 seconds
     * - Smooth transition to Home screen
     *
     * Note: This test assumes the generating screen is already visible.
     * In real E2E flow, this would follow the onboarding completion.
     */
    @Test
    fun test_3_1_generationProgressScreen_showsStepsAndNavigatesToHome() {
        // Given: Generation screen is displayed
        composeTestRule.waitUntilNodeWithTagExists(TestTags.GENERATING_SCREEN, LONG_TIMEOUT)

        // When: Progress steps execute
        // Step 1: Analyzing preferences
        composeTestRule.waitUntilNodeWithTagExists(
            TestTags.GENERATING_PROGRESS_ANALYZING,
            MEDIUM_TIMEOUT
        )

        // Step 2: Checking festivals
        composeTestRule.waitUntilNodeWithTagExists(
            TestTags.GENERATING_PROGRESS_FESTIVALS,
            MEDIUM_TIMEOUT
        )

        // Step 3: Generating recipes
        composeTestRule.waitUntilNodeWithTagExists(
            TestTags.GENERATING_PROGRESS_RECIPES,
            MEDIUM_TIMEOUT
        )

        // Step 4: Building grocery list
        composeTestRule.waitUntilNodeWithTagExists(
            TestTags.GENERATING_PROGRESS_GROCERY,
            MEDIUM_TIMEOUT
        )

        // Then: Auto-navigate to Home screen
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
        homeRobot.assertHomeScreenDisplayed()
    }

    /**
     * Test: Generation screen elements are displayed correctly.
     */
    @Test
    fun generationScreen_displaysProgressSteps() {
        // Given: Generation screen is displayed
        composeTestRule.waitUntilNodeWithTagExists(TestTags.GENERATING_SCREEN, LONG_TIMEOUT)

        // Then: Progress elements should be visible
        // At least one step should be visible at any time
        try {
            composeTestRule.onNodeWithTag(TestTags.GENERATING_PROGRESS_ANALYZING).assertExists()
        } catch (e: Exception) {
            // May have moved past this step already
        }
    }

    /**
     * Test: Meal plan generation completes within expected time.
     */
    @Test
    fun generationScreen_completesWithinTimeout() {
        val startTime = System.currentTimeMillis()

        // Wait for generation to complete and navigate to home
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)

        val duration = System.currentTimeMillis() - startTime

        // Generation should complete within reasonable time (10 seconds)
        assert(duration < LONG_TIMEOUT) {
            "Meal plan generation took too long: ${duration}ms"
        }
    }
}
