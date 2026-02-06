package com.rasoiai.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.Ingredient
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.model.Instruction
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.Recipe

/**
 * DTO for AI recipe catalog search results from the backend.
 * Maps to AiRecipeCatalogResponse schema.
 */
data class AiRecipeCatalogResponse(
    val id: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("normalized_name")
    val normalizedName: String,
    @SerializedName("dietary_tags")
    val dietaryTags: List<String> = emptyList(),
    @SerializedName("cuisine_type")
    val cuisineType: String? = null,
    @SerializedName("meal_types")
    val mealTypes: List<String> = emptyList(),
    val category: String? = null,
    @SerializedName("prep_time_minutes")
    val prepTimeMinutes: Int? = null,
    val calories: Int? = null,
    @SerializedName("usage_count")
    val usageCount: Int = 1,
)

/**
 * Map AI catalog entry to domain Recipe model.
 * Uses defaults for fields not available in the catalog (like instructions).
 */
fun AiRecipeCatalogResponse.toDomain(): Recipe {
    return Recipe(
        id = id,
        name = displayName,
        description = "${category ?: "Recipe"} • ${usageCount}x generated",
        imageUrl = null,
        prepTimeMinutes = prepTimeMinutes ?: 30,
        cookTimeMinutes = 0,
        servings = 4,
        difficulty = Difficulty.MEDIUM,
        cuisineType = CuisineType.fromValue(cuisineType ?: "north"),
        mealTypes = mealTypes.mapNotNull { MealType.fromValue(it) },
        dietaryTags = dietaryTags.mapNotNull { DietaryTag.fromValue(it) },
        ingredients = emptyList(),
        instructions = emptyList(),
        nutrition = null,
        isFavorite = false,
    )
}
