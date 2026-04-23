package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DislikedIngredientsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screen_displaysTitle() {
        composeTestRule.setContent {
            RasoiAITheme {
                DislikedIngredientsTestContent(
                    uiState = DislikedIngredientsUiState(
                        isLoading = false,
                        isSaving = false,
                        dislikedIngredients = emptySet(),
                        searchQuery = "",
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Disliked Ingredients").assertIsDisplayed()
    }

    @Test
    fun screen_displaysSaveButton() {
        composeTestRule.setContent {
            RasoiAITheme {
                DislikedIngredientsTestContent(
                    uiState = DislikedIngredientsUiState(
                        isLoading = false,
                        isSaving = false,
                        dislikedIngredients = emptySet(),
                        searchQuery = "",
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
                DislikedIngredientsTestContent(
                    uiState = DislikedIngredientsUiState(
                        isLoading = true,
                        isSaving = false,
                        dislikedIngredients = emptySet(),
                        searchQuery = "",
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun dislikedIngredients_shownAsChips() {
        composeTestRule.setContent {
            RasoiAITheme {
                DislikedIngredientsTestContent(
                    uiState = DislikedIngredientsUiState(
                        isLoading = false,
                        isSaving = false,
                        dislikedIngredients = setOf("Bitter Gourd", "Okra", "Eggplant"),
                        searchQuery = "",
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        // Custom ingredients appear in a FlowRow below the common-dislikes section; scroll to them
        composeTestRule.onNodeWithText("Bitter Gourd").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Okra").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Eggplant").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun searchField_isVisible() {
        composeTestRule.setContent {
            RasoiAITheme {
                DislikedIngredientsTestContent(
                    uiState = DislikedIngredientsUiState(
                        isLoading = false,
                        isSaving = false,
                        dislikedIngredients = emptySet(),
                        searchQuery = "",
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        // Placeholder text is "Search ingredients..." — use substring match
        composeTestRule.onNodeWithText("Search ingredients", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun saveButton_callsOnSave() {
        var saveCalled = false
        composeTestRule.setContent {
            RasoiAITheme {
                DislikedIngredientsTestContent(
                    uiState = DislikedIngredientsUiState(
                        isLoading = false,
                        isSaving = false,
                        dislikedIngredients = setOf("Bitter Gourd"),
                        searchQuery = "",
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
private fun DislikedIngredientsTestContent(
    uiState: DislikedIngredientsUiState,
    onNavigateBack: () -> Unit = {},
    onUpdateSearchQuery: (String) -> Unit = {},
    onAddCustomIngredient: (String) -> Unit = {},
    onToggleIngredient: (String) -> Unit = {},
    onSave: () -> Unit = {}
) {
    DislikedIngredientsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onUpdateSearchQuery = onUpdateSearchQuery,
        onAddCustomIngredient = onAddCustomIngredient,
        onToggleIngredient = onToggleIngredient,
        onSave = onSave
    )
}
