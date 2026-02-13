package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.SmallMeasurementUnit
import com.rasoiai.domain.model.VolumeUnit
import com.rasoiai.domain.model.WeightUnit
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnitsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setupScreen(uiState: UnitsUiState): UnitsViewModel {
        val mockViewModel = mockk<UnitsViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        composeTestRule.setContent {
            RasoiAITheme {
                UnitsScreen(onNavigateBack = {}, viewModel = mockViewModel)
            }
        }
        return mockViewModel
    }

    @Test
    fun screen_displaysTitle() {
        setupScreen(
            UnitsUiState(
                isLoading = false,
                isSaving = false,
                volumeUnit = VolumeUnit.METRIC,
                weightUnit = WeightUnit.METRIC,
                smallMeasurementUnit = SmallMeasurementUnit.METRIC
            )
        )
        composeTestRule.onNodeWithText("Units", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun screen_displaysSaveButton() {
        setupScreen(
            UnitsUiState(
                isLoading = false,
                isSaving = false,
                volumeUnit = VolumeUnit.METRIC,
                weightUnit = WeightUnit.METRIC,
                smallMeasurementUnit = SmallMeasurementUnit.METRIC
            )
        )
        composeTestRule.onNodeWithText("Save", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_hidesContent() {
        setupScreen(
            UnitsUiState(
                isLoading = true,
                isSaving = false,
                volumeUnit = VolumeUnit.METRIC,
                weightUnit = WeightUnit.METRIC,
                smallMeasurementUnit = SmallMeasurementUnit.METRIC
            )
        )
        composeTestRule.onNodeWithText("Volume", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun volumeUnitSection_isVisible() {
        setupScreen(
            UnitsUiState(
                isLoading = false,
                isSaving = false,
                volumeUnit = VolumeUnit.METRIC,
                weightUnit = WeightUnit.METRIC,
                smallMeasurementUnit = SmallMeasurementUnit.METRIC
            )
        )
        composeTestRule.onNodeWithText("Volume", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun weightUnitSection_isVisible() {
        setupScreen(
            UnitsUiState(
                isLoading = false,
                isSaving = false,
                volumeUnit = VolumeUnit.METRIC,
                weightUnit = WeightUnit.METRIC,
                smallMeasurementUnit = SmallMeasurementUnit.METRIC
            )
        )
        composeTestRule.onNodeWithText("Weight", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun saveButtonClick_callsViewModelSave() {
        val mockViewModel = setupScreen(
            UnitsUiState(
                isLoading = false,
                isSaving = false,
                volumeUnit = VolumeUnit.METRIC,
                weightUnit = WeightUnit.METRIC,
                smallMeasurementUnit = SmallMeasurementUnit.METRIC
            )
        )
        composeTestRule.onNodeWithText("Save", substring = true, ignoreCase = true)
            .performClick()
        verify { mockViewModel.save() }
    }
}
