package com.rasoiai.domain.repository

import com.rasoiai.domain.model.GroceryItem
import com.rasoiai.domain.model.GroceryList
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for grocery list operations.
 */
interface GroceryRepository {

    /**
     * Get the grocery list for a specific week.
     */
    fun getGroceryListForWeek(weekStartDate: LocalDate): Flow<GroceryList?>

    /**
     * Get the current week's grocery list.
     */
    fun getCurrentGroceryList(): Flow<GroceryList?>

    /**
     * Toggle the purchased state of an item.
     */
    suspend fun toggleItemPurchased(itemId: String): Result<GroceryItem>

    /**
     * Update the quantity of an item.
     */
    suspend fun updateItemQuantity(itemId: String, quantity: String, unit: String): Result<GroceryItem>

    /**
     * Delete an item from the grocery list.
     */
    suspend fun deleteItem(itemId: String): Result<Unit>

    /**
     * Add a custom item to the grocery list.
     */
    suspend fun addCustomItem(item: GroceryItem): Result<GroceryItem>

    /**
     * Generate grocery list from meal plan.
     */
    suspend fun generateFromMealPlan(mealPlanId: String): Result<GroceryList>

    /**
     * Clear all purchased items from the current grocery list.
     * Returns the count of items cleared.
     */
    suspend fun clearPurchasedItems(): Result<Int>
}
