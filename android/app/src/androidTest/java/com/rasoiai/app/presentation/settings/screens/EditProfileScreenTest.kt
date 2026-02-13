package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setupScreen(uiState: EditProfileUiState): EditProfileViewModel {
        val mockViewModel = mockk<EditProfileViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        composeTestRule.setContent {
            RasoiAITheme {
                EditProfileScreen(onNavigateBack = {}, viewModel = mockViewModel)
            }
        }
        return mockViewModel
    }

    @Test
    fun screen_displaysTitle() {
        setupScreen(
            EditProfileUiState(
                isLoading = false,
                isSaving = false,
                name = "Abhay Sharma",
                email = "abhay@example.com",
                profileImageUrl = null
            )
        )
        composeTestRule.onNodeWithText("Edit Profile", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_hidesContent() {
        setupScreen(
            EditProfileUiState(
                isLoading = true,
                isSaving = false,
                name = "",
                email = "",
                profileImageUrl = null
            )
        )
        composeTestRule.onNodeWithText("Abhay", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun name_displayedWhenLoaded() {
        setupScreen(
            EditProfileUiState(
                isLoading = false,
                isSaving = false,
                name = "Abhay Sharma",
                email = "abhay@example.com",
                profileImageUrl = null
            )
        )
        composeTestRule.onNodeWithText("Abhay Sharma", substring = true, ignoreCase = false)
            .assertIsDisplayed()
    }

    @Test
    fun email_displayedWhenLoaded() {
        setupScreen(
            EditProfileUiState(
                isLoading = false,
                isSaving = false,
                name = "Abhay Sharma",
                email = "abhay@example.com",
                profileImageUrl = null
            )
        )
        composeTestRule.onNodeWithText("abhay@example.com", substring = true, ignoreCase = false)
            .assertIsDisplayed()
    }

    @Test
    fun screen_displaysSaveButton() {
        setupScreen(
            EditProfileUiState(
                isLoading = false,
                isSaving = false,
                name = "Abhay Sharma",
                email = "abhay@example.com",
                profileImageUrl = null
            )
        )
        composeTestRule.onNodeWithText("Save", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun saveButtonClick_callsViewModelSave() {
        val mockViewModel = setupScreen(
            EditProfileUiState(
                isLoading = false,
                isSaving = false,
                name = "Abhay Sharma",
                email = "abhay@example.com",
                profileImageUrl = null
            )
        )
        composeTestRule.onNodeWithText("Save", substring = true, ignoreCase = true)
            .performClick()
        verify { mockViewModel.save() }
    }
}
