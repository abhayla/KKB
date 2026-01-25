package com.rasoiai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grocery_items")
data class GroceryItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val quantity: String,
    val unit: String,
    val category: String, // vegetables, dairy, grains, spices, etc.
    val isChecked: Boolean = false,
    val mealPlanId: String?, // Associated meal plan
    val recipeIds: List<String>, // Recipes that need this ingredient
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
