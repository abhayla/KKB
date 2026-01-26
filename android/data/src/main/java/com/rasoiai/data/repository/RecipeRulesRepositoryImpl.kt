package com.rasoiai.data.repository

import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.dao.RecipeRulesDao
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.domain.model.FoodCategory
import com.rasoiai.domain.model.NutritionGoal
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.model.RecipeRule
import com.rasoiai.domain.model.RuleType
import com.rasoiai.domain.repository.RecipeRulesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of RecipeRulesRepository with offline-first architecture.
 *
 * Strategy:
 * - All rules and goals stored locally in Room (single source of truth)
 * - Search functionality uses cached recipes
 * - Popular items are derived from local data
 */
@Singleton
class RecipeRulesRepositoryImpl @Inject constructor(
    private val recipeRulesDao: RecipeRulesDao,
    private val recipeDao: RecipeDao
) : RecipeRulesRepository {

    companion object {
        private val POPULAR_INGREDIENTS = listOf(
            "Paneer", "Chicken", "Dal", "Rice", "Aloo (Potato)",
            "Tomato", "Onion", "Palak (Spinach)", "Gobi (Cauliflower)",
            "Mutter (Peas)", "Bhindi (Okra)", "Baigan (Eggplant)"
        )

        private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    // region Recipe Rules

    override fun getAllRules(): Flow<List<RecipeRule>> {
        return recipeRulesDao.getAllRules().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRulesByType(type: RuleType): Flow<List<RecipeRule>> {
        return recipeRulesDao.getRulesByType(type.value).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRuleById(ruleId: String): Flow<RecipeRule?> {
        return recipeRulesDao.getRuleById(ruleId).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getActiveRules(): Flow<List<RecipeRule>> {
        return recipeRulesDao.getActiveRules().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createRule(rule: RecipeRule): Result<RecipeRule> {
        return try {
            val now = LocalDateTime.now()
            val newRule = rule.copy(
                id = if (rule.id.isEmpty()) UUID.randomUUID().toString() else rule.id,
                createdAt = now,
                updatedAt = now
            )

            recipeRulesDao.insertRule(newRule.toEntity())
            Timber.i("Created rule: ${newRule.targetName} (${newRule.type.displayName})")

            Result.success(newRule)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create rule")
            Result.failure(e)
        }
    }

    override suspend fun updateRule(rule: RecipeRule): Result<Unit> {
        return try {
            val updatedRule = rule.copy(updatedAt = LocalDateTime.now())
            recipeRulesDao.updateRule(updatedRule.toEntity())
            Timber.d("Updated rule: ${rule.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update rule")
            Result.failure(e)
        }
    }

    override suspend fun deleteRule(ruleId: String): Result<Unit> {
        return try {
            recipeRulesDao.deleteRule(ruleId)
            Timber.i("Deleted rule: $ruleId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete rule")
            Result.failure(e)
        }
    }

    override suspend fun toggleRuleActive(ruleId: String, isActive: Boolean): Result<Unit> {
        return try {
            val now = LocalDateTime.now().format(dateTimeFormatter)
            recipeRulesDao.updateRuleActive(ruleId, isActive, now)
            Timber.d("Toggled rule $ruleId active: $isActive")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle rule active state")
            Result.failure(e)
        }
    }

    // endregion

    // region Nutrition Goals

    override fun getAllNutritionGoals(): Flow<List<NutritionGoal>> {
        return recipeRulesDao.getAllNutritionGoals().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getNutritionGoalById(goalId: String): Flow<NutritionGoal?> {
        return recipeRulesDao.getNutritionGoalById(goalId).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getActiveNutritionGoals(): Flow<List<NutritionGoal>> {
        return recipeRulesDao.getActiveNutritionGoals().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createNutritionGoal(goal: NutritionGoal): Result<NutritionGoal> {
        return try {
            val now = LocalDateTime.now()
            val newGoal = goal.copy(
                id = if (goal.id.isEmpty()) UUID.randomUUID().toString() else goal.id,
                createdAt = now,
                updatedAt = now
            )

            recipeRulesDao.insertNutritionGoal(newGoal.toEntity())
            Timber.i("Created nutrition goal: ${newGoal.foodCategory.displayName}")

            Result.success(newGoal)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create nutrition goal")
            Result.failure(e)
        }
    }

    override suspend fun updateNutritionGoal(goal: NutritionGoal): Result<Unit> {
        return try {
            val updatedGoal = goal.copy(updatedAt = LocalDateTime.now())
            recipeRulesDao.updateNutritionGoal(updatedGoal.toEntity())
            Timber.d("Updated nutrition goal: ${goal.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update nutrition goal")
            Result.failure(e)
        }
    }

    override suspend fun deleteNutritionGoal(goalId: String): Result<Unit> {
        return try {
            recipeRulesDao.deleteNutritionGoal(goalId)
            Timber.i("Deleted nutrition goal: $goalId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete nutrition goal")
            Result.failure(e)
        }
    }

    override suspend fun toggleNutritionGoalActive(goalId: String, isActive: Boolean): Result<Unit> {
        return try {
            val now = LocalDateTime.now().format(dateTimeFormatter)
            recipeRulesDao.updateNutritionGoalActive(goalId, isActive, now)
            Timber.d("Toggled nutrition goal $goalId active: $isActive")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle nutrition goal active state")
            Result.failure(e)
        }
    }

    override suspend fun updateNutritionGoalProgress(goalId: String, progress: Int): Result<Unit> {
        return try {
            val now = LocalDateTime.now().format(dateTimeFormatter)
            recipeRulesDao.updateNutritionGoalProgress(goalId, progress, now)
            Timber.d("Updated nutrition goal $goalId progress: $progress")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update nutrition goal progress")
            Result.failure(e)
        }
    }

    override suspend fun resetWeeklyProgress(): Result<Unit> {
        return try {
            val now = LocalDateTime.now().format(dateTimeFormatter)
            recipeRulesDao.resetAllNutritionGoalProgress(now)
            Timber.i("Reset weekly progress for all nutrition goals")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to reset weekly progress")
            Result.failure(e)
        }
    }

    // endregion

    // region Search & Suggestions

    override fun searchRecipes(query: String): Flow<List<Recipe>> {
        if (query.isBlank()) {
            return flowOf(emptyList())
        }

        // Search cached recipes by name
        return recipeDao.getAllRecipes().map { entities ->
            entities.filter { entity ->
                entity.name.contains(query, ignoreCase = true) ||
                entity.description.contains(query, ignoreCase = true)
            }.take(10).map { it.toDomain() }
        }
    }

    override fun getPopularRecipes(): Flow<List<Recipe>> {
        // Return a subset of cached recipes as "popular"
        return recipeDao.getAllRecipes().map { entities ->
            entities.take(10).map { it.toDomain() }
        }
    }

    override fun searchIngredients(query: String): Flow<List<String>> {
        if (query.isBlank()) {
            return flowOf(emptyList())
        }

        // Search in popular ingredients list
        val results = POPULAR_INGREDIENTS.filter {
            it.contains(query, ignoreCase = true)
        }

        // Also search in cached recipe ingredients
        return recipeDao.getAllRecipes().map { entities ->
            val recipeIngredients = entities.flatMap { recipe ->
                val recipeDomain = recipe.toDomain()
                recipeDomain.ingredients.map { it.name }
            }.distinct()

            val combinedResults = (results + recipeIngredients.filter {
                it.contains(query, ignoreCase = true)
            }).distinct().take(10)

            combinedResults
        }
    }

    override fun getPopularIngredients(): Flow<List<String>> {
        return flowOf(POPULAR_INGREDIENTS)
    }

    override fun getAvailableFoodCategories(): Flow<List<FoodCategory>> {
        return recipeRulesDao.getActiveNutritionGoals().map { activeGoals ->
            val usedCategories = activeGoals.map { FoodCategory.fromValue(it.foodCategory) }.toSet()
            FoodCategory.entries.filter { it !in usedCategories }
        }
    }

    // endregion
}
