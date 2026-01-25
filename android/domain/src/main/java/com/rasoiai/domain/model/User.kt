package com.rasoiai.domain.model

data class User(
    val id: String,
    val email: String,
    val name: String,
    val profileImageUrl: String?,
    val isOnboarded: Boolean,
    val preferences: UserPreferences?
)

data class UserPreferences(
    val householdSize: Int,
    val dietaryRestrictions: List<DietaryTag>,
    val cuisinePreferences: List<CuisineType>,
    val dislikedIngredients: List<String>,
    val cookingTimePreference: CookingTimePreference,
    val spiceLevel: SpiceLevel
)

enum class CookingTimePreference(val value: String, val displayName: String, val maxMinutes: Int) {
    QUICK("quick", "Quick (< 30 mins)", 30),
    MODERATE("moderate", "Moderate (30-60 mins)", 60),
    ELABORATE("elaborate", "Elaborate (60+ mins)", Int.MAX_VALUE);

    companion object {
        fun fromValue(value: String): CookingTimePreference =
            entries.find { it.value == value } ?: MODERATE
    }
}

enum class SpiceLevel(val value: String, val displayName: String) {
    MILD("mild", "Mild"),
    MEDIUM("medium", "Medium"),
    SPICY("spicy", "Spicy");

    companion object {
        fun fromValue(value: String): SpiceLevel = entries.find { it.value == value } ?: MEDIUM
    }
}
