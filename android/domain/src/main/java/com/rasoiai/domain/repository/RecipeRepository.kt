package com.rasoiai.domain.repository

import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    /**
     * Get a recipe by its ID.
     */
    fun getRecipeById(id: String): Flow<Recipe?>

    /**
     * Get recipes by their IDs.
     */
    fun getRecipesByIds(ids: List<String>): Flow<List<Recipe>>

    /**
     * Search recipes with filters.
     */
    suspend fun searchRecipes(
        query: String? = null,
        cuisine: CuisineType? = null,
        dietary: DietaryTag? = null,
        mealType: MealType? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<List<Recipe>>

    /**
     * Scale a recipe to different servings.
     */
    suspend fun scaleRecipe(recipeId: String, servings: Int): Result<Recipe>

    /**
     * Toggle favorite status for a recipe.
     */
    suspend fun toggleFavorite(recipeId: String): Result<Boolean>

    /**
     * Get all favorite recipes.
     */
    fun getFavoriteRecipes(): Flow<List<Recipe>>

    /**
     * Submit a rating for a recipe.
     */
    suspend fun rateRecipe(recipeId: String, rating: Int, feedback: String): Result<Unit>
}
