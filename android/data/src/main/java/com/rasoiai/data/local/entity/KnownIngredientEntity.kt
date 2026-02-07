package com.rasoiai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "known_ingredients")
data class KnownIngredientEntity(
    @PrimaryKey val name: String,
    val source: String, // "popular", "recipe_cache", "user_rule"
    val addedAt: Long = System.currentTimeMillis()
)
