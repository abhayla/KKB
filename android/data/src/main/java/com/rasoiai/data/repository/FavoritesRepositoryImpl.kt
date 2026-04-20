package com.rasoiai.data.repository

import com.rasoiai.data.local.dao.CollectionDao
import com.rasoiai.data.local.dao.FavoriteDao
import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.entity.FavoriteCollectionEntity
import com.rasoiai.data.local.entity.FavoriteEntity
import com.rasoiai.data.local.entity.RecentlyViewedEntity
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.FavoriteCollection
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.repository.FavoritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import android.database.sqlite.SQLiteConstraintException
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of FavoritesRepository with offline-first architecture.
 *
 * Strategy:
 * - All data stored locally in Room (single source of truth)
 * - Collections and favorites are user-specific local data
 * - Recently viewed tracks recipe access for personalization
 */
@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val collectionDao: CollectionDao,
    private val recipeDao: RecipeDao
) : FavoritesRepository {

    companion object {
        private const val MAX_RECENTLY_VIEWED = 20
        private const val PRUNE_THRESHOLD = 50
    }

    init {
        // Default collections are created on first access if needed
    }

    override fun getCollections(): Flow<List<FavoriteCollection>> {
        return combine(
            collectionDao.getAllCollections(),
            favoriteDao.getAllFavorites()
        ) { collections, favorites ->
            val result = mutableListOf<FavoriteCollection>()

            // Add "All" collection (virtual - contains all favorites)
            val allRecipeIds = favorites.map { it.recipeId }
            result.add(
                FavoriteCollection(
                    id = FavoriteCollection.COLLECTION_ALL,
                    name = "All",
                    recipeIds = allRecipeIds,
                    coverImageUrl = null,
                    isDefault = true,
                    createdAt = 0L
                )
            )

            // Add "Recently Viewed" collection (virtual - from recently_viewed table)
            // This will be populated separately in getRecentlyViewedRecipes()
            result.add(
                FavoriteCollection(
                    id = FavoriteCollection.COLLECTION_RECENTLY_VIEWED,
                    name = "Recently Viewed",
                    recipeIds = emptyList(), // Populated dynamically
                    coverImageUrl = null,
                    isDefault = true,
                    createdAt = 0L
                )
            )

            // Add user-created collections with their recipe IDs
            collections.forEach { entity ->
                val collectionFavorites = favorites
                    .filter { it.collectionId == entity.id }
                    .map { it.recipeId }

                result.add(entity.toDomain(collectionFavorites))
            }

            Timber.d("Loaded ${result.size} collections with ${allRecipeIds.size} total favorites")
            result
        }
    }

    override fun getCollectionById(collectionId: String): Flow<FavoriteCollection?> {
        return when (collectionId) {
            FavoriteCollection.COLLECTION_ALL -> {
                favoriteDao.getAllFavorites().map { favorites ->
                    FavoriteCollection(
                        id = FavoriteCollection.COLLECTION_ALL,
                        name = "All",
                        recipeIds = favorites.map { it.recipeId },
                        coverImageUrl = null,
                        isDefault = true,
                        createdAt = 0L
                    )
                }
            }
            FavoriteCollection.COLLECTION_RECENTLY_VIEWED -> {
                collectionDao.getRecentlyViewedIds(MAX_RECENTLY_VIEWED).map { recipeIds ->
                    FavoriteCollection(
                        id = FavoriteCollection.COLLECTION_RECENTLY_VIEWED,
                        name = "Recently Viewed",
                        recipeIds = recipeIds,
                        coverImageUrl = null,
                        isDefault = true,
                        createdAt = 0L
                    )
                }
            }
            else -> {
                combine(
                    collectionDao.getCollectionById(collectionId),
                    favoriteDao.getFavoritesByCollection(collectionId)
                ) { entity, favorites ->
                    entity?.toDomain(favorites.map { it.recipeId })
                }
            }
        }
    }

    override fun getAllFavoriteRecipes(): Flow<List<Recipe>> {
        return recipeDao.getFavoriteRecipes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecipesInCollection(collectionId: String): Flow<List<Recipe>> {
        return when (collectionId) {
            FavoriteCollection.COLLECTION_ALL -> getAllFavoriteRecipes()
            FavoriteCollection.COLLECTION_RECENTLY_VIEWED -> getRecentlyViewedRecipes()
            else -> {
                favoriteDao.getFavoritesByCollection(collectionId).map { favorites ->
                    val recipeIds = favorites.map { it.recipeId }
                    if (recipeIds.isEmpty()) {
                        emptyList()
                    } else {
                        recipeDao.getRecipesByIds(recipeIds).first().map { it.toDomain() }
                    }
                }
            }
        }
    }

    override fun getRecentlyViewedRecipes(): Flow<List<Recipe>> {
        return collectionDao.getRecentlyViewedIds(MAX_RECENTLY_VIEWED).map { recipeIds ->
            if (recipeIds.isEmpty()) {
                emptyList()
            } else {
                recipeDao.getRecipesByIds(recipeIds).first().map { it.toDomain() }
            }
        }
    }

    override suspend fun addToRecentlyViewed(recipeId: String): Result<Unit> {
        return try {
            Timber.d("Adding to recently viewed: $recipeId")

            // Insert or update the recently viewed entry
            collectionDao.insertRecentlyViewed(
                RecentlyViewedEntity(
                    recipeId = recipeId,
                    viewedAt = System.currentTimeMillis()
                )
            )

            // Prune old entries if needed
            val count = collectionDao.getRecentlyViewedCount()
            if (count > PRUNE_THRESHOLD) {
                collectionDao.pruneRecentlyViewed(MAX_RECENTLY_VIEWED)
                Timber.d("Pruned recently viewed to $MAX_RECENTLY_VIEWED items")
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteConstraintException) {
            Timber.w(e, "Constraint violation adding to recently viewed")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add to recently viewed")
            Result.failure(e)
        }
    }

    override suspend fun createCollection(name: String): Result<FavoriteCollection> {
        return try {
            val collection = FavoriteCollectionEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                coverImageUrl = null,
                order = collectionDao.getCollectionCount(),
                isDefault = false,
                createdAt = System.currentTimeMillis()
            )

            collectionDao.insertCollection(collection)
            Timber.i("Created collection: ${collection.name}")

            Result.success(collection.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteConstraintException) {
            Timber.w(e, "Constraint violation creating collection")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create collection")
            Result.failure(e)
        }
    }

    override suspend fun deleteCollection(collectionId: String): Result<Unit> {
        return try {
            // Cannot delete default collections
            if (collectionId == FavoriteCollection.COLLECTION_ALL ||
                collectionId == FavoriteCollection.COLLECTION_RECENTLY_VIEWED
            ) {
                return Result.failure(IllegalArgumentException("Cannot delete default collections"))
            }

            val collection = collectionDao.getCollectionByIdSync(collectionId)
            if (collection?.isDefault == true) {
                return Result.failure(IllegalArgumentException("Cannot delete default collections"))
            }

            // Move all recipes from this collection to default (null collection)
            val favorites = favoriteDao.getFavoritesByCollection(collectionId).first()
            favorites.forEach { favorite ->
                favoriteDao.moveToCollection(favorite.recipeId, null)
            }

            // Delete the collection
            collectionDao.deleteCollection(collectionId)
            Timber.i("Deleted collection: $collectionId")

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteConstraintException) {
            Timber.w(e, "Constraint violation deleting collection")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete collection")
            Result.failure(e)
        }
    }

    override suspend fun addRecipeToCollection(recipeId: String, collectionId: String): Result<Unit> {
        return try {
            // Can't add to virtual collections
            if (collectionId == FavoriteCollection.COLLECTION_ALL ||
                collectionId == FavoriteCollection.COLLECTION_RECENTLY_VIEWED
            ) {
                return Result.failure(IllegalArgumentException("Cannot add to virtual collections"))
            }

            // Check if already in favorites
            val isFavorite = favoriteDao.isFavoriteSync(recipeId)
            if (!isFavorite) {
                // Add to favorites first
                favoriteDao.insertFavorite(
                    FavoriteEntity(
                        recipeId = recipeId,
                        collectionId = collectionId,
                        addedAt = System.currentTimeMillis()
                    )
                )
                recipeDao.updateFavoriteStatus(recipeId, true)
            } else {
                // Move to new collection
                favoriteDao.moveToCollection(recipeId, collectionId)
            }

            Timber.i("Added recipe $recipeId to collection $collectionId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteConstraintException) {
            Timber.w(e, "Constraint violation adding recipe to collection")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add recipe to collection")
            Result.failure(e)
        }
    }

    override suspend fun removeRecipeFromCollection(recipeId: String, collectionId: String): Result<Unit> {
        return try {
            // Move to default collection (null)
            favoriteDao.moveToCollection(recipeId, null)
            Timber.i("Removed recipe $recipeId from collection $collectionId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteConstraintException) {
            Timber.w(e, "Constraint violation removing recipe from collection")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove recipe from collection")
            Result.failure(e)
        }
    }

    override suspend fun removeFromFavorites(recipeId: String): Result<Unit> {
        return try {
            favoriteDao.deleteFavorite(recipeId)
            recipeDao.updateFavoriteStatus(recipeId, false)
            Timber.i("Removed recipe $recipeId from all favorites")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteConstraintException) {
            Timber.w(e, "Constraint violation removing favorite")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove from favorites")
            Result.failure(e)
        }
    }

    override suspend fun reorderRecipes(collectionId: String, recipeIds: List<String>): Result<Unit> {
        return try {
            recipeIds.forEachIndexed { index, recipeId ->
                favoriteDao.updateOrder(recipeId, index)
            }
            Timber.d("Reordered ${recipeIds.size} recipes in collection $collectionId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteConstraintException) {
            Timber.w(e, "Constraint violation reordering recipes")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to reorder recipes")
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
                val cuisineMatch = cuisine == null || recipe.cuisineType == cuisine
                val timeMatch = maxTimeMinutes == null ||
                        (recipe.prepTimeMinutes + recipe.cookTimeMinutes) <= maxTimeMinutes
                cuisineMatch && timeMatch
            }
        }
    }
}
