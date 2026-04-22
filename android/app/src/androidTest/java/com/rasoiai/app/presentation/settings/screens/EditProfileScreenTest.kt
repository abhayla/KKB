package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screen_displaysTitle() {
        composeTestRule.setContent {
            RasoiAITheme {
                EditProfileTestContent(
                    uiState = EditProfileUiState(
                        isLoading = false,
                        isSaving = false,
                        name = "Abhay Sharma",
                        email = "abhay@example.com",
                        profileImageUrl = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Edit Profile", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_hidesContent() {
        composeTestRule.setContent {
            RasoiAITheme {
                EditProfileTestContent(
                    uiState = EditProfileUiState(
                        isLoading = true,
                        isSaving = false,
                        name = "",
                        email = "",
                        profileImageUrl = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Abhay", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun name_displayedWhenLoaded() {
        composeTestRule.setContent {
            RasoiAITheme {
                EditProfileTestContent(
                    uiState = EditProfileUiState(
                        isLoading = false,
                        isSaving = false,
                        name = "Abhay Sharma",
                        email = "abhay@example.com",
                        profileImageUrl = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Abhay Sharma", substring = true, ignoreCase = false)
            .assertIsDisplayed()
    }

    @Test
    fun email_displayedWhenLoaded() {
        composeTestRule.setContent {
            RasoiAITheme {
                EditProfileTestContent(
                    uiState = EditProfileUiState(
                        isLoading = false,
                        isSaving = false,
                        name = "Abhay Sharma",
                        email = "abhay@example.com",
                        profileImageUrl = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("abhay@example.com", substring = true, ignoreCase = false)
            .assertIsDisplayed()
    }

    @Test
    fun screen_displaysSaveButton() {
        composeTestRule.setContent {
            RasoiAITheme {
                EditProfileTestContent(
                    uiState = EditProfileUiState(
                        isLoading = false,
                        isSaving = false,
                        name = "Abhay Sharma",
                        email = "abhay@example.com",
                        profileImageUrl = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Save", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun saveButtonClick_callsOnSave() {
        var saveCalled = false
        composeTestRule.setContent {
            RasoiAITheme {
                EditProfileTestContent(
                    uiState = EditProfileUiState(
                        isLoading = false,
                        isSaving = false,
                        name = "Abhay Sharma",
                        email = "abhay@example.com",
                        profileImageUrl = null
                    ),
                    onSave = { saveCalled = true }
                )
            }
        }
        composeTestRule.onNodeWithText("Save", substring = true, ignoreCase = true)
            .performClick()
        assert(saveCalled) { "onSave callback was not triggered" }
    }
}

@androidx.compose.runtime.Composable
private fun EditProfileTestContent(
    uiState: EditProfileUiState,
    onNavigateBack: () -> Unit = {},
    onUpdateName: (String) -> Unit = {},
    onSave: () -> Unit = {}
) {
    EditProfileScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onUpdateName = onUpdateName,
        onSave = onSave
    )
}
