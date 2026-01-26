package com.rasoiai.data.repository

import com.rasoiai.domain.model.GroceryItem
import com.rasoiai.domain.model.GroceryList
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.repository.GroceryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of GroceryRepository for development and testing.
 * Provides sample grocery data for an Indian household.
 */
@Singleton
class FakeGroceryRepository @Inject constructor() : GroceryRepository {

    private val groceryLists = MutableStateFlow(createSampleGroceryLists())

    override fun getGroceryListForWeek(weekStartDate: LocalDate): Flow<GroceryList?> {
        return groceryLists.map { lists ->
            lists.find { it.weekStartDate == weekStartDate }
        }
    }

    override fun getCurrentGroceryList(): Flow<GroceryList?> {
        val currentWeekStart = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return getGroceryListForWeek(currentWeekStart)
    }

    override suspend fun toggleItemPurchased(itemId: String): Result<GroceryItem> {
        return try {
            var updatedItem: GroceryItem? = null
            groceryLists.value = groceryLists.value.map { list ->
                list.copy(
                    items = list.items.map { item ->
                        if (item.id == itemId) {
                            val newItem = item.copy(isPurchased = !item.isPurchased)
                            updatedItem = newItem
                            newItem
                        } else item
                    }
                )
            }
            updatedItem?.let { Result.success(it) }
                ?: Result.failure(Exception("Item not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateItemQuantity(
        itemId: String,
        quantity: String,
        unit: String
    ): Result<GroceryItem> {
        return try {
            var updatedItem: GroceryItem? = null
            groceryLists.value = groceryLists.value.map { list ->
                list.copy(
                    items = list.items.map { item ->
                        if (item.id == itemId) {
                            val newItem = item.copy(quantity = quantity, unit = unit)
                            updatedItem = newItem
                            newItem
                        } else item
                    }
                )
            }
            updatedItem?.let { Result.success(it) }
                ?: Result.failure(Exception("Item not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteItem(itemId: String): Result<Unit> {
        return try {
            groceryLists.value = groceryLists.value.map { list ->
                list.copy(items = list.items.filter { it.id != itemId })
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCustomItem(item: GroceryItem): Result<GroceryItem> {
        return try {
            val newItem = item.copy(
                id = UUID.randomUUID().toString(),
                isCustom = true
            )
            groceryLists.value = groceryLists.value.map { list ->
                // Add to current week's list
                if (list.weekStartDate == LocalDate.now()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ) {
                    list.copy(items = list.items + newItem)
                } else list
            }
            Result.success(newItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateFromMealPlan(mealPlanId: String): Result<GroceryList> {
        // In a real implementation, this would aggregate ingredients from the meal plan
        return Result.success(groceryLists.value.first())
    }

    override suspend fun clearPurchasedItems(): Result<Int> {
        return try {
            var clearedCount = 0
            val currentWeekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

            groceryLists.value = groceryLists.value.map { list ->
                if (list.weekStartDate == currentWeekStart) {
                    val purchasedItems = list.items.filter { it.isPurchased }
                    clearedCount = purchasedItems.size
                    list.copy(items = list.items.filter { !it.isPurchased })
                } else list
            }
            Result.success(clearedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createSampleGroceryLists(): List<GroceryList> {
        val weekStart = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)

        return listOf(
            GroceryList(
                id = "grocery-list-1",
                weekStartDate = weekStart,
                weekEndDate = weekEnd,
                mealPlanId = "meal-plan-1",
                items = createSampleItems()
            )
        )
    }

    private fun createSampleItems(): List<GroceryItem> {
        return listOf(
            // Vegetables
            GroceryItem(
                id = "v1",
                name = "Onion",
                quantity = "1",
                unit = "kg",
                category = IngredientCategory.VEGETABLES,
                recipeIds = listOf("dal-tadka", "palak-paneer", "rajma")
            ),
            GroceryItem(
                id = "v2",
                name = "Tomato",
                quantity = "500",
                unit = "g",
                category = IngredientCategory.VEGETABLES,
                recipeIds = listOf("dal-tadka", "palak-paneer")
            ),
            GroceryItem(
                id = "v3",
                name = "Potato",
                quantity = "1",
                unit = "kg",
                category = IngredientCategory.VEGETABLES,
                recipeIds = listOf("paratha", "dosa")
            ),
            GroceryItem(
                id = "v4",
                name = "Palak (Spinach)",
                quantity = "2",
                unit = "bunch",
                category = IngredientCategory.VEGETABLES,
                recipeIds = listOf("palak-paneer")
            ),
            GroceryItem(
                id = "v5",
                name = "Capsicum",
                quantity = "250",
                unit = "g",
                category = IngredientCategory.VEGETABLES,
                recipeIds = listOf("biryani")
            ),
            GroceryItem(
                id = "v6",
                name = "Ginger",
                quantity = "100",
                unit = "g",
                category = IngredientCategory.VEGETABLES,
                recipeIds = listOf("dal-tadka", "palak-paneer", "rajma")
            ),
            GroceryItem(
                id = "v7",
                name = "Garlic",
                quantity = "100",
                unit = "g",
                category = IngredientCategory.VEGETABLES,
                recipeIds = listOf("dal-tadka", "palak-paneer")
            ),
            GroceryItem(
                id = "v8",
                name = "Green Chili",
                quantity = "10",
                unit = "pcs",
                category = IngredientCategory.VEGETABLES,
                recipeIds = listOf("dal-tadka", "dosa", "paratha")
            ),
            GroceryItem(
                id = "v9",
                name = "Coriander Leaves",
                quantity = "1",
                unit = "bunch",
                category = IngredientCategory.VEGETABLES,
                recipeIds = listOf("dal-tadka", "biryani")
            ),
            GroceryItem(
                id = "v10",
                name = "Lemon",
                quantity = "4",
                unit = "pcs",
                category = IngredientCategory.VEGETABLES,
                recipeIds = listOf("dal-tadka")
            ),

            // Dairy
            GroceryItem(
                id = "d1",
                name = "Paneer",
                quantity = "400",
                unit = "g",
                category = IngredientCategory.DAIRY,
                recipeIds = listOf("palak-paneer", "paneer-butter-masala")
            ),
            GroceryItem(
                id = "d2",
                name = "Curd (Yogurt)",
                quantity = "500",
                unit = "g",
                category = IngredientCategory.DAIRY,
                recipeIds = listOf("biryani", "paratha")
            ),
            GroceryItem(
                id = "d3",
                name = "Milk",
                quantity = "1",
                unit = "L",
                category = IngredientCategory.DAIRY,
                recipeIds = listOf("chai", "dessert")
            ),
            GroceryItem(
                id = "d4",
                name = "Ghee",
                quantity = "200",
                unit = "g",
                category = IngredientCategory.DAIRY,
                recipeIds = listOf("dal-tadka", "paratha", "biryani")
            ),
            GroceryItem(
                id = "d5",
                name = "Butter",
                quantity = "100",
                unit = "g",
                category = IngredientCategory.DAIRY,
                recipeIds = listOf("paneer-butter-masala")
            ),

            // Pulses
            GroceryItem(
                id = "p1",
                name = "Toor Dal",
                quantity = "500",
                unit = "g",
                category = IngredientCategory.PULSES,
                recipeIds = listOf("dal-tadka", "idli")
            ),
            GroceryItem(
                id = "p2",
                name = "Rajma (Kidney Beans)",
                quantity = "250",
                unit = "g",
                category = IngredientCategory.PULSES,
                recipeIds = listOf("rajma")
            ),
            GroceryItem(
                id = "p3",
                name = "Chana (Chickpeas)",
                quantity = "250",
                unit = "g",
                category = IngredientCategory.PULSES,
                recipeIds = listOf("chole-bhature")
            ),
            GroceryItem(
                id = "p4",
                name = "Moong Dal",
                quantity = "250",
                unit = "g",
                category = IngredientCategory.PULSES,
                recipeIds = listOf("dal-fry")
            ),

            // Grains
            GroceryItem(
                id = "g1",
                name = "Basmati Rice",
                quantity = "1",
                unit = "kg",
                category = IngredientCategory.GRAINS,
                recipeIds = listOf("biryani", "rajma")
            ),
            GroceryItem(
                id = "g2",
                name = "Whole Wheat Flour (Atta)",
                quantity = "2",
                unit = "kg",
                category = IngredientCategory.GRAINS,
                recipeIds = listOf("paratha", "roti")
            ),

            // Spices
            GroceryItem(
                id = "s1",
                name = "Cumin Seeds",
                quantity = "50",
                unit = "g",
                category = IngredientCategory.SPICES,
                recipeIds = listOf("dal-tadka", "rajma")
            ),
            GroceryItem(
                id = "s2",
                name = "Turmeric Powder",
                quantity = "50",
                unit = "g",
                category = IngredientCategory.SPICES,
                recipeIds = listOf("dal-tadka", "dosa")
            ),
            GroceryItem(
                id = "s3",
                name = "Red Chili Powder",
                quantity = "50",
                unit = "g",
                category = IngredientCategory.SPICES,
                recipeIds = listOf("dal-tadka", "rajma", "chole-bhature")
            ),
            GroceryItem(
                id = "s4",
                name = "Garam Masala",
                quantity = "50",
                unit = "g",
                category = IngredientCategory.SPICES,
                recipeIds = listOf("palak-paneer", "biryani")
            ),
            GroceryItem(
                id = "s5",
                name = "Coriander Powder",
                quantity = "50",
                unit = "g",
                category = IngredientCategory.SPICES,
                recipeIds = listOf("rajma")
            ),
            GroceryItem(
                id = "s6",
                name = "Mustard Seeds",
                quantity = "50",
                unit = "g",
                category = IngredientCategory.SPICES,
                recipeIds = listOf("dosa", "idli")
            ),
            GroceryItem(
                id = "s7",
                name = "Curry Leaves",
                quantity = "1",
                unit = "bunch",
                category = IngredientCategory.SPICES,
                recipeIds = listOf("dosa", "idli")
            ),
            GroceryItem(
                id = "s8",
                name = "Kasuri Methi",
                quantity = "25",
                unit = "g",
                category = IngredientCategory.SPICES,
                recipeIds = listOf("paneer-butter-masala")
            ),

            // Oils
            GroceryItem(
                id = "o1",
                name = "Cooking Oil",
                quantity = "1",
                unit = "L",
                category = IngredientCategory.OILS,
                recipeIds = listOf("dal-tadka", "dosa", "chole-bhature")
            ),

            // Nuts
            GroceryItem(
                id = "n1",
                name = "Cashews",
                quantity = "100",
                unit = "g",
                category = IngredientCategory.NUTS,
                recipeIds = listOf("paneer-butter-masala", "biryani")
            ),

            // Other
            GroceryItem(
                id = "ot1",
                name = "Fresh Cream",
                quantity = "200",
                unit = "ml",
                category = IngredientCategory.OTHER,
                recipeIds = listOf("palak-paneer", "paneer-butter-masala")
            ),
            GroceryItem(
                id = "ot2",
                name = "Tamarind Paste",
                quantity = "50",
                unit = "g",
                category = IngredientCategory.OTHER,
                recipeIds = listOf("idli")
            ),
            GroceryItem(
                id = "ot3",
                name = "Dosa/Idli Batter",
                quantity = "1",
                unit = "kg",
                category = IngredientCategory.OTHER,
                recipeIds = listOf("dosa", "idli")
            )
        )
    }
}
