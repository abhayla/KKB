package com.rasoiai.data.repository

import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.dao.MealPlanDao
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.data.local.mapper.toFestivalEntities
import com.rasoiai.data.local.mapper.toItemEntities
import com.rasoiai.data.di.LongTimeout
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.local.entity.MealPlanItemEntity
import com.rasoiai.data.remote.dto.GenerateMealPlanRequest
import com.rasoiai.data.remote.dto.SwapMealRequest
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealType
import java.time.DayOfWeek
import com.rasoiai.domain.repository.GroceryRepository
import com.rasoiai.domain.repository.MealPlanRepository
import com.rasoiai.domain.repository.RecipeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
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
    @LongTimeout private val longTimeoutApiService: RasoiApiService,
    private val mealPlanDao: MealPlanDao,
    private val networkMonitor: NetworkMonitor,
    private val groceryRepository: GroceryRepository,
    private val recipeRepository: RecipeRepository
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
                Timber.d("No meal plan found for date $dateStr")
                // Don't fetch from API here - let ViewModel handle generation logic
                // This prevents race condition where both Flow and generateNewMealPlan() compete
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

            val response = longTimeoutApiService.generateMealPlan(
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

            // Pre-cache recipe details for offline access
            try {
                val recipeIds = items.map { it.recipeId }.distinct().filter { it.isNotBlank() }
                if (recipeIds.isNotEmpty()) {
                    recipeRepository.prefetchRecipes(recipeIds)
                    Timber.i("Pre-cached ${recipeIds.size} recipes from meal plan")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed to pre-cache recipes, non-critical")
            }

            // Auto-generate grocery list from the new meal plan
            try {
                groceryRepository.generateFromMealPlan(entity.id)
                Timber.i("Auto-generated grocery list for meal plan: ${entity.id}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed to auto-generate grocery list, non-critical")
            }

            // Verify items were saved correctly
            val savedItems = mealPlanDao.getMealPlanItemsSync(entity.id)
            Timber.d("generateMealPlan: Verified ${savedItems.size} items saved to Room")

            Timber.i("Meal plan generated and cached: ${response.id}")

            // Return domain model
            val mealPlan = entity.toDomain(items, festivals)
            Result.success(mealPlan)
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "HTTP ${e.code()} on generate meal plan")
            Result.failure(e)
        } catch (e: IOException) {
            Timber.w(e, "Network error on generate meal plan")
            Result.failure(e)
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
        excludeRecipeIds: List<String>,
        newRecipeId: String?
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
                    excludeRecipeIds = excludeRecipeIds + currentRecipeId,
                    specificRecipeId = newRecipeId
                )
            )

            // Update local cache
            val entity = response.toEntity()
            val newItems = response.toItemEntities()
            val festivals = response.toFestivalEntities()

            mealPlanDao.replaceMealPlan(entity, newItems, festivals)

            Timber.i("Meal swapped successfully")

            // Refresh grocery list after swap
            try {
                groceryRepository.generateFromMealPlan(mealPlanId)
                Timber.d("Grocery list refreshed after swap")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed to refresh grocery list after swap (non-fatal)")
            }

            val mealPlan = entity.toDomain(newItems, festivals)
            Result.success(mealPlan)
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "HTTP ${e.code()} on swap meal")
            Result.failure(e)
        } catch (e: IOException) {
            Timber.w(e, "Network error on swap meal")
            Result.failure(e)
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    mealPlanDao.updateSyncStatus(mealPlanId, false)
                    Timber.w(e, "Network error syncing lock state, queued for later")
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
        } catch (e: CancellationException) {
            throw e
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    mealPlanDao.updateSyncStatus(mealPlanId, false)
                    Timber.w(e, "Network error syncing recipe removal, queued for later")
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove recipe from meal")
            Result.failure(e)
        }
    }

    override suspend fun addRecipeToMeal(
        mealPlanId: String,
        date: LocalDate,
        mealType: MealType,
        recipeId: String,
        recipeName: String,
        recipeImageUrl: String?,
        prepTimeMinutes: Int,
        calories: Int
    ): Result<MealPlan> {
        return try {
            val dateStr = date.format(dateFormatter)
            val dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

            Timber.d("Adding recipe to meal: $mealPlanId, $dateStr, ${mealType.value}, $recipeId")

            // Get current items to determine order
            val existingItems = mealPlanDao.getMealPlanItemsForDateAndType(
                mealPlanId = mealPlanId,
                date = dateStr,
                mealType = mealType.value
            )
            val nextOrder = existingItems.maxOfOrNull { it.order }?.plus(1) ?: 0

            // Create new item
            val newItem = MealPlanItemEntity(
                id = java.util.UUID.randomUUID().toString(),
                mealPlanId = mealPlanId,
                date = dateStr,
                dayName = dayName,
                mealType = mealType.value,
                recipeId = recipeId,
                recipeName = recipeName,
                recipeImageUrl = recipeImageUrl,
                prepTimeMinutes = prepTimeMinutes,
                calories = calories,
                dietaryTags = emptyList(), // Will be loaded from recipe when viewed
                isLocked = false,
                order = nextOrder
            )

            // Insert into local database
            mealPlanDao.insertMealPlanItem(newItem)

            // Get updated meal plan
            val mealPlanEntity = mealPlanDao.getMealPlanById(mealPlanId)
                ?: return Result.failure(Exception("Meal plan not found"))
            val allItems = mealPlanDao.getMealPlanItemsSync(mealPlanId)
            val festivals = mealPlanDao.getFestivalsForMealPlan(mealPlanId)

            Timber.i("Recipe added to meal: $recipeName")

            val mealPlan = mealPlanEntity.toDomain(allItems, festivals)
            Result.success(mealPlan)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to add recipe to meal")
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    Timber.w(e, "Network error syncing meal plan: ${plan.id}")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync meal plan: ${plan.id}")
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "HTTP ${e.code()} on sync meal plans")
            Result.failure(e)
        } catch (e: IOException) {
            Timber.w(e, "Network error on sync meal plans")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync meal plans")
            Result.failure(e)
        }
    }

    override suspend fun hasMealPlanForCurrentWeek(): Boolean {
        val today = LocalDate.now()
        val dateStr = today.format(dateFormatter)
        return mealPlanDao.hasMealPlanForDate(dateStr)
    }

    override suspend fun fetchCurrentMealPlan(): MealPlan? {
        if (!networkMonitor.isOnline.first()) return null
        return fetchAndCacheMealPlan(LocalDate.now())
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

            // Pre-cache recipe details for offline access
            try {
                val recipeIds = items.map { it.recipeId }.distinct().filter { it.isNotBlank() }
                if (recipeIds.isNotEmpty()) {
                    recipeRepository.prefetchRecipes(recipeIds)
                    Timber.i("Pre-cached ${recipeIds.size} recipes from fetched meal plan")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed to pre-cache recipes on fetch, non-critical")
            }

            // Auto-generate grocery list from fetched meal plan
            try {
                groceryRepository.generateFromMealPlan(entity.id)
                Timber.i("Auto-generated grocery list for fetched meal plan: ${entity.id}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed to auto-generate grocery list on fetch, non-critical")
            }

            entity.toDomain(items, festivals)
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "HTTP ${e.code()} fetching meal plan from API")
            null
        } catch (e: IOException) {
            Timber.w(e, "Network error fetching meal plan from API")
            null
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch meal plan from API")
            null
        }
    }

    override suspend fun setDayLockState(mealPlanId: String, date: LocalDate, isLocked: Boolean): Result<Unit> {
        return try {
            val dateStr = date.format(dateFormatter)
            mealPlanDao.updateDayLockState(mealPlanId, dateStr, isLocked)
            Timber.d("Day lock persisted: $dateStr = $isLocked")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to persist day lock state")
            Result.failure(e)
        }
    }

    override suspend fun setMealTypeLockState(mealPlanId: String, date: LocalDate, mealType: MealType, isLocked: Boolean): Result<Unit> {
        return try {
            val dateStr = date.format(dateFormatter)
            mealPlanDao.updateMealTypeLockState(mealPlanId, dateStr, mealType.value, isLocked)
            Timber.d("Meal type lock persisted: $dateStr ${mealType.value} = $isLocked")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to persist meal type lock state")
            Result.failure(e)
        }
    }
}
