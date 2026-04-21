package com.rasoiai.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Sanity tests for Constants and DateFormats. These values are referenced
 * across multiple modules — a silent rename or value change would break
 * serialization compatibility with the backend or previously-cached data.
 */
class ConstantsTest {

    @Nested
    @DisplayName("Meal type constants")
    inner class MealTypes {
        @Test
        fun `meal type strings match backend protocol`() {
            // Backend API uses these exact lowercase values in meal_plan_items.meal_type.
            assertEquals("breakfast", Constants.MEAL_BREAKFAST)
            assertEquals("lunch", Constants.MEAL_LUNCH)
            assertEquals("dinner", Constants.MEAL_DINNER)
            assertEquals("snacks", Constants.MEAL_SNACKS)
        }

        @Test
        fun `meal types are unique`() {
            val types = setOf(
                Constants.MEAL_BREAKFAST,
                Constants.MEAL_LUNCH,
                Constants.MEAL_DINNER,
                Constants.MEAL_SNACKS,
            )
            assertEquals(4, types.size)
        }
    }

    @Nested
    @DisplayName("Diet type constants")
    inner class DietTypes {
        @Test
        fun `diet strings match backend protocol`() {
            assertEquals("vegetarian", Constants.DIET_VEGETARIAN)
            assertEquals("non_vegetarian", Constants.DIET_NON_VEGETARIAN)
            assertEquals("vegan", Constants.DIET_VEGAN)
            assertEquals("jain", Constants.DIET_JAIN)
            assertEquals("sattvic", Constants.DIET_SATTVIC)
            assertEquals("halal", Constants.DIET_HALAL)
            assertEquals("eggetarian", Constants.DIET_EGGETARIAN)
        }

        @Test
        fun `diet values are unique`() {
            val diets = setOf(
                Constants.DIET_VEGETARIAN,
                Constants.DIET_NON_VEGETARIAN,
                Constants.DIET_VEGAN,
                Constants.DIET_JAIN,
                Constants.DIET_SATTVIC,
                Constants.DIET_HALAL,
                Constants.DIET_EGGETARIAN,
            )
            assertEquals(7, diets.size)
        }
    }

    @Nested
    @DisplayName("Cuisine constants")
    inner class Cuisines {
        @Test
        fun `cuisine keys match backend zone ids`() {
            assertEquals("north", Constants.CUISINE_NORTH)
            assertEquals("south", Constants.CUISINE_SOUTH)
            assertEquals("east", Constants.CUISINE_EAST)
            assertEquals("west", Constants.CUISINE_WEST)
        }
    }

    @Nested
    @DisplayName("Days of week")
    inner class DaysOfWeek {
        @Test
        fun `seven days in weekday-first order`() {
            assertEquals(7, Constants.DAYS_OF_WEEK.size)
            assertEquals("Monday", Constants.DAYS_OF_WEEK[0])
            assertEquals("Sunday", Constants.DAYS_OF_WEEK[6])
        }

        @Test
        fun `days are unique and title-cased`() {
            val set = Constants.DAYS_OF_WEEK.toSet()
            assertEquals(7, set.size)
            Constants.DAYS_OF_WEEK.forEach { day ->
                assertTrue(day.first().isUpperCase(), "Day should be title-cased: $day")
            }
        }
    }

    @Nested
    @DisplayName("Numeric constants")
    inner class NumericConstants {
        @Test
        fun `default page size is positive`() {
            assertTrue(Constants.DEFAULT_PAGE_SIZE > 0)
            assertTrue(Constants.DEFAULT_PAGE_SIZE <= 100, "Page size should be reasonable")
        }

        @Test
        fun `cache durations match documented values`() {
            // If these drift, docs referencing "24 hours" / "7 days" become lies.
            assertEquals(24 * 60 * 60 * 1000L, Constants.CACHE_DURATION_MEAL_PLAN)
            assertEquals(7 * 24 * 60 * 60 * 1000L, Constants.CACHE_DURATION_RECIPES)
        }

        @Test
        fun `timeouts are 30 seconds`() {
            assertEquals(30_000L, Constants.NETWORK_TIMEOUT)
            assertEquals(30_000L, Constants.READ_TIMEOUT)
            assertEquals(30_000L, Constants.WRITE_TIMEOUT)
        }
    }

    @Nested
    @DisplayName("DateFormats")
    inner class DateFormatsTests {
        @Test
        fun `api date format is ISO-8601 calendar`() {
            assertEquals("yyyy-MM-dd", DateFormats.API_DATE)
        }

        @Test
        fun `display formats are non-empty`() {
            assertTrue(DateFormats.DISPLAY_DATE.isNotEmpty())
            assertTrue(DateFormats.DISPLAY_DATE_FULL.isNotEmpty())
            assertTrue(DateFormats.DISPLAY_TIME.isNotEmpty())
        }
    }
}
