package com.rasoiai.domain.usecase

import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.repository.MealPlanRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * Use case for generating a new weekly meal plan.
 */
class GenerateMealPlanUseCase @Inject constructor(
    private val mealPlanRepository: MealPlanRepository
) {
    /**
     * Generate a meal plan for the week containing the given date.
     * The week starts on Monday.
     */
    suspend operator fun invoke(date: LocalDate = LocalDate.now()): Result<MealPlan> {
        // Find the Monday of the current week
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return mealPlanRepository.generateMealPlan(weekStart)
    }
}
