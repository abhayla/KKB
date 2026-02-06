package com.rasoiai.data.repository

import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.dao.RecipeRulesDao
import com.rasoiai.data.local.entity.SyncStatus
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.data.local.mapper.toSyncItem
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.RecipeRuleCreateRequest
import com.rasoiai.data.remote.dto.NutritionGoalCreateRequest
import com.rasoiai.data.remote.dto.SyncRequest
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
 * Real implementation of RecipeRulesRepository with offline-first architecture
 * and backend sync support.
 *
 * Strategy:
 * - All rules and goals stored locally in Room (single source of truth)
 * - Changes are marked as PENDING and synced when online
 * - Search functionality uses cached recipes
 * - Popular items are derived from local data
 */
@Singleton
class RecipeRulesRepositoryImpl @Inject constructor(
    private val recipeRulesDao: RecipeRulesDao,
    private val recipeDao: RecipeDao,
    private val apiService: RasoiApiService,
    private val networkMonitor: NetworkMonitor
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

            // Save locally first with PENDING status
            val isOnline = networkMonitor.isOnline.first()
            val syncStatus = if (isOnline) SyncStatus.SYNCED else SyncStatus.PENDING
            recipeRulesDao.insertRule(newRule.toEntity(syncStatus))
            Timber.i("Created rule locally: ${newRule.targetName} (${newRule.type.displayName})")

            // Try to sync to backend if online
            if (isOnline) {
                try {
                    val request = RecipeRuleCreateRequest(
                        targetType = newRule.type.value,
                        action = newRule.action.value,
                        targetId = newRule.targetId.takeIf { it.isNotEmpty() },
                        targetName = newRule.targetName,
                        frequencyType = newRule.frequency.type.value,
                        frequencyCount = newRule.frequency.count,
                        frequencyDays = newRule.frequency.specificDays?.joinToString(",") { it.name },
                        enforcement = newRule.enforcement.value,
                        mealSlot = newRule.mealSlot?.value,
                        isActive = newRule.isActive
                    )
                    apiService.createRecipeRule(request)
                    Timber.i("Synced rule to backend: ${newRule.id}")
                } catch (e: Exception) {
                    // Mark as pending for later sync
                    val nowStr = now.format(dateTimeFormatter)
                    recipeRulesDao.updateRuleSyncStatus(newRule.id, SyncStatus.PENDING, nowStr)
                    Timber.w(e, "Failed to sync rule to backend, marked as PENDING")
                }
            }

            Result.success(newRule)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create rule")
            Result.failure(e)
        }
    }

    override suspend fun updateRule(rule: RecipeRule): Result<Unit> {
        return try {
            val now = LocalDateTime.now()
            val updatedRule = rule.copy(updatedAt = now)

            // Update locally with PENDING status
            val isOnline = networkMonitor.isOnline.first()
            val syncStatus = if (isOnline) SyncStatus.SYNCED else SyncStatus.PENDING
            recipeRulesDao.updateRule(updatedRule.toEntity(syncStatus))
            Timber.d("Updated rule locally: ${rule.id}")

            // Try to sync to backend if online
            if (isOnline) {
                try {
                    val request = com.rasoiai.data.remote.dto.RecipeRuleUpdateRequest(
                        targetType = updatedRule.type.value,
                        action = updatedRule.action.value,
                        targetId = updatedRule.targetId.takeIf { it.isNotEmpty() },
                        targetName = updatedRule.targetName,
                        frequencyType = updatedRule.frequency.type.value,
                        frequencyCount = updatedRule.frequency.count,
                        frequencyDays = updatedRule.frequency.specificDays?.joinToString(",") { it.name },
                        enforcement = updatedRule.enforcement.value,
                        mealSlot = updatedRule.mealSlot?.value,
                        isActive = updatedRule.isActive
                    )
                    apiService.updateRecipeRule(rule.id, request)
                    Timber.i("Synced rule update to backend: ${rule.id}")
                } catch (e: Exception) {
                    val nowStr = now.format(dateTimeFormatter)
                    recipeRulesDao.updateRuleSyncStatus(rule.id, SyncStatus.PENDING, nowStr)
                    Timber.w(e, "Failed to sync rule update to backend")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update rule")
            Result.failure(e)
        }
    }

    override suspend fun deleteRule(ruleId: String): Result<Unit> {
        return try {
            // Delete locally first
            recipeRulesDao.deleteRule(ruleId)
            Timber.i("Deleted rule locally: $ruleId")

            // Try to sync to backend if online
            if (networkMonitor.isOnline.first()) {
                try {
                    apiService.deleteRecipeRule(ruleId)
                    Timber.i("Synced rule deletion to backend: $ruleId")
                } catch (e: Exception) {
                    // Rule is already deleted locally, log warning
                    Timber.w(e, "Failed to sync rule deletion to backend")
                }
            }

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
            recipeRulesDao.updateRuleSyncStatus(ruleId, SyncStatus.PENDING, now)
            Timber.d("Toggled rule $ruleId active: $isActive")

            // Try to sync to backend if online
            if (networkMonitor.isOnline.first()) {
                try {
                    val request = com.rasoiai.data.remote.dto.RecipeRuleUpdateRequest(isActive = isActive)
                    apiService.updateRecipeRule(ruleId, request)
                    recipeRulesDao.updateRuleSyncStatus(ruleId, SyncStatus.SYNCED, now)
                    Timber.i("Synced toggle to backend: $ruleId")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to sync toggle to backend")
                }
            }

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

            // Save locally first with PENDING status
            val isOnline = networkMonitor.isOnline.first()
            val syncStatus = if (isOnline) SyncStatus.SYNCED else SyncStatus.PENDING
            recipeRulesDao.insertNutritionGoal(newGoal.toEntity(syncStatus))
            Timber.i("Created nutrition goal locally: ${newGoal.foodCategory.displayName}")

            // Try to sync to backend if online
            if (isOnline) {
                try {
                    val request = NutritionGoalCreateRequest(
                        foodCategory = newGoal.foodCategory.value,
                        weeklyTarget = newGoal.weeklyTarget,
                        enforcement = newGoal.enforcement.value,
                        isActive = newGoal.isActive
                    )
                    apiService.createNutritionGoal(request)
                    Timber.i("Synced nutrition goal to backend: ${newGoal.id}")
                } catch (e: Exception) {
                    val nowStr = now.format(dateTimeFormatter)
                    recipeRulesDao.updateNutritionGoalSyncStatus(newGoal.id, SyncStatus.PENDING, nowStr)
                    Timber.w(e, "Failed to sync nutrition goal to backend")
                }
            }

            Result.success(newGoal)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create nutrition goal")
            Result.failure(e)
        }
    }

    override suspend fun updateNutritionGoal(goal: NutritionGoal): Result<Unit> {
        return try {
            val now = LocalDateTime.now()
            val updatedGoal = goal.copy(updatedAt = now)

            val isOnline = networkMonitor.isOnline.first()
            val syncStatus = if (isOnline) SyncStatus.SYNCED else SyncStatus.PENDING
            recipeRulesDao.updateNutritionGoal(updatedGoal.toEntity(syncStatus))
            Timber.d("Updated nutrition goal locally: ${goal.id}")

            // Try to sync to backend if online
            if (isOnline) {
                try {
                    val request = com.rasoiai.data.remote.dto.NutritionGoalUpdateRequest(
                        foodCategory = updatedGoal.foodCategory.value,
                        weeklyTarget = updatedGoal.weeklyTarget,
                        currentProgress = updatedGoal.currentProgress,
                        enforcement = updatedGoal.enforcement.value,
                        isActive = updatedGoal.isActive
                    )
                    apiService.updateNutritionGoal(goal.id, request)
                    Timber.i("Synced nutrition goal update to backend: ${goal.id}")
                } catch (e: Exception) {
                    val nowStr = now.format(dateTimeFormatter)
                    recipeRulesDao.updateNutritionGoalSyncStatus(goal.id, SyncStatus.PENDING, nowStr)
                    Timber.w(e, "Failed to sync nutrition goal update to backend")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update nutrition goal")
            Result.failure(e)
        }
    }

    override suspend fun deleteNutritionGoal(goalId: String): Result<Unit> {
        return try {
            recipeRulesDao.deleteNutritionGoal(goalId)
            Timber.i("Deleted nutrition goal locally: $goalId")

            // Try to sync to backend if online
            if (networkMonitor.isOnline.first()) {
                try {
                    apiService.deleteNutritionGoal(goalId)
                    Timber.i("Synced nutrition goal deletion to backend: $goalId")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to sync nutrition goal deletion to backend")
                }
            }

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
            recipeRulesDao.updateNutritionGoalSyncStatus(goalId, SyncStatus.PENDING, now)
            Timber.d("Toggled nutrition goal $goalId active: $isActive")

            // Try to sync to backend if online
            if (networkMonitor.isOnline.first()) {
                try {
                    val request = com.rasoiai.data.remote.dto.NutritionGoalUpdateRequest(isActive = isActive)
                    apiService.updateNutritionGoal(goalId, request)
                    recipeRulesDao.updateNutritionGoalSyncStatus(goalId, SyncStatus.SYNCED, now)
                    Timber.i("Synced toggle to backend: $goalId")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to sync toggle to backend")
                }
            }

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
            recipeRulesDao.updateNutritionGoalSyncStatus(goalId, SyncStatus.PENDING, now)
            Timber.d("Updated nutrition goal $goalId progress: $progress")

            // Try to sync to backend if online
            if (networkMonitor.isOnline.first()) {
                try {
                    val request = com.rasoiai.data.remote.dto.NutritionGoalUpdateRequest(currentProgress = progress)
                    apiService.updateNutritionGoal(goalId, request)
                    recipeRulesDao.updateNutritionGoalSyncStatus(goalId, SyncStatus.SYNCED, now)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to sync progress to backend")
                }
            }

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

    // region Sync

    /**
     * Sync all pending changes with the backend.
     * Call this when the app comes online or on app startup.
     */
    suspend fun syncWithBackend(): Result<Unit> {
        return try {
            if (!networkMonitor.isOnline.first()) {
                Timber.d("Offline - skipping sync")
                return Result.success(Unit)
            }

            // Get pending rules and goals
            val pendingRules = recipeRulesDao.getPendingRules()
            val pendingGoals = recipeRulesDao.getPendingNutritionGoals()

            if (pendingRules.isEmpty() && pendingGoals.isEmpty()) {
                Timber.d("No pending changes to sync")
                return Result.success(Unit)
            }

            Timber.i("Syncing ${pendingRules.size} rules and ${pendingGoals.size} goals")

            // Build sync request
            val syncRequest = SyncRequest(
                recipeRules = pendingRules.map { it.toSyncItem() },
                nutritionGoals = pendingGoals.map { it.toSyncItem() },
                lastSyncTime = null // TODO: Store and use last sync time
            )

            // Send to server
            val response = apiService.syncRecipeRules(syncRequest)

            // Update local sync status for synced items
            if (response.syncedRuleIds.isNotEmpty()) {
                recipeRulesDao.updateRulesSyncStatus(response.syncedRuleIds, SyncStatus.SYNCED)
            }
            if (response.syncedGoalIds.isNotEmpty()) {
                recipeRulesDao.updateNutritionGoalsSyncStatus(response.syncedGoalIds, SyncStatus.SYNCED)
            }

            // Handle conflicts - server version wins, update local
            for (serverRule in response.serverRecipeRules) {
                if (serverRule.id in response.conflictRuleIds) {
                    recipeRulesDao.insertRule(serverRule.toEntity())
                    Timber.d("Resolved conflict for rule: ${serverRule.id}")
                }
            }
            for (serverGoal in response.serverNutritionGoals) {
                if (serverGoal.id in response.conflictGoalIds) {
                    recipeRulesDao.insertNutritionGoal(serverGoal.toEntity())
                    Timber.d("Resolved conflict for goal: ${serverGoal.id}")
                }
            }

            // Handle deletions
            if (response.deletedRuleIds.isNotEmpty()) {
                recipeRulesDao.deleteRules(response.deletedRuleIds)
            }
            if (response.deletedGoalIds.isNotEmpty()) {
                recipeRulesDao.deleteNutritionGoals(response.deletedGoalIds)
            }

            Timber.i("Sync completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync with backend")
            Result.failure(e)
        }
    }

    /**
     * Fetch all rules and goals from the backend and update local database.
     * Call this on first launch or to force refresh.
     */
    suspend fun fetchFromBackend(): Result<Unit> {
        return try {
            if (!networkMonitor.isOnline.first()) {
                return Result.failure(Exception("No network connection"))
            }

            // Fetch rules from backend
            val rulesResponse = apiService.getRecipeRules()
            val goalsResponse = apiService.getNutritionGoals()

            // Update local database
            recipeRulesDao.insertRules(rulesResponse.rules.map { it.toEntity() })
            recipeRulesDao.insertNutritionGoals(goalsResponse.goals.map { it.toEntity() })

            Timber.i("Fetched ${rulesResponse.totalCount} rules and ${goalsResponse.totalCount} goals from backend")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch from backend")
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
