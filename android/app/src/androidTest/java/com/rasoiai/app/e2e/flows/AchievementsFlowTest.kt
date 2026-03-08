package com.rasoiai.app.e2e.flows

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * All tests are @Ignore because they require:
 * - Running backend with achievements endpoints active
 * - Seeded achievement definitions
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

        // Tap the achievements section/button on the Stats screen
        composeTestRule.onNodeWithText("Achievements", substring = true, ignoreCase = true)
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
    @Ignore("Requires running backend with achievements endpoints and seeded data")
    fun testNavigateToAchievementsFromStats() {
        navigateToAchievements()

        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_SCREEN).assertIsDisplayed()

        Log.i(TAG, "testNavigateToAchievementsFromStats: navigated successfully from Stats")
    }

    @Test
    @Ignore("Requires running backend with achievements endpoints and seeded data")
    fun testAchievementsListDisplayed() {
        navigateToAchievements()

        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_LIST).assertIsDisplayed()

        Log.i(TAG, "testAchievementsListDisplayed: achievements list visible")
    }

    @Test
    @Ignore("Requires running backend with achievements endpoints and seeded data")
    fun testUnlockedSectionDisplayed() {
        navigateToAchievements()

        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_SCREEN).assertIsDisplayed()

        // The unlocked section should be visible (may be empty if no achievements earned)
        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_UNLOCKED_SECTION).assertIsDisplayed()

        Log.i(TAG, "testUnlockedSectionDisplayed: unlocked achievements section visible")
    }

    @Test
    @Ignore("Requires running backend with achievements endpoints and seeded data")
    fun testLockedSectionDisplayed() {
        navigateToAchievements()

        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_SCREEN).assertIsDisplayed()

        // The locked section should show remaining achievements to earn
        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_LOCKED_SECTION).assertIsDisplayed()

        Log.i(TAG, "testLockedSectionDisplayed: locked achievements section visible")
    }

    @Test
    @Ignore("Requires running backend with achievements endpoints and seeded data")
    fun testAchievementCardWithProgress() {
        navigateToAchievements()

        composeTestRule.onNodeWithTag(TestTags.ACHIEVEMENTS_SCREEN).assertIsDisplayed()

        // At least one achievement card should be visible with a progress indicator
        composeTestRule.onNodeWithTag("${TestTags.ACHIEVEMENT_CARD_PREFIX}0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("${TestTags.ACHIEVEMENT_PROGRESS_PREFIX}0").assertIsDisplayed()

        Log.i(TAG, "testAchievementCardWithProgress: achievement card with progress bar visible")
    }

    @Test
    @Ignore("Requires running backend with achievements endpoints and seeded data")
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
