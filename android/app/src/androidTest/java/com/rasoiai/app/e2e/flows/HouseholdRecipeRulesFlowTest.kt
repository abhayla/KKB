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
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Household Recipe Rules Flow Tests - Create, list, delete household-scoped
 * recipe rules and view merged constraints.
 *
 * All tests are @Ignore because they require:
 * - Running backend with household endpoints active
 * - The current user to be a member of a household
 *
 * Navigation path: Home → Settings → "My Household" → Recipe Rules section
 */
@HiltAndroidTest
class HouseholdRecipeRulesFlowTest : BaseE2ETest() {

    companion object {
        private const val TAG = "HouseholdRecipeRulesFlowTest"
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
    }

    // ===================== Navigation helpers =====================

    /**
     * Navigate to the household recipe rules screen.
     */
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

    /**
     * Navigate to the household merged constraints view.
     */
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
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testCreateHouseholdIncludeRule() {
        navigateToHouseholdRecipeRules()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RECIPE_RULES_SCREEN).assertIsDisplayed()

        // Tap the add rule button to open the add-rule bottom sheet
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RULE_ADD_BUTTON).performClick()
        composeTestRule.waitForIdle()

        // Select "Include" as the rule type
        composeTestRule.onNodeWithTag(TestTags.RULE_ACTION_INCLUDE).performClick()
        composeTestRule.waitForIdle()

        // Search for and select the target ingredient/recipe
        composeTestRule.onNodeWithTag(TestTags.BOTTOM_SHEET_SEARCH_FIELD)
            .performTextInput(INCLUDE_RULE_TARGET)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("${TestTags.SEARCH_RESULT_CHIP_PREFIX}0").performClick()
        composeTestRule.waitForIdle()

        // Save the rule
        composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        // The new rule should appear in the household rules list
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_RULE_CARD_PREFIX}0").assertIsDisplayed()
        composeTestRule.onNodeWithText(INCLUDE_RULE_TARGET, substring = true).assertIsDisplayed()

        Log.i(TAG, "testCreateHouseholdIncludeRule: INCLUDE rule for '$INCLUDE_RULE_TARGET' created")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testCreateHouseholdExcludeRule() {
        navigateToHouseholdRecipeRules()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RECIPE_RULES_SCREEN).assertIsDisplayed()

        // Tap the add rule button
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RULE_ADD_BUTTON).performClick()
        composeTestRule.waitForIdle()

        // Select "Exclude" as the rule type
        composeTestRule.onNodeWithTag(TestTags.RULE_ACTION_EXCLUDE).performClick()
        composeTestRule.waitForIdle()

        // Search for and select the target
        composeTestRule.onNodeWithTag(TestTags.BOTTOM_SHEET_SEARCH_FIELD)
            .performTextInput(EXCLUDE_RULE_TARGET)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("${TestTags.SEARCH_RESULT_CHIP_PREFIX}0").performClick()
        composeTestRule.waitForIdle()

        // Save the rule
        composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        // The new EXCLUDE rule should appear in the list
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_RULE_CARD_PREFIX}0").assertIsDisplayed()
        composeTestRule.onNodeWithText(EXCLUDE_RULE_TARGET, substring = true).assertIsDisplayed()

        Log.i(TAG, "testCreateHouseholdExcludeRule: EXCLUDE rule for '$EXCLUDE_RULE_TARGET' created")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testListHouseholdRules() {
        navigateToHouseholdRecipeRules()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RECIPE_RULES_SCREEN).assertIsDisplayed()

        // At least one rule card should be present (seeded in backend fixture)
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_RULE_CARD_PREFIX}0").assertIsDisplayed()

        // The add button should also be present for the owner
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RULE_ADD_BUTTON).assertIsDisplayed()

        Log.i(TAG, "testListHouseholdRules: household rules list displayed with at least one rule")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testDeleteHouseholdRule() {
        navigateToHouseholdRecipeRules()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RECIPE_RULES_SCREEN).assertIsDisplayed()

        // Verify at least one rule exists before deletion
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_RULE_CARD_PREFIX}0").assertIsDisplayed()

        // Tap the delete button on the first rule card
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_RULE_DELETE_PREFIX}0").performClick()
        composeTestRule.waitForIdle()

        // Confirm the deletion dialog
        composeTestRule.onNodeWithText("Confirm", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        // The first rule card should no longer exist in the semantics tree
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_RULE_CARD_PREFIX}0")
            .assertDoesNotExist()

        Log.i(TAG, "testDeleteHouseholdRule: rule at index 0 deleted from household")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testNonOwnerCannotCreateRule() {
        navigateToHouseholdRecipeRules()

        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RECIPE_RULES_SCREEN).assertIsDisplayed()

        // A non-owner member should not see the add button, or it should be disabled
        // The test assumes the current user is a member without edit access
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_RULE_ADD_BUTTON).assertIsNotEnabled()

        Log.i(TAG, "testNonOwnerCannotCreateRule: add rule button disabled for non-owner")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testViewMergedConstraints() {
        navigateToMergedConstraints()

        // The merged constraints screen aggregates personal + household constraints
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_CONSTRAINTS_VIEW).assertIsDisplayed()

        // Allergy section should be present, listing all combined allergies
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_CONSTRAINTS_ALLERGIES).assertIsDisplayed()

        // At least one constraint entry should be visible
        composeTestRule.onNodeWithText("Allergy", substring = true, ignoreCase = true)
            .assertIsDisplayed()

        Log.i(TAG, "testViewMergedConstraints: merged constraints view shows allergies section")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testConstraintsEmptyHousehold() {
        navigateToMergedConstraints()

        // When a household has no recipe rules, the merged constraints still shows
        // personal constraints (from the individual user's profile)
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_CONSTRAINTS_VIEW).assertIsDisplayed()

        // The allergy section should still be present (from personal profile)
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_CONSTRAINTS_ALLERGIES).assertIsDisplayed()

        // No "household rules" section should be shown (or it shows an empty state)
        composeTestRule.onNodeWithText(
            "No household rules",
            substring = true,
            ignoreCase = true
        ).assertIsDisplayed()

        Log.i(TAG, "testConstraintsEmptyHousehold: empty household shows personal constraints only")
    }
}
