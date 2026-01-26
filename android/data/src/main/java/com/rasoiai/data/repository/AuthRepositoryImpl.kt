package com.rasoiai.data.repository

import com.rasoiai.data.local.datastore.UserPreferencesDataStore
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.AuthRequest
import com.rasoiai.data.remote.mapper.toDomain
import com.rasoiai.data.remote.mapper.toUser
import com.rasoiai.domain.model.User
import com.rasoiai.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository that:
 * 1. Exchanges Firebase token for backend JWT
 * 2. Stores tokens in DataStore
 * 3. Provides auth state streams
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: RasoiApiService,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)

    override val isAuthenticated: Flow<Boolean> = userPreferencesDataStore.isAuthenticated

    override val currentUser: Flow<User?> = combine(
        userPreferencesDataStore.isAuthenticated,
        _currentUser.asStateFlow()
    ) { isAuth, user ->
        if (isAuth) user else null
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            Timber.d("Exchanging Firebase token for backend JWT")

            // Call backend API to exchange Firebase token for our JWT
            val authResponse = apiService.authenticateWithFirebase(
                AuthRequest(firebaseToken = idToken)
            )

            // Store tokens in DataStore
            userPreferencesDataStore.saveAuthTokens(
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken,
                expiresInSeconds = authResponse.expiresIn,
                userId = authResponse.user.id
            )

            // Map to domain user and cache
            val user = authResponse.toUser()
            _currentUser.value = user

            Timber.i("Successfully authenticated user: ${user.email}")
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "Failed to exchange Firebase token for backend JWT")
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            // Clear tokens from DataStore
            userPreferencesDataStore.clearAuthTokens()
            _currentUser.value = null

            Timber.i("User signed out successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sign out")
            Result.failure(e)
        }
    }

    override suspend fun getAccessToken(): String? {
        return userPreferencesDataStore.getAccessTokenSync()
    }

    override suspend fun refreshToken(): Result<String> {
        return try {
            val refreshToken = userPreferencesDataStore.getRefreshToken()
            if (refreshToken.isNullOrEmpty()) {
                return Result.failure(Exception("No refresh token available"))
            }

            // TODO: Implement refresh token endpoint when available
            // For now, return failure to trigger re-authentication
            Timber.w("Token refresh not implemented - user needs to re-authenticate")
            Result.failure(Exception("Token refresh not implemented"))
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh token")
            Result.failure(e)
        }
    }

    /**
     * Load user from API if authenticated.
     * Call this on app startup to restore user session.
     */
    suspend fun loadCurrentUser(): Result<User> {
        return try {
            val isAuth = userPreferencesDataStore.isAuthenticated.first()
            if (!isAuth) {
                return Result.failure(Exception("Not authenticated"))
            }

            val userResponse = apiService.getCurrentUser()
            val user = userResponse.toDomain()
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load current user")
            Result.failure(e)
        }
    }
}
