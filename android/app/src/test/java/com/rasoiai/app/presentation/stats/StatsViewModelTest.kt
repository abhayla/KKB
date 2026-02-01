package com.rasoiai.app.presentation.stats

import app.cash.turbine.test
import com.rasoiai.domain.model.Achievement
import com.rasoiai.domain.model.CookingStreak
import com.rasoiai.domain.model.LeaderboardEntry
import com.rasoiai.domain.model.MonthlyStats
import com.rasoiai.domain.model.WeeklyChallenge
import com.rasoiai.domain.repository.StatsRepository
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
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockStatsRepository: StatsRepository

    private val testStreak = CookingStreak(
        currentStreak = 5,
        bestStreak = 12,
        lastCookingDate = java.time.LocalDate.now()
    )

    private val testAchievements = listOf(
        Achievement(
            id = "ach-1",
            name = "First Cook",
            description = "Cook your first meal",
            emoji = "👨‍🍳",
            isUnlocked = true
        ),
        Achievement(
            id = "ach-2",
            name = "Week Warrior",
            description = "Cook for 7 days straight",
            emoji = "🔥",
            isUnlocked = false
        )
    )

    private val testChallenge = WeeklyChallenge(
        id = "challenge-1",
        name = "South Indian Week",
        description = "Cook 5 South Indian dishes this week",
        targetCount = 5,
        currentProgress = 2,
        rewardBadge = "🏆",
        isJoined = false
    )

    private val testLeaderboard = listOf(
        LeaderboardEntry(rank = 1, userName = "Chef Master", mealsCount = 150, isCurrentUser = false),
        LeaderboardEntry(rank = 2, userName = "Spice King", mealsCount = 120, isCurrentUser = false),
        LeaderboardEntry(rank = 3, userName = "You", mealsCount = 80, isCurrentUser = true)
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockStatsRepository = mockk(relaxed = true)
        every { mockStatsRepository.getCookingStreak() } returns flowOf(testStreak)
        every { mockStatsRepository.getAchievements() } returns flowOf(testAchievements)
        every { mockStatsRepository.getWeeklyChallenge() } returns flowOf(testChallenge)
        coEvery { mockStatsRepository.getMonthlyStats(any()) } returns Result.success(MonthlyStats(mealsCooked = 20, newRecipes = 15, averageRating = 4.5f))
        coEvery { mockStatsRepository.getCookingDays(any()) } returns Result.success(emptyList())
        coEvery { mockStatsRepository.getLeaderboard(any()) } returns Result.success(testLeaderboard)
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
            val viewModel = StatsViewModel(mockStatsRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("After loading, data should be populated")
        fun `after loading data should be populated`() = runTest {
            val viewModel = StatsViewModel(mockStatsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isLoading)
                assertEquals(testStreak, state.cookingStreak)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Achievements should be loaded")
        fun `achievements should be loaded`() = runTest {
            val viewModel = StatsViewModel(mockStatsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals(2, state.achievements.size)
                assertEquals(1, state.unlockedAchievements.size)
                assertEquals(1, state.lockedAchievements.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Selected month should be current month")
        fun `selected month should be current month`() = runTest {
            val viewModel = StatsViewModel(mockStatsRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(YearMonth.now(), state.selectedYearMonth)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Calendar Navigation")
    inner class CalendarNavigation {

        @Test
        @DisplayName("onPreviousMonth should navigate to previous month")
        fun `onPreviousMonth should navigate to previous month`() = runTest {
            val viewModel = StatsViewModel(mockStatsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.onPreviousMonth()

                val state = awaitItem()
                assertEquals(YearMonth.now().minusMonths(1), state.selectedYearMonth)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onNextMonth should not navigate to future month")
        fun `onNextMonth should not navigate to future month`() = runTest {
            val viewModel = StatsViewModel(mockStatsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial (current month)

                viewModel.onNextMonth() // Should not change - already at current month

                // No state change expected
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onTodayClick should navigate to current month")
        fun `onTodayClick should navigate to current month`() = runTest {
            val viewModel = StatsViewModel(mockStatsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.onPreviousMonth()
                awaitItem() // Previous month

                viewModel.onTodayClick()

                val state = awaitItem()
                assertEquals(YearMonth.now(), state.selectedYearMonth)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Challenge Actions")
    inner class ChallengeActions {

        @Test
        @DisplayName("onJoinChallenge should set isJoiningChallenge")
        fun `onJoinChallenge should set isJoiningChallenge`() = runTest {
            coEvery { mockStatsRepository.joinChallenge(any()) } returns Result.success(Unit)

            val viewModel = StatsViewModel(mockStatsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem() // Wait for challenge to load

                viewModel.onJoinChallenge()

                val joiningState = awaitItem()
                assertTrue(joiningState.isJoiningChallenge)
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
            val viewModel = StatsViewModel(mockStatsRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateBack()
                val event = awaitItem()
                assertEquals(StatsNavigationEvent.NavigateBack, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToHome should emit home event")
        fun `navigateToHome should emit home event`() = runTest {
            val viewModel = StatsViewModel(mockStatsRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateToHome()
                val event = awaitItem()
                assertEquals(StatsNavigationEvent.NavigateToHome, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onViewAllAchievements should emit achievements event")
        fun `onViewAllAchievements should emit achievements event`() = runTest {
            val viewModel = StatsViewModel(mockStatsRepository)

            viewModel.navigationEvent.test {
                viewModel.onViewAllAchievements()
                val event = awaitItem()
                assertEquals(StatsNavigationEvent.NavigateToAllAchievements, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onViewFullLeaderboard should emit leaderboard event")
        fun `onViewFullLeaderboard should emit leaderboard event`() = runTest {
            val viewModel = StatsViewModel(mockStatsRepository)

            viewModel.navigationEvent.test {
                viewModel.onViewFullLeaderboard()
                val event = awaitItem()
                assertEquals(StatsNavigationEvent.NavigateToFullLeaderboard, event)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Computed Properties")
    inner class ComputedProperties {

        @Test
        @DisplayName("hasStreak should be true when streak > 0")
        fun `hasStreak should be true when streak greater than 0`() = runTest {
            val viewModel = StatsViewModel(mockStatsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertTrue(state.hasStreak)
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
            val viewModel = StatsViewModel(mockStatsRepository)

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
