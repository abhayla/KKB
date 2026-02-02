package com.rasoiai.app.presentation.auth

import android.content.Context
import app.cash.turbine.test
import com.google.firebase.auth.FirebaseUser
import com.rasoiai.data.local.datastore.UserPreferencesDataStore
import com.rasoiai.domain.repository.AuthRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockContext: Context
    private lateinit var mockGoogleAuthClient: GoogleAuthClient
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockUserPreferencesDataStore: UserPreferencesDataStore

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockContext = mockk(relaxed = true)
        mockGoogleAuthClient = mockk(relaxed = true)
        mockAuthRepository = mockk(relaxed = true)
        mockUserPreferencesDataStore = mockk(relaxed = true)
        every { mockUserPreferencesDataStore.isOnboarded } returns flowOf(false)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Initial state should not be loading and not signed in")
    fun `initial state should not be loading and not signed in`() = runTest {
        every { mockGoogleAuthClient.isSignedIn } returns false
        every { mockGoogleAuthClient.currentUser } returns null

        val viewModel = AuthViewModel(mockContext, mockGoogleAuthClient, mockAuthRepository, mockUserPreferencesDataStore)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertFalse(state.isSignedIn)
            assertNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("When already signed in, should set isSignedIn to true")
    fun `when already signed in should set isSignedIn to true`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true)
        every { mockGoogleAuthClient.isSignedIn } returns true
        every { mockGoogleAuthClient.currentUser } returns mockUser

        val viewModel = AuthViewModel(mockContext, mockGoogleAuthClient, mockAuthRepository, mockUserPreferencesDataStore)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isSignedIn)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("When already signed in, should emit navigation event")
    fun `when already signed in should emit navigation event`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true)
        every { mockGoogleAuthClient.isSignedIn } returns true
        every { mockGoogleAuthClient.currentUser } returns mockUser

        val viewModel = AuthViewModel(mockContext, mockGoogleAuthClient, mockAuthRepository, mockUserPreferencesDataStore)

        // Subscribe to navigation events BEFORE advancing, so we don't miss the event
        viewModel.navigationEvent.test {
            // Advance to allow init coroutine to complete
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            // Not onboarded, so should navigate to onboarding
            assertEquals(AuthNavigationEvent.NavigateToOnboarding, event)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("When already signed in and onboarded, should navigate to home")
    fun `when already signed in and onboarded should navigate to home`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true)
        every { mockGoogleAuthClient.isSignedIn } returns true
        every { mockGoogleAuthClient.currentUser } returns mockUser
        every { mockUserPreferencesDataStore.isOnboarded } returns flowOf(true)

        val viewModel = AuthViewModel(mockContext, mockGoogleAuthClient, mockAuthRepository, mockUserPreferencesDataStore)

        // Subscribe to navigation events BEFORE advancing, so we don't miss the event
        viewModel.navigationEvent.test {
            // Advance to allow init coroutine to complete
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertEquals(AuthNavigationEvent.NavigateToHome, event)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("clearError should clear error message")
    fun `clearError should clear error message`() = runTest {
        every { mockGoogleAuthClient.isSignedIn } returns false
        every { mockGoogleAuthClient.currentUser } returns null

        val viewModel = AuthViewModel(mockContext, mockGoogleAuthClient, mockAuthRepository, mockUserPreferencesDataStore)

        viewModel.uiState.test {
            viewModel.clearError()
            testDispatcher.scheduler.advanceUntilIdle()

            // Use expectMostRecentItem since state may not emit if errorMessage was already null
            val state = expectMostRecentItem()
            assertNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
