package com.rasoiai.data.repository

import app.cash.turbine.test
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.dao.FavoriteDao
import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.entity.FavoriteEntity
import com.rasoiai.data.local.entity.RecipeEntity
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.IngredientDto
import com.rasoiai.data.remote.dto.InstructionDto
import com.rasoiai.data.remote.dto.NutritionDto
import com.rasoiai.data.remote.dto.RecipeDto
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.MealType
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

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockApiService: RasoiApiService
    private lateinit var mockRecipeDao: RecipeDao
    private lateinit var mockFavoriteDao: FavoriteDao
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
        videoUrl = null,
        ingredients = """[{"id":"ing-1","name":"Paneer","quantity":"250","unit":"g","category":"dairy","isOptional":false}]""",
        instructions = """[{"stepNumber":1,"instruction":"Cut paneer","durationMinutes":5,"timerRequired":false,"tips":"Use fresh paneer"}]""",
        nutrition = """{"calories":350,"proteinGrams":15,"carbohydratesGrams":20,"fatGrams":25,"fiberGrams":3}""",
        tips = "Serve hot with naan",
        isFavorite = false,
        cachedAt = System.currentTimeMillis()
    )

    private val testRecipeDto = RecipeDto(
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
        videoUrl = null,
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
            proteinGrams = 15.0,
            carbohydratesGrams = 20.0,
            fatGrams = 25.0,
            fiberGrams = 3.0
        ),
        tips = "Serve hot with naan"
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockApiService = mockk(relaxed = true)
        mockRecipeDao = mockk(relaxed = true)
        mockFavoriteDao = mockk(relaxed = true)
        mockNetworkMonitor = mockk(relaxed = true)

        repository = RecipeRepositoryImpl(
            apiService = mockApiService,
            recipeDao = mockRecipeDao,
            favoriteDao = mockFavoriteDao,
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
            coEvery { mockApiService.getRecipeById("recipe-1") } returns testRecipeDto
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
            } returns listOf(testRecipeDto)
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
            val scaledDto = testRecipeDto.copy(servings = 8)
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
}
