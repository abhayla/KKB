package com.rasoiai.domain.model

/**
 * Represents an item in the grocery list.
 * Aggregated from meal plan recipes with quantities summed.
 */
data class GroceryItem(
    val id: String,
    val name: String,
    val quantity: String,
    val unit: String,
    val category: IngredientCategory,
    val isPurchased: Boolean = false,
    val recipeIds: List<String> = emptyList(),
    val isCustom: Boolean = false
) {
    val displayQuantity: String
        get() = "$quantity $unit".trim()
}

/**
 * Represents a category of grocery items with its items.
 */
data class GroceryCategory(
    val category: IngredientCategory,
    val items: List<GroceryItem>,
    val isExpanded: Boolean = true
) {
    val itemCount: Int get() = items.size
    val purchasedCount: Int get() = items.count { it.isPurchased }
    val unpurchasedCount: Int get() = items.count { !it.isPurchased }
}

/**
 * Represents a weekly grocery list.
 */
data class GroceryList(
    val id: String,
    val weekStartDate: java.time.LocalDate,
    val weekEndDate: java.time.LocalDate,
    val items: List<GroceryItem>,
    val mealPlanId: String
) {
    val totalItems: Int get() = items.size
    val purchasedItems: Int get() = items.count { it.isPurchased }
    val unpurchasedItems: Int get() = items.count { !it.isPurchased }

    val categorizedItems: List<GroceryCategory>
        get() = items
            .groupBy { it.category }
            .map { (category, categoryItems) ->
                GroceryCategory(
                    category = category,
                    items = categoryItems.sortedBy { it.name }
                )
            }
            .sortedBy { it.category.ordinal }
}
