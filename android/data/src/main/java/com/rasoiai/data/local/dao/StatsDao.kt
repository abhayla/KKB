package com.rasoiai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rasoiai.data.local.entity.AchievementEntity
import com.rasoiai.data.local.entity.CookingDayEntity
import com.rasoiai.data.local.entity.CookingStreakEntity
import com.rasoiai.data.local.entity.WeeklyChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    // ==================== Cooking Streak ====================

    @Query("SELECT * FROM cooking_streak WHERE id = 'user_streak'")
    fun getCookingStreak(): Flow<CookingStreakEntity?>

    @Query("SELECT * FROM cooking_streak WHERE id = 'user_streak'")
    suspend fun getCookingStreakSync(): CookingStreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCookingStreak(streak: CookingStreakEntity)

    @Update
    suspend fun updateCookingStreak(streak: CookingStreakEntity)

    // ==================== Cooking Days ====================

    @Query("SELECT * FROM cooking_days WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getCookingDaysForMonth(startDate: String, endDate: String): List<CookingDayEntity>

    @Query("SELECT * FROM cooking_days WHERE date = :date")
    suspend fun getCookingDayByDate(date: String): CookingDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCookingDay(day: CookingDayEntity)

    @Query("SELECT COUNT(*) FROM cooking_days WHERE didCook = 1 AND date BETWEEN :startDate AND :endDate")
    suspend fun getCookingDaysCountForMonth(startDate: String, endDate: String): Int

    @Query("SELECT SUM(mealsCount) FROM cooking_days WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalMealsForMonth(startDate: String, endDate: String): Int?

    // ==================== Achievements ====================

    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, name ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :achievementId")
    suspend fun getAchievementById(achievementId: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    suspend fun getUnlockedAchievementsCount(): Int

    // ==================== Weekly Challenges ====================

    @Query("SELECT * FROM weekly_challenges WHERE date('now') BETWEEN weekStartDate AND weekEndDate LIMIT 1")
    fun getCurrentWeeklyChallenge(): Flow<WeeklyChallengeEntity?>

    @Query("SELECT * FROM weekly_challenges WHERE date('now') BETWEEN weekStartDate AND weekEndDate LIMIT 1")
    suspend fun getCurrentWeeklyChallengeSync(): WeeklyChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyChallenge(challenge: WeeklyChallengeEntity)

    @Update
    suspend fun updateWeeklyChallenge(challenge: WeeklyChallengeEntity)

    @Query("UPDATE weekly_challenges SET isJoined = :joined WHERE id = :challengeId")
    suspend fun updateChallengeJoinedStatus(challengeId: String, joined: Boolean)

    @Query("UPDATE weekly_challenges SET currentProgress = currentProgress + 1 WHERE id = :challengeId AND isJoined = 1")
    suspend fun incrementChallengeProgress(challengeId: String)
}
