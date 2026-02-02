package com.rasoiai.data.repository

import com.rasoiai.domain.model.Achievement
import com.rasoiai.domain.model.CookingDay
import com.rasoiai.domain.model.CookingStreak
import com.rasoiai.domain.model.LeaderboardEntry
import com.rasoiai.domain.model.MonthlyStats
import com.rasoiai.domain.model.WeeklyChallenge
import com.rasoiai.domain.repository.StatsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of StatsRepository for development and testing.
 */
@Singleton
class FakeStatsRepository @Inject constructor() : StatsRepository {

    private val _cookingStreak = MutableStateFlow(
        CookingStreak(
            currentStreak = 12,
            bestStreak = 23,
            lastCookingDate = LocalDate.now()
        )
    )

    private val _achievements = MutableStateFlow(createMockAchievements())

    private val _weeklyChallenge = MutableStateFlow<WeeklyChallenge?>(
        WeeklyChallenge(
            id = "south-indian-week",
            name = "South Indian Week",
            description = "Cook 5 South Indian dishes",
            targetCount = 5,
            currentProgress = 2,
            rewardBadge = "Explorer Badge",
            isJoined = false
        )
    )

    override fun getCookingStreak(): Flow<CookingStreak> = _cookingStreak.asStateFlow()

    override suspend fun getMonthlyStats(yearMonth: YearMonth): Result<MonthlyStats> {
        delay(300) // Simulate network delay
        return Result.success(
            MonthlyStats(
                mealsCooked = 45,
                newRecipes = 12,
                averageRating = 4.2f
            )
        )
    }

    override suspend fun getCookingDays(yearMonth: YearMonth): Result<List<CookingDay>> {
        delay(200)
        val days = mutableListOf<CookingDay>()
        val today = LocalDate.now()

        // Generate cooking days for the month
        var date = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        while (!date.isAfter(endDate)) {
            val didCook = when {
                date.isAfter(today) -> false
                date == today -> true
                // Random cooking pattern for past days (more cooking on weekends)
                date.dayOfWeek.value >= 6 -> true
                else -> date.dayOfMonth % 3 != 0 // Cook 2 out of 3 days
            }

            days.add(
                CookingDay(
                    date = date,
                    didCook = didCook,
                    mealsCount = if (didCook) (1..3).random() else 0
                )
            )
            date = date.plusDays(1)
        }

        return Result.success(days)
    }

    override fun getAchievements(): Flow<List<Achievement>> = _achievements.asStateFlow()

    override fun getWeeklyChallenge(): Flow<WeeklyChallenge?> = _weeklyChallenge.asStateFlow()

    override suspend fun joinChallenge(challengeId: String): Result<Unit> {
        delay(300)
        _weeklyChallenge.value = _weeklyChallenge.value?.copy(isJoined = true)
        return Result.success(Unit)
    }

    override suspend fun getLeaderboard(limit: Int): Result<List<LeaderboardEntry>> {
        delay(300)
        return Result.success(
            listOf(
                LeaderboardEntry(
                    rank = 1,
                    userName = "Anjali M.",
                    mealsCount = 18,
                    isCurrentUser = false
                ),
                LeaderboardEntry(
                    rank = 2,
                    userName = "You (Priya)",
                    mealsCount = 15,
                    isCurrentUser = true
                ),
                LeaderboardEntry(
                    rank = 3,
                    userName = "Meera S.",
                    mealsCount = 14,
                    isCurrentUser = false
                )
            ).take(limit)
        )
    }

    override suspend fun recordCookedMeal(): Result<Unit> {
        delay(200)
        val current = _cookingStreak.value
        _cookingStreak.value = current.copy(
            currentStreak = current.currentStreak + 1,
            lastCookingDate = LocalDate.now()
        )
        return Result.success(Unit)
    }

    override suspend fun recordCookedRecipe(
        recipeId: String,
        recipeName: String,
        cuisineType: String
    ): Result<Unit> {
        delay(200)
        // Fake implementation - just record the meal
        return recordCookedMeal()
    }

    override suspend fun getCuisineBreakdown(): Result<List<Pair<String, Int>>> {
        delay(200)
        return Result.success(
            listOf(
                "North" to 18,
                "South" to 12,
                "East" to 6,
                "West" to 9
            )
        )
    }

    private fun createMockAchievements(): List<Achievement> = listOf(
        Achievement(
            id = "first-meal",
            name = "First Meal",
            description = "Cook your first meal",
            emoji = "🏅",
            isUnlocked = true,
            unlockedDate = LocalDate.now().minusDays(30)
        ),
        Achievement(
            id = "7-day-streak",
            name = "7-Day Streak",
            description = "Cook for 7 consecutive days",
            emoji = "🥇",
            isUnlocked = true,
            unlockedDate = LocalDate.now().minusDays(15)
        ),
        Achievement(
            id = "master-chef",
            name = "Master Chef",
            description = "Cook 25 different recipes",
            emoji = "👨‍🍳",
            isUnlocked = true,
            unlockedDate = LocalDate.now().minusDays(7)
        ),
        Achievement(
            id = "50-meals",
            name = "50 Meals",
            description = "Cook 50 total meals",
            emoji = "🌟",
            isUnlocked = true,
            unlockedDate = LocalDate.now().minusDays(3)
        ),
        Achievement(
            id = "100-meals",
            name = "Century",
            description = "Cook 100 total meals",
            emoji = "💯",
            isUnlocked = false,
            unlockedDate = null
        ),
        Achievement(
            id = "30-day-streak",
            name = "Monthly Master",
            description = "Cook for 30 consecutive days",
            emoji = "🏆",
            isUnlocked = false,
            unlockedDate = null
        )
    )
}
