package com.rasoiai.domain.usecase

import com.rasoiai.domain.model.MealPlan
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
import java.time.DayOfWeek
import java.time.LocalDate

class GenerateMealPlanUseCaseTest {

    private lateinit var repository: MealPlanRepository
    private lateinit var useCase: GenerateMealPlanUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = GenerateMealPlanUseCase(repository)
    }

    @Test
    @DisplayName("Should snap a mid-week date to the Monday of that week")
    fun `should snap a mid-week date to the Monday of that week`() = runTest {
        val captured = slot<LocalDate>()
        coEvery { repository.generateMealPlan(capture(captured)) } returns
            Result.success(stubPlan())

        // Thursday 2026-05-14 -> Monday 2026-05-11
        useCase(LocalDate.of(2026, 5, 14))

        assertEquals(LocalDate.of(2026, 5, 11), captured.captured)
        assertEquals(DayOfWeek.MONDAY, captured.captured.dayOfWeek)
    }

    @Test
    @DisplayName("Should keep Monday unchanged (previousOrSame)")
    fun `should keep Monday unchanged`() = runTest {
        val captured = slot<LocalDate>()
        coEvery { repository.generateMealPlan(capture(captured)) } returns
            Result.success(stubPlan())

        val monday = LocalDate.of(2026, 5, 11)
        useCase(monday)

        assertEquals(monday, captured.captured)
    }

    @Test
    @DisplayName("Should snap Sunday to the previous Monday")
    fun `should snap Sunday to the previous Monday`() = runTest {
        val captured = slot<LocalDate>()
        coEvery { repository.generateMealPlan(capture(captured)) } returns
            Result.success(stubPlan())

        // Sunday 2026-05-17 -> Monday 2026-05-11 (previousOrSame MONDAY is 6 days back)
        useCase(LocalDate.of(2026, 5, 17))

        assertEquals(LocalDate.of(2026, 5, 11), captured.captured)
    }

    @Test
    @DisplayName("Should return the MealPlan produced by the repository")
    fun `should return the MealPlan produced by the repository`() = runTest {
        val plan = stubPlan()
        coEvery { repository.generateMealPlan(any()) } returns Result.success(plan)

        val result = useCase(LocalDate.of(2026, 5, 14))

        assertTrue(result.isSuccess)
        assertEquals(plan, result.getOrNull())
    }

    @Test
    @DisplayName("Should propagate repository failure")
    fun `should propagate repository failure`() = runTest {
        val err = IllegalArgumentException("no user")
        coEvery { repository.generateMealPlan(any()) } returns Result.failure(err)

        val result = useCase(LocalDate.of(2026, 5, 14))

        assertTrue(result.isFailure)
        assertEquals(err, result.exceptionOrNull())
    }

    @Test
    @DisplayName("Should default to today when no date is provided")
    fun `should default to today when no date is provided`() = runTest {
        val captured = slot<LocalDate>()
        coEvery { repository.generateMealPlan(capture(captured)) } returns
            Result.success(stubPlan())

        useCase()  // no arg

        // Whatever today is, the captured Monday must equal previousOrSame MONDAY of today.
        val expectedMonday = LocalDate.now().with(
            java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        )
        assertEquals(expectedMonday, captured.captured)
        coVerify { repository.generateMealPlan(expectedMonday) }
    }

    private fun stubPlan() = MealPlan(
        id = "mp-gen",
        weekStartDate = LocalDate.of(2026, 5, 11),
        weekEndDate = LocalDate.of(2026, 5, 17),
        days = emptyList(),
        createdAt = 0L,
        updatedAt = 0L
    )
}
