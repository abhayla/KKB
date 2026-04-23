package com.rasoiai.app.presentation.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.settings.screens.HouseholdScreen
import com.rasoiai.app.presentation.settings.viewmodels.HouseholdUiState
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.Household
import com.rasoiai.domain.model.HouseholdDetail
import com.rasoiai.domain.model.HouseholdMember
import com.rasoiai.domain.model.HouseholdRole
import com.rasoiai.domain.model.MemberStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * UI Tests for HouseholdScreen
 *
 * Tests the household management screen including create form,
 * household details, invite code, and owner/member actions.
 */
@RunWith(AndroidJUnit4::class)
class HouseholdScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDateTime = LocalDateTime.of(2026, 3, 1, 10, 0)

    private val testHousehold = Household(
        id = "hh-1",
        name = "Sharma Family",
        inviteCode = "ABC123",
        ownerId = "user-1",
        maxMembers = 8,
        memberCount = 3,
        isActive = true,
        createdAt = testDateTime,
        updatedAt = testDateTime
    )

    private val testOwnerMember = HouseholdMember(
        id = "mem-1",
        userId = "user-1",
        familyMemberId = null,
        name = "Ramesh",
        role = HouseholdRole.OWNER,
        canEditSharedPlan = true,
        joinDate = testDateTime,
        portionSize = 1.0f,
        status = MemberStatus.ACTIVE
    )

    private val testMember = HouseholdMember(
        id = "mem-2",
        userId = "user-2",
        familyMemberId = null,
        name = "Sunita",
        role = HouseholdRole.MEMBER,
        canEditSharedPlan = false,
        joinDate = testDateTime,
        portionSize = 1.0f,
        status = MemberStatus.ACTIVE
    )

    private val testDetail = HouseholdDetail(
        household = testHousehold,
        members = listOf(testOwnerMember, testMember)
    )

    private fun setScreen(uiState: HouseholdUiState) {
        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdScreen(
                    uiState = uiState,
                    snackbarHostState = SnackbarHostState(),
                    onNavigateBack = {},
                    onHouseholdNameChanged = {},
                    onCreateHousehold = {},
                    onRefreshInviteCode = {},
                    onNavigateToMembers = {},
                    onInviteCodeCopied = {},
                    onShareInviteCode = {},
                    onShowDeactivateDialog = {},
                    onDismissDeactivateDialog = {},
                    onConfirmDeactivate = {},
                    onShowLeaveDialog = {},
                    onDismissLeaveDialog = {},
                    onConfirmLeave = {},
                    onShowTransferDialog = {},
                    onDismissTransferDialog = {},
                    onConfirmTransfer = {}
                )
            }
        }
    }

    @Test
    fun loadingIndicator_displayedWhenLoading() {
        setScreen(HouseholdUiState(isLoading = true))
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_SCREEN).assertIsDisplayed()
    }

    @Test
    fun createForm_shownWhenNoHousehold() {
        setScreen(HouseholdUiState(householdDetail = null))
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_NAME_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_CREATE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun householdDetails_shownWhenHouseholdExists() {
        setScreen(HouseholdUiState(householdDetail = testDetail, householdName = "Sharma Family"))
        composeTestRule.onNodeWithText("Sharma Family").assertIsDisplayed()
    }

    @Test
    fun inviteCodeSection_visible() {
        setScreen(HouseholdUiState(householdDetail = testDetail, inviteCode = "ABC123"))
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_INVITE_CODE).assertIsDisplayed()
    }

    @Test
    fun deactivateButton_visibleForOwner() {
        setScreen(HouseholdUiState(householdDetail = testDetail))
        // Deactivate button sits inside HouseholdDetailContent (near the bottom of the
        // screen's verticalScroll Column) — on compact emulator heights it renders below
        // the fold, so scroll before asserting visibility.
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_DEACTIVATE_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun leaveButton_visibleForNonOwner() {
        val nonOwnerDetail = HouseholdDetail(
            household = testHousehold.copy(ownerId = "other-user"),
            members = listOf(testMember)
        )
        setScreen(HouseholdUiState(householdDetail = nonOwnerDetail))
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_LEAVE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun transferButton_visibleForOwner() {
        setScreen(HouseholdUiState(householdDetail = testDetail))
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_TRANSFER_BUTTON).assertIsDisplayed()
    }
}
