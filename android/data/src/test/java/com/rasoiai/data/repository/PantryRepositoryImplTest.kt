package com.rasoiai.data.repository

import app.cash.turbine.test
import com.rasoiai.data.local.dao.PantryDao
import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.entity.PantryItemEntity
import com.rasoiai.data.local.entity.RecipeEntity
import com.rasoiai.domain.model.PantryCategory
import com.rasoiai.domain.model.PantryItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PantryRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockPantryDao: PantryDao
    private lateinit var mockRecipeDao: RecipeDao
    private lateinit var repository: PantryRepositoryImpl

    private val testPantryEntity = PantryItemEntity(
        id = "pantry-1",
        name = "Tomatoes",
        category = "VEGETABLES",
        quantity = 5,
        unit = "piece",
        addedDate = "2026-01-27",
        expiryDate = "2026-02-03",
        imageUrl = null
    )

    private val testRecipeEntity = RecipeEntity(
        id = "recipe-1",
        name = "Tomato Soup",
        description = "Simple tomato soup",
        cuisineType = "north",
        mealTypes = listOf("LUNCH"),
        dietaryTags = listOf("vegetarian"),
        prepTimeMinutes = 10,
        cookTimeMinutes = 20,
        servings = 4,
        difficulty = "easy",
        imageUrl = null,
        ingredients = """[{"id":"ing-1","name":"Tomatoes","quantity":"500","unit":"g","category":"vegetables","isOptional":false}]""",
        instructions = "[]",
        nutritionInfo = "{}",
        calories = 200,
        isFavorite = false,
        cachedAt = System.currentTimeMillis()
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockPantryDao = mockk(relaxed = true)
        mockRecipeDao = mockk(relaxed = true)

        repository = PantryRepositoryImpl(
            pantryDao = mockPantryDao,
            recipeDao = mockRecipeDao,
            apiService = mockk(relaxed = true)
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("getPantryItems")
    inner class GetPantryItems {

        @Test
        @DisplayName("Should return all pantry items")
        fun `should return all pantry items`() = runTest {
            // Given
            every { mockPantryDao.getAllItems() } returns flowOf(listOf(testPantryEntity))

            // When & Then
            repository.getPantryItems().test {
                val items = awaitItem()

                assertEquals(1, items.size)
                assertEquals("Tomatoes", items.first().name)
                assertEquals(PantryCategory.VEGETABLES, items.first().category)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return empty list when no items")
        fun `should return empty list when no items`() = runTest {
            // Given
            every { mockPantryDao.getAllItems() } returns flowOf(emptyList())

            // When & Then
            repository.getPantryItems().test {
                val items = awaitItem()

                assertTrue(items.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("getExpiringSoonItems")
    inner class GetExpiringSoonItems {

        @Test
        @DisplayName("Should return items expiring soon")
        fun `should return items expiring soon`() = runTest {
            // Given
            every { mockPantryDao.getExpiringSoonItems() } returns flowOf(listOf(testPantryEntity))

            // When & Then
            repository.getExpiringSoonItems().test {
                val items = awaitItem()

                assertEquals(1, items.size)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("addItem")
    inner class AddItem {

        @Test
        @DisplayName("Should add item with generated expiry date")
        fun `should add item with generated expiry date`() = runTest {
            // When
            val result = repository.addItem(
                name = "Milk",
                category = PantryCategory.DAIRY_MILK,
                quantity = 2,
                unit = "liters"
            )

            // Then
            assertTrue(result.isSuccess)
            assertNotNull(result.getOrNull()?.id)
            assertEquals("Milk", result.getOrNull()?.name)
            assertEquals(PantryCategory.DAIRY_MILK, result.getOrNull()?.category)
            assertEquals(2, result.getOrNull()?.quantity)
            assertNotNull(result.getOrNull()?.expiryDate) // Dairy has default shelf life

            coVerify { mockPantryDao.insertItem(any()) }
        }

        @Test
        @DisplayName("Should trim item name")
        fun `should trim item name`() = runTest {
            // When
            val result = repository.addItem(
                name = "  Onions  ",
                category = PantryCategory.VEGETABLES,
                quantity = 5,
                unit = "piece"
            )

            // Then
            assertTrue(result.isSuccess)
            assertEquals("Onions", result.getOrNull()?.name)
        }
    }

    @Nested
    @DisplayName("addItemsFromScan")
    inner class AddItemsFromScan {

        @Test
        @DisplayName("Should add multiple items from scan")
        fun `should add multiple items from scan`() = runTest {
            // Given
            val scannedItems = listOf(
                "Tomatoes" to PantryCategory.VEGETABLES,
                "Milk" to PantryCategory.DAIRY_MILK,
                "Rice" to PantryCategory.GRAINS
            )

            // When
            val result = repository.addItemsFromScan(scannedItems)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(3, result.getOrNull()?.size)
            coVerify { mockPantryDao.insertItems(any()) }
        }
    }

    @Nested
    @DisplayName("updateItem")
    inner class UpdateItem {

        @Test
        @DisplayName("Should update item successfully")
        fun `should update item successfully`() = runTest {
            // Given
            val item = PantryItem(
                id = "pantry-1",
                name = "Tomatoes",
                category = PantryCategory.VEGETABLES,
                quantity = 10,
                unit = "piece",
                addedDate = LocalDate.now(),
                expiryDate = null,
                imageUrl = null
            )

            // When
            val result = repository.updateItem(item)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockPantryDao.updateItem(any()) }
        }
    }

    @Nested
    @DisplayName("removeItem")
    inner class RemoveItem {

        @Test
        @DisplayName("Should remove item successfully")
        fun `should remove item successfully`() = runTest {
            // When
            val result = repository.removeItem("pantry-1")

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockPantryDao.deleteItem("pantry-1") }
        }
    }

    @Nested
    @DisplayName("removeExpiredItems")
    inner class RemoveExpiredItems {

        @Test
        @DisplayName("Should remove expired items and return count")
        fun `should remove expired items and return count`() = runTest {
            // Given
            coEvery { mockPantryDao.deleteExpiredItems() } returns 3

            // When
            val result = repository.removeExpiredItems()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(3, result.getOrNull())
        }
    }

    @Nested
    @DisplayName("getItemCount")
    inner class GetItemCount {

        @Test
        @DisplayName("Should return item count from DAO")
        fun `should return item count from DAO`() = runTest {
            // Given
            every { mockPantryDao.getItemCount() } returns flowOf(5)

            // When & Then
            repository.getItemCount().test {
                assertEquals(5, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("getMatchingRecipeCount")
    inner class GetMatchingRecipeCount {

        @Test
        @DisplayName("Should return 0 when pantry is empty")
        fun `should return 0 when pantry is empty`() = runTest {
            // Given
            every { mockPantryDao.getAllItems() } returns flowOf(emptyList())

            // When
            val result = repository.getMatchingRecipeCount()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull())
        }

        @Test
        @DisplayName("Should count matching recipes based on pantry items")
        fun `should count matching recipes based on pantry items`() = runTest {
            // Given
            every { mockPantryDao.getAllItems() } returns flowOf(listOf(testPantryEntity))
            every { mockRecipeDao.getAllRecipes() } returns flowOf(listOf(testRecipeEntity))

            // When
            val result = repository.getMatchingRecipeCount()

            // Then
            assertTrue(result.isSuccess)
            // Tomatoes in pantry match Tomatoes in recipe (100% match)
            assertEquals(1, result.getOrNull())
        }
    }

    @Nested
    @DisplayName("CancellationException propagation (structured concurrency)")
    inner class CancellationPropagation {

        @Test
        @DisplayName("removeItem should propagate CancellationException instead of wrapping in Result.failure")
        fun `removeItem should propagate CancellationException`() = runTest {
            coEvery { mockPantryDao.deleteItem(any()) } throws CancellationException("cancelled")
            try {
                repository.removeItem("pantry-1")
                fail("Expected CancellationException to propagate, got Result wrapper instead")
            } catch (e: CancellationException) {
                assertEquals("cancelled", e.message)
            }
        }
    }
}
