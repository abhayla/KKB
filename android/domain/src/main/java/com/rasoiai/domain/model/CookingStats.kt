package com.rasoiai.domain.model

import java.time.LocalDate

/**
 * Represents the user's cooking streak.
 */
data class CookingStreak(
    val currentStreak: Int,
    val bestStreak: Int,
    val lastCookingDate: LocalDate?
) {
    val isActiveToday: Boolean
        get() = lastCookingDate == LocalDate.now()

    val motivationalText: String
        get() = when {
            currentStreak == 0 -> "Start cooking to build your streak!"
            currentStreak < bestStreak -> "Keep cooking to extend!"
            currentStreak == bestStreak -> "You're at your best streak! 🎉"
            else -> "New record! Keep it up! 🔥"
        }
}

/**
 * Monthly cooking statistics.
 */
data class MonthlyStats(
    val mealsCooked: Int,
    val newRecipes: Int,
    val averageRating: Float
)

/**
 * Represents a day in the cooking calendar.
 */
data class CookingDay(
    val date: LocalDate,
    val didCook: Boolean,
    val mealsCount: Int = 0
) {
    val isToday: Boolean
        get() = date == LocalDate.now()

    val isPast: Boolean
        get() = date.isBefore(LocalDate.now())

    val isFuture: Boolean
        get() = date.isAfter(LocalDate.now())
}

/**
 * Represents an achievement/badge earned by the user.
 */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val isUnlocked: Boolean,
    val unlockedDate: LocalDate? = null
) {
    val displayText: String
        get() = if (isUnlocked) name else "???"
}

/**
 * Represents a weekly challenge.
 */
data class WeeklyChallenge(
    val id: String,
    val name: String,
    val description: String,
    val targetCount: Int,
    val currentProgress: Int,
    val rewardBadge: String,
    val isJoined: Boolean
) {
    val progressFraction: Float
        get() = if (targetCount > 0) currentProgress.toFloat() / targetCount else 0f

    val isCompleted: Boolean
        get() = currentProgress >= targetCount

    val progressText: String
        get() = "$currentProgress/$targetCount"
}

/**
 * Represents an entry in the leaderboard.
 */
data class LeaderboardEntry(
    val rank: Int,
    val userName: String,
    val mealsCount: Int,
    val isCurrentUser: Boolean
) {
    val rankEmoji: String
        get() = when (rank) {
            1 -> "🥇"
            2 -> "🥈"
            3 -> "🥉"
            else -> "$rank."
        }
}
