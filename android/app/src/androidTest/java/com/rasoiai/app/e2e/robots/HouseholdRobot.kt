package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.presentation.common.TestTags

/**
 * Robot for Household setup screen interactions.
 * Covers create, view, update, deactivate, invite code, leave, and transfer ownership flows.
 *
 * Pattern: every method returns `apply {}` to support fluent chaining.
 */
class HouseholdRobot(private val composeTestRule: ComposeContentTestRule) {

    fun waitForHouseholdScreen(timeoutMillis: Long = 10000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(TestTags.HOUSEHOLD_SCREEN, timeoutMillis)
    }

    fun assertHouseholdScreenDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_SCREEN).assertIsDisplayed()
    }

    fun enterHouseholdName(name: String) = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_NAME_FIELD)
            .performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_NAME_FIELD)
            .performTextInput(name)
        composeTestRule.waitForIdle()
    }

    fun tapCreateHousehold() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_CREATE_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    fun tapDeactivateHousehold() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_DEACTIVATE_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    fun assertInviteCodeDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_INVITE_CODE_DISPLAY).assertIsDisplayed()
    }

    fun tapShareInviteCode() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_INVITE_SHARE_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    fun tapRefreshInviteCode() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_INVITE_REFRESH_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    fun tapTransferOwnership() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_TRANSFER_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    fun assertTransferDialogDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_TRANSFER_DIALOG).assertIsDisplayed()
    }

    fun tapLeaveHousehold() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_LEAVE_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    fun assertHouseholdNameDisplayed(name: String) = apply {
        composeTestRule.onNodeWithText(name, substring = true).assertIsDisplayed()
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

    /**
     * Assert the household members list is visible — used to verify the owner
     * is automatically added as a member after creation.
     */
    fun assertMembersListDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEMBERS_LIST).assertIsDisplayed()
    }
}
