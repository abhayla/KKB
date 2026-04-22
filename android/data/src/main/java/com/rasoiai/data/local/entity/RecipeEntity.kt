package com.rasoiai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    val servings: Int,
    val difficulty: String, // easy, medium, hard
    val cuisineType: String, // north, south, east, west
    val mealTypes: List<String>, // breakfast, lunch, dinner, snacks
    val dietaryTags: List<String>, // vegetarian, vegan, jain, etc.
    val ingredients: String, // JSON string of ingredients list
    val instructions: String, // JSON string of instructions list
    val nutritionInfo: String?, // JSON string of nutrition data
    val calories: Int?,
    val isFavorite: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis(),
    // Rating aggregate (issue #21 offline-cache acceptance criterion).
    // Mirrors RecipeResponse.average_rating / rating_count / user_rating so
    // the rating aggregate survives an offline-open round-trip.
    val averageRating: Double? = null,
    val ratingCount: Int = 0,
    val userRating: Double? = null
)
