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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnitsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screen_displaysTitle() {
        composeTestRule.setContent {
            RasoiAITheme {
                UnitsTestContent(
                    uiState = UnitsUiState(
                        isLoading = false,
                        isSaving = false,
                        volumeUnit = VolumeUnit.METRIC,
                        weightUnit = WeightUnit.METRIC,
                        smallMeasurementUnit = SmallMeasurementUnit.METRIC
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Units", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun screen_displaysSaveButton() {
        composeTestRule.setContent {
            RasoiAITheme {
                UnitsTestContent(
                    uiState = UnitsUiState(
                        isLoading = false,
                        isSaving = false,
                        volumeUnit = VolumeUnit.METRIC,
                        weightUnit = WeightUnit.METRIC,
                        smallMeasurementUnit = SmallMeasurementUnit.METRIC
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Save", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_hidesContent() {
        composeTestRule.setContent {
            RasoiAITheme {
                UnitsTestContent(
                    uiState = UnitsUiState(
                        isLoading = true,
                        isSaving = false,
                        volumeUnit = VolumeUnit.METRIC,
                        weightUnit = WeightUnit.METRIC,
                        smallMeasurementUnit = SmallMeasurementUnit.METRIC
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Volume", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun volumeUnitSection_isVisible() {
        composeTestRule.setContent {
            RasoiAITheme {
                UnitsTestContent(
                    uiState = UnitsUiState(
                        isLoading = false,
                        isSaving = false,
                        volumeUnit = VolumeUnit.METRIC,
                        weightUnit = WeightUnit.METRIC,
                        smallMeasurementUnit = SmallMeasurementUnit.METRIC
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Volume", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun weightUnitSection_isVisible() {
        composeTestRule.setContent {
            RasoiAITheme {
                UnitsTestContent(
                    uiState = UnitsUiState(
                        isLoading = false,
                        isSaving = false,
                        volumeUnit = VolumeUnit.METRIC,
                        weightUnit = WeightUnit.METRIC,
                        smallMeasurementUnit = SmallMeasurementUnit.METRIC
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Weight", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun saveButtonClick_callsOnSave() {
        var saveCalled = false
        composeTestRule.setContent {
            RasoiAITheme {
                UnitsTestContent(
                    uiState = UnitsUiState(
                        isLoading = false,
                        isSaving = false,
                        volumeUnit = VolumeUnit.METRIC,
                        weightUnit = WeightUnit.METRIC,
                        smallMeasurementUnit = SmallMeasurementUnit.METRIC
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
private fun UnitsTestContent(
    uiState: UnitsUiState,
    onNavigateBack: () -> Unit = {},
    onUpdateVolumeUnit: (VolumeUnit) -> Unit = {},
    onUpdateWeightUnit: (WeightUnit) -> Unit = {},
    onUpdateSmallMeasurementUnit: (SmallMeasurementUnit) -> Unit = {},
    onSave: () -> Unit = {}
) {
    UnitsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onUpdateVolumeUnit = onUpdateVolumeUnit,
        onUpdateWeightUnit = onUpdateWeightUnit,
        onUpdateSmallMeasurementUnit = onUpdateSmallMeasurementUnit,
        onSave = onSave
    )
}
