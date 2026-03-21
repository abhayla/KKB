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
        excludeRecipeIds: List<String> = emptyList(),
        newRecipeId: String? = null
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
     * Remove a recipe from a meal slot.
     */
    suspend fun removeRecipeFromMeal(
        mealPlanId: String,
        date: LocalDate,
        mealType: MealType,
        recipeId: String
    ): Result<Unit>

    /**
     * Add a recipe to a meal slot.
     */
    suspend fun addRecipeToMeal(
        mealPlanId: String,
        date: LocalDate,
        mealType: MealType,
        recipeId: String,
        recipeName: String,
        recipeImageUrl: String?,
        prepTimeMinutes: Int,
        calories: Int
    ): Result<MealPlan>

    /**
     * Sync local changes to the server.
     */
    suspend fun syncMealPlans(): Result<Unit>

    /**
     * Check if a meal plan exists for the current week.
     * Used to determine if user has completed onboarding.
     */
    suspend fun hasMealPlanForCurrentWeek(): Boolean

    /**
     * Fetch the current meal plan from the backend and cache locally.
     * Returns null if no plan exists on the backend.
     */
    suspend fun fetchCurrentMealPlan(): MealPlan?

    /**
     * Persist day lock state to Room.
     */
    suspend fun setDayLockState(mealPlanId: String, date: LocalDate, isLocked: Boolean): Result<Unit>

    /**
     * Persist meal type lock state to Room.
     */
    suspend fun setMealTypeLockState(mealPlanId: String, date: LocalDate, mealType: MealType, isLocked: Boolean): Result<Unit>
}
