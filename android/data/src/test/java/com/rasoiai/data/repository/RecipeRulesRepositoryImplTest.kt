package com.rasoiai.data.repository

import android.content.Context
import android.database.sqlite.SQLiteException
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
import java.io.IOException
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

    @Nested
    @DisplayName("Unexpected exception propagation (issue #34)")
    inner class UnexpectedExceptionPropagation {

        private fun http500() = retrofit2.HttpException(
            retrofit2.Response.error<Any>(500, okhttp3.ResponseBody.create(null, ""))
        )

        // ---- createRule ----

        @Test
        @DisplayName("createRule wraps SQLiteException in Result.failure")
        fun `createRule wraps SQLiteException`() = runTest {
            coEvery { mockRecipeRulesDao.findDuplicate(any(), any(), any()) } returns null
            coEvery { mockRecipeRulesDao.insertRule(any()) } throws SQLiteException("disk full")

            val rule = RecipeRule(
                id = "",
                type = RuleType.INGREDIENT,
                action = RuleAction.EXCLUDE,
                targetId = "onion",
                targetName = "Onion",
                frequency = RuleFrequency.NEVER,
                enforcement = RuleEnforcement.REQUIRED,
                isActive = true
            )

            val result = repository.createRule(rule)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }

        @Test
        @DisplayName("createRule propagates IllegalStateException instead of wrapping")
        fun `createRule propagates IllegalStateException`() = runTest {
            coEvery { mockRecipeRulesDao.findDuplicate(any(), any(), any()) } throws IllegalStateException("db closed")

            val rule = RecipeRule(
                id = "",
                type = RuleType.INGREDIENT,
                action = RuleAction.EXCLUDE,
                targetId = "onion",
                targetName = "Onion",
                frequency = RuleFrequency.NEVER,
                enforcement = RuleEnforcement.REQUIRED,
                isActive = true
            )
            try {
                repository.createRule(rule)
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        // ---- updateRule ----

        @Test
        @DisplayName("updateRule wraps SQLiteException in Result.failure")
        fun `updateRule wraps SQLiteException`() = runTest {
            coEvery { mockRecipeRulesDao.updateRule(any()) } throws SQLiteException("disk full")

            val rule = RecipeRule(
                id = "rule-1",
                type = RuleType.INGREDIENT,
                action = RuleAction.EXCLUDE,
                targetId = "onion",
                targetName = "Onion",
                frequency = RuleFrequency.NEVER,
                enforcement = RuleEnforcement.REQUIRED,
                isActive = true
            )

            val result = repository.updateRule(rule)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }

        @Test
        @DisplayName("updateRule propagates IllegalStateException instead of wrapping")
        fun `updateRule propagates IllegalStateException`() = runTest {
            coEvery { mockRecipeRulesDao.updateRule(any()) } throws IllegalStateException("db closed")

            val rule = RecipeRule(
                id = "rule-1",
                type = RuleType.INGREDIENT,
                action = RuleAction.EXCLUDE,
                targetId = "onion",
                targetName = "Onion",
                frequency = RuleFrequency.NEVER,
                enforcement = RuleEnforcement.REQUIRED,
                isActive = true
            )
            try {
                repository.updateRule(rule)
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        // ---- deleteRule ----

        @Test
        @DisplayName("deleteRule wraps SQLiteException in Result.failure")
        fun `deleteRule wraps SQLiteException`() = runTest {
            coEvery { mockRecipeRulesDao.deleteRule(any()) } throws SQLiteException("disk full")

            val result = repository.deleteRule("rule-1")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }

        @Test
        @DisplayName("deleteRule propagates IllegalStateException instead of wrapping")
        fun `deleteRule propagates IllegalStateException`() = runTest {
            coEvery { mockRecipeRulesDao.deleteRule(any()) } throws IllegalStateException("db closed")
            try {
                repository.deleteRule("rule-1")
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        // ---- toggleRuleActive ----

        @Test
        @DisplayName("toggleRuleActive wraps SQLiteException in Result.failure")
        fun `toggleRuleActive wraps SQLiteException`() = runTest {
            coEvery { mockRecipeRulesDao.updateRuleActive(any(), any(), any()) } throws SQLiteException("disk full")

            val result = repository.toggleRuleActive("rule-1", false)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }

        @Test
        @DisplayName("toggleRuleActive propagates IllegalStateException instead of wrapping")
        fun `toggleRuleActive propagates IllegalStateException`() = runTest {
            coEvery { mockRecipeRulesDao.updateRuleActive(any(), any(), any()) } throws IllegalStateException("db closed")
            try {
                repository.toggleRuleActive("rule-1", false)
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        // ---- createNutritionGoal ----

        @Test
        @DisplayName("createNutritionGoal wraps SQLiteException in Result.failure")
        fun `createNutritionGoal wraps SQLiteException`() = runTest {
            coEvery { mockRecipeRulesDao.insertNutritionGoal(any()) } throws SQLiteException("disk full")

            val goal = NutritionGoal(
                id = "",
                foodCategory = FoodCategory.IRON_RICH,
                weeklyTarget = 2,
                currentProgress = 0,
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            val result = repository.createNutritionGoal(goal)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }

        @Test
        @DisplayName("createNutritionGoal propagates IllegalStateException instead of wrapping")
        fun `createNutritionGoal propagates IllegalStateException`() = runTest {
            coEvery { mockRecipeRulesDao.insertNutritionGoal(any()) } throws IllegalStateException("db closed")

            val goal = NutritionGoal(
                id = "",
                foodCategory = FoodCategory.IRON_RICH,
                weeklyTarget = 2,
                currentProgress = 0,
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            try {
                repository.createNutritionGoal(goal)
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        // ---- updateNutritionGoal ----

        @Test
        @DisplayName("updateNutritionGoal propagates IllegalStateException instead of wrapping")
        fun `updateNutritionGoal propagates IllegalStateException`() = runTest {
            coEvery { mockRecipeRulesDao.updateNutritionGoal(any()) } throws IllegalStateException("db closed")

            val goal = NutritionGoal(
                id = "goal-1",
                foodCategory = FoodCategory.IRON_RICH,
                weeklyTarget = 2,
                currentProgress = 0,
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            try {
                repository.updateNutritionGoal(goal)
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        // ---- deleteNutritionGoal ----

        @Test
        @DisplayName("deleteNutritionGoal propagates IllegalStateException instead of wrapping")
        fun `deleteNutritionGoal propagates IllegalStateException`() = runTest {
            coEvery { mockRecipeRulesDao.deleteNutritionGoal(any()) } throws IllegalStateException("db closed")
            try {
                repository.deleteNutritionGoal("goal-1")
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        // ---- toggleNutritionGoalActive ----

        @Test
        @DisplayName("toggleNutritionGoalActive propagates IllegalStateException instead of wrapping")
        fun `toggleNutritionGoalActive propagates IllegalStateException`() = runTest {
            coEvery { mockRecipeRulesDao.updateNutritionGoalActive(any(), any(), any()) } throws IllegalStateException("db closed")
            try {
                repository.toggleNutritionGoalActive("goal-1", false)
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        // ---- updateNutritionGoalProgress ----

        @Test
        @DisplayName("updateNutritionGoalProgress propagates IllegalStateException instead of wrapping")
        fun `updateNutritionGoalProgress propagates IllegalStateException`() = runTest {
            coEvery { mockRecipeRulesDao.updateNutritionGoalProgress(any(), any(), any()) } throws IllegalStateException("db closed")
            try {
                repository.updateNutritionGoalProgress("goal-1", 5)
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        // ---- resetWeeklyProgress ----

        @Test
        @DisplayName("resetWeeklyProgress wraps SQLiteException in Result.failure")
        fun `resetWeeklyProgress wraps SQLiteException`() = runTest {
            coEvery { mockRecipeRulesDao.resetAllNutritionGoalProgress(any()) } throws SQLiteException("disk full")

            val result = repository.resetWeeklyProgress()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }

        @Test
        @DisplayName("resetWeeklyProgress propagates IllegalStateException instead of wrapping")
        fun `resetWeeklyProgress propagates IllegalStateException`() = runTest {
            coEvery { mockRecipeRulesDao.resetAllNutritionGoalProgress(any()) } throws IllegalStateException("db closed")
            try {
                repository.resetWeeklyProgress()
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        // ---- syncWithBackend ----

        @Test
        @DisplayName("syncWithBackend wraps SQLiteException in Result.failure")
        fun `syncWithBackend wraps SQLiteException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockRecipeRulesDao.getPendingRules() } throws SQLiteException("disk full")

            val result = repository.syncWithBackend()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }

        @Test
        @DisplayName("syncWithBackend propagates IllegalStateException instead of wrapping")
        fun `syncWithBackend propagates IllegalStateException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockRecipeRulesDao.getPendingRules() } throws IllegalStateException("db closed")
            try {
                repository.syncWithBackend()
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        // ---- fetchFromBackend ----

        @Test
        @DisplayName("fetchFromBackend wraps SQLiteException in Result.failure")
        fun `fetchFromBackend wraps SQLiteException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.getRecipeRules() } returns com.rasoiai.data.remote.dto.RecipeRulesListResponse(
                rules = emptyList(),
                totalCount = 0
            )
            coEvery { mockApiService.getNutritionGoals() } returns com.rasoiai.data.remote.dto.NutritionGoalsListResponse(
                goals = emptyList(),
                totalCount = 0
            )
            coEvery { mockRecipeRulesDao.insertRules(any()) } throws SQLiteException("disk full")

            val result = repository.fetchFromBackend()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }

        @Test
        @DisplayName("fetchFromBackend propagates IllegalStateException instead of wrapping")
        fun `fetchFromBackend propagates IllegalStateException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.getRecipeRules() } throws IllegalStateException("mapper bug")
            try {
                repository.fetchFromBackend()
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("mapper bug", e.message)
            }
        }

        // ---- searchRecipes (Flow) ----

        @Test
        @DisplayName("searchRecipes emits empty list on HttpException (API failure)")
        fun `searchRecipes emits empty on HttpException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            // Force local cache miss: no recipes matching "Biryani"
            every { mockRecipeDao.getAllRecipes() } returns flowOf(emptyList())
            every { mockFavoriteDao.getAllFavorites() } returns flowOf(emptyList())
            coEvery { mockApiService.searchAiRecipeCatalog(any(), any(), any()) } throws http500()

            repository.searchRecipes("Biryani").test {
                val recipes = awaitItem()
                assertTrue(recipes.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("searchRecipes propagates IllegalStateException through the Flow")
        fun `searchRecipes propagates IllegalStateException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            every { mockRecipeDao.getAllRecipes() } returns flowOf(emptyList())
            every { mockFavoriteDao.getAllFavorites() } returns flowOf(emptyList())
            coEvery { mockApiService.searchAiRecipeCatalog(any(), any(), any()) } throws IllegalStateException("unexpected")

            repository.searchRecipes("Biryani").test {
                val error = awaitError()
                assertTrue(error is IllegalStateException)
                assertEquals("unexpected", error.message)
            }
        }

        // ---- getPopularRecipes (Flow) ----

        @Test
        @DisplayName("getPopularRecipes emits empty list on HttpException (API failure)")
        fun `getPopularRecipes emits empty on HttpException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            every { mockRecipeDao.getAllRecipes() } returns flowOf(emptyList())
            every { mockFavoriteDao.getAllFavorites() } returns flowOf(emptyList())
            coEvery { mockApiService.searchAiRecipeCatalog(any(), any(), any()) } throws http500()

            repository.getPopularRecipes().test {
                val recipes = awaitItem()
                assertTrue(recipes.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("getPopularRecipes propagates IllegalStateException through the Flow")
        fun `getPopularRecipes propagates IllegalStateException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            every { mockRecipeDao.getAllRecipes() } returns flowOf(emptyList())
            every { mockFavoriteDao.getAllFavorites() } returns flowOf(emptyList())
            coEvery { mockApiService.searchAiRecipeCatalog(any(), any(), any()) } throws IllegalStateException("unexpected")

            repository.getPopularRecipes().test {
                val error = awaitError()
                assertTrue(error is IllegalStateException)
                assertEquals("unexpected", error.message)
            }
        }

        // ---- persistIngredientsFromRecipes (returns Unit, swallows) ----

        @Test
        @DisplayName("persistIngredientsFromRecipes propagates IllegalStateException instead of swallowing")
        fun `persistIngredientsFromRecipes propagates IllegalStateException`() = runTest {
            coEvery { mockRecipeRulesDao.insertKnownIngredients(any()) } throws IllegalStateException("db closed")

            val recipe = com.rasoiai.domain.model.Recipe(
                id = "r1",
                name = "Test",
                description = "",
                imageUrl = null,
                prepTimeMinutes = 10,
                cookTimeMinutes = 10,
                servings = 2,
                difficulty = com.rasoiai.domain.model.Difficulty.EASY,
                cuisineType = com.rasoiai.domain.model.CuisineType.NORTH,
                mealTypes = emptyList(),
                dietaryTags = emptyList(),
                ingredients = listOf(
                    com.rasoiai.domain.model.Ingredient(
                        id = "i1",
                        name = "Paneer",
                        quantity = "200",
                        unit = "g",
                        category = com.rasoiai.domain.model.IngredientCategory.DAIRY
                    )
                ),
                instructions = emptyList(),
                nutrition = null,
                isFavorite = false
            )

            try {
                repository.persistIngredientsFromRecipes(listOf(recipe))
                fail("Expected IllegalStateException to propagate, got silent swallow instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        @Test
        @DisplayName("persistIngredientsFromRecipes wraps SQLiteException as no-op (logged, swallowed)")
        fun `persistIngredientsFromRecipes swallows SQLiteException`() = runTest {
            coEvery { mockRecipeRulesDao.insertKnownIngredients(any()) } throws SQLiteException("disk full")

            val recipe = com.rasoiai.domain.model.Recipe(
                id = "r1",
                name = "Test",
                description = "",
                imageUrl = null,
                prepTimeMinutes = 10,
                cookTimeMinutes = 10,
                servings = 2,
                difficulty = com.rasoiai.domain.model.Difficulty.EASY,
                cuisineType = com.rasoiai.domain.model.CuisineType.NORTH,
                mealTypes = emptyList(),
                dietaryTags = emptyList(),
                ingredients = listOf(
                    com.rasoiai.domain.model.Ingredient(
                        id = "i1",
                        name = "Paneer",
                        quantity = "200",
                        unit = "g",
                        category = com.rasoiai.domain.model.IngredientCategory.DAIRY
                    )
                ),
                instructions = emptyList(),
                nutrition = null,
                isFavorite = false
            )

            // Should NOT throw — method is best-effort and returns Unit.
            repository.persistIngredientsFromRecipes(listOf(recipe))
        }
    }
}
