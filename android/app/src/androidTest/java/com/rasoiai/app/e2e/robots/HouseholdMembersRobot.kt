package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.presentation.common.TestTags

/**
 * Robot for Household Members screen interactions.
 * Covers add, remove, view role, portion size, edit access, and invite code flows.
 */
class HouseholdMembersRobot(private val composeTestRule: ComposeContentTestRule) {

    fun waitForMembersScreen(timeoutMillis: Long = 10000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(TestTags.HOUSEHOLD_MEMBERS_LIST, timeoutMillis)
    }

    fun assertMembersListDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEMBERS_LIST).assertIsDisplayed()
    }

    fun assertMemberDisplayed(index: Int) = apply {
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_MEMBER_ROW_PREFIX}$index")
            .assertIsDisplayed()
    }

    fun assertMemberRole(index: Int, role: String) = apply {
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_MEMBER_ROLE_PREFIX}$index")
            .assertTextContains(role, substring = true, ignoreCase = true)
    }

    fun tapAddMember() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_ADD_MEMBER_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    fun enterPhoneNumber(phone: String) = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_ADD_MEMBER_PHONE_FIELD)
            .performTextInput(phone)
        composeTestRule.waitForIdle()
    }

    fun tapRemoveMember(index: Int) = apply {
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_MEMBER_REMOVE_PREFIX}$index")
            .performClick()
        composeTestRule.waitForIdle()
    }

    fun tapMemberRow(index: Int) = apply {
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_MEMBER_ROW_PREFIX}$index")
            .performClick()
        composeTestRule.waitForIdle()
    }

    fun assertPortionSizeDisplayed(index: Int) = apply {
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_MEMBER_PORTION_SIZE_PREFIX}$index")
            .assertIsDisplayed()
    }

    fun assertLeaveButtonNotPresent() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_LEAVE_BUTTON).assertDoesNotExist()
    }

    fun tapLeaveHousehold() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_LEAVE_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    fun tapJoinWithCode(inviteCode: String) = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_JOIN_CODE_FIELD)
            .performTextInput(inviteCode)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_JOIN_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    fun assertErrorDisplayed(errorText: String) = apply {
        composeTestRule.onNodeWithText(errorText, substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    fun confirmDialog() = apply {
        composeTestRule.onNodeWithText("Confirm", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    fun cancelDialog() = apply {
        composeTestRule.onNodeWithText("Cancel", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    fun tapTransferOwnership() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_TRANSFER_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    fun assertTransferDialogDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_TRANSFER_DIALOG).assertIsDisplayed()
    }

    fun assertInviteCodeDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_INVITE_CODE_DISPLAY).assertIsDisplayed()
    }

    fun tapRefreshInviteCode() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_INVITE_REFRESH_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }
}
