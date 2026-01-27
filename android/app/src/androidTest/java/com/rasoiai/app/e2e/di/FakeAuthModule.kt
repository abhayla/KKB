package com.rasoiai.app.e2e.di

import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import com.rasoiai.domain.model.User
import com.rasoiai.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.InstallIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

/**
 * Test module that provides fake auth components for E2E testing.
 * Bypasses real Google Sign-In.
 *
 * Note: This module is installed alongside other modules and provides fake
 * implementations that tests can inject for controlling auth behavior.
 */
@Module
@InstallIn(SingletonComponent::class)
object FakeAuthModule {

    @Provides
    @Singleton
    fun provideFakeGoogleAuthClient(
        @ApplicationContext context: Context
    ): FakeGoogleAuthClient {
        return FakeGoogleAuthClient()
    }

    @Provides
    @Singleton
    fun provideFakeAuthRepository(): FakeAuthRepository {
        return FakeAuthRepository()
    }
}

/**
 * Fake Google Auth Client for testing.
 * Allows tests to simulate successful/failed sign-in without actual OAuth.
 */
class FakeGoogleAuthClient {

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: Flow<Boolean> = _isSignedIn

    private var shouldSignInSucceed = true
    private var signInError: Exception? = null

    /**
     * Configure sign-in to succeed.
     */
    fun setSignInSuccess() {
        shouldSignInSucceed = true
        signInError = null
    }

    /**
     * Configure sign-in to fail with an error.
     */
    fun setSignInFailure(error: Exception) {
        shouldSignInSucceed = false
        signInError = error
    }

    /**
     * Simulate successful sign-in.
     */
    suspend fun signIn(): Result<String> {
        return if (shouldSignInSucceed) {
            _isSignedIn.value = true
            Result.success("fake-firebase-token")
        } else {
            Result.failure(signInError ?: Exception("Sign-in failed"))
        }
    }

    /**
     * Sign out.
     */
    suspend fun signOut() {
        _isSignedIn.value = false
    }

    /**
     * Get fake sign-in intent.
     */
    suspend fun getSignInIntentSender(): IntentSenderRequest? {
        return null // Not needed for testing
    }

    /**
     * Handle sign-in result.
     */
    suspend fun handleSignInResult(intent: Intent?): Result<String> {
        return signIn()
    }
}

/**
 * Fake Auth Repository for testing.
 * Implements the full AuthRepository interface from domain layer.
 */
class FakeAuthRepository : AuthRepository {

    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: Flow<Boolean> = _isAuthenticated

    private val _currentUserInternal = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUserInternal

    private var shouldAuthSucceed = true
    private var fakeAccessToken = "fake-access-token"

    /**
     * Configure auth to succeed.
     */
    fun setAuthSuccess() {
        shouldAuthSucceed = true
    }

    /**
     * Configure auth to fail.
     */
    fun setAuthFailure() {
        shouldAuthSucceed = false
    }

    /**
     * Simulate authentication.
     */
    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return if (shouldAuthSucceed) {
            _isAuthenticated.value = true
            val user = User(
                id = "fake-user-id",
                email = "test.sharma@gmail.com",
                name = "Test Sharma",
                profileImageUrl = null,
                isOnboarded = false,
                preferences = null
            )
            _currentUserInternal.value = user
            Result.success(user)
        } else {
            Result.failure(Exception("Authentication failed"))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        _isAuthenticated.value = false
        _currentUserInternal.value = null
        return Result.success(Unit)
    }

    override suspend fun getAccessToken(): String? {
        return if (_isAuthenticated.value) fakeAccessToken else null
    }

    override suspend fun refreshToken(): Result<String> {
        return if (_isAuthenticated.value) {
            fakeAccessToken = "refreshed-fake-access-token"
            Result.success(fakeAccessToken)
        } else {
            Result.failure(Exception("Not authenticated"))
        }
    }

    /**
     * Clear session (simulate session expiry).
     */
    fun clearSession() {
        _isAuthenticated.value = false
        _currentUserInternal.value = null
    }
}
