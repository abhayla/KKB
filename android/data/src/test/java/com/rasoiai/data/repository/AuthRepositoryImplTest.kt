package com.rasoiai.data.repository

import app.cash.turbine.test
import com.rasoiai.data.local.datastore.SecureTokenStorage
import com.rasoiai.data.local.datastore.UserPreferencesDataStore
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.AuthResponse
import com.rasoiai.data.remote.dto.UserResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockApiService: RasoiApiService
    private lateinit var mockUserPreferencesDataStore: UserPreferencesDataStore
    private lateinit var mockSecureTokenStorage: SecureTokenStorage
    private lateinit var repository: AuthRepositoryImpl

    private val testUserResponse = UserResponse(
        id = "user-1",
        email = "test@example.com",
        name = "Test User",
        profileImageUrl = null,
        isOnboarded = true,
        preferences = null
    )

    private val testAuthResponse = AuthResponse(
        accessToken = "access-token-123",
        refreshToken = "refresh-token-456",
        tokenType = "Bearer",
        expiresIn = 3600,
        user = testUserResponse
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockApiService = mockk(relaxed = true)
        mockUserPreferencesDataStore = mockk(relaxed = true)
        mockSecureTokenStorage = mockk(relaxed = true)

        repository = AuthRepositoryImpl(
            apiService = mockApiService,
            userPreferencesDataStore = mockUserPreferencesDataStore,
            secureTokenStorage = mockSecureTokenStorage
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("isAuthenticated")
    inner class IsAuthenticated {

        @Test
        @DisplayName("Should return true when authenticated")
        fun `should return true when authenticated`() = runTest {
            // Given - must set up mock BEFORE constructing repository (eager flow capture)
            every { mockUserPreferencesDataStore.isAuthenticated } returns flowOf(true)
            repository = AuthRepositoryImpl(apiService = mockApiService, userPreferencesDataStore = mockUserPreferencesDataStore, secureTokenStorage = mockSecureTokenStorage)

            // When & Then
            repository.isAuthenticated.test {
                assertTrue(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return false when not authenticated")
        fun `should return false when not authenticated`() = runTest {
            // Given - must set up mock BEFORE constructing repository (eager flow capture)
            every { mockUserPreferencesDataStore.isAuthenticated } returns flowOf(false)
            repository = AuthRepositoryImpl(apiService = mockApiService, userPreferencesDataStore = mockUserPreferencesDataStore, secureTokenStorage = mockSecureTokenStorage)

            // When & Then
            repository.isAuthenticated.test {
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("currentUser")
    inner class CurrentUser {

        @Test
        @DisplayName("Should return null when not authenticated")
        fun `should return null when not authenticated`() = runTest {
            // Given - must set up mock BEFORE constructing repository (eager flow capture)
            every { mockUserPreferencesDataStore.isAuthenticated } returns flowOf(false)
            repository = AuthRepositoryImpl(apiService = mockApiService, userPreferencesDataStore = mockUserPreferencesDataStore, secureTokenStorage = mockSecureTokenStorage)

            // When — use .first() instead of Turbine test{} to avoid UncompletedCoroutinesError
            // from combine() with never-completing StateFlow
            val user = repository.currentUser.first()

            // Then
            assertNull(user)
        }
    }

    @Nested
    @DisplayName("signInWithFirebase")
    inner class SignInWithFirebase {

        @Test
        @DisplayName("Should exchange Firebase token and store JWT")
        fun `should exchange Firebase token and store JWT`() = runTest {
            // Given
            coEvery { mockApiService.authenticateWithFirebase(any()) } returns testAuthResponse

            // When
            val result = repository.signInWithFirebase("firebase-id-token")

            // Then
            assertTrue(result.isSuccess)
            assertEquals("user-1", result.getOrNull()?.id)
            assertEquals("test@example.com", result.getOrNull()?.email)

            coVerify {
                mockUserPreferencesDataStore.saveAuthTokens(
                    accessToken = "access-token-123",
                    refreshToken = "refresh-token-456",
                    expiresInSeconds = 3600,
                    userId = "user-1"
                )
            }

            // Verify tokens also saved to encrypted storage
            verify {
                mockSecureTokenStorage.saveTokens(
                    accessToken = "access-token-123",
                    refreshToken = "refresh-token-456",
                    expiresAt = any()
                )
            }
        }

        @Test
        @DisplayName("Should return failure on API error")
        fun `should return failure on API error`() = runTest {
            // Given
            coEvery { mockApiService.authenticateWithFirebase(any()) } throws RuntimeException("Network error")

            // When
            val result = repository.signInWithFirebase("firebase-id-token")

            // Then
            assertTrue(result.isFailure)
            assertEquals("Network error", result.exceptionOrNull()?.message)
        }
    }

    @Nested
    @DisplayName("signOut")
    inner class SignOut {

        @Test
        @DisplayName("Should clear tokens and reset user state")
        fun `should clear tokens and reset user state`() = runTest {
            // When
            val result = repository.signOut()

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockUserPreferencesDataStore.clearAuthTokens() }
            verify { mockSecureTokenStorage.clearTokens() }
        }
    }

    @Nested
    @DisplayName("getAccessToken")
    inner class GetAccessToken {

        @Test
        @DisplayName("Should return access token from DataStore")
        fun `should return access token from DataStore`() = runTest {
            // Given
            coEvery { mockUserPreferencesDataStore.getAccessTokenSync() } returns "access-token-123"

            // When
            val token = repository.getAccessToken()

            // Then
            assertEquals("access-token-123", token)
        }

        @Test
        @DisplayName("Should return null when no token stored")
        fun `should return null when no token stored`() = runTest {
            // Given
            coEvery { mockUserPreferencesDataStore.getAccessTokenSync() } returns null

            // When
            val token = repository.getAccessToken()

            // Then
            assertNull(token)
        }
    }

    @Nested
    @DisplayName("refreshToken")
    inner class RefreshToken {

        @Test
        @DisplayName("Should return failure when no refresh token available")
        fun `should return failure when no refresh token available`() = runTest {
            // Given
            coEvery { mockUserPreferencesDataStore.getRefreshToken() } returns null

            // When
            val result = repository.refreshToken()

            // Then
            assertTrue(result.isFailure)
            assertEquals("No refresh token available", result.exceptionOrNull()?.message)
        }

        @Test
        @DisplayName("Should return failure when API refresh fails")
        fun `should return failure when API refresh fails`() = runTest {
            // Given
            coEvery { mockUserPreferencesDataStore.getRefreshToken() } returns "refresh-token"
            coEvery { mockApiService.refreshToken(any()) } throws RuntimeException("Token refresh failed")

            // When
            val result = repository.refreshToken()

            // Then
            assertTrue(result.isFailure)
        }
    }

    @Nested
    @DisplayName("loadCurrentUser")
    inner class LoadCurrentUser {

        @Test
        @DisplayName("Should load user from API when authenticated")
        fun `should load user from API when authenticated`() = runTest {
            // Given
            every { mockUserPreferencesDataStore.isAuthenticated } returns flowOf(true)
            coEvery { mockApiService.getCurrentUser() } returns testUserResponse

            // When
            val result = repository.loadCurrentUser()

            // Then
            assertTrue(result.isSuccess)
            assertEquals("user-1", result.getOrNull()?.id)
        }

        @Test
        @DisplayName("Should return failure when not authenticated")
        fun `should return failure when not authenticated`() = runTest {
            // Given
            every { mockUserPreferencesDataStore.isAuthenticated } returns flowOf(false)

            // When
            val result = repository.loadCurrentUser()

            // Then
            assertTrue(result.isFailure)
            assertEquals("Not authenticated", result.exceptionOrNull()?.message)
        }

        @Test
        @DisplayName("Should return failure on API error")
        fun `should return failure on API error`() = runTest {
            // Given
            every { mockUserPreferencesDataStore.isAuthenticated } returns flowOf(true)
            coEvery { mockApiService.getCurrentUser() } throws RuntimeException("API error")

            // When
            val result = repository.loadCurrentUser()

            // Then
            assertTrue(result.isFailure)
        }
    }
}
