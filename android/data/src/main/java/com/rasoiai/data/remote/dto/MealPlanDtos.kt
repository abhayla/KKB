package com.rasoiai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GenerateMealPlanRequest(
    @SerializedName("week_start_date")
    val weekStartDate: String, // yyyy-MM-dd
    @SerializedName("regenerate_days")
    val regenerateDays: List<String>? = null, // Specific days to regenerate
    @SerializedName("exclude_recipe_ids")
    val excludeRecipeIds: List<String>? = null
)

data class SwapMealRequest(
    @SerializedName("exclude_recipe_ids")
    val excludeRecipeIds: List<String>? = null,
    @SerializedName("specific_recipe_id")
    val specificRecipeId: String? = null // If user wants a specific recipe
)

data class MealPlanResponse(
    val id: String,
    @SerializedName("week_start_date")
    val weekStartDate: String,
    @SerializedName("week_end_date")
    val weekEndDate: String,
    val days: List<MealPlanDayDto>,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class MealPlanDayDto(
    val date: String, // yyyy-MM-dd
    @SerializedName("day_name")
    val dayName: String,
    val meals: MealsByTypeDto,
    val festival: FestivalDto?
)

data class MealsByTypeDto(
    val breakfast: List<MealItemDto>,
    val lunch: List<MealItemDto>,
    val dinner: List<MealItemDto>,
    val snacks: List<MealItemDto>
)

data class MealItemDto(
    val id: String,
    @SerializedName("recipe_id")
    val recipeId: String,
    @SerializedName("recipe_name")
    val recipeName: String,
    @SerializedName("recipe_image_url")
    val recipeImageUrl: String?,
    @SerializedName("prep_time_minutes")
    val prepTimeMinutes: Int,
    @SerializedName("is_locked")
    val isLocked: Boolean,
    val order: Int,
    @SerializedName("dietary_tags")
    val dietaryTags: List<String>
)

data class FestivalDto(
    val id: String,
    val name: String,
    @SerializedName("is_fasting_day")
    val isFastingDay: Boolean,
    @SerializedName("suggested_dishes")
    val suggestedDishes: List<String>?
)
