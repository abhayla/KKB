package com.rasoiai.app.presentation.splash

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Initial state should be loading")
    fun `initial state should be loading`() = runTest {
        val viewModel = SplashViewModel()

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("After delay, should navigate to auth when not logged in")
    fun `after delay should navigate to auth when not logged in`() = runTest {
        val viewModel = SplashViewModel()

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
}
