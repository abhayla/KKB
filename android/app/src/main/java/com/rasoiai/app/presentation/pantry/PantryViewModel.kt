package com.rasoiai.app.presentation.pantry

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.domain.model.PantryCategory
import com.rasoiai.domain.model.PantryItem
import com.rasoiai.domain.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
 * UI state for the Pantry screen.
 */
/**
 * Data class representing a scanned item before being added to the pantry
 */
data class ScannedItemData(
    val name: String,
    val category: PantryCategory,
    var quantity: Int = 1,
    var unit: String = "piece"
)

data class PantryUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val pantryItems: List<PantryItem> = emptyList(),
    val expiredItems: List<PantryItem> = emptyList(),
    val matchingRecipeCount: Int = 0,
    val showAddItemDialog: Boolean = false,
    val showRemoveExpiredDialog: Boolean = false,
    val showAllItemsSheet: Boolean = false,
    val showScanResultsSheet: Boolean = false,
    val scannedItems: List<ScannedItemData> = emptyList(),
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
    private val pantryRepository: PantryRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<PantryNavigationEvent>(Channel.BUFFERED)
    val navigationEvent: Flow<PantryNavigationEvent> = _navigationEvent.receiveAsFlow()

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
        // Triggers camera permission + launcher from the Screen
        _uiState.update { it.copy(errorMessage = "Camera capture coming soon!") }
    }

    fun onGalleryClick() {
        // Triggers gallery permission + launcher from the Screen
        _uiState.update { it.copy(errorMessage = "Gallery selection coming soon!") }
    }

    /**
     * Called after camera captures an image. Triggers scan analysis.
     * Currently uses simulated scan — will be replaced with AI image analysis.
     */
    fun onImageCaptured(uri: Uri) {
        Timber.i("Image captured: $uri")
        analyzeImage(uri)
    }

    /**
     * Called after gallery image is selected. Triggers scan analysis.
     * Currently uses simulated scan — will be replaced with AI image analysis.
     */
    fun onImageSelected(uri: Uri) {
        Timber.i("Image selected from gallery: $uri")
        analyzeImage(uri)
    }

    fun analyzeImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }

            try {
                val imageBytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: throw Exception("Could not read image")
                val fileName = uri.lastPathSegment ?: "photo.jpg"

                pantryRepository.analyzeImage(imageBytes, fileName)
                    .onSuccess { items ->
                        val scannedItems = items.map { ScannedItemData(it.name, it.category, it.quantity, it.unit) }
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                scannedItems = scannedItems,
                                showScanResultsSheet = true
                            )
                        }
                    }
                    .onFailure { e ->
                        Timber.e(e, "Photo analysis failed")
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                errorMessage = "Could not analyze image. Please add items manually."
                            )
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to read image")
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = "Could not read image. Please try again."
                    )
                }
            }
        }
    }

    fun dismissScanResultsSheet() {
        _uiState.update {
            it.copy(
                showScanResultsSheet = false,
                scannedItems = emptyList()
            )
        }
    }

    fun confirmScanResults(items: List<ScannedItemData>) {
        viewModelScope.launch {
            dismissScanResultsSheet()

            // Convert to the format expected by repository
            val itemsToAdd = items.map { item ->
                item.name to item.category
            }

            pantryRepository.addItemsFromScan(itemsToAdd)
                .onSuccess { addedItems ->
                    Timber.i("Added ${addedItems.size} items from scan")
                    _uiState.update {
                        it.copy(errorMessage = "Added ${addedItems.size} items to pantry!")
                    }
                    loadMatchingRecipeCount()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to add scanned items")
                    _uiState.update {
                        it.copy(errorMessage = "Failed to add items")
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

    fun navigateBack() {
        _navigationEvent.trySend(PantryNavigationEvent.NavigateBack)
    }

    fun navigateToHome() {
        _navigationEvent.trySend(PantryNavigationEvent.NavigateToHome)
    }

    fun navigateToGrocery() {
        _navigationEvent.trySend(PantryNavigationEvent.NavigateToGrocery)
    }

    fun navigateToChat() {
        _navigationEvent.trySend(PantryNavigationEvent.NavigateToChat)
    }

    fun navigateToFavorites() {
        _navigationEvent.trySend(PantryNavigationEvent.NavigateToFavorites)
    }

    fun navigateToStats() {
        _navigationEvent.trySend(PantryNavigationEvent.NavigateToStats)
    }

    fun onFindRecipesClick() {
        val ingredients = _uiState.value.pantryItems.map { it.name }
        _navigationEvent.trySend(PantryNavigationEvent.NavigateToRecipeSearch(ingredients))
    }

    // endregion

    // region Error Handling

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion
}
