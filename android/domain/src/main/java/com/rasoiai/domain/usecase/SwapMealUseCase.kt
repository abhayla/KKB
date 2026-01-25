package com.rasoiai.domain.usecase

import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.repository.MealPlanRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for swapping a meal item with a new recipe.
 */
class SwapMealUseCase @Inject constructor(
    private val mealPlanRepository: MealPlanRepository
) {
    suspend operator fun invoke(
        mealPlanId: String,
        date: LocalDate,
        mealType: MealType,
        currentRecipeId: String,
        excludeRecipeIds: List<String> = emptyList()
    ): Result<MealPlan> {
        return mealPlanRepository.swapMeal(
            mealPlanId = mealPlanId,
            date = date,
            mealType = mealType,
            currentRecipeId = currentRecipeId,
            excludeRecipeIds = excludeRecipeIds
        )
    }
}
