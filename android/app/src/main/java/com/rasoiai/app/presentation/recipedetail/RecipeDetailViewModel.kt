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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for the Recipe Detail screen
 */
data class RecipeDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val recipe: Recipe? = null,
    val selectedServings: Int = 4,
    val scaledIngredients: List<Ingredient> = emptyList(),
    val scaledNutrition: Nutrition? = null,
    val checkedIngredients: Set<String> = emptySet(),
    val selectedTabIndex: Int = 0
) {
    val isVegetarian: Boolean
        get() = recipe?.dietaryTags?.any {
            it == DietaryTag.VEGETARIAN || it == DietaryTag.VEGAN || it == DietaryTag.JAIN
        } ?: true

    val totalTimeMinutes: Int
        get() = recipe?.totalTimeMinutes ?: 0

    val displayTags: List<String>
        get() {
            val tags = mutableListOf<String>()
            recipe?.let { r ->
                // Add dietary tags
                r.dietaryTags.forEach { tags.add(it.displayName) }
                // Add difficulty
                tags.add(r.difficulty.value.replaceFirstChar { it.uppercase() })
            }
            return tags
        }

    val cuisineDisplayText: String
        get() {
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

    val ingredientCount: Int
        get() = scaledIngredients.size

    val instructionCount: Int
        get() = recipe?.instructions?.size ?: 0

    val allIngredientsChecked: Boolean
        get() = checkedIngredients.size == scaledIngredients.size && scaledIngredients.isNotEmpty()
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

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<RecipeDetailNavigationEvent?>(null)
    val navigationEvent: StateFlow<RecipeDetailNavigationEvent?> = _navigationEvent.asStateFlow()

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
                                scaledIngredients = recipe.ingredients,
                                scaledNutrition = recipe.nutrition
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
                            scaledIngredients = scaledRecipe.ingredients,
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
                state.checkedIngredients - ingredientId
            } else {
                state.checkedIngredients + ingredientId
            }
            state.copy(checkedIngredients = newChecked)
        }
    }

    fun checkAllIngredients() {
        _uiState.update { state ->
            state.copy(checkedIngredients = state.scaledIngredients.map { it.id }.toSet())
        }
    }

    fun uncheckAllIngredients() {
        _uiState.update { it.copy(checkedIngredients = emptySet()) }
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
        _navigationEvent.value = RecipeDetailNavigationEvent.NavigateToCookingMode(recipeId)
    }

    fun modifyWithAI() {
        val recipeName = _uiState.value.recipe?.name ?: "this recipe"
        _navigationEvent.value = RecipeDetailNavigationEvent.NavigateToChat(
            "I'd like to modify $recipeName"
        )
    }

    fun navigateBack() {
        _navigationEvent.value = RecipeDetailNavigationEvent.NavigateBack
    }

    // endregion

    // region Navigation

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
