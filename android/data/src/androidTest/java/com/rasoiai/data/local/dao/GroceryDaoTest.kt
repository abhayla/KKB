package com.rasoiai.data.local.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.rasoiai.data.local.entity.GroceryItemEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroceryDaoTest : BaseDaoTest() {
    private val groceryDao: GroceryDao get() = database.groceryDao()

    private val testGroceryItem = GroceryItemEntity(
        id = "grocery-1",
        name = "Tomatoes",
        quantity = "500",
        unit = "g",
        category = "vegetables",
        isChecked = false,
        mealPlanId = "plan-1",
        recipeIds = listOf("recipe-1", "recipe-2"),
        notes = "Fresh and ripe",
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun insertGroceryItem_andGetAll_returnsItems() = runTest {
        // Given
        groceryDao.insertGroceryItem(testGroceryItem)

        // When & Then
        groceryDao.getAllGroceryItems().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Tomatoes", items.first().name)
            assertEquals("vegetables", items.first().category)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertMultipleItems_sortsByCategoryAndName() = runTest {
        // Given
        val items = listOf(
            testGroceryItem.copy(id = "1", name = "Zucchini", category = "vegetables"),
            testGroceryItem.copy(id = "2", name = "Milk", category = "dairy"),
            testGroceryItem.copy(id = "3", name = "Carrots", category = "vegetables"),
            testGroceryItem.copy(id = "4", name = "Cheese", category = "dairy")
        )
        groceryDao.insertGroceryItems(items)

        // When & Then
        groceryDao.getAllGroceryItems().test {
            val result = awaitItem()
            assertEquals(4, result.size)
            // Should be sorted by category first, then name
            assertEquals("dairy", result[0].category) // Cheese
            assertEquals("Cheese", result[0].name)
            assertEquals("dairy", result[1].category) // Milk
            assertEquals("Milk", result[1].name)
            assertEquals("vegetables", result[2].category) // Carrots
            assertEquals("Carrots", result[2].name)
            assertEquals("vegetables", result[3].category) // Zucchini
            assertEquals("Zucchini", result[3].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getGroceryItemsForMealPlan_returnsMatchingItems() = runTest {
        // Given
        val items = listOf(
            testGroceryItem.copy(id = "1", name = "Item 1", mealPlanId = "plan-1"),
            testGroceryItem.copy(id = "2", name = "Item 2", mealPlanId = "plan-2"),
            testGroceryItem.copy(id = "3", name = "Item 3", mealPlanId = "plan-1")
        )
        groceryDao.insertGroceryItems(items)

        // When & Then
        groceryDao.getGroceryItemsForMealPlan("plan-1").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.mealPlanId == "plan-1" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateCheckState_updatesItem() = runTest {
        // Given
        groceryDao.insertGroceryItem(testGroceryItem)
        assertFalse(testGroceryItem.isChecked)

        // When
        groceryDao.updateCheckState(testGroceryItem.id, true)

        // Then
        groceryDao.getAllGroceryItems().test {
            val items = awaitItem()
            assertTrue(items.first().isChecked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getUncheckedGroceryItems_returnsOnlyUnchecked() = runTest {
        // Given
        val items = listOf(
            testGroceryItem.copy(id = "1", name = "Unchecked 1", isChecked = false),
            testGroceryItem.copy(id = "2", name = "Checked", isChecked = true),
            testGroceryItem.copy(id = "3", name = "Unchecked 2", isChecked = false)
        )
        groceryDao.insertGroceryItems(items)

        // When & Then
        groceryDao.getUncheckedGroceryItems().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.none { it.isChecked })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun markAllChecked_checksAllItems() = runTest {
        // Given
        val items = listOf(
            testGroceryItem.copy(id = "1", isChecked = false),
            testGroceryItem.copy(id = "2", isChecked = false),
            testGroceryItem.copy(id = "3", isChecked = true)
        )
        groceryDao.insertGroceryItems(items)

        // When
        groceryDao.markAllChecked()

        // Then
        groceryDao.getAllGroceryItems().test {
            val result = awaitItem()
            assertTrue(result.all { it.isChecked })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteGroceryItem_removesItem() = runTest {
        // Given
        groceryDao.insertGroceryItem(testGroceryItem)

        // When
        groceryDao.deleteGroceryItem(testGroceryItem.id)

        // Then
        groceryDao.getAllGroceryItems().test {
            val result = awaitItem()
            assertTrue(result.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteCheckedItems_removesOnlyChecked() = runTest {
        // Given
        val items = listOf(
            testGroceryItem.copy(id = "1", name = "Keep", isChecked = false),
            testGroceryItem.copy(id = "2", name = "Delete", isChecked = true),
            testGroceryItem.copy(id = "3", name = "Also Delete", isChecked = true)
        )
        groceryDao.insertGroceryItems(items)

        // When
        groceryDao.deleteCheckedItems()

        // Then
        groceryDao.getAllGroceryItems().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Keep", result.first().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteGroceryItemsForMealPlan_removesMatchingItems() = runTest {
        // Given
        val items = listOf(
            testGroceryItem.copy(id = "1", name = "Plan 1 Item", mealPlanId = "plan-1"),
            testGroceryItem.copy(id = "2", name = "Plan 2 Item", mealPlanId = "plan-2"),
            testGroceryItem.copy(id = "3", name = "Another Plan 1", mealPlanId = "plan-1")
        )
        groceryDao.insertGroceryItems(items)

        // When
        groceryDao.deleteGroceryItemsForMealPlan("plan-1")

        // Then
        groceryDao.getAllGroceryItems().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Plan 2 Item", result.first().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getCategories_returnsDistinctCategories() = runTest {
        // Given
        val items = listOf(
            testGroceryItem.copy(id = "1", category = "vegetables"),
            testGroceryItem.copy(id = "2", category = "dairy"),
            testGroceryItem.copy(id = "3", category = "vegetables"),
            testGroceryItem.copy(id = "4", category = "spices")
        )
        groceryDao.insertGroceryItems(items)

        // When & Then
        groceryDao.getCategories().test {
            val categories = awaitItem()
            assertEquals(3, categories.size)
            assertTrue(categories.contains("vegetables"))
            assertTrue(categories.contains("dairy"))
            assertTrue(categories.contains("spices"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertGroceryItem_withConflict_replacesExisting() = runTest {
        // Given
        groceryDao.insertGroceryItem(testGroceryItem)

        // When
        val updatedItem = testGroceryItem.copy(quantity = "1000")
        groceryDao.insertGroceryItem(updatedItem)

        // Then
        groceryDao.getAllGroceryItems().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("1000", items.first().quantity)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
