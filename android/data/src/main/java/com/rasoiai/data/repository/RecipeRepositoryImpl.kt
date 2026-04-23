package com.rasoiai.data.repository

import android.database.sqlite.SQLiteException
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.dao.FavoriteDao
import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.dao.RecipeRulesDao
import com.rasoiai.data.local.entity.KnownIngredientEntity
import com.rasoiai.data.local.entity.FavoriteEntity
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.RecipeRatingRequest
import com.rasoiai.data.remote.mapper.toDomain
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.repository.RecipeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of RecipeRepository with offline-first architecture.
 *
 * Strategy:
 * - Always return data from local Room database (single source of truth)
 * - Fetch from API when online and cache to Room
 * - Favorites are stored locally with sync support
 */
@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val apiService: RasoiApiService,
    private val recipeDao: RecipeDao,
    private val favoriteDao: FavoriteDao,
    private val recipeRulesDao: RecipeRulesDao,
    private val networkMonitor: NetworkMonitor
) : RecipeRepository {

    override fun getRecipeById(id: String): Flow<Recipe?> {
        return recipeDao.getRecipeById(id).map { entity ->
            if (entity != null) {
                entity.toDomain()
            } else {
                // Try to fetch from API if online
                if (networkMonitor.isOnline.first()) {
                    fetchAndCacheRecipe(id)
                }
                null
            }
        }
    }

    override fun getRecipesByIds(ids: List<String>): Flow<List<Recipe>> {
        return recipeDao.getRecipesByIds(ids).map { entities ->
            // Check for missing recipes and fetch them
            val cachedIds = entities.map { it.id }.toSet()
            val missingIds = ids.filter { it !in cachedIds }

            if (missingIds.isNotEmpty() && networkMonitor.isOnline.first()) {
                // Fetch missing recipes in background — fetchAndCacheRecipe handles
                // its own known errors (returns null). Unexpected exceptions propagate
                // per issue #34 ("unexpected exceptions still crash for debugging").
                missingIds.forEach { id -> fetchAndCacheRecipe(id) }
            }

            entities.map { it.toDomain() }
        }
    }

    override suspend fun searchRecipes(
        query: String?,
        cuisine: CuisineType?,
        dietary: DietaryTag?,
        mealType: MealType?,
        page: Int,
        limit: Int
    ): Result<List<Recipe>> {
        return try {
            if (!networkMonitor.isOnline.first()) {
                // Return cached results when offline
                Timber.d("Offline - returning cached search results")
                val cached = searchLocalRecipes(query, cuisine, dietary, mealType, limit)
                return Result.success(cached)
            }

            Timber.d("Searching recipes: query=$query, cuisine=$cuisine, dietary=$dietary")

            val response = apiService.searchRecipes(
                query = query ?: "",
                cuisine = cuisine?.value,
                dietary = dietary?.value,
                mealType = mealType?.value,
                page = page,
                limit = limit
            )

            // Cache search results
            val entities = response.map { dto ->
                val isFavorite = favoriteDao.isFavoriteSync(dto.id)
                dto.toEntity(isFavorite)
            }
            recipeDao.insertRecipes(entities)

            Timber.i("Cached ${entities.size} recipes from search")

            // Persist ingredient names for Recipe Rules search
            val recipes = response.map { it.toDomain() }
            persistIngredientNames(recipes)

            Result.success(recipes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "HTTP ${e.code()} on search recipes")
            try {
                val cached = searchLocalRecipes(query, cuisine, dietary, mealType, limit)
                Result.success(cached)
            } catch (e2: CancellationException) {
                throw e2
            } catch (e2: SQLiteException) {
                Result.failure(e)
            }
        } catch (e: IOException) {
            Timber.w(e, "Network error on search recipes")
            try {
                val cached = searchLocalRecipes(query, cuisine, dietary, mealType, limit)
                Result.success(cached)
            } catch (e2: CancellationException) {
                throw e2
            } catch (e2: SQLiteException) {
                Result.failure(e)
            }
        }
        // Unexpected exceptions propagate per issue #34 — broad catch removed.
    }

    override suspend fun scaleRecipe(recipeId: String, servings: Int): Result<Recipe> {
        return try {
            if (!networkMonitor.isOnline.first()) {
                // Scale locally if offline
                val entity = recipeDao.getRecipeByIdSync(recipeId)
                    ?: return Result.failure(Exception("Recipe not found"))

                val recipe = entity.toDomain()
                val scaledRecipe = scaleRecipeLocally(recipe, servings)
                return Result.success(scaledRecipe)
            }

            Timber.d("Scaling recipe: $recipeId to $servings servings")

            val response = apiService.scaleRecipe(recipeId, servings)

            // Cache scaled recipe
            val isFavorite = favoriteDao.isFavoriteSync(recipeId)
            recipeDao.insertRecipe(response.toEntity(isFavorite))

            Result.success(response.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "HTTP ${e.code()} on scale recipe")
            try {
                val entity = recipeDao.getRecipeByIdSync(recipeId)
                    ?: return Result.failure(e)
                val recipe = entity.toDomain()
                val scaledRecipe = scaleRecipeLocally(recipe, servings)
                Result.success(scaledRecipe)
            } catch (e2: CancellationException) {
                throw e2
            } catch (e2: SQLiteException) {
                Result.failure(e)
            }
        } catch (e: IOException) {
            Timber.w(e, "Network error on scale recipe")
            try {
                val entity = recipeDao.getRecipeByIdSync(recipeId)
                    ?: return Result.failure(e)
                val recipe = entity.toDomain()
                val scaledRecipe = scaleRecipeLocally(recipe, servings)
                Result.success(scaledRecipe)
            } catch (e2: CancellationException) {
                throw e2
            } catch (e2: SQLiteException) {
                Result.failure(e)
            }
        }
        // Unexpected exceptions propagate per issue #34 — broad catch removed.
    }

    override suspend fun toggleFavorite(recipeId: String): Result<Boolean> {
        return try {
            val currentlyFavorite = favoriteDao.isFavoriteSync(recipeId)
            val newFavoriteState = !currentlyFavorite

            Timber.d("Toggling favorite: $recipeId -> $newFavoriteState")

            if (newFavoriteState) {
                // Add to favorites
                favoriteDao.insertFavorite(FavoriteEntity(recipeId = recipeId))
            } else {
                // Remove from favorites
                favoriteDao.deleteFavorite(recipeId)
            }

            // Update recipe's isFavorite flag
            recipeDao.updateFavoriteStatus(recipeId, newFavoriteState)

            Timber.i("Favorite toggled: $recipeId = $newFavoriteState")
            Result.success(newFavoriteState)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteException) {
            Timber.e(e, "Failed to toggle favorite")
            Result.failure(e)
        }
    }

    override fun getFavoriteRecipes(): Flow<List<Recipe>> {
        return recipeDao.getFavoriteRecipes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Fetch recipe from API and cache locally.
     */
    private suspend fun fetchAndCacheRecipe(recipeId: String): Recipe? {
        return try {
            val response = apiService.getRecipeById(recipeId)
            val isFavorite = favoriteDao.isFavoriteSync(recipeId)
            recipeDao.insertRecipe(response.toEntity(isFavorite))
            Timber.d("Cached recipe: $recipeId")
            val recipe = response.toDomain().copy(isFavorite = isFavorite)
            persistIngredientNames(listOf(recipe))
            recipe
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "HTTP ${e.code()} fetching recipe from API: $recipeId")
            null
        } catch (e: IOException) {
            Timber.w(e, "Network error fetching recipe from API: $recipeId")
            null
        } catch (e: SQLiteException) {
            Timber.e(e, "Failed to cache recipe locally: $recipeId")
            null
        }
        // Unexpected exceptions propagate per issue #34 — broad catch removed.
    }

    /**
     * Search recipes locally when offline.
     * Note: mealType filter intentionally not used - users can add any recipe to any meal slot.
     */
    private suspend fun searchLocalRecipes(
        query: String?,
        cuisine: CuisineType?,
        dietary: DietaryTag?,
        mealType: MealType?,
        limit: Int
    ): List<Recipe> {
        // For now, just return recipes by cuisine if specified, or all recipes
        // A more sophisticated local search would use FTS
        // Note: mealType is intentionally ignored - any recipe can go in any meal slot
        val recipes = when {
            cuisine != null -> recipeDao.getRecipesByCuisine(cuisine.value).first()
            else -> recipeDao.getAllRecipes().first()
        }
        return recipes.take(limit).map { it.toDomain() }
    }

    /**
     * Scale recipe ingredients locally.
     */
    private fun scaleRecipeLocally(recipe: Recipe, targetServings: Int): Recipe {
        val scaleFactor = targetServings.toDouble() / recipe.servings.toDouble()

        val scaledIngredients = recipe.ingredients.map { ingredient ->
            val originalQty = ingredient.quantity.toDoubleOrNull()
            val scaledQty = originalQty?.let { it * scaleFactor }
            ingredient.copy(
                quantity = scaledQty?.let { formatQuantity(it) } ?: ingredient.quantity
            )
        }

        return recipe.copy(
            servings = targetServings,
            ingredients = scaledIngredients
        )
    }

    /**
     * Persist ingredient names from recipes into known_ingredients table
     * for Recipe Rules search auto-population.
     */
    private suspend fun persistIngredientNames(recipes: List<Recipe>) {
        try {
            val ingredientNames = recipes
                .flatMap { it.ingredients.map { ing -> ing.name } }
                .distinct()
            if (ingredientNames.isEmpty()) return

            val entities = ingredientNames.map {
                KnownIngredientEntity(name = it, source = "recipe_cache")
            }
            recipeRulesDao.insertKnownIngredients(entities)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteException) {
            Timber.w(e, "Failed to persist ingredient names")
        }
    }

    override suspend fun prefetchRecipes(recipeIds: List<String>) {
        val cached = recipeDao.getRecipesByIdsSync(recipeIds).map { it.id }.toSet()
        val missing = recipeIds.filter { it !in cached }
        if (missing.isEmpty()) {
            Timber.d("prefetchRecipes: all ${recipeIds.size} recipes already cached")
            return
        }
        Timber.d("prefetchRecipes: ${missing.size}/${recipeIds.size} recipes need fetching")
        // fetchAndCacheRecipe handles its own known errors (returns null). Unexpected
        // exceptions propagate per issue #34 ("unexpected exceptions still crash").
        missing.forEach { id -> fetchAndCacheRecipe(id) }
    }

    override suspend fun rateRecipe(recipeId: String, rating: Int, feedback: String): Result<Unit> {
        return try {
            if (networkMonitor.isOnline.first()) {
                apiService.rateRecipe(recipeId, RecipeRatingRequest(rating.toFloat(), feedback.ifBlank { null }))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "HTTP ${e.code()} on submit recipe rating")
            Result.failure(e)
        } catch (e: IOException) {
            Timber.w(e, "Network error on submit recipe rating")
            Result.failure(e)
        }
        // Unexpected exceptions propagate per issue #34 — broad catch removed.
    }

    /**
     * Format quantity with appropriate precision.
     */
    private fun formatQuantity(value: Double): String {
        return when {
            value == value.toLong().toDouble() -> value.toLong().toString()
            value < 1 -> String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
            else -> String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
        }
    }
}
