package com.rasoiai.app.presentation.auth

import app.cash.turbine.test
import com.google.firebase.auth.FirebaseAuth
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
    private lateinit var mockFirebaseAuth: FirebaseAuth
    private lateinit var mockGoogleAuthClient: GoogleAuthClient
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockUserPreferencesDataStore: UserPreferencesDataStore

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockFirebaseAuth = mockk(relaxed = true)
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

        val viewModel = AuthViewModel(mockGoogleAuthClient, mockAuthRepository, mockUserPreferencesDataStore)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertFalse(state.isSignedIn)
            assertNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("When already signed in, should navigate to onboarding")
    fun `when already signed in should navigate to onboarding`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true)
        every { mockGoogleAuthClient.isSignedIn } returns true
        every { mockGoogleAuthClient.currentUser } returns mockUser

        val viewModel = AuthViewModel(mockGoogleAuthClient, mockAuthRepository, mockUserPreferencesDataStore)

        // Advance to allow init to complete
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isSignedIn)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.navigationEvent.test {
            val event = awaitItem()
            assertEquals(AuthNavigationEvent.NavigateToOnboarding, event)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("clearError should clear error message")
    fun `clearError should clear error message`() = runTest {
        every { mockGoogleAuthClient.isSignedIn } returns false
        every { mockGoogleAuthClient.currentUser } returns null

        val viewModel = AuthViewModel(mockGoogleAuthClient, mockAuthRepository, mockUserPreferencesDataStore)

        viewModel.uiState.test {
            awaitItem() // Initial state

            // Manually trigger an error state would require mocking sign-in
            // For now, just verify clearError clears any error
            viewModel.clearError()

            val state = awaitItem()
            assertNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("onNavigationHandled should clear navigation event")
    fun `onNavigationHandled should clear navigation event`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true)
        every { mockGoogleAuthClient.isSignedIn } returns true
        every { mockGoogleAuthClient.currentUser } returns mockUser

        val viewModel = AuthViewModel(mockGoogleAuthClient, mockAuthRepository, mockUserPreferencesDataStore)

        // Advance to allow navigation event to be set
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.navigationEvent.test {
            // Skip to the navigation event
            val event = awaitItem()
            assertEquals(AuthNavigationEvent.NavigateToOnboarding, event)

            viewModel.onNavigationHandled()

            val clearedEvent = awaitItem()
            assertNull(clearedEvent)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
