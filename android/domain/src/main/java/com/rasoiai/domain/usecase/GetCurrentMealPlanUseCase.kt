package com.rasoiai.domain.usecase

import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for getting the current week's meal plan.
 */
class GetCurrentMealPlanUseCase @Inject constructor(
    private val mealPlanRepository: MealPlanRepository
) {
    operator fun invoke(date: LocalDate = LocalDate.now()): Flow<MealPlan?> {
        return mealPlanRepository.getMealPlanForDate(date)
    }
}
