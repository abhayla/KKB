package com.rasoiai.app.e2e.flows

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.FavoritesRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

/**
 * Requirement: #40 - FR-002: Auto-Add Recipe to Favorites from Suggestions Tab
 *
 * Tests the auto-favorite functionality when adding recipes from the Add Recipe sheet.
 *
 * Feature Description:
 * - When a user adds a recipe from the Suggestions tab, it is automatically added to favorites
 * - When a user adds a recipe from the Favorites tab, no duplicate favorite action occurs
 * - A snackbar confirmation shows "{Recipe Name} added to favorites"
 *
 * @see <a href="https://github.com/abhayla/KKB/issues/40">GitHub Issue #40</a>
 */
@HiltAndroidTest
class AutoFavoriteOnAddRecipeTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var favoritesRobot: FavoritesRobot

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedState()

        homeRobot = HomeRobot(composeTestRule)
        favoritesRobot = FavoritesRobot(composeTestRule)

        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
        homeRobot.waitForMealListToLoad(LONG_TIMEOUT)
    }

    /**
     * Test: Add recipe from Suggestions tab auto-adds to favorites
     *
     * Steps:
     * 1. Open Add Recipe sheet for Breakfast
     * 2. Verify Suggestions tab is selected by default
     * 3. Select a recipe from the Suggestions grid
     * 4. Verify snackbar shows "added to favorites"
     * 5. Navigate to Favorites screen
     * 6. Verify recipe appears in favorites list
     */
    @Test
    fun addRecipeFromSuggestions_autoAddsToFavorites() {
        // Select a day with meals
        homeRobot.selectDay(DayOfWeek.MONDAY)

        // Open Add Recipe sheet for Breakfast
        homeRobot.tapAddRecipeButton(MealType.BREAKFAST)
        homeRobot.assertAddRecipeSheetDisplayed()

        // Suggestions tab should be selected by default
        // Wait for recipes to load
        homeRobot.waitForAddRecipeGridLoaded()

        // Select first recipe from Suggestions tab
        homeRobot.selectFirstRecipeFromAddRecipeGrid()

        // Verify snackbar shows "added to favorites"
        homeRobot.assertFavoriteAddedSnackbarDisplayed()

        // Navigate to Favorites screen
        homeRobot.navigateToFavorites()
        favoritesRobot.waitForFavoritesScreen()

        // Verify favorites list is displayed (recipe was added)
        favoritesRobot.assertFavoritesListDisplayed()
    }

    /**
     * Test: Add recipe from Favorites tab does NOT trigger duplicate favorite action
     *
     * Steps:
     * 1. First add a recipe to favorites (via Recipe Detail)
     * 2. Open Add Recipe sheet
     * 3. Switch to Favorites tab
     * 4. Select a recipe from Favorites grid
     * 5. Verify NO "added to favorites" snackbar appears
     */
    @Test
    fun addRecipeFromFavorites_doesNotShowFavoriteSnackbar() {
        // First, we need a recipe in favorites
        // Add a recipe to favorites via the first test flow
        homeRobot.selectDay(DayOfWeek.MONDAY)

        // Add a recipe from Suggestions first to populate favorites
        homeRobot.tapAddRecipeButton(MealType.LUNCH)
        homeRobot.assertAddRecipeSheetDisplayed()
        homeRobot.waitForAddRecipeGridLoaded()
        homeRobot.selectFirstRecipeFromAddRecipeGrid()

        // Wait for snackbar and action to complete
        homeRobot.assertFavoriteAddedSnackbarDisplayed()
        Thread.sleep(2000) // Wait for snackbar to dismiss

        // Now open Add Recipe sheet again for a different meal
        homeRobot.tapAddRecipeButton(MealType.DINNER)
        homeRobot.assertAddRecipeSheetDisplayed()

        // Switch to Favorites tab
        homeRobot.tapAddRecipeFavoritesTab()
        Thread.sleep(500) // Wait for tab switch

        // Wait for favorites to load (should have at least one from previous step)
        homeRobot.waitForAddRecipeGridLoaded()

        // Select the recipe from Favorites tab
        homeRobot.selectFirstRecipeFromAddRecipeGrid()

        // Verify NO "added to favorites" snackbar appears
        // (because the recipe is already a favorite)
        homeRobot.assertFavoriteAddedSnackbarNotDisplayed()
    }

    /**
     * Test: Verify recipe added from Suggestions appears in Favorites screen
     *
     * This is an integration test that verifies the complete flow:
     * 1. Add recipe from Suggestions
     * 2. Verify it persists in Favorites screen
     */
    @Test
    fun addedRecipeFromSuggestions_persistsInFavoritesScreen() {
        homeRobot.selectDay(DayOfWeek.TUESDAY)

        // Open Add Recipe sheet for Snacks
        homeRobot.tapAddRecipeButton(MealType.SNACKS)
        homeRobot.assertAddRecipeSheetDisplayed()
        homeRobot.waitForAddRecipeGridLoaded()

        // Select a recipe
        homeRobot.selectFirstRecipeFromAddRecipeGrid()

        // Verify snackbar
        homeRobot.assertFavoriteAddedSnackbarDisplayed()

        // Navigate to Favorites
        homeRobot.navigateToFavorites()
        favoritesRobot.waitForFavoritesScreen()

        // Favorites should not be empty
        favoritesRobot.assertFavoritesListDisplayed()

        // Navigate back to Home
        homeRobot.navigateToHome()
        homeRobot.waitForHomeScreen()

        // Navigate to Favorites again to verify persistence
        homeRobot.navigateToFavorites()
        favoritesRobot.waitForFavoritesScreen()
        favoritesRobot.assertFavoritesListDisplayed()
    }
}
