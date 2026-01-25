package com.rasoiai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rasoiai.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY `order`, addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE collectionId = :collectionId ORDER BY `order`, addedAt DESC")
    fun getFavoritesByCollection(collectionId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE collectionId IS NULL ORDER BY `order`, addedAt DESC")
    fun getDefaultFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE recipeId = :recipeId)")
    fun isFavorite(recipeId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE recipeId = :recipeId)")
    suspend fun isFavoriteSync(recipeId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE recipeId = :recipeId")
    suspend fun deleteFavorite(recipeId: String)

    @Query("UPDATE favorites SET collectionId = :collectionId WHERE recipeId = :recipeId")
    suspend fun moveToCollection(recipeId: String, collectionId: String?)

    @Query("UPDATE favorites SET `order` = :order WHERE recipeId = :recipeId")
    suspend fun updateOrder(recipeId: String, order: Int)

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoriteCount(): Int
}
