package com.rasoiai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.rasoiai.data.local.entity.MealPlanEntity
import com.rasoiai.data.local.entity.MealPlanItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {

    @Query("SELECT * FROM meal_plans WHERE weekStartDate <= :date AND weekEndDate >= :date LIMIT 1")
    fun getMealPlanForDate(date: String): Flow<MealPlanEntity?>

    @Query("SELECT * FROM meal_plans WHERE id = :id")
    suspend fun getMealPlanById(id: String): MealPlanEntity?

    @Query("SELECT * FROM meal_plan_items WHERE mealPlanId = :mealPlanId ORDER BY date, mealType, `order`")
    fun getMealPlanItems(mealPlanId: String): Flow<List<MealPlanItemEntity>>

    @Query("SELECT * FROM meal_plan_items WHERE mealPlanId = :mealPlanId AND date = :date ORDER BY mealType, `order`")
    fun getMealPlanItemsForDate(mealPlanId: String, date: String): Flow<List<MealPlanItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: MealPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlanItems(items: List<MealPlanItemEntity>)

    @Transaction
    suspend fun insertMealPlanWithItems(mealPlan: MealPlanEntity, items: List<MealPlanItemEntity>) {
        insertMealPlan(mealPlan)
        insertMealPlanItems(items)
    }

    @Query("UPDATE meal_plan_items SET isLocked = :isLocked WHERE mealPlanId = :mealPlanId AND date = :date AND mealType = :mealType AND recipeId = :recipeId")
    suspend fun updateMealItemLockState(mealPlanId: String, date: String, mealType: String, recipeId: String, isLocked: Boolean)

    @Query("DELETE FROM meal_plan_items WHERE mealPlanId = :mealPlanId AND date = :date AND mealType = :mealType AND recipeId = :recipeId")
    suspend fun deleteMealPlanItem(mealPlanId: String, date: String, mealType: String, recipeId: String)

    @Query("DELETE FROM meal_plans WHERE id = :id")
    suspend fun deleteMealPlan(id: String)

    @Query("DELETE FROM meal_plan_items WHERE mealPlanId = :mealPlanId")
    suspend fun deleteMealPlanItems(mealPlanId: String)

    @Query("SELECT * FROM meal_plans WHERE isSynced = 0")
    suspend fun getUnsyncedMealPlans(): List<MealPlanEntity>
}
