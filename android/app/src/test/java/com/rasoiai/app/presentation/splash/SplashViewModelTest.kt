package com.rasoiai.app.presentation.splash

import app.cash.turbine.test
import com.rasoiai.app.presentation.auth.PhoneAuthClientInterface
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import com.rasoiai.domain.repository.MealPlanRepository
import io.mockk.coEvery
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeNetworkMonitor: FakeNetworkMonitor
    private lateinit var mockUserPreferencesDataStore: UserPreferencesDataStoreInterface
    private lateinit var mockPhoneAuthClient: PhoneAuthClientInterface
    private lateinit var mockMealPlanRepository: MealPlanRepository

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeNetworkMonitor = FakeNetworkMonitor()
        mockUserPreferencesDataStore = mockk(relaxed = true)
        mockPhoneAuthClient = mockk(relaxed = true)
        mockMealPlanRepository = mockk(relaxed = true)
        every { mockUserPreferencesDataStore.isOnboarded } returns flowOf(false)
        every { mockPhoneAuthClient.isSignedIn } returns false
        coEvery { mockMealPlanRepository.hasMealPlanForCurrentWeek() } returns false
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Initial state should be loading")
    fun `initial state should be loading`() = runTest {
        val viewModel = SplashViewModel(fakeNetworkMonitor, mockUserPreferencesDataStore, mockPhoneAuthClient, mockMealPlanRepository)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("isOnline should reflect network state")
    fun `isOnline should reflect network state`() = runTest {
        val viewModel = SplashViewModel(fakeNetworkMonitor, mockUserPreferencesDataStore, mockPhoneAuthClient, mockMealPlanRepository)

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
        val viewModel = SplashViewModel(fakeNetworkMonitor, mockUserPreferencesDataStore, mockPhoneAuthClient, mockMealPlanRepository)

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
        val viewModel = SplashViewModel(fakeNetworkMonitor, mockUserPreferencesDataStore, mockPhoneAuthClient, mockMealPlanRepository)

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

    @Nested
    @DisplayName("Navigation Logic with Meal Plan Check")
    inner class NavigationLogicWithMealPlanCheck {

        @Test
        @DisplayName("Should navigate to Auth when not logged in")
        fun `should navigate to Auth when not logged in`() = runTest {
            every { mockPhoneAuthClient.isSignedIn } returns false

            val viewModel = SplashViewModel(fakeNetworkMonitor, mockUserPreferencesDataStore, mockPhoneAuthClient, mockMealPlanRepository)

            testDispatcher.scheduler.advanceTimeBy(2500)

            viewModel.navigationEvent.test {
                val event = awaitItem()
                assertEquals(SplashNavigationEvent.NavigateToAuth, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should navigate to Onboarding when logged in but not onboarded and no meal plan")
        fun `should navigate to Onboarding when logged in but not onboarded and no meal plan`() = runTest {
            every { mockPhoneAuthClient.isSignedIn } returns true
            every { mockUserPreferencesDataStore.isOnboarded } returns flowOf(false)
            coEvery { mockMealPlanRepository.hasMealPlanForCurrentWeek() } returns false

            val viewModel = SplashViewModel(fakeNetworkMonitor, mockUserPreferencesDataStore, mockPhoneAuthClient, mockMealPlanRepository)

            testDispatcher.scheduler.advanceTimeBy(2500)

            viewModel.navigationEvent.test {
                val event = awaitItem()
                assertEquals(SplashNavigationEvent.NavigateToOnboarding, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should navigate to Home when logged in and onboarded")
        fun `should navigate to Home when logged in and onboarded`() = runTest {
            every { mockPhoneAuthClient.isSignedIn } returns true
            every { mockUserPreferencesDataStore.isOnboarded } returns flowOf(true)

            val viewModel = SplashViewModel(fakeNetworkMonitor, mockUserPreferencesDataStore, mockPhoneAuthClient, mockMealPlanRepository)

            testDispatcher.scheduler.advanceTimeBy(2500)

            viewModel.navigationEvent.test {
                val event = awaitItem()
                assertEquals(SplashNavigationEvent.NavigateToHome, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should navigate to Home when meal plan exists even if isOnboarded is false")
        fun `should navigate to Home when meal plan exists even if isOnboarded is false`() = runTest {
            // This is the key bug fix test - user has meal plan but DataStore was cleared
            every { mockPhoneAuthClient.isSignedIn } returns true
            every { mockUserPreferencesDataStore.isOnboarded } returns flowOf(false) // DataStore was cleared
            coEvery { mockMealPlanRepository.hasMealPlanForCurrentWeek() } returns true // But Room has meal plan

            val viewModel = SplashViewModel(fakeNetworkMonitor, mockUserPreferencesDataStore, mockPhoneAuthClient, mockMealPlanRepository)

            testDispatcher.scheduler.advanceTimeBy(2500)

            viewModel.navigationEvent.test {
                val event = awaitItem()
                // Should go to Home, NOT Onboarding, because meal plan exists
                assertEquals(SplashNavigationEvent.NavigateToHome, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should handle exception when checking meal plan existence")
        fun `should handle exception when checking meal plan existence`() = runTest {
            every { mockPhoneAuthClient.isSignedIn } returns true
            every { mockUserPreferencesDataStore.isOnboarded } returns flowOf(false)
            coEvery { mockMealPlanRepository.hasMealPlanForCurrentWeek() } throws Exception("Database error")

            val viewModel = SplashViewModel(fakeNetworkMonitor, mockUserPreferencesDataStore, mockPhoneAuthClient, mockMealPlanRepository)

            testDispatcher.scheduler.advanceTimeBy(2500)

            viewModel.navigationEvent.test {
                val event = awaitItem()
                // On error, should fall back to Onboarding (hasMealPlan treated as false)
                assertEquals(SplashNavigationEvent.NavigateToOnboarding, event)
                cancelAndIgnoreRemainingEvents()
            }
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
