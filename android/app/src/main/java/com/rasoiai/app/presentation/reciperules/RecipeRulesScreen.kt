package com.rasoiai.app.presentation.reciperules

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.rasoiai.app.presentation.common.TestTags
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.presentation.common.components.ScopeToggle
import com.rasoiai.app.presentation.reciperules.components.AddNutritionGoalSheet
import com.rasoiai.app.presentation.reciperules.components.AddRuleBottomSheet
import com.rasoiai.app.presentation.reciperules.components.AddRuleButton
import com.rasoiai.app.presentation.reciperules.components.DeleteConfirmationDialog
import com.rasoiai.app.presentation.reciperules.components.ForceOverrideDialog
import com.rasoiai.app.presentation.reciperules.components.EmptyRulesState
import com.rasoiai.app.presentation.reciperules.components.NutritionGoalCard
import com.rasoiai.app.presentation.reciperules.components.RuleCard
import com.rasoiai.app.presentation.reciperules.components.RulesTabBar
import com.rasoiai.app.presentation.reciperules.components.SectionHeader
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.DataScope
import com.rasoiai.domain.model.FoodCategory
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.NutritionGoal
import com.rasoiai.domain.model.RecipeRule
import com.rasoiai.domain.model.RuleAction
import com.rasoiai.domain.model.RuleEnforcement
import com.rasoiai.domain.model.RuleFrequency
import com.rasoiai.domain.model.RuleType

@Composable
fun RecipeRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecipeRulesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                RecipeRulesNavigationEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    // Show error in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    RecipeRulesScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = viewModel::navigateBack,
        onTabSelected = viewModel::selectTab,
        onScopeChanged = viewModel::setScope,
        onAddRuleClick = viewModel::showAddRuleSheet,
        onEditRule = viewModel::showEditRuleSheet,
        onToggleRuleActive = viewModel::toggleRuleActive,
        onDeleteRule = viewModel::showDeleteConfirmation,
        onEditNutritionGoal = viewModel::showEditNutritionGoalSheet,
        onToggleNutritionGoalActive = viewModel::toggleNutritionGoalActive,
        onToggleNutritionGoalEnforcement = viewModel::toggleNutritionGoalEnforcement,
        onDeleteNutritionGoal = viewModel::showDeleteConfirmation
    )

    // Add Rule Bottom Sheet
    if (uiState.showAddRuleSheet) {
        AddRuleBottomSheet(
            uiState = uiState,
            onDismiss = viewModel::dismissAddRuleSheet,
            onActionChange = viewModel::updateAction,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSelectSearchResult = viewModel::selectSearchResult,
            onClearTarget = viewModel::clearSelectedTarget,
            onFrequencyTypeChange = viewModel::updateFrequencyType,
            onFrequencyCountChange = viewModel::updateFrequencyCount,
            onToggleDay = viewModel::toggleDay,
            onMealSlotModeChange = viewModel::updateMealSlotMode,
            onToggleMealSlot = viewModel::toggleMealSlot,
            onEnforcementChange = viewModel::updateEnforcement,
            onSave = viewModel::saveRule
        )
    }

    // Add Nutrition Goal Bottom Sheet
    if (uiState.showAddNutritionGoalSheet) {
        AddNutritionGoalSheet(
            uiState = uiState,
            onDismiss = viewModel::dismissAddNutritionGoalSheet,
            onFoodCategoryChange = viewModel::updateFoodCategory,
            onWeeklyTargetChange = viewModel::updateWeeklyTarget,
            onSave = viewModel::saveNutritionGoal
        )
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteConfirmation) {
        DeleteConfirmationDialog(
            ruleName = uiState.ruleToDelete?.targetName
                ?: uiState.goalToDelete?.foodCategory?.displayName
                ?: "",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDeleteConfirmation
        )
    }

    // Force Override Conflict Dialog
    if (uiState.showConflictDialog) {
        ForceOverrideDialog(
            conflictDetails = uiState.pendingConflictDetails,
            onConfirm = viewModel::confirmForceOverride,
            onDismiss = viewModel::dismissConflictDialog
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecipeRulesScreenContent(
    uiState: RecipeRulesUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onTabSelected: (RulesTab) -> Unit,
    onScopeChanged: (DataScope) -> Unit = {},
    onAddRuleClick: () -> Unit,
    onEditRule: (RecipeRule) -> Unit,
    onToggleRuleActive: (RecipeRule) -> Unit,
    onDeleteRule: (RecipeRule) -> Unit,
    onEditNutritionGoal: (NutritionGoal) -> Unit,
    onToggleNutritionGoalActive: (NutritionGoal) -> Unit,
    onToggleNutritionGoalEnforcement: (NutritionGoal) -> Unit,
    onDeleteNutritionGoal: (NutritionGoal) -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.RECIPE_RULES_SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recipe Rules",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Scope Toggle
                    if (uiState.hasHousehold) {
                        ScopeToggle(
                            selectedScope = uiState.selectedScope,
                            onScopeChange = onScopeChanged
                        )
                    }

                    // Tab Bar
                    RulesTabBar(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = onTabSelected,
                        modifier = Modifier.padding(horizontal = spacing.md)
                    )

                    Spacer(modifier = Modifier.height(spacing.md))

                    // Content based on selected tab
                    if (uiState.selectedTab == RulesTab.NUTRITION) {
                        NutritionGoalsContent(
                            goals = uiState.nutritionGoals,
                            onEditGoal = onEditNutritionGoal,
                            onToggleActive = onToggleNutritionGoalActive,
                            onToggleEnforcement = onToggleNutritionGoalEnforcement,
                            onDeleteGoal = onDeleteNutritionGoal,
                            onAddGoal = onAddRuleClick,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        RulesContent(
                            rules = uiState.sortedRules,
                            rulesCount = uiState.rulesCount,
                            onEditRule = onEditRule,
                            onToggleActive = onToggleRuleActive,
                            onDeleteRule = onDeleteRule,
                            onAddRule = onAddRuleClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RulesContent(
    rules: List<RecipeRule>,
    rulesCount: Int,
    onEditRule: (RecipeRule) -> Unit,
    onToggleActive: (RecipeRule) -> Unit,
    onDeleteRule: (RecipeRule) -> Unit,
    onAddRule: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (rules.isEmpty()) {
        EmptyRulesState(
            tabType = RulesTab.RULES,
            onAddClick = onAddRule,
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = spacing.md)
        ) {
            item {
                SectionHeader(title = "MY RULES ($rulesCount)")
                Spacer(modifier = Modifier.height(spacing.sm))
            }

            items(rules, key = { it.id }) { rule ->
                RuleCard(
                    rule = rule,
                    onEdit = { onEditRule(rule) },
                    onToggleActive = { onToggleActive(rule) },
                    onDelete = { onDeleteRule(rule) }
                )
                Spacer(modifier = Modifier.height(spacing.sm))
            }

            item {
                Spacer(modifier = Modifier.height(spacing.md))
                AddRuleButton(
                    text = "+ ADD RULE",
                    onClick = onAddRule
                )
                Spacer(modifier = Modifier.height(spacing.xl))
            }
        }
    }
}

@Composable
private fun NutritionGoalsContent(
    goals: List<NutritionGoal>,
    onEditGoal: (NutritionGoal) -> Unit,
    onToggleActive: (NutritionGoal) -> Unit,
    onToggleEnforcement: (NutritionGoal) -> Unit,
    onDeleteGoal: (NutritionGoal) -> Unit,
    onAddGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (goals.isEmpty()) {
        EmptyRulesState(
            tabType = RulesTab.NUTRITION,
            onAddClick = onAddGoal,
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = spacing.md)
        ) {
            item {
                SectionHeader(title = "MY NUTRITION GOALS (${goals.size})")
                Spacer(modifier = Modifier.height(spacing.sm))
            }

            items(goals, key = { it.id }) { goal ->
                NutritionGoalCard(
                    goal = goal,
                    onEdit = { onEditGoal(goal) },
                    onToggleActive = { onToggleActive(goal) },
                    onToggleEnforcement = { onToggleEnforcement(goal) },
                    onDelete = { onDeleteGoal(goal) }
                )
                Spacer(modifier = Modifier.height(spacing.sm))
            }

            item {
                Spacer(modifier = Modifier.height(spacing.md))
                AddRuleButton(
                    text = "+ ADD NUTRITION GOAL",
                    onClick = onAddGoal
                )
                Spacer(modifier = Modifier.height(spacing.xl))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDFAF4)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1C1B1F
)
@Composable
private fun RecipeRulesScreenPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RecipeRulesScreenContent(
                uiState = RecipeRulesUiState(
                    isLoading = false,
                    selectedTab = RulesTab.RULES,
                    allRules = listOf(
                        RecipeRule(
                            id = "1",
                            type = RuleType.RECIPE,
                            action = RuleAction.INCLUDE,
                            targetId = "r1",
                            targetName = "Paneer Tikka",
                            frequency = RuleFrequency.timesPerWeek(1),
                            enforcement = RuleEnforcement.REQUIRED,
                            mealSlots = listOf(MealType.BREAKFAST),
                            isActive = true
                        ),
                        RecipeRule(
                            id = "2",
                            type = RuleType.INGREDIENT,
                            action = RuleAction.INCLUDE,
                            targetId = "i1",
                            targetName = "Green Tea",
                            frequency = RuleFrequency.DAILY,
                            enforcement = RuleEnforcement.PREFERRED,
                            isActive = true
                        ),
                        RecipeRule(
                            id = "3",
                            type = RuleType.INGREDIENT,
                            action = RuleAction.EXCLUDE,
                            targetId = "i2",
                            targetName = "Karela",
                            frequency = RuleFrequency.DAILY,
                            enforcement = RuleEnforcement.REQUIRED,
                            isActive = true
                        ),
                        RecipeRule(
                            id = "4",
                            type = RuleType.RECIPE,
                            action = RuleAction.INCLUDE,
                            targetId = "r2",
                            targetName = "Dal Tadka",
                            frequency = RuleFrequency.timesPerWeek(3),
                            enforcement = RuleEnforcement.PREFERRED,
                            mealSlots = listOf(MealType.LUNCH, MealType.DINNER),
                            isActive = true
                        ),
                        RecipeRule(
                            id = "5",
                            type = RuleType.INGREDIENT,
                            action = RuleAction.INCLUDE,
                            targetId = "i3",
                            targetName = "Eggs",
                            frequency = RuleFrequency.DAILY,
                            enforcement = RuleEnforcement.PREFERRED,
                            mealSlots = listOf(MealType.BREAKFAST),
                            isActive = false
                        )
                    ),
                    nutritionGoals = listOf(
                        NutritionGoal(
                            id = "g1",
                            foodCategory = FoodCategory.GREEN_LEAFY,
                            weeklyTarget = 7,
                            currentProgress = 4,
                            isActive = true
                        )
                    )
                ),
                snackbarHostState = SnackbarHostState(),
                onBackClick = {},
                onTabSelected = {},
                onAddRuleClick = {},
                onEditRule = {},
                onToggleRuleActive = {},
                onDeleteRule = {},
                onEditNutritionGoal = {},
                onToggleNutritionGoalActive = {},
                onToggleNutritionGoalEnforcement = {},
                onDeleteNutritionGoal = {}
            )
        }
    }
}
