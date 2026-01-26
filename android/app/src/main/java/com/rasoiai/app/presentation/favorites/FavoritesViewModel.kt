package com.rasoiai.app.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.FavoriteCollection
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.repository.FavoritesRepository
import com.rasoiai.domain.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for the Favorites screen
 */
data class FavoritesUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val collections: List<FavoriteCollection> = emptyList(),
    val selectedCollectionId: String = FavoriteCollection.COLLECTION_ALL,
    val recipes: List<Recipe> = emptyList(),
    val isReorderMode: Boolean = false,
    val showCreateCollectionDialog: Boolean = false,
    val showSearchBar: Boolean = false,
    val searchQuery: String = "",
    val selectedCuisineFilter: CuisineType? = null,
    val selectedTimeFilter: TimeFilter? = null
) {
    val selectedCollection: FavoriteCollection?
        get() = collections.find { it.id == selectedCollectionId }

    val filteredRecipes: List<Recipe>
        get() {
            var filtered = recipes

            // Apply search filter
            if (searchQuery.isNotBlank()) {
                filtered = filtered.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
                }
            }

            // Apply cuisine filter
            selectedCuisineFilter?.let { cuisine ->
                filtered = filtered.filter { it.cuisineType == cuisine }
            }

            // Apply time filter
            selectedTimeFilter?.let { timeFilter ->
                filtered = filtered.filter { it.totalTimeMinutes <= timeFilter.maxMinutes }
            }

            return filtered
        }

    val recipeCount: Int
        get() = filteredRecipes.size
}

enum class TimeFilter(val label: String, val maxMinutes: Int) {
    UNDER_15("< 15 min", 15),
    UNDER_30("< 30 min", 30),
    UNDER_45("< 45 min", 45),
    UNDER_60("< 1 hour", 60)
}

/**
 * Navigation events from Favorites screen
 */
sealed class FavoritesNavigationEvent {
    data object NavigateBack : FavoritesNavigationEvent()
    data object NavigateToHome : FavoritesNavigationEvent()
    data object NavigateToGrocery : FavoritesNavigationEvent()
    data object NavigateToChat : FavoritesNavigationEvent()
    data object NavigateToStats : FavoritesNavigationEvent()
    data class NavigateToRecipeDetail(val recipeId: String) : FavoritesNavigationEvent()
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<FavoritesNavigationEvent?>(null)
    val navigationEvent: StateFlow<FavoritesNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadData()
    }

    // region Data Loading

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                combine(
                    favoritesRepository.getCollections(),
                    favoritesRepository.getRecipesInCollection(_uiState.value.selectedCollectionId)
                ) { collections, recipes ->
                    Pair(collections, recipes)
                }.collect { (collections, recipes) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            collections = collections,
                            recipes = recipes
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading favorites")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load favorites. Please try again."
                    )
                }
            }
        }
    }

    private fun loadRecipesForCollection(collectionId: String) {
        viewModelScope.launch {
            try {
                favoritesRepository.getRecipesInCollection(collectionId).collect { recipes ->
                    _uiState.update { it.copy(recipes = recipes) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading recipes for collection")
            }
        }
    }

    // endregion

    // region Collection Actions

    fun selectCollection(collectionId: String) {
        _uiState.update { it.copy(selectedCollectionId = collectionId) }
        loadRecipesForCollection(collectionId)
    }

    fun showCreateCollectionDialog() {
        _uiState.update { it.copy(showCreateCollectionDialog = true) }
    }

    fun dismissCreateCollectionDialog() {
        _uiState.update { it.copy(showCreateCollectionDialog = false) }
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            favoritesRepository.createCollection(name)
                .onSuccess { collection ->
                    Timber.i("Collection created: ${collection.name}")
                    dismissCreateCollectionDialog()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to create collection")
                    _uiState.update { it.copy(errorMessage = "Failed to create collection") }
                }
        }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            favoritesRepository.deleteCollection(collectionId)
                .onSuccess {
                    Timber.i("Collection deleted")
                    // If we deleted the selected collection, switch to All
                    if (_uiState.value.selectedCollectionId == collectionId) {
                        selectCollection(FavoriteCollection.COLLECTION_ALL)
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to delete collection")
                    _uiState.update { it.copy(errorMessage = "Cannot delete this collection") }
                }
        }
    }

    // endregion

    // region Recipe Actions

    fun onRecipeClick(recipeId: String) {
        if (_uiState.value.isReorderMode) return
        _navigationEvent.value = FavoritesNavigationEvent.NavigateToRecipeDetail(recipeId)
    }

    fun removeFromFavorites(recipeId: String) {
        viewModelScope.launch {
            recipeRepository.toggleFavorite(recipeId)
                .onSuccess {
                    Timber.i("Recipe removed from favorites")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to remove from favorites")
                    _uiState.update { it.copy(errorMessage = "Failed to remove recipe") }
                }
        }
    }

    fun addToCollection(recipeId: String, collectionId: String) {
        viewModelScope.launch {
            favoritesRepository.addRecipeToCollection(recipeId, collectionId)
                .onSuccess {
                    Timber.i("Recipe added to collection")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to add to collection")
                    _uiState.update { it.copy(errorMessage = "Failed to add recipe to collection") }
                }
        }
    }

    fun removeFromCollection(recipeId: String) {
        val collectionId = _uiState.value.selectedCollectionId
        if (collectionId == FavoriteCollection.COLLECTION_ALL ||
            collectionId == FavoriteCollection.COLLECTION_RECENTLY_VIEWED) {
            removeFromFavorites(recipeId)
            return
        }

        viewModelScope.launch {
            favoritesRepository.removeRecipeFromCollection(recipeId, collectionId)
                .onSuccess {
                    Timber.i("Recipe removed from collection")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to remove from collection")
                    _uiState.update { it.copy(errorMessage = "Failed to remove recipe from collection") }
                }
        }
    }

    // endregion

    // region Reorder Mode

    fun enterReorderMode() {
        _uiState.update { it.copy(isReorderMode = true) }
    }

    fun exitReorderMode() {
        _uiState.update { it.copy(isReorderMode = false) }
    }

    fun onReorderRecipes(fromIndex: Int, toIndex: Int) {
        val currentRecipes = _uiState.value.recipes.toMutableList()
        val item = currentRecipes.removeAt(fromIndex)
        currentRecipes.add(toIndex, item)
        _uiState.update { it.copy(recipes = currentRecipes) }
    }

    fun saveReorderState() {
        viewModelScope.launch {
            val collectionId = _uiState.value.selectedCollectionId
            val recipeIds = _uiState.value.recipes.map { it.id }
            favoritesRepository.reorderRecipes(collectionId, recipeIds)
                .onSuccess {
                    Timber.i("Recipes reordered successfully")
                    exitReorderMode()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to save reorder")
                    _uiState.update { it.copy(errorMessage = "Failed to save order") }
                }
        }
    }

    // endregion

    // region Search & Filters

    fun toggleSearchBar() {
        _uiState.update {
            it.copy(
                showSearchBar = !it.showSearchBar,
                searchQuery = if (it.showSearchBar) "" else it.searchQuery
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setCuisineFilter(cuisine: CuisineType?) {
        _uiState.update { it.copy(selectedCuisineFilter = cuisine) }
    }

    fun setTimeFilter(timeFilter: TimeFilter?) {
        _uiState.update { it.copy(selectedTimeFilter = timeFilter) }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedCuisineFilter = null,
                selectedTimeFilter = null,
                searchQuery = ""
            )
        }
    }

    // endregion

    // region Navigation

    fun onNavigationHandled() {
        _navigationEvent.value = null
    }

    fun navigateBack() {
        _navigationEvent.value = FavoritesNavigationEvent.NavigateBack
    }

    fun navigateToHome() {
        _navigationEvent.value = FavoritesNavigationEvent.NavigateToHome
    }

    fun navigateToGrocery() {
        _navigationEvent.value = FavoritesNavigationEvent.NavigateToGrocery
    }

    fun navigateToChat() {
        _navigationEvent.value = FavoritesNavigationEvent.NavigateToChat
    }

    fun navigateToStats() {
        _navigationEvent.value = FavoritesNavigationEvent.NavigateToStats
    }

    // endregion

    // region Error Handling

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion
}
