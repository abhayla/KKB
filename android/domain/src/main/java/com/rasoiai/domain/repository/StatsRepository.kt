package com.rasoiai.domain.repository

import com.rasoiai.domain.model.Achievement
import com.rasoiai.domain.model.CookingDay
import com.rasoiai.domain.model.CookingStreak
import com.rasoiai.domain.model.LeaderboardEntry
import com.rasoiai.domain.model.MonthlyStats
import com.rasoiai.domain.model.WeeklyChallenge
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

/**
 * Repository interface for cooking statistics and gamification features.
 */
interface StatsRepository {

    /**
     * Get the user's current cooking streak.
     */
    fun getCookingStreak(): Flow<CookingStreak>

    /**
     * Get monthly statistics for the specified month.
     */
    suspend fun getMonthlyStats(yearMonth: YearMonth): Result<MonthlyStats>

    /**
     * Get cooking days for the specified month (for calendar display).
     */
    suspend fun getCookingDays(yearMonth: YearMonth): Result<List<CookingDay>>

    /**
     * Get all achievements (both locked and unlocked).
     */
    fun getAchievements(): Flow<List<Achievement>>

    /**
     * Get the current weekly challenge.
     */
    fun getWeeklyChallenge(): Flow<WeeklyChallenge?>

    /**
     * Join the current weekly challenge.
     */
    suspend fun joinChallenge(challengeId: String): Result<Unit>

    /**
     * Get leaderboard entries (top performers).
     */
    suspend fun getLeaderboard(limit: Int = 10): Result<List<LeaderboardEntry>>

    /**
     * Record that a meal was cooked today.
     */
    suspend fun recordCookedMeal(): Result<Unit>
}
