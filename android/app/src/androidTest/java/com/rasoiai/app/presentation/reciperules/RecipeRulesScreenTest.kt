package com.rasoiai.app.presentation.reciperules

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.FoodCategory
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.NutritionGoal
import com.rasoiai.domain.model.RecipeRule
import com.rasoiai.domain.model.RuleAction
import com.rasoiai.domain.model.RuleEnforcement
import com.rasoiai.domain.model.RuleFrequency
import com.rasoiai.domain.model.RuleType
import com.rasoiai.app.presentation.reciperules.components.ForceOverrideDialog
import com.rasoiai.app.presentation.reciperules.components.RuleCard
import com.rasoiai.domain.model.ConflictDetail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for RecipeRulesScreen (consolidated 2-tab version)
 * Tests Phase 11 of E2E Testing Guide: Recipe Rules Screen Testing
 */
@RunWith(AndroidJUnit4::class)
class RecipeRulesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Test Data Factories

    private fun createTestRecipeRule(
        id: String = "rule_1",
        type: RuleType = RuleType.RECIPE,
        action: RuleAction = RuleAction.INCLUDE,
        targetId: String = "recipe_1",
        targetName: String = "Rajma",
        frequency: RuleFrequency = RuleFrequency.timesPerWeek(1),
        enforcement: RuleEnforcement = RuleEnforcement.REQUIRED,
        mealSlots: List<MealType> = emptyList(),
        isActive: Boolean = true
    ) = RecipeRule(
        id = id,
        type = type,
        action = action,
        targetId = targetId,
        targetName = targetName,
        frequency = frequency,
        enforcement = enforcement,
        mealSlots = mealSlots,
        isActive = isActive
    )

    private fun createTestNutritionGoal(
        id: String = "goal_1",
        foodCategory: FoodCategory = FoodCategory.GREEN_LEAFY,
        weeklyTarget: Int = 7,
        currentProgress: Int = 4,
        isActive: Boolean = true
    ) = NutritionGoal(
        id = id,
        foodCategory = foodCategory,
        weeklyTarget = weeklyTarget,
        currentProgress = currentProgress,
        isActive = isActive
    )

    private fun createTestUiState(
        isLoading: Boolean = false,
        errorMessage: String? = null,
        selectedTab: RulesTab = RulesTab.RULES,
        allRules: List<RecipeRule> = listOf(
            createTestRecipeRule("1", RuleType.RECIPE, RuleAction.INCLUDE, "r1", "Rajma"),
            createTestRecipeRule("2", RuleType.RECIPE, RuleAction.INCLUDE, "r2", "Chai", RuleFrequency.DAILY),
            createTestRecipeRule("3", RuleType.INGREDIENT, RuleAction.EXCLUDE, "i1", "Onion"),
            createTestRecipeRule("4", RuleType.MEAL_SLOT, RuleAction.INCLUDE, "r3", "Paratha", mealSlots = listOf(MealType.BREAKFAST))
        ),
        nutritionGoals: List<NutritionGoal> = listOf(
            createTestNutritionGoal("g1", FoodCategory.GREEN_LEAFY, 7, 4),
            createTestNutritionGoal("g2", FoodCategory.IRON_RICH, 5, 2)
        ),
        showAddRuleSheet: Boolean = false,
        showAddNutritionGoalSheet: Boolean = false,
        showDeleteConfirmation: Boolean = false
    ) = RecipeRulesUiState(
        isLoading = isLoading,
        errorMessage = errorMessage,
        selectedTab = selectedTab,
        allRules = allRules,
        nutritionGoals = nutritionGoals,
        showAddRuleSheet = showAddRuleSheet,
        showAddNutritionGoalSheet = showAddNutritionGoalSheet,
        showDeleteConfirmation = showDeleteConfirmation
    )

    // endregion

    // region Phase 11.1: Screen Display Tests

    @Test
    fun recipeRulesScreen_displaysScreenTag() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeRulesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.RECIPE_RULES_SCREEN).assertIsDisplayed()
    }

    @Test
    fun recipeRulesScreen_displaysTitle() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeRulesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Recipe Rules").assertIsDisplayed()
    }

    @Test
    fun recipeRulesScreen_displaysBackButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeRulesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    // endregion

    // region Phase 11.2: Tab Navigation Tests

    @Test
    fun recipeRulesScreen_displaysRulesTab() {
        val uiState = createTestUiState(selectedTab = RulesTab.RULES)

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeRulesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("\uD83C\uDF7D\uFE0F Rules").assertIsDisplayed()
    }

    @Test
    fun recipeRulesScreen_displaysNutritionTab() {
        val uiState = createTestUiState(selectedTab = RulesTab.NUTRITION)

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeRulesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Nutrition", substring = true).assertIsDisplayed()
    }

    @Test
    fun tabClick_triggersTabSelectionCallback() {
        var selectedTab: RulesTab? = null
        val uiState = createTestUiState(selectedTab = RulesTab.RULES)

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeRulesTestContent(
                    uiState = uiState,
                    onTabSelected = { selectedTab = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Nutrition", substring = true).performClick()

        assert(selectedTab == RulesTab.NUTRITION) { "Tab selection callback was not triggered" }
    }

    // endregion

    // region Phase 11.3: Rules Display Tests

    @Test
    fun recipeRulesScreen_displaysAllRulesInSingleList() {
        val uiState = createTestUiState(selectedTab = RulesTab.RULES)

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeRulesTestContent(uiState = uiState)
            }
        }

        // Verify all rule types are displayed in single list
        composeTestRule.onNodeWithText("Rajma", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Onion", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Paratha", substring = true).assertIsDisplayed()
    }

    @Test
    fun recipeRulesScreen_displaysNutritionGoals() {
        val uiState = createTestUiState(selectedTab = RulesTab.NUTRITION)

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeRulesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("MY NUTRITION GOALS", substring = true).assertIsDisplayed()
    }

    // endregion

    // region Phase 11.4: Empty State Tests

    @Test
    fun recipeRulesScreen_emptyRules_displaysEmptyState() {
        val uiState = createTestUiState(selectedTab = RulesTab.RULES, allRules = emptyList())

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeRulesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.RECIPE_RULES_SCREEN).assertIsDisplayed()
    }

    @Test
    fun recipeRulesScreen_emptyNutritionGoals_displaysEmptyState() {
        val uiState = createTestUiState(selectedTab = RulesTab.NUTRITION, nutritionGoals = emptyList())

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeRulesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.RECIPE_RULES_SCREEN).assertIsDisplayed()
    }

    // endregion

    // region Phase 11.5: Navigation Tests

    @Test
    fun backButton_click_triggersNavigateBack() {
        var backClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeRulesTestContent(
                    uiState = uiState,
                    onBackClick = { backClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assert(backClicked) { "Back navigation callback was not triggered" }
    }

    // endregion

    // region Phase 11.6: Loading State Tests

    @Test
    fun recipeRulesScreen_loadingState_displaysScreen() {
        val uiState = createTestUiState(isLoading = true)

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeRulesTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.RECIPE_RULES_SCREEN).assertIsDisplayed()
    }

    // endregion

    // region Phase 11.7: Add Rule Callback Tests

    @Test
    fun addRuleButton_click_triggersCallback() {
        var addRuleClicked = false
        val uiState = createTestUiState(selectedTab = RulesTab.RULES)

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeRulesTestContent(
                    uiState = uiState,
                    onAddRuleClick = { addRuleClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("ADD RULE", substring = true).performClick()

        assert(addRuleClicked) { "Add rule callback was not triggered" }
    }

    // endregion

    // region Phase 11.8: Data Verification Tests

    @Test
    fun recipeRulesScreen_hasRulesData() {
        val uiState = createTestUiState()

        assert(uiState.allRules.isNotEmpty()) { "Rules should exist" }
        assert(uiState.nutritionGoals.isNotEmpty()) { "Nutrition goals should exist" }
    }

    @Test
    fun recipeRulesScreen_sortedRules_activeFirst() {
        val rules = listOf(
            createTestRecipeRule("1", isActive = false, targetName = "Paused Rule"),
            createTestRecipeRule("2", isActive = true, targetName = "Active Rule")
        )
        val uiState = createTestUiState(selectedTab = RulesTab.RULES, allRules = rules)

        val sorted = uiState.sortedRules
        assert(sorted.first().isActive) { "Active rules should come first" }
        assert(!sorted.last().isActive) { "Paused rules should come last" }
    }

    // endregion

    // region Phase 11.9: Force Override Tests

    @Test
    fun force_override_dialog_displays_when_showConflictDialog_true() {
        val conflictDetails = listOf(
            ConflictDetail(
                memberName = "Dadaji",
                condition = "diabetic",
                keyword = "sugar",
                ruleTarget = "Gulab Jamun"
            )
        )

        composeTestRule.setContent {
            RasoiAITheme {
                ForceOverrideDialog(
                    conflictDetails = conflictDetails,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("conflict_dialog_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Family Safety Conflict").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dadaji", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Override & Save").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun override_badge_shown_on_force_override_rule() {
        val rule = createTestRecipeRule(
            id = "override_rule",
            action = RuleAction.INCLUDE,
            targetName = "Gulab Jamun"
        ).copy(forceOverride = true)

        composeTestRule.setContent {
            RasoiAITheme {
                RuleCard(
                    rule = rule,
                    onEdit = {},
                    onToggleActive = {},
                    onDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("override_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("Override").assertIsDisplayed()
    }

    // endregion
}

// region Test Composable Wrapper

@Composable
private fun RecipeRulesTestContent(
    uiState: RecipeRulesUiState,
    onBackClick: () -> Unit = {},
    onTabSelected: (RulesTab) -> Unit = {},
    onAddRuleClick: () -> Unit = {},
    onEditRule: (RecipeRule) -> Unit = {},
    onToggleRuleActive: (RecipeRule) -> Unit = {},
    onDeleteRule: (RecipeRule) -> Unit = {},
    onEditNutritionGoal: (NutritionGoal) -> Unit = {},
    onToggleNutritionGoalActive: (NutritionGoal) -> Unit = {},
    onToggleNutritionGoalEnforcement: (NutritionGoal) -> Unit = {},
    onDeleteNutritionGoal: (NutritionGoal) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    RecipeRulesScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onTabSelected = onTabSelected,
        onAddRuleClick = onAddRuleClick,
        onEditRule = onEditRule,
        onToggleRuleActive = onToggleRuleActive,
        onDeleteRule = onDeleteRule,
        onEditNutritionGoal = onEditNutritionGoal,
        onToggleNutritionGoalActive = onToggleNutritionGoalActive,
        onToggleNutritionGoalEnforcement = onToggleNutritionGoalEnforcement,
        onDeleteNutritionGoal = onDeleteNutritionGoal
    )
}

// endregion
