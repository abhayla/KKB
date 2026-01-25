package com.rasoiai.domain.usecase

import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.repository.MealPlanRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetCurrentMealPlanUseCaseTest {

    private lateinit var repository: MealPlanRepository
    private lateinit var useCase: GetCurrentMealPlanUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = GetCurrentMealPlanUseCase(repository)
    }

    @Test
    @DisplayName("Should return meal plan for current date")
    fun `should return meal plan for current date`() = runTest {
        // Given
        val today = LocalDate.now()
        val expectedMealPlan = createTestMealPlan(today)
        coEvery { repository.getMealPlanForDate(today) } returns flowOf(expectedMealPlan)

        // When
        val result = useCase(today).first()

        // Then
        assertEquals(expectedMealPlan, result)
    }

    @Test
    @DisplayName("Should return null when no meal plan exists")
    fun `should return null when no meal plan exists`() = runTest {
        // Given
        val today = LocalDate.now()
        coEvery { repository.getMealPlanForDate(today) } returns flowOf(null)

        // When
        val result = useCase(today).first()

        // Then
        assertNull(result)
    }

    private fun createTestMealPlan(date: LocalDate): MealPlan {
        return MealPlan(
            id = "test-meal-plan-id",
            weekStartDate = date.minusDays(date.dayOfWeek.value.toLong() - 1),
            weekEndDate = date.plusDays(7 - date.dayOfWeek.value.toLong()),
            days = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
