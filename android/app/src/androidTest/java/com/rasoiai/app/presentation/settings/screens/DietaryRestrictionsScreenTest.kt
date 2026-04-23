package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.DietaryRestriction
import com.rasoiai.domain.model.PrimaryDiet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DietaryRestrictionsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screen_displaysTitle() {
        composeTestRule.setContent {
            RasoiAITheme {
                DietaryRestrictionsTestContent(
                    uiState = DietaryRestrictionsUiState(
                        isLoading = false,
                        isSaving = false,
                        primaryDiet = PrimaryDiet.VEGETARIAN,
                        dietaryRestrictions = emptySet(),
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Dietary Restrictions").assertIsDisplayed()
    }

    @Test
    fun screen_displaysPrimaryDietSectionHeader() {
        composeTestRule.setContent {
            RasoiAITheme {
                DietaryRestrictionsTestContent(
                    uiState = DietaryRestrictionsUiState(
                        isLoading = false,
                        isSaving = false,
                        primaryDiet = PrimaryDiet.VEGETARIAN,
                        dietaryRestrictions = emptySet(),
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Primary Diet").assertIsDisplayed()
    }

    @Test
    fun screen_displaysAllPrimaryDietOptions() {
        composeTestRule.setContent {
            RasoiAITheme {
                DietaryRestrictionsTestContent(
                    uiState = DietaryRestrictionsUiState(
                        isLoading = false,
                        isSaving = false,
                        primaryDiet = PrimaryDiet.VEGETARIAN,
                        dietaryRestrictions = emptySet(),
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        PrimaryDiet.entries.forEach { diet ->
            composeTestRule.onNodeWithText(diet.displayName).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun screen_displaysSpecialDietaryRestrictionsSectionHeader() {
        composeTestRule.setContent {
            RasoiAITheme {
                DietaryRestrictionsTestContent(
                    uiState = DietaryRestrictionsUiState(
                        isLoading = false,
                        isSaving = false,
                        primaryDiet = PrimaryDiet.VEGETARIAN,
                        dietaryRestrictions = emptySet(),
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Special Dietary Restrictions").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun screen_displaysSaveButton() {
        composeTestRule.setContent {
            RasoiAITheme {
                DietaryRestrictionsTestContent(
                    uiState = DietaryRestrictionsUiState(
                        isLoading = false,
                        isSaving = false,
                        primaryDiet = PrimaryDiet.VEGETARIAN,
                        dietaryRestrictions = emptySet(),
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
                DietaryRestrictionsTestContent(
                    uiState = DietaryRestrictionsUiState(
                        isLoading = true,
                        isSaving = false,
                        primaryDiet = PrimaryDiet.VEGETARIAN,
                        dietaryRestrictions = emptySet(),
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Primary Diet").assertDoesNotExist()
    }

    @Test
    fun saveButton_callsOnSave() {
        var saveCalled = false
        composeTestRule.setContent {
            RasoiAITheme {
                DietaryRestrictionsTestContent(
                    uiState = DietaryRestrictionsUiState(
                        isLoading = false,
                        isSaving = false,
                        primaryDiet = PrimaryDiet.VEGETARIAN,
                        dietaryRestrictions = emptySet(),
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
private fun DietaryRestrictionsTestContent(
    uiState: DietaryRestrictionsUiState,
    onNavigateBack: () -> Unit = {},
    onUpdatePrimaryDiet: (PrimaryDiet) -> Unit = {},
    onToggleDietaryRestriction: (DietaryRestriction) -> Unit = {},
    onSave: () -> Unit = {}
) {
    DietaryRestrictionsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onUpdatePrimaryDiet = onUpdatePrimaryDiet,
        onToggleDietaryRestriction = onToggleDietaryRestriction,
        onSave = onSave
    )
}
