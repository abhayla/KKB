package com.rasoiai.domain.model

import java.time.DayOfWeek
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RecipeRuleTest {

    // ==================== RecipeRule.frequencyDisplayText ====================

    @Nested
    @DisplayName("RecipeRule.frequencyDisplayText")
    inner class FrequencyDisplayText {
        @Test
        fun `DAILY renders Every day`() {
            assertEquals("Every day", rule(frequency = RuleFrequency.DAILY).frequencyDisplayText)
        }

        @Test
        fun `NEVER renders Never`() {
            assertEquals("Never", rule(frequency = RuleFrequency.NEVER).frequencyDisplayText)
        }

        @Test
        fun `TIMES_PER_WEEK renders Nx per week`() {
            assertEquals("3x per week", rule(frequency = RuleFrequency.timesPerWeek(3)).frequencyDisplayText)
        }

        @Test
        fun `SPECIFIC_DAYS renders abbreviated days`() {
            val r = rule(
                frequency = RuleFrequency.specificDays(
                    listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                ),
            )
            // Format: first 3 chars, title-cased — Mon, Wed, Fri
            assertEquals("Mon, Wed, Fri", r.frequencyDisplayText)
        }

        @Test
        fun `SPECIFIC_DAYS without specificDays falls back to label`() {
            val r = rule(frequency = RuleFrequency(type = FrequencyType.SPECIFIC_DAYS))
            assertEquals("Specific days", r.frequencyDisplayText)
        }
    }

    // ==================== RecipeRule.mealSlotsDisplayText ====================

    @Nested
    @DisplayName("RecipeRule.mealSlotsDisplayText")
    inner class MealSlotsDisplayText {
        @Test
        fun `empty slots render Any slot`() {
            assertEquals("Any slot", rule(mealSlots = emptyList()).mealSlotsDisplayText)
        }

        @Test
        fun `all MealType entries render All meals`() {
            val all = MealType.entries.toList()
            assertEquals("All meals", rule(mealSlots = all).mealSlotsDisplayText)
        }

        @Test
        fun `single slot is title-cased`() {
            assertEquals("Breakfast", rule(mealSlots = listOf(MealType.BREAKFAST)).mealSlotsDisplayText)
        }

        @Test
        fun `multiple slots are comma-joined`() {
            val r = rule(mealSlots = listOf(MealType.LUNCH, MealType.DINNER))
            assertEquals("Lunch, Dinner", r.mealSlotsDisplayText)
        }
    }

    @Test
    @DisplayName("fullDescriptionText combines frequency and slots with dot separator")
    fun `fullDescriptionText combines`() {
        val r = rule(
            frequency = RuleFrequency.DAILY,
            mealSlots = listOf(MealType.BREAKFAST),
        )
        assertEquals("Every day · Breakfast", r.fullDescriptionText)
    }

    // ==================== RecipeRule.iconEmoji ====================

    @Nested
    @DisplayName("RecipeRule.iconEmoji")
    inner class IconEmoji {
        @Test
        fun `RECIPE type uses book emoji`() {
            assertEquals("📖", rule(type = RuleType.RECIPE, action = RuleAction.INCLUDE).iconEmoji)
            assertEquals("📖", rule(type = RuleType.RECIPE, action = RuleAction.EXCLUDE).iconEmoji)
        }

        @Test
        fun `INGREDIENT type uses carrot emoji`() {
            assertEquals("🥕", rule(type = RuleType.INGREDIENT, action = RuleAction.INCLUDE).iconEmoji)
            assertEquals("🥕", rule(type = RuleType.INGREDIENT, action = RuleAction.EXCLUDE).iconEmoji)
        }

        @Test
        fun `NUTRITION type uses salad emoji`() {
            assertEquals("🥗", rule(type = RuleType.NUTRITION, action = RuleAction.INCLUDE).iconEmoji)
        }

        @Test
        fun `MEAL_SLOT type uses book emoji`() {
            assertEquals("📖", rule(type = RuleType.MEAL_SLOT, action = RuleAction.INCLUDE).iconEmoji)
        }
    }

    // ==================== Enum round-trips ====================

    @Nested
    @DisplayName("Enum round-trips and defaults")
    inner class Enums {
        @Test
        fun `RuleType round-trips`() {
            RuleType.entries.forEach { assertEquals(it, RuleType.fromValue(it.value)) }
        }

        @Test
        fun `RuleType unknown falls back to RECIPE`() {
            assertEquals(RuleType.RECIPE, RuleType.fromValue("x"))
        }

        @Test
        fun `RuleAction round-trips and unknown falls back to INCLUDE`() {
            RuleAction.entries.forEach { assertEquals(it, RuleAction.fromValue(it.value)) }
            assertEquals(RuleAction.INCLUDE, RuleAction.fromValue("x"))
        }

        @Test
        fun `RuleEnforcement round-trips and unknown falls back to PREFERRED`() {
            RuleEnforcement.entries.forEach { assertEquals(it, RuleEnforcement.fromValue(it.value)) }
            assertEquals(RuleEnforcement.PREFERRED, RuleEnforcement.fromValue("x"))
        }

        @Test
        fun `FrequencyType round-trips and unknown falls back to TIMES_PER_WEEK`() {
            FrequencyType.entries.forEach { assertEquals(it, FrequencyType.fromValue(it.value)) }
            assertEquals(FrequencyType.TIMES_PER_WEEK, FrequencyType.fromValue("x"))
        }

        @Test
        fun `FoodCategory round-trips and unknown falls back to GREEN_LEAFY`() {
            FoodCategory.entries.forEach { assertEquals(it, FoodCategory.fromValue(it.value)) }
            assertEquals(FoodCategory.GREEN_LEAFY, FoodCategory.fromValue("x"))
        }
    }

    // ==================== RuleFrequency factories ====================

    @Nested
    @DisplayName("RuleFrequency factory constants")
    inner class RuleFrequencyFactories {
        @Test
        fun `DAILY is a singleton of FrequencyType DAILY`() {
            assertEquals(FrequencyType.DAILY, RuleFrequency.DAILY.type)
        }

        @Test
        fun `NEVER is FrequencyType NEVER`() {
            assertEquals(FrequencyType.NEVER, RuleFrequency.NEVER.type)
        }

        @Test
        fun `timesPerWeek factory sets type and count`() {
            val f = RuleFrequency.timesPerWeek(5)
            assertEquals(FrequencyType.TIMES_PER_WEEK, f.type)
            assertEquals(5, f.count)
        }

        @Test
        fun `specificDays factory sets type and days`() {
            val days = listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
            val f = RuleFrequency.specificDays(days)
            assertEquals(FrequencyType.SPECIFIC_DAYS, f.type)
            assertEquals(days, f.specificDays)
        }
    }

    // ==================== NutritionGoal ====================

    @Nested
    @DisplayName("NutritionGoal")
    inner class NutritionGoalTests {
        @Test
        fun `progressFraction is zero when weeklyTarget is zero`() {
            assertEquals(0f, goal(target = 0, progress = 5).progressFraction)
        }

        @Test
        fun `progressFraction is partial when in progress`() {
            assertEquals(0.4f, goal(target = 5, progress = 2).progressFraction, 0.0001f)
        }

        @Test
        fun `progressFraction is clamped to 1 when exceeded`() {
            // Contract: NutritionGoal CLAMPS (unlike WeeklyChallenge which
            // returns raw ratio) so progress bars don't overflow visually.
            assertEquals(1f, goal(target = 5, progress = 10).progressFraction)
        }

        @Test
        fun `isCompleted when progress meets target`() {
            assertTrue(goal(target = 5, progress = 5).isCompleted)
            assertTrue(goal(target = 5, progress = 7).isCompleted)
            assertFalse(goal(target = 5, progress = 4).isCompleted)
        }

        @Test
        fun `progressDisplayText formats as current over total times`() {
            assertEquals("3/5 times", goal(target = 5, progress = 3).progressDisplayText)
        }

        private fun goal(target: Int, progress: Int) = NutritionGoal(
            id = "g-1",
            foodCategory = FoodCategory.GREEN_LEAFY,
            weeklyTarget = target,
            currentProgress = progress,
        )
    }

    // ==================== Factories ====================

    private fun rule(
        type: RuleType = RuleType.RECIPE,
        action: RuleAction = RuleAction.INCLUDE,
        frequency: RuleFrequency = RuleFrequency.DAILY,
        mealSlots: List<MealType> = emptyList(),
    ) = RecipeRule(
        id = "r-1",
        type = type,
        action = action,
        targetId = "t-1",
        targetName = "Test",
        frequency = frequency,
        enforcement = RuleEnforcement.PREFERRED,
        mealSlots = mealSlots,
    )
}
