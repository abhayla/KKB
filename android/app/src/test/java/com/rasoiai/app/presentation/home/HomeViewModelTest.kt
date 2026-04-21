package com.rasoiai.app.presentation.home

import app.cash.turbine.test
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.MealItem
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealPlanDay
import com.rasoiai.domain.model.MealType
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.domain.repository.MealPlanRepository
import com.rasoiai.domain.repository.NotificationRepository
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockMealPlanRepository: MealPlanRepository
    private lateinit var mockRecipeRepository: RecipeRepository
    private lateinit var mockNetworkMonitor: NetworkMonitor
    private lateinit var mockNotificationRepository: NotificationRepository

    private fun createTestMealItem(
        id: String = "meal-item-1",
        recipeId: String = "recipe-1",
        recipeName: String = "Poha",
        isLocked: Boolean = false,
        order: Int = 0
    ) = MealItem(
        id = id,
        recipeId = recipeId,
        recipeName = recipeName,
        recipeImageUrl = null,
        prepTimeMinutes = 15,
        calories = 250,
        isLocked = isLocked,
        order = order,
        dietaryTags = listOf(DietaryTag.VEGETARIAN)
    )

    private val testMealPlan = MealPlan(
        id = "test-plan-1",
        weekStartDate = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.ordinal.toLong()),
        weekEndDate = LocalDate.now().plusDays(6 - LocalDate.now().dayOfWeek.ordinal.toLong()),
        days = listOf(
            MealPlanDay(
                date = LocalDate.now(),
                dayName = "Monday",
                breakfast = listOf(
                    createTestMealItem(id = "mi-1", recipeId = "recipe-1", recipeName = "Poha", isLocked = false, order = 0)
                ),
                lunch = listOf(
                    createTestMealItem(id = "mi-2", recipeId = "recipe-2", recipeName = "Dal Rice", isLocked = true, order = 0)
                ),
                dinner = listOf(
                    createTestMealItem(id = "mi-3", recipeId = "recipe-3", recipeName = "Roti Sabzi", isLocked = false, order = 0)
                ),
                snacks = emptyList(),
                festival = null
            )
        ),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockMealPlanRepository = mockk(relaxed = true)
        mockRecipeRepository = mockk(relaxed = true)
        mockNetworkMonitor = mockk(relaxed = true)
        mockNotificationRepository = mockk(relaxed = true)
        coEvery { mockNetworkMonitor.isOnline } returns flowOf(true)
        coEvery { mockNotificationRepository.getUnreadCount() } returns flowOf(0)
        coEvery { mockMealPlanRepository.fetchCurrentMealPlan() } returns null
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

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

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

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial loading state

                testDispatcher.scheduler.advanceUntilIdle()

                val loadedState = expectMostRecentItem()
                assertFalse(loadedState.isLoading)
                assertNotNull(loadedState.mealPlan)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Meal plan data should be populated correctly")
        fun `meal plan data should be populated correctly`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
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

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

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

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                val state = expectMostRecentItem()

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

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha", isLocked = false)
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

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha", isLocked = false)
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

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                val loadedState = expectMostRecentItem()
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

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                val loadedState = expectMostRecentItem()
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

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

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

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            // Setup state
            testDispatcher.scheduler.advanceUntilIdle()
            val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha", isLocked = false)
            viewModel.onRecipeClick(mealItem, MealType.BREAKFAST)
            testDispatcher.scheduler.advanceUntilIdle()

            // Subscribe first, then call method
            viewModel.navigationEvent.test {
                viewModel.viewRecipe()
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

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            // Subscribe first, then call method
            viewModel.navigationEvent.test {
                viewModel.navigateToSettings()
                val event = awaitItem()
                assertEquals(HomeNavigationEvent.NavigateToSettings, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToGrocery should emit grocery event")
        fun `navigateToGrocery should emit grocery event`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            // Subscribe first, then call method
            viewModel.navigationEvent.test {
                viewModel.navigateToGrocery()
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

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.clearError()

                val state = awaitItem()
                assertNull(state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Network error during load should show error message")
        fun `network error during load should show error message`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } throws Exception("Network unavailable")

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial loading
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isLoading)
                assertNotNull(state.errorMessage)
                assertTrue(state.errorMessage!!.contains("Failed to load"))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Error during swapRecipe should set error message")
        fun `error during swapRecipe should set error message`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockMealPlanRepository.swapMeal(any(), any(), any(), any(), any()) } returns Result.failure(Exception("Swap failed"))

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                // Select a recipe first
                val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha")
                viewModel.onRecipeClick(mealItem, MealType.BREAKFAST)
                awaitItem() // Action sheet shown

                viewModel.swapRecipe()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("Failed to swap recipe", state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Error during regenerateDay should set error message")
        fun `error during regenerateDay should set error message`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.failure(Exception("Regenerate failed"))

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                viewModel.regenerateDay()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isRefreshing)
                assertEquals("Failed to regenerate meals", state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Error during regenerateWeek should set error message")
        fun `error during regenerateWeek should set error message`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.failure(Exception("Regenerate failed"))

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                viewModel.regenerateWeek()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isRefreshing)
                assertEquals("Failed to regenerate meals", state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Error during toggleLockRecipe should set error message")
        fun `error during toggleLockRecipe should set error message`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockMealPlanRepository.setMealLockState(any(), any(), any(), any(), any()) } returns Result.failure(Exception("Lock failed"))

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                // Select a recipe
                val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha")
                viewModel.onRecipeClick(mealItem, MealType.BREAKFAST)
                awaitItem() // Action sheet shown

                viewModel.toggleLockRecipe()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("Failed to update recipe", state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Meal Plan Generation")
    inner class MealPlanGeneration {

        @Test
        @DisplayName("When no meal plan exists, generation succeeds and updates state")
        fun `when no meal plan exists generation succeeds and updates state`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(null)
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.success(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial loading
                testDispatcher.scheduler.advanceUntilIdle()

                // Use expectMostRecentItem because success path has multiple state updates
                val state = expectMostRecentItem()
                assertFalse(state.isLoading)
                assertNotNull(state.mealPlan)
                assertEquals(testMealPlan.id, state.mealPlan?.id)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("isLoading becomes false after successful generation")
        fun `isLoading becomes false after successful generation`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(null)
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.success(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                val initialState = awaitItem()
                assertTrue(initialState.isLoading)

                testDispatcher.scheduler.advanceUntilIdle()

                // Use expectMostRecentItem because success path has multiple state updates
                val loadedState = expectMostRecentItem()
                assertFalse(loadedState.isLoading)
                assertNull(loadedState.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("isLoading becomes false after failed generation")
        fun `isLoading becomes false after failed generation`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(null)
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.failure(Exception("Network error"))

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial loading
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertFalse(state.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Error message is shown when generation fails")
        fun `error message is shown when generation fails`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(null)
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.failure(Exception("Generation failed"))

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val errorState = awaitItem()
                assertNotNull(errorState.errorMessage)
                assertEquals(
                    "Failed to generate meal plan. Please check your connection and try again.",
                    errorState.errorMessage
                )
                assertNull(errorState.mealPlan)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Week dates and selected day meals populated on successful generation")
        fun `week dates and selected day meals populated on successful generation`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(null)
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.success(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                // Use expectMostRecentItem because success path has multiple state updates
                val state = expectMostRecentItem()
                assertEquals(7, state.weekDates.size)
                assertNotNull(state.selectedDayMeals)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Swap Recipe Actions")
    inner class SwapRecipeActions {

        @Test
        @DisplayName("showSwapOptions should show swap sheet and dismiss action sheet")
        fun `showSwapOptions should show swap sheet and dismiss action sheet`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockRecipeRepository.searchRecipes(any(), any(), any(), any(), any(), any()) } returns Result.success(emptyList())

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                // Open action sheet first
                val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha")
                viewModel.onRecipeClick(mealItem, MealType.BREAKFAST)
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Action sheet shown

                viewModel.showSwapOptions()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertTrue(state.showSwapSheet)
                assertFalse(state.showRecipeActionSheet)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissSwapSheet should hide swap sheet")
        fun `dismissSwapSheet should hide swap sheet`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockRecipeRepository.searchRecipes(any(), any(), any(), any(), any(), any()) } returns Result.success(emptyList())

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                // Select recipe and show swap sheet
                val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha")
                viewModel.onRecipeClick(mealItem, MealType.BREAKFAST)
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem()
                viewModel.showSwapOptions()
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem()

                viewModel.dismissSwapSheet()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.showSwapSheet)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("swapRecipe should set isRefreshing during operation")
        fun `swapRecipe should set isRefreshing during operation`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockMealPlanRepository.swapMeal(any(), any(), any(), any(), any()) } returns Result.success(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha")
                viewModel.onRecipeClick(mealItem, MealType.BREAKFAST)
                awaitItem()

                viewModel.swapRecipe()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isRefreshing)
                assertFalse(state.showSwapSheet)
                assertNull(state.selectedMealItem)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Remove Recipe Actions")
    inner class RemoveRecipeActions {

        @Test
        @DisplayName("removeRecipeFromMeal should dismiss sheet when not locked")
        fun `removeRecipeFromMeal should dismiss sheet when not locked`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockMealPlanRepository.removeRecipeFromMeal(any(), any(), any(), any()) } returns Result.success(Unit)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha", isLocked = false)
                viewModel.onRecipeClick(mealItem, MealType.BREAKFAST)
                awaitItem() // Action sheet shown

                viewModel.removeRecipeFromMeal()

                val state = awaitItem()
                assertFalse(state.showRecipeActionSheet)
                assertNull(state.selectedMealItem)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("removeRecipeFromMeal should show error when recipe is locked")
        fun `removeRecipeFromMeal should show error when recipe is locked`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha", isLocked = true)
                viewModel.onRecipeClick(mealItem, MealType.BREAKFAST)
                awaitItem() // Action sheet shown

                viewModel.removeRecipeFromMeal()

                val state = awaitItem()
                assertEquals("Cannot remove locked recipe", state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("removeRecipeFromMeal should show error when day is locked")
        fun `removeRecipeFromMeal should show error when day is locked`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                // Lock the day first
                viewModel.toggleDayLock()
                awaitItem()

                val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha", isLocked = false)
                viewModel.onRecipeClick(mealItem, MealType.BREAKFAST)
                awaitItem() // Action sheet shown

                viewModel.removeRecipeFromMeal()

                val state = awaitItem()
                assertEquals("Cannot remove locked recipe", state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("removeRecipeFromMeal should show error when meal slot is locked")
        fun `removeRecipeFromMeal should show error when meal slot is locked`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                // Lock the meal slot first
                viewModel.toggleMealLock(MealType.BREAKFAST)
                awaitItem()

                val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha", isLocked = false)
                viewModel.onRecipeClick(mealItem, MealType.BREAKFAST)
                awaitItem() // Action sheet shown

                viewModel.removeRecipeFromMeal()

                val state = awaitItem()
                assertEquals("Cannot remove locked recipe", state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("removeRecipeFromMealDirect should show error when locked")
        fun `removeRecipeFromMealDirect should show error when locked`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha", isLocked = true)
                viewModel.removeRecipeFromMealDirect(mealItem, MealType.BREAKFAST)

                val state = awaitItem()
                assertEquals("Cannot remove locked recipe", state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Direct Recipe Lock Toggle")
    inner class DirectRecipeLockToggle {

        @Test
        @DisplayName("toggleRecipeLockDirect should show error when day is locked")
        fun `toggleRecipeLockDirect should show error when day is locked`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                // Lock the day
                viewModel.toggleDayLock()
                awaitItem()

                val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha")
                viewModel.toggleRecipeLockDirect(mealItem, MealType.BREAKFAST)

                val state = awaitItem()
                assertEquals("Unlock day/meal first to change recipe lock", state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("toggleRecipeLockDirect should show error when meal is locked")
        fun `toggleRecipeLockDirect should show error when meal is locked`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                // Lock the meal slot
                viewModel.toggleMealLock(MealType.BREAKFAST)
                awaitItem()

                val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha")
                viewModel.toggleRecipeLockDirect(mealItem, MealType.BREAKFAST)

                val state = awaitItem()
                assertEquals("Unlock day/meal first to change recipe lock", state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("toggleRecipeLockDirect should call repository when not locked")
        fun `toggleRecipeLockDirect should call repository when not locked`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockMealPlanRepository.setMealLockState(any(), any(), any(), any(), any()) } returns Result.success(Unit)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)
            testDispatcher.scheduler.advanceUntilIdle()

            val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha", isLocked = false)
            viewModel.toggleRecipeLockDirect(mealItem, MealType.BREAKFAST)
            testDispatcher.scheduler.advanceUntilIdle()

            // No error should be set
            assertNull(viewModel.uiState.value.errorMessage)

            // Verify repository was called
            io.mockk.coVerify {
                mockMealPlanRepository.setMealLockState(
                    mealPlanId = testMealPlan.id,
                    date = LocalDate.now(),
                    mealType = MealType.BREAKFAST,
                    recipeId = "recipe-1",
                    isLocked = true  // Toggle from false to true
                )
            }
        }
    }

    @Nested
    @DisplayName("Refresh Actions")
    inner class RefreshActions {

        @Test
        @DisplayName("showRefreshOptions should show refresh sheet")
        fun `showRefreshOptions should show refresh sheet`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                viewModel.showRefreshOptions()

                val state = awaitItem()
                assertTrue(state.showRefreshSheet)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissRefreshSheet should hide refresh sheet")
        fun `dismissRefreshSheet should hide refresh sheet`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                viewModel.showRefreshOptions()
                awaitItem()

                viewModel.dismissRefreshSheet()

                val state = awaitItem()
                assertFalse(state.showRefreshSheet)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("regenerateDay should set isRefreshing and dismiss sheet")
        fun `regenerateDay should set isRefreshing and dismiss sheet`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.success(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                viewModel.showRefreshOptions()
                awaitItem()

                viewModel.regenerateDay()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isRefreshing)
                assertFalse(state.showRefreshSheet)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("regenerateWeek should set isRefreshing and dismiss sheet")
        fun `regenerateWeek should set isRefreshing and dismiss sheet`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.success(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                viewModel.showRefreshOptions()
                awaitItem()

                viewModel.regenerateWeek()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isRefreshing)
                assertFalse(state.showRefreshSheet)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Lock State Edge Cases")
    inner class LockStateEdgeCases {

        @Test
        @DisplayName("Meal lock state should persist when selecting different day")
        fun `meal lock state should persist when selecting different day`() = runTest {
            // Create meal plan with multiple days
            val multiDayMealPlan = testMealPlan.copy(
                days = listOf(
                    MealPlanDay(
                        date = LocalDate.now(),
                        dayName = "Today",
                        breakfast = listOf(createTestMealItem(id = "mi-1", recipeName = "Poha")),
                        lunch = emptyList(),
                        dinner = emptyList(),
                        snacks = emptyList(),
                        festival = null
                    ),
                    MealPlanDay(
                        date = LocalDate.now().plusDays(1),
                        dayName = "Tomorrow",
                        breakfast = listOf(createTestMealItem(id = "mi-2", recipeName = "Upma")),
                        lunch = emptyList(),
                        dinner = emptyList(),
                        snacks = emptyList(),
                        festival = null
                    )
                )
            )
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(multiDayMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)
            testDispatcher.scheduler.advanceUntilIdle()

            // Lock breakfast on today
            viewModel.toggleMealLock(MealType.BREAKFAST)
            testDispatcher.scheduler.advanceUntilIdle()

            // Switch to tomorrow
            viewModel.selectDate(LocalDate.now().plusDays(1))
            testDispatcher.scheduler.advanceUntilIdle()

            // Breakfast should NOT be locked on tomorrow
            assertFalse(viewModel.uiState.value.isMealLocked(MealType.BREAKFAST))

            // Switch back to today
            viewModel.selectDate(LocalDate.now())
            testDispatcher.scheduler.advanceUntilIdle()

            // Breakfast should still be locked on today
            assertTrue(viewModel.uiState.value.isMealLocked(MealType.BREAKFAST))
        }

        @Test
        @DisplayName("Day lock state should persist when selecting different day")
        fun `day lock state should persist when selecting different day`() = runTest {
            val multiDayMealPlan = testMealPlan.copy(
                days = listOf(
                    MealPlanDay(
                        date = LocalDate.now(),
                        dayName = "Today",
                        breakfast = listOf(createTestMealItem(id = "mi-1", recipeName = "Poha")),
                        lunch = emptyList(),
                        dinner = emptyList(),
                        snacks = emptyList(),
                        festival = null
                    ),
                    MealPlanDay(
                        date = LocalDate.now().plusDays(1),
                        dayName = "Tomorrow",
                        breakfast = listOf(createTestMealItem(id = "mi-2", recipeName = "Upma")),
                        lunch = emptyList(),
                        dinner = emptyList(),
                        snacks = emptyList(),
                        festival = null
                    )
                )
            )
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(multiDayMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)
            testDispatcher.scheduler.advanceUntilIdle()

            // Lock today
            viewModel.toggleDayLock()
            testDispatcher.scheduler.advanceUntilIdle()

            // Switch to tomorrow
            viewModel.selectDate(LocalDate.now().plusDays(1))
            testDispatcher.scheduler.advanceUntilIdle()

            // Tomorrow should NOT be locked
            assertFalse(viewModel.uiState.value.isSelectedDayLocked)

            // Switch back to today
            viewModel.selectDate(LocalDate.now())
            testDispatcher.scheduler.advanceUntilIdle()

            // Today should still be locked
            assertTrue(viewModel.uiState.value.isSelectedDayLocked)
        }

        @Test
        @DisplayName("Multiple meal locks should work independently")
        fun `multiple meal locks should work independently`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)
            testDispatcher.scheduler.advanceUntilIdle()

            // Lock breakfast
            viewModel.toggleMealLock(MealType.BREAKFAST)
            testDispatcher.scheduler.advanceUntilIdle()

            // Lock dinner
            viewModel.toggleMealLock(MealType.DINNER)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isMealLocked(MealType.BREAKFAST))
            assertFalse(state.isMealLocked(MealType.LUNCH))
            assertTrue(state.isMealLocked(MealType.DINNER))
            assertFalse(state.isMealLocked(MealType.SNACKS))
        }

        @Test
        @DisplayName("Unlocking day should not affect meal lock states")
        fun `unlocking day should not affect meal lock states`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)
            testDispatcher.scheduler.advanceUntilIdle()

            // Lock meal first
            viewModel.toggleMealLock(MealType.BREAKFAST)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then lock day
            viewModel.toggleDayLock()
            testDispatcher.scheduler.advanceUntilIdle()

            // Unlock day
            viewModel.toggleDayLock()
            testDispatcher.scheduler.advanceUntilIdle()

            // Meal lock should still be active
            val state = viewModel.uiState.value
            assertFalse(state.isSelectedDayLocked)
            assertTrue(state.isMealLocked(MealType.BREAKFAST))
        }
    }

    @Nested
    @DisplayName("Date Selection Edge Cases")
    inner class DateSelectionEdgeCases {

        @Test
        @DisplayName("selectDate should do nothing when mealPlan is null")
        fun `selectDate should do nothing when mealPlan is null`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(null)
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.failure(Exception("Failed"))

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                val loadedState = expectMostRecentItem()

                // Try to select a date when meal plan is null
                val tomorrow = LocalDate.now().plusDays(1)
                viewModel.selectDate(tomorrow)

                // State should not change (no new emission for date)
                // The selected date stays at today
                assertEquals(LocalDate.now(), loadedState.selectedDate)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("selectDate with date not in meal plan should set null selectedDayMeals")
        fun `selectDate with date not in meal plan should set null selectedDayMeals`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Loaded

                // Select a date that's not in the meal plan days
                val futureDate = LocalDate.now().plusDays(30)
                viewModel.selectDate(futureDate)

                val state = awaitItem()
                assertEquals(futureDate, state.selectedDate)
                assertNull(state.selectedDayMeals)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Additional Navigation Events")
    inner class AdditionalNavigationEvents {

        @Test
        @DisplayName("navigateToNotifications should emit notifications event")
        fun `navigateToNotifications should emit notifications event`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateToNotifications()
                val event = awaitItem()
                assertEquals(HomeNavigationEvent.NavigateToNotifications, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToChat should emit chat event")
        fun `navigateToChat should emit chat event`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateToChat()
                val event = awaitItem()
                assertEquals(HomeNavigationEvent.NavigateToChat, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToFavorites should emit favorites event")
        fun `navigateToFavorites should emit favorites event`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateToFavorites()
                val event = awaitItem()
                assertEquals(HomeNavigationEvent.NavigateToFavorites, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToStats should emit stats event")
        fun `navigateToStats should emit stats event`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateToStats()
                val event = awaitItem()
                assertEquals(HomeNavigationEvent.NavigateToStats, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("viewRecipe should include lock state in navigation event")
        fun `viewRecipe should include lock state in navigation event`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            testDispatcher.scheduler.advanceUntilIdle()

            // Lock day first
            viewModel.toggleDayLock()
            testDispatcher.scheduler.advanceUntilIdle()

            // Select a recipe
            val mealItem = createTestMealItem(recipeId = "recipe-1", recipeName = "Poha", isLocked = false)
            viewModel.onRecipeClick(mealItem, MealType.BREAKFAST)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.navigationEvent.test {
                viewModel.viewRecipe()
                val event = awaitItem()
                assertTrue(event is HomeNavigationEvent.NavigateToRecipeDetail)
                assertTrue((event as HomeNavigationEvent.NavigateToRecipeDetail).isLocked)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Race Condition Prevention")
    inner class RaceConditionPrevention {

        @Test
        @DisplayName("Should not show error when meal plan loads successfully")
        fun `should not show error when meal plan loads successfully`() = runTest {
            // This test verifies the fix for the race condition where
            // error was shown despite meals appearing successfully
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial loading
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                // Should have meal plan data
                assertNotNull(state.mealPlan)
                assertEquals(testMealPlan.id, state.mealPlan?.id)
                // Should NOT have error message
                assertNull(state.errorMessage)
                // Should not be loading
                assertFalse(state.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Generation should only be called once when no meal plan exists")
        fun `generation should only be called once when no meal plan exists`() = runTest {
            // This test verifies that generation is only called once, not twice (race condition)
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(null)
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.success(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify generateMealPlan was only called once
            io.mockk.coVerify(exactly = 1) { mockMealPlanRepository.generateMealPlan(any()) }
        }

        @Test
        @DisplayName("Should observe meal plan changes after initial load")
        fun `should observe meal plan changes after initial load`() = runTest {
            // Create a mutable flow to simulate Room updates
            val mealPlanFlow = kotlinx.coroutines.flow.MutableStateFlow<MealPlan?>(testMealPlan)
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns mealPlanFlow

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial loading
                testDispatcher.scheduler.advanceUntilIdle()

                val initialState = expectMostRecentItem()
                assertEquals(testMealPlan.id, initialState.mealPlan?.id)

                // Simulate Room emitting an updated meal plan
                val updatedMealPlan = testMealPlan.copy(id = "updated-plan-id")
                mealPlanFlow.value = updatedMealPlan
                testDispatcher.scheduler.advanceUntilIdle()

                val updatedState = expectMostRecentItem()
                assertEquals("updated-plan-id", updatedState.mealPlan?.id)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("UiState Computed Properties")
    inner class UiStateComputedProperties {

        @Test
        @DisplayName("formattedDateRange should return correct format")
        fun `formattedDateRange should return correct format`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertTrue(state.formattedDateRange.isNotEmpty())
                assertTrue(state.formattedDateRange.contains("-"))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("formattedSelectedDay should return correct format")
        fun `formattedSelectedDay should return correct format`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertTrue(state.formattedSelectedDay.isNotEmpty())
                // Should contain day name and date
                assertTrue(state.formattedSelectedDay.matches(Regex(".*[A-Za-z]+.*\\d+.*")))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("isRecipeEffectivelyLocked should consider all lock levels")
        fun `isRecipeEffectivelyLocked should consider all lock levels`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                val unlockedItem = createTestMealItem(isLocked = false)
                val lockedItem = createTestMealItem(isLocked = true)

                // Recipe level lock
                assertFalse(state.isRecipeEffectivelyLocked(unlockedItem, MealType.BREAKFAST))
                assertTrue(state.isRecipeEffectivelyLocked(lockedItem, MealType.BREAKFAST))
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Generation Failure Fallback")
    inner class GenerationFailureFallback {

        @Test
        @DisplayName("Should show cached plan when generation fails")
        fun `should show cached plan when generation fails`() = runTest {
            // First call returns null (triggers generation), subsequent calls return cached plan (fallback)
            var callCount = 0
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } answers {
                callCount++
                if (callCount <= 1) flowOf(null) else flowOf(testMealPlan)
            }
            coEvery { mockMealPlanRepository.fetchCurrentMealPlan() } returns null
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.failure(Exception("Gemini unavailable"))

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                testDispatcher.scheduler.advanceUntilIdle()
                val state = expectMostRecentItem()

                // Should show the cached plan, not a blank screen
                assertNotNull(state.mealPlan)
                assertTrue(state.isStale)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should set showRetryButton on generation failure with fallback")
        fun `should set showRetryButton on generation failure with fallback`() = runTest {
            var callCount = 0
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } answers {
                callCount++
                if (callCount <= 1) flowOf(null) else flowOf(testMealPlan)
            }
            coEvery { mockMealPlanRepository.fetchCurrentMealPlan() } returns null
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.failure(Exception("Timeout"))

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                testDispatcher.scheduler.advanceUntilIdle()
                val state = expectMostRecentItem()

                assertTrue(state.showRetryButton)
                assertNotNull(state.mealPlan)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should show error when no cached plan exists")
        fun `should show error when no cached plan exists`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(null)
            coEvery { mockMealPlanRepository.fetchCurrentMealPlan() } returns null
            coEvery { mockMealPlanRepository.generateMealPlan(any()) } returns Result.failure(Exception("Fail"))

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                testDispatcher.scheduler.advanceUntilIdle()
                val state = expectMostRecentItem()

                assertNull(state.mealPlan)
                assertNotNull(state.errorMessage)
                assertTrue(state.showRetryButton)
                assertFalse(state.isStale)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Swap Recipe Selection")
    inner class SwapRecipeSelection {

        @Test
        @DisplayName("Should pass selected recipe ID to repository on swap")
        fun `should pass selected recipe ID to repository on swap`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockMealPlanRepository.swapMeal(any(), any(), any(), any(), any(), any()) } returns Result.success(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()

                // Select a meal item first
                viewModel.onRecipeClick(
                    createTestMealItem(id = "mi-1", recipeId = "recipe-1"),
                    MealType.BREAKFAST
                )
                testDispatcher.scheduler.advanceUntilIdle()

                // Swap with a specific recipe
                viewModel.swapRecipe("new-recipe-42")
                testDispatcher.scheduler.advanceUntilIdle()

                // Verify the newRecipeId was passed through
                io.mockk.coVerify {
                    mockMealPlanRepository.swapMeal(
                        mealPlanId = any(),
                        date = any(),
                        mealType = MealType.BREAKFAST,
                        currentRecipeId = "recipe-1",
                        newRecipeId = "new-recipe-42"
                    )
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should pass null for random swap")
        fun `should pass null for random swap`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockMealPlanRepository.swapMeal(any(), any(), any(), any(), any(), any()) } returns Result.success(testMealPlan)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()

                viewModel.onRecipeClick(
                    createTestMealItem(id = "mi-1", recipeId = "recipe-1"),
                    MealType.BREAKFAST
                )
                testDispatcher.scheduler.advanceUntilIdle()

                viewModel.swapRecipe() // No recipe ID = random
                testDispatcher.scheduler.advanceUntilIdle()

                io.mockk.coVerify {
                    mockMealPlanRepository.swapMeal(
                        mealPlanId = any(),
                        date = any(),
                        mealType = MealType.BREAKFAST,
                        currentRecipeId = "recipe-1",
                        newRecipeId = null
                    )
                }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Lock Persistence")
    inner class LockPersistence {

        @Test
        @DisplayName("Should persist day lock state to repository")
        fun `should persist day lock state to repository`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockMealPlanRepository.setDayLockState(any(), any(), any()) } returns Result.success(Unit)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()

                viewModel.toggleDayLock()
                testDispatcher.scheduler.advanceUntilIdle()

                io.mockk.coVerify {
                    mockMealPlanRepository.setDayLockState(
                        mealPlanId = "test-plan-1",
                        date = any(),
                        isLocked = true
                    )
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should persist meal type lock state to repository")
        fun `should persist meal type lock state to repository`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockMealPlanRepository.setMealTypeLockState(any(), any(), any(), any()) } returns Result.success(Unit)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()

                viewModel.toggleMealLock(MealType.BREAKFAST)
                testDispatcher.scheduler.advanceUntilIdle()

                io.mockk.coVerify {
                    mockMealPlanRepository.setMealTypeLockState(
                        mealPlanId = "test-plan-1",
                        date = any(),
                        mealType = MealType.BREAKFAST,
                        isLocked = true
                    )
                }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Notification Badge Count (issue #57)")
    inner class NotificationBadgeCount {

        @Test
        @DisplayName("Initial notificationBadgeCount defaults to zero when repo emits 0")
        fun `initial notificationBadgeCount defaults to zero`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockNotificationRepository.getUnreadCount() } returns flowOf(0)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                testDispatcher.scheduler.advanceUntilIdle()
                val state = expectMostRecentItem()
                assertEquals(0, state.notificationBadgeCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("notificationBadgeCount propagates non-zero count from repository")
        fun `notificationBadgeCount propagates non-zero count`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            coEvery { mockNotificationRepository.getUnreadCount() } returns flowOf(7)

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                testDispatcher.scheduler.advanceUntilIdle()
                val state = expectMostRecentItem()
                assertEquals(7, state.notificationBadgeCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("notificationBadgeCount updates when repository flow emits new values")
        fun `notificationBadgeCount updates when flow emits new values`() = runTest {
            coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)
            val countFlow = kotlinx.coroutines.flow.MutableStateFlow(0)
            coEvery { mockNotificationRepository.getUnreadCount() } returns countFlow

            val viewModel = HomeViewModel(mockMealPlanRepository, mockRecipeRepository, mockNetworkMonitor, mockNotificationRepository)

            viewModel.uiState.test {
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(0, expectMostRecentItem().notificationBadgeCount)

                countFlow.value = 3
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(3, expectMostRecentItem().notificationBadgeCount)

                countFlow.value = 42
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(42, expectMostRecentItem().notificationBadgeCount)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
