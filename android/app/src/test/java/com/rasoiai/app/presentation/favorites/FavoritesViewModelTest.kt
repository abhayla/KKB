package com.rasoiai.app.presentation.favorites

import app.cash.turbine.test
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.FavoriteCollection
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.repository.FavoritesRepository
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockFavoritesRepository: FavoritesRepository
    private lateinit var mockRecipeRepository: RecipeRepository

    private val testCollections = listOf(
        FavoriteCollection(
            id = FavoriteCollection.COLLECTION_ALL,
            name = "All Favorites",
            recipeIds = listOf("recipe-1", "recipe-2", "recipe-3", "recipe-4", "recipe-5"),
            coverImageUrl = null,
            isDefault = true,
            createdAt = System.currentTimeMillis()
        ),
        FavoriteCollection(
            id = "collection-1",
            name = "Quick Meals",
            recipeIds = listOf("recipe-1", "recipe-2", "recipe-3"),
            coverImageUrl = null,
            isDefault = false,
            createdAt = System.currentTimeMillis()
        )
    )

    private val testRecipes = listOf(
        Recipe(
            id = "recipe-1",
            name = "Poha",
            description = "Quick breakfast",
            imageUrl = null,
            prepTimeMinutes = 5,
            cookTimeMinutes = 10,
            servings = 2,
            difficulty = Difficulty.EASY,
            cuisineType = CuisineType.WEST,
            mealTypes = listOf(com.rasoiai.domain.model.MealType.BREAKFAST),
            dietaryTags = listOf(DietaryTag.VEGETARIAN),
            ingredients = emptyList(),
            instructions = emptyList(),
            nutrition = null,
            isFavorite = true
        ),
        Recipe(
            id = "recipe-2",
            name = "Dal Tadka",
            description = "Comfort food",
            imageUrl = null,
            prepTimeMinutes = 10,
            cookTimeMinutes = 25,
            servings = 4,
            difficulty = Difficulty.EASY,
            cuisineType = CuisineType.NORTH,
            mealTypes = listOf(com.rasoiai.domain.model.MealType.LUNCH, com.rasoiai.domain.model.MealType.DINNER),
            dietaryTags = listOf(DietaryTag.VEGETARIAN, DietaryTag.VEGAN),
            ingredients = emptyList(),
            instructions = emptyList(),
            nutrition = null,
            isFavorite = true
        )
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockFavoritesRepository = mockk(relaxed = true)
        mockRecipeRepository = mockk(relaxed = true)

        coEvery { mockFavoritesRepository.getCollections() } returns flowOf(testCollections)
        coEvery { mockFavoritesRepository.getRecipesInCollection(any()) } returns flowOf(testRecipes)
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
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                val initialState = awaitItem()
                assertTrue(initialState.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("After loading, collections should be populated")
        fun `after loading collections should be populated`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals(2, state.collections.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Default selected collection should be ALL")
        fun `default selected collection should be ALL`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(FavoriteCollection.COLLECTION_ALL, state.selectedCollectionId)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Collection Selection")
    inner class CollectionSelection {

        @Test
        @DisplayName("selectCollection should update selected collection")
        fun `selectCollection should update selected collection`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.selectCollection("collection-1")

                val state = awaitItem()
                assertEquals("collection-1", state.selectedCollectionId)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Create Collection Dialog")
    inner class CreateCollectionDialog {

        @Test
        @DisplayName("showCreateCollectionDialog should show dialog")
        fun `showCreateCollectionDialog should show dialog`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showCreateCollectionDialog()

                val state = awaitItem()
                assertTrue(state.showCreateCollectionDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissCreateCollectionDialog should hide dialog")
        fun `dismissCreateCollectionDialog should hide dialog`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showCreateCollectionDialog()
                awaitItem()

                viewModel.dismissCreateCollectionDialog()

                val state = awaitItem()
                assertFalse(state.showCreateCollectionDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Reorder Mode")
    inner class ReorderMode {

        @Test
        @DisplayName("enterReorderMode should enable reorder mode")
        fun `enterReorderMode should enable reorder mode`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.enterReorderMode()

                val state = awaitItem()
                assertTrue(state.isReorderMode)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("exitReorderMode should disable reorder mode")
        fun `exitReorderMode should disable reorder mode`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.enterReorderMode()
                awaitItem()

                viewModel.exitReorderMode()

                val state = awaitItem()
                assertFalse(state.isReorderMode)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Recipe click should be ignored in reorder mode")
        fun `recipe click should be ignored in reorder mode`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.enterReorderMode()
                awaitItem()

                // Click should be ignored, no navigation event
                viewModel.onRecipeClick("recipe-1")

                // No new state change expected from click
                cancelAndIgnoreRemainingEvents()
            }

            // Navigation event should not be emitted
            viewModel.navigationEvent.test {
                expectNoEvents()
            }
        }
    }

    @Nested
    @DisplayName("Search and Filters")
    inner class SearchAndFilters {

        @Test
        @DisplayName("toggleSearchBar should toggle search visibility")
        fun `toggleSearchBar should toggle search visibility`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial (search hidden)

                viewModel.toggleSearchBar()

                val state = awaitItem()
                assertTrue(state.showSearchBar)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateSearchQuery should update search query")
        fun `updateSearchQuery should update search query`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateSearchQuery("poha")

                val state = awaitItem()
                assertEquals("poha", state.searchQuery)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("setCuisineFilter should set cuisine filter")
        fun `setCuisineFilter should set cuisine filter`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.setCuisineFilter(CuisineType.NORTH)

                val state = awaitItem()
                assertEquals(CuisineType.NORTH, state.selectedCuisineFilter)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("setTimeFilter should set time filter")
        fun `setTimeFilter should set time filter`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.setTimeFilter(TimeFilter.UNDER_30)

                val state = awaitItem()
                assertEquals(TimeFilter.UNDER_30, state.selectedTimeFilter)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("clearFilters should clear all filters")
        fun `clearFilters should clear all filters`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                // Set filters
                viewModel.setCuisineFilter(CuisineType.NORTH)
                awaitItem()
                viewModel.setTimeFilter(TimeFilter.UNDER_30)
                awaitItem()
                viewModel.updateSearchQuery("test")
                awaitItem()

                // Clear all
                viewModel.clearFilters()

                val state = awaitItem()
                assertNull(state.selectedCuisineFilter)
                assertNull(state.selectedTimeFilter)
                assertEquals("", state.searchQuery)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        @DisplayName("onRecipeClick should emit navigation event")
        fun `onRecipeClick should emit navigation event`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.navigationEvent.test {
                viewModel.onRecipeClick("recipe-1")
                val event = awaitItem()
                assertTrue(event is FavoritesNavigationEvent.NavigateToRecipeDetail)
                assertEquals("recipe-1", (event as FavoritesNavigationEvent.NavigateToRecipeDetail).recipeId)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateBack should emit back event")
        fun `navigateBack should emit back event`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateBack()
                val event = awaitItem()
                assertEquals(FavoritesNavigationEvent.NavigateBack, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToHome should emit home event")
        fun `navigateToHome should emit home event`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateToHome()
                val event = awaitItem()
                assertEquals(FavoritesNavigationEvent.NavigateToHome, event)
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
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

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
    @DisplayName("Filtered Recipes")
    inner class FilteredRecipes {

        @Test
        @DisplayName("filteredRecipes should filter by search query")
        fun `filteredRecipes should filter by search query`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.updateSearchQuery("Poha")

                val state = awaitItem()
                assertEquals(1, state.filteredRecipes.size)
                assertEquals("Poha", state.filteredRecipes[0].name)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("filteredRecipes should filter by cuisine")
        fun `filteredRecipes should filter by cuisine`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.setCuisineFilter(CuisineType.NORTH)

                val state = awaitItem()
                assertEquals(1, state.filteredRecipes.size)
                assertEquals("Dal Tadka", state.filteredRecipes[0].name)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("filteredRecipes should filter by time")
        fun `filteredRecipes should filter by time`() = runTest {
            val viewModel = FavoritesViewModel(mockFavoritesRepository, mockRecipeRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.setTimeFilter(TimeFilter.UNDER_15)

                val state = awaitItem()
                // Only Poha (15 min total) should match UNDER_15
                assertEquals(1, state.filteredRecipes.size)
                assertEquals("Poha", state.filteredRecipes[0].name)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
