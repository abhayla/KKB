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
    val familyMembers: List<FamilyMember>,
    val primaryDiet: PrimaryDiet,
    val dietaryRestrictions: List<DietaryRestriction>,
    val cuisinePreferences: List<CuisineType>,
    val spiceLevel: SpiceLevel,
    val dislikedIngredients: List<String>,
    val weekdayCookingTimeMinutes: Int,
    val weekendCookingTimeMinutes: Int,
    val busyDays: List<DayOfWeek>,
    // Meal generation settings
    val itemsPerMeal: Int = 2,  // Number of items per meal slot (1-4)
    val strictAllergenMode: Boolean = true,  // Strictly exclude allergens
    val strictDietaryMode: Boolean = true,  // Strictly enforce dietary restrictions
    val allowRecipeRepeat: Boolean = false  // Allow same recipe multiple times per week
)

data class FamilyMember(
    val id: String,
    val name: String,
    val type: MemberType,
    val age: Int?,
    val specialNeeds: List<SpecialDietaryNeed>
)

enum class MemberType(val value: String, val displayName: String) {
    ADULT("adult", "Adult"),
    CHILD("child", "Child"),
    SENIOR("senior", "Senior");

    companion object {
        fun fromValue(value: String): MemberType = entries.find { it.value == value } ?: ADULT
    }
}

enum class SpecialDietaryNeed(val value: String, val displayName: String) {
    DIABETIC("diabetic", "Diabetic"),
    LOW_OIL("low_oil", "Low oil"),
    NO_SPICY("no_spicy", "No spicy"),
    SOFT_FOOD("soft_food", "Soft food"),
    LOW_SALT("low_salt", "Low salt"),
    HIGH_PROTEIN("high_protein", "High protein"),
    LOW_CARB("low_carb", "Low carb");

    companion object {
        fun fromValue(value: String): SpecialDietaryNeed? = entries.find { it.value == value }
    }
}

enum class PrimaryDiet(val value: String, val displayName: String, val description: String) {
    VEGETARIAN("vegetarian", "Vegetarian", "No meat, fish, or eggs"),
    EGGETARIAN("eggetarian", "Eggetarian", "Vegetarian + eggs"),
    NON_VEGETARIAN("non_vegetarian", "Non-Vegetarian", "All foods");

    companion object {
        fun fromValue(value: String): PrimaryDiet = entries.find { it.value == value } ?: VEGETARIAN
    }
}

enum class DietaryRestriction(val value: String, val displayName: String) {
    JAIN("jain", "Jain (No root vegetables)"),
    SATTVIC("sattvic", "Sattvic (No onion/garlic)"),
    HALAL("halal", "Halal only"),
    VEGAN("vegan", "Vegan");

    companion object {
        fun fromValue(value: String): DietaryRestriction? = entries.find { it.value == value }
    }
}

enum class SpiceLevel(val value: String, val displayName: String) {
    MILD("mild", "Mild"),
    MEDIUM("medium", "Medium"),
    SPICY("spicy", "Spicy"),
    VERY_SPICY("very_spicy", "Very Spicy");

    companion object {
        fun fromValue(value: String): SpiceLevel = entries.find { it.value == value } ?: MEDIUM
    }
}

enum class DayOfWeek(val value: String, val shortName: String) {
    MONDAY("monday", "Mon"),
    TUESDAY("tuesday", "Tue"),
    WEDNESDAY("wednesday", "Wed"),
    THURSDAY("thursday", "Thu"),
    FRIDAY("friday", "Fri"),
    SATURDAY("saturday", "Sat"),
    SUNDAY("sunday", "Sun");

    companion object {
        fun fromValue(value: String): DayOfWeek? = entries.find { it.value == value }
    }
}

// Legacy enum kept for Recipe compatibility
enum class CookingTimePreference(val value: String, val displayName: String, val maxMinutes: Int) {
    QUICK("quick", "Quick (< 30 mins)", 30),
    MODERATE("moderate", "Moderate (30-60 mins)", 60),
    ELABORATE("elaborate", "Elaborate (60+ mins)", Int.MAX_VALUE);

    companion object {
        fun fromValue(value: String): CookingTimePreference =
            entries.find { it.value == value } ?: MODERATE
    }
}
