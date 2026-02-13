package com.rasoiai.app.presentation.settings.screens

import app.cash.turbine.test
import com.rasoiai.domain.repository.SettingsRepository
import com.rasoiai.domain.model.User
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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("ConnectedAccountsViewModel")
class ConnectedAccountsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockSettingsRepository: SettingsRepository
    private lateinit var viewModel: ConnectedAccountsViewModel

    private val testUser = User(
        id = "user-1", email = "test@example.com", name = "Test User",
        profileImageUrl = null, isOnboarded = true, preferences = null
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSettingsRepository = mockk(relaxed = true)
        every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ConnectedAccountsViewModel {
        return ConnectedAccountsViewModel(mockSettingsRepository).also { viewModel = it }
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        @DisplayName("initial state is loading")
        fun `initial state is loading`() = runTest {
            val vm = createViewModel()
            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.isLoading)
            }
        }
    }

    @Nested
    @DisplayName("Loading")
    inner class Loading {

        @Test
        @DisplayName("after loading, google email populated")
        fun `after loading, google email populated`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals("test@example.com", state.googleEmail)
            }
        }

        @Test
        @DisplayName("after loading, google name populated")
        fun `after loading, google name populated`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals("Test User", state.googleName)
            }
        }
    }
}
