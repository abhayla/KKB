package com.rasoiai.app.presentation.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.settings.screens.HouseholdMembersScreen
import com.rasoiai.app.presentation.settings.viewmodels.HouseholdMembersUiState
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.HouseholdMember
import com.rasoiai.domain.model.HouseholdRole
import com.rasoiai.domain.model.MemberStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * UI Tests for HouseholdMembersScreen
 *
 * Tests member list display, role badges, add member FAB,
 * and remove buttons for owner users.
 */
@RunWith(AndroidJUnit4::class)
class HouseholdMembersScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDateTime = LocalDateTime.of(2026, 3, 1, 10, 0)

    private val testMembers = listOf(
        HouseholdMember(
            id = "mem-1", userId = "user-1", familyMemberId = null,
            name = "Ramesh", role = HouseholdRole.OWNER,
            canEditSharedPlan = true, joinDate = testDateTime,
            portionSize = 1.0f, status = MemberStatus.ACTIVE
        ),
        HouseholdMember(
            id = "mem-2", userId = "user-2", familyMemberId = null,
            name = "Sunita", role = HouseholdRole.MEMBER,
            canEditSharedPlan = false, joinDate = testDateTime,
            portionSize = 1.0f, status = MemberStatus.ACTIVE
        )
    )

    private fun setScreen(uiState: HouseholdMembersUiState) {
        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdMembersScreen(
                    uiState = uiState,
                    snackbarHostState = SnackbarHostState(),
                    onNavigateBack = {},
                    onPhoneChanged = {},
                    onAddMember = {},
                    onRemoveMember = {},
                    onShowAddDialog = {},
                    onDismissAddDialog = {}
                )
            }
        }
    }

    @Test
    fun loadingState_displayed() {
        setScreen(HouseholdMembersUiState(isLoading = true))
        // Screen should render without crash during loading
    }

    @Test
    fun memberList_displaysCorrectCount() {
        setScreen(HouseholdMembersUiState(members = testMembers, isOwner = true))
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEMBERS_LIST).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEMBER_ROW_PREFIX + "mem-1").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEMBER_ROW_PREFIX + "mem-2").assertIsDisplayed()
    }

    @Test
    fun addMemberFAB_visibleForOwner() {
        setScreen(HouseholdMembersUiState(members = testMembers, isOwner = true))
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_ADD_MEMBER_BUTTON).assertIsDisplayed()
    }

    @Test
    fun emptyState_whenNoMembers() {
        setScreen(HouseholdMembersUiState(members = emptyList()))
        composeTestRule.onNodeWithText("No members in this household yet.").assertIsDisplayed()
    }

    @Test
    fun memberRows_displayRoleBadges() {
        setScreen(HouseholdMembersUiState(members = testMembers, isOwner = true))
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEMBER_ROLE_PREFIX + "mem-1").assertIsDisplayed()
    }

    @Test
    fun removeButton_visibleForNonOwnerMember() {
        setScreen(HouseholdMembersUiState(members = testMembers, isOwner = true))
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEMBER_REMOVE_PREFIX + "mem-2").assertIsDisplayed()
    }
}
