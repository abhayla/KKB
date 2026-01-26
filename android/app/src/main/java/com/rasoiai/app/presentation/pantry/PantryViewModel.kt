package com.rasoiai.app.presentation.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.domain.model.PantryCategory
import com.rasoiai.domain.model.PantryItem
import com.rasoiai.domain.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for the Pantry screen.
 */
data class PantryUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val pantryItems: List<PantryItem> = emptyList(),
    val expiredItems: List<PantryItem> = emptyList(),
    val matchingRecipeCount: Int = 0,
    val showAddItemDialog: Boolean = false,
    val showRemoveExpiredDialog: Boolean = false,
    val showAllItemsSheet: Boolean = false,
    val isScanning: Boolean = false
) {
    val itemCount: Int get() = pantryItems.size

    val expiringSoonItems: List<PantryItem>
        get() = pantryItems.filter { it.isExpiringSoon && !it.isExpired }

    val hasExpiredItems: Boolean get() = expiredItems.isNotEmpty()
}

/**
 * Navigation events from Pantry screen.
 */
sealed class PantryNavigationEvent {
    data object NavigateBack : PantryNavigationEvent()
    data object NavigateToHome : PantryNavigationEvent()
    data object NavigateToGrocery : PantryNavigationEvent()
    data object NavigateToChat : PantryNavigationEvent()
    data object NavigateToFavorites : PantryNavigationEvent()
    data object NavigateToStats : PantryNavigationEvent()
    data class NavigateToRecipeSearch(val ingredients: List<String>) : PantryNavigationEvent()
}

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val pantryRepository: PantryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<PantryNavigationEvent?>(null)
    val navigationEvent: StateFlow<PantryNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadPantryItems()
        loadExpiredItems()
        loadMatchingRecipeCount()
    }

    // region Data Loading

    private fun loadPantryItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                pantryRepository.getPantryItems().collect { items ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pantryItems = items.sortedBy { item -> item.daysUntilExpiry ?: Int.MAX_VALUE }
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading pantry items")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load pantry items"
                    )
                }
            }
        }
    }

    private fun loadExpiredItems() {
        viewModelScope.launch {
            try {
                pantryRepository.getExpiredItems().collect { items ->
                    _uiState.update { it.copy(expiredItems = items) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading expired items")
            }
        }
    }

    private fun loadMatchingRecipeCount() {
        viewModelScope.launch {
            pantryRepository.getMatchingRecipeCount()
                .onSuccess { count ->
                    _uiState.update { it.copy(matchingRecipeCount = count) }
                }
                .onFailure { e ->
                    Timber.e(e, "Error getting recipe count")
                }
        }
    }

    // endregion

    // region Item Actions

    fun addItem(name: String, category: PantryCategory, quantity: Int, unit: String) {
        viewModelScope.launch {
            pantryRepository.addItem(name, category, quantity, unit)
                .onSuccess {
                    Timber.i("Item added: $name")
                    dismissAddItemDialog()
                    loadMatchingRecipeCount()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to add item")
                    _uiState.update { it.copy(errorMessage = "Failed to add item") }
                }
        }
    }

    fun removeItem(itemId: String) {
        viewModelScope.launch {
            pantryRepository.removeItem(itemId)
                .onSuccess {
                    Timber.i("Item removed")
                    loadMatchingRecipeCount()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to remove item")
                    _uiState.update { it.copy(errorMessage = "Failed to remove item") }
                }
        }
    }

    fun removeExpiredItems() {
        viewModelScope.launch {
            dismissRemoveExpiredDialog()
            pantryRepository.removeExpiredItems()
                .onSuccess { count ->
                    Timber.i("Removed $count expired items")
                    loadMatchingRecipeCount()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to remove expired items")
                    _uiState.update { it.copy(errorMessage = "Failed to remove expired items") }
                }
        }
    }

    // endregion

    // region Scan Actions

    fun onCaptureClick() {
        // Placeholder for camera capture
        _uiState.update { it.copy(errorMessage = "Camera capture coming soon!") }
    }

    fun onGalleryClick() {
        // Placeholder for gallery selection
        _uiState.update { it.copy(errorMessage = "Gallery selection coming soon!") }
    }

    fun simulateScan() {
        // Simulate a scan for demo purposes
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }

            // Simulate delay
            kotlinx.coroutines.delay(1500)

            // Add some mock scanned items
            val scannedItems = listOf(
                "Carrot" to PantryCategory.VEGETABLES,
                "Apple" to PantryCategory.FRUITS,
                "Cucumber" to PantryCategory.VEGETABLES
            )

            pantryRepository.addItemsFromScan(scannedItems)
                .onSuccess { items ->
                    Timber.i("Added ${items.size} items from scan")
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = "Added ${items.size} items from scan!"
                        )
                    }
                    loadMatchingRecipeCount()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to add scanned items")
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = "Failed to process scan"
                        )
                    }
                }
        }
    }

    // endregion

    // region Dialog Actions

    fun showAddItemDialog() {
        _uiState.update { it.copy(showAddItemDialog = true) }
    }

    fun dismissAddItemDialog() {
        _uiState.update { it.copy(showAddItemDialog = false) }
    }

    fun showRemoveExpiredDialog() {
        _uiState.update { it.copy(showRemoveExpiredDialog = true) }
    }

    fun dismissRemoveExpiredDialog() {
        _uiState.update { it.copy(showRemoveExpiredDialog = false) }
    }

    fun showAllItemsSheet() {
        _uiState.update { it.copy(showAllItemsSheet = true) }
    }

    fun dismissAllItemsSheet() {
        _uiState.update { it.copy(showAllItemsSheet = false) }
    }

    // endregion

    // region Navigation

    fun onNavigationHandled() {
        _navigationEvent.value = null
    }

    fun navigateBack() {
        _navigationEvent.value = PantryNavigationEvent.NavigateBack
    }

    fun navigateToHome() {
        _navigationEvent.value = PantryNavigationEvent.NavigateToHome
    }

    fun navigateToGrocery() {
        _navigationEvent.value = PantryNavigationEvent.NavigateToGrocery
    }

    fun navigateToChat() {
        _navigationEvent.value = PantryNavigationEvent.NavigateToChat
    }

    fun navigateToFavorites() {
        _navigationEvent.value = PantryNavigationEvent.NavigateToFavorites
    }

    fun navigateToStats() {
        _navigationEvent.value = PantryNavigationEvent.NavigateToStats
    }

    fun onFindRecipesClick() {
        val ingredients = _uiState.value.pantryItems.map { it.name }
        _navigationEvent.value = PantryNavigationEvent.NavigateToRecipeSearch(ingredients)
    }

    // endregion

    // region Error Handling

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion
}
