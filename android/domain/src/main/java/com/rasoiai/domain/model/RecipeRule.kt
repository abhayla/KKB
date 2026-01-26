package com.rasoiai.domain.model

import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * Represents a rule that influences meal plan generation.
 * Rules can include/exclude recipes or ingredients, assign recipes to meal slots,
 * or set nutrition goals.
 */
data class RecipeRule(
    val id: String,
    val type: RuleType,
    val action: RuleAction,
    val targetId: String,
    val targetName: String,
    val frequency: RuleFrequency,
    val enforcement: RuleEnforcement,
    val mealSlot: MealType? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Display text for the frequency setting.
     */
    val frequencyDisplayText: String
        get() = when (frequency.type) {
            FrequencyType.DAILY -> "Every day"
            FrequencyType.TIMES_PER_WEEK -> "${frequency.count}x per week"
            FrequencyType.SPECIFIC_DAYS -> frequency.specificDays?.joinToString(", ") {
                it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }
            } ?: "Specific days"
            FrequencyType.NEVER -> "Never"
        }

    /**
     * Display text combining frequency and meal slot.
     */
    val fullDescriptionText: String
        get() = when {
            type == RuleType.MEAL_SLOT && mealSlot != null -> {
                "$frequencyDisplayText • ${mealSlot.name.lowercase().replaceFirstChar { it.uppercase() }}"
            }
            mealSlot != null -> {
                "$frequencyDisplayText • ${mealSlot.name.lowercase().replaceFirstChar { it.uppercase() }}"
            }
            else -> frequencyDisplayText
        }

    /**
     * Icon to display based on rule type and action.
     */
    val iconEmoji: String
        get() = when {
            type == RuleType.RECIPE && action == RuleAction.INCLUDE -> "📖"
            type == RuleType.RECIPE && action == RuleAction.EXCLUDE -> "🚫"
            type == RuleType.INGREDIENT && action == RuleAction.INCLUDE -> "🥕"
            type == RuleType.INGREDIENT && action == RuleAction.EXCLUDE -> "🚫"
            type == RuleType.MEAL_SLOT -> "🍽️"
            type == RuleType.NUTRITION -> "🥗"
            else -> "📋"
        }
}

/**
 * Type of rule.
 */
enum class RuleType(val value: String, val displayName: String) {
    RECIPE("recipe", "Recipe"),
    INGREDIENT("ingredient", "Ingredient"),
    MEAL_SLOT("meal_slot", "Meal-Slot"),
    NUTRITION("nutrition", "Nutrition");

    companion object {
        fun fromValue(value: String): RuleType = entries.find { it.value == value } ?: RECIPE
    }
}

/**
 * Action to take - include or exclude.
 */
enum class RuleAction(val value: String, val displayName: String) {
    INCLUDE("include", "Include"),
    EXCLUDE("exclude", "Exclude");

    companion object {
        fun fromValue(value: String): RuleAction = entries.find { it.value == value } ?: INCLUDE
    }
}

/**
 * How strictly to enforce the rule.
 */
enum class RuleEnforcement(val value: String, val displayName: String) {
    REQUIRED("required", "Required"),
    PREFERRED("preferred", "Preferred");

    companion object {
        fun fromValue(value: String): RuleEnforcement = entries.find { it.value == value } ?: PREFERRED
    }
}

/**
 * Frequency configuration for a rule.
 */
data class RuleFrequency(
    val type: FrequencyType,
    val count: Int? = null,
    val specificDays: List<DayOfWeek>? = null
) {
    companion object {
        val DAILY = RuleFrequency(FrequencyType.DAILY)
        val NEVER = RuleFrequency(FrequencyType.NEVER)

        fun timesPerWeek(count: Int) = RuleFrequency(
            type = FrequencyType.TIMES_PER_WEEK,
            count = count
        )

        fun specificDays(days: List<DayOfWeek>) = RuleFrequency(
            type = FrequencyType.SPECIFIC_DAYS,
            specificDays = days
        )
    }
}

/**
 * Type of frequency.
 */
enum class FrequencyType(val value: String, val displayName: String) {
    DAILY("daily", "Every day"),
    TIMES_PER_WEEK("times_per_week", "X times per week"),
    SPECIFIC_DAYS("specific_days", "Specific days"),
    NEVER("never", "Never");

    companion object {
        fun fromValue(value: String): FrequencyType = entries.find { it.value == value } ?: TIMES_PER_WEEK
    }
}

/**
 * Nutrition goal for tracking food category consumption.
 */
data class NutritionGoal(
    val id: String,
    val foodCategory: FoodCategory,
    val weeklyTarget: Int,
    val currentProgress: Int = 0,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Progress as a float between 0 and 1 for progress bar display.
     */
    val progressFraction: Float
        get() = if (weeklyTarget > 0) {
            (currentProgress.toFloat() / weeklyTarget).coerceIn(0f, 1f)
        } else 0f

    /**
     * Display text for progress.
     */
    val progressDisplayText: String
        get() = "$currentProgress/$weeklyTarget times"

    /**
     * Whether the goal is completed.
     */
    val isCompleted: Boolean
        get() = currentProgress >= weeklyTarget
}

/**
 * Food categories for nutrition goals.
 */
enum class FoodCategory(val value: String, val displayName: String, val emoji: String) {
    GREEN_LEAFY("green_leafy", "Green leafy vegetables", "🥬"),
    CITRUS_VITAMIN_C("citrus_vitamin_c", "Citrus/Vitamin C foods", "🍋"),
    IRON_RICH("iron_rich", "Iron-rich foods", "🥜"),
    HIGH_PROTEIN("high_protein", "High protein foods", "🥩"),
    CALCIUM_RICH("calcium_rich", "Calcium-rich foods", "🥛"),
    FIBER_RICH("fiber_rich", "Fiber-rich foods", "🌾"),
    OMEGA_3("omega_3", "Omega-3 rich foods", "🐟"),
    ANTIOXIDANT("antioxidant", "Antioxidant-rich foods", "🫐");

    companion object {
        fun fromValue(value: String): FoodCategory = entries.find { it.value == value } ?: GREEN_LEAFY
    }
}
