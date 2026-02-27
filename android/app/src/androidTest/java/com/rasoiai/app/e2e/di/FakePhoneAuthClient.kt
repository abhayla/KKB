package com.rasoiai.app.e2e.di

import android.app.Activity
import com.rasoiai.app.presentation.auth.OtpVerificationResult
import com.rasoiai.app.presentation.auth.PhoneAuthClientInterface
import com.rasoiai.app.presentation.auth.PhoneAuthResult
import com.rasoiai.app.presentation.auth.SignInUserData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake Phone Auth Client for E2E testing.
 * Implements PhoneAuthClientInterface and auto-returns success.
 * No need to interact with Firebase since SignInUserData is a simple data class.
 *
 * ## Pre-Initialization Pattern
 * For tests that need to start already signed in, set the static companion
 * object state BEFORE Hilt injection via @BeforeClass.
 */
@Singleton
class FakePhoneAuthClient @Inject constructor() : PhoneAuthClientInterface {

    /**
     * Static configuration that can be set BEFORE Hilt injection.
     * This solves the race condition where activity launches before @Before method runs.
     */
    companion object {
        /**
         * Initial signed-in state to use when this class is instantiated.
         * Set this in @BeforeClass before the test class instantiates.
         */
        var initialSignedIn: Boolean = false

        /**
         * Reset all static state to defaults.
         * Call this in @AfterClass to prevent test pollution.
         */
        fun resetStaticState() {
            initialSignedIn = false
        }
    }

    // Initialize from companion state (set before injection)
    private var _isSignedIn = initialSignedIn
    private var shouldSucceed = true
    private var autoVerify = true
    private var errorMessage = "Sign-in failed"

    // Fake user data
    private val fakeUserId = "fake-user-id"
    private val fakeUserPhone = "+911111111111"
    private val fakeUserEmail = "e2e-test@rasoiai.test"
    private val fakeUserName = "E2E Test User"
    private val fakeFirebaseToken = "fake-firebase-token"
    private val fakeVerificationId = "fake-verification-id"

    override val isSignedIn: Boolean
        get() = _isSignedIn

    override suspend fun sendOtp(phoneNumber: String, activity: Activity): PhoneAuthResult {
        return if (shouldSucceed) {
            if (autoVerify) {
                _isSignedIn = true
                PhoneAuthResult.AutoVerified(createFakeUserData())
            } else {
                PhoneAuthResult.CodeSent(fakeVerificationId)
            }
        } else {
            PhoneAuthResult.Error(errorMessage)
        }
    }

    override suspend fun verifyOtp(verificationId: String, code: String): OtpVerificationResult {
        return if (shouldSucceed) {
            _isSignedIn = true
            OtpVerificationResult.Success(createFakeUserData())
        } else {
            OtpVerificationResult.Error(errorMessage)
        }
    }

    override suspend fun signOut() {
        _isSignedIn = false
    }

    private fun createFakeUserData() = SignInUserData(
        userId = fakeUserId,
        phoneNumber = fakeUserPhone,
        email = fakeUserEmail,
        displayName = fakeUserName,
        photoUrl = null,
        firebaseIdToken = fakeFirebaseToken
    )

    // Test control methods
    fun setSignInSuccess() {
        shouldSucceed = true
    }

    fun setSignInFailure(error: Exception) {
        shouldSucceed = false
        errorMessage = error.message ?: "Sign-in failed"
    }

    fun setAutoVerify(auto: Boolean) {
        autoVerify = auto
    }

    fun simulateSignedIn() {
        _isSignedIn = true
    }

    fun reset() {
        _isSignedIn = false
        shouldSucceed = true
        autoVerify = true
        errorMessage = "Sign-in failed"
    }
}
