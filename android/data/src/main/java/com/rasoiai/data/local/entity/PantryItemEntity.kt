package com.rasoiai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pantry_items")
data class PantryItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val quantity: Int = 1,
    val unit: String = "piece",
    val addedDate: String, // ISO date format
    val expiryDate: String?, // ISO date format, null for non-perishables
    val imageUrl: String? = null
)
