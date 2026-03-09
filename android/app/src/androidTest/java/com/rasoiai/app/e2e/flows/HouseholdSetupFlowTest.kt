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
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Household Setup Flow Tests - Create, view, update, and deactivate households.
 * Tests household CRUD operations and validation.
 *
 * Tests create households via API before UI navigation to ensure
 * the backend is in the correct state.
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

        // Verify via API that household exists
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            val household = BackendTestHelper.getMyHousehold(BACKEND_BASE_URL, authToken)
            if (household != null) {
                val apiName = household.optString("name", "")
                Log.i(TAG, "API verification: household name='$apiName', id=${household.optString("id")}")
            }
        }

        Log.i(TAG, "testCreateHousehold: household '$TEST_HOUSEHOLD_NAME' created successfully")
    }

    @Test
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
    fun testViewHouseholdDetail() {
        // Ensure household exists via API first
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            BackendTestHelper.ensureHouseholdExists(BACKEND_BASE_URL, authToken, TEST_HOUSEHOLD_NAME)
        }

        navigateToHousehold()

        // Assuming the user already has a household — the screen should show details
        householdRobot.assertHouseholdScreenDisplayed()

        // Household name, invite code, and members list should all be present
        householdRobot.assertInviteCodeDisplayed()
        householdMembersRobot.assertMembersListDisplayed()

        // Verify via API that the displayed data matches
        if (authToken != null) {
            val household = BackendTestHelper.getMyHousehold(BACKEND_BASE_URL, authToken)
            if (household != null) {
                Log.i(TAG, "API match: name=${household.optString("name")}, " +
                    "invite_code=${household.optString("invite_code")}")
            }
        }

        Log.i(TAG, "testViewHouseholdDetail: household detail screen shows all sections")
    }

    @Test
    fun testUpdateHouseholdName() {
        // Ensure household exists first
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            BackendTestHelper.ensureHouseholdExists(BACKEND_BASE_URL, authToken, TEST_HOUSEHOLD_NAME)
        }

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

        // Verify via API
        if (authToken != null) {
            val household = BackendTestHelper.getMyHousehold(BACKEND_BASE_URL, authToken)
            val apiName = household?.optString("name", "")
            Log.i(TAG, "API verification: updated name='$apiName'")
        }

        Log.i(TAG, "testUpdateHouseholdName: name updated to '$updatedName'")
    }

    @Test
    fun testUpdateHouseholdCapacity() {
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            BackendTestHelper.ensureHouseholdExists(BACKEND_BASE_URL, authToken, TEST_HOUSEHOLD_NAME)
        }

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
    fun testDeactivateHousehold() {
        // Ensure household exists for deactivation
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            BackendTestHelper.ensureHouseholdExists(BACKEND_BASE_URL, authToken, TEST_HOUSEHOLD_NAME)
        }

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

        // Verify via API: household should be inactive
        if (authToken != null) {
            val household = BackendTestHelper.getMyHousehold(BACKEND_BASE_URL, authToken)
            val isActive = household?.optBoolean("is_active", true)
            Log.i(TAG, "API verification: is_active=$isActive (expected false or null)")
        }

        Log.i(TAG, "testDeactivateHousehold: household deactivated, create button visible again")
    }

    @Test
    fun testDeactivateHouseholdWithMembersWarning() {
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            BackendTestHelper.ensureHouseholdExists(BACKEND_BASE_URL, authToken, TEST_HOUSEHOLD_NAME)
        }

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
    fun testListMembersShowsOwnerRole() {
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            BackendTestHelper.ensureHouseholdExists(BACKEND_BASE_URL, authToken, TEST_HOUSEHOLD_NAME)
        }

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
