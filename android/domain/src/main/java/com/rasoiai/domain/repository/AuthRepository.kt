package com.rasoiai.domain.repository

import com.rasoiai.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /**
     * Check if user is currently authenticated.
     */
    val isAuthenticated: Flow<Boolean>

    /**
     * Get the current user.
     */
    val currentUser: Flow<User?>

    /**
     * Sign in with Google (Firebase Auth).
     */
    suspend fun signInWithGoogle(idToken: String): Result<User>

    /**
     * Sign out the current user.
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Get the current access token for API calls.
     */
    suspend fun getAccessToken(): String?

    /**
     * Refresh the access token if expired.
     */
    suspend fun refreshToken(): Result<String>
}
