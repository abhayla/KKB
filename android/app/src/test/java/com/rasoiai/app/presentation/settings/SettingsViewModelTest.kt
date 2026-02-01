package com.rasoiai.app.presentation.settings

import app.cash.turbine.test
import com.rasoiai.domain.model.AppSettings
import com.rasoiai.domain.model.DarkModePreference
import com.rasoiai.domain.model.User
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockSettingsRepository: SettingsRepository

    private val testUser = User(
        id = "user-1",
        email = "test@example.com",
        name = "Test User",
        profileImageUrl = null,
        isOnboarded = true,
        preferences = null
    )

    private val testAppSettings = AppSettings(
        darkMode = DarkModePreference.SYSTEM,
        notificationsEnabled = true
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSettingsRepository = mockk(relaxed = true)
        every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)
        every { mockSettingsRepository.getAppSettings() } returns flowOf(testAppSettings)
        every { mockSettingsRepository.getAppVersion() } returns "1.2.3"
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        @DisplayName("Initial state should be loading")
        fun `initial state should be loading`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("After loading, user should be populated")
        fun `after loading user should be populated`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isLoading)
                assertEquals(testUser, state.user)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("App version should be loaded")
        fun `app version should be loaded`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("1.2.3", state.appVersion)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("App settings should be loaded")
        fun `app settings should be loaded`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals(testAppSettings, state.appSettings)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Dark Mode Dialog")
    inner class DarkModeDialog {

        @Test
        @DisplayName("showDarkModeDialog should show dialog")
        fun `showDarkModeDialog should show dialog`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showDarkModeDialog()

                val state = awaitItem()
                assertTrue(state.showDarkModeDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissDarkModeDialog should hide dialog")
        fun `dismissDarkModeDialog should hide dialog`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showDarkModeDialog()
                awaitItem()

                viewModel.dismissDarkModeDialog()

                val state = awaitItem()
                assertFalse(state.showDarkModeDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onDarkModeSelected should update and dismiss")
        fun `onDarkModeSelected should update and dismiss`() = runTest {
            coEvery { mockSettingsRepository.updateDarkMode(any()) } returns Result.success(Unit)

            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showDarkModeDialog()
                awaitItem()

                viewModel.onDarkModeSelected(DarkModePreference.DARK)

                val state = awaitItem()
                assertFalse(state.showDarkModeDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Sign Out Dialog")
    inner class SignOutDialog {

        @Test
        @DisplayName("showSignOutDialog should show dialog")
        fun `showSignOutDialog should show dialog`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showSignOutDialog()

                val state = awaitItem()
                assertTrue(state.showSignOutDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissSignOutDialog should hide dialog")
        fun `dismissSignOutDialog should hide dialog`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showSignOutDialog()
                awaitItem()

                viewModel.dismissSignOutDialog()

                val state = awaitItem()
                assertFalse(state.showSignOutDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onSignOutConfirmed should set isSigningOut and emit navigation")
        fun `onSignOutConfirmed should set isSigningOut and emit navigation`() = runTest {
            coEvery { mockSettingsRepository.signOut() } returns Result.success(Unit)

            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showSignOutDialog()
                awaitItem()

                viewModel.onSignOutConfirmed()

                testDispatcher.scheduler.advanceUntilIdle()

                val signingOutState = expectMostRecentItem()
                assertTrue(signingOutState.isSigningOut)
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.navigationEvent.test {
                viewModel.onSignOutConfirmed()
                testDispatcher.scheduler.advanceUntilIdle()
                val event = awaitItem()
                assertEquals(SettingsNavigationEvent.NavigateToAuth, event)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        @DisplayName("navigateBack should emit back event")
        fun `navigateBack should emit back event`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateBack()
                val event = awaitItem()
                assertEquals(SettingsNavigationEvent.NavigateBack, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onEditProfileClick should emit edit profile event")
        fun `onEditProfileClick should emit edit profile event`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.navigationEvent.test {
                viewModel.onEditProfileClick()
                val event = awaitItem()
                assertEquals(SettingsNavigationEvent.NavigateToEditProfile, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onRecipeRulesClick should emit recipe rules event")
        fun `onRecipeRulesClick should emit recipe rules event`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.navigationEvent.test {
                viewModel.onRecipeRulesClick()
                val event = awaitItem()
                assertEquals(SettingsNavigationEvent.NavigateToRecipeRules, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onPrivacyPolicyClick should emit privacy policy event")
        fun `onPrivacyPolicyClick should emit privacy policy event`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.navigationEvent.test {
                viewModel.onPrivacyPolicyClick()
                val event = awaitItem()
                assertEquals(SettingsNavigationEvent.NavigateToPrivacyPolicy, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onEditFamilyMemberClick should emit edit member event with id")
        fun `onEditFamilyMemberClick should emit edit member event with id`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.navigationEvent.test {
                viewModel.onEditFamilyMemberClick("member-123")
                val event = awaitItem()
                assertTrue(event is SettingsNavigationEvent.NavigateToEditFamilyMember)
                assertEquals("member-123", (event as SettingsNavigationEvent.NavigateToEditFamilyMember).memberId)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Computed Properties")
    inner class ComputedProperties {

        @Test
        @DisplayName("userName should return user name or Guest")
        fun `userName should return user name or Guest`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("Test User", state.userName)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("userEmail should return user email")
        fun `userEmail should return user email`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("test@example.com", state.userEmail)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("clearError should clear error message")
        fun `clearError should clear error message`() = runTest {
            val viewModel = SettingsViewModel(mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.clearError()

                val state = awaitItem()
                assertNull(state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
