package com.rasoiai.app.presentation.splash

import app.cash.turbine.test
import com.rasoiai.core.network.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeNetworkMonitor: FakeNetworkMonitor

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeNetworkMonitor = FakeNetworkMonitor()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Initial state should be loading")
    fun `initial state should be loading`() = runTest {
        val viewModel = SplashViewModel(fakeNetworkMonitor)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("After delay, should navigate to auth when not logged in")
    fun `after delay should navigate to auth when not logged in`() = runTest {
        val viewModel = SplashViewModel(fakeNetworkMonitor)

        viewModel.uiState.test {
            // Skip initial state
            awaitItem()

            // Advance time to complete splash delay
            testDispatcher.scheduler.advanceTimeBy(2000)

            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertNotNull(finalState.navigationEvent)
            assertEquals(
                SplashNavigationEvent.NavigateToAuth,
                finalState.navigationEvent
            )
        }
    }

    @Test
    @DisplayName("isOnline should reflect network state")
    fun `isOnline should reflect network state`() = runTest {
        val viewModel = SplashViewModel(fakeNetworkMonitor)

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
    @DisplayName("onNavigationHandled should clear navigation event")
    fun `onNavigationHandled should clear navigation event`() = runTest {
        val viewModel = SplashViewModel(fakeNetworkMonitor)

        viewModel.uiState.test {
            // Skip initial state
            awaitItem()

            // Advance time to trigger navigation
            testDispatcher.scheduler.advanceTimeBy(2000)

            val stateWithNavigation = awaitItem()
            assertNotNull(stateWithNavigation.navigationEvent)

            // Clear navigation event
            viewModel.onNavigationHandled()

            val stateAfterHandled = awaitItem()
            assertEquals(null, stateAfterHandled.navigationEvent)
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
