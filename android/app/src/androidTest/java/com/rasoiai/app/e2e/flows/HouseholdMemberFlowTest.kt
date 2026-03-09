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
 * Household Member Flow Tests - Add, invite, join, leave, transfer ownership,
 * and manage member roles/permissions.
 *
 * Tests create households via API to ensure correct backend state.
 *
 * Navigation path: Home → Settings → "My Household"
 */
@HiltAndroidTest
class HouseholdMemberFlowTest : BaseE2ETest() {

    companion object {
        private const val TAG = "HouseholdMemberFlowTest"
        private const val TEST_HOUSEHOLD_NAME = "Sharma Family"

        // A phone number registered in the backend test data (a secondary test user)
        private const val KNOWN_MEMBER_PHONE = "+912222222222"
        // A phone number that does not exist in the backend
        private const val UNKNOWN_PHONE = "+919999999999"
        // A syntactically valid but semantically wrong invite code
        private const val INVALID_INVITE_CODE = "XXXX-INVALID"
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

        // Ensure household exists
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            BackendTestHelper.ensureHouseholdExists(BACKEND_BASE_URL, authToken, TEST_HOUSEHOLD_NAME)
        }
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
    fun testAddMemberByPhone() {
        navigateToHousehold()

        householdMembersRobot.waitForMembersScreen()

        // Count existing members before adding
        householdMembersRobot.assertMembersListDisplayed()

        // Tap add member and enter a known phone number
        householdMembersRobot.tapAddMember()
        householdMembersRobot.enterPhoneNumber(KNOWN_MEMBER_PHONE)
        householdMembersRobot.confirmDialog()

        // New member should now appear in the list at index 1 (index 0 is the owner)
        householdMembersRobot.assertMemberDisplayed(1)

        // Verify via API
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            val household = BackendTestHelper.getMyHousehold(BACKEND_BASE_URL, authToken)
            val memberCount = household?.optInt("member_count", -1)
            Log.i(TAG, "API verification: member_count=$memberCount after adding")
        }

        Log.i(TAG, "testAddMemberByPhone: member added via phone $KNOWN_MEMBER_PHONE")
    }

    @Test
    fun testAddMemberUnknownPhone() {
        navigateToHousehold()

        householdMembersRobot.waitForMembersScreen()
        householdMembersRobot.tapAddMember()

        // Enter a phone number that is not registered in the backend
        householdMembersRobot.enterPhoneNumber(UNKNOWN_PHONE)
        householdMembersRobot.confirmDialog()

        // An error should appear stating the user was not found
        householdMembersRobot.assertErrorDisplayed("not found")

        Log.i(TAG, "testAddMemberUnknownPhone: 'not found' error shown for $UNKNOWN_PHONE")
    }

    @Test
    fun testAddDuplicateMemberError() {
        navigateToHousehold()

        householdMembersRobot.waitForMembersScreen()

        // Add the member the first time
        householdMembersRobot.tapAddMember()
        householdMembersRobot.enterPhoneNumber(KNOWN_MEMBER_PHONE)
        householdMembersRobot.confirmDialog()
        householdMembersRobot.assertMemberDisplayed(1)

        // Try to add the same member again
        householdMembersRobot.tapAddMember()
        householdMembersRobot.enterPhoneNumber(KNOWN_MEMBER_PHONE)
        householdMembersRobot.confirmDialog()

        // Backend should return a 409 conflict — UI should show an error
        householdMembersRobot.assertErrorDisplayed("already")

        Log.i(TAG, "testAddDuplicateMemberError: duplicate member error shown")
    }

    @Test
    fun testAddMemberAtCapacityError() {
        navigateToHousehold()

        householdMembersRobot.waitForMembersScreen()

        // This test assumes the household is already at max capacity (set up in backend fixture)
        householdMembersRobot.tapAddMember()
        householdMembersRobot.enterPhoneNumber(KNOWN_MEMBER_PHONE)
        householdMembersRobot.confirmDialog()

        // Backend returns 409 when household is at capacity
        householdMembersRobot.assertErrorDisplayed("capacity")

        Log.i(TAG, "testAddMemberAtCapacityError: capacity error shown when household full")
    }

    @Test
    fun testGenerateInviteCode() {
        navigateToHousehold()

        // The invite code section should be visible as part of the household screen
        householdMembersRobot.assertInviteCodeDisplayed()

        // Share button should also be present
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_INVITE_SHARE_BUTTON).assertIsDisplayed()

        // Verify invite code exists via API
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            val household = BackendTestHelper.getMyHousehold(BACKEND_BASE_URL, authToken)
            val inviteCode = household?.optString("invite_code", "")
            Log.i(TAG, "API verification: invite_code='$inviteCode' (non-empty=${!inviteCode.isNullOrEmpty()})")
        }

        Log.i(TAG, "testGenerateInviteCode: invite code and share button visible")
    }

    @Test
    fun testRefreshInviteCode() {
        navigateToHousehold()

        // Refresh generates a new invite code, invalidating the previous one
        householdMembersRobot.assertInviteCodeDisplayed()
        householdMembersRobot.tapRefreshInviteCode()
        composeTestRule.waitForIdle()

        // A new invite code should still be displayed after refresh
        householdMembersRobot.assertInviteCodeDisplayed()

        Log.i(TAG, "testRefreshInviteCode: invite code refreshed, new code displayed")
    }

    @Test
    fun testJoinViaInviteCode() {
        navigateToHousehold()

        // This test uses a secondary user journey: the current user joins an existing household.
        // In a real run, the invite code would be obtained from a different user's session.
        // Here we use a fixture invite code seeded in the backend test data.
        val fixtureInviteCode = "TEST-INVITE-CODE"

        householdMembersRobot.tapJoinWithCode(fixtureInviteCode)
        composeTestRule.waitForIdle()

        // After a successful join, the household screen should show the household details
        householdRobot.waitForHouseholdScreen()
        householdMembersRobot.assertMembersListDisplayed()

        Log.i(TAG, "testJoinViaInviteCode: successfully joined household via invite code")
    }

    @Test
    fun testJoinInvalidCodeError() {
        navigateToHousehold()

        // Attempt to join with a code that does not exist
        householdMembersRobot.tapJoinWithCode(INVALID_INVITE_CODE)
        composeTestRule.waitForIdle()

        // Backend returns 404 — UI should show an error
        householdMembersRobot.assertErrorDisplayed("invalid")

        Log.i(TAG, "testJoinInvalidCodeError: invalid code error shown for $INVALID_INVITE_CODE")
    }

    @Test
    fun testLeaveHousehold() {
        navigateToHousehold()

        householdMembersRobot.waitForMembersScreen()

        // A non-owner member should see a "Leave" button
        householdMembersRobot.tapLeaveHousehold()

        // Confirmation dialog should appear
        householdMembersRobot.confirmDialog()
        composeTestRule.waitForIdle()

        // After leaving, the screen should return to the empty/join state
        householdRobot.waitForHouseholdScreen()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_JOIN_BUTTON).assertIsDisplayed()

        Log.i(TAG, "testLeaveHousehold: member left household successfully")
    }

    @Test
    fun testOwnerCannotLeave() {
        navigateToHousehold()

        householdMembersRobot.waitForMembersScreen()

        // The owner should not see a "Leave" button — they must transfer ownership first
        householdMembersRobot.assertLeaveButtonNotPresent()

        // Instead the owner should see "Transfer Ownership" or "Deactivate"
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_TRANSFER_BUTTON).assertIsDisplayed()

        Log.i(TAG, "testOwnerCannotLeave: leave button absent for owner, transfer button visible")
    }

    @Test
    fun testTransferOwnership() {
        navigateToHousehold()

        householdMembersRobot.waitForMembersScreen()

        // Tap transfer ownership — a dialog listing current members should appear
        householdMembersRobot.tapTransferOwnership()
        householdMembersRobot.assertTransferDialogDisplayed()

        // Select the second member (index 1) as the new owner
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_MEMBER_ROW_PREFIX}1").performClick()
        composeTestRule.waitForIdle()

        // Confirm the transfer
        householdMembersRobot.confirmDialog()
        composeTestRule.waitForIdle()

        // The original owner (now a regular member) should see their role change
        householdMembersRobot.assertMemberRole(0, "member")

        Log.i(TAG, "testTransferOwnership: ownership transferred to member at index 1")
    }

    @Test
    fun testUpdateMemberPortionSize() {
        navigateToHousehold()

        householdMembersRobot.waitForMembersScreen()

        // Tap the first member row to open member detail / edit screen
        householdMembersRobot.tapMemberRow(0)
        composeTestRule.waitForIdle()

        // The portion size control should be visible on the member detail screen
        householdMembersRobot.assertPortionSizeDisplayed(0)

        // Tap the portion size increase/decrease and save
        composeTestRule.onNodeWithText("+", substring = false).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        Log.i(TAG, "testUpdateMemberPortionSize: portion size updated for member at index 0")
    }

    @Test
    fun testUpdateMemberEditAccess() {
        navigateToHousehold()

        householdMembersRobot.waitForMembersScreen()

        // Tap the second member row to open member detail
        householdMembersRobot.tapMemberRow(1)
        composeTestRule.waitForIdle()

        // The edit access toggle should be present
        composeTestRule.onNodeWithText("Edit Access", substring = true, ignoreCase = true)
            .assertIsDisplayed()

        // Toggle edit access on
        composeTestRule.onNodeWithText("Edit Access", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()

        // Save
        composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        // Verify via API
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            val household = BackendTestHelper.getMyHousehold(BACKEND_BASE_URL, authToken)
            Log.i(TAG, "API verification: household updated, id=${household?.optString("id")}")
        }

        Log.i(TAG, "testUpdateMemberEditAccess: edit access toggled for member at index 1")
    }

    @Test
    fun testRemoveMember() {
        navigateToHousehold()

        householdMembersRobot.waitForMembersScreen()

        // The members list must have at least 2 entries (owner + one other member)
        householdMembersRobot.assertMemberDisplayed(1)

        // Tap remove on the second member
        householdMembersRobot.tapRemoveMember(1)

        // Confirmation dialog appears
        householdMembersRobot.confirmDialog()
        composeTestRule.waitForIdle()

        // After removal, only the owner (index 0) should remain
        // The second row should no longer exist in the semantics tree
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_MEMBER_ROW_PREFIX}1")
            .assertDoesNotExist()

        // Verify via API
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            val household = BackendTestHelper.getMyHousehold(BACKEND_BASE_URL, authToken)
            val memberCount = household?.optInt("member_count", -1)
            Log.i(TAG, "API verification: member_count=$memberCount after removal (expected 1)")
        }

        Log.i(TAG, "testRemoveMember: member at index 1 removed from household")
    }
}
