package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.DayOfWeek
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CookingTimeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screen_displaysTitle() {
        composeTestRule.setContent {
            RasoiAITheme {
                CookingTimeTestContent(
                    uiState = CookingTimeUiState(
                        isLoading = false,
                        isSaving = false,
                        weekdayCookingTime = 30,
                        weekendCookingTime = 60,
                        busyDays = emptySet(),
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Cooking Time").assertIsDisplayed()
    }

    @Test
    fun screen_displaysWeekdayAndWeekendSections() {
        composeTestRule.setContent {
            RasoiAITheme {
                CookingTimeTestContent(
                    uiState = CookingTimeUiState(
                        isLoading = false,
                        isSaving = false,
                        weekdayCookingTime = 30,
                        weekendCookingTime = 60,
                        busyDays = emptySet(),
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Weekday", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Weekend", substring = true).assertIsDisplayed()
    }

    @Test
    fun screen_displaysSaveButton() {
        composeTestRule.setContent {
            RasoiAITheme {
                CookingTimeTestContent(
                    uiState = CookingTimeUiState(
                        isLoading = false,
                        isSaving = false,
                        weekdayCookingTime = 30,
                        weekendCookingTime = 60,
                        busyDays = emptySet(),
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
                CookingTimeTestContent(
                    uiState = CookingTimeUiState(
                        isLoading = true,
                        isSaving = false,
                        weekdayCookingTime = 30,
                        weekendCookingTime = 60,
                        busyDays = emptySet(),
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun busyDaysSection_isVisible() {
        composeTestRule.setContent {
            RasoiAITheme {
                CookingTimeTestContent(
                    uiState = CookingTimeUiState(
                        isLoading = false,
                        isSaving = false,
                        weekdayCookingTime = 30,
                        weekendCookingTime = 60,
                        busyDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                        saveSuccess = false,
                        errorMessage = null
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Busy Days", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saveButton_callsOnSave() {
        var saveCalled = false
        composeTestRule.setContent {
            RasoiAITheme {
                CookingTimeTestContent(
                    uiState = CookingTimeUiState(
                        isLoading = false,
                        isSaving = false,
                        weekdayCookingTime = 30,
                        weekendCookingTime = 60,
                        busyDays = emptySet(),
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
private fun CookingTimeTestContent(
    uiState: CookingTimeUiState,
    onNavigateBack: () -> Unit = {},
    onUpdateWeekdayCookingTime: (Int) -> Unit = {},
    onUpdateWeekendCookingTime: (Int) -> Unit = {},
    onToggleBusyDay: (DayOfWeek) -> Unit = {},
    onSave: () -> Unit = {}
) {
    CookingTimeScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onUpdateWeekdayCookingTime = onUpdateWeekdayCookingTime,
        onUpdateWeekendCookingTime = onUpdateWeekendCookingTime,
        onToggleBusyDay = onToggleBusyDay,
        onSave = onSave
    )
}
