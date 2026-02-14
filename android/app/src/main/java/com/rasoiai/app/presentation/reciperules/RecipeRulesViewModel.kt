package com.rasoiai.app.presentation.reciperules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.domain.model.FoodCategory
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.NutritionGoal
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.model.RecipeRule
import com.rasoiai.domain.model.RuleAction
import com.rasoiai.domain.model.RuleEnforcement
import com.rasoiai.domain.model.RuleFrequency
import com.rasoiai.domain.model.RuleType
import com.rasoiai.domain.model.UserPreferences
import com.rasoiai.domain.repository.RecipeRulesRepository
import com.rasoiai.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * Tab types for the Recipe Rules screen (consolidated from 4 to 2 tabs).
 */
enum class RulesTab(val title: String, val emoji: String) {
    RULES("Rules", "\uD83C\uDF7D\uFE0F"),
    NUTRITION("Nutrition", "\uD83E\uDD57");
}

/**
 * Represents a search result item - either a recipe or an ingredient.
 */
sealed class SearchResultItem {
    data class RecipeItem(val recipe: Recipe) : SearchResultItem()
    data class IngredientItem(val name: String) : SearchResultItem()

    val displayName: String
        get() = when (this) {
            is RecipeItem -> recipe.name
            is IngredientItem -> name
        }

    val isRecipe: Boolean
        get() = this is RecipeItem

    val emoji: String
        get() = when (this) {
            is RecipeItem -> "\uD83D\uDCD6"
            is IngredientItem -> "\uD83E\uDD55"
        }
}

/**
 * Meal slot selection mode for the bottom sheet.
 */
enum class MealSlotMode {
    ANY,
    SPECIFIC
}

/**
 * UI state for the Recipe Rules screen.
 */
data class RecipeRulesUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedTab: RulesTab = RulesTab.RULES,
    val allRules: List<RecipeRule> = emptyList(),
    val nutritionGoals: List<NutritionGoal> = emptyList(),

    // Add/Edit Rule state
    val showAddRuleSheet: Boolean = false,
    val showAddNutritionGoalSheet: Boolean = false,
    val editingRule: RecipeRule? = null,
    val editingNutritionGoal: NutritionGoal? = null,

    // Form fields for adding/editing rules
    val selectedAction: RuleAction = RuleAction.INCLUDE,
    val searchQuery: String = "",
    val selectedTarget: SearchResultItem? = null,
    val selectedFrequencyType: FrequencyType = FrequencyType.TIMES_PER_WEEK,
    val selectedFrequencyCount: Int = 1,
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val mealSlotMode: MealSlotMode = MealSlotMode.ANY,
    val selectedMealSlots: Set<MealType> = emptySet(),
    val selectedEnforcement: RuleEnforcement = RuleEnforcement.REQUIRED,

    // Form fields for nutrition goals
    val selectedFoodCategory: FoodCategory? = null,
    val weeklyTarget: Int = 3,

    // Search results (mixed)
    val searchResults: List<SearchResultItem> = emptyList(),
    val popularItems: List<SearchResultItem> = emptyList(),
    val availableFoodCategories: List<FoodCategory> = emptyList(),

    // Delete confirmation
    val showDeleteConfirmation: Boolean = false,
    val ruleToDelete: RecipeRule? = null,
    val goalToDelete: NutritionGoal? = null,

    // Diet conflict warning (Issue #42)
    val conflictWarning: String? = null,
    val hasConflict: Boolean = false
) {
    /**
     * Rules sorted: active first (newest first), then paused (newest first).
     */
    val sortedRules: List<RecipeRule>
        get() {
            val active = allRules.filter { it.isActive }.sortedByDescending { it.createdAt }
            val paused = allRules.filter { !it.isActive }.sortedByDescending { it.createdAt }
            return active + paused
        }

    val rulesCount: Int
        get() = allRules.size

    val isEditing: Boolean
        get() = editingRule != null || editingNutritionGoal != null

    val canSaveRule: Boolean
        get() = selectedTarget != null && (
            selectedFrequencyType != FrequencyType.SPECIFIC_DAYS || selectedDays.isNotEmpty()
        ) && (
            mealSlotMode == MealSlotMode.ANY || selectedMealSlots.isNotEmpty()
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
    private val repository: RecipeRulesRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeRulesUiState())
    val uiState: StateFlow<RecipeRulesUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<RecipeRulesNavigationEvent>()
    val navigationEvent: Flow<RecipeRulesNavigationEvent> = _navigationEvent.receiveAsFlow()

    // Cached user preferences for diet conflict checking
    private var userPreferences: UserPreferences? = null

    init {
        loadData()
        loadUserPreferences()
    }

    private fun loadUserPreferences() {
        viewModelScope.launch {
            settingsRepository.getCurrentUser().collect { user ->
                userPreferences = user?.preferences
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load popular recipes and ingredients for suggestions
            launch {
                repository.getPopularRecipes().collect { recipes ->
                    val recipeItems = recipes.map { SearchResultItem.RecipeItem(it) }
                    repository.getPopularIngredients().first().let { ingredients ->
                        val ingredientItems = ingredients.map { SearchResultItem.IngredientItem(it) }
                        // Interleave recipe and ingredient suggestions
                        val mixed = mutableListOf<SearchResultItem>()
                        val maxSize = maxOf(recipeItems.size, ingredientItems.size)
                        for (i in 0 until maxSize) {
                            if (i < recipeItems.size) mixed.add(recipeItems[i])
                            if (i < ingredientItems.size) mixed.add(ingredientItems[i])
                        }
                        _uiState.update { it.copy(popularItems = mixed.take(8)) }
                    }
                }
            }

            launch {
                repository.getAvailableFoodCategories().collect { categories ->
                    _uiState.update { it.copy(availableFoodCategories = categories) }
                }
            }

            // Load all rules (unified)
            launch {
                repository.getAllRules().collect { rules ->
                    _uiState.update { it.copy(allRules = rules) }
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
                    selectedTarget = null,
                    selectedFrequencyType = FrequencyType.TIMES_PER_WEEK,
                    selectedFrequencyCount = 1,
                    selectedDays = emptySet(),
                    mealSlotMode = MealSlotMode.ANY,
                    selectedMealSlots = emptySet(),
                    selectedEnforcement = RuleEnforcement.REQUIRED,
                    searchResults = emptyList(),
                    conflictWarning = null,
                    hasConflict = false
                )
            }
        }
    }

    fun showEditRuleSheet(rule: RecipeRule) {
        val frequencyType = when (rule.frequency.type) {
            com.rasoiai.domain.model.FrequencyType.DAILY -> FrequencyType.DAILY
            com.rasoiai.domain.model.FrequencyType.TIMES_PER_WEEK -> FrequencyType.TIMES_PER_WEEK
            com.rasoiai.domain.model.FrequencyType.SPECIFIC_DAYS -> FrequencyType.SPECIFIC_DAYS
            com.rasoiai.domain.model.FrequencyType.NEVER -> FrequencyType.NEVER
        }

        val target: SearchResultItem = if (rule.type == RuleType.RECIPE || rule.type == RuleType.MEAL_SLOT) {
            SearchResultItem.RecipeItem(
                Recipe(
                    id = rule.targetId,
                    name = rule.targetName,
                    description = "",
                    imageUrl = null,
                    prepTimeMinutes = 0,
                    cookTimeMinutes = 0,
                    servings = 0,
                    difficulty = com.rasoiai.domain.model.Difficulty.MEDIUM,
                    cuisineType = com.rasoiai.domain.model.CuisineType.NORTH,
                    mealTypes = emptyList(),
                    dietaryTags = emptyList(),
                    ingredients = emptyList(),
                    instructions = emptyList(),
                    nutrition = null
                )
            )
        } else {
            SearchResultItem.IngredientItem(rule.targetName)
        }

        _uiState.update {
            it.copy(
                showAddRuleSheet = true,
                editingRule = rule,
                selectedAction = rule.action,
                searchQuery = "",
                selectedTarget = target,
                selectedFrequencyType = frequencyType,
                selectedFrequencyCount = rule.frequency.count ?: 1,
                selectedDays = rule.frequency.specificDays?.toSet() ?: emptySet(),
                mealSlotMode = if (rule.mealSlots.isEmpty()) MealSlotMode.ANY else MealSlotMode.SPECIFIC,
                selectedMealSlots = rule.mealSlots.toSet(),
                selectedEnforcement = rule.enforcement
            )
        }
    }

    fun dismissAddRuleSheet() {
        _uiState.update {
            it.copy(
                showAddRuleSheet = false,
                editingRule = null,
                conflictWarning = null,
                hasConflict = false
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
        val target = _uiState.value.selectedTarget
        val conflict = if (action == RuleAction.INCLUDE && target != null) {
            checkDietConflict(target.displayName)
        } else {
            null
        }

        _uiState.update {
            it.copy(
                selectedAction = action,
                conflictWarning = conflict,
                hasConflict = conflict != null
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        if (query.isNotBlank()) {
            viewModelScope.launch {
                // Search both recipes and ingredients in parallel
                val recipeResults = try {
                    repository.searchRecipes(query).first()
                } catch (e: Exception) {
                    emptyList()
                }
                val ingredientResults = try {
                    repository.searchIngredients(query).first()
                } catch (e: Exception) {
                    emptyList()
                }

                // Mix results: recipes first, then ingredients, max 8 total
                val mixed = mutableListOf<SearchResultItem>()
                val recipeItems = recipeResults.map { SearchResultItem.RecipeItem(it) }
                val ingredientItems = ingredientResults.map { SearchResultItem.IngredientItem(it) }

                // Interleave for variety
                val maxSize = maxOf(recipeItems.size, ingredientItems.size)
                for (i in 0 until maxSize) {
                    if (i < recipeItems.size) mixed.add(recipeItems[i])
                    if (i < ingredientItems.size) mixed.add(ingredientItems[i])
                }

                _uiState.update { it.copy(searchResults = mixed.take(8)) }
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    fun selectSearchResult(item: SearchResultItem) {
        val conflict = if (_uiState.value.selectedAction == RuleAction.INCLUDE) {
            checkDietConflict(item.displayName)
        } else {
            null
        }

        _uiState.update {
            it.copy(
                selectedTarget = item,
                searchQuery = "",
                searchResults = emptyList(),
                conflictWarning = conflict,
                hasConflict = conflict != null
            )
        }
    }

    fun clearSelectedTarget() {
        _uiState.update {
            it.copy(
                selectedTarget = null,
                conflictWarning = null,
                hasConflict = false
            )
        }
    }

    /**
     * Check if the selected ingredient conflicts with user's diet preferences.
     * Issue #42: Diet conflict detection for INCLUDE rules.
     */
    private fun checkDietConflict(ingredient: String): String? {
        val prefs = userPreferences ?: return null

        val nonVegIngredients = setOf(
            "chicken", "mutton", "fish", "prawns", "pork", "beef",
            "lamb", "goat", "crab", "lobster", "shrimp", "meat",
            "keema", "bacon", "ham", "sausage", "salami"
        )
        val eggIngredients = setOf("egg", "eggs", "omelette", "omelet", "anda")

        val ingredientLower = ingredient.lowercase()

        // Check vegetarian diet conflicts
        if (prefs.primaryDiet == PrimaryDiet.VEGETARIAN) {
            if (nonVegIngredients.any { ingredientLower.contains(it) }) {
                return "\"$ingredient\" conflicts with your Vegetarian diet preference."
            }
            if (eggIngredients.any { ingredientLower.contains(it) }) {
                return "\"$ingredient\" conflicts with your Vegetarian diet preference."
            }
        }

        // Check eggetarian diet conflicts (no meat, but eggs allowed)
        if (prefs.primaryDiet == PrimaryDiet.EGGETARIAN) {
            if (nonVegIngredients.any { ingredientLower.contains(it) }) {
                return "\"$ingredient\" conflicts with your Eggetarian diet preference."
            }
        }

        // Check allergy conflicts
        val userAllergies = prefs.dislikedIngredients.map { it.lowercase() }
        if (userAllergies.any { ingredientLower.contains(it) || it.contains(ingredientLower) }) {
            return "\"$ingredient\" is in your disliked/allergy list. Adding it as INCLUDE may cause issues."
        }

        return null
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

    fun updateMealSlotMode(mode: MealSlotMode) {
        _uiState.update {
            it.copy(
                mealSlotMode = mode,
                selectedMealSlots = if (mode == MealSlotMode.ANY) emptySet() else it.selectedMealSlots
            )
        }
    }

    fun toggleMealSlot(mealType: MealType) {
        _uiState.update {
            val newSlots = if (mealType in it.selectedMealSlots) {
                it.selectedMealSlots - mealType
            } else {
                it.selectedMealSlots + mealType
            }
            it.copy(selectedMealSlots = newSlots)
        }
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

    /**
     * Auto-infer the backend type based on the selected target and meal slots.
     * See wireframe "Backend Type Auto-Inference" table.
     */
    private fun inferRuleType(target: SearchResultItem, mealSlots: List<MealType>): RuleType {
        return when {
            target is SearchResultItem.RecipeItem && mealSlots.isNotEmpty() -> RuleType.MEAL_SLOT
            target is SearchResultItem.RecipeItem -> RuleType.RECIPE
            else -> RuleType.INGREDIENT
        }
    }

    fun saveRule() {
        val state = _uiState.value
        if (!state.canSaveRule || state.selectedTarget == null) return

        viewModelScope.launch {
            val frequency = when (state.selectedFrequencyType) {
                FrequencyType.DAILY -> RuleFrequency.DAILY
                FrequencyType.TIMES_PER_WEEK -> RuleFrequency.timesPerWeek(state.selectedFrequencyCount)
                FrequencyType.SPECIFIC_DAYS -> RuleFrequency.specificDays(state.selectedDays.toList())
                FrequencyType.NEVER -> RuleFrequency.NEVER
            }

            val mealSlots = if (state.mealSlotMode == MealSlotMode.SPECIFIC) {
                state.selectedMealSlots.sortedBy { it.ordinal }
            } else {
                emptyList()
            }

            val target = state.selectedTarget
            val targetId = when (target) {
                is SearchResultItem.RecipeItem -> target.recipe.id
                is SearchResultItem.IngredientItem -> "ingredient-${target.name.lowercase().replace(" ", "-")}"
            }

            val rule = RecipeRule(
                id = state.editingRule?.id ?: "",
                type = inferRuleType(target, mealSlots),
                action = state.selectedAction,
                targetId = targetId,
                targetName = target.displayName,
                frequency = frequency,
                enforcement = state.selectedEnforcement,
                mealSlots = mealSlots,
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
                if (e is com.rasoiai.domain.model.DuplicateRuleException) {
                    _uiState.update { it.copy(errorMessage = "A rule for ${rule.targetName} at ${rule.mealSlotsDisplayText} already exists") }
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to save rule") }
                }
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
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleRuleActive(rule.id, !rule.isActive)
                .onFailure { e ->
                    Timber.e(e, "Failed to toggle rule active state")
                    _uiState.update { it.copy(errorMessage = "Failed to update rule") }
                }
        }
    }

    fun toggleNutritionGoalActive(goal: NutritionGoal) {
        viewModelScope.launch(Dispatchers.IO) {
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
        _navigationEvent.trySend(RecipeRulesNavigationEvent.NavigateBack)
    }

    // endregion

    // region Error Handling

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion
}
