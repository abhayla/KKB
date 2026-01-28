package com.rasoiai.data.local.datastore

import com.rasoiai.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Interface for UserPreferencesDataStore to allow test faking.
 */
interface UserPreferencesDataStoreInterface {
    val isAuthenticated: Flow<Boolean>
    val accessToken: Flow<String?>
    val userId: Flow<String?>
    val isOnboarded: Flow<Boolean>
    val userPreferences: Flow<UserPreferences?>

    suspend fun saveAuthTokens(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
        userId: String
    )

    suspend fun getAccessTokenSync(): String?
    suspend fun getRefreshToken(): String?
    suspend fun clearAuthTokens()
    suspend fun saveOnboardingComplete(preferences: UserPreferences)
    suspend fun clearPreferences()
}
