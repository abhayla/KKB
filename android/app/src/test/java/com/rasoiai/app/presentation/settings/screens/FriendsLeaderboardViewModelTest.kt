package com.rasoiai.app.presentation.settings.screens

import app.cash.turbine.test
import com.rasoiai.domain.model.CookingStreak
import com.rasoiai.domain.model.LeaderboardEntry
import com.rasoiai.domain.repository.StatsRepository
import java.time.LocalDate
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
@DisplayName("FriendsLeaderboardViewModel")
class FriendsLeaderboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockStatsRepository: StatsRepository
    private lateinit var viewModel: FriendsLeaderboardViewModel

    private val testStreak = CookingStreak(
        currentStreak = 5,
        bestStreak = 10,
        lastCookingDate = null
    )

    private val testLeaderboard = listOf(
        LeaderboardEntry(
            rank = 1,
            userName = "Test User",
            mealsCount = 100,
            isCurrentUser = true
        ),
        LeaderboardEntry(
            rank = 2,
            userName = "Friend User",
            mealsCount = 80,
            isCurrentUser = false
        )
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockStatsRepository = mockk(relaxed = true)
        every { mockStatsRepository.getCookingStreak() } returns flowOf(testStreak)
        coEvery { mockStatsRepository.getLeaderboard(any()) } returns Result.success(testLeaderboard)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): FriendsLeaderboardViewModel {
        return FriendsLeaderboardViewModel(mockStatsRepository).also { viewModel = it }
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
        @DisplayName("after loading, streak populated")
        fun `after loading, streak populated`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertNotNull(state.streak)
                assertEquals(5, state.streak?.currentStreak)
                assertEquals(10, state.streak?.bestStreak)
            }
        }

        @Test
        @DisplayName("after loading, leaderboard populated")
        fun `after loading, leaderboard populated`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals(2, state.leaderboard.size)
                assertEquals("Test User", state.leaderboard[0].userName)
                assertEquals(1, state.leaderboard[0].rank)
                assertEquals("Friend User", state.leaderboard[1].userName)
                assertEquals(2, state.leaderboard[1].rank)
            }
        }
    }

    @Nested
    @DisplayName("Error State")
    inner class ErrorState {

        @Test
        @DisplayName("error state when repository fails")
        fun `error state when repository fails`() = runTest {
            coEvery { mockStatsRepository.getLeaderboard(any()) } returns Result.failure(
                RuntimeException("Network error")
            )
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertNotNull(state.errorMessage)
            }
        }
    }
}
