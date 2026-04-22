package com.rasoiai.domain.model

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CookingStatsTest {

    @Nested
    @DisplayName("CookingStreak")
    inner class CookingStreakTests {
        @Test
        fun `isActiveToday true when lastCookingDate is today`() {
            val s = CookingStreak(currentStreak = 3, bestStreak = 5, lastCookingDate = LocalDate.now())
            assertTrue(s.isActiveToday)
        }

        @Test
        fun `isActiveToday false when lastCookingDate is yesterday`() {
            val s = CookingStreak(
                currentStreak = 3,
                bestStreak = 5,
                lastCookingDate = LocalDate.now().minusDays(1),
            )
            assertFalse(s.isActiveToday)
        }

        @Test
        fun `isActiveToday false when lastCookingDate is null`() {
            assertFalse(CookingStreak(0, 0, null).isActiveToday)
        }

        @Test
        fun `motivationalText for zero streak`() {
            val s = CookingStreak(0, 0, null)
            assertTrue(s.motivationalText.contains("Start"))
        }

        @Test
        fun `motivationalText when current below best`() {
            val s = CookingStreak(currentStreak = 2, bestStreak = 5, lastCookingDate = LocalDate.now())
            assertTrue(s.motivationalText.contains("extend"))
        }

        @Test
        fun `motivationalText when current equals best`() {
            val s = CookingStreak(currentStreak = 5, bestStreak = 5, lastCookingDate = LocalDate.now())
            assertTrue(s.motivationalText.contains("best"))
        }

        @Test
        fun `motivationalText when current exceeds best`() {
            val s = CookingStreak(currentStreak = 7, bestStreak = 5, lastCookingDate = LocalDate.now())
            assertTrue(s.motivationalText.contains("record") || s.motivationalText.contains("New"))
        }
    }

    @Nested
    @DisplayName("CookingDay temporal predicates")
    inner class CookingDayTemporal {
        @Test
        fun `isToday true for today`() {
            val d = CookingDay(LocalDate.now(), didCook = true)
            assertTrue(d.isToday)
            assertFalse(d.isPast)
            assertFalse(d.isFuture)
        }

        @Test
        fun `isPast true for yesterday`() {
            val d = CookingDay(LocalDate.now().minusDays(1), didCook = false)
            assertTrue(d.isPast)
            assertFalse(d.isToday)
            assertFalse(d.isFuture)
        }

        @Test
        fun `isFuture true for tomorrow`() {
            val d = CookingDay(LocalDate.now().plusDays(1), didCook = false)
            assertTrue(d.isFuture)
            assertFalse(d.isToday)
            assertFalse(d.isPast)
        }

        @Test
        fun `three predicates are mutually exclusive and exhaustive`() {
            listOf(
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                LocalDate.now().plusDays(1),
            ).forEach { date ->
                val d = CookingDay(date, didCook = false)
                val flags = listOf(d.isPast, d.isToday, d.isFuture)
                assertEquals(1, flags.count { it }, "Exactly one of past/today/future must be true for $date")
            }
        }
    }

    @Nested
    @DisplayName("Achievement.displayText")
    inner class AchievementDisplayText {
        @Test
        fun `unlocked shows name`() {
            val a = Achievement("a-1", "Master Chef", "desc", "🍳", isUnlocked = true)
            assertEquals("Master Chef", a.displayText)
        }

        @Test
        fun `locked hides name with question marks`() {
            val a = Achievement("a-1", "Master Chef", "desc", "🍳", isUnlocked = false)
            assertEquals("???", a.displayText)
        }
    }

    @Nested
    @DisplayName("WeeklyChallenge")
    inner class WeeklyChallengeTests {
        @Test
        fun `progressFraction is zero for zero target (safe division)`() {
            val c = challenge(target = 0, progress = 0)
            assertEquals(0f, c.progressFraction)
        }

        @Test
        fun `progressFraction is fractional while in progress`() {
            val c = challenge(target = 10, progress = 3)
            assertEquals(0.3f, c.progressFraction, 0.0001f)
        }

        @Test
        fun `progressFraction caps conceptually but returns raw ratio`() {
            // Contract: raw ratio, not clamped. UI clamps on render.
            val c = challenge(target = 5, progress = 10)
            assertEquals(2.0f, c.progressFraction, 0.0001f)
        }

        @Test
        fun `isCompleted true when progress meets target`() {
            assertTrue(challenge(target = 5, progress = 5).isCompleted)
            assertTrue(challenge(target = 5, progress = 6).isCompleted)
            assertFalse(challenge(target = 5, progress = 4).isCompleted)
        }

        @Test
        fun `progressText formats as current over total`() {
            assertEquals("3/10", challenge(target = 10, progress = 3).progressText)
            assertEquals("0/5", challenge(target = 5, progress = 0).progressText)
        }

        private fun challenge(target: Int, progress: Int) = WeeklyChallenge(
            id = "c-1", name = "n", description = "d",
            targetCount = target, currentProgress = progress,
            rewardBadge = "🏅", isJoined = true,
        )
    }

    @Nested
    @DisplayName("LeaderboardEntry.rankEmoji")
    inner class RankEmoji {
        @Test
        fun `top three get medal emojis`() {
            assertEquals("🥇", entry(rank = 1).rankEmoji)
            assertEquals("🥈", entry(rank = 2).rankEmoji)
            assertEquals("🥉", entry(rank = 3).rankEmoji)
        }

        @Test
        fun `fourth place onwards gets numeric label`() {
            assertEquals("4.", entry(rank = 4).rankEmoji)
            assertEquals("10.", entry(rank = 10).rankEmoji)
            assertEquals("99.", entry(rank = 99).rankEmoji)
        }

        private fun entry(rank: Int) = LeaderboardEntry(rank = rank, userName = "U", mealsCount = 1, isCurrentUser = false)
    }
}
