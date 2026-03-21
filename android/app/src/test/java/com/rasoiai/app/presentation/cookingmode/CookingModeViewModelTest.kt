package com.rasoiai.app.presentation.cookingmode

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rasoiai.app.presentation.navigation.Screen
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.Instruction
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.repository.RecipeRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CookingModeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRecipeRepository: RecipeRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private val testRecipe = Recipe(
        id = "test-recipe-1",
        name = "Paneer Tikka",
        description = "Grilled paneer cubes with spices",
        imageUrl = null,
        prepTimeMinutes = 15,
        cookTimeMinutes = 20,
        servings = 4,
        difficulty = Difficulty.MEDIUM,
        cuisineType = CuisineType.NORTH,
        mealTypes = listOf(com.rasoiai.domain.model.MealType.SNACKS),
        dietaryTags = listOf(DietaryTag.VEGETARIAN),
        ingredients = emptyList(),
        instructions = listOf(
            Instruction(stepNumber = 1, instruction = "Cut paneer into cubes", durationMinutes = null, tips = null),
            Instruction(stepNumber = 2, instruction = "Marinate with spices", durationMinutes = 30, tips = null),
            Instruction(stepNumber = 3, instruction = "Grill until golden", durationMinutes = 10, tips = null),
            Instruction(stepNumber = 4, instruction = "Serve hot with chutney", durationMinutes = null, tips = null)
        ),
        nutrition = null,
        isFavorite = false
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRecipeRepository = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle().apply {
            set(Screen.CookingMode.ARG_RECIPE_ID, "test-recipe-1")
        }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        @DisplayName("Initial state should be loading")
        fun `initial state should be loading`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(null)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                val initialState = awaitItem()
                assertTrue(initialState.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("After loading, recipe should be populated")
        fun `after loading recipe should be populated`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertFalse(state.isLoading)
                assertNotNull(state.recipe)
                assertEquals(0, state.currentStepIndex)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("totalSteps should return correct count")
        fun `totalSteps should return correct count`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertEquals(4, state.totalSteps)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Step Navigation")
    inner class StepNavigation {

        @Test
        @DisplayName("nextStep should increment step index")
        fun `nextStep should increment step index`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.nextStep()

                val state = awaitItem()
                assertEquals(1, state.currentStepIndex)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("previousStep should decrement step index")
        fun `previousStep should decrement step index`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.nextStep()
                awaitItem() // Step 2

                viewModel.previousStep()

                val state = awaitItem()
                assertEquals(0, state.currentStepIndex)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("goToStep should navigate to specific step")
        fun `goToStep should navigate to specific step`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.goToStep(2)

                val state = awaitItem()
                assertEquals(2, state.currentStepIndex)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("isFirstStep should be true on first step")
        fun `isFirstStep should be true on first step`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertTrue(state.isFirstStep)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("isLastStep should be true on last step")
        fun `isLastStep should be true on last step`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.goToStep(3) // Last step (index 3)

                val state = awaitItem()
                assertTrue(state.isLastStep)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("nextStep on last step should show completion dialog")
        fun `nextStep on last step should show completion dialog`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.goToStep(3)
                awaitItem()

                viewModel.nextStep()

                val state = awaitItem()
                assertTrue(state.showCompletionDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Timer")
    inner class Timer {

        @Test
        @DisplayName("hasTimer should be true for step with duration")
        fun `hasTimer should be true for step with duration`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.goToStep(1) // Step with 30 min duration

                val state = awaitItem()
                assertTrue(state.hasTimer)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("hasTimer should be false for step without duration")
        fun `hasTimer should be false for step without duration`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem() // First step (no duration)
                assertFalse(state.hasTimer)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("startTimer should set timer to running state")
        fun `startTimer should set timer to running state`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.goToStep(1) // Step with 30 min duration
                awaitItem()

                viewModel.startTimer()

                val state = awaitItem()
                assertEquals(TimerState.RUNNING, state.timerState)
                assertEquals(1800, state.timerTotalSeconds) // 30 * 60
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("pauseTimer should set timer to paused state")
        fun `pauseTimer should set timer to paused state`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.goToStep(1)
                awaitItem()

                viewModel.startTimer()
                awaitItem()

                viewModel.pauseTimer()

                val state = awaitItem()
                assertEquals(TimerState.PAUSED, state.timerState)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("stopTimer should reset timer to idle")
        fun `stopTimer should reset timer to idle`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.goToStep(1)
                awaitItem()

                viewModel.startTimer()
                awaitItem()

                viewModel.stopTimer()

                val state = awaitItem()
                assertEquals(TimerState.IDLE, state.timerState)
                assertEquals(0, state.timerRemainingSeconds)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Voice Guidance")
    inner class VoiceGuidance {

        @Test
        @DisplayName("toggleVoiceGuidance should toggle voice guidance")
        fun `toggleVoiceGuidance should toggle voice guidance`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial (voice guidance off)

                viewModel.toggleVoiceGuidance()

                val state = awaitItem()
                assertTrue(state.voiceGuidanceEnabled)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Exit and Completion")
    inner class ExitAndCompletion {

        @Test
        @DisplayName("requestExit should show exit confirmation")
        fun `requestExit should show exit confirmation`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.requestExit()

                val state = awaitItem()
                assertTrue(state.showExitConfirmation)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissExitConfirmation should hide exit confirmation")
        fun `dismissExitConfirmation should hide exit confirmation`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.requestExit()
                awaitItem()

                viewModel.dismissExitConfirmation()

                val state = awaitItem()
                assertFalse(state.showExitConfirmation)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("confirmExit should emit back navigation event")
        fun `confirmExit should emit back navigation event`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.navigationEvent.test {
                viewModel.confirmExit()
                val event = awaitItem()
                assertEquals(CookingModeNavigationEvent.NavigateBack, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateRating should update rating")
        fun `updateRating should update rating`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateRating(4)

                val state = awaitItem()
                assertEquals(4, state.rating)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateFeedback should update feedback")
        fun `updateFeedback should update feedback`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateFeedback("Great recipe!")

                val state = awaitItem()
                assertEquals("Great recipe!", state.feedback)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("skipRating should emit home navigation event")
        fun `skipRating should emit home navigation event`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.navigationEvent.test {
                viewModel.skipRating()
                val event = awaitItem()
                assertEquals(CookingModeNavigationEvent.NavigateToHome, event)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("Recipe not found should show error")
        fun `recipe not found should show error`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(null)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertNotNull(state.errorMessage)
                assertTrue(state.errorMessage?.contains("not found") == true)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("clearError should clear error message")
        fun `clearError should clear error message`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.clearError()

                val state = awaitItem()
                assertNull(state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Computed Properties")
    inner class ComputedProperties {

        @Test
        @DisplayName("stepNumber should be 1-indexed")
        fun `stepNumber should be 1-indexed`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertEquals(1, state.stepNumber) // First step is index 0, but stepNumber is 1
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("progress should be calculated correctly")
        fun `progress should be calculated correctly`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertEquals(0.25f, state.progress) // 1/4 steps
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("timerDisplayText should format correctly")
        fun `timerDisplayText should format correctly`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = CookingModeViewModel(savedStateHandle, mockRecipeRepository, mockk(relaxed = true))

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.goToStep(1)
                awaitItem()

                viewModel.startTimer()

                val state = awaitItem()
                assertEquals("30:00", state.timerDisplayText)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
