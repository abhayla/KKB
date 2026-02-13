package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.DayOfWeek
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CookingTimeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setupScreen(uiState: CookingTimeUiState): CookingTimeViewModel {
        val mockViewModel = mockk<CookingTimeViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        composeTestRule.setContent {
            RasoiAITheme {
                CookingTimeScreen(onNavigateBack = {}, viewModel = mockViewModel)
            }
        }
        return mockViewModel
    }

    @Test
    fun screen_displaysTitle() {
        setupScreen(
            CookingTimeUiState(
                isLoading = false,
                isSaving = false,
                weekdayCookingTime = 30,
                weekendCookingTime = 60,
                busyDays = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Cooking Time").assertIsDisplayed()
    }

    @Test
    fun screen_displaysWeekdayAndWeekendSections() {
        setupScreen(
            CookingTimeUiState(
                isLoading = false,
                isSaving = false,
                weekdayCookingTime = 30,
                weekendCookingTime = 60,
                busyDays = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Weekday", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Weekend", substring = true).assertIsDisplayed()
    }

    @Test
    fun screen_displaysSaveButton() {
        setupScreen(
            CookingTimeUiState(
                isLoading = false,
                isSaving = false,
                weekdayCookingTime = 30,
                weekendCookingTime = 60,
                busyDays = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Save").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadingState_hidesContent() {
        setupScreen(
            CookingTimeUiState(
                isLoading = true,
                isSaving = false,
                weekdayCookingTime = 30,
                weekendCookingTime = 60,
                busyDays = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun busyDaysSection_isVisible() {
        setupScreen(
            CookingTimeUiState(
                isLoading = false,
                isSaving = false,
                weekdayCookingTime = 30,
                weekendCookingTime = 60,
                busyDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Busy Days", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saveButton_callsViewModelSave() {
        val mockViewModel = setupScreen(
            CookingTimeUiState(
                isLoading = false,
                isSaving = false,
                weekdayCookingTime = 30,
                weekendCookingTime = 60,
                busyDays = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Save").performScrollTo().performClick()

        verify { mockViewModel.save() }
    }
}
