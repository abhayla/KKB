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
import com.rasoiai.domain.model.SpecialDietaryNeed
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

    @Test
    fun screen_displaysTitle() {
        composeTestRule.setContent {
            RasoiAITheme {
                FamilyMembersTestContent(
                    uiState = FamilyMembersUiState(
                        isLoading = false,
                        familyMembers = emptyList(),
                        showAddEditSheet = false,
                        editingMember = null,
                        showDeleteDialog = false,
                        deletingMemberId = null,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Family Members").assertIsDisplayed()
    }

    @Test
    fun screen_displaysAddButton() {
        composeTestRule.setContent {
            RasoiAITheme {
                FamilyMembersTestContent(
                    uiState = FamilyMembersUiState(
                        isLoading = false,
                        familyMembers = emptyList(),
                        showAddEditSheet = false,
                        editingMember = null,
                        showDeleteDialog = false,
                        deletingMemberId = null,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Add", substring = true).assertIsDisplayed()
    }

    @Test
    fun loadingState_showsContent() {
        composeTestRule.setContent {
            RasoiAITheme {
                FamilyMembersTestContent(
                    uiState = FamilyMembersUiState(
                        isLoading = true,
                        familyMembers = emptyList(),
                        showAddEditSheet = false,
                        editingMember = null,
                        showDeleteDialog = false,
                        deletingMemberId = null,
                        errorMessage = null
                    )
                )
            }
        }
        // Loading state should still show the title and empty state
        composeTestRule.onNodeWithText("Family Members").assertIsDisplayed()
    }

    @Test
    fun familyMembers_displayedWhenListNonEmpty() {
        composeTestRule.setContent {
            RasoiAITheme {
                FamilyMembersTestContent(
                    uiState = FamilyMembersUiState(
                        isLoading = false,
                        familyMembers = sampleMembers,
                        showAddEditSheet = false,
                        editingMember = null,
                        showDeleteDialog = false,
                        deletingMemberId = null,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Aarav").assertIsDisplayed()
        composeTestRule.onNodeWithText("Priya").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dadaji").assertIsDisplayed()
    }

    @Test
    fun memberNames_shownCorrectly() {
        composeTestRule.setContent {
            RasoiAITheme {
                FamilyMembersTestContent(
                    uiState = FamilyMembersUiState(
                        isLoading = false,
                        familyMembers = sampleMembers,
                        showAddEditSheet = false,
                        editingMember = null,
                        showDeleteDialog = false,
                        deletingMemberId = null,
                        errorMessage = null
                    )
                )
            }
        }
        sampleMembers.forEach { member ->
            composeTestRule.onNodeWithText(member.name).assertIsDisplayed()
        }
    }

    @Test
    fun emptyState_showsAppropriateMessage() {
        composeTestRule.setContent {
            RasoiAITheme {
                FamilyMembersTestContent(
                    uiState = FamilyMembersUiState(
                        isLoading = false,
                        familyMembers = emptyList(),
                        showAddEditSheet = false,
                        editingMember = null,
                        showDeleteDialog = false,
                        deletingMemberId = null,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("No family members", substring = true).assertIsDisplayed()
    }

    @Test
    fun addButton_callsOnShowAddSheet() {
        var showAddSheetCalled = false
        composeTestRule.setContent {
            RasoiAITheme {
                FamilyMembersTestContent(
                    uiState = FamilyMembersUiState(
                        isLoading = false,
                        familyMembers = emptyList(),
                        showAddEditSheet = false,
                        editingMember = null,
                        showDeleteDialog = false,
                        deletingMemberId = null,
                        errorMessage = null
                    ),
                    onShowAddSheet = { showAddSheetCalled = true }
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Add", substring = true).performClick()
        assert(showAddSheetCalled) { "onShowAddSheet callback was not triggered" }
    }
}

@androidx.compose.runtime.Composable
private fun FamilyMembersTestContent(
    uiState: FamilyMembersUiState,
    onNavigateBack: () -> Unit = {},
    onShowAddSheet: () -> Unit = {},
    onShowEditSheet: (FamilyMember) -> Unit = {},
    onShowDeleteDialog: (String) -> Unit = {},
    onDismissSheet: () -> Unit = {},
    onSaveMember: (String, MemberType, Int?, List<SpecialDietaryNeed>) -> Unit = { _, _, _, _ -> },
    onDismissDeleteDialog: () -> Unit = {},
    onDeleteMember: () -> Unit = {}
) {
    FamilyMembersScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onShowAddSheet = onShowAddSheet,
        onShowEditSheet = onShowEditSheet,
        onShowDeleteDialog = onShowDeleteDialog,
        onDismissSheet = onDismissSheet,
        onSaveMember = onSaveMember,
        onDismissDeleteDialog = onDismissDeleteDialog,
        onDeleteMember = onDeleteMember
    )
}
