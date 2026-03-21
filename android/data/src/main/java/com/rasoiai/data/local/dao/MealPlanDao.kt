package com.rasoiai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.rasoiai.data.local.entity.MealPlanEntity
import com.rasoiai.data.local.entity.MealPlanFestivalEntity
import com.rasoiai.data.local.entity.MealPlanItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {

    // ==================== Meal Plans ====================

    @Query("SELECT * FROM meal_plans WHERE weekStartDate <= :date AND weekEndDate >= :date LIMIT 1")
    fun getMealPlanForDate(date: String): Flow<MealPlanEntity?>

    @Query("SELECT * FROM meal_plans WHERE id = :id")
    suspend fun getMealPlanById(id: String): MealPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: MealPlanEntity)

    @Query("UPDATE meal_plans SET isSynced = :isSynced WHERE id = :id")
    suspend fun updateSyncStatus(id: String, isSynced: Boolean)

    @Query("DELETE FROM meal_plans WHERE id = :id")
    suspend fun deleteMealPlan(id: String)

    @Query("SELECT * FROM meal_plans WHERE isSynced = 0")
    suspend fun getUnsyncedMealPlans(): List<MealPlanEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM meal_plans WHERE weekStartDate <= :date AND weekEndDate >= :date LIMIT 1)")
    suspend fun hasMealPlanForDate(date: String): Boolean

    // ==================== Meal Plan Items ====================

    @Query("SELECT * FROM meal_plan_items WHERE mealPlanId = :mealPlanId ORDER BY date, mealType, `order`")
    fun getMealPlanItems(mealPlanId: String): Flow<List<MealPlanItemEntity>>

    @Query("SELECT * FROM meal_plan_items WHERE mealPlanId = :mealPlanId ORDER BY date, mealType, `order`")
    suspend fun getMealPlanItemsSync(mealPlanId: String): List<MealPlanItemEntity>

    @Query("SELECT * FROM meal_plan_items WHERE mealPlanId = :mealPlanId AND date = :date ORDER BY mealType, `order`")
    fun getMealPlanItemsForDate(mealPlanId: String, date: String): Flow<List<MealPlanItemEntity>>

    @Query("SELECT * FROM meal_plan_items WHERE mealPlanId = :mealPlanId AND date = :date AND mealType = :mealType ORDER BY `order`")
    suspend fun getMealPlanItemsForDateAndType(mealPlanId: String, date: String, mealType: String): List<MealPlanItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlanItem(item: MealPlanItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlanItems(items: List<MealPlanItemEntity>)

    @Query("UPDATE meal_plan_items SET isLocked = :isLocked WHERE mealPlanId = :mealPlanId AND date = :date AND mealType = :mealType AND recipeId = :recipeId")
    suspend fun updateMealItemLockState(mealPlanId: String, date: String, mealType: String, recipeId: String, isLocked: Boolean)

    @Query("DELETE FROM meal_plan_items WHERE mealPlanId = :mealPlanId AND date = :date AND mealType = :mealType AND recipeId = :recipeId")
    suspend fun deleteMealPlanItem(mealPlanId: String, date: String, mealType: String, recipeId: String)

    @Query("DELETE FROM meal_plan_items WHERE mealPlanId = :mealPlanId")
    suspend fun deleteMealPlanItems(mealPlanId: String)

    // ==================== Festivals ====================

    @Query("SELECT * FROM meal_plan_festivals WHERE mealPlanId = :mealPlanId")
    suspend fun getFestivalsForMealPlan(mealPlanId: String): List<MealPlanFestivalEntity>

    @Query("SELECT * FROM meal_plan_festivals WHERE mealPlanId = :mealPlanId AND date = :date LIMIT 1")
    suspend fun getFestivalForDate(mealPlanId: String, date: String): MealPlanFestivalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFestivals(festivals: List<MealPlanFestivalEntity>)

    @Query("DELETE FROM meal_plan_festivals WHERE mealPlanId = :mealPlanId")
    suspend fun deleteFestivalsForMealPlan(mealPlanId: String)

    // ==================== Overlap Cleanup ====================

    @Query("DELETE FROM meal_plan_festivals WHERE mealPlanId IN (SELECT id FROM meal_plans WHERE weekStartDate <= :weekEndDate AND weekEndDate >= :weekStartDate AND id != :excludeId)")
    suspend fun deleteFestivalsForOverlappingPlans(weekStartDate: String, weekEndDate: String, excludeId: String)

    @Query("DELETE FROM meal_plans WHERE weekStartDate <= :weekEndDate AND weekEndDate >= :weekStartDate AND id != :excludeId")
    suspend fun deleteOverlappingPlans(weekStartDate: String, weekEndDate: String, excludeId: String)

    // ==================== Transactions ====================

    @Transaction
    suspend fun insertMealPlanWithItems(
        mealPlan: MealPlanEntity,
        items: List<MealPlanItemEntity>,
        festivals: List<MealPlanFestivalEntity> = emptyList()
    ) {
        insertMealPlan(mealPlan)
        insertMealPlanItems(items)
        if (festivals.isNotEmpty()) {
            insertFestivals(festivals)
        }
    }

    @Transaction
    suspend fun replaceMealPlan(
        mealPlan: MealPlanEntity,
        items: List<MealPlanItemEntity>,
        festivals: List<MealPlanFestivalEntity> = emptyList()
    ) {
        // Clean up any older plans with overlapping date ranges (different IDs)
        // Festivals first (no CASCADE FK), then plans (items auto-cascade via FK)
        deleteFestivalsForOverlappingPlans(mealPlan.weekStartDate, mealPlan.weekEndDate, mealPlan.id)
        deleteOverlappingPlans(mealPlan.weekStartDate, mealPlan.weekEndDate, mealPlan.id)
        // Clean up this plan's existing data
        deleteMealPlanItems(mealPlan.id)
        deleteFestivalsForMealPlan(mealPlan.id)
        insertMealPlanWithItems(mealPlan, items, festivals)
    }

    // ==================== Lock State Persistence ====================

    @Query("""
        UPDATE meal_plan_items
        SET isDayLocked = :isLocked
        WHERE mealPlanId = :mealPlanId AND date = :date
    """)
    suspend fun updateDayLockState(mealPlanId: String, date: String, isLocked: Boolean)

    @Query("""
        UPDATE meal_plan_items
        SET isMealTypeLocked = :isLocked
        WHERE mealPlanId = :mealPlanId AND date = :date AND mealType = :mealType
    """)
    suspend fun updateMealTypeLockState(mealPlanId: String, date: String, mealType: String, isLocked: Boolean)
}
