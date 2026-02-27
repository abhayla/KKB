package com.rasoiai.app.presentation.auth

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import com.rasoiai.data.sync.SyncWorker
import com.rasoiai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
 * UI state for the Auth screen (Phone OTP flow).
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val phoneNumber: String = "",
    val isPhoneValid: Boolean = false,
    val otpSent: Boolean = false,
    val otpCode: String = "",
    val verificationId: String? = null,
    val resendCountdownSeconds: Int = 0,
    val isVerifying: Boolean = false,
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
    private val phoneAuthClient: PhoneAuthClientInterface,
    private val authRepository: AuthRepository,
    private val userPreferencesDataStore: UserPreferencesDataStoreInterface
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<AuthNavigationEvent>()
    val navigationEvent: Flow<AuthNavigationEvent> = _navigationEvent.receiveAsFlow()

    private var countdownJob: Job? = null

    init {
        // Check if already signed in
        if (phoneAuthClient.isSignedIn) {
            _uiState.update { it.copy(isSignedIn = true) }
            navigateAfterSignIn()
        }
    }

    /**
     * Updates the phone number field and validates it.
     */
    fun updatePhoneNumber(phone: String) {
        // Only allow digits, max 10
        val cleaned = phone.filter { it.isDigit() }.take(10)
        _uiState.update {
            it.copy(
                phoneNumber = cleaned,
                isPhoneValid = cleaned.length == 10,
                errorMessage = null
            )
        }
    }

    /**
     * Send OTP to the entered phone number.
     */
    fun sendOtp(activity: Activity) {
        val phone = _uiState.value.phoneNumber
        if (phone.length != 10) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid 10-digit phone number") }
            return
        }
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val fullNumber = "+91$phone"
            when (val result = phoneAuthClient.sendOtp(fullNumber, activity)) {
                is PhoneAuthResult.CodeSent -> {
                    Timber.i("OTP sent to $fullNumber")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            otpSent = true,
                            verificationId = result.verificationId
                        )
                    }
                    startResendCountdown()
                }
                is PhoneAuthResult.AutoVerified -> {
                    Timber.i("Phone auto-verified for $fullNumber")
                    exchangeFirebaseToken(result.userData.firebaseIdToken)
                }
                is PhoneAuthResult.Error -> {
                    Timber.e("OTP send error: ${result.message}")
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
     * Updates the OTP code field.
     */
    fun updateOtpCode(code: String) {
        val cleaned = code.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(otpCode = cleaned, errorMessage = null) }
    }

    /**
     * Verify the entered OTP code.
     */
    fun verifyOtp() {
        val state = _uiState.value
        val verificationId = state.verificationId ?: return
        val code = state.otpCode
        if (code.length != 6) {
            _uiState.update { it.copy(errorMessage = "Please enter the 6-digit OTP") }
            return
        }
        if (state.isVerifying) return

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, errorMessage = null) }

            when (val result = phoneAuthClient.verifyOtp(verificationId, code)) {
                is OtpVerificationResult.Success -> {
                    Timber.i("OTP verified successfully")
                    exchangeFirebaseToken(result.userData.firebaseIdToken)
                }
                is OtpVerificationResult.Error -> {
                    Timber.e("OTP verification error: ${result.message}")
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Resend OTP to the same phone number.
     */
    fun resendOtp(activity: Activity) {
        if (_uiState.value.resendCountdownSeconds > 0) return
        _uiState.update { it.copy(otpCode = "") }
        sendOtp(activity)
    }

    /**
     * Go back to phone input screen.
     */
    fun goBack() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                otpSent = false,
                otpCode = "",
                verificationId = null,
                resendCountdownSeconds = 0,
                isVerifying = false,
                errorMessage = null
            )
        }
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Exchange Firebase ID token for backend JWT.
     */
    private suspend fun exchangeFirebaseToken(firebaseToken: String) {
        authRepository.signInWithFirebase(firebaseToken)
            .onSuccess { user ->
                Timber.i("Backend auth successful: ${user.phoneNumber ?: user.email}")
                _uiState.update { it.copy(isLoading = false, isVerifying = false, isSignedIn = true) }
                SyncWorker.triggerImmediateSync(appContext)
                navigateAfterSignIn()
            }
            .onFailure { error ->
                Timber.e(error, "Backend auth failed")
                val message = when {
                    error.message?.contains("409") == true ->
                        "Account conflict. Please try again."
                    error.message?.contains("timeout", ignoreCase = true) == true ->
                        "Server timeout. Please check your connection and try again."
                    else ->
                        "Sign in failed. Please check your connection and try again."
                }
                _uiState.update {
                    it.copy(isLoading = false, isVerifying = false, errorMessage = message)
                }
            }
    }

    private fun navigateAfterSignIn() {
        viewModelScope.launch {
            val isOnboarded = userPreferencesDataStore.isOnboarded.first()
            val event = if (isOnboarded) {
                AuthNavigationEvent.NavigateToHome
            } else {
                AuthNavigationEvent.NavigateToOnboarding
            }
            _navigationEvent.send(event)
        }
    }

    private fun startResendCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in 30 downTo 0) {
                _uiState.update { it.copy(resendCountdownSeconds = i) }
                if (i > 0) delay(1000)
            }
        }
    }
}
