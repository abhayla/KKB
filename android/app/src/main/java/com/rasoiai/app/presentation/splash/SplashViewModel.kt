package com.rasoiai.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.core.network.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashUiState(
    val isLoading: Boolean = true,
    val navigationEvent: SplashNavigationEvent? = null
)

sealed class SplashNavigationEvent {
    data object NavigateToAuth : SplashNavigationEvent()
    data object NavigateToOnboarding : SplashNavigationEvent()
    data object NavigateToHome : SplashNavigationEvent()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    networkMonitor: NetworkMonitor
    // TODO: Inject use cases for checking auth state and onboarding status
    // private val checkAuthStateUseCase: CheckAuthStateUseCase,
    // private val checkOnboardingCompleteUseCase: CheckOnboardingCompleteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    init {
        checkInitialState()
    }

    private fun checkInitialState() {
        viewModelScope.launch {
            // Splash delay for branding (2 seconds as per wireframe spec)
            delay(2000)

            // TODO: Replace with actual auth check
            // val isLoggedIn = checkAuthStateUseCase()
            // val isOnboarded = checkOnboardingCompleteUseCase()
            val isLoggedIn = false // Placeholder
            val isOnboarded = false // Placeholder

            val navigationEvent = when {
                !isLoggedIn -> SplashNavigationEvent.NavigateToAuth
                !isOnboarded -> SplashNavigationEvent.NavigateToOnboarding
                else -> SplashNavigationEvent.NavigateToHome
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    navigationEvent = navigationEvent
                )
            }
        }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigationEvent = null) }
    }
}
