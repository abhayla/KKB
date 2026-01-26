package com.rasoiai.domain.repository

import com.rasoiai.domain.model.FoodCategory
import com.rasoiai.domain.model.NutritionGoal
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.model.RecipeRule
import com.rasoiai.domain.model.RuleType
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing recipe rules and nutrition goals.
 */
interface RecipeRulesRepository {

    // region Recipe Rules

    /**
     * Get all rules.
     */
    fun getAllRules(): Flow<List<RecipeRule>>

    /**
     * Get rules filtered by type.
     */
    fun getRulesByType(type: RuleType): Flow<List<RecipeRule>>

    /**
     * Get a specific rule by ID.
     */
    fun getRuleById(ruleId: String): Flow<RecipeRule?>

    /**
     * Get only active (enabled) rules.
     */
    fun getActiveRules(): Flow<List<RecipeRule>>

    /**
     * Create a new rule.
     */
    suspend fun createRule(rule: RecipeRule): Result<RecipeRule>

    /**
     * Update an existing rule.
     */
    suspend fun updateRule(rule: RecipeRule): Result<Unit>

    /**
     * Delete a rule.
     */
    suspend fun deleteRule(ruleId: String): Result<Unit>

    /**
     * Toggle rule active/inactive state.
     */
    suspend fun toggleRuleActive(ruleId: String, isActive: Boolean): Result<Unit>

    // endregion

    // region Nutrition Goals

    /**
     * Get all nutrition goals.
     */
    fun getAllNutritionGoals(): Flow<List<NutritionGoal>>

    /**
     * Get a specific nutrition goal by ID.
     */
    fun getNutritionGoalById(goalId: String): Flow<NutritionGoal?>

    /**
     * Get only active nutrition goals.
     */
    fun getActiveNutritionGoals(): Flow<List<NutritionGoal>>

    /**
     * Create a new nutrition goal.
     */
    suspend fun createNutritionGoal(goal: NutritionGoal): Result<NutritionGoal>

    /**
     * Update an existing nutrition goal.
     */
    suspend fun updateNutritionGoal(goal: NutritionGoal): Result<Unit>

    /**
     * Delete a nutrition goal.
     */
    suspend fun deleteNutritionGoal(goalId: String): Result<Unit>

    /**
     * Toggle nutrition goal active/inactive state.
     */
    suspend fun toggleNutritionGoalActive(goalId: String, isActive: Boolean): Result<Unit>

    /**
     * Update progress for a nutrition goal (called when meals are logged).
     */
    suspend fun updateNutritionGoalProgress(goalId: String, progress: Int): Result<Unit>

    /**
     * Reset weekly progress for all nutrition goals (typically called on week start).
     */
    suspend fun resetWeeklyProgress(): Result<Unit>

    // endregion

    // region Search & Suggestions

    /**
     * Search recipes for rule creation.
     */
    fun searchRecipes(query: String): Flow<List<Recipe>>

    /**
     * Get popular recipes for suggestions.
     */
    fun getPopularRecipes(): Flow<List<Recipe>>

    /**
     * Search ingredients for rule creation.
     */
    fun searchIngredients(query: String): Flow<List<String>>

    /**
     * Get popular ingredients for suggestions.
     */
    fun getPopularIngredients(): Flow<List<String>>

    /**
     * Get available food categories that don't have a goal yet.
     */
    fun getAvailableFoodCategories(): Flow<List<FoodCategory>>

    // endregion
}
