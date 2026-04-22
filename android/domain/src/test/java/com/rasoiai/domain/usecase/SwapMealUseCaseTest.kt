package com.rasoiai.domain.usecase

import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.repository.MealPlanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SwapMealUseCaseTest {

    private lateinit var repository: MealPlanRepository
    private lateinit var useCase: SwapMealUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = SwapMealUseCase(repository)
    }

    @Test
    @DisplayName("Should return the swapped MealPlan on success")
    fun `should return the swapped MealPlan on success`() = runTest {
        val plan = stubPlan()
        coEvery {
            repository.swapMeal(any(), any(), any(), any(), any())
        } returns Result.success(plan)

        val result = useCase(
            mealPlanId = "mp-1",
            date = LocalDate.of(2026, 5, 12),
            mealType = MealType.LUNCH,
            currentRecipeId = "r-old"
        )

        assertTrue(result.isSuccess)
        assertEquals(plan, result.getOrNull())
    }

    @Test
    @DisplayName("Should default excludeRecipeIds to an empty list")
    fun `should default excludeRecipeIds to an empty list`() = runTest {
        val captured = slot<List<String>>()
        coEvery {
            repository.swapMeal(any(), any(), any(), any(), capture(captured))
        } returns Result.success(stubPlan())

        useCase(
            mealPlanId = "mp-1",
            date = LocalDate.of(2026, 5, 12),
            mealType = MealType.DINNER,
            currentRecipeId = "r-old"
            // excludeRecipeIds omitted — should default to []
        )

        assertEquals(emptyList<String>(), captured.captured)
    }

    @Test
    @DisplayName("Should forward excludeRecipeIds to repository")
    fun `should forward excludeRecipeIds to repository`() = runTest {
        val exclude = listOf("r-a", "r-b", "r-c")
        val captured = slot<List<String>>()
        coEvery {
            repository.swapMeal(any(), any(), any(), any(), capture(captured))
        } returns Result.success(stubPlan())

        useCase(
            mealPlanId = "mp-1",
            date = LocalDate.of(2026, 5, 12),
            mealType = MealType.BREAKFAST,
            currentRecipeId = "r-old",
            excludeRecipeIds = exclude
        )

        assertEquals(exclude, captured.captured)
    }

    @Test
    @DisplayName("Should propagate repository failure")
    fun `should propagate repository failure`() = runTest {
        val err = RuntimeException("no candidates")
        coEvery {
            repository.swapMeal(any(), any(), any(), any(), any())
        } returns Result.failure(err)

        val result = useCase(
            mealPlanId = "mp-1",
            date = LocalDate.of(2026, 5, 12),
            mealType = MealType.LUNCH,
            currentRecipeId = "r-old"
        )

        assertTrue(result.isFailure)
        assertEquals(err, result.exceptionOrNull())
    }

    @Test
    @DisplayName("Should forward all arguments to the repository unchanged")
    fun `should forward all arguments to the repository unchanged`() = runTest {
        coEvery {
            repository.swapMeal(any(), any(), any(), any(), any())
        } returns Result.success(stubPlan())

        val date = LocalDate.of(2026, 7, 4)
        useCase(
            mealPlanId = "mp-42",
            date = date,
            mealType = MealType.SNACKS,
            currentRecipeId = "r-original",
            excludeRecipeIds = listOf("r-x")
        )

        coVerify {
            repository.swapMeal(
                mealPlanId = "mp-42",
                date = date,
                mealType = MealType.SNACKS,
                currentRecipeId = "r-original",
                excludeRecipeIds = listOf("r-x")
            )
        }
    }

    private fun stubPlan() = MealPlan(
        id = "mp-1",
        weekStartDate = LocalDate.of(2026, 5, 11),
        weekEndDate = LocalDate.of(2026, 5, 17),
        days = emptyList(),
        createdAt = 0L,
        updatedAt = 0L
    )
}
