package com.rasoiai.domain.model

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GroceryItemTest {

    @Nested
    @DisplayName("GroceryItem.displayQuantity")
    inner class DisplayQuantity {
        @Test
        fun `joins quantity and unit with space`() {
            val item = groceryItem(quantity = "500", unit = "g")
            assertEquals("500 g", item.displayQuantity)
        }

        @Test
        fun `trims when one side is blank`() {
            val empty = groceryItem(quantity = "", unit = "g")
            assertEquals("g", empty.displayQuantity)

            val noUnit = groceryItem(quantity = "5", unit = "")
            assertEquals("5", noUnit.displayQuantity)
        }

        @Test
        fun `fully blank produces empty string`() {
            val item = groceryItem(quantity = "", unit = "")
            assertEquals("", item.displayQuantity)
        }
    }

    @Nested
    @DisplayName("GroceryCategory counts")
    inner class CategoryCounts {
        @Test
        fun `empty category yields all zeros`() {
            val cat = GroceryCategory(
                category = IngredientCategory.VEGETABLES,
                items = emptyList(),
            )
            assertEquals(0, cat.itemCount)
            assertEquals(0, cat.purchasedCount)
            assertEquals(0, cat.unpurchasedCount)
        }

        @Test
        fun `counts split by isPurchased flag`() {
            val cat = GroceryCategory(
                category = IngredientCategory.SPICES,
                items = listOf(
                    groceryItem(isPurchased = true),
                    groceryItem(isPurchased = true),
                    groceryItem(isPurchased = false),
                ),
            )
            assertEquals(3, cat.itemCount)
            assertEquals(2, cat.purchasedCount)
            assertEquals(1, cat.unpurchasedCount)
            assertEquals(cat.itemCount, cat.purchasedCount + cat.unpurchasedCount)
        }
    }

    @Nested
    @DisplayName("GroceryList aggregates")
    inner class GroceryListAggregates {
        @Test
        fun `empty list yields zeros and empty categorization`() {
            val list = groceryList(items = emptyList())
            assertEquals(0, list.totalItems)
            assertEquals(0, list.purchasedItems)
            assertEquals(0, list.unpurchasedItems)
            assertTrue(list.categorizedItems.isEmpty())
        }

        @Test
        fun `totals reflect items`() {
            val list = groceryList(
                items = listOf(
                    groceryItem(isPurchased = true),
                    groceryItem(isPurchased = false),
                    groceryItem(isPurchased = false),
                ),
            )
            assertEquals(3, list.totalItems)
            assertEquals(1, list.purchasedItems)
            assertEquals(2, list.unpurchasedItems)
        }
    }

    @Nested
    @DisplayName("GroceryList.categorizedItems")
    inner class CategorizedItems {
        @Test
        fun `groups by category`() {
            val list = groceryList(
                items = listOf(
                    groceryItem(name = "Salt", category = IngredientCategory.SPICES),
                    groceryItem(name = "Tomato", category = IngredientCategory.VEGETABLES),
                    groceryItem(name = "Chili", category = IngredientCategory.SPICES),
                ),
            )
            val cats = list.categorizedItems
            val byCategory = cats.associateBy { it.category }

            assertEquals(2, cats.size)
            assertEquals(2, byCategory[IngredientCategory.SPICES]?.itemCount)
            assertEquals(1, byCategory[IngredientCategory.VEGETABLES]?.itemCount)
        }

        @Test
        fun `items within a category are sorted by name`() {
            val list = groceryList(
                items = listOf(
                    groceryItem(name = "Zucchini", category = IngredientCategory.VEGETABLES),
                    groceryItem(name = "Aloo", category = IngredientCategory.VEGETABLES),
                    groceryItem(name = "Mushroom", category = IngredientCategory.VEGETABLES),
                ),
            )
            val vegCat = list.categorizedItems.first { it.category == IngredientCategory.VEGETABLES }
            assertEquals(listOf("Aloo", "Mushroom", "Zucchini"), vegCat.items.map { it.name })
        }

        @Test
        fun `categories are ordered by enum ordinal`() {
            // VEGETABLES.ordinal == 0, SPICES.ordinal > 0 -> VEGETABLES must come first.
            val list = groceryList(
                items = listOf(
                    groceryItem(category = IngredientCategory.SPICES),
                    groceryItem(category = IngredientCategory.VEGETABLES),
                ),
            )
            val cats = list.categorizedItems
            assertEquals(IngredientCategory.VEGETABLES.ordinal, cats[0].category.ordinal)
            assertTrue(cats[0].category.ordinal < cats[1].category.ordinal)
        }
    }

    // ==================== Factories ====================

    private fun groceryItem(
        id: String = java.util.UUID.randomUUID().toString(),
        name: String = "Item",
        quantity: String = "1",
        unit: String = "pc",
        category: IngredientCategory = IngredientCategory.VEGETABLES,
        isPurchased: Boolean = false,
    ) = GroceryItem(
        id = id, name = name, quantity = quantity, unit = unit,
        category = category, isPurchased = isPurchased,
    )

    private fun groceryList(
        items: List<GroceryItem>,
        weekStart: LocalDate = LocalDate.of(2026, 5, 11),
    ) = GroceryList(
        id = "list-1",
        weekStartDate = weekStart,
        weekEndDate = weekStart.plusDays(6),
        items = items,
        mealPlanId = "plan-1",
    )
}
