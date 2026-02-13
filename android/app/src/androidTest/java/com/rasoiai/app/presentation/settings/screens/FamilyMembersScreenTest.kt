package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.MemberType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FamilyMembersScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleMembers = listOf(
        FamilyMember(
            id = "1",
            name = "Aarav",
            type = MemberType.CHILD,
            age = 8,
            specialNeeds = emptyList()
        ),
        FamilyMember(
            id = "2",
            name = "Priya",
            type = MemberType.ADULT,
            age = 32,
            specialNeeds = emptyList()
        ),
        FamilyMember(
            id = "3",
            name = "Dadaji",
            type = MemberType.SENIOR,
            age = 70,
            specialNeeds = emptyList()
        )
    )

    private fun setupScreen(uiState: FamilyMembersUiState): FamilyMembersViewModel {
        val mockViewModel = mockk<FamilyMembersViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        composeTestRule.setContent {
            RasoiAITheme {
                FamilyMembersScreen(onNavigateBack = {}, viewModel = mockViewModel)
            }
        }
        return mockViewModel
    }

    @Test
    fun screen_displaysTitle() {
        setupScreen(
            FamilyMembersUiState(
                isLoading = false,
                familyMembers = emptyList(),
                showAddEditSheet = false,
                editingMember = null,
                showDeleteDialog = false,
                deletingMemberId = null,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Family Members").assertIsDisplayed()
    }

    @Test
    fun screen_displaysAddButton() {
        setupScreen(
            FamilyMembersUiState(
                isLoading = false,
                familyMembers = emptyList(),
                showAddEditSheet = false,
                editingMember = null,
                showDeleteDialog = false,
                deletingMemberId = null,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithContentDescription("Add", substring = true).assertIsDisplayed()
    }

    @Test
    fun loadingState_showsContent() {
        setupScreen(
            FamilyMembersUiState(
                isLoading = true,
                familyMembers = emptyList(),
                showAddEditSheet = false,
                editingMember = null,
                showDeleteDialog = false,
                deletingMemberId = null,
                errorMessage = null
            )
        )

        // Loading state should still show the title and empty state
        composeTestRule.onNodeWithText("Family Members").assertIsDisplayed()
    }

    @Test
    fun familyMembers_displayedWhenListNonEmpty() {
        setupScreen(
            FamilyMembersUiState(
                isLoading = false,
                familyMembers = sampleMembers,
                showAddEditSheet = false,
                editingMember = null,
                showDeleteDialog = false,
                deletingMemberId = null,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Aarav").assertIsDisplayed()
        composeTestRule.onNodeWithText("Priya").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dadaji").assertIsDisplayed()
    }

    @Test
    fun memberNames_shownCorrectly() {
        setupScreen(
            FamilyMembersUiState(
                isLoading = false,
                familyMembers = sampleMembers,
                showAddEditSheet = false,
                editingMember = null,
                showDeleteDialog = false,
                deletingMemberId = null,
                errorMessage = null
            )
        )

        sampleMembers.forEach { member ->
            composeTestRule.onNodeWithText(member.name).assertIsDisplayed()
        }
    }

    @Test
    fun emptyState_showsAppropriateMessage() {
        setupScreen(
            FamilyMembersUiState(
                isLoading = false,
                familyMembers = emptyList(),
                showAddEditSheet = false,
                editingMember = null,
                showDeleteDialog = false,
                deletingMemberId = null,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("No family members", substring = true).assertIsDisplayed()
    }

    @Test
    fun addButton_callsViewModelShowAddSheet() {
        val mockViewModel = setupScreen(
            FamilyMembersUiState(
                isLoading = false,
                familyMembers = emptyList(),
                showAddEditSheet = false,
                editingMember = null,
                showDeleteDialog = false,
                deletingMemberId = null,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithContentDescription("Add", substring = true).performClick()

        verify { mockViewModel.showAddSheet() }
    }
}
