package com.rasoiai.data.repository

import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.dao.GroceryDao
import com.rasoiai.data.local.dao.MealPlanDao
import com.rasoiai.data.local.entity.GroceryItemEntity
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.data.local.mapper.toGroceryList
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.domain.model.GroceryItem
import com.rasoiai.domain.model.GroceryList
import com.rasoiai.domain.model.Ingredient
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.repository.GroceryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of GroceryRepository with offline-first architecture.
 *
 * Strategy:
 * - Always return data from local Room database (single source of truth)
 * - Generate grocery list locally from meal plan
 * - Sync with API when online for WhatsApp sharing feature
 */
@Singleton
class GroceryRepositoryImpl @Inject constructor(
    private val apiService: RasoiApiService,
    private val groceryDao: GroceryDao,
    private val mealPlanDao: MealPlanDao,
    private val networkMonitor: NetworkMonitor
) : GroceryRepository {

    private val dateFormatter = DateTimeFormatter.ISO_DATE

    override fun getGroceryListForWeek(weekStartDate: LocalDate): Flow<GroceryList?> {
        val dateStr = weekStartDate.format(dateFormatter)

        return mealPlanDao.getMealPlanForDate(dateStr).map { mealPlanEntity ->
            if (mealPlanEntity != null) {
                val items = groceryDao.getGroceryItemsForMealPlan(mealPlanEntity.id).first()
                if (items.isNotEmpty()) {
                    val startDate = LocalDate.parse(mealPlanEntity.weekStartDate, dateFormatter)
                    val endDate = LocalDate.parse(mealPlanEntity.weekEndDate, dateFormatter)
                    items.toGroceryList(mealPlanEntity.id, startDate, endDate)
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    override fun getCurrentGroceryList(): Flow<GroceryList?> {
        val today = LocalDate.now()
        return getGroceryListForWeek(today)
    }

    override suspend fun toggleItemPurchased(itemId: String): Result<GroceryItem> {
        return try {
            val items = groceryDao.getAllGroceryItems().first()
            val item = items.find { it.id == itemId }
                ?: return Result.failure(Exception("Item not found"))

            val newCheckedState = !item.isChecked

            Timber.d("Toggling item purchased: $itemId -> $newCheckedState")
            groceryDao.updateCheckState(itemId, newCheckedState)

            Result.success(item.copy(isChecked = newCheckedState).toDomain())
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle item purchased")
            Result.failure(e)
        }
    }

    override suspend fun updateItemQuantity(
        itemId: String,
        quantity: String,
        unit: String
    ): Result<GroceryItem> {
        return try {
            val items = groceryDao.getAllGroceryItems().first()
            val item = items.find { it.id == itemId }
                ?: return Result.failure(Exception("Item not found"))

            Timber.d("Updating item quantity: $itemId -> $quantity $unit")

            val updatedItem = item.copy(quantity = quantity, unit = unit)
            groceryDao.insertGroceryItem(updatedItem)

            Result.success(updatedItem.toDomain())
        } catch (e: Exception) {
            Timber.e(e, "Failed to update item quantity")
            Result.failure(e)
        }
    }

    override suspend fun deleteItem(itemId: String): Result<Unit> {
        return try {
            Timber.d("Deleting grocery item: $itemId")
            groceryDao.deleteGroceryItem(itemId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete item")
            Result.failure(e)
        }
    }

    override suspend fun addCustomItem(item: GroceryItem): Result<GroceryItem> {
        return try {
            // Get current meal plan to associate
            val today = LocalDate.now().format(dateFormatter)
            val mealPlan = mealPlanDao.getMealPlanForDate(today).first()

            val itemWithId = if (item.id.isBlank()) {
                item.copy(id = UUID.randomUUID().toString())
            } else {
                item
            }

            Timber.d("Adding custom grocery item: ${itemWithId.name}")

            val entity = itemWithId.toEntity(mealPlan?.id)
            groceryDao.insertGroceryItem(entity)

            Result.success(itemWithId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add custom item")
            Result.failure(e)
        }
    }

    override suspend fun generateFromMealPlan(mealPlanId: String): Result<GroceryList> {
        return try {
            Timber.d("Generating grocery list from meal plan: $mealPlanId")

            // First try to fetch from API if online
            if (networkMonitor.isOnline.first()) {
                try {
                    val apiResponse = apiService.getGroceryList(mealPlanId)
                    val groceryItems = parseApiGroceryResponse(apiResponse, mealPlanId)
                    groceryDao.deleteGroceryItemsForMealPlan(mealPlanId)
                    groceryDao.insertGroceryItems(groceryItems)
                    Timber.i("Generated grocery list from API: ${groceryItems.size} items")
                } catch (e: IOException) {
                    Timber.w(e, "Network error fetching grocery list from API, generating locally")
                    generateGroceryListLocally(mealPlanId)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to fetch grocery list from API, generating locally")
                    generateGroceryListLocally(mealPlanId)
                }
            } else {
                // Generate locally when offline
                generateGroceryListLocally(mealPlanId)
            }

            // Return the grocery list from local DB
            val mealPlan = mealPlanDao.getMealPlanById(mealPlanId)
                ?: return Result.failure(Exception("Meal plan not found"))

            val items = groceryDao.getGroceryItemsForMealPlan(mealPlanId).first()
            val startDate = LocalDate.parse(mealPlan.weekStartDate, dateFormatter)
            val endDate = LocalDate.parse(mealPlan.weekEndDate, dateFormatter)

            val groceryList = items.toGroceryList(mealPlanId, startDate, endDate)
            Result.success(groceryList)
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate grocery list")
            Result.failure(e)
        }
    }

    override suspend fun clearPurchasedItems(): Result<Int> {
        return try {
            val items = groceryDao.getAllGroceryItems().first()
            val purchasedCount = items.count { it.isChecked }

            Timber.d("Clearing $purchasedCount purchased items")
            groceryDao.deleteCheckedItems()

            Result.success(purchasedCount)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear purchased items")
            Result.failure(e)
        }
    }

    /**
     * Parse API grocery response into entities.
     */
    private fun parseApiGroceryResponse(
        response: List<Map<String, Any>>,
        mealPlanId: String
    ): List<GroceryItemEntity> {
        return response.mapIndexed { index, item ->
            GroceryItemEntity(
                id = (item["id"] as? String) ?: "$mealPlanId-$index",
                name = (item["name"] as? String) ?: "Unknown",
                quantity = (item["quantity"] as? String) ?: "1",
                unit = (item["unit"] as? String) ?: "",
                category = (item["category"] as? String) ?: "other",
                isChecked = false,
                mealPlanId = mealPlanId,
                recipeIds = (item["recipe_ids"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                notes = item["notes"] as? String,
                createdAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * Generate grocery list locally from meal plan recipes.
     * Aggregates ingredients across all recipes in the meal plan.
     */
    private suspend fun generateGroceryListLocally(mealPlanId: String) {
        val mealPlanItems = mealPlanDao.getMealPlanItemsSync(mealPlanId)

        // Group by ingredient name and sum quantities
        val ingredientMap = mutableMapOf<String, GroceryItemEntity>()

        mealPlanItems.forEach { mealItem ->
            // For local generation, we create a placeholder grocery item per recipe
            // A full implementation would load recipes and extract their ingredients
            val key = "${mealItem.recipeName}-${mealItem.recipeId}"
            if (!ingredientMap.containsKey(key)) {
                ingredientMap[key] = GroceryItemEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Ingredients for ${mealItem.recipeName}",
                    quantity = "1",
                    unit = "set",
                    category = "other",
                    isChecked = false,
                    mealPlanId = mealPlanId,
                    recipeIds = listOf(mealItem.recipeId),
                    notes = null,
                    createdAt = System.currentTimeMillis()
                )
            }
        }

        // Clear old grocery items and insert new ones
        groceryDao.deleteGroceryItemsForMealPlan(mealPlanId)
        groceryDao.insertGroceryItems(ingredientMap.values.toList())

        Timber.d("Generated local grocery list: ${ingredientMap.size} items")
    }

    override suspend fun addIngredientsFromRecipe(
        ingredients: List<Ingredient>,
        recipeId: String,
        recipeName: String
    ): Result<List<GroceryItem>> {
        return try {
            Timber.d("Adding ${ingredients.size} ingredients from recipe: $recipeName")

            // Get current meal plan to associate
            val today = LocalDate.now().format(dateFormatter)
            val mealPlan = mealPlanDao.getMealPlanForDate(today).first()
            val mealPlanId = mealPlan?.id

            // Get existing grocery items to check for duplicates
            val existingItems = groceryDao.getAllGroceryItems().first()
            val existingByName = existingItems.associateBy { it.name.lowercase() }

            val addedItems = mutableListOf<GroceryItem>()

            for (ingredient in ingredients) {
                val existingItem = existingByName[ingredient.name.lowercase()]

                if (existingItem != null) {
                    // Item already exists - add recipe to recipeIds if not already there
                    val updatedRecipeIds = if (recipeId !in existingItem.recipeIds) {
                        existingItem.recipeIds + recipeId
                    } else {
                        existingItem.recipeIds
                    }

                    // Try to merge quantities if units match
                    val (mergedQty, mergedUnit) = tryMergeQuantities(
                        existingItem.quantity, existingItem.unit,
                        ingredient.quantity, ingredient.unit
                    )

                    val updatedEntity = existingItem.copy(
                        quantity = mergedQty,
                        unit = mergedUnit,
                        recipeIds = updatedRecipeIds
                    )
                    groceryDao.insertGroceryItem(updatedEntity)
                    addedItems.add(updatedEntity.toDomain())

                    Timber.d("Merged ingredient: ${ingredient.name} (now: $mergedQty $mergedUnit)")
                } else {
                    // New item - add it
                    val newEntity = GroceryItemEntity(
                        id = UUID.randomUUID().toString(),
                        name = ingredient.name,
                        quantity = ingredient.quantity,
                        unit = ingredient.unit,
                        category = ingredient.category.value,
                        isChecked = false,
                        mealPlanId = mealPlanId,
                        recipeIds = listOf(recipeId),
                        notes = "From: $recipeName",
                        createdAt = System.currentTimeMillis()
                    )
                    groceryDao.insertGroceryItem(newEntity)
                    addedItems.add(newEntity.toDomain())

                    Timber.d("Added new ingredient: ${ingredient.name}")
                }
            }

            Timber.i("Added ${addedItems.size} ingredients to grocery list from recipe: $recipeName")
            Result.success(addedItems)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add ingredients from recipe")
            Result.failure(e)
        }
    }

    /**
     * Try to merge two quantities with units.
     * If units match, add quantities. Otherwise, append as note.
     */
    private fun tryMergeQuantities(
        qty1: String, unit1: String,
        qty2: String, unit2: String
    ): Pair<String, String> {
        // If units are the same (or both empty), try to add numeric parts
        if (unit1.equals(unit2, ignoreCase = true) || (unit1.isBlank() && unit2.isBlank())) {
            val num1 = qty1.toDoubleOrNull()
            val num2 = qty2.toDoubleOrNull()

            if (num1 != null && num2 != null) {
                val sum = num1 + num2
                // Format nicely - no decimal if whole number
                val sumStr = if (sum == sum.toLong().toDouble()) {
                    sum.toLong().toString()
                } else {
                    String.format("%.1f", sum)
                }
                return sumStr to (unit1.ifBlank { unit2 })
            }
        }

        // Units don't match or not numeric - just append
        val combined = "$qty1 $unit1 + $qty2 $unit2".trim()
        return combined to ""
    }
}
