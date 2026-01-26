package com.rasoiai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rasoiai.data.local.entity.FavoriteCollectionEntity
import com.rasoiai.data.local.entity.RecentlyViewedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    // ==================== Collections ====================

    @Query("SELECT * FROM favorite_collections ORDER BY `order`, createdAt DESC")
    fun getAllCollections(): Flow<List<FavoriteCollectionEntity>>

    @Query("SELECT * FROM favorite_collections WHERE id = :collectionId")
    fun getCollectionById(collectionId: String): Flow<FavoriteCollectionEntity?>

    @Query("SELECT * FROM favorite_collections WHERE id = :collectionId")
    suspend fun getCollectionByIdSync(collectionId: String): FavoriteCollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: FavoriteCollectionEntity)

    @Update
    suspend fun updateCollection(collection: FavoriteCollectionEntity)

    @Query("DELETE FROM favorite_collections WHERE id = :collectionId")
    suspend fun deleteCollection(collectionId: String)

    @Query("SELECT COUNT(*) FROM favorite_collections")
    suspend fun getCollectionCount(): Int

    // ==================== Recently Viewed ====================

    @Query("SELECT * FROM recently_viewed ORDER BY viewedAt DESC LIMIT :limit")
    fun getRecentlyViewed(limit: Int = 20): Flow<List<RecentlyViewedEntity>>

    @Query("SELECT recipeId FROM recently_viewed ORDER BY viewedAt DESC LIMIT :limit")
    fun getRecentlyViewedIds(limit: Int = 20): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyViewed(item: RecentlyViewedEntity)

    @Query("DELETE FROM recently_viewed WHERE recipeId = :recipeId")
    suspend fun deleteRecentlyViewed(recipeId: String)

    @Query("DELETE FROM recently_viewed")
    suspend fun clearRecentlyViewed()

    @Query("SELECT COUNT(*) FROM recently_viewed")
    suspend fun getRecentlyViewedCount(): Int

    /**
     * Keep only the most recent N items.
     */
    @Query("""
        DELETE FROM recently_viewed
        WHERE recipeId NOT IN (
            SELECT recipeId FROM recently_viewed
            ORDER BY viewedAt DESC
            LIMIT :keepCount
        )
    """)
    suspend fun pruneRecentlyViewed(keepCount: Int = 50)
}
