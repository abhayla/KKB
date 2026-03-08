package com.rasoiai.app.e2e.flows

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Requirement: #77 - FR-021: Scope Toggle E2E coverage across 5 screens
 *
 * Tests the Family/Personal scope toggle (ScopeToggle composable) on all 5 screens
 * where it is implemented: Stats, Grocery, Favorites, RecipeRules, Chat.
 *
 * All tests are @Ignore because they require:
 * - The current user to be a member of an active household
 * - Running backend with household endpoints active
 *
 * @see docs/testing/Functional-Requirement-Rule.md
 */
@HiltAndroidTest
class ScopeToggleFlowTest : BaseE2ETest() {

    companion object {
        private const val TAG = "ScopeToggleFlowTest"
    }

    private lateinit var homeRobot: HomeRobot

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedStateWithoutMealPlan()

        homeRobot = HomeRobot(composeTestRule)
    }

    // ===================== Helper =====================

    private fun waitForScopeToggle(timeoutMillis: Long = 10000) {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodes(
                hasTestTag(TestTags.SCOPE_TOGGLE)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ===================== Stats Screen =====================

    @Test
    @Ignore("Requires user to be a member of an active household")
    fun testScopeToggleVisibleOnStats() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToStats()

        composeTestRule.onNodeWithTag(TestTags.STATS_SCREEN).assertIsDisplayed()
        waitForScopeToggle()
        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_FAMILY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_PERSONAL).assertIsDisplayed()

        Log.i(TAG, "testScopeToggleVisibleOnStats: toggle visible with both options")
    }

    @Test
    @Ignore("Requires user to be a member of an active household")
    fun testScopeToggleSwitchOnStats() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToStats()

        composeTestRule.onNodeWithTag(TestTags.STATS_SCREEN).assertIsDisplayed()
        waitForScopeToggle()

        // Default is Personal — switch to Family
        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_FAMILY).performClick()
        composeTestRule.waitForIdle()

        // Switch back to Personal
        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_PERSONAL).performClick()
        composeTestRule.waitForIdle()

        Log.i(TAG, "testScopeToggleSwitchOnStats: toggled Family → Personal successfully")
    }

    // ===================== Grocery Screen =====================

    @Test
    @Ignore("Requires user to be a member of an active household")
    fun testScopeToggleVisibleOnGrocery() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToGrocery()

        composeTestRule.onNodeWithTag(TestTags.GROCERY_SCREEN).assertIsDisplayed()
        waitForScopeToggle()
        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE).assertIsDisplayed()

        Log.i(TAG, "testScopeToggleVisibleOnGrocery: toggle visible")
    }

    @Test
    @Ignore("Requires user to be a member of an active household")
    fun testScopeToggleSwitchOnGrocery() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToGrocery()

        composeTestRule.onNodeWithTag(TestTags.GROCERY_SCREEN).assertIsDisplayed()
        waitForScopeToggle()

        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_FAMILY).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_PERSONAL).performClick()
        composeTestRule.waitForIdle()

        Log.i(TAG, "testScopeToggleSwitchOnGrocery: toggled Family → Personal successfully")
    }

    // ===================== Favorites Screen =====================

    @Test
    @Ignore("Requires user to be a member of an active household")
    fun testScopeToggleVisibleOnFavorites() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToFavorites()

        composeTestRule.onNodeWithTag(TestTags.FAVORITES_SCREEN).assertIsDisplayed()
        waitForScopeToggle()
        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE).assertIsDisplayed()

        Log.i(TAG, "testScopeToggleVisibleOnFavorites: toggle visible")
    }

    @Test
    @Ignore("Requires user to be a member of an active household")
    fun testScopeToggleSwitchOnFavorites() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToFavorites()

        composeTestRule.onNodeWithTag(TestTags.FAVORITES_SCREEN).assertIsDisplayed()
        waitForScopeToggle()

        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_FAMILY).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_PERSONAL).performClick()
        composeTestRule.waitForIdle()

        Log.i(TAG, "testScopeToggleSwitchOnFavorites: toggled Family → Personal successfully")
    }

    // ===================== Recipe Rules Screen =====================

    @Test
    @Ignore("Requires user to be a member of an active household")
    fun testScopeToggleVisibleOnRecipeRules() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToSettings()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_RECIPE_RULES).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.RECIPE_RULES_SCREEN).assertIsDisplayed()
        waitForScopeToggle()
        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE).assertIsDisplayed()

        Log.i(TAG, "testScopeToggleVisibleOnRecipeRules: toggle visible")
    }

    @Test
    @Ignore("Requires user to be a member of an active household")
    fun testScopeToggleSwitchOnRecipeRules() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToSettings()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_RECIPE_RULES).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.RECIPE_RULES_SCREEN).assertIsDisplayed()
        waitForScopeToggle()

        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_FAMILY).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_PERSONAL).performClick()
        composeTestRule.waitForIdle()

        Log.i(TAG, "testScopeToggleSwitchOnRecipeRules: toggled Family → Personal successfully")
    }

    // ===================== Chat Screen =====================

    @Test
    @Ignore("Requires user to be a member of an active household")
    fun testScopeToggleVisibleOnChat() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToChat()

        composeTestRule.onNodeWithTag(TestTags.CHAT_SCREEN).assertIsDisplayed()
        waitForScopeToggle()
        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE).assertIsDisplayed()

        Log.i(TAG, "testScopeToggleVisibleOnChat: toggle visible")
    }

    @Test
    @Ignore("Requires user to be a member of an active household")
    fun testScopeToggleSwitchOnChat() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToChat()

        composeTestRule.onNodeWithTag(TestTags.CHAT_SCREEN).assertIsDisplayed()
        waitForScopeToggle()

        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_FAMILY).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE_PERSONAL).performClick()
        composeTestRule.waitForIdle()

        Log.i(TAG, "testScopeToggleSwitchOnChat: toggled Family → Personal successfully")
    }

    // ===================== Cross-Screen Toggle Persistence =====================

    @Test
    @Ignore("Requires user to be a member of an active household")
    fun testScopeToggleNotVisibleWithoutHousehold() {
        // When user has no active household, scope toggle should not appear
        // This test assumes the user is NOT a household member
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToStats()

        composeTestRule.onNodeWithTag(TestTags.STATS_SCREEN).assertIsDisplayed()

        // Scope toggle should NOT be visible
        composeTestRule.onNodeWithTag(TestTags.SCOPE_TOGGLE).assertDoesNotExist()

        Log.i(TAG, "testScopeToggleNotVisibleWithoutHousehold: toggle correctly hidden")
    }
}
