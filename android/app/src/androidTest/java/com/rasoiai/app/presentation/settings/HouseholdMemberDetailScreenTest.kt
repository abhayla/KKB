package com.rasoiai.app.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.settings.screens.HouseholdMemberDetailScreen
import com.rasoiai.app.presentation.settings.screens.HouseholdMemberDetailUiState
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.HouseholdMember
import com.rasoiai.domain.model.HouseholdRole
import com.rasoiai.domain.model.MemberStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * UI Tests for HouseholdMemberDetailScreen
 *
 * Tests member detail view including name display, portion size control,
 * edit permissions toggle, and remove button visibility.
 */
@RunWith(AndroidJUnit4::class)
class HouseholdMemberDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testMember = HouseholdMember(
        id = "mem-1",
        userId = "user-1",
        familyMemberId = null,
        name = "Ramesh Sharma",
        role = HouseholdRole.MEMBER,
        canEditSharedPlan = false,
        joinDate = LocalDateTime.of(2026, 3, 1, 10, 0),
        portionSize = 1.0f,
        status = MemberStatus.ACTIVE
    )

    private fun setScreen(uiState: HouseholdMemberDetailUiState) {
        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdMemberDetailScreen(
                    uiState = uiState,
                    onNavigateBack = {},
                    onCanEditChanged = {},
                    onPortionSizeChanged = {},
                    onTemporaryChanged = {},
                    onSave = {},
                    onRemove = {},
                    onShowRemoveDialog = {},
                    onDismissRemoveDialog = {}
                )
            }
        }
    }

    @Test
    fun loadingState_displayed() {
        setScreen(HouseholdMemberDetailUiState(isLoading = true))
        // Should render without crash
    }

    @Test
    fun memberName_displayedInTopBar() {
        setScreen(HouseholdMemberDetailUiState(member = testMember))
        composeTestRule.onNodeWithText("Ramesh Sharma").assertIsDisplayed()
    }

    @Test
    fun portionSizeControl_displayed() {
        setScreen(HouseholdMemberDetailUiState(member = testMember, portionSize = 1.0f))
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_MEMBER_PORTION_SIZE_PREFIX + "mem-1").assertIsDisplayed()
    }

    @Test
    fun editPermissionsToggle_displayed() {
        setScreen(HouseholdMemberDetailUiState(member = testMember, isOwner = true))
        composeTestRule.onNodeWithText("Can edit shared plan").assertIsDisplayed()
    }

    @Test
    fun removeButton_displayedForOwner() {
        setScreen(HouseholdMemberDetailUiState(member = testMember, isOwner = true))
        composeTestRule.onNodeWithText("Remove Member").assertIsDisplayed()
    }
}
