package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.CuisineType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CuisinePreferencesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screen_displaysTitle() {
        composeTestRule.setContent {
            RasoiAITheme {
                CuisinePreferencesTestContent(
                    uiState = CuisinePreferencesUiState(
                        isLoading = false,
                        isSaving = false,
                        selectedCuisines = emptySet(),
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Cuisine Preferences").assertIsDisplayed()
    }

    @Test
    fun screen_displaysSaveButton() {
        composeTestRule.setContent {
            RasoiAITheme {
                CuisinePreferencesTestContent(
                    uiState = CuisinePreferencesUiState(
                        isLoading = false,
                        isSaving = false,
                        selectedCuisines = emptySet(),
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
                CuisinePreferencesTestContent(
                    uiState = CuisinePreferencesUiState(
                        isLoading = true,
                        isSaving = false,
                        selectedCuisines = emptySet(),
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun cuisineOptions_areDisplayed() {
        composeTestRule.setContent {
            RasoiAITheme {
                CuisinePreferencesTestContent(
                    uiState = CuisinePreferencesUiState(
                        isLoading = false,
                        isSaving = false,
                        selectedCuisines = setOf(CuisineType.NORTH),
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("North Indian").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("South Indian").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saveButton_callsOnSave() {
        var saveCalled = false
        composeTestRule.setContent {
            RasoiAITheme {
                CuisinePreferencesTestContent(
                    uiState = CuisinePreferencesUiState(
                        isLoading = false,
                        isSaving = false,
                        selectedCuisines = setOf(CuisineType.NORTH, CuisineType.SOUTH),
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
private fun CuisinePreferencesTestContent(
    uiState: CuisinePreferencesUiState,
    onNavigateBack: () -> Unit = {},
    onToggleCuisine: (CuisineType) -> Unit = {},
    onSave: () -> Unit = {}
) {
    CuisinePreferencesScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onToggleCuisine = onToggleCuisine,
        onSave = onSave
    )
}
