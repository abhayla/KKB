package com.rasoiai.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashUiState(
    val isLoading: Boolean = true,
    val navigationEvent: SplashNavigationEvent? = null
)

sealed class SplashNavigationEvent {
    data object NavigateToAuth : SplashNavigationEvent()
    data object NavigateToHome : SplashNavigationEvent()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    // TODO: Inject use cases for checking auth state and onboarding status
    // private val checkAuthStateUseCase: CheckAuthStateUseCase,
    // private val checkOnboardingCompleteUseCase: CheckOnboardingCompleteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkInitialState()
    }

    private fun checkInitialState() {
        viewModelScope.launch {
            // Simulate splash delay for branding
            delay(1500)

            // TODO: Replace with actual auth check
            // val isLoggedIn = checkAuthStateUseCase()
            // val isOnboarded = checkOnboardingCompleteUseCase()
            val isLoggedIn = false // Placeholder

            val navigationEvent = if (isLoggedIn) {
                SplashNavigationEvent.NavigateToHome
            } else {
                SplashNavigationEvent.NavigateToAuth
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    navigationEvent = navigationEvent
                )
            }
        }
    }
}
