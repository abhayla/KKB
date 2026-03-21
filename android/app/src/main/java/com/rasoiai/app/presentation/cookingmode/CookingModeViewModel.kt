package com.rasoiai.app.presentation.cookingmode

import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.navigation.Screen
import com.rasoiai.domain.model.Instruction
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.repository.RecipeRepository
import com.rasoiai.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
 * Timer state for cooking steps
 */
enum class TimerState {
    IDLE,       // Timer not started
    RUNNING,    // Timer counting down
    PAUSED,     // Timer paused
    COMPLETED   // Timer finished
}

/**
 * UI state for Cooking Mode screen
 */
data class CookingModeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val recipe: Recipe? = null,
    val currentStepIndex: Int = 0,
    val timerState: TimerState = TimerState.IDLE,
    val timerRemainingSeconds: Int = 0,
    val timerTotalSeconds: Int = 0,
    val showExitConfirmation: Boolean = false,
    val showCompletionDialog: Boolean = false,
    val rating: Int = 0,
    val feedback: String = "",
    val voiceGuidanceEnabled: Boolean = false
) {
    val totalSteps: Int
        get() = recipe?.instructions?.size ?: 0

    val currentStep: Instruction?
        get() = recipe?.instructions?.getOrNull(currentStepIndex)

    val stepNumber: Int
        get() = currentStepIndex + 1

    val progress: Float
        get() = if (totalSteps > 0) stepNumber.toFloat() / totalSteps else 0f

    val isFirstStep: Boolean
        get() = currentStepIndex == 0

    val isLastStep: Boolean
        get() = currentStepIndex == totalSteps - 1

    val hasTimer: Boolean
        get() = currentStep?.durationMinutes != null && (currentStep?.durationMinutes ?: 0) > 0

    val timerProgress: Float
        get() = if (timerTotalSeconds > 0) {
            timerRemainingSeconds.toFloat() / timerTotalSeconds
        } else 0f

    val timerDisplayMinutes: Int
        get() = timerRemainingSeconds / 60

    val timerDisplaySeconds: Int
        get() = timerRemainingSeconds % 60

    val timerDisplayText: String
        get() = String.format("%02d:%02d", timerDisplayMinutes, timerDisplaySeconds)
}

/**
 * Navigation events from Cooking Mode screen
 */
sealed class CookingModeNavigationEvent {
    data object NavigateBack : CookingModeNavigationEvent()
    data object NavigateToHome : CookingModeNavigationEvent()
}

@HiltViewModel
class CookingModeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedStateHandle[Screen.CookingMode.ARG_RECIPE_ID])

    private val _uiState = MutableStateFlow(CookingModeUiState())
    val uiState: StateFlow<CookingModeUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<CookingModeNavigationEvent>(Channel.BUFFERED)
    val navigationEvent: Flow<CookingModeNavigationEvent> = _navigationEvent.receiveAsFlow()

    private var timerJob: Job? = null

    // Callbacks for sound/vibration (set by the screen)
    var onTimerComplete: (() -> Unit)? = null

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
                                currentStepIndex = 0
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

    // region Step Navigation

    fun nextStep() {
        val currentState = _uiState.value
        if (!currentState.isLastStep) {
            stopTimer()
            _uiState.update {
                it.copy(
                    currentStepIndex = it.currentStepIndex + 1,
                    timerState = TimerState.IDLE,
                    timerRemainingSeconds = 0,
                    timerTotalSeconds = 0
                )
            }
        } else {
            // Last step completed - show completion dialog
            _uiState.update { it.copy(showCompletionDialog = true) }
        }
    }

    fun previousStep() {
        val currentState = _uiState.value
        if (!currentState.isFirstStep) {
            stopTimer()
            _uiState.update {
                it.copy(
                    currentStepIndex = it.currentStepIndex - 1,
                    timerState = TimerState.IDLE,
                    timerRemainingSeconds = 0,
                    timerTotalSeconds = 0
                )
            }
        }
    }

    fun goToStep(index: Int) {
        val totalSteps = _uiState.value.totalSteps
        if (index in 0 until totalSteps) {
            stopTimer()
            _uiState.update {
                it.copy(
                    currentStepIndex = index,
                    timerState = TimerState.IDLE,
                    timerRemainingSeconds = 0,
                    timerTotalSeconds = 0
                )
            }
        }
    }

    // endregion

    // region Timer

    fun startTimer() {
        val currentStep = _uiState.value.currentStep ?: return
        val durationMinutes = currentStep.durationMinutes ?: return

        val totalSeconds = durationMinutes * 60
        _uiState.update {
            it.copy(
                timerState = TimerState.RUNNING,
                timerRemainingSeconds = totalSeconds,
                timerTotalSeconds = totalSeconds
            )
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timerRemainingSeconds > 0 &&
                   _uiState.value.timerState == TimerState.RUNNING) {
                delay(1000)
                _uiState.update {
                    it.copy(timerRemainingSeconds = it.timerRemainingSeconds - 1)
                }
            }

            if (_uiState.value.timerRemainingSeconds == 0 &&
                _uiState.value.timerState == TimerState.RUNNING) {
                _uiState.update { it.copy(timerState = TimerState.COMPLETED) }
                onTimerComplete?.invoke()
            }
        }
    }

    fun pauseTimer() {
        if (_uiState.value.timerState == TimerState.RUNNING) {
            timerJob?.cancel()
            _uiState.update { it.copy(timerState = TimerState.PAUSED) }
        }
    }

    fun resumeTimer() {
        if (_uiState.value.timerState == TimerState.PAUSED) {
            _uiState.update { it.copy(timerState = TimerState.RUNNING) }

            timerJob = viewModelScope.launch {
                while (_uiState.value.timerRemainingSeconds > 0 &&
                       _uiState.value.timerState == TimerState.RUNNING) {
                    delay(1000)
                    _uiState.update {
                        it.copy(timerRemainingSeconds = it.timerRemainingSeconds - 1)
                    }
                }

                if (_uiState.value.timerRemainingSeconds == 0 &&
                    _uiState.value.timerState == TimerState.RUNNING) {
                    _uiState.update { it.copy(timerState = TimerState.COMPLETED) }
                    onTimerComplete?.invoke()
                }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                timerState = TimerState.IDLE,
                timerRemainingSeconds = 0,
                timerTotalSeconds = 0
            )
        }
    }

    fun dismissTimerComplete() {
        _uiState.update { it.copy(timerState = TimerState.IDLE) }
    }

    // endregion

    // region Voice Guidance

    fun toggleVoiceGuidance() {
        _uiState.update { it.copy(voiceGuidanceEnabled = !it.voiceGuidanceEnabled) }
        Timber.i("Voice guidance toggled: ${_uiState.value.voiceGuidanceEnabled}")
    }

    // endregion

    // region Exit & Completion

    fun requestExit() {
        _uiState.update { it.copy(showExitConfirmation = true) }
    }

    fun dismissExitConfirmation() {
        _uiState.update { it.copy(showExitConfirmation = false) }
    }

    fun confirmExit() {
        stopTimer()
        _uiState.update { it.copy(showExitConfirmation = false) }
        _navigationEvent.trySend(CookingModeNavigationEvent.NavigateBack)
    }

    fun updateRating(rating: Int) {
        _uiState.update { it.copy(rating = rating) }
    }

    fun updateFeedback(feedback: String) {
        _uiState.update { it.copy(feedback = feedback) }
    }

    fun submitRating() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val recipeId = currentState.recipe?.id ?: return@launch

            recipeRepository.rateRecipe(recipeId, currentState.rating, currentState.feedback)
                .onSuccess { Timber.i("Rating submitted: ${currentState.rating} stars for recipe $recipeId") }
                .onFailure { Timber.e(it, "Failed to submit rating for $recipeId") }

            // Record cooking activity for streak tracking
            statsRepository.recordCookedMeal()
                .onSuccess { Timber.i("Cooking activity recorded") }
                .onFailure { Timber.w(it, "Failed to record cooking activity (non-fatal)") }

            _uiState.update { it.copy(showCompletionDialog = false) }
            _navigationEvent.send(CookingModeNavigationEvent.NavigateToHome)
        }
    }

    fun skipRating() {
        _uiState.update { it.copy(showCompletionDialog = false) }
        _navigationEvent.trySend(CookingModeNavigationEvent.NavigateToHome)
    }

    // endregion

    // region Error Handling

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
