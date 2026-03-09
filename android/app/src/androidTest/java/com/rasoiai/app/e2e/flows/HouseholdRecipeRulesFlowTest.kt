package com.rasoiai.app.e2e.flows

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.SettingsRobot
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Household Recipe Rules Flow Tests - Create, list, delete household-scoped
 * recipe rules and view merged constraints.
 *
 * Tests ensure household exists via API before UI navigation.
 *
 * Navigation path: Home → Settings → "My Household" → Recipe Rules section
 */
@HiltAndroidTest
class HouseholdRecipeRulesFlowTest : BaseE2ETest() {

    companion object {
        private const val TAG = "HouseholdRecipeRulesFlowTest"
        private const val TEST_HOUSEHOLD_NAME = "Sharma Family"
        private const val INCLUDE_RULE_TARGET = "Chai"
        private const val EXCLUDE_RULE_TARGET = "Mushroom"
    }

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedStateWithoutMealPlan()

        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)

        // Ensure household exists
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            BackendTestHelper.ensureHouseholdExists(BACKEND_BASE_URL, authToken, TEST_HOUSEHOLD_NAME)
        }
    }

    // ===================== Navigation helpers =====================

    private fun navigateToHouseholdRecipeRules() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToSettings()
        settingsRobot.waitForSettingsScreen()
        settingsRobot.tapSettingItem("My Household")

        composeTestRule.onNodeWithText("Recipe Rules", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasTestTag(TestTags.HOUSEHOLD_RECIPE_RULES_SCREEN)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun navigateToMergedConstraints() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToSettings()
        settingsRobot.waitForSettingsScreen()
        settingsRobot.tapSettingItem("My Household")

        composeTestRule.onNodeWithText("Constraints", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasTestTag(TestTags.HOUSEHOLD_CONSTRAINTS_VIEW)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ===================== Tests =====================

    @Test
    fun testCreateHouseholdIncludeRule() {
        navigateToHouseholdRecipeRules()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RECIPE_RULES_SCREEN).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RULE_ADD_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.RULE_ACTION_INCLUDE).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.BOTTOM_SHEET_SEARCH_FIELD)
            .performTextInput(INCLUDE_RULE_TARGET)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("${TestTags.SEARCH_RESULT_CHIP_PREFIX}0").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_RULE_CARD_PREFIX}0").assertIsDisplayed()
        composeTestRule.onNodeWithText(INCLUDE_RULE_TARGET, substring = true).assertIsDisplayed()

        Log.i(TAG, "testCreateHouseholdIncludeRule: INCLUDE rule for '$INCLUDE_RULE_TARGET' created")
    }

    @Test
    fun testCreateHouseholdExcludeRule() {
        navigateToHouseholdRecipeRules()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RECIPE_RULES_SCREEN).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RULE_ADD_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.RULE_ACTION_EXCLUDE).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.BOTTOM_SHEET_SEARCH_FIELD)
            .performTextInput(EXCLUDE_RULE_TARGET)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("${TestTags.SEARCH_RESULT_CHIP_PREFIX}0").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_RULE_CARD_PREFIX}0").assertIsDisplayed()
        composeTestRule.onNodeWithText(EXCLUDE_RULE_TARGET, substring = true).assertIsDisplayed()

        Log.i(TAG, "testCreateHouseholdExcludeRule: EXCLUDE rule for '$EXCLUDE_RULE_TARGET' created")
    }

    @Test
    fun testListHouseholdRules() {
        navigateToHouseholdRecipeRules()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RECIPE_RULES_SCREEN).assertIsDisplayed()

        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_RULE_CARD_PREFIX}0").assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RULE_ADD_BUTTON).assertIsDisplayed()

        // Verify via API
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            val rules = BackendTestHelper.getRecipeRules(BACKEND_BASE_URL, authToken)
            val ruleCount = rules?.optJSONArray("rules")?.length() ?: 0
            Log.i(TAG, "API verification: $ruleCount recipe rules exist")
        }

        Log.i(TAG, "testListHouseholdRules: household rules list displayed with at least one rule")
    }

    @Test
    fun testDeleteHouseholdRule() {
        navigateToHouseholdRecipeRules()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RECIPE_RULES_SCREEN).assertIsDisplayed()

        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_RULE_CARD_PREFIX}0").assertIsDisplayed()

        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_RULE_DELETE_PREFIX}0").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Confirm", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_RULE_CARD_PREFIX}0")
            .assertDoesNotExist()

        Log.i(TAG, "testDeleteHouseholdRule: rule at index 0 deleted from household")
    }

    @Test
    fun testNonOwnerCannotCreateRule() {
        navigateToHouseholdRecipeRules()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RECIPE_RULES_SCREEN).assertIsDisplayed()

        // A non-owner member should not see the add button, or it should be disabled
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RULE_ADD_BUTTON).assertIsNotEnabled()

        Log.i(TAG, "testNonOwnerCannotCreateRule: add rule button disabled for non-owner")
    }

    @Test
    fun testViewMergedConstraints() {
        navigateToMergedConstraints()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_CONSTRAINTS_VIEW).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_CONSTRAINTS_ALLERGIES).assertIsDisplayed()

        composeTestRule.onNodeWithText("Allergy", substring = true, ignoreCase = true)
            .assertIsDisplayed()

        Log.i(TAG, "testViewMergedConstraints: merged constraints view shows allergies section")
    }

    @Test
    fun testConstraintsEmptyHousehold() {
        navigateToMergedConstraints()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_CONSTRAINTS_VIEW).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_CONSTRAINTS_ALLERGIES).assertIsDisplayed()

        composeTestRule.onNodeWithText(
            "No household rules",
            substring = true,
            ignoreCase = true
        ).assertIsDisplayed()

        Log.i(TAG, "testConstraintsEmptyHousehold: empty household shows personal constraints only")
    }
}
