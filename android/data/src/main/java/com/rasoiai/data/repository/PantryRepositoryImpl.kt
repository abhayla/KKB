package com.rasoiai.data.repository

import com.rasoiai.data.local.dao.PantryDao
import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.domain.model.PantryCategory
import com.rasoiai.domain.model.PantryItem
import com.rasoiai.domain.repository.PantryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of PantryRepository with offline-first architecture.
 *
 * Strategy:
 * - All pantry data stored locally in Room (single source of truth)
 * - Expiry dates calculated based on category default shelf life
 * - Supports batch add from barcode/image scan
 */
@Singleton
class PantryRepositoryImpl @Inject constructor(
    private val pantryDao: PantryDao,
    private val recipeDao: RecipeDao
) : PantryRepository {

    override fun getPantryItems(): Flow<List<PantryItem>> {
        return pantryDao.getAllItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getExpiringSoonItems(): Flow<List<PantryItem>> {
        return pantryDao.getExpiringSoonItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getExpiredItems(): Flow<List<PantryItem>> {
        return pantryDao.getExpiredItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addItem(
        name: String,
        category: PantryCategory,
        quantity: Int,
        unit: String
    ): Result<PantryItem> {
        return try {
            val today = LocalDate.now()
            val expiryDate = category.defaultShelfLifeDays?.let {
                today.plusDays(it.toLong())
            }

            val item = PantryItem(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                category = category,
                quantity = quantity,
                unit = unit,
                addedDate = today,
                expiryDate = expiryDate,
                imageUrl = null
            )

            pantryDao.insertItem(item.toEntity())
            Timber.i("Added pantry item: ${item.name} (${item.category.displayName})")

            Result.success(item)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add pantry item")
            Result.failure(e)
        }
    }

    override suspend fun addItemsFromScan(items: List<Pair<String, PantryCategory>>): Result<List<PantryItem>> {
        return try {
            val today = LocalDate.now()
            val pantryItems = items.map { (name, category) ->
                val expiryDate = category.defaultShelfLifeDays?.let {
                    today.plusDays(it.toLong())
                }

                PantryItem(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    category = category,
                    quantity = 1,
                    unit = "piece",
                    addedDate = today,
                    expiryDate = expiryDate,
                    imageUrl = null
                )
            }

            pantryDao.insertItems(pantryItems.map { it.toEntity() })
            Timber.i("Added ${pantryItems.size} items from scan")

            Result.success(pantryItems)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add items from scan")
            Result.failure(e)
        }
    }

    override suspend fun updateItem(item: PantryItem): Result<PantryItem> {
        return try {
            pantryDao.updateItem(item.toEntity())
            Timber.d("Updated pantry item: ${item.name}")
            Result.success(item)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update pantry item")
            Result.failure(e)
        }
    }

    override suspend fun removeItem(itemId: String): Result<Unit> {
        return try {
            pantryDao.deleteItem(itemId)
            Timber.d("Removed pantry item: $itemId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove pantry item")
            Result.failure(e)
        }
    }

    override suspend fun removeExpiredItems(): Result<Int> {
        return try {
            val count = pantryDao.deleteExpiredItems()
            Timber.i("Removed $count expired pantry items")
            Result.success(count)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove expired items")
            Result.failure(e)
        }
    }

    override fun getItemCount(): Flow<Int> {
        return pantryDao.getItemCount()
    }

    override suspend fun getMatchingRecipeCount(): Result<Int> {
        return try {
            // Get current pantry ingredient names
            val pantryItems = pantryDao.getAllItems().first()
            val pantryIngredients = pantryItems.map { it.name.lowercase() }.toSet()

            if (pantryIngredients.isEmpty()) {
                return Result.success(0)
            }

            // Get all cached recipes and count matches
            // A recipe "matches" if at least 50% of its ingredients are in pantry
            val recipes = recipeDao.getAllRecipes().first()
            var matchingCount = 0

            for (recipe in recipes) {
                val recipeDomain = recipe.toDomain()
                val recipeIngredients = recipeDomain.ingredients
                    .map { it.name.lowercase() }
                    .toSet()

                val matchingIngredients = recipeIngredients.intersect(pantryIngredients)
                val matchPercentage = if (recipeIngredients.isNotEmpty()) {
                    matchingIngredients.size.toFloat() / recipeIngredients.size
                } else {
                    0f
                }

                if (matchPercentage >= 0.5f) {
                    matchingCount++
                }
            }

            Timber.d("Found $matchingCount recipes matching pantry items")
            Result.success(matchingCount)
        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate matching recipe count")
            // Return estimated count based on pantry size
            try {
                val itemCount = pantryDao.getItemCountSync()
                Result.success(itemCount * 2) // Rough estimate
            } catch (e2: Exception) {
                Result.failure(e)
            }
        }
    }
}
