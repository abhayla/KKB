package com.rasoiai.data.repository

import app.cash.turbine.test
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.dao.GroceryDao
import com.rasoiai.data.local.dao.MealPlanDao
import com.rasoiai.data.local.entity.GroceryItemEntity
import com.rasoiai.data.local.entity.MealPlanEntity
import com.rasoiai.data.local.entity.MealPlanItemEntity
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.domain.model.GroceryItem
import com.rasoiai.domain.model.IngredientCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class GroceryRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockApiService: RasoiApiService
    private lateinit var mockGroceryDao: GroceryDao
    private lateinit var mockMealPlanDao: MealPlanDao
    private lateinit var mockNetworkMonitor: NetworkMonitor
    private lateinit var repository: GroceryRepositoryImpl

    private val testDate = LocalDate.of(2026, 1, 27)
    private val testDateString = "2026-01-27"

    private val testMealPlanEntity = MealPlanEntity(
        id = "plan-1",
        weekStartDate = "2026-01-27",
        weekEndDate = "2026-02-02",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        isSynced = true
    )

    private val testMealPlanItems = listOf(
        MealPlanItemEntity(
            id = "item-1",
            mealPlanId = "plan-1",
            date = "2026-01-27",
            dayName = "Monday",
            mealType = "BREAKFAST",
            recipeId = "recipe-1",
            recipeName = "Poha",
            recipeImageUrl = null,
            prepTimeMinutes = 20,
            calories = 300,
            dietaryTags = listOf("vegetarian"),
            isLocked = false,
            order = 0
        )
    )

    private val testGroceryItems = listOf(
        GroceryItemEntity(
            id = "grocery-1",
            name = "Onion",
            quantity = "500",
            unit = "g",
            category = "vegetables",
            isChecked = false,
            mealPlanId = "plan-1",
            recipeIds = listOf("recipe-1"),
            notes = null,
            createdAt = System.currentTimeMillis()
        ),
        GroceryItemEntity(
            id = "grocery-2",
            name = "Tomato",
            quantity = "250",
            unit = "g",
            category = "vegetables",
            isChecked = true,
            mealPlanId = "plan-1",
            recipeIds = listOf("recipe-1"),
            notes = null,
            createdAt = System.currentTimeMillis()
        )
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockApiService = mockk(relaxed = true)
        mockGroceryDao = mockk(relaxed = true)
        mockMealPlanDao = mockk(relaxed = true)
        mockNetworkMonitor = mockk(relaxed = true)

        repository = GroceryRepositoryImpl(
            apiService = mockApiService,
            groceryDao = mockGroceryDao,
            mealPlanDao = mockMealPlanDao,
            networkMonitor = mockNetworkMonitor
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("getGroceryListForWeek")
    inner class GetGroceryListForWeek {

        @Test
        @DisplayName("Should return grocery list when meal plan and items exist")
        fun `should return grocery list when meal plan and items exist`() = runTest {
            // Given
            every { mockMealPlanDao.getMealPlanForDate(testDateString) } returns flowOf(testMealPlanEntity)
            every { mockGroceryDao.getGroceryItemsForMealPlan("plan-1") } returns flowOf(testGroceryItems)

            // When & Then
            repository.getGroceryListForWeek(testDate).test {
                val groceryList = awaitItem()

                assertNotNull(groceryList)
                assertEquals("plan-1", groceryList?.mealPlanId)
                assertEquals(2, groceryList?.items?.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return null when no meal plan exists")
        fun `should return null when no meal plan exists`() = runTest {
            // Given
            every { mockMealPlanDao.getMealPlanForDate(testDateString) } returns flowOf(null)

            // When & Then
            repository.getGroceryListForWeek(testDate).test {
                val groceryList = awaitItem()

                assertNull(groceryList)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return null when meal plan exists but no grocery items")
        fun `should return null when meal plan exists but no grocery items`() = runTest {
            // Given
            every { mockMealPlanDao.getMealPlanForDate(testDateString) } returns flowOf(testMealPlanEntity)
            every { mockGroceryDao.getGroceryItemsForMealPlan("plan-1") } returns flowOf(emptyList())

            // When & Then
            repository.getGroceryListForWeek(testDate).test {
                val groceryList = awaitItem()

                assertNull(groceryList)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("toggleItemPurchased")
    inner class ToggleItemPurchased {

        @Test
        @DisplayName("Should toggle unchecked item to checked")
        fun `should toggle unchecked item to checked`() = runTest {
            // Given
            val uncheckedItem = testGroceryItems.first()
            every { mockGroceryDao.getAllGroceryItems() } returns flowOf(listOf(uncheckedItem))

            // When
            val result = repository.toggleItemPurchased("grocery-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(true, result.getOrNull()?.isPurchased)
            coVerify { mockGroceryDao.updateCheckState("grocery-1", true) }
        }

        @Test
        @DisplayName("Should toggle checked item to unchecked")
        fun `should toggle checked item to unchecked`() = runTest {
            // Given
            val checkedItem = testGroceryItems[1]
            every { mockGroceryDao.getAllGroceryItems() } returns flowOf(listOf(checkedItem))

            // When
            val result = repository.toggleItemPurchased("grocery-2")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(false, result.getOrNull()?.isPurchased)
            coVerify { mockGroceryDao.updateCheckState("grocery-2", false) }
        }

        @Test
        @DisplayName("Should return failure when item not found")
        fun `should return failure when item not found`() = runTest {
            // Given
            every { mockGroceryDao.getAllGroceryItems() } returns flowOf(emptyList())

            // When
            val result = repository.toggleItemPurchased("nonexistent")

            // Then
            assertTrue(result.isFailure)
            assertEquals("Item not found", result.exceptionOrNull()?.message)
        }
    }

    @Nested
    @DisplayName("updateItemQuantity")
    inner class UpdateItemQuantity {

        @Test
        @DisplayName("Should update item quantity and unit")
        fun `should update item quantity and unit`() = runTest {
            // Given
            every { mockGroceryDao.getAllGroceryItems() } returns flowOf(testGroceryItems)

            // When
            val result = repository.updateItemQuantity("grocery-1", "1", "kg")

            // Then
            assertTrue(result.isSuccess)
            assertEquals("1", result.getOrNull()?.quantity)
            assertEquals("kg", result.getOrNull()?.unit)
            coVerify { mockGroceryDao.insertGroceryItem(any()) }
        }

        @Test
        @DisplayName("Should return failure when item not found")
        fun `should return failure when item not found`() = runTest {
            // Given
            every { mockGroceryDao.getAllGroceryItems() } returns flowOf(emptyList())

            // When
            val result = repository.updateItemQuantity("nonexistent", "1", "kg")

            // Then
            assertTrue(result.isFailure)
        }
    }

    @Nested
    @DisplayName("deleteItem")
    inner class DeleteItem {

        @Test
        @DisplayName("Should delete item successfully")
        fun `should delete item successfully`() = runTest {
            // When
            val result = repository.deleteItem("grocery-1")

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockGroceryDao.deleteGroceryItem("grocery-1") }
        }
    }

    @Nested
    @DisplayName("addCustomItem")
    inner class AddCustomItem {

        @Test
        @DisplayName("Should add custom item with generated ID")
        fun `should add custom item with generated ID`() = runTest {
            // Given
            every { mockMealPlanDao.getMealPlanForDate(any()) } returns flowOf(testMealPlanEntity)

            val customItem = GroceryItem(
                id = "",
                name = "Custom Item",
                quantity = "1",
                unit = "piece",
                category = IngredientCategory.OTHER,
                isPurchased = false,
                recipeIds = emptyList(),
                isCustom = true
            )

            // When
            val result = repository.addCustomItem(customItem)

            // Then
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.id?.isNotBlank() == true)
            assertEquals("Custom Item", result.getOrNull()?.name)
            coVerify { mockGroceryDao.insertGroceryItem(any()) }
        }

        @Test
        @DisplayName("Should add custom item with provided ID")
        fun `should add custom item with provided ID`() = runTest {
            // Given
            every { mockMealPlanDao.getMealPlanForDate(any()) } returns flowOf(testMealPlanEntity)

            val customItem = GroceryItem(
                id = "custom-id-123",
                name = "Custom Item",
                quantity = "1",
                unit = "piece",
                category = IngredientCategory.OTHER,
                isPurchased = false,
                recipeIds = emptyList(),
                isCustom = true
            )

            // When
            val result = repository.addCustomItem(customItem)

            // Then
            assertTrue(result.isSuccess)
            assertEquals("custom-id-123", result.getOrNull()?.id)
        }
    }

    @Nested
    @DisplayName("generateFromMealPlan")
    inner class GenerateFromMealPlan {

        @Test
        @DisplayName("Should generate from API when online")
        fun `should generate from API when online`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.getGroceryList("plan-1") } returns listOf(
                mapOf(
                    "id" to "grocery-api-1",
                    "name" to "API Item",
                    "quantity" to "500",
                    "unit" to "g",
                    "category" to "vegetables"
                )
            )
            coEvery { mockMealPlanDao.getMealPlanById("plan-1") } returns testMealPlanEntity
            every { mockGroceryDao.getGroceryItemsForMealPlan("plan-1") } returns flowOf(testGroceryItems)

            // When
            val result = repository.generateFromMealPlan("plan-1")

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockGroceryDao.deleteGroceryItemsForMealPlan("plan-1") }
            coVerify { mockGroceryDao.insertGroceryItems(any()) }
        }

        @Test
        @DisplayName("Should generate locally when offline")
        fun `should generate locally when offline`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(false)
            coEvery { mockMealPlanDao.getMealPlanItemsSync("plan-1") } returns testMealPlanItems
            coEvery { mockMealPlanDao.getMealPlanById("plan-1") } returns testMealPlanEntity
            every { mockGroceryDao.getGroceryItemsForMealPlan("plan-1") } returns flowOf(testGroceryItems)

            // When
            val result = repository.generateFromMealPlan("plan-1")

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockGroceryDao.deleteGroceryItemsForMealPlan("plan-1") }
            coVerify { mockGroceryDao.insertGroceryItems(any()) }
        }

        @Test
        @DisplayName("Should fallback to local generation on API error")
        fun `should fallback to local generation on API error`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.getGroceryList("plan-1") } throws RuntimeException("API error")
            coEvery { mockMealPlanDao.getMealPlanItemsSync("plan-1") } returns testMealPlanItems
            coEvery { mockMealPlanDao.getMealPlanById("plan-1") } returns testMealPlanEntity
            every { mockGroceryDao.getGroceryItemsForMealPlan("plan-1") } returns flowOf(testGroceryItems)

            // When
            val result = repository.generateFromMealPlan("plan-1")

            // Then
            assertTrue(result.isSuccess)
        }

        @Test
        @DisplayName("Should return failure when meal plan not found")
        fun `should return failure when meal plan not found`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(false)
            coEvery { mockMealPlanDao.getMealPlanItemsSync("plan-1") } returns emptyList()
            coEvery { mockMealPlanDao.getMealPlanById("plan-1") } returns null

            // When
            val result = repository.generateFromMealPlan("plan-1")

            // Then
            assertTrue(result.isFailure)
            assertEquals("Meal plan not found", result.exceptionOrNull()?.message)
        }
    }

    @Nested
    @DisplayName("clearPurchasedItems")
    inner class ClearPurchasedItems {

        @Test
        @DisplayName("Should clear all purchased items and return count")
        fun `should clear all purchased items and return count`() = runTest {
            // Given
            every { mockGroceryDao.getAllGroceryItems() } returns flowOf(testGroceryItems)

            // When
            val result = repository.clearPurchasedItems()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()) // One item is checked in testGroceryItems
            coVerify { mockGroceryDao.deleteCheckedItems() }
        }

        @Test
        @DisplayName("Should return 0 when no items purchased")
        fun `should return 0 when no items purchased`() = runTest {
            // Given
            val uncheckedItems = listOf(testGroceryItems.first())
            every { mockGroceryDao.getAllGroceryItems() } returns flowOf(uncheckedItems)

            // When
            val result = repository.clearPurchasedItems()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull())
        }
    }
}
