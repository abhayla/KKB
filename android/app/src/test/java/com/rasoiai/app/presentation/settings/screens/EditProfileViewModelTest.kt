package com.rasoiai.app.presentation.settings.screens

import app.cash.turbine.test
import com.rasoiai.domain.repository.SettingsRepository
import com.rasoiai.domain.model.User
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
@DisplayName("EditProfileViewModel")
class EditProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockSettingsRepository: SettingsRepository
    private lateinit var viewModel: EditProfileViewModel

    private val testUser = User(
        id = "user-1", email = "test@example.com", name = "Test User",
        profileImageUrl = null, isOnboarded = true, preferences = null
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSettingsRepository = mockk(relaxed = true)
        every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)
        coEvery { mockSettingsRepository.updateUserPreferences(any()) } returns Result.success(Unit)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): EditProfileViewModel {
        return EditProfileViewModel(mockSettingsRepository).also { viewModel = it }
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
        @DisplayName("after loading, name and email populated")
        fun `after loading, name and email populated`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals("Test User", state.name)
                assertEquals("test@example.com", state.email)
            }
        }
    }

    @Nested
    @DisplayName("Update Name")
    inner class UpdateName {

        @Test
        @DisplayName("updateName changes name")
        fun `updateName changes name`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.updateName("New Name")
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertEquals("New Name", state.name)
            }
        }

        @Test
        @DisplayName("email is read-only and not changed by updateName")
        fun `email is read-only and not changed by updateName`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.updateName("Different Name")
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertEquals("Different Name", state.name)
                assertEquals("test@example.com", state.email)
            }
        }
    }

    @Nested
    @DisplayName("Save")
    inner class Save {

        @Test
        @DisplayName("save success sets saveSuccess true")
        fun `save success sets saveSuccess true`() = runTest {
            coEvery { mockSettingsRepository.updateUserPreferences(any()) } returns Result.success(Unit)
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.save()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.saveSuccess)
                assertNull(state.errorMessage)
            }
        }

        @Test
        @DisplayName("save with blank name sets errorMessage")
        fun `save with blank name sets errorMessage`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.updateName("   ")
            testDispatcher.scheduler.advanceUntilIdle()

            vm.save()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.saveSuccess)
                assertNotNull(state.errorMessage)
            }
        }
    }
}
