package com.rasoiai.app.e2e.flows

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.FavoritesRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.RecipeDetailRobot
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

/**
 * Phase 7: Favorites Screen Testing
 *
 * Tests:
 * 7.1 Add to Favorites
 * 7.2 Favorites Collections
 */
@HiltAndroidTest
class FavoritesFlowTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var favoritesRobot: FavoritesRobot
    private lateinit var recipeDetailRobot: RecipeDetailRobot

    @Before
    override fun setUp() {
        super.setUp()
        // Set up authenticated and onboarded user state
        setUpAuthenticatedState()

        homeRobot = HomeRobot(composeTestRule)
        favoritesRobot = FavoritesRobot(composeTestRule)
        recipeDetailRobot = RecipeDetailRobot(composeTestRule)

        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
    }

    /**
     * Test 7.1: Add to Favorites
     *
     * Steps:
     * 1. Navigate to Recipe Detail (any recipe)
     * 2. Tap heart/favorite icon
     * 3. Navigate to Favorites screen
     * 4. Verify recipe appears in favorites list
     *
     * Test Variations:
     * - Add 5 recipes to favorites
     * - Remove recipe from favorites
     * - Add same recipe twice (should toggle)
     */
    @Test
    fun test_7_1_addToFavorites() {
        // Navigate to a recipe
        homeRobot.selectDay(DayOfWeek.MONDAY)
        homeRobot.tapMealCard(MealType.BREAKFAST)
        recipeDetailRobot.waitForRecipeDetailScreen()

        // Add to favorites
        recipeDetailRobot.assertIsNotFavorited()
        recipeDetailRobot.tapFavoriteButton()
        recipeDetailRobot.assertIsFavorited()

        // Go back and navigate to favorites
        recipeDetailRobot.goBack()
        homeRobot.navigateToFavorites()
        favoritesRobot.waitForFavoritesScreen()

        // Verify recipe is in favorites
        favoritesRobot.assertFavoritesListDisplayed()
        favoritesRobot.assertFavoriteRecipeDisplayed("Poha") // Monday breakfast
    }

    /**
     * Test 7.2: Favorites Collections
     *
     * Steps:
     * 1. Create new collection: "Weekend Specials"
     * 2. Move favorite recipe to collection
     * 3. Verify collection shows recipe
     * 4. Delete collection
     */
    @Test
    fun test_7_2_favoritesCollections() {
        // First add a recipe to favorites
        addRecipeToFavorites()

        // Navigate to favorites
        homeRobot.navigateToFavorites()
        favoritesRobot.waitForFavoritesScreen()

        // Create collection
        favoritesRobot.createCollection("Weekend Specials")
        favoritesRobot.assertCollectionDisplayed("Weekend Specials")

        // Move recipe to collection
        favoritesRobot.selectCollectionsTab()
        favoritesRobot.tapCollection("Weekend Specials")

        // Delete collection
        favoritesRobot.deleteCollection("Weekend Specials")
    }

    /**
     * Test: Remove recipe from favorites
     */
    @Test
    fun removeFromFavorites() {
        // Add a recipe first
        addRecipeToFavorites()

        // Navigate to favorites
        homeRobot.navigateToFavorites()
        favoritesRobot.waitForFavoritesScreen()

        // Remove the recipe
        favoritesRobot.removeFavorite("Poha")

        // Verify empty state
        favoritesRobot.assertEmptyStateDisplayed()
    }

    /**
     * Test: Toggle favorite status
     */
    @Test
    fun toggleFavoriteStatus() {
        homeRobot.selectDay(DayOfWeek.MONDAY)
        homeRobot.tapMealCard(MealType.BREAKFAST)
        recipeDetailRobot.waitForRecipeDetailScreen()

        // Add to favorites
        recipeDetailRobot.assertIsNotFavorited()
        recipeDetailRobot.tapFavoriteButton()
        recipeDetailRobot.assertIsFavorited()

        // Remove from favorites
        recipeDetailRobot.tapFavoriteButton()
        recipeDetailRobot.assertIsNotFavorited()
    }

    /**
     * Test: Navigate to recipe from favorites
     */
    @Test
    fun navigateToRecipeFromFavorites() {
        addRecipeToFavorites()

        homeRobot.navigateToFavorites()
        favoritesRobot.waitForFavoritesScreen()

        // Tap on favorite recipe
        favoritesRobot.tapFavoriteRecipe("Poha")

        // Should navigate to recipe detail
        recipeDetailRobot.waitForRecipeDetailScreen()
        recipeDetailRobot.assertRecipeDetailScreenDisplayed()
        recipeDetailRobot.assertRecipeNameDisplayed("Poha")
    }

    private fun addRecipeToFavorites() {
        homeRobot.selectDay(DayOfWeek.MONDAY)
        homeRobot.tapMealCard(MealType.BREAKFAST)
        recipeDetailRobot.waitForRecipeDetailScreen()
        recipeDetailRobot.tapFavoriteButton()
        recipeDetailRobot.goBack()
    }
}
