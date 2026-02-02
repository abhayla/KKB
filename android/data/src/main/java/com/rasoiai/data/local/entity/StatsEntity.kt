package com.rasoiai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the user's cooking streak data.
 */
@Entity(tableName = "cooking_streak")
data class CookingStreakEntity(
    @PrimaryKey
    val id: String = "user_streak", // Single row for current user
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastCookingDate: String? = null // ISO date format
)

/**
 * Tracks individual cooking days for calendar display.
 */
@Entity(tableName = "cooking_days")
data class CookingDayEntity(
    @PrimaryKey
    val date: String, // ISO date format (YYYY-MM-DD)
    val didCook: Boolean = false,
    val mealsCount: Int = 0
)

/**
 * Stores achievement data.
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val isUnlocked: Boolean = false,
    val unlockedDate: String? = null // ISO date format
)

/**
 * Stores weekly challenge data.
 */
@Entity(tableName = "weekly_challenges")
data class WeeklyChallengeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val targetCount: Int,
    val currentProgress: Int = 0,
    val rewardBadge: String,
    val isJoined: Boolean = false,
    val weekStartDate: String, // ISO date format
    val weekEndDate: String // ISO date format
)

/**
 * Tracks cooked recipes for cuisine breakdown statistics.
 */
@Entity(tableName = "cooked_recipes")
data class CookedRecipeEntity(
    @PrimaryKey
    val id: String,
    val recipeId: String,
    val recipeName: String,
    val cuisineType: String, // NORTH, SOUTH, EAST, WEST
    val cookedDate: String, // ISO date format
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Result class for cuisine count queries.
 */
data class CuisineCountResult(
    val cuisineType: String,
    val count: Int
)
