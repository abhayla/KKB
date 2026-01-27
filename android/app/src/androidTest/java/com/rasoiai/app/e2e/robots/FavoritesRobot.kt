package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.presentation.common.TestTags

/**
 * Robot for Favorites screen interactions.
 * Handles favorites list and collections.
 */
class FavoritesRobot(private val composeTestRule: ComposeContentTestRule) {

    /**
     * Wait for favorites screen to be displayed.
     */
    fun waitForFavoritesScreen(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(TestTags.FAVORITES_SCREEN, timeoutMillis)
    }

    /**
     * Assert favorites screen is displayed.
     */
    fun assertFavoritesScreenDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.FAVORITES_SCREEN).assertIsDisplayed()
    }

    // ===================== Favorites List =====================

    /**
     * Assert favorites list is displayed.
     */
    fun assertFavoritesListDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.FAVORITES_LIST).assertIsDisplayed()
    }

    /**
     * Assert favorite recipe is displayed.
     */
    fun assertFavoriteRecipeDisplayed(recipeName: String) = apply {
        composeTestRule.onNodeWithText(recipeName, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert favorites count.
     */
    fun assertFavoritesCount(expectedCount: Int) = apply {
        composeTestRule.onNodeWithText("$expectedCount", substring = true).assertIsDisplayed()
    }

    /**
     * Tap on favorite recipe.
     */
    fun tapFavoriteRecipe(recipeName: String) = apply {
        composeTestRule.onNodeWithText(recipeName, substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Remove recipe from favorites.
     */
    fun removeFavorite(recipeName: String) = apply {
        // Usually long press or swipe to delete
        composeTestRule.onNodeWithText(recipeName, substring = true)
            .performScrollTo()
            .performClick() // Might show options
        composeTestRule.onNodeWithText("Remove", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Empty State =====================

    /**
     * Assert empty state is displayed.
     */
    fun assertEmptyStateDisplayed() = apply {
        composeTestRule.onNodeWithText("No favorites", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Collections =====================

    /**
     * Tap create collection button.
     */
    fun tapCreateCollection() = apply {
        composeTestRule.onNodeWithText("Create Collection", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Enter collection name.
     */
    fun enterCollectionName(name: String) = apply {
        composeTestRule.onNodeWithText("Collection Name", substring = true, ignoreCase = true)
            .performTextInput(name)
    }

    /**
     * Save collection.
     */
    fun saveCollection() = apply {
        composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Create a new collection.
     */
    fun createCollection(name: String) = apply {
        tapCreateCollection()
        enterCollectionName(name)
        saveCollection()
    }

    /**
     * Assert collection is displayed.
     */
    fun assertCollectionDisplayed(collectionName: String) = apply {
        composeTestRule.onNodeWithText(collectionName, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Tap on collection.
     */
    fun tapCollection(collectionName: String) = apply {
        composeTestRule.onNodeWithText(collectionName, substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Delete collection.
     */
    fun deleteCollection(collectionName: String) = apply {
        composeTestRule.onNodeWithText(collectionName, substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("Delete", ignoreCase = true).performClick()
        composeTestRule.onNodeWithText("Confirm", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Move recipe to collection.
     */
    fun moveToCollection(recipeName: String, collectionName: String) = apply {
        composeTestRule.onNodeWithText(recipeName, substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("Move to Collection", ignoreCase = true).performClick()
        composeTestRule.onNodeWithText(collectionName, substring = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Tabs =====================

    /**
     * Switch to All Favorites tab.
     */
    fun selectAllFavoritesTab() = apply {
        composeTestRule.onNodeWithText("All", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Switch to Collections tab.
     */
    fun selectCollectionsTab() = apply {
        composeTestRule.onNodeWithText("Collections", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }
}
