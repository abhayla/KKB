package com.rasoiai.app.presentation.auth

import android.content.Context
import app.cash.turbine.test
import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import com.rasoiai.domain.repository.AuthRepository
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
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockContext: Context
    private lateinit var mockPhoneAuthClient: PhoneAuthClientInterface
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockUserPreferencesDataStore: UserPreferencesDataStoreInterface

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockContext = mockk(relaxed = true)
        mockPhoneAuthClient = mockk(relaxed = true)
        mockAuthRepository = mockk(relaxed = true)
        mockUserPreferencesDataStore = mockk(relaxed = true)
        every { mockUserPreferencesDataStore.isOnboarded } returns flowOf(false)
        every { mockPhoneAuthClient.isSignedIn } returns false
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Initial state should not be loading and not signed in")
    fun `initial state should not be loading and not signed in`() = runTest {
        val viewModel = AuthViewModel(mockContext, mockPhoneAuthClient, mockAuthRepository, mockUserPreferencesDataStore)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertFalse(state.isSignedIn)
            assertNull(state.errorMessage)
            assertEquals("", state.phoneNumber)
            assertFalse(state.otpSent)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("updatePhoneNumber validates 10-digit Indian number")
    fun `updatePhoneNumber validates 10-digit Indian number`() = runTest {
        val viewModel = AuthViewModel(mockContext, mockPhoneAuthClient, mockAuthRepository, mockUserPreferencesDataStore)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.updatePhoneNumber("98765")
            val partial = awaitItem()
            assertEquals("98765", partial.phoneNumber)
            assertFalse(partial.isPhoneValid)

            viewModel.updatePhoneNumber("9876543210")
            val valid = awaitItem()
            assertEquals("9876543210", valid.phoneNumber)
            assertTrue(valid.isPhoneValid)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("clearError should clear error message")
    fun `clearError should clear error message`() = runTest {
        val viewModel = AuthViewModel(mockContext, mockPhoneAuthClient, mockAuthRepository, mockUserPreferencesDataStore)

        viewModel.uiState.test {
            viewModel.clearError()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("goBack should reset to phone input screen")
    fun `goBack should reset to phone input screen`() = runTest {
        val viewModel = AuthViewModel(mockContext, mockPhoneAuthClient, mockAuthRepository, mockUserPreferencesDataStore)

        // goBack from default state keeps otpSent=false, otpCode="", verificationId=null
        viewModel.goBack()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.otpSent)
            assertEquals("", state.otpCode)
            assertNull(state.verificationId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("When already signed in, should set isSignedIn to true")
    fun `when already signed in should set isSignedIn to true`() = runTest {
        every { mockPhoneAuthClient.isSignedIn } returns true

        val viewModel = AuthViewModel(mockContext, mockPhoneAuthClient, mockAuthRepository, mockUserPreferencesDataStore)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isSignedIn)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
