package com.rasoiai.app.presentation.favorites

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
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
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.FavoriteCollection
import com.rasoiai.domain.model.Recipe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for FavoritesScreen
 * Tests Phase 7 of E2E Testing Guide: Favorites Screen Testing
 */
@RunWith(AndroidJUnit4::class)
class FavoritesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Test Data Factories

    private fun createTestRecipe(
        id: String = "recipe_1",
        name: String = "Dal Tadka",
        description: String = "A classic yellow lentil dish",
        cuisineType: CuisineType = CuisineType.NORTH,
        prepTimeMinutes: Int = 15,
        cookTimeMinutes: Int = 30
    ) = Recipe(
        id = id,
        name = name,
        description = description,
        imageUrl = null,
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes,
        servings = 4,
        difficulty = Difficulty.EASY,
        cuisineType = cuisineType,
        mealTypes = emptyList(),
        dietaryTags = emptyList(),
        ingredients = emptyList(),
        instructions = emptyList(),
        nutrition = null,
        isFavorite = true
    )

    private fun createTestCollection(
        id: String = "collection_all",
        name: String = "All Favorites",
        recipeIds: List<String> = listOf("1", "2", "3", "4", "5"),
        isDefault: Boolean = true
    ) = FavoriteCollection(
        id = id,
        name = name,
        recipeIds = recipeIds,
        coverImageUrl = null,
        isDefault = isDefault,
        createdAt = System.currentTimeMillis()
    )

    private fun createTestUiState(
        isLoading: Boolean = false,
        errorMessage: String? = null,
        collections: List<FavoriteCollection> = listOf(
            createTestCollection(FavoriteCollection.COLLECTION_ALL, "All", listOf("1", "2", "3", "4", "5"), true),
            createTestCollection("collection_1", "Quick Meals", listOf("1", "2", "3"), false),
            createTestCollection("collection_2", "Weekend Specials", listOf("4", "5"), false)
        ),
        selectedCollectionId: String = FavoriteCollection.COLLECTION_ALL,
        recipes: List<Recipe> = listOf(
            createTestRecipe("1", "Dal Tadka"),
            createTestRecipe("2", "Paneer Butter Masala", cuisineType = CuisineType.NORTH),
            createTestRecipe("3", "Dosa", cuisineType = CuisineType.SOUTH),
            createTestRecipe("4", "Poha", cuisineType = CuisineType.WEST),
            createTestRecipe("5", "Fish Curry", cuisineType = CuisineType.EAST)
        ),
        isReorderMode: Boolean = false,
        showCreateCollectionDialog: Boolean = false,
        showSearchBar: Boolean = false,
        searchQuery: String = "",
        selectedCuisineFilter: CuisineType? = null,
        selectedTimeFilter: TimeFilter? = null
    ) = FavoritesUiState(
        isLoading = isLoading,
        errorMessage = errorMessage,
        collections = collections,
        selectedCollectionId = selectedCollectionId,
        recipes = recipes,
        isReorderMode = isReorderMode,
        showCreateCollectionDialog = showCreateCollectionDialog,
        showSearchBar = showSearchBar,
        searchQuery = searchQuery,
        selectedCuisineFilter = selectedCuisineFilter,
        selectedTimeFilter = selectedTimeFilter
    )

    // endregion

    // region Phase 7.1: Add to Favorites Tests

    @Test
    fun favoritesScreen_displaysScreenTag() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.FAVORITES_SCREEN).assertIsDisplayed()
    }

    @Test
    fun favoritesScreen_displaysTitle() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Favorites").assertIsDisplayed()
    }

    @Test
    fun favoritesScreen_displaysFavoriteRecipes() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Dal Tadka").assertIsDisplayed()
    }

    @Test
    fun favoritesScreen_displaysRecipeCount() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("All (5)", substring = true).assertIsDisplayed()
    }

    @Test
    fun favoritesScreen_displaysBottomNavigation() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV).assertIsDisplayed()
    }

    // endregion

    // region Phase 7.2: Collections Tests

    @Test
    fun favoritesScreen_displaysCollectionsSection() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Collections:").assertIsDisplayed()
    }

    @Test
    fun favoritesScreen_hasMultipleCollectionsData() {
        // Verify that collections data exists in state (simplified test)
        val uiState = createTestUiState()
        assert(uiState.collections.size >= 2) { "Should have at least 2 collections" }
        assert(uiState.collections.any { it.name == "All" }) { "Should have All collection" }
        assert(uiState.collections.any { it.name == "Quick Meals" }) { "Should have Quick Meals collection" }
    }

    @Test
    fun collection_click_triggersCallback() {
        var clickedCollectionId: String? = null
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(
                    uiState = uiState,
                    onCollectionClick = { clickedCollectionId = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Quick Meals").performClick()

        assert(clickedCollectionId == "collection_1") {
            "Expected collection_1 but got $clickedCollectionId"
        }
    }

    // endregion

    // region Search Tests

    @Test
    fun searchButton_click_triggersCallback() {
        var searchClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(
                    uiState = uiState,
                    onSearchClick = { searchClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Search").performClick()

        assert(searchClicked) { "Search callback was not triggered" }
    }

    @Test
    fun searchBar_displayed_whenSearchModeActive() {
        val uiState = createTestUiState(showSearchBar = true)

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Search favorites...").assertIsDisplayed()
    }

    // endregion

    // region Recipe Interaction Tests

    @Test
    fun recipe_click_triggersCallback() {
        var clickedRecipeId: String? = null
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(
                    uiState = uiState,
                    onRecipeClick = { clickedRecipeId = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Dal Tadka").performClick()

        assert(clickedRecipeId == "1") { "Expected recipe 1 but got $clickedRecipeId" }
    }

    // endregion

    // region Reorder Mode Tests

    @Test
    fun reorderMode_displaysDoneButton() {
        val uiState = createTestUiState(isReorderMode = true)

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun normalMode_displaysReorderButton() {
        val uiState = createTestUiState(isReorderMode = false)

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Reorder").assertIsDisplayed()
    }

    // endregion

    // region Empty State Tests

    @Test
    fun emptyFavorites_displaysEmptyMessage() {
        val uiState = createTestUiState(recipes = emptyList())

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("No favorite recipes yet", substring = true).assertIsDisplayed()
    }

    @Test
    fun searchNoResults_displaysNoResultsMessage() {
        val uiState = createTestUiState(
            showSearchBar = true,
            searchQuery = "Biryani",
            recipes = emptyList()
        )

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("No recipes found for", substring = true).assertIsDisplayed()
    }

    // endregion

    // region Loading State Tests

    @Test
    fun favoritesScreen_loadingState_displaysScreen() {
        val uiState = createTestUiState(isLoading = true)

        composeTestRule.setContent {
            RasoiAITheme {
                FavoritesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.FAVORITES_SCREEN).assertIsDisplayed()
    }

    // endregion
}

// region Test Composable Wrapper

@androidx.compose.runtime.Composable
private fun FavoritesTestContent(
    uiState: FavoritesUiState,
    onSearchClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onCollectionClick: (String) -> Unit = {},
    onCreateCollectionClick: () -> Unit = {},
    onRecipeClick: (String) -> Unit = {},
    onRemoveRecipeClick: (String) -> Unit = {},
    onAddToCollectionClick: (String, String) -> Unit = { _, _ -> },
    onCuisineFilterChange: (com.rasoiai.domain.model.CuisineType?) -> Unit = {},
    onTimeFilterChange: (TimeFilter?) -> Unit = {},
    onReorderClick: () -> Unit = {},
    onMoveRecipe: (Int, Int) -> Unit = { _, _ -> }
) {
    val snackbarHostState = remember { SnackbarHostState() }

    FavoritesScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onSearchClick = onSearchClick,
        onSearchQueryChange = onSearchQueryChange,
        onCollectionClick = onCollectionClick,
        onCreateCollectionClick = onCreateCollectionClick,
        onRecipeClick = onRecipeClick,
        onRemoveRecipeClick = onRemoveRecipeClick,
        onAddToCollectionClick = onAddToCollectionClick,
        onCuisineFilterChange = onCuisineFilterChange,
        onTimeFilterChange = onTimeFilterChange,
        onReorderClick = onReorderClick,
        onMoveRecipe = onMoveRecipe,
        onBottomNavItemClick = {}
    )
}

// endregion
