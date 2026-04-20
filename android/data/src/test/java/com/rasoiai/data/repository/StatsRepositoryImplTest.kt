package com.rasoiai.data.repository

import app.cash.turbine.test
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.dao.StatsDao
import com.rasoiai.data.local.entity.AchievementEntity
import com.rasoiai.data.local.entity.CookingDayEntity
import com.rasoiai.data.local.entity.CookingStreakEntity
import com.rasoiai.data.local.entity.WeeklyChallengeEntity
import com.rasoiai.data.remote.api.RasoiApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class StatsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockStatsDao: StatsDao
    private lateinit var mockApiService: RasoiApiService
    private lateinit var mockNetworkMonitor: NetworkMonitor
    private lateinit var repository: StatsRepositoryImpl

    private val testStreakEntity = CookingStreakEntity(
        id = "user_streak",
        currentStreak = 5,
        bestStreak = 10,
        lastCookingDate = "2026-01-27"
    )

    private val testAchievementEntity = AchievementEntity(
        id = "first_meal",
        name = "First Meal",
        description = "Cook your first meal",
        emoji = "👨‍🍳",
        isUnlocked = true,
        unlockedDate = "2026-01-20"
    )

    private val testCookingDayEntity = CookingDayEntity(
        date = "2026-01-27",
        didCook = true,
        mealsCount = 2
    )

    private val testChallengeEntity = WeeklyChallengeEntity(
        id = "weekly_2026-01-27",
        name = "Home Chef Week",
        description = "Cook 5 homemade meals this week",
        targetCount = 5,
        currentProgress = 2,
        rewardBadge = "👨‍🍳",
        isJoined = true,
        weekStartDate = "2026-01-27",
        weekEndDate = "2026-02-02"
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockStatsDao = mockk(relaxed = true)
        mockApiService = mockk(relaxed = true)
        mockNetworkMonitor = mockk(relaxed = true)

        repository = StatsRepositoryImpl(
            statsDao = mockStatsDao,
            apiService = mockApiService,
            networkMonitor = mockNetworkMonitor
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("getCookingStreak")
    inner class GetCookingStreak {

        @Test
        @DisplayName("Should return cooking streak from DAO")
        fun `should return cooking streak from DAO`() = runTest {
            // Given
            every { mockStatsDao.getCookingStreak() } returns flowOf(testStreakEntity)
            coEvery { mockStatsDao.getCookingStreakSync() } returns testStreakEntity

            // When & Then
            repository.getCookingStreak().test {
                val streak = awaitItem()

                assertEquals(5, streak.currentStreak)
                assertEquals(10, streak.bestStreak)
                assertNotNull(streak.lastCookingDate)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return default streak when none exists")
        fun `should return default streak when none exists`() = runTest {
            // Given
            every { mockStatsDao.getCookingStreak() } returns flowOf(null)
            coEvery { mockStatsDao.getCookingStreakSync() } returns null

            // When & Then
            repository.getCookingStreak().test {
                val streak = awaitItem()

                assertEquals(0, streak.currentStreak)
                assertEquals(0, streak.bestStreak)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("getMonthlyStats")
    inner class GetMonthlyStats {

        @Test
        @DisplayName("Should fetch from API when online")
        fun `should fetch from API when online`() = runTest {
            // Given
            val yearMonth = YearMonth.of(2026, 1)
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.getMonthlyStats(any()) } returns mapOf(
                "meals_cooked" to 15,
                "new_recipes" to 5,
                "average_rating" to 4.5f
            )

            // When
            val result = repository.getMonthlyStats(yearMonth)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(15, result.getOrNull()?.mealsCooked)
            assertEquals(5, result.getOrNull()?.newRecipes)
        }

        @Test
        @DisplayName("Should calculate from local when offline")
        fun `should calculate from local when offline`() = runTest {
            // Given
            val yearMonth = YearMonth.of(2026, 1)
            every { mockNetworkMonitor.isOnline } returns flowOf(false)
            coEvery { mockStatsDao.getTotalMealsForMonth(any(), any()) } returns 12
            coEvery { mockStatsDao.getCookingDaysCountForMonth(any(), any()) } returns 10

            // When
            val result = repository.getMonthlyStats(yearMonth)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(12, result.getOrNull()?.mealsCooked)
        }
    }

    @Nested
    @DisplayName("getCookingDays")
    inner class GetCookingDays {

        @Test
        @DisplayName("Should return cooking days for month")
        fun `should return cooking days for month`() = runTest {
            // Given
            val yearMonth = YearMonth.of(2026, 1)
            coEvery { mockStatsDao.getCookingDaysForMonth(any(), any()) } returns listOf(testCookingDayEntity)

            // When
            val result = repository.getCookingDays(yearMonth)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(31, result.getOrNull()?.size) // All days in January
        }
    }

    @Nested
    @DisplayName("getAchievements")
    inner class GetAchievements {

        @Test
        @DisplayName("Should return achievements from DAO")
        fun `should return achievements from DAO`() = runTest {
            // Given
            every { mockStatsDao.getAllAchievements() } returns flowOf(listOf(testAchievementEntity))

            // When & Then
            repository.getAchievements().test {
                val achievements = awaitItem()

                assertEquals(1, achievements.size)
                assertEquals("First Meal", achievements.first().name)
                assertTrue(achievements.first().isUnlocked)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return default achievements when empty")
        fun `should return default achievements when empty`() = runTest {
            // Given
            every { mockStatsDao.getAllAchievements() } returns flowOf(emptyList())

            // When & Then
            repository.getAchievements().test {
                val achievements = awaitItem()

                assertTrue(achievements.isNotEmpty())
                assertEquals("First Meal", achievements.first().name)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("getWeeklyChallenge")
    inner class GetWeeklyChallenge {

        @Test
        @DisplayName("Should return weekly challenge from DAO")
        fun `should return weekly challenge from DAO`() = runTest {
            // Given
            every { mockStatsDao.getCurrentWeeklyChallenge() } returns flowOf(testChallengeEntity)
            coEvery { mockStatsDao.getCurrentWeeklyChallengeSync() } returns testChallengeEntity

            // When & Then
            repository.getWeeklyChallenge().test {
                val challenge = awaitItem()

                assertNotNull(challenge)
                assertEquals("Home Chef Week", challenge?.name)
                assertEquals(5, challenge?.targetCount)
                assertEquals(2, challenge?.currentProgress)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("joinChallenge")
    inner class JoinChallenge {

        @Test
        @DisplayName("Should update challenge joined status")
        fun `should update challenge joined status`() = runTest {
            // When
            val result = repository.joinChallenge("weekly_2026-01-27")

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockStatsDao.updateChallengeJoinedStatus("weekly_2026-01-27", true) }
        }
    }

    @Nested
    @DisplayName("getLeaderboard")
    inner class GetLeaderboard {

        @Test
        @DisplayName("Should return leaderboard entries")
        fun `should return leaderboard entries`() = runTest {
            // When
            val result = repository.getLeaderboard(5)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(5, result.getOrNull()?.size)
            assertEquals(1, result.getOrNull()?.first()?.rank)
        }
    }

    @Nested
    @DisplayName("recordCookedMeal")
    inner class RecordCookedMeal {

        @Test
        @DisplayName("Should record meal and update streak")
        fun `should record meal and update streak`() = runTest {
            // Given
            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            coEvery { mockStatsDao.getCookingDayByDate(today) } returns null
            coEvery { mockStatsDao.getCookingStreakSync() } returns testStreakEntity
            every { mockStatsDao.getAllAchievements() } returns flowOf(listOf(testAchievementEntity))
            coEvery { mockStatsDao.getCurrentWeeklyChallengeSync() } returns testChallengeEntity

            // When
            val result = repository.recordCookedMeal()

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockStatsDao.insertCookingDay(any()) }
            coVerify { mockStatsDao.insertCookingStreak(any()) }
        }

        @Test
        @DisplayName("Should increment existing day meals count")
        fun `should increment existing day meals count`() = runTest {
            // Given
            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            coEvery { mockStatsDao.getCookingDayByDate(today) } returns testCookingDayEntity.copy(date = today)
            coEvery { mockStatsDao.getCookingStreakSync() } returns testStreakEntity
            every { mockStatsDao.getAllAchievements() } returns flowOf(listOf(testAchievementEntity))
            coEvery { mockStatsDao.getCurrentWeeklyChallengeSync() } returns null

            // When
            val result = repository.recordCookedMeal()

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockStatsDao.insertCookingDay(match { it.mealsCount == 3 }) }
        }
    }

    @Nested
    @DisplayName("CancellationException propagation (structured concurrency)")
    inner class CancellationPropagation {

        @Test
        @DisplayName("joinChallenge should propagate CancellationException instead of wrapping in Result.failure")
        fun `joinChallenge should propagate CancellationException`() = runTest {
            coEvery { mockStatsDao.updateChallengeJoinedStatus(any(), any()) } throws CancellationException("cancelled")
            try {
                repository.joinChallenge("weekly_2026-01-27")
                fail("Expected CancellationException to propagate, got Result wrapper instead")
            } catch (e: CancellationException) {
                assertEquals("cancelled", e.message)
            }
        }
    }
}
