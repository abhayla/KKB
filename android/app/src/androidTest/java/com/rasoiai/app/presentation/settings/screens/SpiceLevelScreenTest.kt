package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        composeTestRule.onNodeWithText("Spice Level").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("Save").performScrollTo().assertIsDisplayed()
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
        SpiceLevel.entries.forEach { level ->
            composeTestRule.onNodeWithText(level.displayName).performScrollTo().assertIsDisplayed()
        }
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
        composeTestRule.onNodeWithText("Save").performScrollTo().performClick()
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
