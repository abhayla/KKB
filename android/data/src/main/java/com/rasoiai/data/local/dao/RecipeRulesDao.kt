package com.rasoiai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rasoiai.data.local.entity.NutritionGoalEntity
import com.rasoiai.data.local.entity.RecipeRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeRulesDao {

    // ==================== Recipe Rules ====================

    @Query("SELECT * FROM recipe_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<RecipeRuleEntity>>

    @Query("SELECT * FROM recipe_rules WHERE type = :type ORDER BY createdAt DESC")
    fun getRulesByType(type: String): Flow<List<RecipeRuleEntity>>

    @Query("SELECT * FROM recipe_rules WHERE id = :ruleId")
    fun getRuleById(ruleId: String): Flow<RecipeRuleEntity?>

    @Query("SELECT * FROM recipe_rules WHERE id = :ruleId")
    suspend fun getRuleByIdSync(ruleId: String): RecipeRuleEntity?

    @Query("SELECT * FROM recipe_rules WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveRules(): Flow<List<RecipeRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RecipeRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<RecipeRuleEntity>)

    @Update
    suspend fun updateRule(rule: RecipeRuleEntity)

    @Query("DELETE FROM recipe_rules WHERE id = :ruleId")
    suspend fun deleteRule(ruleId: String)

    @Query("DELETE FROM recipe_rules WHERE id IN (:ruleIds)")
    suspend fun deleteRules(ruleIds: List<String>)

    @Query("UPDATE recipe_rules SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :ruleId")
    suspend fun updateRuleActive(ruleId: String, isActive: Boolean, updatedAt: String)

    @Query("SELECT COUNT(*) FROM recipe_rules")
    suspend fun getRuleCount(): Int

    @Query(
        """SELECT * FROM recipe_rules
        WHERE UPPER(targetName) = UPPER(:targetName)
        AND UPPER(action) = UPPER(:action)
        AND UPPER(type) = UPPER(:targetType)
        AND (CASE WHEN :mealSlot IS NULL THEN mealSlot IS NULL ELSE UPPER(mealSlot) = UPPER(:mealSlot) END)
        LIMIT 1"""
    )
    suspend fun findDuplicate(
        targetName: String,
        action: String,
        targetType: String,
        mealSlot: String?
    ): RecipeRuleEntity?

    // Sync-related queries
    @Query("SELECT * FROM recipe_rules WHERE syncStatus = :status")
    suspend fun getRulesBySyncStatus(status: String): List<RecipeRuleEntity>

    @Query("SELECT * FROM recipe_rules WHERE syncStatus = 'PENDING'")
    suspend fun getPendingRules(): List<RecipeRuleEntity>

    @Query("UPDATE recipe_rules SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :ruleId")
    suspend fun updateRuleSyncStatus(ruleId: String, syncStatus: String, updatedAt: String)

    @Query("UPDATE recipe_rules SET syncStatus = :syncStatus WHERE id IN (:ruleIds)")
    suspend fun updateRulesSyncStatus(ruleIds: List<String>, syncStatus: String)

    // ==================== Nutrition Goals ====================

    @Query("SELECT * FROM nutrition_goals ORDER BY createdAt DESC")
    fun getAllNutritionGoals(): Flow<List<NutritionGoalEntity>>

    @Query("SELECT * FROM nutrition_goals WHERE id = :goalId")
    fun getNutritionGoalById(goalId: String): Flow<NutritionGoalEntity?>

    @Query("SELECT * FROM nutrition_goals WHERE id = :goalId")
    suspend fun getNutritionGoalByIdSync(goalId: String): NutritionGoalEntity?

    @Query("SELECT * FROM nutrition_goals WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveNutritionGoals(): Flow<List<NutritionGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutritionGoal(goal: NutritionGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutritionGoals(goals: List<NutritionGoalEntity>)

    @Update
    suspend fun updateNutritionGoal(goal: NutritionGoalEntity)

    @Query("DELETE FROM nutrition_goals WHERE id = :goalId")
    suspend fun deleteNutritionGoal(goalId: String)

    @Query("DELETE FROM nutrition_goals WHERE id IN (:goalIds)")
    suspend fun deleteNutritionGoals(goalIds: List<String>)

    @Query("UPDATE nutrition_goals SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :goalId")
    suspend fun updateNutritionGoalActive(goalId: String, isActive: Boolean, updatedAt: String)

    @Query("UPDATE nutrition_goals SET currentProgress = :progress, updatedAt = :updatedAt WHERE id = :goalId")
    suspend fun updateNutritionGoalProgress(goalId: String, progress: Int, updatedAt: String)

    @Query("UPDATE nutrition_goals SET currentProgress = 0, updatedAt = :updatedAt")
    suspend fun resetAllNutritionGoalProgress(updatedAt: String)

    @Query("SELECT foodCategory FROM nutrition_goals WHERE isActive = 1")
    suspend fun getActiveFoodCategories(): List<String>

    // Sync-related queries
    @Query("SELECT * FROM nutrition_goals WHERE syncStatus = :status")
    suspend fun getNutritionGoalsBySyncStatus(status: String): List<NutritionGoalEntity>

    @Query("SELECT * FROM nutrition_goals WHERE syncStatus = 'PENDING'")
    suspend fun getPendingNutritionGoals(): List<NutritionGoalEntity>

    @Query("UPDATE nutrition_goals SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :goalId")
    suspend fun updateNutritionGoalSyncStatus(goalId: String, syncStatus: String, updatedAt: String)

    @Query("UPDATE nutrition_goals SET syncStatus = :syncStatus WHERE id IN (:goalIds)")
    suspend fun updateNutritionGoalsSyncStatus(goalIds: List<String>, syncStatus: String)
}
