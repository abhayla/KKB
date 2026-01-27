package com.rasoiai.data.repository

import app.cash.turbine.test
import com.rasoiai.data.local.dao.RecipeDao
import com.rasoiai.data.local.dao.RecipeRulesDao
import com.rasoiai.data.local.entity.NutritionGoalEntity
import com.rasoiai.data.local.entity.RecipeEntity
import com.rasoiai.data.local.entity.RecipeRuleEntity
import com.rasoiai.domain.model.FoodCategory
import com.rasoiai.domain.model.FrequencyType
import com.rasoiai.domain.model.NutritionGoal
import com.rasoiai.domain.model.RecipeRule
import com.rasoiai.domain.model.RuleAction
import com.rasoiai.domain.model.RuleEnforcement
import com.rasoiai.domain.model.RuleType
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
import org.junit.jupiter.api.Assertions.assertTrue
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
    private lateinit var repository: RecipeRulesRepositoryImpl

    private val testRuleEntity = RecipeRuleEntity(
        id = "rule-1",
        type = "ingredient",
        action = "exclude",
        targetId = "onion",
        targetName = "Onion",
        frequency = "never",
        enforcement = "required",
        mealSlot = null,
        isActive = true,
        createdAt = "2026-01-27T10:00:00",
        updatedAt = "2026-01-27T10:00:00"
    )

    private val testNutritionGoalEntity = NutritionGoalEntity(
        id = "goal-1",
        foodCategory = "green_leafy",
        weeklyTarget = 3,
        currentProgress = 1,
        isActive = true,
        createdAt = "2026-01-27T10:00:00",
        updatedAt = "2026-01-27T10:00:00"
    )

    private val testRecipeEntity = RecipeEntity(
        id = "recipe-1",
        name = "Palak Paneer",
        description = "Spinach curry with paneer",
        cuisineType = "north",
        mealTypes = listOf("LUNCH", "DINNER"),
        dietaryTags = listOf("vegetarian"),
        prepTimeMinutes = 15,
        cookTimeMinutes = 30,
        servings = 4,
        difficulty = "medium",
        imageUrl = null,
        videoUrl = null,
        ingredients = "[]",
        instructions = "[]",
        nutrition = "{}",
        tips = "",
        isFavorite = false,
        cachedAt = System.currentTimeMillis()
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRecipeRulesDao = mockk(relaxed = true)
        mockRecipeDao = mockk(relaxed = true)

        repository = RecipeRulesRepositoryImpl(
            recipeRulesDao = mockRecipeRulesDao,
            recipeDao = mockRecipeDao
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
            // Given
            val newRule = RecipeRule(
                id = "",
                type = RuleType.INGREDIENT,
                action = RuleAction.EXCLUDE,
                targetId = "garlic",
                targetName = "Garlic",
                frequency = FrequencyType.NEVER,
                enforcement = RuleEnforcement.REQUIRED,
                mealSlot = null,
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
                frequency = FrequencyType.NEVER,
                enforcement = RuleEnforcement.REQUIRED,
                mealSlot = null,
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
                assertTrue(ingredients.contains("Onion"))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should search ingredients from popular list and recipes")
        fun `should search ingredients from popular list and recipes`() = runTest {
            // Given
            every { mockRecipeDao.getAllRecipes() } returns flowOf(emptyList())

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
}
