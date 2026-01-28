package com.rasoiai.app.e2e.di

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import com.rasoiai.app.presentation.auth.GoogleAuthClientInterface
import com.rasoiai.app.presentation.auth.GoogleSignInResult
import com.rasoiai.app.presentation.auth.SignInUserData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake Google Auth Client for E2E testing.
 * Implements GoogleAuthClientInterface and auto-returns success.
 * No need to mock FirebaseUser since SignInUserData is a simple data class.
 */
@Singleton
class FakeGoogleAuthClient @Inject constructor() : GoogleAuthClientInterface {

    private var _isSignedIn = false
    private var shouldSucceed = true
    private var errorMessage = "Sign-in failed"

    // Fake user data
    private val fakeUserId = "fake-user-id"
    private val fakeUserEmail = "test@example.com"
    private val fakeUserName = "Test User"
    private val fakeFirebaseToken = "fake-firebase-token"

    override val currentUser: FirebaseUser?
        get() = null // Not used in tests since we check isSignedIn

    override val isSignedIn: Boolean
        get() = _isSignedIn

    override suspend fun signIn(
        activityContext: Context,
        webClientId: String
    ): GoogleSignInResult {
        return if (shouldSucceed) {
            _isSignedIn = true
            val userData = SignInUserData(
                userId = fakeUserId,
                email = fakeUserEmail,
                displayName = fakeUserName,
                photoUrl = null,
                firebaseIdToken = fakeFirebaseToken
            )
            GoogleSignInResult.Success(userData)
        } else {
            GoogleSignInResult.Error(errorMessage)
        }
    }

    override suspend fun signOut() {
        _isSignedIn = false
    }

    // Test control methods
    fun setSignInSuccess() {
        shouldSucceed = true
    }

    fun setSignInFailure(error: Exception) {
        shouldSucceed = false
        errorMessage = error.message ?: "Sign-in failed"
    }

    fun simulateSignedIn() {
        _isSignedIn = true
    }

    fun reset() {
        _isSignedIn = false
        shouldSucceed = true
        errorMessage = "Sign-in failed"
    }
}
