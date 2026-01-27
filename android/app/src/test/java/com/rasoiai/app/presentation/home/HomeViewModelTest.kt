package com.rasoiai.app.presentation.home

import app.cash.turbine.test
import com.rasoiai.domain.model.MealItem
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealPlanDay
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.repository.MealPlanRepository
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockMealPlanRepository: MealPlanRepository

    private val testMealPlan = MealPlan(
        id = "test-plan-1",
        weekStartDate = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.ordinal.toLong()),
        weekEndDate = LocalDate.now().plusDays(6 - LocalDate.now().dayOfWeek.ordinal.toLong()),
        days = listOf(
            MealPlanDay(
                date = LocalDate.now(),
                breakfast = listOf(
                    MealItem(recipeId = "recipe-1", recipeName = "Poha", isLocked = false, servings = 2)
                ),
                lunch = listOf(
                    MealItem(recipeId = "recipe-2", recipeName = "Dal Rice", isLocked = true, servings = 4)
                ),
                dinner = listOf(
                    MealItem(recipeId = "recipe-3", recipeName = "Roti Sabzi", isLocked = false, servings = 4)
                ),
                snacks = emptyList()
            )
        )
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockMealPlanRepository = mockk(relaxed = true)
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
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(null)
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.success(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository)

            viewModel.uiState.test {
                val initialState = awaitItem()
                assertTrue(initialState.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("After loading meal plan, isLoading should be false")
        fun `after loading meal plan isLoading should be false`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial loading state

                testDispatcher.scheduler.advanceUntilIdle()

                val loadedState = awaitItem()
                assertFalse(loadedState.isLoading)
                assertNotNull(loadedState.mealPlan)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Meal plan data should be populated correctly")
        fun `meal plan data should be populated correctly`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertEquals(testMealPlan.id, state.mealPlan?.id)
                assertNotNull(state.weekDates)
                assertEquals(7, state.weekDates.size)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Date Selection")
    inner class DateSelection {

        @Test
        @DisplayName("selectDate should update selected date")
        fun `selectDate should update selected date`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                val tomorrow = LocalDate.now().plusDays(1)
                viewModel.selectDate(tomorrow)

                val state = awaitItem()
                assertEquals(tomorrow, state.selectedDate)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("isToday should return true for today")
        fun `isToday should return true for today`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                val state = awaitItem()

                assertTrue(state.isToday)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Recipe Actions")
    inner class RecipeActions {

        @Test
        @DisplayName("onRecipeClick should show action sheet")
        fun `onRecipeClick should show action sheet`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                val mealItem = MealItem(recipeId = "recipe-1", recipeName = "Poha", isLocked = false, servings = 2)
                viewModel.onRecipeClick(mealItem, MealType.BREAKFAST)

                val state = awaitItem()
                assertTrue(state.showRecipeActionSheet)
                assertEquals(mealItem, state.selectedMealItem)
                assertEquals(MealType.BREAKFAST, state.selectedMealType)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissRecipeActionSheet should hide action sheet")
        fun `dismissRecipeActionSheet should hide action sheet`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                val mealItem = MealItem(recipeId = "recipe-1", recipeName = "Poha", isLocked = false, servings = 2)
                viewModel.onRecipeClick(mealItem, MealType.BREAKFAST)
                awaitItem() // Action sheet shown

                viewModel.dismissRecipeActionSheet()

                val state = awaitItem()
                assertFalse(state.showRecipeActionSheet)
                assertNull(state.selectedMealItem)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Locking System")
    inner class LockingSystem {

        @Test
        @DisplayName("toggleDayLock should toggle day lock state")
        fun `toggleDayLock should toggle day lock state`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                val loadedState = awaitItem()
                assertFalse(loadedState.isSelectedDayLocked)

                viewModel.toggleDayLock()

                val lockedState = awaitItem()
                assertTrue(lockedState.isSelectedDayLocked)

                viewModel.toggleDayLock()

                val unlockedState = awaitItem()
                assertFalse(unlockedState.isSelectedDayLocked)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("toggleMealLock should toggle meal lock state")
        fun `toggleMealLock should toggle meal lock state`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                val loadedState = awaitItem()
                assertFalse(loadedState.isMealLocked(MealType.BREAKFAST))

                viewModel.toggleMealLock(MealType.BREAKFAST)

                val lockedState = awaitItem()
                assertTrue(lockedState.isMealLocked(MealType.BREAKFAST))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Day lock should take precedence over meal lock")
        fun `day lock should take precedence over meal lock`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.toggleDayLock()
                val lockedDayState = awaitItem()

                // Even without meal lock, isMealLocked should return true due to day lock
                assertTrue(lockedDayState.isMealLocked(MealType.BREAKFAST))
                assertTrue(lockedDayState.isMealLocked(MealType.LUNCH))
                assertTrue(lockedDayState.isMealLocked(MealType.DINNER))
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        @DisplayName("viewRecipe should emit navigation event")
        fun `viewRecipe should emit navigation event`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository)

            // Setup state
            testDispatcher.scheduler.advanceUntilIdle()
            val mealItem = MealItem(recipeId = "recipe-1", recipeName = "Poha", isLocked = false, servings = 2)
            viewModel.onRecipeClick(mealItem, MealType.BREAKFAST)
            testDispatcher.scheduler.advanceUntilIdle()

            // View recipe
            viewModel.viewRecipe()

            viewModel.navigationEvent.test {
                val event = awaitItem()
                assertTrue(event is HomeNavigationEvent.NavigateToRecipeDetail)
                assertEquals("recipe-1", (event as HomeNavigationEvent.NavigateToRecipeDetail).recipeId)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToSettings should emit settings event")
        fun `navigateToSettings should emit settings event`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository)

            viewModel.navigateToSettings()

            viewModel.navigationEvent.test {
                val event = awaitItem()
                assertEquals(HomeNavigationEvent.NavigateToSettings, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToGrocery should emit grocery event")
        fun `navigateToGrocery should emit grocery event`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository)

            viewModel.navigateToGrocery()

            viewModel.navigationEvent.test {
                val event = awaitItem()
                assertEquals(HomeNavigationEvent.NavigateToGrocery, event)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("clearError should clear error message")
        fun `clearError should clear error message`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.clearError()

                val state = awaitItem()
                assertNull(state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
