package com.rasoiai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_plans")
data class MealPlanEntity(
    @PrimaryKey
    val id: String,
    val weekStartDate: String, // yyyy-MM-dd format
    val weekEndDate: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isSynced: Boolean = false
)

@Entity(
    tableName = "meal_plan_items",
    primaryKeys = ["mealPlanId", "date", "mealType", "recipeId"]
)
data class MealPlanItemEntity(
    val mealPlanId: String,
    val date: String, // yyyy-MM-dd format
    val mealType: String, // breakfast, lunch, dinner, snacks
    val recipeId: String,
    val isLocked: Boolean = false,
    val order: Int = 0
)
