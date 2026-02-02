package com.rasoiai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    @SerializedName("firebase_token")
    val firebaseToken: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class RefreshTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Long
)

data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
    val user: UserResponse
)

data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    @SerializedName("profile_image_url")
    val profileImageUrl: String?,
    @SerializedName("is_onboarded")
    val isOnboarded: Boolean,
    val preferences: UserPreferencesDto?
)

data class UserPreferencesDto(
    @SerializedName("household_size")
    val householdSize: Int,
    @SerializedName("dietary_restrictions")
    val dietaryRestrictions: List<String>,
    @SerializedName("cuisine_preferences")
    val cuisinePreferences: List<String>,
    @SerializedName("disliked_ingredients")
    val dislikedIngredients: List<String>,
    @SerializedName("cooking_time_preference")
    val cookingTimePreference: String, // quick, moderate, elaborate
    @SerializedName("spice_level")
    val spiceLevel: String, // mild, medium, spicy
    // Meal generation settings
    @SerializedName("items_per_meal")
    val itemsPerMeal: Int = 2,
    @SerializedName("strict_allergen_mode")
    val strictAllergenMode: Boolean = true,
    @SerializedName("strict_dietary_mode")
    val strictDietaryMode: Boolean = true,
    @SerializedName("allow_recipe_repeat")
    val allowRecipeRepeat: Boolean = false
)
