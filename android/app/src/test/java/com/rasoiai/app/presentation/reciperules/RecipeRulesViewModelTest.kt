package com.rasoiai.app.presentation.reciperules

import app.cash.turbine.test
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.FoodCategory
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.NutritionGoal
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.model.RecipeRule
import com.rasoiai.domain.model.RuleAction
import com.rasoiai.domain.model.RuleEnforcement
import com.rasoiai.domain.model.RuleFrequency
import com.rasoiai.domain.model.RuleType
import com.rasoiai.domain.repository.RecipeRulesRepository
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeRulesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepository: RecipeRulesRepository

    private val testRecipeRules = listOf(
        RecipeRule(
            id = "rule-1",
            type = RuleType.RECIPE,
            action = RuleAction.INCLUDE,
            targetId = "recipe-1",
            targetName = "Poha",
            frequency = RuleFrequency.timesPerWeek(2),
            enforcement = RuleEnforcement.REQUIRED,
            mealSlot = null,
            isActive = true
        ),
        RecipeRule(
            id = "rule-2",
            type = RuleType.RECIPE,
            action = RuleAction.EXCLUDE,
            targetId = "recipe-2",
            targetName = "Biryani",
            frequency = RuleFrequency.NEVER,
            enforcement = RuleEnforcement.REQUIRED,
            mealSlot = null,
            isActive = true
        )
    )

    private val testIngredientRules = listOf(
        RecipeRule(
            id = "rule-3",
            type = RuleType.INGREDIENT,
            action = RuleAction.EXCLUDE,
            targetId = "ingredient-onion",
            targetName = "Onion",
            frequency = RuleFrequency.NEVER,
            enforcement = RuleEnforcement.REQUIRED,
            mealSlot = null,
            isActive = true
        )
    )

    private val testMealSlotRules = listOf(
        RecipeRule(
            id = "rule-4",
            type = RuleType.MEAL_SLOT,
            action = RuleAction.INCLUDE,
            targetId = "recipe-3",
            targetName = "Idli",
            frequency = RuleFrequency.DAILY,
            enforcement = RuleEnforcement.PREFERRED,
            mealSlot = MealType.BREAKFAST,
            isActive = true
        )
    )

    private val testNutritionGoals = listOf(
        NutritionGoal(
            id = "goal-1",
            foodCategory = FoodCategory.GREEN_LEAFY,
            weeklyTarget = 5,
            currentProgress = 2,
            isActive = true
        ),
        NutritionGoal(
            id = "goal-2",
            foodCategory = FoodCategory.HIGH_PROTEIN,
            weeklyTarget = 7,
            currentProgress = 3,
            isActive = true
        )
    )

    private val testPopularRecipes = listOf(
        Recipe(
            id = "recipe-1",
            name = "Poha",
            description = "Flattened rice breakfast",
            cuisineType = CuisineType.WEST,
            dietaryTags = listOf(DietaryTag.VEGETARIAN),
            prepTimeMinutes = 5,
            cookTimeMinutes = 10,
            servings = 2,
            difficulty = Difficulty.EASY,
            ingredients = emptyList(),
            instructions = emptyList()
        )
    )

    private val testPopularIngredients = listOf("Tomato", "Onion", "Garlic", "Ginger")

    private val testFoodCategories = listOf(
        FoodCategory.GREEN_LEAFY,
        FoodCategory.HIGH_PROTEIN,
        FoodCategory.IRON_RICH
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk(relaxed = true)

        every { mockRepository.getRulesByType(RuleType.RECIPE) } returns flowOf(testRecipeRules)
        every { mockRepository.getRulesByType(RuleType.INGREDIENT) } returns flowOf(testIngredientRules)
        every { mockRepository.getRulesByType(RuleType.MEAL_SLOT) } returns flowOf(testMealSlotRules)
        every { mockRepository.getAllNutritionGoals() } returns flowOf(testNutritionGoals)
        every { mockRepository.getPopularRecipes() } returns flowOf(testPopularRecipes)
        every { mockRepository.getPopularIngredients() } returns flowOf(testPopularIngredients)
        every { mockRepository.getAvailableFoodCategories() } returns flowOf(testFoodCategories)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        @DisplayName("Initial state should be loading")
        fun `initial state should be loading`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Default tab should be RECIPE")
        fun `default tab should be RECIPE`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(RulesTab.RECIPE, state.selectedTab)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("After loading, rules should be populated")
        fun `after loading rules should be populated`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isLoading)
                assertEquals(2, state.recipeRules.size)
                assertEquals(1, state.ingredientRules.size)
                assertEquals(1, state.mealSlotRules.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Nutrition goals should be loaded")
        fun `nutrition goals should be loaded`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals(2, state.nutritionGoals.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Popular recipes and ingredients should be loaded")
        fun `popular recipes and ingredients should be loaded`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals(1, state.popularRecipes.size)
                assertEquals(4, state.popularIngredients.size)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Tab Navigation")
    inner class TabNavigation {

        @Test
        @DisplayName("selectTab should update selected tab")
        fun `selectTab should update selected tab`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.selectTab(RulesTab.INGREDIENT)

                val state = awaitItem()
                assertEquals(RulesTab.INGREDIENT, state.selectedTab)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("rulesForCurrentTab should return correct rules for each tab")
        fun `rulesForCurrentTab should return correct rules for each tab`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Wait for load

                // Recipe tab
                viewModel.selectTab(RulesTab.RECIPE)
                var state = awaitItem()
                assertEquals(2, state.rulesForCurrentTab.size)

                // Ingredient tab
                viewModel.selectTab(RulesTab.INGREDIENT)
                state = awaitItem()
                assertEquals(1, state.rulesForCurrentTab.size)

                // Meal-slot tab
                viewModel.selectTab(RulesTab.MEAL_SLOT)
                state = awaitItem()
                assertEquals(1, state.rulesForCurrentTab.size)

                // Nutrition tab (returns empty list for rules)
                viewModel.selectTab(RulesTab.NUTRITION)
                state = awaitItem()
                assertEquals(0, state.rulesForCurrentTab.size)

                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("currentTabCount should return correct count")
        fun `currentTabCount should return correct count`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Wait for load

                viewModel.selectTab(RulesTab.NUTRITION)
                val state = awaitItem()
                assertEquals(2, state.currentTabCount) // 2 nutrition goals
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Add Rule Sheet")
    inner class AddRuleSheet {

        @Test
        @DisplayName("showAddRuleSheet should show sheet with default values")
        fun `showAddRuleSheet should show sheet with default values`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddRuleSheet()

                val state = awaitItem()
                assertTrue(state.showAddRuleSheet)
                assertNull(state.editingRule)
                assertEquals(RuleAction.INCLUDE, state.selectedAction)
                assertEquals("", state.searchQuery)
                assertEquals(FrequencyType.TIMES_PER_WEEK, state.selectedFrequencyType)
                assertEquals(RuleEnforcement.REQUIRED, state.selectedEnforcement)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("showAddRuleSheet on NUTRITION tab should show nutrition goal sheet")
        fun `showAddRuleSheet on NUTRITION tab should show nutrition goal sheet`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.selectTab(RulesTab.NUTRITION)
                awaitItem()

                viewModel.showAddRuleSheet()

                val state = awaitItem()
                assertTrue(state.showAddNutritionGoalSheet)
                assertFalse(state.showAddRuleSheet)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissAddRuleSheet should hide sheet")
        fun `dismissAddRuleSheet should hide sheet`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddRuleSheet()
                awaitItem()

                viewModel.dismissAddRuleSheet()

                val state = awaitItem()
                assertFalse(state.showAddRuleSheet)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("showEditRuleSheet should populate form with rule data")
        fun `showEditRuleSheet should populate form with rule data`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showEditRuleSheet(testRecipeRules[0])

                val state = awaitItem()
                assertTrue(state.showAddRuleSheet)
                assertEquals(testRecipeRules[0], state.editingRule)
                assertEquals(RuleAction.INCLUDE, state.selectedAction)
                assertEquals("Poha", state.selectedTargetName)
                assertEquals(FrequencyType.TIMES_PER_WEEK, state.selectedFrequencyType)
                assertEquals(2, state.selectedFrequencyCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("MEAL_SLOT tab should set default meal slot to BREAKFAST")
        fun `MEAL_SLOT tab should set default meal slot to BREAKFAST`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.selectTab(RulesTab.MEAL_SLOT)
                awaitItem()

                viewModel.showAddRuleSheet()

                val state = awaitItem()
                assertEquals(MealType.BREAKFAST, state.selectedMealSlot)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Nutrition Goal Sheet")
    inner class NutritionGoalSheet {

        @Test
        @DisplayName("showAddNutritionGoalSheet should show sheet with defaults")
        fun `showAddNutritionGoalSheet should show sheet with defaults`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Wait for categories to load

                viewModel.showAddNutritionGoalSheet()

                val state = awaitItem()
                assertTrue(state.showAddNutritionGoalSheet)
                assertNull(state.editingNutritionGoal)
                assertEquals(3, state.weeklyTarget)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("showEditNutritionGoalSheet should populate form with goal data")
        fun `showEditNutritionGoalSheet should populate form with goal data`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showEditNutritionGoalSheet(testNutritionGoals[0])

                val state = awaitItem()
                assertTrue(state.showAddNutritionGoalSheet)
                assertEquals(testNutritionGoals[0], state.editingNutritionGoal)
                assertEquals(FoodCategory.GREEN_LEAFY, state.selectedFoodCategory)
                assertEquals(5, state.weeklyTarget)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissAddNutritionGoalSheet should hide sheet")
        fun `dismissAddNutritionGoalSheet should hide sheet`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddNutritionGoalSheet()
                awaitItem()

                viewModel.dismissAddNutritionGoalSheet()

                val state = awaitItem()
                assertFalse(state.showAddNutritionGoalSheet)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Form Updates")
    inner class FormUpdates {

        @Test
        @DisplayName("updateAction should update selected action")
        fun `updateAction should update selected action`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateAction(RuleAction.EXCLUDE)

                val state = awaitItem()
                assertEquals(RuleAction.EXCLUDE, state.selectedAction)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateSearchQuery should update search query")
        fun `updateSearchQuery should update search query`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateSearchQuery("poha")

                val state = awaitItem()
                assertEquals("poha", state.searchQuery)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("selectRecipe should set target")
        fun `selectRecipe should set target`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.selectRecipe(testPopularRecipes[0])

                val state = awaitItem()
                assertEquals("recipe-1", state.selectedTargetId)
                assertEquals("Poha", state.selectedTargetName)
                assertEquals("Poha", state.searchQuery)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("selectIngredient should set target")
        fun `selectIngredient should set target`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.selectIngredient("Tomato")

                val state = awaitItem()
                assertEquals("ingredient-tomato", state.selectedTargetId)
                assertEquals("Tomato", state.selectedTargetName)
                assertEquals("Tomato", state.searchQuery)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateFrequencyType should update frequency type")
        fun `updateFrequencyType should update frequency type`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateFrequencyType(FrequencyType.DAILY)

                val state = awaitItem()
                assertEquals(FrequencyType.DAILY, state.selectedFrequencyType)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateFrequencyCount should coerce to valid range")
        fun `updateFrequencyCount should coerce to valid range`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateFrequencyCount(10) // Over max

                val state = awaitItem()
                assertEquals(7, state.selectedFrequencyCount) // Coerced to max
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("toggleDay should toggle day in selected days")
        fun `toggleDay should toggle day in selected days`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.toggleDay(DayOfWeek.MONDAY)
                var state = awaitItem()
                assertTrue(DayOfWeek.MONDAY in state.selectedDays)

                viewModel.toggleDay(DayOfWeek.MONDAY)
                state = awaitItem()
                assertFalse(DayOfWeek.MONDAY in state.selectedDays)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateMealSlot should update meal slot")
        fun `updateMealSlot should update meal slot`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateMealSlot(MealType.LUNCH)

                val state = awaitItem()
                assertEquals(MealType.LUNCH, state.selectedMealSlot)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateEnforcement should update enforcement")
        fun `updateEnforcement should update enforcement`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateEnforcement(RuleEnforcement.PREFERRED)

                val state = awaitItem()
                assertEquals(RuleEnforcement.PREFERRED, state.selectedEnforcement)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateFoodCategory should update category")
        fun `updateFoodCategory should update category`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateFoodCategory(FoodCategory.IRON_RICH)

                val state = awaitItem()
                assertEquals(FoodCategory.IRON_RICH, state.selectedFoodCategory)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateWeeklyTarget should coerce to valid range")
        fun `updateWeeklyTarget should coerce to valid range`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateWeeklyTarget(30) // Over max

                val state = awaitItem()
                assertEquals(21, state.weeklyTarget) // Coerced to max
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Save Operations")
    inner class SaveOperations {

        @Test
        @DisplayName("saveRule should call repository and dismiss sheet on success")
        fun `saveRule should call repository and dismiss sheet on success`() = runTest {
            coEvery { mockRepository.createRule(any()) } returns Result.success(testRecipeRules[0])

            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                // Set up form
                viewModel.showAddRuleSheet()
                awaitItem()
                viewModel.selectRecipe(testPopularRecipes[0])
                awaitItem()

                viewModel.saveRule()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.showAddRuleSheet)
                cancelAndIgnoreRemainingEvents()
            }

            coVerify { mockRepository.createRule(any()) }
        }

        @Test
        @DisplayName("saveRule with editing should call updateRule")
        fun `saveRule with editing should call updateRule`() = runTest {
            coEvery { mockRepository.updateRule(any()) } returns Result.success(Unit)

            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showEditRuleSheet(testRecipeRules[0])
                awaitItem()

                viewModel.saveRule()
                testDispatcher.scheduler.advanceUntilIdle()

                expectMostRecentItem()
                cancelAndIgnoreRemainingEvents()
            }

            coVerify { mockRepository.updateRule(any()) }
        }

        @Test
        @DisplayName("saveNutritionGoal should call repository and dismiss sheet")
        fun `saveNutritionGoal should call repository and dismiss sheet`() = runTest {
            coEvery { mockRepository.createNutritionGoal(any()) } returns Result.success(testNutritionGoals[0])

            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddNutritionGoalSheet()
                awaitItem()
                viewModel.updateFoodCategory(FoodCategory.GREEN_LEAFY)
                awaitItem()
                viewModel.updateWeeklyTarget(5)
                awaitItem()

                viewModel.saveNutritionGoal()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.showAddNutritionGoalSheet)
                cancelAndIgnoreRemainingEvents()
            }

            coVerify { mockRepository.createNutritionGoal(any()) }
        }
    }

    @Nested
    @DisplayName("Toggle Operations")
    inner class ToggleOperations {

        @Test
        @DisplayName("toggleRuleActive should call repository")
        fun `toggleRuleActive should call repository`() = runTest {
            coEvery { mockRepository.toggleRuleActive(any(), any()) } returns Result.success(Unit)

            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.toggleRuleActive(testRecipeRules[0])
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { mockRepository.toggleRuleActive("rule-1", false) }
        }

        @Test
        @DisplayName("toggleNutritionGoalActive should call repository")
        fun `toggleNutritionGoalActive should call repository`() = runTest {
            coEvery { mockRepository.toggleNutritionGoalActive(any(), any()) } returns Result.success(Unit)

            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.toggleNutritionGoalActive(testNutritionGoals[0])
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { mockRepository.toggleNutritionGoalActive("goal-1", false) }
        }

        @Test
        @DisplayName("toggleNutritionGoalEnforcement should toggle between REQUIRED and PREFERRED")
        fun `toggleNutritionGoalEnforcement should toggle between REQUIRED and PREFERRED`() = runTest {
            coEvery { mockRepository.updateNutritionGoal(any()) } returns Result.success(Unit)

            val viewModel = RecipeRulesViewModel(mockRepository)

            // Goal with REQUIRED enforcement
            viewModel.toggleNutritionGoalEnforcement(testNutritionGoals[0])
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify {
                mockRepository.updateNutritionGoal(match { it.enforcement == RuleEnforcement.PREFERRED })
            }
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    inner class DeleteOperations {

        @Test
        @DisplayName("showDeleteConfirmation for rule should show dialog")
        fun `showDeleteConfirmation for rule should show dialog`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showDeleteConfirmation(testRecipeRules[0])

                val state = awaitItem()
                assertTrue(state.showDeleteConfirmation)
                assertEquals(testRecipeRules[0], state.ruleToDelete)
                assertNull(state.goalToDelete)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("showDeleteConfirmation for goal should show dialog")
        fun `showDeleteConfirmation for goal should show dialog`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showDeleteConfirmation(testNutritionGoals[0])

                val state = awaitItem()
                assertTrue(state.showDeleteConfirmation)
                assertNull(state.ruleToDelete)
                assertEquals(testNutritionGoals[0], state.goalToDelete)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissDeleteConfirmation should hide dialog")
        fun `dismissDeleteConfirmation should hide dialog`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showDeleteConfirmation(testRecipeRules[0])
                awaitItem()

                viewModel.dismissDeleteConfirmation()

                val state = awaitItem()
                assertFalse(state.showDeleteConfirmation)
                assertNull(state.ruleToDelete)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("confirmDelete should delete rule and dismiss dialog")
        fun `confirmDelete should delete rule and dismiss dialog`() = runTest {
            coEvery { mockRepository.deleteRule(any()) } returns Result.success(Unit)

            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showDeleteConfirmation(testRecipeRules[0])
                awaitItem()

                viewModel.confirmDelete()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.showDeleteConfirmation)
                cancelAndIgnoreRemainingEvents()
            }

            coVerify { mockRepository.deleteRule("rule-1") }
        }

        @Test
        @DisplayName("confirmDelete should delete nutrition goal")
        fun `confirmDelete should delete nutrition goal`() = runTest {
            coEvery { mockRepository.deleteNutritionGoal(any()) } returns Result.success(Unit)

            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showDeleteConfirmation(testNutritionGoals[0])
                awaitItem()

                viewModel.confirmDelete()
                testDispatcher.scheduler.advanceUntilIdle()

                expectMostRecentItem()
                cancelAndIgnoreRemainingEvents()
            }

            coVerify { mockRepository.deleteNutritionGoal("goal-1") }
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        @DisplayName("navigateBack should emit back event")
        fun `navigateBack should emit back event`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.navigateBack()

            viewModel.navigationEvent.test {
                val event = awaitItem()
                assertEquals(RecipeRulesNavigationEvent.NavigateBack, event)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("clearError should clear error message")
        fun `clearError should clear error message`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.clearError()

                val state = awaitItem()
                assertNull(state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("save failure should set error message")
        fun `save failure should set error message`() = runTest {
            coEvery { mockRepository.createRule(any()) } returns Result.failure(Exception("Save failed"))

            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddRuleSheet()
                awaitItem()
                viewModel.selectRecipe(testPopularRecipes[0])
                awaitItem()

                viewModel.saveRule()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("Failed to save rule", state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Computed Properties")
    inner class ComputedProperties {

        @Test
        @DisplayName("isEditing should be true when editing rule")
        fun `isEditing should be true when editing rule`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showEditRuleSheet(testRecipeRules[0])

                val state = awaitItem()
                assertTrue(state.isEditing)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("canSaveRule should be false when target is empty")
        fun `canSaveRule should be false when target is empty`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddRuleSheet()

                val state = awaitItem()
                assertFalse(state.canSaveRule)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("canSaveRule should be true when target is set")
        fun `canSaveRule should be true when target is set`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddRuleSheet()
                awaitItem()
                viewModel.selectRecipe(testPopularRecipes[0])

                val state = awaitItem()
                assertTrue(state.canSaveRule)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("canSaveRule with SPECIFIC_DAYS should require days selected")
        fun `canSaveRule with SPECIFIC_DAYS should require days selected`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddRuleSheet()
                awaitItem()
                viewModel.selectRecipe(testPopularRecipes[0])
                awaitItem()
                viewModel.updateFrequencyType(FrequencyType.SPECIFIC_DAYS)

                var state = awaitItem()
                assertFalse(state.canSaveRule) // No days selected

                viewModel.toggleDay(DayOfWeek.MONDAY)
                state = awaitItem()
                assertTrue(state.canSaveRule) // Day selected
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("canSaveNutritionGoal should be false when category is null")
        fun `canSaveNutritionGoal should be false when category is null`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                val state = awaitItem() // Initial
                // Category is null by default
                assertFalse(state.canSaveNutritionGoal)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("canSaveNutritionGoal should be true when category is set")
        fun `canSaveNutritionGoal should be true when category is set`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateFoodCategory(FoodCategory.GREEN_LEAFY)

                val state = awaitItem()
                assertTrue(state.canSaveNutritionGoal)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("RulesTab Enum")
    inner class RulesTabEnum {

        @Test
        @DisplayName("fromRuleType should map correctly")
        fun `fromRuleType should map correctly`() {
            assertEquals(RulesTab.RECIPE, RulesTab.fromRuleType(RuleType.RECIPE))
            assertEquals(RulesTab.INGREDIENT, RulesTab.fromRuleType(RuleType.INGREDIENT))
            assertEquals(RulesTab.MEAL_SLOT, RulesTab.fromRuleType(RuleType.MEAL_SLOT))
            assertEquals(RulesTab.NUTRITION, RulesTab.fromRuleType(RuleType.NUTRITION))
        }

        @Test
        @DisplayName("toRuleType should map correctly")
        fun `toRuleType should map correctly`() {
            assertEquals(RuleType.RECIPE, RulesTab.RECIPE.toRuleType())
            assertEquals(RuleType.INGREDIENT, RulesTab.INGREDIENT.toRuleType())
            assertEquals(RuleType.MEAL_SLOT, RulesTab.MEAL_SLOT.toRuleType())
            assertEquals(RuleType.NUTRITION, RulesTab.NUTRITION.toRuleType())
        }
    }
}
