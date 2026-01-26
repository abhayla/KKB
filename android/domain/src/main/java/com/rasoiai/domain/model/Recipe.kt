package com.rasoiai.domain.model

data class Recipe(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    val servings: Int,
    val difficulty: Difficulty,
    val cuisineType: CuisineType,
    val mealTypes: List<MealType>,
    val dietaryTags: List<DietaryTag>,
    val ingredients: List<Ingredient>,
    val instructions: List<Instruction>,
    val nutrition: Nutrition?,
    val isFavorite: Boolean = false
) {
    val totalTimeMinutes: Int get() = prepTimeMinutes + cookTimeMinutes
}

data class Ingredient(
    val id: String,
    val name: String,
    val quantity: String,
    val unit: String,
    val category: IngredientCategory,
    val isOptional: Boolean = false,
    val substituteFor: String? = null
) {
    val displayText: String get() = "$quantity $unit $name"
}

data class Instruction(
    val stepNumber: Int,
    val instruction: String,
    val durationMinutes: Int?,
    val timerRequired: Boolean = false,
    val tips: String?,
    val imageUrl: String? = null
)

data class Nutrition(
    val calories: Int,
    val proteinGrams: Int,
    val carbohydratesGrams: Int,
    val fatGrams: Int,
    val fiberGrams: Int,
    val sugarGrams: Int,
    val sodiumMg: Int
)

enum class Difficulty(val value: String) {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard");

    companion object {
        fun fromValue(value: String): Difficulty = entries.find { it.value == value } ?: MEDIUM
    }
}

enum class CuisineType(val value: String, val displayName: String) {
    NORTH("north", "North Indian"),
    SOUTH("south", "South Indian"),
    EAST("east", "East Indian"),
    WEST("west", "West Indian");

    companion object {
        fun fromValue(value: String): CuisineType = entries.find { it.value == value } ?: NORTH
    }
}

enum class DietaryTag(val value: String, val displayName: String) {
    VEGETARIAN("vegetarian", "Vegetarian"),
    NON_VEGETARIAN("non_vegetarian", "Non-Vegetarian"),
    VEGAN("vegan", "Vegan"),
    JAIN("jain", "Jain"),
    SATTVIC("sattvic", "Sattvic"),
    HALAL("halal", "Halal"),
    EGGETARIAN("eggetarian", "Eggetarian");

    companion object {
        fun fromValue(value: String): DietaryTag? = entries.find { it.value == value }
    }
}

enum class IngredientCategory(val value: String, val displayName: String) {
    VEGETABLES("vegetables", "Vegetables"),
    FRUITS("fruits", "Fruits"),
    DAIRY("dairy", "Dairy"),
    GRAINS("grains", "Grains & Cereals"),
    PULSES("pulses", "Pulses & Lentils"),
    SPICES("spices", "Spices & Seasonings"),
    OILS("oils", "Oils & Fats"),
    MEAT("meat", "Meat & Poultry"),
    SEAFOOD("seafood", "Seafood"),
    NUTS("nuts", "Nuts & Dry Fruits"),
    SWEETENERS("sweeteners", "Sweeteners"),
    OTHER("other", "Other");

    companion object {
        fun fromValue(value: String): IngredientCategory = entries.find { it.value == value } ?: OTHER
    }
}
