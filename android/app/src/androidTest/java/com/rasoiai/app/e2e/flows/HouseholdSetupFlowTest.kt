package com.rasoiai.app.e2e.flows

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.HouseholdMembersRobot
import com.rasoiai.app.e2e.robots.HouseholdRobot
import com.rasoiai.app.e2e.robots.SettingsRobot
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Household Setup Flow Tests - Create, view, update, and deactivate households.
 * Tests household CRUD operations and validation.
 *
 * All tests are @Ignore because they require:
 * - Running backend with household endpoints active
 * - A fresh user who is not yet a member of any household
 *
 * Navigation path: Home → Settings (profile button) → "My Household" item
 */
@HiltAndroidTest
class HouseholdSetupFlowTest : BaseE2ETest() {

    companion object {
        private const val TAG = "HouseholdSetupFlowTest"
        private const val TEST_HOUSEHOLD_NAME = "Sharma Family"
        private const val LONG_NAME_101_CHARS =
            "AAAAAAAAAA AAAAAAAAAA AAAAAAAAAA AAAAAAAAAA AAAAAAAAAA AAAAAAAAAA AAAAAAAAAA AAAAAAAAAA AAAAAAAAAA AA"
    }

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot
    private lateinit var householdRobot: HouseholdRobot
    private lateinit var householdMembersRobot: HouseholdMembersRobot

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedStateWithoutMealPlan()

        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)
        householdRobot = HouseholdRobot(composeTestRule)
        householdMembersRobot = HouseholdMembersRobot(composeTestRule)
    }

    // ===================== Navigation helper =====================

    private fun navigateToHousehold() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToSettings()
        settingsRobot.waitForSettingsScreen()
        settingsRobot.tapSettingItem("My Household")
        householdRobot.waitForHouseholdScreen()
    }

    // ===================== Tests =====================

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testCreateHousehold() {
        navigateToHousehold()

        // Enter name and create
        householdRobot.enterHouseholdName(TEST_HOUSEHOLD_NAME)
        householdRobot.tapCreateHousehold()

        // Verify household is created and name is shown
        householdRobot.waitForHouseholdScreen()
        householdRobot.assertHouseholdNameDisplayed(TEST_HOUSEHOLD_NAME)

        // Invite code should appear after creation
        householdRobot.assertInviteCodeDisplayed()

        Log.i(TAG, "testCreateHousehold: household '$TEST_HOUSEHOLD_NAME' created successfully")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testCreateHouseholdOwnerIsMember() {
        navigateToHousehold()

        householdRobot.enterHouseholdName(TEST_HOUSEHOLD_NAME)
        householdRobot.tapCreateHousehold()
        householdRobot.waitForHouseholdScreen()

        // The members list should be visible and contain at least the owner (index 0)
        householdMembersRobot.waitForMembersScreen()
        householdMembersRobot.assertMembersListDisplayed()
        householdMembersRobot.assertMemberDisplayed(0)

        // Owner role label should read "Owner" or "OWNER"
        householdMembersRobot.assertMemberRole(0, "owner")

        Log.i(TAG, "testCreateHouseholdOwnerIsMember: owner appears as member at index 0")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testViewHouseholdDetail() {
        navigateToHousehold()

        // Assuming the user already has a household — the screen should show details
        householdRobot.assertHouseholdScreenDisplayed()

        // Household name, invite code, and members list should all be present
        householdRobot.assertInviteCodeDisplayed()
        householdMembersRobot.assertMembersListDisplayed()

        Log.i(TAG, "testViewHouseholdDetail: household detail screen shows all sections")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testUpdateHouseholdName() {
        navigateToHousehold()

        val updatedName = "Sharma Family Updated"

        // Clear existing name and enter the new one
        householdRobot.enterHouseholdName(updatedName)

        // The update button may be labelled "Save" or "Update" depending on UI state
        composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        // Verify the updated name is now displayed on the screen
        householdRobot.waitForHouseholdScreen()
        householdRobot.assertHouseholdNameDisplayed(updatedName)

        Log.i(TAG, "testUpdateHouseholdName: name updated to '$updatedName'")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testUpdateHouseholdCapacity() {
        navigateToHousehold()

        // Household capacity is typically a dropdown or stepper on the screen
        composeTestRule.onNodeWithText("Capacity", substring = true, ignoreCase = true)
            .assertIsDisplayed()

        // Select a new capacity value of 8
        composeTestRule.onNodeWithText("8", substring = true).performClick()
        composeTestRule.waitForIdle()

        // Save the change
        composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        // Verify the new capacity value is reflected
        composeTestRule.onNodeWithText("8", substring = true).assertIsDisplayed()

        Log.i(TAG, "testUpdateHouseholdCapacity: capacity updated to 8")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testDeactivateHousehold() {
        navigateToHousehold()

        // Tap deactivate — should show a confirmation dialog
        householdRobot.tapDeactivateHousehold()

        // Cancel first to verify the dialog is dismissible
        householdRobot.cancelDialog()
        householdRobot.assertHouseholdScreenDisplayed()

        // Tap deactivate again and confirm
        householdRobot.tapDeactivateHousehold()
        householdRobot.confirmDialog()
        composeTestRule.waitForIdle()

        // After deactivation the screen should return to the "create household" empty state
        householdRobot.waitForHouseholdScreen()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_CREATE_BUTTON).assertIsDisplayed()

        Log.i(TAG, "testDeactivateHousehold: household deactivated, create button visible again")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testDeactivateHouseholdWithMembersWarning() {
        navigateToHousehold()

        // Attempt to deactivate a household that still has members
        householdRobot.tapDeactivateHousehold()

        // The confirmation dialog should include a warning about existing members
        composeTestRule.onNodeWithText("members", substring = true, ignoreCase = true)
            .assertIsDisplayed()

        // Cancel to keep the household intact
        householdRobot.cancelDialog()
        householdRobot.assertHouseholdScreenDisplayed()

        Log.i(TAG, "testDeactivateHouseholdWithMembersWarning: members warning shown in dialog")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testListMembersShowsOwnerRole() {
        navigateToHousehold()

        // The members list must be visible after navigating to household
        householdMembersRobot.waitForMembersScreen()
        householdMembersRobot.assertMembersListDisplayed()

        // The first member (the owner) should have the "owner" role label
        householdMembersRobot.assertMemberDisplayed(0)
        householdMembersRobot.assertMemberRole(0, "owner")

        Log.i(TAG, "testListMembersShowsOwnerRole: owner role label correct at index 0")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testCreateHouseholdEmptyNameError() {
        navigateToHousehold()

        // Submit without entering a name — field cleared to ensure it is empty
        householdRobot.enterHouseholdName("")
        householdRobot.tapCreateHousehold()

        // An inline error or snackbar should appear indicating name is required
        householdRobot.assertErrorDisplayed("name")

        // The household should not have been created — create button is still present
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_CREATE_BUTTON).assertIsDisplayed()

        Log.i(TAG, "testCreateHouseholdEmptyNameError: validation error shown for empty name")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testCreateHouseholdNameTooLongError() {
        navigateToHousehold()

        // Enter a name that exceeds the 100-character limit
        householdRobot.enterHouseholdName(LONG_NAME_101_CHARS)
        householdRobot.tapCreateHousehold()

        // An inline error or snackbar should appear indicating the name is too long
        householdRobot.assertErrorDisplayed("too long")

        // The household should not have been created — create button is still present
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_CREATE_BUTTON).assertIsDisplayed()

        Log.i(TAG, "testCreateHouseholdNameTooLongError: validation error shown for long name")
    }
}
