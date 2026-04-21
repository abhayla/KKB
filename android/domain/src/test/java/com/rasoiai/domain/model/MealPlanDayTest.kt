package com.rasoiai.domain.model

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MealPlanDayTest {

    @Nested
    @DisplayName("MealPlanDay.getAllMeals")
    inner class GetAllMeals {
        @Test
        fun `empty day returns empty list`() {
            val day = emptyDay()
            assertTrue(day.getAllMeals().isEmpty())
        }

        @Test
        fun `concatenates breakfast, lunch, snacks, dinner in that order`() {
            val day = emptyDay().copy(
                breakfast = listOf(item("b1")),
                lunch = listOf(item("l1"), item("l2")),
                snacks = listOf(item("s1")),
                dinner = listOf(item("d1"), item("d2")),
            )
            val result = day.getAllMeals()

            // Order matters — matches implementation's breakfast+lunch+snacks+dinner.
            // This order is the canonical "all meals" sequence used by aggregators.
            assertEquals(
                listOf("b1", "l1", "l2", "s1", "d1", "d2"),
                result.map { it.id },
            )
        }

        @Test
        fun `total count sums all four meal type lists`() {
            val day = emptyDay().copy(
                breakfast = listOf(item("b1")),
                lunch = listOf(item("l1"), item("l2")),
                snacks = listOf(item("s1"), item("s2"), item("s3")),
                dinner = emptyList(),
            )
            assertEquals(6, day.getAllMeals().size)
        }
    }

    @Nested
    @DisplayName("MealPlanDay.getMealsByType")
    inner class GetMealsByType {
        private val day = emptyDay().copy(
            breakfast = listOf(item("b1")),
            lunch = listOf(item("l1")),
            dinner = listOf(item("d1")),
            snacks = listOf(item("s1")),
        )

        @Test
        fun `BREAKFAST returns breakfast list`() {
            assertEquals(listOf("b1"), day.getMealsByType(MealType.BREAKFAST).map { it.id })
        }

        @Test
        fun `LUNCH returns lunch list`() {
            assertEquals(listOf("l1"), day.getMealsByType(MealType.LUNCH).map { it.id })
        }

        @Test
        fun `DINNER returns dinner list`() {
            assertEquals(listOf("d1"), day.getMealsByType(MealType.DINNER).map { it.id })
        }

        @Test
        fun `SNACKS returns snacks list`() {
            assertEquals(listOf("s1"), day.getMealsByType(MealType.SNACKS).map { it.id })
        }

        @Test
        fun `every MealType returns a list without throwing (exhaustiveness)`() {
            // Guards against future MealType additions that miss the when branch.
            MealType.entries.forEach { type ->
                day.getMealsByType(type)  // Must not throw
            }
        }
    }

    @Nested
    @DisplayName("MealType.fromValue")
    inner class MealTypeFromValue {
        @Test
        fun `known values map`() {
            assertEquals(MealType.BREAKFAST, MealType.fromValue("breakfast"))
            assertEquals(MealType.LUNCH, MealType.fromValue("lunch"))
            assertEquals(MealType.DINNER, MealType.fromValue("dinner"))
            assertEquals(MealType.SNACKS, MealType.fromValue("snacks"))
        }

        @Test
        fun `unknown returns null (contract - no default)`() {
            // Unlike other enums, MealType.fromValue returns nullable so callers
            // can distinguish unknown values from legitimate ones.
            assertNull(MealType.fromValue("brunch"))
            assertNull(MealType.fromValue(""))
        }

        @Test
        fun `round-trips`() {
            MealType.entries.forEach {
                assertEquals(it, MealType.fromValue(it.value))
            }
        }
    }

    // ==================== Factories ====================

    private fun emptyDay() = MealPlanDay(
        date = LocalDate.of(2026, 5, 11),
        dayName = "Monday",
        breakfast = emptyList(),
        lunch = emptyList(),
        dinner = emptyList(),
        snacks = emptyList(),
        festival = null,
    )

    private fun item(id: String) = MealItem(
        id = id,
        recipeId = "r-$id",
        recipeName = "Recipe $id",
        recipeImageUrl = null,
        prepTimeMinutes = 15,
        calories = 200,
        isLocked = false,
        order = 0,
        dietaryTags = emptyList(),
    )
}
