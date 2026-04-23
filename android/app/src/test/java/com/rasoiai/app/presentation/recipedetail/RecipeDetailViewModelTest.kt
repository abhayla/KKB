package com.rasoiai.app.presentation.recipedetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rasoiai.app.presentation.navigation.Screen
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.Ingredient
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.model.Instruction
import com.rasoiai.domain.model.FavoriteCollection
import com.rasoiai.domain.model.Nutrition
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.repository.FavoritesRepository
import com.rasoiai.domain.repository.GroceryRepository
import com.rasoiai.domain.repository.RecipeRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
class RecipeDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRecipeRepository: RecipeRepository
    private lateinit var mockGroceryRepository: GroceryRepository
    private lateinit var mockFavoritesRepository: FavoritesRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private val testCollections = listOf(
        FavoriteCollection(
            id = "all",
            name = "All",
            recipeIds = emptyList(),
            coverImageUrl = null,
            isDefault = true,
            createdAt = 0L
        ),
        FavoriteCollection(
            id = "weeknight-quick",
            name = "Weeknight Quick",
            recipeIds = listOf("other-recipe"),
            coverImageUrl = null,
            isDefault = false,
            createdAt = 1_000L
        ),
        FavoriteCollection(
            id = "weekend-feasts",
            name = "Weekend Feasts",
            recipeIds = emptyList(),
            coverImageUrl = null,
            isDefault = false,
            createdAt = 2_000L
        )
    )

    private val testRecipe = Recipe(
        id = "test-recipe-1",
        name = "Paneer Butter Masala",
        description = "A rich and creamy curry dish",
        imageUrl = "https://example.com/paneer.jpg",
        prepTimeMinutes = 15,
        cookTimeMinutes = 30,
        servings = 4,
        difficulty = Difficulty.MEDIUM,
        cuisineType = CuisineType.NORTH,
        mealTypes = listOf(com.rasoiai.domain.model.MealType.LUNCH, com.rasoiai.domain.model.MealType.DINNER),
        dietaryTags = listOf(DietaryTag.VEGETARIAN),
        ingredients = listOf(
            Ingredient(id = "ing-1", name = "Paneer", quantity = "200", unit = "g", category = IngredientCategory.DAIRY),
            Ingredient(id = "ing-2", name = "Tomatoes", quantity = "3", unit = "medium", category = IngredientCategory.VEGETABLES),
            Ingredient(id = "ing-3", name = "Cream", quantity = "100", unit = "ml", category = IngredientCategory.DAIRY)
        ),
        instructions = listOf(
            Instruction(stepNumber = 1, instruction = "Cut paneer into cubes", durationMinutes = 5, tips = null),
            Instruction(stepNumber = 2, instruction = "Blend tomatoes", durationMinutes = 2, tips = null),
            Instruction(stepNumber = 3, instruction = "Cook with spices and add cream", durationMinutes = 20, tips = "Stir frequently")
        ),
        nutrition = Nutrition(calories = 350, proteinGrams = 15, carbohydratesGrams = 12, fatGrams = 28, fiberGrams = 2, sugarGrams = 4, sodiumMg = 600),
        isFavorite = false
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRecipeRepository = mockk(relaxed = true)
        mockGroceryRepository = mockk(relaxed = true)
        mockFavoritesRepository = mockk(relaxed = true)
        coEvery { mockFavoritesRepository.getCollections() } returns flowOf(emptyList())
        savedStateHandle = SavedStateHandle().apply {
            set(Screen.RecipeDetail.ARG_RECIPE_ID, "test-recipe-1")
            set(Screen.RecipeDetail.ARG_IS_LOCKED, false)
            set(Screen.RecipeDetail.ARG_FROM_MEAL_PLAN, false)
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

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            viewModel.uiState.test {
                val initialState = awaitItem()
                assertTrue(initialState.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("After loading recipe, isLoading should be false")
        fun `after loading recipe isLoading should be false`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            viewModel.uiState.test {
                awaitItem() // Initial loading

                testDispatcher.scheduler.advanceUntilIdle()

                val loadedState = awaitItem()
                assertFalse(loadedState.isLoading)
                assertNotNull(loadedState.recipe)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Lock state should be NO_CONTEXT when not from meal plan")
        fun `lock state should be NO_CONTEXT when not from meal plan`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(RecipeLockState.NO_CONTEXT, state.lockState)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Lock state should be LOCKED when from meal plan and locked")
        fun `lock state should be LOCKED when from meal plan and locked`() = runTest {
            savedStateHandle.apply {
                set(Screen.RecipeDetail.ARG_IS_LOCKED, true)
                set(Screen.RecipeDetail.ARG_FROM_MEAL_PLAN, true)
            }
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(RecipeLockState.LOCKED, state.lockState)
                assertTrue(state.isLocked)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Tab Selection")
    inner class TabSelection {

        @Test
        @DisplayName("selectTab should update selected tab index")
        fun `selectTab should update selected tab index`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.selectTab(1)

                val state = awaitItem()
                assertEquals(1, state.selectedTabIndex)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Ingredient Checking")
    inner class IngredientChecking {

        @Test
        @DisplayName("toggleIngredientChecked should add ingredient to checked list")
        fun `toggleIngredientChecked should add ingredient to checked list`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.toggleIngredientChecked("ing-1")

                val state = awaitItem()
                assertTrue(state.checkedIngredients.contains("ing-1"))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("toggleIngredientChecked should remove ingredient from checked list")
        fun `toggleIngredientChecked should remove ingredient from checked list`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.toggleIngredientChecked("ing-1")
                awaitItem() // Checked

                viewModel.toggleIngredientChecked("ing-1")

                val state = awaitItem()
                assertFalse(state.checkedIngredients.contains("ing-1"))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("checkAllIngredients should check all ingredients")
        fun `checkAllIngredients should check all ingredients`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.checkAllIngredients()

                val state = awaitItem()
                assertTrue(state.allIngredientsChecked)
                assertEquals(3, state.checkedIngredients.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("uncheckAllIngredients should uncheck all ingredients")
        fun `uncheckAllIngredients should uncheck all ingredients`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.checkAllIngredients()
                awaitItem() // All checked

                viewModel.uncheckAllIngredients()

                val state = awaitItem()
                assertTrue(state.checkedIngredients.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        @DisplayName("startCookingMode should emit navigation event")
        fun `startCookingMode should emit navigation event`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            viewModel.navigationEvent.test {
                viewModel.startCookingMode()
                val event = awaitItem()
                assertTrue(event is RecipeDetailNavigationEvent.NavigateToCookingMode)
                assertEquals("test-recipe-1", (event as RecipeDetailNavigationEvent.NavigateToCookingMode).recipeId)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateBack should emit back navigation event")
        fun `navigateBack should emit back navigation event`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateBack()
                val event = awaitItem()
                assertEquals(RecipeDetailNavigationEvent.NavigateBack, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("modifyWithAI should emit chat navigation event")
        fun `modifyWithAI should emit chat navigation event`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.navigationEvent.test {
                viewModel.modifyWithAI()
                val event = awaitItem()
                assertTrue(event is RecipeDetailNavigationEvent.NavigateToChat)
                assertTrue((event as RecipeDetailNavigationEvent.NavigateToChat).recipeContext.contains("Paneer Butter Masala"))
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Computed Properties")
    inner class ComputedProperties {

        @Test
        @DisplayName("isVegetarian should be true for vegetarian recipe")
        fun `isVegetarian should be true for vegetarian recipe`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertTrue(state.isVegetarian)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("totalTimeMinutes should return sum of prep and cook time")
        fun `totalTimeMinutes should return sum of prep and cook time`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertEquals(45, state.totalTimeMinutes) // 15 + 30
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("ingredientCount should return correct count")
        fun `ingredientCount should return correct count`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertEquals(3, state.ingredientCount)
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

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

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

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)

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
    @DisplayName("Add to Collection")
    inner class AddToCollection {

        @Test
        @DisplayName("collections exposes non-default collections from repository")
        fun `collections exposes non-default collections from repository`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)
            coEvery { mockFavoritesRepository.getCollections() } returns flowOf(testCollections)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2, state.collections.size)
            assertEquals("weeknight-quick", state.collections[0].id)
            assertEquals("weekend-feasts", state.collections[1].id)
            assertFalse(state.collections.any { it.isDefault })
        }

        @Test
        @DisplayName("addToCollection delegates to favoritesRepository with recipeId and collectionId")
        fun `addToCollection delegates to favoritesRepository with recipeId and collectionId`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)
            coEvery {
                mockFavoritesRepository.addRecipeToCollection(any(), any())
            } returns Result.success(Unit)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.addToCollection("weeknight-quick")
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                mockFavoritesRepository.addRecipeToCollection("test-recipe-1", "weeknight-quick")
            }
        }

        @Test
        @DisplayName("addToCollection on success sets confirmation feedback")
        fun `addToCollection on success sets confirmation feedback`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)
            coEvery { mockFavoritesRepository.getCollections() } returns flowOf(testCollections)
            coEvery {
                mockFavoritesRepository.addRecipeToCollection("test-recipe-1", "weeknight-quick")
            } returns Result.success(Unit)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.addToCollection("weeknight-quick")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.errorMessage)
            assertTrue(
                state.errorMessage?.contains("Weeknight Quick") == true,
                "Expected feedback to name the collection, got: ${state.errorMessage}"
            )
        }

        @Test
        @DisplayName("addToCollection on failure sets failure feedback")
        fun `addToCollection on failure sets failure feedback`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(testRecipe)
            coEvery { mockFavoritesRepository.getCollections() } returns flowOf(testCollections)
            coEvery {
                mockFavoritesRepository.addRecipeToCollection(any(), any())
            } returns Result.failure(RuntimeException("boom"))

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.addToCollection("weeknight-quick")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.errorMessage)
            assertTrue(
                state.errorMessage?.contains("Failed") == true,
                "Expected failure feedback, got: ${state.errorMessage}"
            )
        }

        @Test
        @DisplayName("addToCollection is a no-op when recipe has not loaded yet")
        fun `addToCollection is a no-op when recipe has not loaded yet`() = runTest {
            coEvery { mockRecipeRepository.getRecipeById(any()) } returns flowOf(null)

            val viewModel = RecipeDetailViewModel(savedStateHandle, mockRecipeRepository, mockGroceryRepository, mockFavoritesRepository)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.addToCollection("weeknight-quick")
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) {
                mockFavoritesRepository.addRecipeToCollection(any(), any())
            }
        }
    }
}
