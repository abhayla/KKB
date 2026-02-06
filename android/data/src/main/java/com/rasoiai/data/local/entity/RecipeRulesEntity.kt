package com.rasoiai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sync status for offline-first architecture.
 */
object SyncStatus {
    const val SYNCED = "SYNCED"
    const val PENDING = "PENDING"
    const val CONFLICT = "CONFLICT"
}

/**
 * Room entity for recipe rules.
 */
@Entity(tableName = "recipe_rules")
data class RecipeRuleEntity(
    @PrimaryKey
    val id: String,
    val type: String, // RuleType value
    val action: String, // RuleAction value
    val targetId: String,
    val targetName: String,
    val frequencyType: String, // FrequencyType value
    val frequencyCount: Int? = null,
    val frequencyDays: String? = null, // Comma-separated day values
    val enforcement: String, // RuleEnforcement value
    val mealSlot: String? = null, // MealType value
    val isActive: Boolean = true,
    val syncStatus: String = SyncStatus.SYNCED, // SYNCED, PENDING, CONFLICT
    val createdAt: String, // ISO datetime
    val updatedAt: String // ISO datetime
)

/**
 * Room entity for nutrition goals.
 */
@Entity(tableName = "nutrition_goals")
data class NutritionGoalEntity(
    @PrimaryKey
    val id: String,
    val foodCategory: String, // FoodCategory value
    val weeklyTarget: Int,
    val currentProgress: Int = 0,
    val enforcement: String, // RuleEnforcement value
    val isActive: Boolean = true,
    val syncStatus: String = SyncStatus.SYNCED, // SYNCED, PENDING, CONFLICT
    val createdAt: String, // ISO datetime
    val updatedAt: String // ISO datetime
)
