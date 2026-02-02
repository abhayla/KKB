package com.rasoiai.data.repository

import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.dao.StatsDao
import com.rasoiai.data.local.entity.AchievementEntity
import com.rasoiai.data.local.entity.CookedRecipeEntity
import com.rasoiai.data.local.entity.CookingDayEntity
import com.rasoiai.data.local.entity.CookingStreakEntity
import com.rasoiai.data.local.entity.WeeklyChallengeEntity
import java.util.UUID
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.domain.model.Achievement
import com.rasoiai.domain.model.CookingDay
import com.rasoiai.domain.model.CookingStreak
import com.rasoiai.domain.model.LeaderboardEntry
import com.rasoiai.domain.model.MonthlyStats
import com.rasoiai.domain.model.WeeklyChallenge
import com.rasoiai.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of StatsRepository with offline-first architecture.
 *
 * Strategy:
 * - All stats stored locally in Room (single source of truth)
 * - Streak tracking is fully local
 * - Achievements can sync with server when online
 * - Leaderboard requires API call
 */
@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val statsDao: StatsDao,
    private val apiService: RasoiApiService,
    private val networkMonitor: NetworkMonitor
) : StatsRepository {

    companion object {
        private val DEFAULT_ACHIEVEMENTS = listOf(
            AchievementEntity("first_meal", "First Meal", "Cook your first meal", "👨‍🍳", false, null),
            AchievementEntity("week_streak", "Week Warrior", "Cook for 7 days in a row", "🔥", false, null),
            AchievementEntity("month_streak", "Month Master", "Cook for 30 days in a row", "⭐", false, null),
            AchievementEntity("fifty_meals", "Fifty Feasts", "Cook 50 meals total", "🏆", false, null),
            AchievementEntity("five_cuisines", "Cuisine Explorer", "Try recipes from 5 different cuisines", "🌍", false, null),
            AchievementEntity("ten_new_recipes", "Recipe Pioneer", "Try 10 new recipes", "📚", false, null)
        )
    }

    init {
        // Initialize default data on first run
    }

    override fun getCookingStreak(): Flow<CookingStreak> {
        return statsDao.getCookingStreak()
            .onStart { ensureStreakExists() }
            .map { entity ->
                entity?.toDomain() ?: CookingStreak(0, 0, null)
            }
    }

    override suspend fun getMonthlyStats(yearMonth: YearMonth): Result<MonthlyStats> {
        return try {
            val startDate = yearMonth.atDay(1).format(DateTimeFormatter.ISO_DATE)
            val endDate = yearMonth.atEndOfMonth().format(DateTimeFormatter.ISO_DATE)

            // Try to fetch from API if online
            if (networkMonitor.isOnline.first()) {
                try {
                    val monthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    val response = apiService.getMonthlyStats(monthStr)

                    // Parse API response
                    val mealsCooked = (response["meals_cooked"] as? Number)?.toInt() ?: 0
                    val newRecipes = (response["new_recipes"] as? Number)?.toInt() ?: 0
                    val avgRating = (response["average_rating"] as? Number)?.toFloat() ?: 0f

                    return Result.success(MonthlyStats(mealsCooked, newRecipes, avgRating))
                } catch (e: Exception) {
                    Timber.w(e, "Failed to fetch monthly stats from API, using local")
                }
            }

            // Fall back to local calculation
            val totalMeals = statsDao.getTotalMealsForMonth(startDate, endDate) ?: 0
            val cookingDays = statsDao.getCookingDaysCountForMonth(startDate, endDate)

            // Estimate new recipes (simplified - in production would track this)
            val newRecipes = (totalMeals * 0.3).toInt() // Assume 30% are new

            Result.success(MonthlyStats(
                mealsCooked = totalMeals,
                newRecipes = newRecipes,
                averageRating = 4.2f // Default rating
            ))
        } catch (e: Exception) {
            Timber.e(e, "Failed to get monthly stats")
            Result.failure(e)
        }
    }

    override suspend fun getCookingDays(yearMonth: YearMonth): Result<List<CookingDay>> {
        return try {
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val startDateStr = startDate.format(DateTimeFormatter.ISO_DATE)
            val endDateStr = endDate.format(DateTimeFormatter.ISO_DATE)

            val storedDays = statsDao.getCookingDaysForMonth(startDateStr, endDateStr)
            val storedDaysMap = storedDays.associateBy { it.date }

            // Generate all days in the month
            val allDays = generateSequence(startDate) { it.plusDays(1) }
                .takeWhile { !it.isAfter(endDate) }
                .map { date ->
                    val dateStr = date.format(DateTimeFormatter.ISO_DATE)
                    storedDaysMap[dateStr]?.toDomain() ?: CookingDay(date, false, 0)
                }
                .toList()

            Result.success(allDays)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get cooking days")
            Result.failure(e)
        }
    }

    override fun getAchievements(): Flow<List<Achievement>> {
        return statsDao.getAllAchievements()
            .onStart { ensureAchievementsExist() }
            .map { entities ->
                if (entities.isEmpty()) {
                    DEFAULT_ACHIEVEMENTS.map { it.toDomain() }
                } else {
                    entities.map { it.toDomain() }
                }
            }
    }

    override fun getWeeklyChallenge(): Flow<WeeklyChallenge?> {
        return statsDao.getCurrentWeeklyChallenge()
            .onStart { ensureWeeklyChallengeExists() }
            .map { entity -> entity?.toDomain() }
    }

    override suspend fun joinChallenge(challengeId: String): Result<Unit> {
        return try {
            statsDao.updateChallengeJoinedStatus(challengeId, true)
            Timber.i("Joined challenge: $challengeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to join challenge")
            Result.failure(e)
        }
    }

    override suspend fun getLeaderboard(limit: Int): Result<List<LeaderboardEntry>> {
        return try {
            // Leaderboard requires online - return mock data for now
            // In production, this would be an API call
            val entries = listOf(
                LeaderboardEntry(1, "Priya S.", 156, false),
                LeaderboardEntry(2, "Amit K.", 142, false),
                LeaderboardEntry(3, "Neha P.", 138, false),
                LeaderboardEntry(4, "Rahul M.", 125, false),
                LeaderboardEntry(5, "You", 98, true),
                LeaderboardEntry(6, "Anjali R.", 95, false),
                LeaderboardEntry(7, "Vikram J.", 87, false),
                LeaderboardEntry(8, "Meera T.", 82, false),
                LeaderboardEntry(9, "Sanjay D.", 78, false),
                LeaderboardEntry(10, "Kavita B.", 71, false)
            ).take(limit)

            Result.success(entries)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get leaderboard")
            Result.failure(e)
        }
    }

    override suspend fun recordCookedMeal(): Result<Unit> {
        return try {
            val today = LocalDate.now()
            val todayStr = today.format(DateTimeFormatter.ISO_DATE)

            // Update or create cooking day
            val existingDay = statsDao.getCookingDayByDate(todayStr)
            if (existingDay != null) {
                statsDao.insertCookingDay(
                    existingDay.copy(
                        didCook = true,
                        mealsCount = existingDay.mealsCount + 1
                    )
                )
            } else {
                statsDao.insertCookingDay(
                    CookingDayEntity(
                        date = todayStr,
                        didCook = true,
                        mealsCount = 1
                    )
                )
            }

            // Update streak
            updateStreak(today)

            // Check for achievements
            checkAndUpdateAchievements()

            // Update weekly challenge progress
            val challenge = statsDao.getCurrentWeeklyChallengeSync()
            if (challenge?.isJoined == true) {
                statsDao.incrementChallengeProgress(challenge.id)
            }

            Timber.i("Recorded cooked meal for $todayStr")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to record cooked meal")
            Result.failure(e)
        }
    }

    private suspend fun updateStreak(today: LocalDate) {
        val streak = statsDao.getCookingStreakSync() ?: CookingStreakEntity()
        val lastDate = streak.lastCookingDate?.let {
            LocalDate.parse(it, DateTimeFormatter.ISO_DATE)
        }

        val newStreak = when {
            lastDate == null -> 1
            lastDate == today -> streak.currentStreak // Already recorded today
            lastDate == today.minusDays(1) -> streak.currentStreak + 1 // Consecutive
            else -> 1 // Streak broken
        }

        val newBest = maxOf(newStreak, streak.bestStreak)

        statsDao.insertCookingStreak(
            streak.copy(
                currentStreak = newStreak,
                bestStreak = newBest,
                lastCookingDate = today.format(DateTimeFormatter.ISO_DATE)
            )
        )

        Timber.d("Updated streak: $newStreak (best: $newBest)")
    }

    private suspend fun checkAndUpdateAchievements() {
        val streak = statsDao.getCookingStreakSync() ?: return
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_DATE)

        // First meal achievement
        val firstMeal = statsDao.getAchievementById("first_meal")
        if (firstMeal?.isUnlocked == false) {
            statsDao.updateAchievement(firstMeal.copy(isUnlocked = true, unlockedDate = todayStr))
            Timber.i("Achievement unlocked: First Meal")
        }

        // Week streak achievement
        if (streak.currentStreak >= 7) {
            val weekStreak = statsDao.getAchievementById("week_streak")
            if (weekStreak?.isUnlocked == false) {
                statsDao.updateAchievement(weekStreak.copy(isUnlocked = true, unlockedDate = todayStr))
                Timber.i("Achievement unlocked: Week Warrior")
            }
        }

        // Month streak achievement
        if (streak.currentStreak >= 30) {
            val monthStreak = statsDao.getAchievementById("month_streak")
            if (monthStreak?.isUnlocked == false) {
                statsDao.updateAchievement(monthStreak.copy(isUnlocked = true, unlockedDate = todayStr))
                Timber.i("Achievement unlocked: Month Master")
            }
        }
    }

    private suspend fun ensureStreakExists() {
        if (statsDao.getCookingStreakSync() == null) {
            statsDao.insertCookingStreak(CookingStreakEntity())
            Timber.d("Initialized cooking streak")
        }
    }

    private suspend fun ensureAchievementsExist() {
        val existing = statsDao.getAllAchievements().first()
        if (existing.isEmpty()) {
            statsDao.insertAchievements(DEFAULT_ACHIEVEMENTS)
            Timber.d("Initialized default achievements")
        }
    }

    private suspend fun ensureWeeklyChallengeExists() {
        if (statsDao.getCurrentWeeklyChallengeSync() == null) {
            // Create a default weekly challenge
            val today = LocalDate.now()
            val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1) // Monday
            val weekEnd = weekStart.plusDays(6) // Sunday

            val challenge = WeeklyChallengeEntity(
                id = "weekly_${weekStart.format(DateTimeFormatter.ISO_DATE)}",
                name = "Home Chef Week",
                description = "Cook 5 homemade meals this week",
                targetCount = 5,
                currentProgress = 0,
                rewardBadge = "👨‍🍳",
                isJoined = false,
                weekStartDate = weekStart.format(DateTimeFormatter.ISO_DATE),
                weekEndDate = weekEnd.format(DateTimeFormatter.ISO_DATE)
            )
            statsDao.insertWeeklyChallenge(challenge)
            Timber.d("Initialized weekly challenge")
        }
    }

    override suspend fun recordCookedRecipe(
        recipeId: String,
        recipeName: String,
        cuisineType: String
    ): Result<Unit> {
        return try {
            val today = LocalDate.now()
            val cookedRecipe = CookedRecipeEntity(
                id = UUID.randomUUID().toString(),
                recipeId = recipeId,
                recipeName = recipeName,
                cuisineType = cuisineType,
                cookedDate = today.format(DateTimeFormatter.ISO_DATE),
                createdAt = System.currentTimeMillis()
            )

            statsDao.insertCookedRecipe(cookedRecipe)
            Timber.i("Recorded cooked recipe: $recipeName ($cuisineType)")

            // Also record the meal and check achievements
            recordCookedMeal()

            // Check for cuisine explorer achievement
            val uniqueCuisines = statsDao.getUniqueCuisinesCount()
            if (uniqueCuisines >= 5) {
                val achievement = statsDao.getAchievementById("five_cuisines")
                if (achievement?.isUnlocked == false) {
                    val todayStr = today.format(DateTimeFormatter.ISO_DATE)
                    statsDao.updateAchievement(achievement.copy(isUnlocked = true, unlockedDate = todayStr))
                    Timber.i("Achievement unlocked: Cuisine Explorer")
                }
            }

            // Check for fifty meals achievement
            val totalCooked = statsDao.getTotalCookedRecipesCount()
            if (totalCooked >= 50) {
                val achievement = statsDao.getAchievementById("fifty_meals")
                if (achievement?.isUnlocked == false) {
                    val todayStr = today.format(DateTimeFormatter.ISO_DATE)
                    statsDao.updateAchievement(achievement.copy(isUnlocked = true, unlockedDate = todayStr))
                    Timber.i("Achievement unlocked: Fifty Feasts")
                }
            }

            // Check for ten new recipes achievement
            val uniqueRecipes = statsDao.getUniqueCookedRecipesCount()
            if (uniqueRecipes >= 10) {
                val achievement = statsDao.getAchievementById("ten_new_recipes")
                if (achievement?.isUnlocked == false) {
                    val todayStr = today.format(DateTimeFormatter.ISO_DATE)
                    statsDao.updateAchievement(achievement.copy(isUnlocked = true, unlockedDate = todayStr))
                    Timber.i("Achievement unlocked: Recipe Pioneer")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to record cooked recipe")
            Result.failure(e)
        }
    }

    override suspend fun getCuisineBreakdown(): Result<List<Pair<String, Int>>> {
        return try {
            val breakdown = statsDao.getCuisineBreakdown()
            val result = breakdown.map { it.cuisineType to it.count }
            Timber.d("Cuisine breakdown: $result")
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get cuisine breakdown")
            Result.failure(e)
        }
    }
}
