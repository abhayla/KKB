package com.rasoiai.data.repository

import com.rasoiai.domain.model.PantryCategory
import com.rasoiai.domain.model.PantryItem
import com.rasoiai.domain.repository.PantryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of PantryRepository for development and testing.
 */
@Singleton
class FakePantryRepository @Inject constructor() : PantryRepository {

    private val _pantryItems = MutableStateFlow(createSampleItems())

    override fun getPantryItems(): Flow<List<PantryItem>> = _pantryItems

    override fun getExpiringSoonItems(): Flow<List<PantryItem>> = _pantryItems.map { items ->
        items.filter { it.isExpiringSoon && !it.isExpired }
    }

    override fun getExpiredItems(): Flow<List<PantryItem>> = _pantryItems.map { items ->
        items.filter { it.isExpired }
    }

    override suspend fun addItem(
        name: String,
        category: PantryCategory,
        quantity: Int,
        unit: String
    ): Result<PantryItem> {
        return try {
            val today = LocalDate.now()
            val expiryDate = category.defaultShelfLifeDays?.let { today.plusDays(it.toLong()) }

            val newItem = PantryItem(
                id = UUID.randomUUID().toString(),
                name = name,
                category = category,
                quantity = quantity,
                unit = unit,
                addedDate = today,
                expiryDate = expiryDate
            )

            _pantryItems.value = _pantryItems.value + newItem
            Result.success(newItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addItemsFromScan(items: List<Pair<String, PantryCategory>>): Result<List<PantryItem>> {
        return try {
            val today = LocalDate.now()
            val newItems = items.map { (name, category) ->
                val expiryDate = category.defaultShelfLifeDays?.let { today.plusDays(it.toLong()) }
                PantryItem(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    category = category,
                    quantity = 1,
                    unit = "piece",
                    addedDate = today,
                    expiryDate = expiryDate
                )
            }
            _pantryItems.value = _pantryItems.value + newItems
            Result.success(newItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateItem(item: PantryItem): Result<PantryItem> {
        return try {
            _pantryItems.value = _pantryItems.value.map {
                if (it.id == item.id) item else it
            }
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeItem(itemId: String): Result<Unit> {
        return try {
            _pantryItems.value = _pantryItems.value.filter { it.id != itemId }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeExpiredItems(): Result<Int> {
        return try {
            val expiredCount = _pantryItems.value.count { it.isExpired }
            _pantryItems.value = _pantryItems.value.filter { !it.isExpired }
            Result.success(expiredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getItemCount(): Flow<Int> = _pantryItems.map { it.size }

    override suspend fun getMatchingRecipeCount(): Result<Int> {
        // Mock: return a count based on pantry items
        val itemCount = _pantryItems.value.size
        val matchingCount = when {
            itemCount >= 10 -> 24
            itemCount >= 5 -> 15
            itemCount >= 3 -> 8
            else -> 3
        }
        return Result.success(matchingCount)
    }

    private fun createSampleItems(): List<PantryItem> {
        val today = LocalDate.now()
        return listOf(
            PantryItem(
                id = "1",
                name = "Potato",
                category = PantryCategory.VEGETABLES,
                quantity = 5,
                unit = "pieces",
                addedDate = today.minusDays(4),
                expiryDate = today.plusDays(3)
            ),
            PantryItem(
                id = "2",
                name = "Onion",
                category = PantryCategory.VEGETABLES,
                quantity = 4,
                unit = "pieces",
                addedDate = today.minusDays(2),
                expiryDate = today.plusDays(5)
            ),
            PantryItem(
                id = "3",
                name = "Tomato",
                category = PantryCategory.VEGETABLES,
                quantity = 6,
                unit = "pieces",
                addedDate = today.minusDays(5),
                expiryDate = today.plusDays(2)
            ),
            PantryItem(
                id = "4",
                name = "Milk",
                category = PantryCategory.DAIRY_MILK,
                quantity = 1,
                unit = "liter",
                addedDate = today.minusDays(4),
                expiryDate = today.plusDays(1)
            ),
            PantryItem(
                id = "5",
                name = "Paneer",
                category = PantryCategory.DAIRY_PANEER,
                quantity = 200,
                unit = "grams",
                addedDate = today.minusDays(3),
                expiryDate = today.plusDays(4)
            ),
            PantryItem(
                id = "6",
                name = "Spinach",
                category = PantryCategory.LEAFY_VEGETABLES,
                quantity = 1,
                unit = "bunch",
                addedDate = today.minusDays(1),
                expiryDate = today.plusDays(2)
            ),
            PantryItem(
                id = "7",
                name = "Rice",
                category = PantryCategory.GRAINS,
                quantity = 2,
                unit = "kg",
                addedDate = today.minusDays(30),
                expiryDate = null
            ),
            PantryItem(
                id = "8",
                name = "Toor Dal",
                category = PantryCategory.GRAINS,
                quantity = 1,
                unit = "kg",
                addedDate = today.minusDays(20),
                expiryDate = null
            ),
            PantryItem(
                id = "9",
                name = "Ginger",
                category = PantryCategory.VEGETABLES,
                quantity = 1,
                unit = "piece",
                addedDate = today.minusDays(3),
                expiryDate = today.plusDays(4)
            ),
            PantryItem(
                id = "10",
                name = "Garlic",
                category = PantryCategory.VEGETABLES,
                quantity = 1,
                unit = "bulb",
                addedDate = today.minusDays(5),
                expiryDate = today.plusDays(9)
            ),
            PantryItem(
                id = "11",
                name = "Coriander",
                category = PantryCategory.LEAFY_VEGETABLES,
                quantity = 1,
                unit = "bunch",
                addedDate = today.minusDays(2),
                expiryDate = today.plusDays(1)
            ),
            PantryItem(
                id = "12",
                name = "Green Chili",
                category = PantryCategory.VEGETABLES,
                quantity = 10,
                unit = "pieces",
                addedDate = today.minusDays(3),
                expiryDate = today.plusDays(4)
            ),
            PantryItem(
                id = "13",
                name = "Cumin Seeds",
                category = PantryCategory.SPICES,
                quantity = 100,
                unit = "grams",
                addedDate = today.minusDays(60),
                expiryDate = null
            ),
            PantryItem(
                id = "14",
                name = "Turmeric Powder",
                category = PantryCategory.SPICES,
                quantity = 50,
                unit = "grams",
                addedDate = today.minusDays(45),
                expiryDate = null
            ),
            PantryItem(
                id = "15",
                name = "Eggs",
                category = PantryCategory.EGGS,
                quantity = 6,
                unit = "pieces",
                addedDate = today.minusDays(5),
                expiryDate = today.plusDays(9)
            ),
            PantryItem(
                id = "16",
                name = "Bread",
                category = PantryCategory.BREAD,
                quantity = 1,
                unit = "pack",
                addedDate = today.minusDays(3),
                expiryDate = today.plusDays(2)
            ),
            PantryItem(
                id = "17",
                name = "Yogurt",
                category = PantryCategory.DAIRY_MILK,
                quantity = 400,
                unit = "grams",
                addedDate = today.minusDays(2),
                expiryDate = today.plusDays(3)
            ),
            PantryItem(
                id = "18",
                name = "Capsicum",
                category = PantryCategory.VEGETABLES,
                quantity = 2,
                unit = "pieces",
                addedDate = today.minusDays(2),
                expiryDate = today.plusDays(5)
            )
        )
    }
}
