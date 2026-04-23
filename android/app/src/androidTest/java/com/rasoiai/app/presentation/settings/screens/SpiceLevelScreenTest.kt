package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.SpiceLevel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpiceLevelScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screen_displaysTitle() {
        composeTestRule.setContent {
            RasoiAITheme {
                SpiceLevelTestContent(
                    uiState = SpiceLevelUiState(
                        isLoading = false,
                        isSaving = false,
                        spiceLevel = SpiceLevel.MEDIUM,
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        // "Spice Level" appears in TopAppBar AND as a body section header — take the first match
        composeTestRule.onAllNodesWithText("Spice Level", substring = false, ignoreCase = false)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun screen_displaysSaveButton() {
        composeTestRule.setContent {
            RasoiAITheme {
                SpiceLevelTestContent(
                    uiState = SpiceLevelUiState(
                        isLoading = false,
                        isSaving = false,
                        spiceLevel = SpiceLevel.MEDIUM,
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        // Save button is outside the scrollable body Column — performScrollTo is not valid here
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun loadingState_hidesContent() {
        composeTestRule.setContent {
            RasoiAITheme {
                SpiceLevelTestContent(
                    uiState = SpiceLevelUiState(
                        isLoading = true,
                        isSaving = false,
                        spiceLevel = SpiceLevel.MEDIUM,
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun spiceLevelOptions_areDisplayed() {
        composeTestRule.setContent {
            RasoiAITheme {
                SpiceLevelTestContent(
                    uiState = SpiceLevelUiState(
                        isLoading = false,
                        isSaving = false,
                        spiceLevel = SpiceLevel.MEDIUM,
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        // Spice level is shown in a collapsed ExposedDropdownMenuBox; the dropdown items are only
        // visible when the menu is open. Assert only the current selection displayed in the TextField.
        composeTestRule.onNodeWithText(SpiceLevel.MEDIUM.displayName).assertIsDisplayed()
    }

    @Test
    fun saveButton_callsOnSave() {
        var saveCalled = false
        composeTestRule.setContent {
            RasoiAITheme {
                SpiceLevelTestContent(
                    uiState = SpiceLevelUiState(
                        isLoading = false,
                        isSaving = false,
                        spiceLevel = SpiceLevel.MEDIUM,
                        saveSuccess = false,
                        errorMessage = null
                    ),
                    onSave = { saveCalled = true }
                )
            }
        }
        // Save button is outside the scrollable body Column — performScrollTo is not valid here
        composeTestRule.onNodeWithText("Save").performClick()
        assert(saveCalled) { "onSave callback was not triggered" }
    }
}

@androidx.compose.runtime.Composable
private fun SpiceLevelTestContent(
    uiState: SpiceLevelUiState,
    onNavigateBack: () -> Unit = {},
    onUpdateSpiceLevel: (SpiceLevel) -> Unit = {},
    onSave: () -> Unit = {}
) {
    SpiceLevelScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onUpdateSpiceLevel = onUpdateSpiceLevel,
        onSave = onSave
    )
}
