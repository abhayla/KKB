package com.rasoiai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val recipeId: String,
    val collectionId: String? = null, // null means default collection
    val addedAt: Long = System.currentTimeMillis(),
    val order: Int = 0
)

@Entity(tableName = "favorite_collections")
data class FavoriteCollectionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val coverImageUrl: String?,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
