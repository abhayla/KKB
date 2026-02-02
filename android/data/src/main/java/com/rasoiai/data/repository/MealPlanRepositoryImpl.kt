package com.rasoiai.data.repository

import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.dao.MealPlanDao
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.data.local.mapper.toFestivalEntities
import com.rasoiai.data.local.mapper.toItemEntities
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.GenerateMealPlanRequest
import com.rasoiai.data.remote.dto.SwapMealRequest
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of MealPlanRepository with offline-first architecture.
 *
 * Strategy:
 * - Always return data from local Room database (single source of truth)
 * - Fetch from API when online and cache to Room
 * - Queue mutations for sync when offline
 */
@Singleton
class MealPlanRepositoryImpl @Inject constructor(
    private val apiService: RasoiApiService,
    private val mealPlanDao: MealPlanDao,
    private val networkMonitor: NetworkMonitor
) : MealPlanRepository {

    private val dateFormatter = DateTimeFormatter.ISO_DATE

    override fun getMealPlanForDate(date: LocalDate): Flow<MealPlan?> {
        val dateStr = date.format(dateFormatter)
        Timber.d("getMealPlanForDate: Looking for date $dateStr")

        return mealPlanDao.getMealPlanForDate(dateStr).map { entity ->
            if (entity != null) {
                Timber.d("Found meal plan entity: ${entity.id}, range: ${entity.weekStartDate} - ${entity.weekEndDate}")
                val items = mealPlanDao.getMealPlanItemsSync(entity.id)
                Timber.d("Loaded ${items.size} items for plan ${entity.id}")
                items.take(5).forEach { item ->
                    Timber.d("  Item: ${item.date} | ${item.mealType} | ${item.recipeName}")
                }
                val festivals = mealPlanDao.getFestivalsForMealPlan(entity.id)
                entity.toDomain(items, festivals)
            } else {
                Timber.d("No meal plan found for date $dateStr, will fetch from API")
                // Try to fetch from API if online and no local data
                if (networkMonitor.isOnline.first()) {
                    fetchAndCacheMealPlan(date)
                }
                null
            }
        }
    }

    override suspend fun generateMealPlan(weekStartDate: LocalDate): Result<MealPlan> {
        return try {
            val dateStr = weekStartDate.format(dateFormatter)

            if (!networkMonitor.isOnline.first()) {
                return Result.failure(Exception("No network connection. Cannot generate meal plan offline."))
            }

            Timber.d("Generating meal plan for week starting: $dateStr")

            val response = apiService.generateMealPlan(
                GenerateMealPlanRequest(weekStartDate = dateStr)
            )

            // Cache to local database
            val entity = response.toEntity()
            val items = response.toItemEntities()
            val festivals = response.toFestivalEntities()

            Timber.d("generateMealPlan: API response has ${response.days.size} days")
            Timber.d("generateMealPlan: Created ${items.size} item entities")
            items.take(5).forEach { item ->
                Timber.d("  Generated item: ${item.date} | ${item.mealType} | ${item.recipeName}")
            }

            mealPlanDao.replaceMealPlan(entity, items, festivals)

            // Verify items were saved correctly
            val savedItems = mealPlanDao.getMealPlanItemsSync(entity.id)
            Timber.d("generateMealPlan: Verified ${savedItems.size} items saved to Room")

            Timber.i("Meal plan generated and cached: ${response.id}")

            // Return domain model
            val mealPlan = entity.toDomain(items, festivals)
            Result.success(mealPlan)
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate meal plan")
            Result.failure(e)
        }
    }

    override suspend fun swapMeal(
        mealPlanId: String,
        date: LocalDate,
        mealType: MealType,
        currentRecipeId: String,
        excludeRecipeIds: List<String>
    ): Result<MealPlan> {
        return try {
            val dateStr = date.format(dateFormatter)

            if (!networkMonitor.isOnline.first()) {
                return Result.failure(Exception("No network connection. Cannot swap meal offline."))
            }

            // Find the item ID for this meal
            val items = mealPlanDao.getMealPlanItemsSync(mealPlanId)
            val targetItem = items.find {
                it.date == dateStr &&
                it.mealType == mealType.value &&
                it.recipeId == currentRecipeId
            } ?: return Result.failure(Exception("Meal item not found"))

            Timber.d("Swapping meal: $mealPlanId, $dateStr, ${mealType.value}, $currentRecipeId")

            val response = apiService.swapMealItem(
                planId = mealPlanId,
                itemId = "${mealPlanId}-${dateStr}-${mealType.value}-${currentRecipeId}",
                request = SwapMealRequest(
                    excludeRecipeIds = excludeRecipeIds + currentRecipeId
                )
            )

            // Update local cache
            val entity = response.toEntity()
            val newItems = response.toItemEntities()
            val festivals = response.toFestivalEntities()

            mealPlanDao.replaceMealPlan(entity, newItems, festivals)

            Timber.i("Meal swapped successfully")

            val mealPlan = entity.toDomain(newItems, festivals)
            Result.success(mealPlan)
        } catch (e: Exception) {
            Timber.e(e, "Failed to swap meal")
            Result.failure(e)
        }
    }

    override suspend fun setMealLockState(
        mealPlanId: String,
        date: LocalDate,
        mealType: MealType,
        recipeId: String,
        isLocked: Boolean
    ): Result<Unit> {
        return try {
            val dateStr = date.format(dateFormatter)

            Timber.d("Setting lock state: $mealPlanId, $dateStr, ${mealType.value}, $recipeId -> $isLocked")

            // Update local database immediately
            mealPlanDao.updateMealItemLockState(
                mealPlanId = mealPlanId,
                date = dateStr,
                mealType = mealType.value,
                recipeId = recipeId,
                isLocked = isLocked
            )

            // Sync to server if online
            if (networkMonitor.isOnline.first()) {
                try {
                    apiService.lockMealItem(
                        planId = mealPlanId,
                        itemId = "${mealPlanId}-${dateStr}-${mealType.value}-${recipeId}"
                    )
                    Timber.d("Lock state synced to server")
                } catch (e: Exception) {
                    // Mark as unsynced for later
                    mealPlanDao.updateSyncStatus(mealPlanId, false)
                    Timber.w(e, "Failed to sync lock state, queued for later")
                }
            } else {
                // Mark as unsynced
                mealPlanDao.updateSyncStatus(mealPlanId, false)
                Timber.d("Offline - lock state queued for sync")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set lock state")
            Result.failure(e)
        }
    }

    override suspend fun removeRecipeFromMeal(
        mealPlanId: String,
        date: LocalDate,
        mealType: MealType,
        recipeId: String
    ): Result<Unit> {
        return try {
            val dateStr = date.format(dateFormatter)

            Timber.d("Removing recipe: $mealPlanId, $dateStr, ${mealType.value}, $recipeId")

            // Update local database immediately
            mealPlanDao.deleteMealPlanItem(
                mealPlanId = mealPlanId,
                date = dateStr,
                mealType = mealType.value,
                recipeId = recipeId
            )

            // Sync to server if online
            if (networkMonitor.isOnline.first()) {
                try {
                    val itemId = "${mealPlanId}-${dateStr}-${mealType.value}-${recipeId}"
                    apiService.removeMealItem(
                        planId = mealPlanId,
                        itemId = itemId
                    )
                    Timber.d("Recipe removal synced to server")
                } catch (e: Exception) {
                    // Mark as unsynced for later
                    mealPlanDao.updateSyncStatus(mealPlanId, false)
                    Timber.w(e, "Failed to sync recipe removal, queued for later")
                }
            } else {
                // Mark as unsynced
                mealPlanDao.updateSyncStatus(mealPlanId, false)
                Timber.d("Offline - recipe removal queued for sync")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove recipe from meal")
            Result.failure(e)
        }
    }

    override suspend fun syncMealPlans(): Result<Unit> {
        return try {
            if (!networkMonitor.isOnline.first()) {
                return Result.failure(Exception("No network connection"))
            }

            val unsyncedPlans = mealPlanDao.getUnsyncedMealPlans()
            Timber.d("Syncing ${unsyncedPlans.size} unsynced meal plans")

            unsyncedPlans.forEach { plan ->
                try {
                    // Re-fetch from server to ensure consistency
                    val response = apiService.getMealPlanById(plan.id)
                    val entity = response.toEntity()
                    val items = response.toItemEntities()
                    val festivals = response.toFestivalEntities()

                    mealPlanDao.replaceMealPlan(entity, items, festivals)
                    Timber.d("Synced meal plan: ${plan.id}")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync meal plan: ${plan.id}")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync meal plans")
            Result.failure(e)
        }
    }

    /**
     * Fetch meal plan from API and cache locally.
     */
    private suspend fun fetchAndCacheMealPlan(date: LocalDate): MealPlan? {
        return try {
            val response = apiService.getCurrentMealPlan()
            val entity = response.toEntity()
            val items = response.toItemEntities()
            val festivals = response.toFestivalEntities()

            mealPlanDao.replaceMealPlan(entity, items, festivals)

            entity.toDomain(items, festivals)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch meal plan from API")
            null
        }
    }
}
