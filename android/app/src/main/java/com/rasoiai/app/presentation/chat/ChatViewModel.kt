package com.rasoiai.app.presentation.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.domain.model.ChatMessage
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.repository.ChatRepository
import com.rasoiai.domain.repository.MealPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
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
    val showMenu: Boolean = false,
    // Image attachment state
    val showImageSourceDialog: Boolean = false,
    val selectedImageUri: Uri? = null,
    val isUploadingImage: Boolean = false
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
    private val chatRepository: ChatRepository,
    private val mealPlanRepository: MealPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<ChatNavigationEvent>()
    val navigationEvent: Flow<ChatNavigationEvent> = _navigationEvent.receiveAsFlow()

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

    fun navigateBack() {
        _navigationEvent.trySend(ChatNavigationEvent.NavigateBack)
    }

    fun navigateToHome() {
        _navigationEvent.trySend(ChatNavigationEvent.NavigateToHome)
    }

    fun navigateToGrocery() {
        _navigationEvent.trySend(ChatNavigationEvent.NavigateToGrocery)
    }

    fun navigateToFavorites() {
        _navigationEvent.trySend(ChatNavigationEvent.NavigateToFavorites)
    }

    fun navigateToStats() {
        _navigationEvent.trySend(ChatNavigationEvent.NavigateToStats)
    }

    fun navigateToRecipeDetail(recipeId: String) {
        _navigationEvent.trySend(ChatNavigationEvent.NavigateToRecipeDetail(recipeId))
    }

    // endregion

    // region Meal Plan Actions

    /**
     * Add a recipe to the user's meal plan for today.
     * Uses the current time to determine appropriate meal type.
     */
    fun addRecipeToMealPlan(recipeId: String, recipeName: String = "Recipe") {
        viewModelScope.launch {
            val today = LocalDate.now()
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

            // Determine meal type based on time of day
            val mealType = when {
                hour in 6..10 -> MealType.BREAKFAST
                hour in 11..15 -> MealType.LUNCH
                hour in 16..18 -> MealType.SNACKS
                else -> MealType.DINNER
            }

            Timber.i("Adding recipe $recipeId to $mealType")

            try {
                // Get current meal plan
                val mealPlan = mealPlanRepository.getMealPlanForDate(today).first()

                if (mealPlan != null) {
                    mealPlanRepository.addRecipeToMeal(
                        mealPlanId = mealPlan.id,
                        date = today,
                        mealType = mealType,
                        recipeId = recipeId,
                        recipeName = recipeName,
                        recipeImageUrl = null,
                        prepTimeMinutes = 30,
                        calories = 0
                    ).onSuccess {
                        val mealTypeDisplay = mealType.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                        _uiState.update {
                            it.copy(errorMessage = "Added $recipeName to $mealTypeDisplay!")
                        }
                    }.onFailure { e ->
                        Timber.e(e, "Failed to add recipe to meal plan")
                        _uiState.update {
                            it.copy(errorMessage = "Failed to add recipe to meal plan")
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(errorMessage = "No meal plan available. Generate one first!")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error accessing meal plan")
                _uiState.update { it.copy(errorMessage = "Failed to access meal plan") }
            }
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

    // endregion

    // region Image Attachment

    fun onAttachmentButtonClick() {
        _uiState.update { it.copy(showImageSourceDialog = true) }
    }

    fun dismissImageSourceDialog() {
        _uiState.update { it.copy(showImageSourceDialog = false) }
    }

    fun onImageSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                showImageSourceDialog = false,
                selectedImageUri = uri,
                isUploadingImage = true
            )
        }
        processAndSendImage(uri)
    }

    private fun processAndSendImage(uri: Uri) {
        viewModelScope.launch {
            try {
                chatRepository.sendImageMessage(uri.toString())
                    .onSuccess { response ->
                        Timber.i("Image analyzed successfully")
                        _uiState.update {
                            it.copy(
                                isUploadingImage = false,
                                selectedImageUri = null
                            )
                        }
                    }
                    .onFailure { e ->
                        Timber.e(e, "Failed to send image")
                        _uiState.update {
                            it.copy(
                                isUploadingImage = false,
                                selectedImageUri = null,
                                errorMessage = "Failed to analyze image. Please try again."
                            )
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error processing image")
                _uiState.update {
                    it.copy(
                        isUploadingImage = false,
                        selectedImageUri = null,
                        errorMessage = "Failed to process image. Please try again."
                    )
                }
            }
        }
    }

    fun clearSelectedImage() {
        _uiState.update {
            it.copy(
                selectedImageUri = null,
                isUploadingImage = false
            )
        }
    }

    // endregion
}
