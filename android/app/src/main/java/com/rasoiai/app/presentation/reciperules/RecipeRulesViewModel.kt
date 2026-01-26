package com.rasoiai.app.presentation.reciperules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * Tab types for the Recipe Rules screen.
 */
enum class RulesTab(val title: String, val emoji: String) {
    RECIPE("Recipe", "📖"),
    INGREDIENT("Ingredient", "🥕"),
    MEAL_SLOT("Meal-Slot", "🍽️"),
    NUTRITION("Nutrition", "🥗");

    companion object {
        fun fromRuleType(ruleType: RuleType): RulesTab = when (ruleType) {
            RuleType.RECIPE -> RECIPE
            RuleType.INGREDIENT -> INGREDIENT
            RuleType.MEAL_SLOT -> MEAL_SLOT
            RuleType.NUTRITION -> NUTRITION
        }
    }

    fun toRuleType(): RuleType = when (this) {
        RECIPE -> RuleType.RECIPE
        INGREDIENT -> RuleType.INGREDIENT
        MEAL_SLOT -> RuleType.MEAL_SLOT
        NUTRITION -> RuleType.NUTRITION
    }
}

/**
 * UI state for the Recipe Rules screen.
 */
data class RecipeRulesUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedTab: RulesTab = RulesTab.RECIPE,
    val recipeRules: List<RecipeRule> = emptyList(),
    val ingredientRules: List<RecipeRule> = emptyList(),
    val mealSlotRules: List<RecipeRule> = emptyList(),
    val nutritionGoals: List<NutritionGoal> = emptyList(),

    // Add/Edit Rule state
    val showAddRuleSheet: Boolean = false,
    val showAddNutritionGoalSheet: Boolean = false,
    val editingRule: RecipeRule? = null,
    val editingNutritionGoal: NutritionGoal? = null,

    // Form fields for adding/editing rules
    val selectedAction: RuleAction = RuleAction.INCLUDE,
    val searchQuery: String = "",
    val selectedTargetId: String? = null,
    val selectedTargetName: String = "",
    val selectedFrequencyType: FrequencyType = FrequencyType.TIMES_PER_WEEK,
    val selectedFrequencyCount: Int = 1,
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val selectedMealSlot: MealType? = null,
    val selectedEnforcement: RuleEnforcement = RuleEnforcement.REQUIRED,

    // Form fields for nutrition goals
    val selectedFoodCategory: FoodCategory? = null,
    val weeklyTarget: Int = 3,

    // Search results
    val recipeSearchResults: List<Recipe> = emptyList(),
    val ingredientSearchResults: List<String> = emptyList(),
    val popularRecipes: List<Recipe> = emptyList(),
    val popularIngredients: List<String> = emptyList(),
    val availableFoodCategories: List<FoodCategory> = emptyList(),

    // Delete confirmation
    val showDeleteConfirmation: Boolean = false,
    val ruleToDelete: RecipeRule? = null,
    val goalToDelete: NutritionGoal? = null
) {
    val rulesForCurrentTab: List<RecipeRule>
        get() = when (selectedTab) {
            RulesTab.RECIPE -> recipeRules
            RulesTab.INGREDIENT -> ingredientRules
            RulesTab.MEAL_SLOT -> mealSlotRules
            RulesTab.NUTRITION -> emptyList()
        }

    val currentTabCount: Int
        get() = when (selectedTab) {
            RulesTab.RECIPE -> recipeRules.size
            RulesTab.INGREDIENT -> ingredientRules.size
            RulesTab.MEAL_SLOT -> mealSlotRules.size
            RulesTab.NUTRITION -> nutritionGoals.size
        }

    val isEditing: Boolean
        get() = editingRule != null || editingNutritionGoal != null

    val canSaveRule: Boolean
        get() = selectedTargetName.isNotBlank() && (
            selectedFrequencyType != FrequencyType.SPECIFIC_DAYS || selectedDays.isNotEmpty()
        )

    val canSaveNutritionGoal: Boolean
        get() = selectedFoodCategory != null && weeklyTarget > 0
}

/**
 * Frequency type for UI selection.
 */
enum class FrequencyType(val displayName: String) {
    DAILY("Every day"),
    TIMES_PER_WEEK("X times per week"),
    SPECIFIC_DAYS("Specific days"),
    NEVER("Never")
}

/**
 * Navigation events from Recipe Rules screen.
 */
sealed class RecipeRulesNavigationEvent {
    data object NavigateBack : RecipeRulesNavigationEvent()
}

@HiltViewModel
class RecipeRulesViewModel @Inject constructor(
    private val repository: RecipeRulesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeRulesUiState())
    val uiState: StateFlow<RecipeRulesUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<RecipeRulesNavigationEvent?>(null)
    val navigationEvent: StateFlow<RecipeRulesNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load popular recipes and ingredients for suggestions
            launch {
                repository.getPopularRecipes().collect { recipes ->
                    _uiState.update { it.copy(popularRecipes = recipes) }
                }
            }

            launch {
                repository.getPopularIngredients().collect { ingredients ->
                    _uiState.update { it.copy(popularIngredients = ingredients) }
                }
            }

            launch {
                repository.getAvailableFoodCategories().collect { categories ->
                    _uiState.update { it.copy(availableFoodCategories = categories) }
                }
            }

            // Load rules by type
            launch {
                repository.getRulesByType(RuleType.RECIPE).collect { rules ->
                    _uiState.update { it.copy(recipeRules = rules) }
                }
            }

            launch {
                repository.getRulesByType(RuleType.INGREDIENT).collect { rules ->
                    _uiState.update { it.copy(ingredientRules = rules) }
                }
            }

            launch {
                repository.getRulesByType(RuleType.MEAL_SLOT).collect { rules ->
                    _uiState.update { it.copy(mealSlotRules = rules) }
                }
            }

            launch {
                repository.getAllNutritionGoals().collect { goals ->
                    _uiState.update { it.copy(nutritionGoals = goals, isLoading = false) }
                }
            }
        }
    }

    // region Tab Navigation

    fun selectTab(tab: RulesTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // endregion

    // region Add/Edit Rule Sheet

    fun showAddRuleSheet() {
        val currentTab = _uiState.value.selectedTab
        if (currentTab == RulesTab.NUTRITION) {
            showAddNutritionGoalSheet()
        } else {
            _uiState.update {
                it.copy(
                    showAddRuleSheet = true,
                    editingRule = null,
                    selectedAction = RuleAction.INCLUDE,
                    searchQuery = "",
                    selectedTargetId = null,
                    selectedTargetName = "",
                    selectedFrequencyType = FrequencyType.TIMES_PER_WEEK,
                    selectedFrequencyCount = 1,
                    selectedDays = emptySet(),
                    selectedMealSlot = if (currentTab == RulesTab.MEAL_SLOT) MealType.BREAKFAST else null,
                    selectedEnforcement = RuleEnforcement.REQUIRED,
                    recipeSearchResults = emptyList(),
                    ingredientSearchResults = emptyList()
                )
            }
        }
    }

    fun showEditRuleSheet(rule: RecipeRule) {
        val frequencyType = when {
            rule.frequency.type == com.rasoiai.domain.model.FrequencyType.DAILY -> FrequencyType.DAILY
            rule.frequency.type == com.rasoiai.domain.model.FrequencyType.TIMES_PER_WEEK -> FrequencyType.TIMES_PER_WEEK
            rule.frequency.type == com.rasoiai.domain.model.FrequencyType.SPECIFIC_DAYS -> FrequencyType.SPECIFIC_DAYS
            rule.frequency.type == com.rasoiai.domain.model.FrequencyType.NEVER -> FrequencyType.NEVER
            else -> FrequencyType.TIMES_PER_WEEK
        }

        _uiState.update {
            it.copy(
                showAddRuleSheet = true,
                editingRule = rule,
                selectedAction = rule.action,
                searchQuery = "",
                selectedTargetId = rule.targetId,
                selectedTargetName = rule.targetName,
                selectedFrequencyType = frequencyType,
                selectedFrequencyCount = rule.frequency.count ?: 1,
                selectedDays = rule.frequency.specificDays?.toSet() ?: emptySet(),
                selectedMealSlot = rule.mealSlot,
                selectedEnforcement = rule.enforcement
            )
        }
    }

    fun dismissAddRuleSheet() {
        _uiState.update {
            it.copy(
                showAddRuleSheet = false,
                editingRule = null
            )
        }
    }

    // endregion

    // region Add/Edit Nutrition Goal Sheet

    fun showAddNutritionGoalSheet() {
        _uiState.update {
            it.copy(
                showAddNutritionGoalSheet = true,
                editingNutritionGoal = null,
                selectedFoodCategory = it.availableFoodCategories.firstOrNull(),
                weeklyTarget = 3
            )
        }
    }

    fun showEditNutritionGoalSheet(goal: NutritionGoal) {
        _uiState.update {
            it.copy(
                showAddNutritionGoalSheet = true,
                editingNutritionGoal = goal,
                selectedFoodCategory = goal.foodCategory,
                weeklyTarget = goal.weeklyTarget
            )
        }
    }

    fun dismissAddNutritionGoalSheet() {
        _uiState.update {
            it.copy(
                showAddNutritionGoalSheet = false,
                editingNutritionGoal = null
            )
        }
    }

    // endregion

    // region Form Updates

    fun updateAction(action: RuleAction) {
        _uiState.update { it.copy(selectedAction = action) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        if (query.isNotBlank()) {
            viewModelScope.launch {
                val currentTab = _uiState.value.selectedTab
                if (currentTab == RulesTab.RECIPE || currentTab == RulesTab.MEAL_SLOT) {
                    val results = repository.searchRecipes(query).first()
                    _uiState.update { it.copy(recipeSearchResults = results) }
                } else if (currentTab == RulesTab.INGREDIENT) {
                    val results = repository.searchIngredients(query).first()
                    _uiState.update { it.copy(ingredientSearchResults = results) }
                }
            }
        } else {
            _uiState.update {
                it.copy(recipeSearchResults = emptyList(), ingredientSearchResults = emptyList())
            }
        }
    }

    fun selectRecipe(recipe: Recipe) {
        _uiState.update {
            it.copy(
                selectedTargetId = recipe.id,
                selectedTargetName = recipe.name,
                searchQuery = recipe.name,
                recipeSearchResults = emptyList()
            )
        }
    }

    fun selectIngredient(ingredient: String) {
        _uiState.update {
            it.copy(
                selectedTargetId = "ingredient-${ingredient.lowercase().replace(" ", "-")}",
                selectedTargetName = ingredient,
                searchQuery = ingredient,
                ingredientSearchResults = emptyList()
            )
        }
    }

    fun updateFrequencyType(type: FrequencyType) {
        _uiState.update { it.copy(selectedFrequencyType = type) }
    }

    fun updateFrequencyCount(count: Int) {
        _uiState.update { it.copy(selectedFrequencyCount = count.coerceIn(1, 7)) }
    }

    fun toggleDay(day: DayOfWeek) {
        _uiState.update {
            val newDays = if (day in it.selectedDays) {
                it.selectedDays - day
            } else {
                it.selectedDays + day
            }
            it.copy(selectedDays = newDays)
        }
    }

    fun updateMealSlot(mealType: MealType?) {
        _uiState.update { it.copy(selectedMealSlot = mealType) }
    }

    fun updateEnforcement(enforcement: RuleEnforcement) {
        _uiState.update { it.copy(selectedEnforcement = enforcement) }
    }

    fun updateFoodCategory(category: FoodCategory) {
        _uiState.update { it.copy(selectedFoodCategory = category) }
    }

    fun updateWeeklyTarget(target: Int) {
        _uiState.update { it.copy(weeklyTarget = target.coerceIn(1, 21)) }
    }

    // endregion

    // region Save Operations

    fun saveRule() {
        val state = _uiState.value
        if (!state.canSaveRule) return

        viewModelScope.launch {
            val frequency = when (state.selectedFrequencyType) {
                FrequencyType.DAILY -> RuleFrequency.DAILY
                FrequencyType.TIMES_PER_WEEK -> RuleFrequency.timesPerWeek(state.selectedFrequencyCount)
                FrequencyType.SPECIFIC_DAYS -> RuleFrequency.specificDays(state.selectedDays.toList())
                FrequencyType.NEVER -> RuleFrequency.NEVER
            }

            val rule = RecipeRule(
                id = state.editingRule?.id ?: "",
                type = state.selectedTab.toRuleType(),
                action = state.selectedAction,
                targetId = state.selectedTargetId ?: "",
                targetName = state.selectedTargetName,
                frequency = frequency,
                enforcement = state.selectedEnforcement,
                mealSlot = state.selectedMealSlot,
                isActive = state.editingRule?.isActive ?: true
            )

            val result = if (state.editingRule != null) {
                repository.updateRule(rule)
            } else {
                repository.createRule(rule).map { Unit }
            }

            result.onSuccess {
                dismissAddRuleSheet()
                Timber.i("Rule saved: ${rule.targetName}")
            }.onFailure { e ->
                Timber.e(e, "Failed to save rule")
                _uiState.update { it.copy(errorMessage = "Failed to save rule") }
            }
        }
    }

    fun saveNutritionGoal() {
        val state = _uiState.value
        if (!state.canSaveNutritionGoal || state.selectedFoodCategory == null) return

        viewModelScope.launch {
            val goal = NutritionGoal(
                id = state.editingNutritionGoal?.id ?: "",
                foodCategory = state.selectedFoodCategory,
                weeklyTarget = state.weeklyTarget,
                currentProgress = state.editingNutritionGoal?.currentProgress ?: 0,
                isActive = state.editingNutritionGoal?.isActive ?: true
            )

            val result = if (state.editingNutritionGoal != null) {
                repository.updateNutritionGoal(goal)
            } else {
                repository.createNutritionGoal(goal).map { Unit }
            }

            result.onSuccess {
                dismissAddNutritionGoalSheet()
                Timber.i("Nutrition goal saved: ${goal.foodCategory.displayName}")
            }.onFailure { e ->
                Timber.e(e, "Failed to save nutrition goal")
                _uiState.update { it.copy(errorMessage = "Failed to save nutrition goal") }
            }
        }
    }

    // endregion

    // region Toggle Active

    fun toggleRuleActive(rule: RecipeRule) {
        viewModelScope.launch {
            repository.toggleRuleActive(rule.id, !rule.isActive)
                .onFailure { e ->
                    Timber.e(e, "Failed to toggle rule active state")
                    _uiState.update { it.copy(errorMessage = "Failed to update rule") }
                }
        }
    }

    fun toggleNutritionGoalActive(goal: NutritionGoal) {
        viewModelScope.launch {
            repository.toggleNutritionGoalActive(goal.id, !goal.isActive)
                .onFailure { e ->
                    Timber.e(e, "Failed to toggle nutrition goal active state")
                    _uiState.update { it.copy(errorMessage = "Failed to update goal") }
                }
        }
    }

    fun toggleNutritionGoalEnforcement(goal: NutritionGoal) {
        viewModelScope.launch {
            val newEnforcement = if (goal.enforcement == RuleEnforcement.REQUIRED) {
                RuleEnforcement.PREFERRED
            } else {
                RuleEnforcement.REQUIRED
            }

            val updatedGoal = goal.copy(enforcement = newEnforcement)
            repository.updateNutritionGoal(updatedGoal)
                .onSuccess {
                    Timber.i("Nutrition goal enforcement toggled: ${goal.foodCategory.displayName} -> ${newEnforcement.displayName}")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to toggle nutrition goal enforcement")
                    _uiState.update { it.copy(errorMessage = "Failed to update goal enforcement") }
                }
        }
    }

    // endregion

    // region Delete Operations

    fun showDeleteConfirmation(rule: RecipeRule) {
        _uiState.update {
            it.copy(
                showDeleteConfirmation = true,
                ruleToDelete = rule,
                goalToDelete = null
            )
        }
    }

    fun showDeleteConfirmation(goal: NutritionGoal) {
        _uiState.update {
            it.copy(
                showDeleteConfirmation = true,
                ruleToDelete = null,
                goalToDelete = goal
            )
        }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update {
            it.copy(
                showDeleteConfirmation = false,
                ruleToDelete = null,
                goalToDelete = null
            )
        }
    }

    fun confirmDelete() {
        val state = _uiState.value

        viewModelScope.launch {
            state.ruleToDelete?.let { rule ->
                repository.deleteRule(rule.id)
                    .onSuccess {
                        Timber.i("Rule deleted: ${rule.targetName}")
                    }
                    .onFailure { e ->
                        Timber.e(e, "Failed to delete rule")
                        _uiState.update { it.copy(errorMessage = "Failed to delete rule") }
                    }
            }

            state.goalToDelete?.let { goal ->
                repository.deleteNutritionGoal(goal.id)
                    .onSuccess {
                        Timber.i("Nutrition goal deleted: ${goal.foodCategory.displayName}")
                    }
                    .onFailure { e ->
                        Timber.e(e, "Failed to delete nutrition goal")
                        _uiState.update { it.copy(errorMessage = "Failed to delete goal") }
                    }
            }

            dismissDeleteConfirmation()
        }
    }

    // endregion

    // region Navigation

    fun navigateBack() {
        _navigationEvent.value = RecipeRulesNavigationEvent.NavigateBack
    }

    fun onNavigationHandled() {
        _navigationEvent.value = null
    }

    // endregion

    // region Error Handling

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion
}
