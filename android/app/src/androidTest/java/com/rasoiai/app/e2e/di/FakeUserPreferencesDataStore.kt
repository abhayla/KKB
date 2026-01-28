package com.rasoiai.app.e2e.di

import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.SpiceLevel
import com.rasoiai.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of UserPreferencesDataStore for E2E testing.
 * Allows tests to control the onboarding and authentication state.
 */
@Singleton
class FakeUserPreferencesDataStore @Inject constructor() : UserPreferencesDataStoreInterface {

    private val _isOnboarded = MutableStateFlow(false)
    private val _accessToken = MutableStateFlow<String?>(null)
    private val _userId = MutableStateFlow<String?>(null)
    private val _tokenExpiry = MutableStateFlow(0L)
    private val _userPreferences = MutableStateFlow<UserPreferences?>(null)

    override val isOnboarded: Flow<Boolean> = _isOnboarded

    override val isAuthenticated: Flow<Boolean> = _accessToken.map { token ->
        !token.isNullOrEmpty() && System.currentTimeMillis() < _tokenExpiry.value
    }

    override val accessToken: Flow<String?> = _accessToken

    override val userId: Flow<String?> = _userId

    override val userPreferences: Flow<UserPreferences?> = _userPreferences

    override suspend fun saveAuthTokens(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
        userId: String
    ) {
        _accessToken.value = accessToken
        _tokenExpiry.value = System.currentTimeMillis() + (expiresInSeconds * 1000)
        _userId.value = userId
    }

    override suspend fun getAccessTokenSync(): String? {
        return if (System.currentTimeMillis() < _tokenExpiry.value) {
            _accessToken.value
        } else {
            null
        }
    }

    override suspend fun getRefreshToken(): String? = null

    override suspend fun clearAuthTokens() {
        _accessToken.value = null
        _tokenExpiry.value = 0L
        _userId.value = null
    }

    override suspend fun saveOnboardingComplete(preferences: UserPreferences) {
        _isOnboarded.value = true
        _userPreferences.value = preferences
    }

    override suspend fun clearPreferences() {
        _isOnboarded.value = false
        _accessToken.value = null
        _tokenExpiry.value = 0L
        _userId.value = null
        _userPreferences.value = null
    }

    // ========== Test Control Methods ==========

    /**
     * Simulate a fully onboarded user state.
     * Call this for tests that need to start at Home screen.
     */
    fun simulateOnboarded() {
        _isOnboarded.value = true
        _userPreferences.value = createDefaultPreferences()
    }

    /**
     * Simulate a new user state (not onboarded).
     * Call this for tests that test the onboarding flow.
     */
    fun simulateNewUser() {
        _isOnboarded.value = false
        _userPreferences.value = null
    }

    /**
     * Reset all state to defaults.
     */
    fun reset() {
        _isOnboarded.value = false
        _accessToken.value = null
        _tokenExpiry.value = 0L
        _userId.value = null
        _userPreferences.value = null
    }

    private fun createDefaultPreferences(): UserPreferences = UserPreferences(
        householdSize = 4,
        familyMembers = emptyList(),
        primaryDiet = PrimaryDiet.VEGETARIAN,
        dietaryRestrictions = emptyList(),
        cuisinePreferences = listOf(CuisineType.NORTH),
        spiceLevel = SpiceLevel.MEDIUM,
        dislikedIngredients = emptyList(),
        weekdayCookingTimeMinutes = 30,
        weekendCookingTimeMinutes = 60,
        busyDays = emptyList()
    )
}
