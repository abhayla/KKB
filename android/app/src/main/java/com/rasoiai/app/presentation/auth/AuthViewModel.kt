package com.rasoiai.app.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.app.BuildConfig
import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import com.rasoiai.data.sync.SyncWorker
import com.rasoiai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * UI state for the Auth screen
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSignedIn: Boolean = false
)

/**
 * Navigation events from Auth screen
 */
sealed class AuthNavigationEvent {
    data object NavigateToOnboarding : AuthNavigationEvent()
    data object NavigateToHome : AuthNavigationEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val googleAuthClient: GoogleAuthClientInterface,
    private val authRepository: AuthRepository,
    private val userPreferencesDataStore: UserPreferencesDataStoreInterface
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<AuthNavigationEvent>()
    val navigationEvent: Flow<AuthNavigationEvent> = _navigationEvent.receiveAsFlow()

    init {
        // Check if already signed in
        if (googleAuthClient.isSignedIn) {
            _uiState.update { it.copy(isSignedIn = true) }
            navigateAfterSignIn()
        }
    }

    /**
     * Initiates Google Sign-In flow.
     *
     * @param activityContext Activity context required for Credential Manager
     */
    fun signInWithGoogle(activityContext: Context) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Get the web client ID from BuildConfig
            val webClientId = getWebClientId()

            when (val result = googleAuthClient.signIn(activityContext, webClientId)) {
                is GoogleSignInResult.Success -> {
                    Timber.i("Google sign in successful: ${result.userData.email}")

                    // Exchange Firebase token for backend JWT
                    exchangeFirebaseToken(result.userData.firebaseIdToken)
                }
                is GoogleSignInResult.Cancelled -> {
                    _uiState.update { it.copy(isLoading = false) }
                    // Don't show error for user cancellation
                }
                is GoogleSignInResult.Error -> {
                    Timber.e("Sign in error: ${result.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Exchange Firebase ID token for backend JWT.
     */
    private suspend fun exchangeFirebaseToken(firebaseToken: String) {
        authRepository.signInWithGoogle(firebaseToken)
            .onSuccess { user ->
                Timber.i("Backend auth successful: ${user.email}")
                _uiState.update { it.copy(isLoading = false, isSignedIn = true) }
                SyncWorker.triggerImmediateSync(appContext)
                navigateAfterSignIn()
            }
            .onFailure { error ->
                Timber.w(error, "Backend auth failed, proceeding in offline mode")
                // Still allow the user to proceed - they're authenticated with Firebase
                // Backend sync will happen when network is available
                _uiState.update { it.copy(isLoading = false, isSignedIn = true) }
                navigateAfterSignIn()
            }
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun navigateAfterSignIn() {
        viewModelScope.launch {
            // Check if onboarding is complete from DataStore
            val isOnboarded = userPreferencesDataStore.isOnboarded.first()

            val event = if (isOnboarded) {
                AuthNavigationEvent.NavigateToHome
            } else {
                AuthNavigationEvent.NavigateToOnboarding
            }
            _navigationEvent.send(event)
        }
    }

    private fun getWebClientId(): String {
        // Web Client ID from google-services.json (oauth_client with client_type: 3)
        // Set via buildConfigField in app/build.gradle.kts
        return BuildConfig.WEB_CLIENT_ID
    }
}
