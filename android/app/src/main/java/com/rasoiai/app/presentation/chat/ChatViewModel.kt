package com.rasoiai.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.domain.model.ChatMessage
import com.rasoiai.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for the Chat screen.
 */
data class ChatUiState(
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val quickActions: List<String> = emptyList(),
    val showClearChatDialog: Boolean = false,
    val showMenu: Boolean = false
)

/**
 * Navigation events from Chat screen.
 */
sealed class ChatNavigationEvent {
    data object NavigateBack : ChatNavigationEvent()
    data object NavigateToHome : ChatNavigationEvent()
    data object NavigateToGrocery : ChatNavigationEvent()
    data object NavigateToFavorites : ChatNavigationEvent()
    data object NavigateToStats : ChatNavigationEvent()
    data class NavigateToRecipeDetail(val recipeId: String) : ChatNavigationEvent()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<ChatNavigationEvent?>(null)
    val navigationEvent: StateFlow<ChatNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadMessages()
        loadQuickActions()
    }

    // region Data Loading

    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                chatRepository.getMessages().collect { messages ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = messages
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading messages")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load messages"
                    )
                }
            }
        }
    }

    private fun loadQuickActions() {
        val quickActions = chatRepository.getQuickActions()
        _uiState.update { it.copy(quickActions = quickActions) }
    }

    // endregion

    // region Message Actions

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val message = _uiState.value.inputText.trim()
        if (message.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, inputText = "") }

            chatRepository.sendMessage(message)
                .onSuccess {
                    Timber.i("Message sent successfully")
                    _uiState.update { it.copy(isSending = false) }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to send message")
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = "Failed to send message"
                        )
                    }
                }
        }
    }

    fun onQuickActionClick(action: String) {
        _uiState.update { it.copy(inputText = action) }
        sendMessage()
    }

    // endregion

    // region Menu Actions

    fun toggleMenu() {
        _uiState.update { it.copy(showMenu = !it.showMenu) }
    }

    fun dismissMenu() {
        _uiState.update { it.copy(showMenu = false) }
    }

    fun showClearChatDialog() {
        dismissMenu()
        _uiState.update { it.copy(showClearChatDialog = true) }
    }

    fun dismissClearChatDialog() {
        _uiState.update { it.copy(showClearChatDialog = false) }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            dismissClearChatDialog()
            chatRepository.clearHistory()
                .onSuccess {
                    Timber.i("Chat history cleared")
                    loadQuickActions()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to clear history")
                    _uiState.update { it.copy(errorMessage = "Failed to clear chat") }
                }
        }
    }

    // endregion

    // region Navigation

    fun onNavigationHandled() {
        _navigationEvent.value = null
    }

    fun navigateBack() {
        _navigationEvent.value = ChatNavigationEvent.NavigateBack
    }

    fun navigateToHome() {
        _navigationEvent.value = ChatNavigationEvent.NavigateToHome
    }

    fun navigateToGrocery() {
        _navigationEvent.value = ChatNavigationEvent.NavigateToGrocery
    }

    fun navigateToFavorites() {
        _navigationEvent.value = ChatNavigationEvent.NavigateToFavorites
    }

    fun navigateToStats() {
        _navigationEvent.value = ChatNavigationEvent.NavigateToStats
    }

    fun navigateToRecipeDetail(recipeId: String) {
        _navigationEvent.value = ChatNavigationEvent.NavigateToRecipeDetail(recipeId)
    }

    // endregion

    // region Meal Plan Actions

    fun addRecipeToMealPlan(recipeId: String) {
        viewModelScope.launch {
            // TODO: Implement actual meal plan integration via MealPlanRepository
            // For now, show a confirmation message
            Timber.i("Adding recipe $recipeId to meal plan")
            _uiState.update { it.copy(errorMessage = "Recipe added to your meal plan!") }
        }
    }

    // endregion

    // region Error Handling

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion

    // region Placeholder Actions

    fun onVoiceButtonClick() {
        _uiState.update { it.copy(errorMessage = "Voice input coming soon!") }
    }

    fun onAttachmentButtonClick() {
        _uiState.update { it.copy(errorMessage = "Photo attachment coming soon!") }
    }

    // endregion
}
