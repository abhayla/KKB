package com.rasoiai.app.presentation.common

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Base ViewModel that provides common functionality for state management.
 *
 * Usage:
 * ```
 * @HiltViewModel
 * class HomeViewModel @Inject constructor(
 *     private val getMealPlanUseCase: GetMealPlanUseCase
 * ) : BaseViewModel<HomeUiState>(HomeUiState()) {
 *
 *     fun loadMealPlan() {
 *         updateState { it.copy(isLoading = true) }
 *         // ... load data
 *     }
 * }
 * ```
 */
abstract class BaseViewModel<T : BaseUiState>(initialState: T) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<T> = _uiState.asStateFlow()

    protected val currentState: T
        get() = _uiState.value

    protected fun updateState(update: (T) -> T) {
        _uiState.update(update)
    }

    protected fun setState(state: T) {
        _uiState.value = state
    }

    /**
     * Call this when user dismisses an error message
     */
    open fun onErrorDismissed() {
        // Subclasses should override to clear error state
    }
}
