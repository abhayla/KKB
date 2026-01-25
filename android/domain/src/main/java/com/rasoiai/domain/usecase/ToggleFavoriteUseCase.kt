package com.rasoiai.domain.usecase

import com.rasoiai.domain.repository.RecipeRepository
import javax.inject.Inject

/**
 * Use case for toggling a recipe's favorite status.
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val recipeRepository: RecipeRepository
) {
    /**
     * Toggle favorite status and return the new state.
     * @return true if now favorited, false if unfavorited
     */
    suspend operator fun invoke(recipeId: String): Result<Boolean> {
        return recipeRepository.toggleFavorite(recipeId)
    }
}
