package com.rasoiai.app.presentation.settings.screens

import app.cash.turbine.test
import com.rasoiai.domain.model.AppSettings
import com.rasoiai.domain.repository.SettingsRepository
import io.mockk.coEvery
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
@DisplayName("NotificationSettingsViewModel")
class NotificationSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockSettingsRepository: SettingsRepository
    private lateinit var viewModel: NotificationSettingsViewModel

    private val testSettings = AppSettings(
        notificationsEnabled = true
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSettingsRepository = mockk(relaxed = true)
        every { mockSettingsRepository.getAppSettings() } returns flowOf(testSettings)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): NotificationSettingsViewModel {
        return NotificationSettingsViewModel(mockSettingsRepository).also { viewModel = it }
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
        @DisplayName("after loading, notification settings populated")
        fun `after loading, notification settings populated`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertTrue(state.notificationsEnabled)
                assertTrue(state.mealReminders)
                assertTrue(state.shoppingReminders)
                assertTrue(state.cookingReminders)
                assertTrue(state.festivalSuggestions)
                assertTrue(state.achievementNotifications)
            }
        }
    }

    @Nested
    @DisplayName("Toggle Actions")
    inner class ToggleActions {

        @Test
        @DisplayName("toggleMasterNotifications disables all")
        fun `toggleMasterNotifications disables all`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.toggleMasterNotifications(false)
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.notificationsEnabled)
            }
        }

        @Test
        @DisplayName("toggleMealReminders toggles meal reminders")
        fun `toggleMealReminders toggles meal reminders`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.toggleMealReminders(false)
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.mealReminders)
            }
        }

        @Test
        @DisplayName("toggleShoppingReminders toggles shopping reminders")
        fun `toggleShoppingReminders toggles shopping reminders`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.toggleShoppingReminders(false)
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.shoppingReminders)
            }
        }

        @Test
        @DisplayName("toggleCookingReminders toggles cooking reminders")
        fun `toggleCookingReminders toggles cooking reminders`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.toggleCookingReminders(false)
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.cookingReminders)
            }
        }
    }
}
