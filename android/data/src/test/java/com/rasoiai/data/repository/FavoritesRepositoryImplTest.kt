package com.rasoiai.data.repository

import app.cash.turbine.test
import com.rasoiai.data.local.dao.CollectionDao
import com.rasoiai.data.local.dao.FavoriteDao
import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.entity.FavoriteCollectionEntity
import com.rasoiai.data.local.entity.FavoriteEntity
import com.rasoiai.data.local.entity.RecipeEntity
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.FavoriteCollection
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

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockFavoriteDao: FavoriteDao
    private lateinit var mockCollectionDao: CollectionDao
    private lateinit var mockRecipeDao: RecipeDao
    private lateinit var repository: FavoritesRepositoryImpl

    private val testCollectionEntity = FavoriteCollectionEntity(
        id = "collection-1",
        name = "Weekday Dinners",
        coverImageUrl = null,
        order = 0,
        isDefault = false,
        createdAt = System.currentTimeMillis()
    )

    private val testFavoriteEntity = FavoriteEntity(
        recipeId = "recipe-1",
        collectionId = "collection-1",
        addedAt = System.currentTimeMillis(),
        order = 0
    )

    private val testRecipeEntity = RecipeEntity(
        id = "recipe-1",
        name = "Paneer Butter Masala",
        description = "Creamy tomato-based curry",
        imageUrl = "https://example.com/image.jpg",
        prepTimeMinutes = 15,
        cookTimeMinutes = 30,
        servings = 4,
        difficulty = "medium",
        cuisineType = "north",
        mealTypes = listOf("LUNCH", "DINNER"),
        dietaryTags = listOf("vegetarian"),
        ingredients = "[]",
        instructions = "[]",
        nutritionInfo = """{"calories":350}""",
        calories = 350,
        isFavorite = true,
        cachedAt = System.currentTimeMillis()
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockFavoriteDao = mockk(relaxed = true)
        mockCollectionDao = mockk(relaxed = true)
        mockRecipeDao = mockk(relaxed = true)

        repository = FavoritesRepositoryImpl(
            favoriteDao = mockFavoriteDao,
            collectionDao = mockCollectionDao,
            recipeDao = mockRecipeDao
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("getCollections")
    inner class GetCollections {

        @Test
        @DisplayName("Should return all collections with All and Recently Viewed")
        fun `should return all collections with All and Recently Viewed`() = runTest {
            // Given
            every { mockCollectionDao.getAllCollections() } returns flowOf(listOf(testCollectionEntity))
            every { mockFavoriteDao.getAllFavorites() } returns flowOf(listOf(testFavoriteEntity))

            // When & Then
            repository.getCollections().test {
                val collections = awaitItem()

                // Should have All, Recently Viewed, and user collection
                assertEquals(3, collections.size)
                assertEquals(FavoriteCollection.COLLECTION_ALL, collections[0].id)
                assertEquals(FavoriteCollection.COLLECTION_RECENTLY_VIEWED, collections[1].id)
                assertEquals("collection-1", collections[2].id)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should include recipe IDs in All collection")
        fun `should include recipe IDs in All collection`() = runTest {
            // Given
            every { mockCollectionDao.getAllCollections() } returns flowOf(emptyList())
            every { mockFavoriteDao.getAllFavorites() } returns flowOf(listOf(testFavoriteEntity))

            // When & Then
            repository.getCollections().test {
                val collections = awaitItem()

                val allCollection = collections.find { it.id == FavoriteCollection.COLLECTION_ALL }
                assertNotNull(allCollection)
                assertEquals(1, allCollection?.recipeIds?.size)
                assertEquals("recipe-1", allCollection?.recipeIds?.first())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("getCollectionById")
    inner class GetCollectionById {

        @Test
        @DisplayName("Should return All collection with all favorites")
        fun `should return All collection with all favorites`() = runTest {
            // Given
            every { mockFavoriteDao.getAllFavorites() } returns flowOf(listOf(testFavoriteEntity))

            // When & Then
            repository.getCollectionById(FavoriteCollection.COLLECTION_ALL).test {
                val collection = awaitItem()

                assertNotNull(collection)
                assertEquals("All", collection?.name)
                assertEquals(1, collection?.recipeIds?.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return Recently Viewed collection")
        fun `should return Recently Viewed collection`() = runTest {
            // Given
            every { mockCollectionDao.getRecentlyViewedIds(20) } returns flowOf(listOf("recipe-1", "recipe-2"))

            // When & Then
            repository.getCollectionById(FavoriteCollection.COLLECTION_RECENTLY_VIEWED).test {
                val collection = awaitItem()

                assertNotNull(collection)
                assertEquals("Recently Viewed", collection?.name)
                assertEquals(2, collection?.recipeIds?.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return user collection with recipe IDs")
        fun `should return user collection with recipe IDs`() = runTest {
            // Given
            every { mockCollectionDao.getCollectionById("collection-1") } returns flowOf(testCollectionEntity)
            every { mockFavoriteDao.getFavoritesByCollection("collection-1") } returns flowOf(listOf(testFavoriteEntity))

            // When & Then
            repository.getCollectionById("collection-1").test {
                val collection = awaitItem()

                assertNotNull(collection)
                assertEquals("Weekday Dinners", collection?.name)
                assertEquals(1, collection?.recipeIds?.size)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("createCollection")
    inner class CreateCollection {

        @Test
        @DisplayName("Should create collection with trimmed name")
        fun `should create collection with trimmed name`() = runTest {
            // Given
            coEvery { mockCollectionDao.getCollectionCount() } returns 0

            // When
            val result = repository.createCollection("  Quick Meals  ")

            // Then
            assertTrue(result.isSuccess)
            assertEquals("Quick Meals", result.getOrNull()?.name)
            coVerify { mockCollectionDao.insertCollection(any()) }
        }
    }

    @Nested
    @DisplayName("deleteCollection")
    inner class DeleteCollection {

        @Test
        @DisplayName("Should not allow deleting All collection")
        fun `should not allow deleting All collection`() = runTest {
            // When
            val result = repository.deleteCollection(FavoriteCollection.COLLECTION_ALL)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        }

        @Test
        @DisplayName("Should not allow deleting Recently Viewed collection")
        fun `should not allow deleting Recently Viewed collection`() = runTest {
            // When
            val result = repository.deleteCollection(FavoriteCollection.COLLECTION_RECENTLY_VIEWED)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        }

        @Test
        @DisplayName("Should delete user collection and move recipes")
        fun `should delete user collection and move recipes`() = runTest {
            // Given
            coEvery { mockCollectionDao.getCollectionByIdSync("collection-1") } returns testCollectionEntity
            every { mockFavoriteDao.getFavoritesByCollection("collection-1") } returns flowOf(listOf(testFavoriteEntity))

            // When
            val result = repository.deleteCollection("collection-1")

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockFavoriteDao.moveToCollection("recipe-1", null) }
            coVerify { mockCollectionDao.deleteCollection("collection-1") }
        }
    }

    @Nested
    @DisplayName("addRecipeToCollection")
    inner class AddRecipeToCollection {

        @Test
        @DisplayName("Should not allow adding to All collection")
        fun `should not allow adding to All collection`() = runTest {
            // When
            val result = repository.addRecipeToCollection("recipe-1", FavoriteCollection.COLLECTION_ALL)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        }

        @Test
        @DisplayName("Should add new recipe as favorite and to collection")
        fun `should add new recipe as favorite and to collection`() = runTest {
            // Given
            coEvery { mockFavoriteDao.isFavoriteSync("recipe-1") } returns false

            // When
            val result = repository.addRecipeToCollection("recipe-1", "collection-1")

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockFavoriteDao.insertFavorite(any()) }
            coVerify { mockRecipeDao.updateFavoriteStatus("recipe-1", true) }
        }

        @Test
        @DisplayName("Should move existing favorite to new collection")
        fun `should move existing favorite to new collection`() = runTest {
            // Given
            coEvery { mockFavoriteDao.isFavoriteSync("recipe-1") } returns true

            // When
            val result = repository.addRecipeToCollection("recipe-1", "collection-1")

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockFavoriteDao.moveToCollection("recipe-1", "collection-1") }
        }
    }

    @Nested
    @DisplayName("removeFromFavorites")
    inner class RemoveFromFavorites {

        @Test
        @DisplayName("Should remove recipe from favorites completely")
        fun `should remove recipe from favorites completely`() = runTest {
            // When
            val result = repository.removeFromFavorites("recipe-1")

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockFavoriteDao.deleteFavorite("recipe-1") }
            coVerify { mockRecipeDao.updateFavoriteStatus("recipe-1", false) }
        }
    }

    @Nested
    @DisplayName("addToRecentlyViewed")
    inner class AddToRecentlyViewed {

        @Test
        @DisplayName("Should add recipe to recently viewed")
        fun `should add recipe to recently viewed`() = runTest {
            // Given
            coEvery { mockCollectionDao.getRecentlyViewedCount() } returns 10

            // When
            val result = repository.addToRecentlyViewed("recipe-1")

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockCollectionDao.insertRecentlyViewed(any()) }
        }

        @Test
        @DisplayName("Should prune old entries when threshold exceeded")
        fun `should prune old entries when threshold exceeded`() = runTest {
            // Given
            coEvery { mockCollectionDao.getRecentlyViewedCount() } returns 60

            // When
            val result = repository.addToRecentlyViewed("recipe-1")

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockCollectionDao.pruneRecentlyViewed(20) }
        }
    }

    @Nested
    @DisplayName("reorderRecipes")
    inner class ReorderRecipes {

        @Test
        @DisplayName("Should update order for all recipes")
        fun `should update order for all recipes`() = runTest {
            // Given
            val recipeIds = listOf("recipe-1", "recipe-2", "recipe-3")

            // When
            val result = repository.reorderRecipes("collection-1", recipeIds)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockFavoriteDao.updateOrder("recipe-1", 0) }
            coVerify { mockFavoriteDao.updateOrder("recipe-2", 1) }
            coVerify { mockFavoriteDao.updateOrder("recipe-3", 2) }
        }
    }

    @Nested
    @DisplayName("filterRecipes")
    inner class FilterRecipes {

        @Test
        @DisplayName("Should filter by cuisine type")
        fun `should filter by cuisine type`() = runTest {
            // Given
            val northRecipe = testRecipeEntity.copy(id = "recipe-north", cuisineType = "north")
            val southRecipe = testRecipeEntity.copy(id = "recipe-south", cuisineType = "south")
            every { mockRecipeDao.getFavoriteRecipes() } returns flowOf(listOf(northRecipe, southRecipe))

            // When & Then
            repository.filterRecipes(
                collectionId = FavoriteCollection.COLLECTION_ALL,
                cuisine = CuisineType.NORTH,
                maxTimeMinutes = null
            ).test {
                val recipes = awaitItem()

                assertEquals(1, recipes.size)
                assertEquals("recipe-north", recipes.first().id)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should filter by max time")
        fun `should filter by max time`() = runTest {
            // Given
            val quickRecipe = testRecipeEntity.copy(id = "recipe-quick", prepTimeMinutes = 10, cookTimeMinutes = 10)
            val slowRecipe = testRecipeEntity.copy(id = "recipe-slow", prepTimeMinutes = 30, cookTimeMinutes = 60)
            every { mockRecipeDao.getFavoriteRecipes() } returns flowOf(listOf(quickRecipe, slowRecipe))

            // When & Then
            repository.filterRecipes(
                collectionId = FavoriteCollection.COLLECTION_ALL,
                cuisine = null,
                maxTimeMinutes = 30
            ).test {
                val recipes = awaitItem()

                assertEquals(1, recipes.size)
                assertEquals("recipe-quick", recipes.first().id)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("CancellationException propagation (structured concurrency)")
    inner class CancellationPropagation {

        @Test
        @DisplayName("createCollection should propagate CancellationException instead of wrapping in Result.failure")
        fun `createCollection should propagate CancellationException`() = runTest {
            coEvery { mockCollectionDao.getCollectionCount() } throws CancellationException("cancelled")
            try {
                repository.createCollection("Test Collection")
                fail("Expected CancellationException to propagate, got Result wrapper instead")
            } catch (e: CancellationException) {
                assertEquals("cancelled", e.message)
            }
        }
    }
}
