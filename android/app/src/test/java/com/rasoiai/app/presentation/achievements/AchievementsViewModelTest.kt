package com.rasoiai.app.presentation.achievements

import app.cash.turbine.test
import com.rasoiai.domain.model.Achievement
import com.rasoiai.domain.repository.StatsRepository
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AchievementsViewModel")
class AchievementsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockStatsRepository: StatsRepository
    private lateinit var viewModel: AchievementsViewModel

    private val testAchievements = listOf(
        Achievement(
            id = "first-meal",
            name = "First Meal",
            description = "Cook your first meal",
            emoji = "🍳",
            isUnlocked = true,
            unlockedDate = LocalDate.of(2026, 1, 15)
        ),
        Achievement(
            id = "7-day-streak",
            name = "Week Warrior",
            description = "Complete a 7-day cooking streak",
            emoji = "📅",
            isUnlocked = true,
            unlockedDate = LocalDate.of(2026, 1, 20)
        ),
        Achievement(
            id = "100-meals",
            name = "Master Chef",
            description = "Cook 100 meals",
            emoji = "👨‍🍳",
            isUnlocked = false
        )
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockStatsRepository = mockk(relaxed = true)
        every { mockStatsRepository.getAchievements() } returns flowOf(testAchievements)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AchievementsViewModel {
        return AchievementsViewModel(mockStatsRepository).also { viewModel = it }
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
        @DisplayName("after loading, achievements populated from repository and extras")
        fun `after loading, achievements populated`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                // 3 from repo + 8 extras = 11 total
                assertTrue(state.achievements.size > 3)
                // Verify repo achievements are included
                assertTrue(state.achievements.any { it.achievement.id == "first-meal" })
                assertTrue(state.achievements.any { it.achievement.id == "7-day-streak" })
                assertTrue(state.achievements.any { it.achievement.id == "100-meals" })
            }
        }
    }

    @Nested
    @DisplayName("Clear Error")
    inner class ClearError {

        @Test
        @DisplayName("clearError clears error message")
        fun `clearError clears error message`() = runTest {
            every { mockStatsRepository.getAchievements() } returns flowOf(emptyList())
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.clearError()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertNull(state.errorMessage)
            }
        }
    }

    @Nested
    @DisplayName("Computed Properties")
    inner class ComputedProperties {

        @Test
        @DisplayName("computed properties filter unlocked and locked correctly")
        fun `computed properties filter unlocked and locked correctly`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.unlockedAchievements.isNotEmpty())
                assertTrue(state.lockedAchievements.isNotEmpty())
                assertEquals(state.achievements.size, state.totalCount)
                assertEquals(state.unlockedAchievements.size, state.unlockedCount)
                assertTrue(state.unlockedAchievements.all { it.achievement.isUnlocked })
                assertTrue(state.lockedAchievements.none { it.achievement.isUnlocked })
            }
        }

        @Test
        @DisplayName("completionText shows correct format")
        fun `completionText shows correct format`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                val expected = "${state.unlockedCount} / ${state.totalCount} Unlocked"
                assertEquals(expected, state.completionText)
            }
        }
    }
}
