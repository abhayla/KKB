package com.rasoiai.data.repository

import android.content.Context
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.dao.FavoriteDao
import com.rasoiai.data.local.dao.OfflineQueueDao
import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.dao.RecipeRulesDao
import com.rasoiai.data.local.entity.KnownIngredientEntity
import com.rasoiai.data.local.entity.OfflineQueueEntity
import com.rasoiai.data.local.entity.SyncStatus
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.data.local.mapper.toSyncItem
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.NutritionGoalCreateRequest
import com.rasoiai.data.remote.dto.RecipeRuleCreateRequest
import com.rasoiai.data.remote.dto.SyncRequest
import com.rasoiai.data.remote.dto.toDomain
import com.rasoiai.data.sync.SyncWorker
import com.rasoiai.domain.model.DuplicateRuleException
import com.rasoiai.domain.model.FoodCategory
import com.rasoiai.domain.model.NutritionGoal
import com.rasoiai.domain.model.OfflineActionType
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.model.RecipeRule
import com.rasoiai.domain.model.RuleType
import com.rasoiai.domain.repository.RecipeRulesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.IOException
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
    private val favoriteDao: FavoriteDao,
    private val apiService: RasoiApiService,
    private val networkMonitor: NetworkMonitor,
    private val offlineQueueDao: OfflineQueueDao,
    @ApplicationContext private val context: Context
) : RecipeRulesRepository {

    companion object {
        internal val POPULAR_INGREDIENTS = listOf(
            // Proteins
            "Paneer", "Chicken", "Mutton", "Fish", "Prawns", "Egg", "Tofu",
            // Dals & Legumes
            "Dal", "Chana Dal", "Moong Dal", "Toor Dal", "Masoor Dal",
            "Rajma", "Chole",
            // Vegetables
            "Aloo", "Tamatar", "Pyaz", "Palak", "Gobi",
            "Matar", "Bhindi", "Baingan", "Gajar", "Shimla Mirch",
            "Mushroom", "Methi", "Karela", "Lauki", "Bandh Gobi",
            // Dairy
            "Dahi", "Ghee", "Malai",
            // Grains
            "Chawal", "Atta", "Suji", "Besan",
            // Beverages & Staples
            "Chai", "Moringa",
            // Nuts
            "Cashew", "Badam", "Nariyal"
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
            // Check for duplicate rule locally before inserting
            val sortedMealSlots = if (rule.mealSlots.isNotEmpty()) {
                rule.mealSlots.sortedBy { it.ordinal }.joinToString(",") { it.value }
            } else null
            val existingDup = recipeRulesDao.findDuplicate(
                targetName = rule.targetName,
                action = rule.action.value,
                mealSlots = sortedMealSlots
            )
            if (existingDup != null) {
                return Result.failure(
                    DuplicateRuleException(
                        message = "A ${rule.action.displayName} rule for '${rule.targetName}' already exists",
                        existingRuleId = existingDup.id
                    )
                )
            }

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
                        mealSlot = if (newRule.mealSlots.isNotEmpty()) newRule.mealSlots.joinToString(",") { it.value } else null,
                        isActive = newRule.isActive,
                        forceOverride = newRule.forceOverride
                    )
                    val response = apiService.createRecipeRule(request)
                    if (response.isSuccessful) {
                        Timber.i("Synced rule to backend: ${newRule.id}")
                    } else if (response.code() == 409) {
                        // Check if it's a family safety conflict
                        val errorBody = response.errorBody()?.string()
                        if (errorBody != null && errorBody.contains("family_safety")) {
                            // Delete the locally saved rule since it was rejected
                            recipeRulesDao.deleteRule(newRule.id)
                            val conflictResponse = com.google.gson.Gson().fromJson(
                                errorBody,
                                com.rasoiai.data.remote.dto.ConflictResponseDto::class.java
                            )
                            return Result.failure(
                                com.rasoiai.domain.model.FamilyConflictException(
                                    message = conflictResponse.detail,
                                    conflictDetails = conflictResponse.conflictDetails.map {
                                        com.rasoiai.domain.model.ConflictDetail(
                                            memberName = it.memberName,
                                            condition = it.condition,
                                            keyword = it.keyword,
                                            ruleTarget = it.ruleTarget
                                        )
                                    }
                                )
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    val nowStr = now.format(dateTimeFormatter)
                    recipeRulesDao.updateRuleSyncStatus(newRule.id, SyncStatus.PENDING, nowStr)
                    queueRuleAction(newRule, OfflineActionType.CREATE_RECIPE_RULE)
                    Timber.w(e, "Network error syncing rule to backend, queued for retry")
                } catch (e: Exception) {
                    val nowStr = now.format(dateTimeFormatter)
                    recipeRulesDao.updateRuleSyncStatus(newRule.id, SyncStatus.PENDING, nowStr)
                    queueRuleAction(newRule, OfflineActionType.CREATE_RECIPE_RULE)
                    Timber.w(e, "Failed to sync rule to backend, queued for retry")
                }
            } else {
                // Offline: queue for later sync
                queueRuleAction(newRule, OfflineActionType.CREATE_RECIPE_RULE)
            }

            Result.success(newRule)
        } catch (e: CancellationException) {
            throw e
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
                        mealSlot = if (updatedRule.mealSlots.isNotEmpty()) updatedRule.mealSlots.joinToString(",") { it.value } else null,
                        isActive = updatedRule.isActive
                    )
                    apiService.updateRecipeRule(rule.id, request)
                    Timber.i("Synced rule update to backend: ${rule.id}")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    val nowStr = now.format(dateTimeFormatter)
                    recipeRulesDao.updateRuleSyncStatus(rule.id, SyncStatus.PENDING, nowStr)
                    Timber.w(e, "Network error syncing rule update to backend")
                } catch (e: Exception) {
                    val nowStr = now.format(dateTimeFormatter)
                    recipeRulesDao.updateRuleSyncStatus(rule.id, SyncStatus.PENDING, nowStr)
                    Timber.w(e, "Failed to sync rule update to backend")
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    queueDeleteRuleAction(ruleId)
                    Timber.w(e, "Network error syncing rule deletion, queued for retry")
                } catch (e: Exception) {
                    queueDeleteRuleAction(ruleId)
                    Timber.w(e, "Failed to sync rule deletion, queued for retry")
                }
            } else {
                // Offline: queue for later sync
                queueDeleteRuleAction(ruleId)
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete rule")
            Result.failure(e)
        }
    }

    private suspend fun queueDeleteRuleAction(ruleId: String) {
        try {
            offlineQueueDao.insertAction(
                OfflineQueueEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    actionType = OfflineActionType.DELETE_RECIPE_RULE.value,
                    payload = """{"rule_id":"$ruleId"}""",
                    createdAt = System.currentTimeMillis()
                )
            )
            Timber.d("Queued DELETE_RECIPE_RULE for rule=$ruleId")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Failed to queue rule delete action")
        }
    }

    override suspend fun toggleRuleActive(ruleId: String, isActive: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val now = LocalDateTime.now().format(dateTimeFormatter)
                // 1. Update Room immediately (source of truth)
                recipeRulesDao.updateRuleActive(ruleId, isActive, now)
                recipeRulesDao.updateRuleSyncStatus(ruleId, SyncStatus.PENDING, now)
                Timber.d("Toggled rule $ruleId active: $isActive")

                // 2. Queue offline action for reliability
                val queueId = UUID.randomUUID().toString()
                val payload = """{"rule_id":"$ruleId","is_active":$isActive}"""
                offlineQueueDao.insertAction(
                    OfflineQueueEntity(
                        id = queueId,
                        actionType = OfflineActionType.TOGGLE_RECIPE_RULE.value,
                        payload = payload,
                        createdAt = System.currentTimeMillis()
                    )
                )

                // 3. Fast path: try immediate sync if online
                val isOnline = withTimeoutOrNull(500L) { networkMonitor.isOnline.first() } ?: false
                if (isOnline) {
                    try {
                        val request = com.rasoiai.data.remote.dto.RecipeRuleUpdateRequest(isActive = isActive)
                        apiService.updateRecipeRule(ruleId, request)
                        recipeRulesDao.updateRuleSyncStatus(ruleId, SyncStatus.SYNCED, now)
                        offlineQueueDao.deleteAction(queueId)
                        Timber.i("Synced toggle to backend: $ruleId")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: IOException) {
                        Timber.w(e, "Network error syncing rule toggle, queued for WorkManager")
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to sync toggle, queued for WorkManager")
                    }
                }

                // 4. Trigger WorkManager for reliability (best-effort)
                try {
                    SyncWorker.triggerImmediateSync(context)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "Could not trigger immediate sync, queued action will be processed later")
                }

                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle rule active state")
                Result.failure(e)
            }
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    val nowStr = now.format(dateTimeFormatter)
                    recipeRulesDao.updateNutritionGoalSyncStatus(newGoal.id, SyncStatus.PENDING, nowStr)
                    Timber.w(e, "Network error syncing nutrition goal to backend")
                } catch (e: Exception) {
                    val nowStr = now.format(dateTimeFormatter)
                    recipeRulesDao.updateNutritionGoalSyncStatus(newGoal.id, SyncStatus.PENDING, nowStr)
                    Timber.w(e, "Failed to sync nutrition goal to backend")
                }
            }

            Result.success(newGoal)
        } catch (e: CancellationException) {
            throw e
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    val nowStr = now.format(dateTimeFormatter)
                    recipeRulesDao.updateNutritionGoalSyncStatus(goal.id, SyncStatus.PENDING, nowStr)
                    Timber.w(e, "Network error syncing nutrition goal update to backend")
                } catch (e: Exception) {
                    val nowStr = now.format(dateTimeFormatter)
                    recipeRulesDao.updateNutritionGoalSyncStatus(goal.id, SyncStatus.PENDING, nowStr)
                    Timber.w(e, "Failed to sync nutrition goal update to backend")
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    Timber.w(e, "Network error syncing nutrition goal deletion to backend")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to sync nutrition goal deletion to backend")
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete nutrition goal")
            Result.failure(e)
        }
    }

    override suspend fun toggleNutritionGoalActive(goalId: String, isActive: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val now = LocalDateTime.now().format(dateTimeFormatter)
                // 1. Update Room immediately (source of truth)
                recipeRulesDao.updateNutritionGoalActive(goalId, isActive, now)
                recipeRulesDao.updateNutritionGoalSyncStatus(goalId, SyncStatus.PENDING, now)
                Timber.d("Toggled nutrition goal $goalId active: $isActive")

                // 2. Queue offline action for reliability
                val queueId = UUID.randomUUID().toString()
                val payload = """{"goal_id":"$goalId","is_active":$isActive}"""
                offlineQueueDao.insertAction(
                    OfflineQueueEntity(
                        id = queueId,
                        actionType = OfflineActionType.TOGGLE_NUTRITION_GOAL.value,
                        payload = payload,
                        createdAt = System.currentTimeMillis()
                    )
                )

                // 3. Fast path: try immediate sync if online
                val isOnline = withTimeoutOrNull(500L) { networkMonitor.isOnline.first() } ?: false
                if (isOnline) {
                    try {
                        val request = com.rasoiai.data.remote.dto.NutritionGoalUpdateRequest(isActive = isActive)
                        apiService.updateNutritionGoal(goalId, request)
                        recipeRulesDao.updateNutritionGoalSyncStatus(goalId, SyncStatus.SYNCED, now)
                        offlineQueueDao.deleteAction(queueId)
                        Timber.i("Synced toggle to backend: $goalId")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: IOException) {
                        Timber.w(e, "Network error syncing nutrition goal toggle, queued for WorkManager")
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to sync toggle, queued for WorkManager")
                    }
                }

                // 4. Trigger WorkManager for reliability (best-effort)
                try {
                    SyncWorker.triggerImmediateSync(context)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "Could not trigger immediate sync, queued action will be processed later")
                }

                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle nutrition goal active state")
                Result.failure(e)
            }
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    Timber.w(e, "Network error syncing progress to backend")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to sync progress to backend")
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
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
        } catch (e: CancellationException) {
            throw e
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "HTTP ${e.code()} on sync with backend")
            Result.failure(e)
        } catch (e: IOException) {
            Timber.w(e, "Network error on sync with backend")
            Result.failure(e)
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "HTTP ${e.code()} fetching from backend")
            Result.failure(e)
        } catch (e: IOException) {
            Timber.w(e, "Network error fetching from backend")
            Result.failure(e)
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

        return flow {
            // First try local cache (name + description + ingredient names)
            val localResults = recipeDao.getAllRecipes().first()
                .filter { entity ->
                    entity.name.contains(query, ignoreCase = true) ||
                    entity.description.contains(query, ignoreCase = true) ||
                    entity.toDomain().ingredients.any { ing ->
                        ing.name.contains(query, ignoreCase = true)
                    }
                }
                .take(10)
                .map { it.toDomain() }

            if (localResults.isNotEmpty()) {
                emit(localResults)
                return@flow
            }

            // Fallback to AI catalog if local cache is empty/no results
            if (networkMonitor.isOnline.first()) {
                try {
                    val favoriteNames = getFavoriteNamesForCatalog()
                    val catalogResults = apiService.searchAiRecipeCatalog(
                        query = query,
                        favorites = favoriteNames,
                        limit = 10
                    )
                    emit(catalogResults.map { it.toDomain() })
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    Timber.w(e, "Network error searching AI recipe catalog")
                    emit(emptyList())
                } catch (e: Exception) {
                    Timber.w(e, "Failed to search AI recipe catalog")
                    emit(emptyList())
                }
            } else {
                emit(emptyList())
            }
        }
    }

    override fun getPopularRecipes(): Flow<List<Recipe>> {
        return flow {
            // First try local cache
            val localResults = recipeDao.getAllRecipes().first()
                .take(10)
                .map { it.toDomain() }

            if (localResults.isNotEmpty()) {
                emit(localResults)
                return@flow
            }

            // Fallback to AI catalog for popular recipes (empty query = popular)
            if (networkMonitor.isOnline.first()) {
                try {
                    val favoriteNames = getFavoriteNamesForCatalog()
                    val catalogResults = apiService.searchAiRecipeCatalog(
                        query = "",
                        favorites = favoriteNames,
                        limit = 10
                    )
                    emit(catalogResults.map { it.toDomain() })
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    Timber.w(e, "Network error getting popular recipes from AI catalog")
                    emit(emptyList())
                } catch (e: Exception) {
                    Timber.w(e, "Failed to get popular recipes from AI catalog")
                    emit(emptyList())
                }
            } else {
                emit(emptyList())
            }
        }
    }

    /**
     * Get comma-separated favorite recipe names for AI catalog sorting.
     */
    private suspend fun getFavoriteNamesForCatalog(): String? {
        return try {
            val favorites = favoriteDao.getAllFavorites().first()
            if (favorites.isEmpty()) return null
            // FavoriteEntity stores recipeId, we need the recipe name
            val names = favorites.mapNotNull { fav ->
                try {
                    val recipes = recipeDao.getAllRecipes().first()
                    recipes.find { it.id == fav.recipeId }?.name
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
            }
            names.joinToString(",").takeIf { it.isNotEmpty() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Failed to get favorite names for catalog")
            null
        }
    }

    override fun searchIngredients(query: String): Flow<List<String>> {
        if (query.isBlank()) {
            return flowOf(emptyList())
        }

        // Search persistent known_ingredients table (populated from popular + recipe cache)
        return recipeRulesDao.searchKnownIngredients(query).map { dbResults ->
            // Also include hardcoded list as fallback (always available, no DB seed needed)
            // Bidirectional match: "Egg" matches query "Eggs" and vice versa
            val hardcodedMatches = POPULAR_INGREDIENTS.filter {
                it.contains(query, ignoreCase = true) || query.contains(it, ignoreCase = true)
            }
            (hardcodedMatches + dbResults).distinct().take(10)
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

    override suspend fun persistIngredientsFromRecipes(recipes: List<Recipe>) {
        try {
            val ingredientNames = recipes
                .flatMap { it.ingredients.map { ing -> ing.name } }
                .distinct()
            if (ingredientNames.isEmpty()) return

            val entities = ingredientNames.map {
                KnownIngredientEntity(name = it, source = "recipe_cache")
            }
            recipeRulesDao.insertKnownIngredients(entities) // INSERT OR IGNORE — no duplicates
            Timber.d("Persisted ${ingredientNames.size} ingredient names from ${recipes.size} recipes")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Failed to persist ingredients from recipes")
        }
    }

    // endregion

    // region Offline Queue Helpers

    private suspend fun queueRuleAction(rule: RecipeRule, actionType: OfflineActionType) {
        try {
            val payload = when (actionType) {
                OfflineActionType.CREATE_RECIPE_RULE -> """{"rule_id":"${rule.id}","target_type":"${rule.type.value}","action":"${rule.action.value}","target_name":"${rule.targetName}","frequency_type":"${rule.frequency.type.value}","frequency_count":${rule.frequency.count},"enforcement":"${rule.enforcement.value}","is_active":${rule.isActive},"force_override":${rule.forceOverride}}"""
                OfflineActionType.DELETE_RECIPE_RULE -> """{"rule_id":"${rule.id}"}"""
                else -> return
            }
            offlineQueueDao.insertAction(
                OfflineQueueEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    actionType = actionType.value,
                    payload = payload,
                    createdAt = System.currentTimeMillis()
                )
            )
            Timber.d("Queued ${actionType.value} for rule=${rule.id}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Failed to queue rule action")
        }
    }

    // endregion
}
