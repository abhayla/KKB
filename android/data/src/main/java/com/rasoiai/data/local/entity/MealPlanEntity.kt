package com.rasoiai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
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
    foreignKeys = [
        ForeignKey(
            entity = MealPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealPlanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealPlanId"), Index("date")]
)
data class MealPlanItemEntity(
    @PrimaryKey
    val id: String,
    val mealPlanId: String,
    val date: String, // yyyy-MM-dd format
    val dayName: String, // Monday, Tuesday, etc.
    val mealType: String, // breakfast, lunch, dinner, snacks
    val recipeId: String,
    val recipeName: String,
    val recipeImageUrl: String?,
    val prepTimeMinutes: Int,
    val calories: Int,
    val dietaryTags: List<String>, // vegetarian, vegan, etc.
    val isLocked: Boolean = false,
    val isDayLocked: Boolean = false,
    val isMealTypeLocked: Boolean = false,
    val order: Int = 0
)

@Entity(tableName = "meal_plan_festivals")
data class MealPlanFestivalEntity(
    @PrimaryKey
    val id: String,
    val mealPlanId: String,
    val date: String, // yyyy-MM-dd format
    val name: String,
    val isFastingDay: Boolean,
    val suggestedDishes: List<String>
)
