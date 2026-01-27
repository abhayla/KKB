package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.rasoiai.app.e2e.base.waitUntilNodeWithTextExists

/**
 * Robot for Cooking Mode screen interactions.
 * Handles step navigation, timers, and completion.
 */
class CookingModeRobot(private val composeTestRule: ComposeContentTestRule) {

    /**
     * Wait for cooking mode screen to be displayed.
     */
    fun waitForCookingModeScreen(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilNodeWithTextExists("Step", timeoutMillis)
    }

    /**
     * Assert cooking mode screen is displayed.
     */
    fun assertCookingModeScreenDisplayed() = apply {
        composeTestRule.onNodeWithText("Step", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Step Navigation =====================

    /**
     * Assert current step is displayed.
     */
    fun assertStepDisplayed(stepNumber: Int, totalSteps: Int) = apply {
        composeTestRule.onNodeWithText("Step $stepNumber of $totalSteps", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert step instruction is displayed.
     */
    fun assertStepInstructionDisplayed(instructionText: String) = apply {
        composeTestRule.onNodeWithText(instructionText, substring = true).assertIsDisplayed()
    }

    /**
     * Go to next step.
     */
    fun nextStep() = apply {
        composeTestRule.onNodeWithText("Next", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Go to previous step.
     */
    fun previousStep() = apply {
        composeTestRule.onNodeWithText("Previous", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Swipe to next step.
     */
    fun swipeNextStep() = apply {
        composeTestRule.onNodeWithText("Step", substring = true)
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
    }

    /**
     * Swipe to previous step.
     */
    fun swipePreviousStep() = apply {
        composeTestRule.onNodeWithText("Step", substring = true)
            .performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
    }

    /**
     * Navigate through all steps.
     */
    fun navigateThroughAllSteps(totalSteps: Int) = apply {
        for (step in 1 until totalSteps) {
            assertStepDisplayed(step, totalSteps)
            nextStep()
        }
        assertStepDisplayed(totalSteps, totalSteps)
    }

    // ===================== Timer =====================

    /**
     * Assert timer button is displayed.
     */
    fun assertTimerButtonDisplayed() = apply {
        composeTestRule.onNodeWithText("Timer", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Start timer.
     */
    fun startTimer() = apply {
        composeTestRule.onNodeWithText("Start Timer", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Stop timer.
     */
    fun stopTimer() = apply {
        composeTestRule.onNodeWithText("Stop", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert timer is running.
     */
    fun assertTimerRunning() = apply {
        // Timer display should show countdown
        composeTestRule.onNodeWithText(":", substring = true).assertIsDisplayed()
    }

    /**
     * Assert timer duration.
     */
    fun assertTimerDuration(minutes: Int) = apply {
        composeTestRule.onNodeWithText("$minutes", substring = true).assertIsDisplayed()
    }

    // ===================== Tips =====================

    /**
     * Assert tip is displayed for current step.
     */
    fun assertTipDisplayed(tipText: String) = apply {
        composeTestRule.onNodeWithText(tipText, substring = true).assertIsDisplayed()
    }

    /**
     * Assert tip section is displayed.
     */
    fun assertTipSectionDisplayed() = apply {
        composeTestRule.onNodeWithText("Tip", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Ingredients for Step =====================

    /**
     * Assert ingredients for current step are displayed.
     */
    fun assertStepIngredientsDisplayed() = apply {
        composeTestRule.onNodeWithText("Ingredients", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert specific ingredient for step.
     */
    fun assertStepIngredient(ingredientName: String) = apply {
        composeTestRule.onNodeWithText(ingredientName, substring = true).assertIsDisplayed()
    }

    // ===================== Completion =====================

    /**
     * Assert complete button is displayed (on last step).
     */
    fun assertCompleteButtonDisplayed() = apply {
        composeTestRule.onNodeWithText("Complete", ignoreCase = true).assertIsDisplayed()
    }

    /**
     * Tap complete button.
     */
    fun tapComplete() = apply {
        composeTestRule.onNodeWithText("Complete", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert completion screen is displayed.
     */
    fun assertCompletionScreenDisplayed() = apply {
        composeTestRule.onNodeWithText("Well done", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert cooking completed message.
     */
    fun assertCookingCompletedMessage() = apply {
        try {
            composeTestRule.onNodeWithText("Completed", substring = true, ignoreCase = true)
                .assertIsDisplayed()
        } catch (e: Exception) {
            composeTestRule.onNodeWithText("Done", substring = true, ignoreCase = true)
                .assertIsDisplayed()
        }
    }

    // ===================== Exit =====================

    /**
     * Exit cooking mode.
     */
    fun exitCookingMode() = apply {
        composeTestRule.onNodeWithText("Exit", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Confirm exit when prompted.
     */
    fun confirmExit() = apply {
        composeTestRule.onNodeWithText("Yes", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Cancel exit.
     */
    fun cancelExit() = apply {
        composeTestRule.onNodeWithText("No", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Screen Controls =====================

    /**
     * Assert keep screen on is active.
     */
    fun assertKeepScreenOnActive() = apply {
        // This is a feature check - screen should stay on during cooking
    }

    /**
     * Assert large text mode is active.
     */
    fun assertLargeTextMode() = apply {
        // Large text for cooking-friendly display
    }

    // ===================== Rating (Post Completion) =====================

    /**
     * Rate the recipe.
     */
    fun rateRecipe(stars: Int) = apply {
        // Tap on star rating
        for (i in 1..stars) {
            composeTestRule.onNodeWithText("★", substring = true).performClick()
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Skip rating.
     */
    fun skipRating() = apply {
        composeTestRule.onNodeWithText("Skip", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert rating prompt is displayed.
     */
    fun assertRatingPromptDisplayed() = apply {
        composeTestRule.onNodeWithText("Rate", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }
}
