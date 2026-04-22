package com.rasoiai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RecipeResponse(
    val id: String,
    val name: String,
    val description: String,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("prep_time_minutes")
    val prepTimeMinutes: Int,
    @SerializedName("cook_time_minutes")
    val cookTimeMinutes: Int,
    val servings: Int,
    val difficulty: String, // easy, medium, hard
    @SerializedName("cuisine_type")
    val cuisineType: String, // north, south, east, west
    @SerializedName("meal_types")
    val mealTypes: List<String>,
    @SerializedName("dietary_tags")
    val dietaryTags: List<String>,
    val ingredients: List<IngredientDto>,
    val instructions: List<InstructionDto>,
    val nutrition: NutritionDto?,
    @SerializedName("average_rating")
    val averageRating: Double? = null,
    @SerializedName("rating_count")
    val ratingCount: Int = 0,
    @SerializedName("user_rating")
    val userRating: Double? = null
)

data class IngredientDto(
    val id: String,
    val name: String,
    val quantity: String,
    val unit: String,
    val category: String, // vegetables, dairy, grains, spices, etc.
    @SerializedName("is_optional")
    val isOptional: Boolean = false,
    @SerializedName("substitute_for")
    val substituteFor: String? = null
)

data class InstructionDto(
    @SerializedName("step_number")
    val stepNumber: Int,
    val instruction: String,
    @SerializedName("duration_minutes")
    val durationMinutes: Int?,
    @SerializedName("timer_required")
    val timerRequired: Boolean = false,
    val tips: String?
)

data class NutritionDto(
    val calories: Int,
    val protein: Int, // grams
    val carbohydrates: Int, // grams
    val fat: Int, // grams
    val fiber: Int, // grams
    val sugar: Int, // grams
    val sodium: Int // mg
)

data class RecipeRatingRequest(
    val rating: Float,
    val feedback: String? = null
)

data class RecipeRatingResponse(
    val id: String,
    @SerializedName("recipe_id")
    val recipeId: String,
    val rating: Float,
    val feedback: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)
