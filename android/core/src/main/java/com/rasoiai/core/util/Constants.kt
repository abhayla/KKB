package com.rasoiai.core.util

/**
 * App-wide constants.
 */
object Constants {
    // Meal types
    const val MEAL_BREAKFAST = "breakfast"
    const val MEAL_LUNCH = "lunch"
    const val MEAL_DINNER = "dinner"
    const val MEAL_SNACKS = "snacks"

    // Dietary types
    const val DIET_VEGETARIAN = "vegetarian"
    const val DIET_NON_VEGETARIAN = "non_vegetarian"
    const val DIET_VEGAN = "vegan"
    const val DIET_JAIN = "jain"
    const val DIET_SATTVIC = "sattvic"
    const val DIET_HALAL = "halal"
    const val DIET_EGGETARIAN = "eggetarian"

    // Cuisine zones
    const val CUISINE_NORTH = "north"
    const val CUISINE_SOUTH = "south"
    const val CUISINE_EAST = "east"
    const val CUISINE_WEST = "west"

    // Days of week
    val DAYS_OF_WEEK = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    // Pagination
    const val DEFAULT_PAGE_SIZE = 20

    // Cache duration (in milliseconds)
    const val CACHE_DURATION_MEAL_PLAN = 24 * 60 * 60 * 1000L // 24 hours
    const val CACHE_DURATION_RECIPES = 7 * 24 * 60 * 60 * 1000L // 7 days

    // Timeouts (in milliseconds)
    const val NETWORK_TIMEOUT = 30_000L
    const val READ_TIMEOUT = 30_000L
    const val WRITE_TIMEOUT = 30_000L
}

/**
 * Date format patterns.
 */
object DateFormats {
    const val API_DATE = "yyyy-MM-dd"
    const val DISPLAY_DATE = "EEE, MMM d"
    const val DISPLAY_DATE_FULL = "EEEE, MMMM d, yyyy"
    const val DISPLAY_TIME = "h:mm a"
}
