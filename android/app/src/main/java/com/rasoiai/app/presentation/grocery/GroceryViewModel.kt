package com.rasoiai.app.presentation.grocery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.domain.model.GroceryCategory
import com.rasoiai.domain.model.GroceryItem
import com.rasoiai.domain.model.GroceryList
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.repository.GroceryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * UI state for the Grocery List screen
 */
data class GroceryUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val groceryList: GroceryList? = null,
    val categories: List<GroceryCategory> = emptyList(),
    val expandedCategories: Set<IngredientCategory> = IngredientCategory.entries.toSet(),
    val showWhatsAppDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showAddItemDialog: Boolean = false,
    val showMoreOptionsMenu: Boolean = false,
    val selectedItem: GroceryItem? = null,
    val shareOption: ShareOption = ShareOption.FULL_LIST
) {
    val formattedDateRange: String
        get() = groceryList?.let {
            val formatter = DateTimeFormatter.ofPattern("MMM d")
            "${it.weekStartDate.format(formatter)}-${it.weekEndDate.format(formatter)}"
        } ?: ""

    val totalItems: Int
        get() = groceryList?.totalItems ?: 0

    val purchasedItems: Int
        get() = groceryList?.purchasedItems ?: 0

    val unpurchasedItems: Int
        get() = groceryList?.unpurchasedItems ?: 0

    val whatsAppShareText: String
        get() = generateWhatsAppText()

    private fun generateWhatsAppText(): String {
        val items = when (shareOption) {
            ShareOption.FULL_LIST -> groceryList?.items ?: emptyList()
            ShareOption.UNPURCHASED_ONLY -> groceryList?.items?.filter { !it.isPurchased } ?: emptyList()
        }

        if (items.isEmpty()) return ""

        val grouped = items.groupBy { it.category }
        val builder = StringBuilder()

        builder.appendLine("*Grocery List*")
        builder.appendLine("Week: $formattedDateRange")
        builder.appendLine()

        grouped.forEach { (category, categoryItems) ->
            builder.appendLine("*${getCategoryEmoji(category)} ${category.displayName}*")
            categoryItems.forEach { item ->
                val checkMark = if (item.isPurchased) "[x]" else "[ ]"
                builder.appendLine("$checkMark ${item.name} - ${item.displayQuantity}")
            }
            builder.appendLine()
        }

        builder.appendLine("_Sent from RasoiAI_")
        return builder.toString()
    }

    private fun getCategoryEmoji(category: IngredientCategory): String {
        return when (category) {
            IngredientCategory.VEGETABLES -> ""
            IngredientCategory.FRUITS -> ""
            IngredientCategory.DAIRY -> ""
            IngredientCategory.GRAINS -> ""
            IngredientCategory.PULSES -> ""
            IngredientCategory.SPICES -> ""
            IngredientCategory.OILS -> ""
            IngredientCategory.MEAT -> ""
            IngredientCategory.SEAFOOD -> ""
            IngredientCategory.NUTS -> ""
            IngredientCategory.SWEETENERS -> ""
            IngredientCategory.OTHER -> ""
        }
    }
}

enum class ShareOption {
    FULL_LIST,
    UNPURCHASED_ONLY
}

/**
 * Navigation events from Grocery screen
 */
sealed class GroceryNavigationEvent {
    data object NavigateBack : GroceryNavigationEvent()
    data object NavigateToHome : GroceryNavigationEvent()
    data object NavigateToChat : GroceryNavigationEvent()
    data object NavigateToFavorites : GroceryNavigationEvent()
    data object NavigateToStats : GroceryNavigationEvent()
    data class ShareViaWhatsApp(val text: String) : GroceryNavigationEvent()
}

@HiltViewModel
class GroceryViewModel @Inject constructor(
    private val groceryRepository: GroceryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroceryUiState())
    val uiState: StateFlow<GroceryUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<GroceryNavigationEvent?>(null)
    val navigationEvent: StateFlow<GroceryNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadGroceryList()
    }

    // region Data Loading

    private fun loadGroceryList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                groceryRepository.getCurrentGroceryList().collect { groceryList ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            groceryList = groceryList,
                            categories = groceryList?.categorizedItems ?: emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading grocery list")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load grocery list. Please try again."
                    )
                }
            }
        }
    }

    // endregion

    // region Item Actions

    fun toggleItemPurchased(item: GroceryItem) {
        viewModelScope.launch {
            groceryRepository.toggleItemPurchased(item.id)
                .onSuccess { Timber.i("Item toggled: ${item.name}") }
                .onFailure { e ->
                    Timber.e(e, "Failed to toggle item")
                    _uiState.update { it.copy(errorMessage = "Failed to update item") }
                }
        }
    }

    fun deleteItem(item: GroceryItem) {
        viewModelScope.launch {
            groceryRepository.deleteItem(item.id)
                .onSuccess { Timber.i("Item deleted: ${item.name}") }
                .onFailure { e ->
                    Timber.e(e, "Failed to delete item")
                    _uiState.update { it.copy(errorMessage = "Failed to delete item") }
                }
        }
    }

    fun showEditDialog(item: GroceryItem) {
        _uiState.update {
            it.copy(
                showEditDialog = true,
                selectedItem = item
            )
        }
    }

    fun dismissEditDialog() {
        _uiState.update {
            it.copy(
                showEditDialog = false,
                selectedItem = null
            )
        }
    }

    fun updateItemQuantity(quantity: String, unit: String) {
        val item = _uiState.value.selectedItem ?: return

        viewModelScope.launch {
            groceryRepository.updateItemQuantity(item.id, quantity, unit)
                .onSuccess {
                    Timber.i("Item updated: ${item.name}")
                    dismissEditDialog()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to update item")
                    _uiState.update { it.copy(errorMessage = "Failed to update item") }
                }
        }
    }

    // endregion

    // region Add Custom Item

    fun showAddItemDialog() {
        _uiState.update { it.copy(showAddItemDialog = true) }
    }

    fun dismissAddItemDialog() {
        _uiState.update { it.copy(showAddItemDialog = false) }
    }

    fun addCustomItem(name: String, quantity: String, unit: String, category: IngredientCategory) {
        val newItem = GroceryItem(
            id = "", // Will be generated by repository
            name = name,
            quantity = quantity,
            unit = unit,
            category = category,
            isCustom = true
        )

        viewModelScope.launch {
            groceryRepository.addCustomItem(newItem)
                .onSuccess {
                    Timber.i("Custom item added: $name")
                    dismissAddItemDialog()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to add custom item")
                    _uiState.update { it.copy(errorMessage = "Failed to add item") }
                }
        }
    }

    // endregion

    // region Category Expansion

    fun toggleCategoryExpanded(category: IngredientCategory) {
        _uiState.update { state ->
            val newExpanded = if (category in state.expandedCategories) {
                state.expandedCategories - category
            } else {
                state.expandedCategories + category
            }
            state.copy(expandedCategories = newExpanded)
        }
    }

    // endregion

    // region WhatsApp Share

    fun showWhatsAppDialog() {
        _uiState.update { it.copy(showWhatsAppDialog = true) }
    }

    fun dismissWhatsAppDialog() {
        _uiState.update { it.copy(showWhatsAppDialog = false) }
    }

    fun setShareOption(option: ShareOption) {
        _uiState.update { it.copy(shareOption = option) }
    }

    fun shareViaWhatsApp() {
        val text = _uiState.value.whatsAppShareText
        dismissWhatsAppDialog()
        _navigationEvent.value = GroceryNavigationEvent.ShareViaWhatsApp(text)
    }

    // endregion

    // region More Options Menu

    fun showMoreOptionsMenu() {
        _uiState.update { it.copy(showMoreOptionsMenu = true) }
    }

    fun dismissMoreOptionsMenu() {
        _uiState.update { it.copy(showMoreOptionsMenu = false) }
    }

    fun clearPurchasedItems() {
        viewModelScope.launch {
            dismissMoreOptionsMenu()
            groceryRepository.clearPurchasedItems()
                .onSuccess { count ->
                    Timber.i("Cleared $count purchased items")
                    _uiState.update { it.copy(errorMessage = "Cleared $count purchased items") }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to clear purchased items")
                    _uiState.update { it.copy(errorMessage = "Failed to clear items") }
                }
        }
    }

    fun shareAsText() {
        dismissMoreOptionsMenu()
        val text = _uiState.value.whatsAppShareText
        _navigationEvent.value = GroceryNavigationEvent.ShareViaWhatsApp(text)
    }

    // endregion

    // region Navigation

    fun onNavigationHandled() {
        _navigationEvent.value = null
    }

    fun navigateBack() {
        _navigationEvent.value = GroceryNavigationEvent.NavigateBack
    }

    fun navigateToHome() {
        _navigationEvent.value = GroceryNavigationEvent.NavigateToHome
    }

    fun navigateToChat() {
        _navigationEvent.value = GroceryNavigationEvent.NavigateToChat
    }

    fun navigateToFavorites() {
        _navigationEvent.value = GroceryNavigationEvent.NavigateToFavorites
    }

    fun navigateToStats() {
        _navigationEvent.value = GroceryNavigationEvent.NavigateToStats
    }

    // endregion

    // region Error Handling

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion
}
