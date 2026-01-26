package com.rasoiai.data.repository

import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.FavoriteCollection
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.repository.FavoritesRepository
import com.rasoiai.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of FavoritesRepository for development and testing.
 */
@Singleton
class FakeFavoritesRepository @Inject constructor(
    private val recipeRepository: RecipeRepository
) : FavoritesRepository {

    private val collections = MutableStateFlow(createDefaultCollections())
    private val recentlyViewedIds = MutableStateFlow(listOf("dal-tadka", "palak-paneer", "dosa"))

    override fun getCollections(): Flow<List<FavoriteCollection>> {
        return combine(collections, recipeRepository.getFavoriteRecipes(), recentlyViewedIds) { cols, favorites, recentIds ->
            cols.map { collection ->
                when (collection.id) {
                    FavoriteCollection.COLLECTION_ALL -> collection.copy(recipeIds = favorites.map { it.id })
                    FavoriteCollection.COLLECTION_RECENTLY_VIEWED -> collection.copy(recipeIds = recentIds)
                    else -> collection
                }
            }
        }
    }

    override fun getCollectionById(collectionId: String): Flow<FavoriteCollection?> {
        return getCollections().map { list -> list.find { it.id == collectionId } }
    }

    override fun getAllFavoriteRecipes(): Flow<List<Recipe>> {
        return recipeRepository.getFavoriteRecipes()
    }

    override fun getRecipesInCollection(collectionId: String): Flow<List<Recipe>> {
        return combine(getCollectionById(collectionId), recipeRepository.getFavoriteRecipes(), recentlyViewedIds) { collection, favorites, recentIds ->
            when (collectionId) {
                FavoriteCollection.COLLECTION_ALL -> favorites
                FavoriteCollection.COLLECTION_RECENTLY_VIEWED -> {
                    // Get recipes from the recipe repository by IDs (including non-favorites)
                    recentIds.mapNotNull { id -> favorites.find { it.id == id } }
                }
                else -> {
                    collection?.recipeIds?.mapNotNull { id ->
                        favorites.find { it.id == id }
                    } ?: emptyList()
                }
            }
        }
    }

    override fun getRecentlyViewedRecipes(): Flow<List<Recipe>> {
        return getRecipesInCollection(FavoriteCollection.COLLECTION_RECENTLY_VIEWED)
    }

    override suspend fun addToRecentlyViewed(recipeId: String): Result<Unit> {
        return try {
            val current = recentlyViewedIds.value.toMutableList()
            current.remove(recipeId)
            current.add(0, recipeId)
            // Keep only last 20
            recentlyViewedIds.value = current.take(20)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createCollection(name: String): Result<FavoriteCollection> {
        return try {
            val newCollection = FavoriteCollection(
                id = "collection-${System.currentTimeMillis()}",
                name = name,
                recipeIds = emptyList(),
                coverImageUrl = null,
                isDefault = false,
                createdAt = System.currentTimeMillis()
            )
            collections.value = collections.value + newCollection
            Result.success(newCollection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCollection(collectionId: String): Result<Unit> {
        return try {
            val collection = collections.value.find { it.id == collectionId }
            if (collection?.isDefault == true) {
                return Result.failure(IllegalArgumentException("Cannot delete default collection"))
            }
            collections.value = collections.value.filter { it.id != collectionId }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addRecipeToCollection(recipeId: String, collectionId: String): Result<Unit> {
        return try {
            collections.value = collections.value.map { collection ->
                if (collection.id == collectionId && recipeId !in collection.recipeIds) {
                    collection.copy(recipeIds = collection.recipeIds + recipeId)
                } else {
                    collection
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeRecipeFromCollection(recipeId: String, collectionId: String): Result<Unit> {
        return try {
            collections.value = collections.value.map { collection ->
                if (collection.id == collectionId) {
                    collection.copy(recipeIds = collection.recipeIds - recipeId)
                } else {
                    collection
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFromFavorites(recipeId: String): Result<Unit> {
        return recipeRepository.toggleFavorite(recipeId).map { }
    }

    override suspend fun reorderRecipes(collectionId: String, recipeIds: List<String>): Result<Unit> {
        return try {
            collections.value = collections.value.map { collection ->
                if (collection.id == collectionId) {
                    collection.copy(recipeIds = recipeIds)
                } else {
                    collection
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun filterRecipes(
        collectionId: String,
        cuisine: CuisineType?,
        maxTimeMinutes: Int?
    ): Flow<List<Recipe>> {
        return getRecipesInCollection(collectionId).map { recipes ->
            recipes.filter { recipe ->
                val matchesCuisine = cuisine == null || recipe.cuisineType == cuisine
                val matchesTime = maxTimeMinutes == null || recipe.totalTimeMinutes <= maxTimeMinutes
                matchesCuisine && matchesTime
            }
        }
    }

    private fun createDefaultCollections(): List<FavoriteCollection> {
        val now = System.currentTimeMillis()
        return listOf(
            FavoriteCollection(
                id = FavoriteCollection.COLLECTION_ALL,
                name = "All",
                recipeIds = emptyList(), // Will be populated dynamically
                coverImageUrl = null,
                isDefault = true,
                createdAt = now
            ),
            FavoriteCollection(
                id = FavoriteCollection.COLLECTION_RECENTLY_VIEWED,
                name = "Recently Viewed",
                recipeIds = emptyList(), // Will be populated dynamically
                coverImageUrl = null,
                isDefault = true,
                createdAt = now
            ),
            FavoriteCollection(
                id = "weekend-specials",
                name = "Weekend Specials",
                recipeIds = listOf("biryani", "malai-kofta", "chole-bhature"),
                coverImageUrl = null,
                isDefault = false,
                createdAt = now
            ),
            FavoriteCollection(
                id = "quick-meals",
                name = "Quick Meals",
                recipeIds = listOf("dal-tadka", "dosa", "idli"),
                coverImageUrl = null,
                isDefault = false,
                createdAt = now
            ),
            FavoriteCollection(
                id = "kids-friendly",
                name = "Kids Friendly",
                recipeIds = listOf("paratha", "paneer-butter-masala"),
                coverImageUrl = null,
                isDefault = false,
                createdAt = now
            )
        )
    }
}
