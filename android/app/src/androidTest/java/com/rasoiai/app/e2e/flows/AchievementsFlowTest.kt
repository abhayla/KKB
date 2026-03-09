package com.rasoiai.app.e2e.flows

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Requirement: #78 - FR-022: Achievements screen E2E flow test
 *
 * Tests the Achievements screen navigation from Stats and content display.
 * The Achievements screen shows unlocked/locked achievement sections with
 * progress bars and share actions.
 *
 * @see docs/testing/Functional-Requirement-Rule.md
 */
@HiltAndroidTest
class AchievementsFlowTest : BaseE2ETest() {

    companion object {
        private const val TAG = "AchievementsFlowTest"
    }

    private lateinit var homeRobot: HomeRobot

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedStateWithoutMealPlan()

        homeRobot = HomeRobot(composeTestRule)
    }

    // ===================== Helper =====================

    private fun navigateToAchievements() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToStats()

        composeTestRule.onNodeWithTag(TestTags.STATS_SCREEN).assertIsDisplayed()

        // Wait for stats data to load (loading spinner to disappear, LazyColumn to appear)
        composeTestRule.waitUntil(15000) {
            composeTestRule.onAllNodes(
                hasTestTag("stats_lazy_column")
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Swipe up on the LazyColumn to scroll to the achievements section
        // AchievementsSection is ~8th item in the LazyColumn, needs several swipes
        repeat(5) {
            composeTestRule.onNodeWithTag("stats_lazy_column")
                .performTouchInput { swipeUp() }
            composeTestRule.waitForIdle()
        }

        // Now tap "View All" in the achievements section
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodes(
                hasTestTag(TestTags.ACHIEVEMENTS_VIEW_ALL)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_VIEW_ALL)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodes(
                hasTestTag(TestTags.ACHIEVEMENTS_SCREEN)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ===================== Tests =====================

    @Test
    fun testNavigateToAchievementsFromStats() {
        navigateToAchievements()

        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_SCREEN).assertIsDisplayed()

        Log.i(TAG, "testNavigateToAchievementsFromStats: navigated successfully from Stats")
    }

    @Test
    fun testAchievementsListDisplayed() {
        navigateToAchievements()

        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_LIST).assertIsDisplayed()

        Log.i(TAG, "testAchievementsListDisplayed: achievements list visible")
    }

    @Test
    fun testUnlockedSectionDisplayed() {
        navigateToAchievements()

        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_SCREEN).assertIsDisplayed()

        // The unlocked section should be visible (may be empty if no achievements earned)
        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_UNLOCKED_SECTION).assertIsDisplayed()

        Log.i(TAG, "testUnlockedSectionDisplayed: unlocked achievements section visible")
    }

    @Test
    fun testLockedSectionDisplayed() {
        navigateToAchievements()

        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_SCREEN).assertIsDisplayed()

        // The locked section should show remaining achievements to earn
        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_LOCKED_SECTION).assertIsDisplayed()

        Log.i(TAG, "testLockedSectionDisplayed: locked achievements section visible")
    }

    @Test
    @Ignore("Requires seeded achievement data with progress")
    fun testAchievementCardWithProgress() {
        navigateToAchievements()

        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_SCREEN).assertIsDisplayed()

        // At least one achievement card should be visible with a progress indicator
        composeTestRule.onNodeWithTag("${TestTags.ACHIEVEMENT_CARD_PREFIX}0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("${TestTags.ACHIEVEMENT_PROGRESS_PREFIX}0").assertIsDisplayed()

        Log.i(TAG, "testAchievementCardWithProgress: achievement card with progress bar visible")
    }

    @Test
    @Ignore("Requires seeded achievement data to share")
    fun testShareAchievement() {
        navigateToAchievements()

        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_SCREEN).assertIsDisplayed()

        // Tap share on the first achievement
        composeTestRule.onNodeWithTag("${TestTags.ACHIEVEMENT_SHARE_PREFIX}0").performClick()
        composeTestRule.waitForIdle()

        // Share intent should be triggered (we can't fully verify external intents,
        // but the button click should not crash)
        Log.i(TAG, "testShareAchievement: share action triggered without crash")
    }
}
