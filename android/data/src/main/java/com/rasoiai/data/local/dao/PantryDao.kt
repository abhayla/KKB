package com.rasoiai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rasoiai.data.local.entity.PantryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {

    @Query("SELECT * FROM pantry_items ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate ASC, name ASC")
    fun getAllItems(): Flow<List<PantryItemEntity>>

    @Query("SELECT * FROM pantry_items WHERE id = :itemId")
    fun getItemById(itemId: String): Flow<PantryItemEntity?>

    @Query("SELECT * FROM pantry_items WHERE id = :itemId")
    suspend fun getItemByIdSync(itemId: String): PantryItemEntity?

    @Query("""
        SELECT * FROM pantry_items
        WHERE expiryDate IS NOT NULL
        AND date(expiryDate) <= date('now', '+3 days')
        AND date(expiryDate) >= date('now')
        ORDER BY expiryDate ASC
    """)
    fun getExpiringSoonItems(): Flow<List<PantryItemEntity>>

    @Query("""
        SELECT * FROM pantry_items
        WHERE expiryDate IS NOT NULL
        AND date(expiryDate) < date('now')
        ORDER BY expiryDate ASC
    """)
    fun getExpiredItems(): Flow<List<PantryItemEntity>>

    @Query("SELECT * FROM pantry_items WHERE category = :category ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate ASC")
    fun getItemsByCategory(category: String): Flow<List<PantryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PantryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PantryItemEntity>)

    @Update
    suspend fun updateItem(item: PantryItemEntity)

    @Query("DELETE FROM pantry_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)

    @Query("DELETE FROM pantry_items WHERE expiryDate IS NOT NULL AND date(expiryDate) < date('now')")
    suspend fun deleteExpiredItems(): Int

    @Query("SELECT COUNT(*) FROM pantry_items")
    fun getItemCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pantry_items")
    suspend fun getItemCountSync(): Int

    @Query("DELETE FROM pantry_items")
    suspend fun clearAll()
}
