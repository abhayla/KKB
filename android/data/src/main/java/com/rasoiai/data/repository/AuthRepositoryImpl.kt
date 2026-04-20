package com.rasoiai.data.repository

import com.rasoiai.data.local.datastore.SecureTokenStorage
import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.AuthRequest
import com.rasoiai.data.remote.dto.RefreshTokenRequest
import com.rasoiai.data.remote.mapper.toDomain
import com.rasoiai.data.remote.mapper.toUser
import com.rasoiai.domain.model.User
import com.rasoiai.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository that:
 * 1. Exchanges Firebase token for backend JWT
 * 2. Stores tokens in both EncryptedSharedPreferences (primary) and DataStore (fallback)
 * 3. Provides auth state streams
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: RasoiApiService,
    private val userPreferencesDataStore: UserPreferencesDataStoreInterface,
    private val secureTokenStorage: SecureTokenStorage
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)

    override val isAuthenticated: Flow<Boolean> = userPreferencesDataStore.isAuthenticated

    override val currentUser: Flow<User?> = combine(
        userPreferencesDataStore.isAuthenticated,
        _currentUser.asStateFlow()
    ) { isAuth, user ->
        if (isAuth) user else null
    }

    override suspend fun signInWithFirebase(idToken: String): Result<User> {
        return try {
            Timber.d("Exchanging Firebase token for backend JWT")

            // Call backend API to exchange Firebase token for our JWT
            val authResponse = apiService.authenticateWithFirebase(
                AuthRequest(firebaseToken = idToken)
            )

            // Store tokens in DataStore (fallback)
            userPreferencesDataStore.saveAuthTokens(
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken,
                expiresInSeconds = authResponse.expiresIn,
                userId = authResponse.user.id
            )

            // Store tokens in encrypted storage (primary)
            val expiresAt = System.currentTimeMillis() + (authResponse.expiresIn * 1000)
            secureTokenStorage.saveTokens(
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken,
                expiresAt = expiresAt
            )

            // Save email for Settings profile display (optional with phone auth)
            if (!authResponse.user.email.isNullOrBlank()) {
                userPreferencesDataStore.saveEmail(authResponse.user.email)
            }

            // Map to domain user and cache
            val user = authResponse.toUser()
            _currentUser.value = user

            Timber.i("Successfully authenticated user: ${user.email}")
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "HTTP ${e.code()} on Firebase token exchange")
            Result.failure(e)
        } catch (e: IOException) {
            Timber.w(e, "Network error on Firebase token exchange")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to exchange Firebase token for backend JWT")
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            // Clear tokens from both stores
            userPreferencesDataStore.clearAuthTokens()
            secureTokenStorage.clearTokens()
            _currentUser.value = null

            Timber.i("User signed out successfully")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Timber.w(e, "IO error on sign out")
            Result.failure(e)
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
                Timber.w("No refresh token available")
                return Result.failure(Exception("No refresh token available"))
            }

            Timber.d("Refreshing access token")

            // Call backend API to refresh the access token
            val response = apiService.refreshToken(
                RefreshTokenRequest(refreshToken = refreshToken)
            )

            // Update only the access token in DataStore (fallback)
            userPreferencesDataStore.updateAccessToken(
                accessToken = response.accessToken,
                expiresInSeconds = response.expiresIn
            )

            // Update access token in encrypted storage (primary)
            // Preserve the existing refresh token in secure storage
            val existingRefreshToken = secureTokenStorage.getRefreshToken() ?: refreshToken
            val expiresAt = System.currentTimeMillis() + (response.expiresIn * 1000)
            secureTokenStorage.saveTokens(
                accessToken = response.accessToken,
                refreshToken = existingRefreshToken,
                expiresAt = expiresAt
            )

            Timber.i("Successfully refreshed access token")
            Result.success(response.accessToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "HTTP ${e.code()} on token refresh")
            Result.failure(e)
        } catch (e: IOException) {
            Timber.w(e, "Network error on token refresh")
            Result.failure(e)
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "HTTP ${e.code()} on load current user")
            Result.failure(e)
        } catch (e: IOException) {
            Timber.w(e, "Network error on load current user")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load current user")
            Result.failure(e)
        }
    }
}
