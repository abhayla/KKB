package com.rasoiai.data.repository

import android.content.Context
import app.cash.turbine.test
import com.rasoiai.data.local.dao.FavoriteDao
import com.rasoiai.data.local.dao.OfflineQueueDao
import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.dao.RecipeRulesDao
import com.rasoiai.data.local.entity.NutritionGoalEntity
import com.rasoiai.data.local.entity.RecipeEntity
import com.rasoiai.data.local.entity.RecipeRuleEntity
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.domain.model.FoodCategory
import com.rasoiai.domain.model.NutritionGoal
import com.rasoiai.domain.model.RecipeRule
import com.rasoiai.domain.model.RuleAction
import com.rasoiai.domain.model.RuleEnforcement
import com.rasoiai.domain.model.RuleFrequency
import com.rasoiai.domain.model.FrequencyType
import com.rasoiai.domain.model.RuleType
import com.rasoiai.data.local.entity.OfflineQueueEntity
import com.rasoiai.domain.model.OfflineActionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeRulesRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRecipeRulesDao: RecipeRulesDao
    private lateinit var mockRecipeDao: RecipeDao
    private lateinit var mockFavoriteDao: FavoriteDao
    private lateinit var mockApiService: RasoiApiService
    private lateinit var mockNetworkMonitor: NetworkMonitor
    private lateinit var mockOfflineQueueDao: OfflineQueueDao
    private lateinit var mockContext: Context
    private lateinit var repository: RecipeRulesRepositoryImpl

    private val testRuleEntity = RecipeRuleEntity(
        id = "rule-1",
        type = "ingredient",
        action = "exclude",
        targetId = "onion",
        targetName = "Onion",
        frequencyType = "never",
        frequencyCount = null,
        frequencyDays = null,
        enforcement = "required",
        mealSlots = null,
        isActive = true,
        createdAt = "2026-01-27T10:00:00",
        updatedAt = "2026-01-27T10:00:00"
    )

    private val testNutritionGoalEntity = NutritionGoalEntity(
        id = "goal-1",
        foodCategory = "green_leafy",
        weeklyTarget = 3,
        currentProgress = 1,
        enforcement = "preferred",
        isActive = true,
        createdAt = "2026-01-27T10:00:00",
        updatedAt = "2026-01-27T10:00:00"
    )

    private val testRecipeEntity = RecipeEntity(
        id = "recipe-1",
        name = "Palak Paneer",
        description = "Spinach curry with paneer",
        imageUrl = null,
        prepTimeMinutes = 15,
        cookTimeMinutes = 30,
        servings = 4,
        difficulty = "medium",
        cuisineType = "north",
        mealTypes = listOf("LUNCH", "DINNER"),
        dietaryTags = listOf("vegetarian"),
        ingredients = "[]",
        instructions = "[]",
        nutritionInfo = "{}",
        calories = 350,
        isFavorite = false,
        cachedAt = System.currentTimeMillis()
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRecipeRulesDao = mockk(relaxed = true)
        mockRecipeDao = mockk(relaxed = true)
        mockFavoriteDao = mockk(relaxed = true)
        mockApiService = mockk(relaxed = true)
        mockNetworkMonitor = mockk(relaxed = true)
        mockOfflineQueueDao = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)

        // Default to offline to avoid network calls in tests
        every { mockNetworkMonitor.isOnline } returns flowOf(false)

        repository = RecipeRulesRepositoryImpl(
            recipeRulesDao = mockRecipeRulesDao,
            recipeDao = mockRecipeDao,
            favoriteDao = mockFavoriteDao,
            apiService = mockApiService,
            networkMonitor = mockNetworkMonitor,
            offlineQueueDao = mockOfflineQueueDao,
            context = mockContext
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Recipe Rules")
    inner class RecipeRules {

        @Test
        @DisplayName("Should return all rules from DAO")
        fun `should return all rules from DAO`() = runTest {
            // Given
            every { mockRecipeRulesDao.getAllRules() } returns flowOf(listOf(testRuleEntity))

            // When & Then
            repository.getAllRules().test {
                val rules = awaitItem()

                assertEquals(1, rules.size)
                assertEquals("rule-1", rules.first().id)
                assertEquals(RuleType.INGREDIENT, rules.first().type)
                assertEquals(RuleAction.EXCLUDE, rules.first().action)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return rules by type")
        fun `should return rules by type`() = runTest {
            // Given
            every { mockRecipeRulesDao.getRulesByType("ingredient") } returns flowOf(listOf(testRuleEntity))

            // When & Then
            repository.getRulesByType(RuleType.INGREDIENT).test {
                val rules = awaitItem()

                assertEquals(1, rules.size)
                assertEquals(RuleType.INGREDIENT, rules.first().type)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return active rules only")
        fun `should return active rules only`() = runTest {
            // Given
            every { mockRecipeRulesDao.getActiveRules() } returns flowOf(listOf(testRuleEntity))

            // When & Then
            repository.getActiveRules().test {
                val rules = awaitItem()

                assertEquals(1, rules.size)
                assertTrue(rules.first().isActive)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should create rule with generated ID and timestamps")
        fun `should create rule with generated ID and timestamps`() = runTest {
            // Given - ensure findDuplicate returns null (no existing duplicate)
            coEvery { mockRecipeRulesDao.findDuplicate(any(), any(), any()) } returns null

            val newRule = RecipeRule(
                id = "",
                type = RuleType.INGREDIENT,
                action = RuleAction.EXCLUDE,
                targetId = "garlic",
                targetName = "Garlic",
                frequency = RuleFrequency.NEVER,
                enforcement = RuleEnforcement.REQUIRED,
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            // When
            val result = repository.createRule(newRule)

            // Then
            assertTrue(result.isSuccess)
            assertNotNull(result.getOrNull()?.id)
            assertTrue(result.getOrNull()?.id?.isNotEmpty() == true)
            coVerify { mockRecipeRulesDao.insertRule(any()) }
        }

        @Test
        @DisplayName("Should update rule with new timestamp")
        fun `should update rule with new timestamp`() = runTest {
            // Given
            val rule = RecipeRule(
                id = "rule-1",
                type = RuleType.INGREDIENT,
                action = RuleAction.EXCLUDE,
                targetId = "onion",
                targetName = "Onion",
                frequency = RuleFrequency.NEVER,
                enforcement = RuleEnforcement.REQUIRED,
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            // When
            val result = repository.updateRule(rule)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockRecipeRulesDao.updateRule(any()) }
        }

        @Test
        @DisplayName("Should delete rule")
        fun `should delete rule`() = runTest {
            // When
            val result = repository.deleteRule("rule-1")

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockRecipeRulesDao.deleteRule("rule-1") }
        }

        @Test
        @DisplayName("Should toggle rule active state")
        fun `should toggle rule active state`() = runTest {
            // When
            val result = repository.toggleRuleActive("rule-1", false)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockRecipeRulesDao.updateRuleActive("rule-1", false, any()) }
        }
    }

    @Nested
    @DisplayName("Nutrition Goals")
    inner class NutritionGoals {

        @Test
        @DisplayName("Should return all nutrition goals")
        fun `should return all nutrition goals`() = runTest {
            // Given
            every { mockRecipeRulesDao.getAllNutritionGoals() } returns flowOf(listOf(testNutritionGoalEntity))

            // When & Then
            repository.getAllNutritionGoals().test {
                val goals = awaitItem()

                assertEquals(1, goals.size)
                assertEquals(FoodCategory.GREEN_LEAFY, goals.first().foodCategory)
                assertEquals(3, goals.first().weeklyTarget)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return active nutrition goals")
        fun `should return active nutrition goals`() = runTest {
            // Given
            every { mockRecipeRulesDao.getActiveNutritionGoals() } returns flowOf(listOf(testNutritionGoalEntity))

            // When & Then
            repository.getActiveNutritionGoals().test {
                val goals = awaitItem()

                assertEquals(1, goals.size)
                assertTrue(goals.first().isActive)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should create nutrition goal")
        fun `should create nutrition goal`() = runTest {
            // Given
            val newGoal = NutritionGoal(
                id = "",
                foodCategory = FoodCategory.IRON_RICH,
                weeklyTarget = 2,
                currentProgress = 0,
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            // When
            val result = repository.createNutritionGoal(newGoal)

            // Then
            assertTrue(result.isSuccess)
            assertNotNull(result.getOrNull()?.id)
            coVerify { mockRecipeRulesDao.insertNutritionGoal(any()) }
        }

        @Test
        @DisplayName("Should update nutrition goal progress")
        fun `should update nutrition goal progress`() = runTest {
            // When
            val result = repository.updateNutritionGoalProgress("goal-1", 2)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockRecipeRulesDao.updateNutritionGoalProgress("goal-1", 2, any()) }
        }

        @Test
        @DisplayName("Should reset weekly progress for all goals")
        fun `should reset weekly progress for all goals`() = runTest {
            // When
            val result = repository.resetWeeklyProgress()

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockRecipeRulesDao.resetAllNutritionGoalProgress(any()) }
        }
    }

    @Nested
    @DisplayName("Search & Suggestions")
    inner class SearchAndSuggestions {

        @Test
        @DisplayName("Should search recipes by name")
        fun `should search recipes by name`() = runTest {
            // Given
            every { mockRecipeDao.getAllRecipes() } returns flowOf(listOf(testRecipeEntity))

            // When & Then
            repository.searchRecipes("Palak").test {
                val recipes = awaitItem()

                assertEquals(1, recipes.size)
                assertEquals("Palak Paneer", recipes.first().name)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return empty list for blank query")
        fun `should return empty list for blank query`() = runTest {
            // When & Then
            repository.searchRecipes("").test {
                val recipes = awaitItem()

                assertTrue(recipes.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return popular ingredients")
        fun `should return popular ingredients`() = runTest {
            // When & Then
            repository.getPopularIngredients().test {
                val ingredients = awaitItem()

                assertTrue(ingredients.isNotEmpty())
                assertTrue(ingredients.contains("Paneer"))
                assertTrue(ingredients.contains("Chai"))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should search ingredients from popular list and known ingredients DB")
        fun `should search ingredients from popular list and known ingredients DB`() = runTest {
            // Given - mock the known_ingredients DB query
            every { mockRecipeRulesDao.searchKnownIngredients("Pan") } returns flowOf(listOf("Paneer"))

            // When & Then
            repository.searchIngredients("Pan").test {
                val ingredients = awaitItem()

                assertTrue(ingredients.isNotEmpty())
                assertTrue(ingredients.any { it.contains("Pan", ignoreCase = true) })
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return available food categories excluding active ones")
        fun `should return available food categories excluding active ones`() = runTest {
            // Given
            every { mockRecipeRulesDao.getActiveNutritionGoals() } returns flowOf(listOf(testNutritionGoalEntity))

            // When & Then
            repository.getAvailableFoodCategories().test {
                val categories = awaitItem()

                assertTrue(categories.isNotEmpty())
                assertTrue(categories.none { it == FoodCategory.GREEN_LEAFY })
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Offline Rule Sync")
    inner class OfflineRuleSync {

        @Test
        @DisplayName("Should queue CREATE_RECIPE_RULE when creating rule offline")
        fun `should queue CREATE_RECIPE_RULE when offline`() = runTest {
            // Given: offline
            every { mockNetworkMonitor.isOnline } returns flowOf(false)
            coEvery { mockRecipeRulesDao.findDuplicate(any(), any(), any()) } returns null

            val rule = RecipeRule(
                id = "",
                type = RuleType.INGREDIENT,
                action = RuleAction.EXCLUDE,
                targetId = "mushroom",
                targetName = "Mushroom",
                frequency = RuleFrequency(type = FrequencyType.NEVER),
                enforcement = RuleEnforcement.REQUIRED,
                isActive = true
            )

            // When
            val result = repository.createRule(rule)

            // Then
            assertTrue(result.isSuccess)
            val actionSlot = slot<OfflineQueueEntity>()
            coVerify { mockOfflineQueueDao.insertAction(capture(actionSlot)) }
            assertEquals(OfflineActionType.CREATE_RECIPE_RULE.value, actionSlot.captured.actionType)
            assertTrue(actionSlot.captured.payload.contains("Mushroom"))
        }

        @Test
        @DisplayName("Should queue DELETE_RECIPE_RULE when deleting rule offline")
        fun `should queue DELETE_RECIPE_RULE when offline`() = runTest {
            // Given: offline
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            // When
            val result = repository.deleteRule("rule-1")

            // Then
            assertTrue(result.isSuccess)
            val actionSlot = slot<OfflineQueueEntity>()
            coVerify { mockOfflineQueueDao.insertAction(capture(actionSlot)) }
            assertEquals(OfflineActionType.DELETE_RECIPE_RULE.value, actionSlot.captured.actionType)
            assertTrue(actionSlot.captured.payload.contains("rule-1"))
        }

        @Test
        @DisplayName("Should queue CREATE on network error during sync")
        fun `should queue CREATE on network error`() = runTest {
            // Given: online but API fails
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockRecipeRulesDao.findDuplicate(any(), any(), any()) } returns null
            coEvery { mockApiService.createRecipeRule(any()) } throws java.io.IOException("Network error")

            val rule = RecipeRule(
                id = "",
                type = RuleType.INGREDIENT,
                action = RuleAction.INCLUDE,
                targetId = "chai",
                targetName = "Chai",
                frequency = RuleFrequency(type = FrequencyType.NEVER),
                enforcement = RuleEnforcement.REQUIRED,
                isActive = true
            )

            // When
            val result = repository.createRule(rule)

            // Then — still succeeds (saved locally + queued)
            assertTrue(result.isSuccess)
            coVerify { mockOfflineQueueDao.insertAction(match {
                it.actionType == OfflineActionType.CREATE_RECIPE_RULE.value
            }) }
        }
    }

    @Nested
    @DisplayName("CancellationException propagation (structured concurrency)")
    inner class CancellationPropagation {

        @Test
        @DisplayName("resetWeeklyProgress should propagate CancellationException instead of wrapping in Result.failure")
        fun `resetWeeklyProgress should propagate CancellationException`() = runTest {
            coEvery { mockRecipeRulesDao.resetAllNutritionGoalProgress(any()) } throws CancellationException("cancelled")
            try {
                repository.resetWeeklyProgress()
                fail("Expected CancellationException to propagate, got Result wrapper instead")
            } catch (e: CancellationException) {
                assertEquals("cancelled", e.message)
            }
        }
    }
}
