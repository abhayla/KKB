package com.rasoiai.data.repository

import app.cash.turbine.test
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.dao.FavoriteDao
import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.dao.RecipeRulesDao
import com.rasoiai.data.local.entity.FavoriteEntity
import com.rasoiai.data.local.entity.RecipeEntity
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.IngredientDto
import com.rasoiai.data.remote.dto.InstructionDto
import com.rasoiai.data.remote.dto.NutritionDto
import com.rasoiai.data.remote.dto.RecipeResponse
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.MealType
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockApiService: RasoiApiService
    private lateinit var mockRecipeDao: RecipeDao
    private lateinit var mockFavoriteDao: FavoriteDao
    private lateinit var mockRecipeRulesDao: RecipeRulesDao
    private lateinit var mockNetworkMonitor: NetworkMonitor
    private lateinit var repository: RecipeRepositoryImpl

    private val testRecipeEntity = RecipeEntity(
        id = "recipe-1",
        name = "Paneer Butter Masala",
        description = "Creamy tomato-based curry with paneer",
        cuisineType = "north",
        mealTypes = listOf("LUNCH", "DINNER"),
        dietaryTags = listOf("vegetarian"),
        prepTimeMinutes = 15,
        cookTimeMinutes = 30,
        servings = 4,
        difficulty = "medium",
        imageUrl = "https://example.com/image.jpg",
        ingredients = """[{"id":"ing-1","name":"Paneer","quantity":"250","unit":"g","category":"dairy","isOptional":false}]""",
        instructions = """[{"stepNumber":1,"instruction":"Cut paneer","durationMinutes":5,"timerRequired":false,"tips":"Use fresh paneer"}]""",
        nutritionInfo = """{"calories":350,"protein":15,"carbohydrates":20,"fat":25,"fiber":3,"sugar":5,"sodium":400}""",
        calories = 350,
        isFavorite = false,
        cachedAt = System.currentTimeMillis()
    )

    private val testRecipeResponse = RecipeResponse(
        id = "recipe-1",
        name = "Paneer Butter Masala",
        description = "Creamy tomato-based curry with paneer",
        cuisineType = "north",
        mealTypes = listOf("LUNCH", "DINNER"),
        dietaryTags = listOf("vegetarian"),
        prepTimeMinutes = 15,
        cookTimeMinutes = 30,
        servings = 4,
        difficulty = "medium",
        imageUrl = "https://example.com/image.jpg",
        ingredients = listOf(
            IngredientDto(
                id = "ing-1",
                name = "Paneer",
                quantity = "250",
                unit = "g",
                category = "dairy",
                isOptional = false
            )
        ),
        instructions = listOf(
            InstructionDto(
                stepNumber = 1,
                instruction = "Cut paneer",
                durationMinutes = 5,
                timerRequired = false,
                tips = "Use fresh paneer"
            )
        ),
        nutrition = NutritionDto(
            calories = 350,
            protein = 15,
            carbohydrates = 20,
            fat = 25,
            fiber = 3,
            sugar = 5,
            sodium = 400
        )
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockApiService = mockk(relaxed = true)
        mockRecipeDao = mockk(relaxed = true)
        mockFavoriteDao = mockk(relaxed = true)
        mockRecipeRulesDao = mockk(relaxed = true)
        mockNetworkMonitor = mockk(relaxed = true)

        repository = RecipeRepositoryImpl(
            apiService = mockApiService,
            recipeDao = mockRecipeDao,
            favoriteDao = mockFavoriteDao,
            recipeRulesDao = mockRecipeRulesDao,
            networkMonitor = mockNetworkMonitor
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("getRecipeById")
    inner class GetRecipeById {

        @Test
        @DisplayName("Should return recipe from local database when available")
        fun `should return recipe from local database when available`() = runTest {
            // Given
            every { mockRecipeDao.getRecipeById("recipe-1") } returns flowOf(testRecipeEntity)

            // When & Then
            repository.getRecipeById("recipe-1").test {
                val recipe = awaitItem()

                assertNotNull(recipe)
                assertEquals("recipe-1", recipe?.id)
                assertEquals("Paneer Butter Masala", recipe?.name)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return null and fetch from API when not in local and offline")
        fun `should return null when not in local and offline`() = runTest {
            // Given
            every { mockRecipeDao.getRecipeById("recipe-1") } returns flowOf(null)
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            // When & Then
            repository.getRecipeById("recipe-1").test {
                val recipe = awaitItem()

                assertNull(recipe)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should fetch from API when not in local and online")
        fun `should fetch from API when not in local and online`() = runTest {
            // Given
            every { mockRecipeDao.getRecipeById("recipe-1") } returns flowOf(null)
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.getRecipeById("recipe-1") } returns testRecipeResponse
            coEvery { mockFavoriteDao.isFavoriteSync("recipe-1") } returns false

            // When & Then
            repository.getRecipeById("recipe-1").test {
                awaitItem() // null initially
                testDispatcher.scheduler.advanceUntilIdle()
                cancelAndIgnoreRemainingEvents()
            }

            coVerify { mockApiService.getRecipeById("recipe-1") }
            coVerify { mockRecipeDao.insertRecipe(any()) }
        }
    }

    @Nested
    @DisplayName("searchRecipes")
    inner class SearchRecipes {

        @Test
        @DisplayName("Should return cached results when offline")
        fun `should return cached results when offline`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(false)
            every { mockRecipeDao.getRecipesByCuisine("north") } returns flowOf(listOf(testRecipeEntity))

            // When
            val result = repository.searchRecipes(
                query = null,
                cuisine = CuisineType.NORTH,
                dietary = null,
                mealType = null,
                page = 1,
                limit = 10
            )

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.size)
        }

        @Test
        @DisplayName("Should search from API when online")
        fun `should search from API when online`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery {
                mockApiService.searchRecipes(
                    query = "paneer",
                    cuisine = "north",
                    dietary = null,
                    mealType = null,
                    page = 1,
                    limit = 10
                )
            } returns listOf(testRecipeResponse)
            coEvery { mockFavoriteDao.isFavoriteSync(any()) } returns false

            // When
            val result = repository.searchRecipes(
                query = "paneer",
                cuisine = CuisineType.NORTH,
                dietary = null,
                mealType = null,
                page = 1,
                limit = 10
            )

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.size)
            assertEquals("Paneer Butter Masala", result.getOrNull()?.first()?.name)
            coVerify { mockRecipeDao.insertRecipes(any()) }
        }

        @Test
        @DisplayName("Should fallback to local search on API error")
        fun `should fallback to local search on API error`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.searchRecipes(any(), any(), any(), any(), any(), any()) } throws RuntimeException("API error")
            every { mockRecipeDao.getRecipesByCuisine("north") } returns flowOf(listOf(testRecipeEntity))

            // When
            val result = repository.searchRecipes(
                query = "paneer",
                cuisine = CuisineType.NORTH,
                dietary = null,
                mealType = null,
                page = 1,
                limit = 10
            )

            // Then
            assertTrue(result.isSuccess)
        }
    }

    @Nested
    @DisplayName("scaleRecipe")
    inner class ScaleRecipe {

        @Test
        @DisplayName("Should scale locally when offline")
        fun `should scale locally when offline`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(false)
            coEvery { mockRecipeDao.getRecipeByIdSync("recipe-1") } returns testRecipeEntity

            // When
            val result = repository.scaleRecipe("recipe-1", 8)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(8, result.getOrNull()?.servings)
        }

        @Test
        @DisplayName("Should scale via API when online")
        fun `should scale via API when online`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            val scaledDto = testRecipeResponse.copy(servings = 8)
            coEvery { mockApiService.scaleRecipe("recipe-1", 8) } returns scaledDto
            coEvery { mockFavoriteDao.isFavoriteSync("recipe-1") } returns false

            // When
            val result = repository.scaleRecipe("recipe-1", 8)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(8, result.getOrNull()?.servings)
            coVerify { mockRecipeDao.insertRecipe(any()) }
        }

        @Test
        @DisplayName("Should return failure when recipe not found offline")
        fun `should return failure when recipe not found offline`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(false)
            coEvery { mockRecipeDao.getRecipeByIdSync("recipe-1") } returns null

            // When
            val result = repository.scaleRecipe("recipe-1", 8)

            // Then
            assertTrue(result.isFailure)
            assertEquals("Recipe not found", result.exceptionOrNull()?.message)
        }
    }

    @Nested
    @DisplayName("toggleFavorite")
    inner class ToggleFavorite {

        @Test
        @DisplayName("Should add to favorites when not favorited")
        fun `should add to favorites when not favorited`() = runTest {
            // Given
            coEvery { mockFavoriteDao.isFavoriteSync("recipe-1") } returns false

            // When
            val result = repository.toggleFavorite("recipe-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(true, result.getOrNull())
            coVerify { mockFavoriteDao.insertFavorite(any()) }
            coVerify { mockRecipeDao.updateFavoriteStatus("recipe-1", true) }
        }

        @Test
        @DisplayName("Should remove from favorites when already favorited")
        fun `should remove from favorites when already favorited`() = runTest {
            // Given
            coEvery { mockFavoriteDao.isFavoriteSync("recipe-1") } returns true

            // When
            val result = repository.toggleFavorite("recipe-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(false, result.getOrNull())
            coVerify { mockFavoriteDao.deleteFavorite("recipe-1") }
            coVerify { mockRecipeDao.updateFavoriteStatus("recipe-1", false) }
        }
    }

    @Nested
    @DisplayName("getFavoriteRecipes")
    inner class GetFavoriteRecipes {

        @Test
        @DisplayName("Should return favorite recipes from local database")
        fun `should return favorite recipes from local database`() = runTest {
            // Given
            val favoriteRecipe = testRecipeEntity.copy(isFavorite = true)
            every { mockRecipeDao.getFavoriteRecipes() } returns flowOf(listOf(favoriteRecipe))

            // When & Then
            repository.getFavoriteRecipes().test {
                val recipes = awaitItem()

                assertEquals(1, recipes.size)
                assertEquals("recipe-1", recipes.first().id)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return empty list when no favorites")
        fun `should return empty list when no favorites`() = runTest {
            // Given
            every { mockRecipeDao.getFavoriteRecipes() } returns flowOf(emptyList())

            // When & Then
            repository.getFavoriteRecipes().test {
                val recipes = awaitItem()

                assertTrue(recipes.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Network Timeout and Exception Handling")
    inner class NetworkTimeoutAndExceptionHandling {

        @Test
        @DisplayName("Should fallback to local results when API throws SocketTimeoutException on search")
        fun `should fallback to local results when searchRecipes gets SocketTimeoutException`() = runTest {
            // Given — online but API times out, local cache has data
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery {
                mockApiService.searchRecipes(any(), any(), any(), any(), any(), any())
            } throws java.net.SocketTimeoutException("timeout")
            every { mockRecipeDao.getRecipesByCuisine("north") } returns flowOf(listOf(testRecipeEntity))

            // When
            val result = repository.searchRecipes(
                query = "paneer",
                cuisine = CuisineType.NORTH,
                dietary = null,
                mealType = null,
                page = 1,
                limit = 10
            )

            // Then — should fallback to local cache
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.isNotEmpty() == true)
        }

        @Test
        @DisplayName("Should return cached recipe when API throws on getRecipeById")
        fun `should return cached recipe when API throws on getRecipeById`() = runTest {
            // Given — recipe exists locally, API not needed
            every { mockRecipeDao.getRecipeById("recipe-1") } returns flowOf(testRecipeEntity)

            // When & Then — Room serves data regardless of network state
            repository.getRecipeById("recipe-1").test {
                val recipe = awaitItem()

                assertNotNull(recipe)
                assertEquals("recipe-1", recipe?.id)
                assertEquals("Paneer Butter Masala", recipe?.name)
                cancelAndIgnoreRemainingEvents()
            }

            // API should not be called when local data exists
            coVerify(exactly = 0) { mockApiService.getRecipeById(any()) }
        }

        @Test
        @DisplayName("Should silently fail when prefetchRecipes encounters IOException")
        fun `should silently fail when prefetchRecipes encounters IOException`() = runTest {
            // Given — some recipes not cached, API throws on fetch
            coEvery { mockRecipeDao.getRecipesByIdsSync(any()) } returns emptyList()
            coEvery { mockApiService.getRecipeById(any()) } throws java.io.IOException("Network unreachable")
            coEvery { mockFavoriteDao.isFavoriteSync(any()) } returns false

            // When — should not throw, prefetch errors are non-fatal
            repository.prefetchRecipes(listOf("recipe-1", "recipe-2"))

            // Then — API was attempted for each missing recipe
            coVerify(atLeast = 1) { mockApiService.getRecipeById(any()) }
            // No exception thrown — test passes if we reach here
        }
    }

    @Nested
    @DisplayName("CancellationException propagation (structured concurrency)")
    inner class CancellationPropagation {

        @Test
        @DisplayName("toggleFavorite should propagate CancellationException instead of wrapping in Result.failure")
        fun `toggleFavorite should propagate CancellationException`() = runTest {
            coEvery { mockFavoriteDao.isFavoriteSync(any()) } throws CancellationException("cancelled")
            try {
                repository.toggleFavorite("recipe-1")
                fail("Expected CancellationException to propagate, got Result wrapper instead")
            } catch (e: CancellationException) {
                assertEquals("cancelled", e.message)
            }
        }
    }
}
