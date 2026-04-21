package com.rasoiai.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for pure computed properties and enum round-trips on Recipe.kt
 * and its related classes.
 */
class RecipeModelTest {

    @Nested
    @DisplayName("Recipe.totalTimeMinutes")
    inner class TotalTimeMinutes {
        @Test
        fun `sums prep and cook times`() {
            val r = recipe(prep = 10, cook = 25)
            assertEquals(35, r.totalTimeMinutes)
        }

        @Test
        fun `zero prep and cook yield zero total`() {
            val r = recipe(prep = 0, cook = 0)
            assertEquals(0, r.totalTimeMinutes)
        }

        @Test
        fun `large values do not overflow Int`() {
            val r = recipe(prep = 500_000, cook = 500_000)
            assertEquals(1_000_000, r.totalTimeMinutes)
        }
    }

    @Nested
    @DisplayName("Ingredient.displayText")
    inner class IngredientDisplayText {
        @Test
        fun `formats quantity unit and name`() {
            val i = ingredient(quantity = "200", unit = "g", name = "Paneer")
            assertEquals("200 g Paneer", i.displayText)
        }

        @Test
        fun `handles empty quantity`() {
            val i = ingredient(quantity = "", unit = "pinch", name = "Salt")
            assertEquals(" pinch Salt", i.displayText)
        }
    }

    @Nested
    @DisplayName("Difficulty.fromValue")
    inner class DifficultyFromValue {
        @Test
        fun `known values map`() {
            assertEquals(Difficulty.EASY, Difficulty.fromValue("easy"))
            assertEquals(Difficulty.MEDIUM, Difficulty.fromValue("medium"))
            assertEquals(Difficulty.HARD, Difficulty.fromValue("hard"))
        }

        @Test
        fun `unknown falls back to MEDIUM`() {
            assertEquals(Difficulty.MEDIUM, Difficulty.fromValue("impossible"))
        }

        @Test
        fun `round-trips`() {
            Difficulty.entries.forEach { assertEquals(it, Difficulty.fromValue(it.value)) }
        }
    }

    @Nested
    @DisplayName("CuisineType.fromValue")
    inner class CuisineTypeFromValue {
        @Test
        fun `known values map`() {
            assertEquals(CuisineType.NORTH, CuisineType.fromValue("north"))
            assertEquals(CuisineType.SOUTH, CuisineType.fromValue("south"))
            assertEquals(CuisineType.EAST, CuisineType.fromValue("east"))
            assertEquals(CuisineType.WEST, CuisineType.fromValue("west"))
        }

        @Test
        fun `unknown falls back to NORTH`() {
            assertEquals(CuisineType.NORTH, CuisineType.fromValue("unknown"))
        }

        @Test
        fun `round-trips`() {
            CuisineType.entries.forEach { assertEquals(it, CuisineType.fromValue(it.value)) }
        }

        @Test
        fun `display names are non-empty`() {
            CuisineType.entries.forEach { entry ->
                assert(entry.displayName.isNotEmpty()) { "$entry has empty displayName" }
            }
        }
    }

    @Nested
    @DisplayName("DietaryTag.fromValue")
    inner class DietaryTagFromValue {
        @Test
        fun `known values map`() {
            assertEquals(DietaryTag.VEGETARIAN, DietaryTag.fromValue("vegetarian"))
            assertEquals(DietaryTag.VEGAN, DietaryTag.fromValue("vegan"))
            assertEquals(DietaryTag.JAIN, DietaryTag.fromValue("jain"))
        }

        @Test
        fun `unknown returns null (not fall-back)`() {
            // Contract: DietaryTag.fromValue returns NULLABLE — unknown tags
            // should be filtered out rather than coerced to a default diet.
            assertNull(DietaryTag.fromValue("bogus"))
        }

        @Test
        fun `round-trips`() {
            DietaryTag.entries.forEach {
                assertEquals(it, DietaryTag.fromValue(it.value))
            }
        }
    }

    @Nested
    @DisplayName("IngredientCategory.fromValue")
    inner class IngredientCategoryFromValue {
        @Test
        fun `known values map`() {
            assertEquals(IngredientCategory.VEGETABLES, IngredientCategory.fromValue("vegetables"))
            assertEquals(IngredientCategory.DAIRY, IngredientCategory.fromValue("dairy"))
        }

        @Test
        fun `unknown falls back to OTHER`() {
            assertEquals(IngredientCategory.OTHER, IngredientCategory.fromValue("no_such_category"))
        }

        @Test
        fun `round-trips`() {
            IngredientCategory.entries.forEach {
                assertEquals(it, IngredientCategory.fromValue(it.value))
            }
        }
    }

    // ==================== Factories ====================

    private fun recipe(prep: Int, cook: Int) = Recipe(
        id = "r-1", name = "R", description = "", imageUrl = null,
        prepTimeMinutes = prep, cookTimeMinutes = cook, servings = 2,
        difficulty = Difficulty.EASY, cuisineType = CuisineType.NORTH,
        mealTypes = emptyList(), dietaryTags = emptyList(),
        ingredients = emptyList(), instructions = emptyList(), nutrition = null,
    )

    private fun ingredient(
        quantity: String,
        unit: String,
        name: String,
    ) = Ingredient(
        id = "i-1", name = name, quantity = quantity, unit = unit,
        category = IngredientCategory.OTHER,
    )
}
