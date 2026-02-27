package com.rasoiai.app.e2e.di

import com.rasoiai.domain.model.User
import com.rasoiai.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of AuthRepository for E2E testing.
 * Can be injected and controlled by tests.
 */
@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: Flow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    private var shouldSucceed = true
    private var fakeAccessToken = "fake-access-token"

    override suspend fun signInWithFirebase(idToken: String): Result<User> {
        return if (shouldSucceed) {
            _isAuthenticated.value = true
            val user = createFakeUser()
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(Exception("Authentication failed"))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        _isAuthenticated.value = false
        _currentUser.value = null
        return Result.success(Unit)
    }

    override suspend fun getAccessToken(): String? {
        return if (_isAuthenticated.value) fakeAccessToken else null
    }

    override suspend fun refreshToken(): Result<String> {
        return if (_isAuthenticated.value) {
            fakeAccessToken = "refreshed-fake-access-token-${System.currentTimeMillis()}"
            Result.success(fakeAccessToken)
        } else {
            Result.failure(Exception("Not authenticated"))
        }
    }

    // Test control methods
    fun setAuthSuccess() {
        shouldSucceed = true
    }

    fun setAuthFailure() {
        shouldSucceed = false
    }

    fun simulateSignedIn() {
        _isAuthenticated.value = true
        _currentUser.value = createFakeUser(isOnboarded = false)
    }

    /**
     * Simulate a fully authenticated AND onboarded user.
     * Use this for tests that need to start at the Home screen.
     */
    fun simulateOnboardedUser() {
        _isAuthenticated.value = true
        _currentUser.value = createFakeUser(isOnboarded = true)
    }

    fun clearSession() {
        _isAuthenticated.value = false
        _currentUser.value = null
    }

    fun reset() {
        _isAuthenticated.value = false
        _currentUser.value = null
        shouldSucceed = true
        fakeAccessToken = "fake-access-token"
    }

    private fun createFakeUser(isOnboarded: Boolean = false): User = User(
        id = "fake-user-id",
        email = "test@example.com",
        phoneNumber = "+911111111111",
        name = "Test User",
        profileImageUrl = null,
        isOnboarded = isOnboarded,
        preferences = null
    )
}
