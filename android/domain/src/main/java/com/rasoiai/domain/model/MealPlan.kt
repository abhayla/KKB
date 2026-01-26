package com.rasoiai.domain.model

import java.time.LocalDate

data class MealPlan(
    val id: String,
    val weekStartDate: LocalDate,
    val weekEndDate: LocalDate,
    val days: List<MealPlanDay>,
    val createdAt: Long,
    val updatedAt: Long
)

data class MealPlanDay(
    val date: LocalDate,
    val dayName: String,
    val breakfast: List<MealItem>,
    val lunch: List<MealItem>,
    val dinner: List<MealItem>,
    val snacks: List<MealItem>,
    val festival: Festival?
) {
    fun getAllMeals(): List<MealItem> = breakfast + lunch + dinner + snacks

    fun getMealsByType(mealType: MealType): List<MealItem> = when (mealType) {
        MealType.BREAKFAST -> breakfast
        MealType.LUNCH -> lunch
        MealType.DINNER -> dinner
        MealType.SNACKS -> snacks
    }
}

data class MealItem(
    val id: String,
    val recipeId: String,
    val recipeName: String,
    val recipeImageUrl: String?,
    val prepTimeMinutes: Int,
    val calories: Int,
    val isLocked: Boolean,
    val order: Int,
    val dietaryTags: List<DietaryTag>
)

enum class MealType(val value: String) {
    BREAKFAST("breakfast"),
    LUNCH("lunch"),
    DINNER("dinner"),
    SNACKS("snacks");

    companion object {
        fun fromValue(value: String): MealType? = entries.find { it.value == value }
    }
}
