package com.rasoiai.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.auth.GoogleAuthClientInterface
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import com.rasoiai.domain.repository.MealPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SplashUiState(
    val isLoading: Boolean = true
)

sealed class SplashNavigationEvent {
    data object NavigateToAuth : SplashNavigationEvent()
    data object NavigateToOnboarding : SplashNavigationEvent()
    data object NavigateToHome : SplashNavigationEvent()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    networkMonitor: NetworkMonitor,
    private val userPreferencesDataStore: UserPreferencesDataStoreInterface,
    private val googleAuthClient: GoogleAuthClientInterface,
    private val mealPlanRepository: MealPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<SplashNavigationEvent>()
    val navigationEvent: Flow<SplashNavigationEvent> = _navigationEvent.receiveAsFlow()

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

            // Check if user is logged in via injected auth client (allows test mocking)
            val isLoggedIn = googleAuthClient.isSignedIn

            // Check if onboarding is complete from DataStore
            val isOnboarded = userPreferencesDataStore.isOnboarded.first()

            // Check if meal plan exists in Room (indicates user completed onboarding before)
            // This handles the case where DataStore was cleared but Room still has data
            val hasMealPlan = try {
                mealPlanRepository.hasMealPlanForCurrentWeek()
            } catch (e: Exception) {
                Timber.e(e, "Error checking for existing meal plan")
                false
            }

            Timber.d("SplashViewModel: isLoggedIn=$isLoggedIn, isOnboarded=$isOnboarded, hasMealPlan=$hasMealPlan")

            val event = when {
                !isLoggedIn -> SplashNavigationEvent.NavigateToAuth
                // Navigate to onboarding only if neither flag is set AND no meal plan exists
                !isOnboarded && !hasMealPlan -> SplashNavigationEvent.NavigateToOnboarding
                else -> SplashNavigationEvent.NavigateToHome
            }

            _uiState.update { it.copy(isLoading = false) }
            _navigationEvent.send(event)
        }
    }
}
