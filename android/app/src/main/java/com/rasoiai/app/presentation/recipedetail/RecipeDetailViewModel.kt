package com.rasoiai.app.presentation.recipedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.navigation.Screen
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Ingredient
import com.rasoiai.domain.model.Instruction
import com.rasoiai.domain.model.Nutrition
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Lock state for recipe in meal plan context
 */
enum class RecipeLockState {
    /** Recipe is locked in the meal plan (protected from regeneration) - shows 🔒 */
    LOCKED,
    /** Recipe is unlocked in the meal plan (can be swapped/regenerated) - shows 🔓 */
    UNLOCKED,
    /** Recipe not accessed from meal plan context (favorites, search, chat) - no icon */
    NO_CONTEXT
}

/**
 * UI state for the Recipe Detail screen
 * Uses immutable collections for Compose stability optimization.
 */
data class RecipeDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val recipe: Recipe? = null,
    val selectedServings: Int = 4,
    val scaledIngredients: ImmutableList<Ingredient> = persistentListOf(),
    val scaledNutrition: Nutrition? = null,
    val checkedIngredients: ImmutableSet<String> = persistentSetOf(),
    val selectedTabIndex: Int = 0,
    /** Lock state in meal plan context - determines which lock icon to show (if any) */
    val lockState: RecipeLockState = RecipeLockState.NO_CONTEXT,
    /** Cached display tags to avoid recomputation on every access */
    val displayTags: ImmutableList<String> = persistentListOf(),
    /** Cached cuisine display text */
    val cuisineDisplayText: String = ""
) {
    /** For backwards compatibility */
    val isLocked: Boolean
        get() = lockState == RecipeLockState.LOCKED

    val isVegetarian: Boolean
        get() = recipe?.dietaryTags?.any {
            it == DietaryTag.VEGETARIAN || it == DietaryTag.VEGAN || it == DietaryTag.JAIN
        } ?: true

    val totalTimeMinutes: Int
        get() = recipe?.totalTimeMinutes ?: 0

    val ingredientCount: Int
        get() = scaledIngredients.size

    val instructionCount: Int
        get() = recipe?.instructions?.size ?: 0

    val allIngredientsChecked: Boolean
        get() = checkedIngredients.size == scaledIngredients.size && scaledIngredients.isNotEmpty()

    companion object {
        /** Compute display tags from recipe - call when recipe changes */
        fun computeDisplayTags(recipe: Recipe?): ImmutableList<String> {
            if (recipe == null) return persistentListOf()
            return buildList {
                recipe.dietaryTags.forEach { add(it.displayName) }
                add(recipe.difficulty.value.replaceFirstChar { it.uppercase() })
            }.toImmutableList()
        }

        /** Compute cuisine display text from recipe - call when recipe changes */
        fun computeCuisineDisplayText(recipe: Recipe?): String {
            val cuisine = recipe?.cuisineType?.displayName ?: ""
            val region = when (recipe?.cuisineType) {
                com.rasoiai.domain.model.CuisineType.NORTH -> "Punjabi"
                com.rasoiai.domain.model.CuisineType.SOUTH -> "Tamil"
                com.rasoiai.domain.model.CuisineType.EAST -> "Bengali"
                com.rasoiai.domain.model.CuisineType.WEST -> "Gujarati"
                null -> ""
            }
            return if (region.isNotEmpty()) "$cuisine \u2022 $region" else cuisine
        }
    }
}

/**
 * Navigation events from Recipe Detail screen
 */
sealed class RecipeDetailNavigationEvent {
    data class NavigateToCookingMode(val recipeId: String) : RecipeDetailNavigationEvent()
    data class NavigateToChat(val recipeContext: String) : RecipeDetailNavigationEvent()
    data object NavigateBack : RecipeDetailNavigationEvent()
}

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedStateHandle[Screen.RecipeDetail.ARG_RECIPE_ID])
    private val isLocked: Boolean = savedStateHandle[Screen.RecipeDetail.ARG_IS_LOCKED] ?: false
    private val fromMealPlan: Boolean = savedStateHandle[Screen.RecipeDetail.ARG_FROM_MEAL_PLAN] ?: false

    // Determine lock state based on navigation parameters
    private val lockState: RecipeLockState = when {
        !fromMealPlan -> RecipeLockState.NO_CONTEXT
        isLocked -> RecipeLockState.LOCKED
        else -> RecipeLockState.UNLOCKED
    }

    private val _uiState = MutableStateFlow(RecipeDetailUiState(lockState = lockState))
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<RecipeDetailNavigationEvent>()
    val navigationEvent: Flow<RecipeDetailNavigationEvent> = _navigationEvent.receiveAsFlow()

    init {
        loadRecipe()
    }

    // region Data Loading

    private fun loadRecipe() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                recipeRepository.getRecipeById(recipeId).collect { recipe ->
                    if (recipe != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                recipe = recipe,
                                selectedServings = recipe.servings,
                                scaledIngredients = recipe.ingredients.toImmutableList(),
                                scaledNutrition = recipe.nutrition,
                                displayTags = RecipeDetailUiState.computeDisplayTags(recipe),
                                cuisineDisplayText = RecipeDetailUiState.computeCuisineDisplayText(recipe)
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Recipe not found"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading recipe")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load recipe. Please try again."
                    )
                }
            }
        }
    }

    // endregion

    // region Tab Selection

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    // endregion

    // region Servings Adjustment

    fun updateServings(servings: Int) {
        if (servings < 1 || servings > 12) return

        viewModelScope.launch {
            recipeRepository.scaleRecipe(recipeId, servings)
                .onSuccess { scaledRecipe ->
                    _uiState.update {
                        it.copy(
                            selectedServings = servings,
                            scaledIngredients = scaledRecipe.ingredients.toImmutableList(),
                            scaledNutrition = scaledRecipe.nutrition
                        )
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to scale recipe")
                }
        }
    }

    // endregion

    // region Ingredient Checking

    fun toggleIngredientChecked(ingredientId: String) {
        _uiState.update { state ->
            val newChecked = if (ingredientId in state.checkedIngredients) {
                (state.checkedIngredients - ingredientId).toImmutableSet()
            } else {
                (state.checkedIngredients + ingredientId).toImmutableSet()
            }
            state.copy(checkedIngredients = newChecked)
        }
    }

    fun checkAllIngredients() {
        _uiState.update { state ->
            state.copy(checkedIngredients = state.scaledIngredients.map { it.id }.toImmutableSet())
        }
    }

    fun uncheckAllIngredients() {
        _uiState.update { it.copy(checkedIngredients = persistentSetOf()) }
    }

    // endregion

    // region Favorite

    fun toggleFavorite() {
        viewModelScope.launch {
            recipeRepository.toggleFavorite(recipeId)
                .onSuccess { isFavorite ->
                    _uiState.update { state ->
                        state.copy(
                            recipe = state.recipe?.copy(isFavorite = isFavorite)
                        )
                    }
                    Timber.i("Favorite toggled: $isFavorite")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to toggle favorite")
                    _uiState.update { it.copy(errorMessage = "Failed to update favorite") }
                }
        }
    }

    // endregion

    // region Actions

    fun addAllToGroceryList() {
        // TODO: Implement grocery list integration
        Timber.i("Adding ${_uiState.value.scaledIngredients.size} ingredients to grocery list")
        _uiState.update { it.copy(errorMessage = "Added to grocery list!") }
    }

    fun startCookingMode() {
        _navigationEvent.trySend(RecipeDetailNavigationEvent.NavigateToCookingMode(recipeId))
    }

    fun modifyWithAI() {
        val recipeName = _uiState.value.recipe?.name ?: "this recipe"
        _navigationEvent.trySend(RecipeDetailNavigationEvent.NavigateToChat(
            "I'd like to modify $recipeName"
        ))
    }

    fun navigateBack() {
        _navigationEvent.trySend(RecipeDetailNavigationEvent.NavigateBack)
    }

    // endregion

    // region Error Handling

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion
}
