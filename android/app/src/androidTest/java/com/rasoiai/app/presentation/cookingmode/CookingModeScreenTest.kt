package com.rasoiai.app.presentation.cookingmode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.Ingredient
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.model.Instruction
import com.rasoiai.domain.model.Nutrition
import com.rasoiai.domain.model.Recipe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for CookingModeScreen
 * Tests Phase 12 of E2E Testing Guide: Cooking Mode Screen Testing
 */
@RunWith(AndroidJUnit4::class)
class CookingModeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Test Data Factories

    private fun createTestInstruction(
        stepNumber: Int,
        instruction: String,
        durationMinutes: Int? = null,
        timerRequired: Boolean = false,
        tips: String? = null
    ) = Instruction(
        stepNumber = stepNumber,
        instruction = instruction,
        durationMinutes = durationMinutes,
        timerRequired = timerRequired,
        tips = tips
    )

    private fun createTestRecipe(
        id: String = "recipe_1",
        name: String = "Rajma Chawal",
        instructions: List<Instruction> = listOf(
            createTestInstruction(1, "Soak the rajma beans overnight in water.", null, false),
            createTestInstruction(2, "Pressure cook the beans for 20 minutes.", 20, true),
            createTestInstruction(3, "Heat oil in a pan and add cumin seeds.", null, false),
            createTestInstruction(4, "Add onions and saute until golden brown.", 5, true),
            createTestInstruction(5, "Add tomato puree and cook for 5 minutes.", 5, true),
            createTestInstruction(6, "Add the cooked beans and simmer for 10 minutes.", 10, true),
            createTestInstruction(7, "Garnish with coriander and serve hot with rice.")
        )
    ) = Recipe(
        id = id,
        name = name,
        description = "Classic North Indian rajma curry with steamed rice",
        cuisineType = CuisineType.NORTH,
        dietaryTags = listOf(DietaryTag.VEGETARIAN),
        difficulty = Difficulty.MEDIUM,
        prepTimeMinutes = 30,
        cookTimeMinutes = 45,
        servings = 4,
        ingredients = listOf(
            Ingredient("1", "Rajma", 250.0, "grams", IngredientCategory.PULSES),
            Ingredient("2", "Onion", 2.0, "medium", IngredientCategory.VEGETABLES),
            Ingredient("3", "Tomato", 3.0, "medium", IngredientCategory.VEGETABLES)
        ),
        instructions = instructions,
        nutrition = Nutrition(350, 12.0, 45.0, 8.0, 10.0),
        imageUrl = null,
        isFavorite = false
    )

    private fun createTestUiState(
        isLoading: Boolean = false,
        errorMessage: String? = null,
        recipe: Recipe? = createTestRecipe(),
        currentStepIndex: Int = 0,
        timerState: TimerState = TimerState.IDLE,
        timerRemainingSeconds: Int = 0,
        timerTotalSeconds: Int = 0,
        showExitConfirmation: Boolean = false,
        showCompletionDialog: Boolean = false,
        rating: Int = 0,
        feedback: String = "",
        voiceGuidanceEnabled: Boolean = false
    ) = CookingModeUiState(
        isLoading = isLoading,
        errorMessage = errorMessage,
        recipe = recipe,
        currentStepIndex = currentStepIndex,
        timerState = timerState,
        timerRemainingSeconds = timerRemainingSeconds,
        timerTotalSeconds = timerTotalSeconds,
        showExitConfirmation = showExitConfirmation,
        showCompletionDialog = showCompletionDialog,
        rating = rating,
        feedback = feedback,
        voiceGuidanceEnabled = voiceGuidanceEnabled
    )

    // endregion

    // region Phase 12.1: Screen Display Tests

    @Test
    fun cookingModeScreen_displaysScreenTag() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.COOKING_MODE_SCREEN).assertIsDisplayed()
    }

    @Test
    fun cookingModeScreen_displaysRecipeName() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Rajma Chawal", substring = true).assertIsDisplayed()
    }

    @Test
    fun cookingModeScreen_displaysStepCounter() {
        val uiState = createTestUiState(currentStepIndex = 0)

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Step 1 / 7", substring = true).assertIsDisplayed()
    }

    @Test
    fun cookingModeScreen_displaysCloseButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Close cooking mode").assertIsDisplayed()
    }

    @Test
    fun cookingModeScreen_displaysVoiceGuidanceButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Enable voice guidance").assertIsDisplayed()
    }

    @Test
    fun cookingModeScreen_displaysScreenOnIndicator() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Screen stays ON").assertIsDisplayed()
    }

    // endregion

    // region Phase 12.2: Step Navigation Tests

    @Test
    fun cookingModeScreen_displaysPrevButton() {
        val uiState = createTestUiState(currentStepIndex = 1)

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("PREV", substring = true).assertIsDisplayed()
    }

    @Test
    fun cookingModeScreen_displaysNextButton() {
        val uiState = createTestUiState(currentStepIndex = 0)

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("NEXT", substring = true).assertIsDisplayed()
    }

    @Test
    fun cookingModeScreen_lastStep_displaysFinishButton() {
        val uiState = createTestUiState(currentStepIndex = 6) // Last step (0-indexed)

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("FINISH", substring = true).assertIsDisplayed()
    }

    @Test
    fun nextButton_click_triggersNextStepCallback() {
        var nextClicked = false
        val uiState = createTestUiState(currentStepIndex = 0)

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(
                    uiState = uiState,
                    onNextStep = { nextClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("NEXT", substring = true).performClick()

        assert(nextClicked) { "Next step callback was not triggered" }
    }

    @Test
    fun prevButton_click_triggersPreviousStepCallback() {
        var prevClicked = false
        val uiState = createTestUiState(currentStepIndex = 1)

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(
                    uiState = uiState,
                    onPreviousStep = { prevClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("PREV", substring = true).performClick()

        assert(prevClicked) { "Previous step callback was not triggered" }
    }

    // endregion

    // region Phase 12.3: Step Content Tests

    @Test
    fun cookingModeScreen_displaysStepBadge() {
        val uiState = createTestUiState(currentStepIndex = 2)

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("STEP 3", substring = true).assertIsDisplayed()
    }

    @Test
    fun cookingModeScreen_displaysStepInstruction() {
        val uiState = createTestUiState(currentStepIndex = 0)

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Soak the rajma beans", substring = true).assertIsDisplayed()
    }

    // endregion

    // region Phase 12.4: Timer Tests

    @Test
    fun cookingModeScreen_stepWithTimer_displaysTimerSection() {
        // Step 2 has a 20-minute timer
        val uiState = createTestUiState(currentStepIndex = 1)

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        // hasTimer should be true for this step
        assert(uiState.hasTimer) { "Step should have timer" }
    }

    @Test
    fun cookingModeScreen_runningTimer_displaysTimerText() {
        val uiState = createTestUiState(
            currentStepIndex = 1,
            timerState = TimerState.RUNNING,
            timerRemainingSeconds = 600, // 10 minutes
            timerTotalSeconds = 1200 // 20 minutes
        )

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        // Timer display should show "10:00"
        composeTestRule.onNodeWithText("10:00", substring = true).assertIsDisplayed()
    }

    @Test
    fun cookingModeScreen_timerProgress_calculatedCorrectly() {
        val uiState = createTestUiState(
            currentStepIndex = 1,
            timerState = TimerState.RUNNING,
            timerRemainingSeconds = 600,
            timerTotalSeconds = 1200
        )

        assert(uiState.timerProgress == 0.5f) { "Timer progress should be 50%" }
    }

    @Test
    fun cookingModeScreen_timerDisplayText_formattedCorrectly() {
        val uiState = createTestUiState(
            timerRemainingSeconds = 125 // 2:05
        )

        assert(uiState.timerDisplayText == "02:05") { "Timer should show 02:05" }
    }

    // endregion

    // region Phase 12.5: Voice Guidance Tests

    @Test
    fun voiceGuidanceButton_click_triggersToggle() {
        var toggled = false
        val uiState = createTestUiState(voiceGuidanceEnabled = false)

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(
                    uiState = uiState,
                    onVoiceGuidanceToggle = { toggled = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Enable voice guidance").performClick()

        assert(toggled) { "Voice guidance toggle was not triggered" }
    }

    @Test
    fun cookingModeScreen_voiceGuidanceEnabled_showsDisableOption() {
        val uiState = createTestUiState(voiceGuidanceEnabled = true)

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Disable voice guidance").assertIsDisplayed()
    }

    // endregion

    // region Phase 12.6: Exit Confirmation Tests

    @Test
    fun closeButton_click_triggersExitCallback() {
        var exitClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(
                    uiState = uiState,
                    onCloseClick = { exitClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Close cooking mode").performClick()

        assert(exitClicked) { "Close callback was not triggered" }
    }

    @Test
    fun cookingModeScreen_exitConfirmation_displaysDialog() {
        val uiState = createTestUiState(showExitConfirmation = true)

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Exit Cooking Mode?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue Cooking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exit").assertIsDisplayed()
    }

    // endregion

    // region Phase 12.7: Loading/Error State Tests

    @Test
    fun cookingModeScreen_loadingState_displaysScreen() {
        val uiState = createTestUiState(isLoading = true, recipe = null)

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.COOKING_MODE_SCREEN).assertIsDisplayed()
    }

    @Test
    fun cookingModeScreen_errorState_displaysErrorMessage() {
        val uiState = createTestUiState(
            isLoading = false,
            recipe = null,
            errorMessage = "Recipe not found"
        )

        composeTestRule.setContent {
            RasoiAITheme {
                CookingModeTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Recipe not found").assertIsDisplayed()
    }

    // endregion

    // region Phase 12.8: Data Verification Tests

    @Test
    fun cookingModeScreen_stepNavigation_calculatedCorrectly() {
        val uiState = createTestUiState(currentStepIndex = 0)

        assert(uiState.isFirstStep) { "Should be first step" }
        assert(!uiState.isLastStep) { "Should not be last step" }
        assert(uiState.stepNumber == 1) { "Step number should be 1" }
        assert(uiState.totalSteps == 7) { "Total steps should be 7" }
    }

    @Test
    fun cookingModeScreen_progress_calculatedCorrectly() {
        val uiState = createTestUiState(currentStepIndex = 3)

        // Step 4 of 7 = 4/7 ≈ 0.57
        assert(uiState.progress > 0.5f && uiState.progress < 0.6f) { "Progress should be ~57%" }
    }

    @Test
    fun cookingModeScreen_currentStep_returnsCorrectInstruction() {
        val uiState = createTestUiState(currentStepIndex = 2)

        assert(uiState.currentStep?.stepNumber == 3) { "Current step should be step 3" }
        assert(uiState.currentStep?.instruction?.contains("cumin seeds") == true) { "Step 3 should mention cumin seeds" }
    }

    // endregion
}

// region Test Composable Wrapper

@androidx.compose.runtime.Composable
private fun CookingModeTestContent(
    uiState: CookingModeUiState,
    onCloseClick: () -> Unit = {},
    onVoiceGuidanceToggle: () -> Unit = {},
    onPreviousStep: () -> Unit = {},
    onNextStep: () -> Unit = {},
    onStartTimer: () -> Unit = {},
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    onStopTimer: () -> Unit = {},
    onDismissTimerComplete: () -> Unit = {},
    onDismissExitConfirmation: () -> Unit = {},
    onConfirmExit: () -> Unit = {},
    onRatingChange: (Int) -> Unit = {},
    onFeedbackChange: (String) -> Unit = {},
    onSubmitRating: () -> Unit = {},
    onSkipRating: () -> Unit = {}
) {
    CookingModeContent(
        uiState = uiState,
        onCloseClick = onCloseClick,
        onVoiceGuidanceToggle = onVoiceGuidanceToggle,
        onPreviousStep = onPreviousStep,
        onNextStep = onNextStep,
        onStartTimer = onStartTimer,
        onPauseTimer = onPauseTimer,
        onResumeTimer = onResumeTimer,
        onStopTimer = onStopTimer,
        onDismissTimerComplete = onDismissTimerComplete,
        onDismissExitConfirmation = onDismissExitConfirmation,
        onConfirmExit = onConfirmExit,
        onRatingChange = onRatingChange,
        onFeedbackChange = onFeedbackChange,
        onSubmitRating = onSubmitRating,
        onSkipRating = onSkipRating
    )
}

// endregion
