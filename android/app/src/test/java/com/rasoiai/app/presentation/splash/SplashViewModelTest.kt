package com.rasoiai.app.presentation.splash

import app.cash.turbine.test
import com.rasoiai.app.presentation.auth.GoogleAuthClientInterface
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeNetworkMonitor: FakeNetworkMonitor
    private lateinit var mockUserPreferencesDataStore: UserPreferencesDataStoreInterface
    private lateinit var mockGoogleAuthClient: GoogleAuthClientInterface

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeNetworkMonitor = FakeNetworkMonitor()
        mockUserPreferencesDataStore = mockk(relaxed = true)
        mockGoogleAuthClient = mockk(relaxed = true)
        every { mockUserPreferencesDataStore.isOnboarded } returns flowOf(false)
        every { mockGoogleAuthClient.isSignedIn } returns false
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Initial state should be loading")
    fun `initial state should be loading`() = runTest {
        val viewModel = SplashViewModel(fakeNetworkMonitor, mockUserPreferencesDataStore, mockGoogleAuthClient)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("isOnline should reflect network state")
    fun `isOnline should reflect network state`() = runTest {
        val viewModel = SplashViewModel(fakeNetworkMonitor, mockUserPreferencesDataStore, mockGoogleAuthClient)

        viewModel.isOnline.test {
            // Initial state is online
            assertTrue(awaitItem())

            // Simulate going offline
            fakeNetworkMonitor.setOnline(false)
            assertFalse(awaitItem())

            // Simulate going back online
            fakeNetworkMonitor.setOnline(true)
            assertTrue(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("After delay, isLoading should be false")
    fun `after delay isLoading should be false`() = runTest {
        val viewModel = SplashViewModel(fakeNetworkMonitor, mockUserPreferencesDataStore, mockGoogleAuthClient)

        viewModel.uiState.test {
            // Initial state
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)

            // Advance time to complete splash delay (2 seconds + buffer)
            testDispatcher.scheduler.advanceTimeBy(2500)

            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Navigation event should be emitted after delay")
    fun `navigation event should be emitted after delay`() = runTest {
        val viewModel = SplashViewModel(fakeNetworkMonitor, mockUserPreferencesDataStore, mockGoogleAuthClient)

        // Advance time to trigger navigation
        testDispatcher.scheduler.advanceTimeBy(2500)

        viewModel.navigationEvent.test {
            // Note: FirebaseAuth.getInstance() returns null in tests,
            // so it should navigate to Auth
            val event = awaitItem()
            assertTrue(event is SplashNavigationEvent)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

/**
 * Fake NetworkMonitor for testing
 */
class FakeNetworkMonitor : NetworkMonitor {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: Flow<Boolean> = _isOnline

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }
}
