package com.rasoiai.app.presentation.reciperules

import app.cash.turbine.test
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DataScope
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
import com.rasoiai.domain.repository.SettingsRepository
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
    private lateinit var mockSettingsRepository: SettingsRepository

    private val testAllRules = listOf(
        RecipeRule(
            id = "rule-1",
            type = RuleType.RECIPE,
            action = RuleAction.INCLUDE,
            targetId = "recipe-1",
            targetName = "Poha",
            frequency = RuleFrequency.timesPerWeek(2),
            enforcement = RuleEnforcement.REQUIRED,
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
            isActive = true
        ),
        RecipeRule(
            id = "rule-3",
            type = RuleType.INGREDIENT,
            action = RuleAction.EXCLUDE,
            targetId = "ingredient-onion",
            targetName = "Onion",
            frequency = RuleFrequency.NEVER,
            enforcement = RuleEnforcement.REQUIRED,
            isActive = true
        ),
        RecipeRule(
            id = "rule-4",
            type = RuleType.MEAL_SLOT,
            action = RuleAction.INCLUDE,
            targetId = "recipe-3",
            targetName = "Idli",
            frequency = RuleFrequency.DAILY,
            enforcement = RuleEnforcement.PREFERRED,
            mealSlots = listOf(MealType.BREAKFAST),
            isActive = true
        ),
        RecipeRule(
            id = "rule-5",
            type = RuleType.INGREDIENT,
            action = RuleAction.INCLUDE,
            targetId = "ingredient-eggs",
            targetName = "Eggs",
            frequency = RuleFrequency.DAILY,
            enforcement = RuleEnforcement.PREFERRED,
            mealSlots = listOf(MealType.BREAKFAST),
            isActive = false
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
            imageUrl = null,
            prepTimeMinutes = 5,
            cookTimeMinutes = 10,
            servings = 2,
            difficulty = Difficulty.EASY,
            cuisineType = CuisineType.WEST,
            mealTypes = listOf(MealType.BREAKFAST),
            dietaryTags = listOf(DietaryTag.VEGETARIAN),
            ingredients = emptyList(),
            instructions = emptyList(),
            nutrition = null
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
        mockSettingsRepository = mockk(relaxed = true)

        every { mockRepository.getAllRules() } returns flowOf(testAllRules)
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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Default tab should be RULES")
        fun `default tab should be RULES`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(RulesTab.RULES, state.selectedTab)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("After loading, all rules should be in single list")
        fun `after loading all rules should be in single list`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isLoading)
                assertEquals(5, state.allRules.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Sorted rules should show active first then paused")
        fun `sorted rules should show active first then paused`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                val sorted = state.sortedRules
                // Active rules (4) should come before paused rules (1)
                val activeCount = sorted.takeWhile { it.isActive }.size
                assertEquals(4, activeCount)
                assertFalse(sorted.last().isActive)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Nutrition goals should be loaded")
        fun `nutrition goals should be loaded`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals(2, state.nutritionGoals.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Popular items should be mixed recipes and ingredients")
        fun `popular items should be mixed recipes and ingredients`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertTrue(state.popularItems.isNotEmpty())
                // Should contain both types
                assertTrue(state.popularItems.any { it is SearchResultItem.RecipeItem })
                assertTrue(state.popularItems.any { it is SearchResultItem.IngredientItem })
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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.selectTab(RulesTab.NUTRITION)

                val state = awaitItem()
                assertEquals(RulesTab.NUTRITION, state.selectedTab)
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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddRuleSheet()

                val state = awaitItem()
                assertTrue(state.showAddRuleSheet)
                assertNull(state.editingRule)
                assertEquals(RuleAction.INCLUDE, state.selectedAction)
                assertEquals("", state.searchQuery)
                assertNull(state.selectedTarget)
                assertEquals(FrequencyType.TIMES_PER_WEEK, state.selectedFrequencyType)
                assertEquals(MealSlotMode.ANY, state.mealSlotMode)
                assertTrue(state.selectedMealSlots.isEmpty())
                assertEquals(RuleEnforcement.REQUIRED, state.selectedEnforcement)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("showAddRuleSheet on NUTRITION tab should show nutrition goal sheet")
        fun `showAddRuleSheet on NUTRITION tab should show nutrition goal sheet`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showEditRuleSheet(testAllRules[0])

                val state = awaitItem()
                assertTrue(state.showAddRuleSheet)
                assertEquals(testAllRules[0], state.editingRule)
                assertEquals(RuleAction.INCLUDE, state.selectedAction)
                assertTrue(state.selectedTarget is SearchResultItem.RecipeItem)
                assertEquals("Poha", state.selectedTarget?.displayName)
                assertEquals(FrequencyType.TIMES_PER_WEEK, state.selectedFrequencyType)
                assertEquals(2, state.selectedFrequencyCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("showEditRuleSheet should set meal slot mode to SPECIFIC for rules with slots")
        fun `showEditRuleSheet should set meal slot mode to SPECIFIC for rules with slots`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showEditRuleSheet(testAllRules[3]) // Idli with BREAKFAST slot

                val state = awaitItem()
                assertEquals(MealSlotMode.SPECIFIC, state.mealSlotMode)
                assertTrue(MealType.BREAKFAST in state.selectedMealSlots)
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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
            every { mockRepository.searchRecipes(any()) } returns flowOf(emptyList())
            every { mockRepository.searchIngredients(any()) } returns flowOf(emptyList())

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.uiState.test {
                val initial = awaitItem()
                assertEquals("", initial.searchQuery)

                viewModel.updateSearchQuery("poha")

                val state = awaitItem()
                assertEquals("poha", state.searchQuery)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("selectSearchResult with recipe should set target")
        fun `selectSearchResult with recipe should set target`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                val item = SearchResultItem.RecipeItem(testPopularRecipes[0])
                viewModel.selectSearchResult(item)

                val state = awaitItem()
                assertEquals(item, state.selectedTarget)
                assertEquals("Poha", state.selectedTarget?.displayName)
                assertTrue(state.selectedTarget?.isRecipe == true)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("selectSearchResult with ingredient should set target")
        fun `selectSearchResult with ingredient should set target`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                val item = SearchResultItem.IngredientItem("Tomato")
                viewModel.selectSearchResult(item)

                val state = awaitItem()
                assertEquals(item, state.selectedTarget)
                assertEquals("Tomato", state.selectedTarget?.displayName)
                assertFalse(state.selectedTarget?.isRecipe == true)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("clearSelectedTarget should clear target")
        fun `clearSelectedTarget should clear target`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                val item = SearchResultItem.IngredientItem("Tomato")
                viewModel.selectSearchResult(item)
                awaitItem()

                viewModel.clearSelectedTarget()

                val state = awaitItem()
                assertNull(state.selectedTarget)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateFrequencyType should update frequency type")
        fun `updateFrequencyType should update frequency type`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
        @DisplayName("updateMealSlotMode should update mode")
        fun `updateMealSlotMode should update mode`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateMealSlotMode(MealSlotMode.SPECIFIC)

                val state = awaitItem()
                assertEquals(MealSlotMode.SPECIFIC, state.mealSlotMode)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateMealSlotMode to ANY should clear selected slots")
        fun `updateMealSlotMode to ANY should clear selected slots`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateMealSlotMode(MealSlotMode.SPECIFIC)
                awaitItem()
                viewModel.toggleMealSlot(MealType.BREAKFAST)
                awaitItem()

                viewModel.updateMealSlotMode(MealSlotMode.ANY)
                val state = awaitItem()
                assertTrue(state.selectedMealSlots.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("toggleMealSlot should toggle meal slot in selection")
        fun `toggleMealSlot should toggle meal slot in selection`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.toggleMealSlot(MealType.BREAKFAST)
                var state = awaitItem()
                assertTrue(MealType.BREAKFAST in state.selectedMealSlots)

                viewModel.toggleMealSlot(MealType.LUNCH)
                state = awaitItem()
                assertTrue(MealType.BREAKFAST in state.selectedMealSlots)
                assertTrue(MealType.LUNCH in state.selectedMealSlots)

                viewModel.toggleMealSlot(MealType.BREAKFAST)
                state = awaitItem()
                assertFalse(MealType.BREAKFAST in state.selectedMealSlots)
                assertTrue(MealType.LUNCH in state.selectedMealSlots)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateEnforcement should update enforcement")
        fun `updateEnforcement should update enforcement`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
            coEvery { mockRepository.createRule(any()) } returns Result.success(testAllRules[0])

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                // Set up form
                viewModel.showAddRuleSheet()
                awaitItem()
                viewModel.selectSearchResult(SearchResultItem.RecipeItem(testPopularRecipes[0]))
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

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showEditRuleSheet(testAllRules[0])
                awaitItem()

                viewModel.saveRule()
                testDispatcher.scheduler.advanceUntilIdle()

                expectMostRecentItem()
                cancelAndIgnoreRemainingEvents()
            }

            coVerify { mockRepository.updateRule(any()) }
        }

        @Test
        @DisplayName("saveRule should auto-infer MEAL_SLOT type when recipe has specific slots")
        fun `saveRule should auto-infer MEAL_SLOT type when recipe has specific slots`() = runTest {
            coEvery { mockRepository.createRule(any()) } returns Result.success(testAllRules[3])

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddRuleSheet()
                awaitItem()
                viewModel.selectSearchResult(SearchResultItem.RecipeItem(testPopularRecipes[0]))
                awaitItem()
                viewModel.updateMealSlotMode(MealSlotMode.SPECIFIC)
                awaitItem()
                viewModel.toggleMealSlot(MealType.BREAKFAST)
                awaitItem()

                viewModel.saveRule()
                testDispatcher.scheduler.advanceUntilIdle()

                expectMostRecentItem()
                cancelAndIgnoreRemainingEvents()
            }

            coVerify {
                mockRepository.createRule(match { it.type == RuleType.MEAL_SLOT })
            }
        }

        @Test
        @DisplayName("saveRule should auto-infer INGREDIENT type for ingredients regardless of slots")
        fun `saveRule should auto-infer INGREDIENT type for ingredients regardless of slots`() = runTest {
            coEvery { mockRepository.createRule(any()) } returns Result.success(testAllRules[2])

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddRuleSheet()
                awaitItem()
                viewModel.selectSearchResult(SearchResultItem.IngredientItem("Onion"))
                awaitItem()
                viewModel.updateMealSlotMode(MealSlotMode.SPECIFIC)
                awaitItem()
                viewModel.toggleMealSlot(MealType.DINNER)
                awaitItem()

                viewModel.saveRule()
                testDispatcher.scheduler.advanceUntilIdle()

                expectMostRecentItem()
                cancelAndIgnoreRemainingEvents()
            }

            coVerify {
                mockRepository.createRule(match { it.type == RuleType.INGREDIENT })
            }
        }

        @Test
        @DisplayName("saveNutritionGoal should call repository and dismiss sheet")
        fun `saveNutritionGoal should call repository and dismiss sheet`() = runTest {
            coEvery { mockRepository.createNutritionGoal(any()) } returns Result.success(testNutritionGoals[0])

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.toggleRuleActive(testAllRules[0])
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { mockRepository.toggleRuleActive("rule-1", false) }
        }

        @Test
        @DisplayName("toggleNutritionGoalActive should call repository")
        fun `toggleNutritionGoalActive should call repository`() = runTest {
            coEvery { mockRepository.toggleNutritionGoalActive(any(), any()) } returns Result.success(Unit)

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.toggleNutritionGoalActive(testNutritionGoals[0])
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { mockRepository.toggleNutritionGoalActive("goal-1", false) }
        }

        @Test
        @DisplayName("toggleNutritionGoalEnforcement should toggle between REQUIRED and PREFERRED")
        fun `toggleNutritionGoalEnforcement should toggle between REQUIRED and PREFERRED`() = runTest {
            coEvery { mockRepository.updateNutritionGoal(any()) } returns Result.success(Unit)

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.toggleNutritionGoalEnforcement(testNutritionGoals[0])
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify {
                mockRepository.updateNutritionGoal(match { it.enforcement == RuleEnforcement.REQUIRED })
            }
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    inner class DeleteOperations {

        @Test
        @DisplayName("showDeleteConfirmation for rule should show dialog")
        fun `showDeleteConfirmation for rule should show dialog`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showDeleteConfirmation(testAllRules[0])

                val state = awaitItem()
                assertTrue(state.showDeleteConfirmation)
                assertEquals(testAllRules[0], state.ruleToDelete)
                assertNull(state.goalToDelete)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("showDeleteConfirmation for goal should show dialog")
        fun `showDeleteConfirmation for goal should show dialog`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showDeleteConfirmation(testAllRules[0])
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

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showDeleteConfirmation(testAllRules[0])
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

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateBack()
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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddRuleSheet()
                awaitItem()
                viewModel.selectSearchResult(SearchResultItem.RecipeItem(testPopularRecipes[0]))
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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showEditRuleSheet(testAllRules[0])

                val state = awaitItem()
                assertTrue(state.isEditing)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("canSaveRule should be false when target is null")
        fun `canSaveRule should be false when target is null`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddRuleSheet()
                awaitItem()
                viewModel.selectSearchResult(SearchResultItem.RecipeItem(testPopularRecipes[0]))

                val state = awaitItem()
                assertTrue(state.canSaveRule)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("canSaveRule with SPECIFIC_DAYS should require days selected")
        fun `canSaveRule with SPECIFIC_DAYS should require days selected`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddRuleSheet()
                awaitItem()
                viewModel.selectSearchResult(SearchResultItem.RecipeItem(testPopularRecipes[0]))
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
        @DisplayName("canSaveRule with SPECIFIC meal slots should require slots selected")
        fun `canSaveRule with SPECIFIC meal slots should require slots selected`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddRuleSheet()
                awaitItem()
                viewModel.selectSearchResult(SearchResultItem.RecipeItem(testPopularRecipes[0]))
                awaitItem()
                viewModel.updateMealSlotMode(MealSlotMode.SPECIFIC)

                var state = awaitItem()
                assertFalse(state.canSaveRule) // No slots selected

                viewModel.toggleMealSlot(MealType.BREAKFAST)
                state = awaitItem()
                assertTrue(state.canSaveRule) // Slot selected
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("canSaveNutritionGoal should be false when category is null")
        fun `canSaveNutritionGoal should be false when category is null`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                val state = awaitItem() // Initial
                assertFalse(state.canSaveNutritionGoal)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("canSaveNutritionGoal should be true when category is set")
        fun `canSaveNutritionGoal should be true when category is set`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

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
    @DisplayName("Force Override")
    inner class ForceOverride {

        @Test
        @DisplayName("FamilyConflictException should show conflict dialog")
        fun `FamilyConflictException should show conflict dialog`() = runTest {
            val conflictDetails = listOf(
                com.rasoiai.domain.model.ConflictDetail(
                    memberName = "Ramesh",
                    condition = "DIABETIC",
                    keyword = "sugar",
                    ruleTarget = "Gulab Jamun"
                )
            )
            val exception = com.rasoiai.domain.model.FamilyConflictException(
                message = "Family safety conflict",
                conflictDetails = conflictDetails
            )
            coEvery { mockRepository.createRule(any()) } returns Result.failure(exception)

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // After load

                // Set up rule form state to enable save
                viewModel.updateAction(RuleAction.INCLUDE)
                viewModel.selectSearchResult(
                    SearchResultItem.IngredientItem("Gulab Jamun")
                )

                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem()

                viewModel.saveRule()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertTrue(state.showConflictDialog)
                assertEquals(1, state.pendingConflictDetails.size)
                assertEquals("Ramesh", state.pendingConflictDetails[0].memberName)
                assertEquals("DIABETIC", state.pendingConflictDetails[0].condition)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("confirmForceOverride should retry with forceOverride=true")
        fun `confirmForceOverride should retry with forceOverride true`() = runTest {
            val conflictDetails = listOf(
                com.rasoiai.domain.model.ConflictDetail(
                    memberName = "Ramesh",
                    condition = "DIABETIC",
                    keyword = "sugar",
                    ruleTarget = "Gulab Jamun"
                )
            )
            val exception = com.rasoiai.domain.model.FamilyConflictException(
                message = "Family safety conflict",
                conflictDetails = conflictDetails
            )
            // First call fails with conflict, second call (with forceOverride) succeeds
            coEvery { mockRepository.createRule(match { !it.forceOverride }) } returns Result.failure(exception)
            coEvery { mockRepository.createRule(match { it.forceOverride }) } returns Result.success(
                RecipeRule(
                    id = "new-rule",
                    type = RuleType.INGREDIENT,
                    action = RuleAction.INCLUDE,
                    targetId = "ingredient-gulab-jamun",
                    targetName = "Gulab Jamun",
                    frequency = RuleFrequency.DAILY,
                    enforcement = RuleEnforcement.REQUIRED,
                    isActive = true,
                    forceOverride = true
                )
            )

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem()

                viewModel.updateAction(RuleAction.INCLUDE)
                viewModel.selectSearchResult(SearchResultItem.IngredientItem("Gulab Jamun"))
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem()

                viewModel.saveRule()
                testDispatcher.scheduler.advanceUntilIdle()

                val conflictState = expectMostRecentItem()
                assertTrue(conflictState.showConflictDialog)

                viewModel.confirmForceOverride()
                testDispatcher.scheduler.advanceUntilIdle()

                val finalState = expectMostRecentItem()
                assertFalse(finalState.showConflictDialog)
                assertNull(finalState.pendingConflictRule)

                coVerify { mockRepository.createRule(match { it.forceOverride }) }
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissConflictDialog should clear conflict state")
        fun `dismissConflictDialog should clear conflict state`() = runTest {
            val conflictDetails = listOf(
                com.rasoiai.domain.model.ConflictDetail(
                    memberName = "Ramesh",
                    condition = "DIABETIC",
                    keyword = "sugar",
                    ruleTarget = "Gulab Jamun"
                )
            )
            val exception = com.rasoiai.domain.model.FamilyConflictException(
                message = "Family safety conflict",
                conflictDetails = conflictDetails
            )
            coEvery { mockRepository.createRule(any()) } returns Result.failure(exception)

            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem()

                viewModel.updateAction(RuleAction.INCLUDE)
                viewModel.selectSearchResult(SearchResultItem.IngredientItem("Gulab Jamun"))
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem()

                viewModel.saveRule()
                testDispatcher.scheduler.advanceUntilIdle()

                val conflictState = expectMostRecentItem()
                assertTrue(conflictState.showConflictDialog)

                viewModel.dismissConflictDialog()

                val finalState = awaitItem()
                assertFalse(finalState.showConflictDialog)
                assertTrue(finalState.pendingConflictDetails.isEmpty())
                assertNull(finalState.pendingConflictRule)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Scope Toggle")
    inner class ScopeToggleTests {

        @Test
        @DisplayName("initial selectedScope should be PERSONAL")
        fun `initial selectedScope should be PERSONAL`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(DataScope.PERSONAL, state.selectedScope)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("setScope to FAMILY should update selectedScope")
        fun `setScope to FAMILY should update selectedScope`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.setScope(DataScope.FAMILY)

                val state = awaitItem()
                assertEquals(DataScope.FAMILY, state.selectedScope)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("setScope back to PERSONAL should restore selectedScope")
        fun `setScope back to PERSONAL should restore selectedScope`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.setScope(DataScope.FAMILY)
                awaitItem() // FAMILY

                viewModel.setScope(DataScope.PERSONAL)

                val state = awaitItem()
                assertEquals(DataScope.PERSONAL, state.selectedScope)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("setScope should not affect other state fields")
        fun `setScope should not affect other state fields`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()
                val beforeScope = expectMostRecentItem()

                viewModel.setScope(DataScope.FAMILY)
                val afterScope = awaitItem()

                assertEquals(beforeScope.allRules, afterScope.allRules)
                assertEquals(beforeScope.nutritionGoals, afterScope.nutritionGoals)
                assertEquals(DataScope.FAMILY, afterScope.selectedScope)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("setScope should not trigger additional repository calls")
        fun `setScope should not trigger additional repository calls`() = runTest {
            val viewModel = RecipeRulesViewModel(mockRepository, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem()

                viewModel.setScope(DataScope.FAMILY)
                awaitItem()

                // setScope only updates local state, no new repository calls
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
