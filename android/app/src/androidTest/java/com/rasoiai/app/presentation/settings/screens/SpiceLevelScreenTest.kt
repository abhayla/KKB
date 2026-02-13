package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.SpiceLevel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpiceLevelScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setupScreen(uiState: SpiceLevelUiState): SpiceLevelViewModel {
        val mockViewModel = mockk<SpiceLevelViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        composeTestRule.setContent {
            RasoiAITheme {
                SpiceLevelScreen(onNavigateBack = {}, viewModel = mockViewModel)
            }
        }
        return mockViewModel
    }

    @Test
    fun screen_displaysTitle() {
        setupScreen(
            SpiceLevelUiState(
                isLoading = false,
                isSaving = false,
                spiceLevel = SpiceLevel.MEDIUM,
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Spice Level").assertIsDisplayed()
    }

    @Test
    fun screen_displaysSaveButton() {
        setupScreen(
            SpiceLevelUiState(
                isLoading = false,
                isSaving = false,
                spiceLevel = SpiceLevel.MEDIUM,
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Save").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadingState_hidesContent() {
        setupScreen(
            SpiceLevelUiState(
                isLoading = true,
                isSaving = false,
                spiceLevel = SpiceLevel.MEDIUM,
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun spiceLevelOptions_areDisplayed() {
        setupScreen(
            SpiceLevelUiState(
                isLoading = false,
                isSaving = false,
                spiceLevel = SpiceLevel.MEDIUM,
                saveSuccess = false,
                errorMessage = null
            )
        )

        SpiceLevel.entries.forEach { level ->
            composeTestRule.onNodeWithText(level.displayName).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun saveButton_callsViewModelSave() {
        val mockViewModel = setupScreen(
            SpiceLevelUiState(
                isLoading = false,
                isSaving = false,
                spiceLevel = SpiceLevel.MEDIUM,
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Save").performScrollTo().performClick()

        verify { mockViewModel.save() }
    }
}
