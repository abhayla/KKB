package com.rasoiai.domain.repository

import com.rasoiai.domain.model.PantryCategory
import com.rasoiai.domain.model.PantryItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository for pantry item operations.
 */
interface PantryRepository {
    /**
     * Get all pantry items as a flow.
     */
    fun getPantryItems(): Flow<List<PantryItem>>

    /**
     * Get pantry items that are expiring soon (within 3 days).
     */
    fun getExpiringSoonItems(): Flow<List<PantryItem>>

    /**
     * Get expired pantry items.
     */
    fun getExpiredItems(): Flow<List<PantryItem>>

    /**
     * Add a new item to the pantry.
     */
    suspend fun addItem(
        name: String,
        category: PantryCategory,
        quantity: Int = 1,
        unit: String = "piece"
    ): Result<PantryItem>

    /**
     * Add multiple items from a scan result.
     */
    suspend fun addItemsFromScan(items: List<Pair<String, PantryCategory>>): Result<List<PantryItem>>

    /**
     * Update an existing pantry item.
     */
    suspend fun updateItem(item: PantryItem): Result<PantryItem>

    /**
     * Remove an item from the pantry.
     */
    suspend fun removeItem(itemId: String): Result<Unit>

    /**
     * Remove all expired items.
     */
    suspend fun removeExpiredItems(): Result<Int>

    /**
     * Get the count of items in the pantry.
     */
    fun getItemCount(): Flow<Int>

    /**
     * Get count of recipes that can be made with current pantry items.
     */
    suspend fun getMatchingRecipeCount(): Result<Int>

    /**
     * Analyze a food image and return identified items.
     * Returns list of (name, category, quantity, unit) tuples.
     */
    suspend fun analyzeImage(imageBytes: ByteArray, fileName: String): Result<List<AnalyzedItem>>
}

data class AnalyzedItem(
    val name: String,
    val category: PantryCategory,
    val quantity: Int = 1,
    val unit: String = "piece"
)
