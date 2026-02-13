package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.CuisineType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CuisinePreferencesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setupScreen(uiState: CuisinePreferencesUiState): CuisinePreferencesViewModel {
        val mockViewModel = mockk<CuisinePreferencesViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        composeTestRule.setContent {
            RasoiAITheme {
                CuisinePreferencesScreen(onNavigateBack = {}, viewModel = mockViewModel)
            }
        }
        return mockViewModel
    }

    @Test
    fun screen_displaysTitle() {
        setupScreen(
            CuisinePreferencesUiState(
                isLoading = false,
                isSaving = false,
                selectedCuisines = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Cuisine Preferences").assertIsDisplayed()
    }

    @Test
    fun screen_displaysSaveButton() {
        setupScreen(
            CuisinePreferencesUiState(
                isLoading = false,
                isSaving = false,
                selectedCuisines = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Save").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadingState_hidesContent() {
        setupScreen(
            CuisinePreferencesUiState(
                isLoading = true,
                isSaving = false,
                selectedCuisines = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun cuisineOptions_areDisplayed() {
        setupScreen(
            CuisinePreferencesUiState(
                isLoading = false,
                isSaving = false,
                selectedCuisines = setOf(CuisineType.NORTH),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("North Indian").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("South Indian").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saveButton_callsViewModelSave() {
        val mockViewModel = setupScreen(
            CuisinePreferencesUiState(
                isLoading = false,
                isSaving = false,
                selectedCuisines = setOf(CuisineType.NORTH, CuisineType.SOUTH),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Save").performScrollTo().performClick()

        verify { mockViewModel.save() }
    }
}
