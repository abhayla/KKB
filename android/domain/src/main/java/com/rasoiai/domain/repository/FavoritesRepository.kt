package com.rasoiai.domain.repository

import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.FavoriteCollection
import com.rasoiai.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    /**
     * Get all collections including default ones (All, Recently Viewed)
     */
    fun getCollections(): Flow<List<FavoriteCollection>>

    /**
     * Get a specific collection by ID
     */
    fun getCollectionById(collectionId: String): Flow<FavoriteCollection?>

    /**
     * Get all favorite recipes
     */
    fun getAllFavoriteRecipes(): Flow<List<Recipe>>

    /**
     * Get recipes in a specific collection
     */
    fun getRecipesInCollection(collectionId: String): Flow<List<Recipe>>

    /**
     * Get recently viewed recipes
     */
    fun getRecentlyViewedRecipes(): Flow<List<Recipe>>

    /**
     * Add recipe to recently viewed
     */
    suspend fun addToRecentlyViewed(recipeId: String): Result<Unit>

    /**
     * Create a new collection
     */
    suspend fun createCollection(name: String): Result<FavoriteCollection>

    /**
     * Delete a collection (cannot delete default collections)
     */
    suspend fun deleteCollection(collectionId: String): Result<Unit>

    /**
     * Add recipe to a collection
     */
    suspend fun addRecipeToCollection(recipeId: String, collectionId: String): Result<Unit>

    /**
     * Remove recipe from a collection
     */
    suspend fun removeRecipeFromCollection(recipeId: String, collectionId: String): Result<Unit>

    /**
     * Remove recipe from all favorites
     */
    suspend fun removeFromFavorites(recipeId: String): Result<Unit>

    /**
     * Reorder recipes in a collection
     */
    suspend fun reorderRecipes(collectionId: String, recipeIds: List<String>): Result<Unit>

    /**
     * Filter recipes by cuisine and time
     */
    fun filterRecipes(
        collectionId: String,
        cuisine: CuisineType?,
        maxTimeMinutes: Int?
    ): Flow<List<Recipe>>
}
