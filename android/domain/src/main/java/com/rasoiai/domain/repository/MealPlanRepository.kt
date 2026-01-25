package com.rasoiai.domain.repository

import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface MealPlanRepository {
    /**
     * Get the meal plan for a specific date (finds the week containing that date).
     */
    fun getMealPlanForDate(date: LocalDate): Flow<MealPlan?>

    /**
     * Generate a new meal plan for the week starting from the given date.
     */
    suspend fun generateMealPlan(weekStartDate: LocalDate): Result<MealPlan>

    /**
     * Swap a meal item with a new recipe suggestion.
     */
    suspend fun swapMeal(
        mealPlanId: String,
        date: LocalDate,
        mealType: MealType,
        currentRecipeId: String,
        excludeRecipeIds: List<String> = emptyList()
    ): Result<MealPlan>

    /**
     * Lock or unlock a meal item.
     */
    suspend fun setMealLockState(
        mealPlanId: String,
        date: LocalDate,
        mealType: MealType,
        recipeId: String,
        isLocked: Boolean
    ): Result<Unit>

    /**
     * Sync local changes to the server.
     */
    suspend fun syncMealPlans(): Result<Unit>
}
