package com.rasoiai.domain.usecase

import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting a recipe by ID.
 */
class GetRecipeUseCase @Inject constructor(
    private val recipeRepository: RecipeRepository
) {
    operator fun invoke(recipeId: String): Flow<Recipe?> {
        return recipeRepository.getRecipeById(recipeId)
    }
}
