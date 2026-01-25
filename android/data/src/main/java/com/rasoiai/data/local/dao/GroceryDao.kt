package com.rasoiai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rasoiai.data.local.entity.GroceryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryDao {

    @Query("SELECT * FROM grocery_items ORDER BY category, name")
    fun getAllGroceryItems(): Flow<List<GroceryItemEntity>>

    @Query("SELECT * FROM grocery_items WHERE mealPlanId = :mealPlanId ORDER BY category, name")
    fun getGroceryItemsForMealPlan(mealPlanId: String): Flow<List<GroceryItemEntity>>

    @Query("SELECT * FROM grocery_items WHERE isChecked = 0 ORDER BY category, name")
    fun getUncheckedGroceryItems(): Flow<List<GroceryItemEntity>>

    @Query("SELECT DISTINCT category FROM grocery_items ORDER BY category")
    fun getCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryItem(item: GroceryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryItems(items: List<GroceryItemEntity>)

    @Query("UPDATE grocery_items SET isChecked = :isChecked WHERE id = :id")
    suspend fun updateCheckState(id: String, isChecked: Boolean)

    @Query("UPDATE grocery_items SET isChecked = 1")
    suspend fun markAllChecked()

    @Query("DELETE FROM grocery_items WHERE id = :id")
    suspend fun deleteGroceryItem(id: String)

    @Query("DELETE FROM grocery_items WHERE isChecked = 1")
    suspend fun deleteCheckedItems()

    @Query("DELETE FROM grocery_items WHERE mealPlanId = :mealPlanId")
    suspend fun deleteGroceryItemsForMealPlan(mealPlanId: String)
}
