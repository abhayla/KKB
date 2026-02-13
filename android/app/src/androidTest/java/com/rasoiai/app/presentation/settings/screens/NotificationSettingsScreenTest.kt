package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setupScreen(uiState: NotificationSettingsUiState): NotificationSettingsViewModel {
        val mockViewModel = mockk<NotificationSettingsViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        composeTestRule.setContent {
            RasoiAITheme {
                NotificationSettingsScreen(onNavigateBack = {}, viewModel = mockViewModel)
            }
        }
        return mockViewModel
    }

    @Test
    fun screen_displaysTitle() {
        setupScreen(
            NotificationSettingsUiState(
                isLoading = false,
                notificationsEnabled = true,
                mealReminders = true,
                shoppingReminders = true,
                cookingReminders = true,
                festivalSuggestions = true,
                achievementNotifications = true
            )
        )
        composeTestRule.onNodeWithText("Notifications", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_hidesContent() {
        setupScreen(
            NotificationSettingsUiState(
                isLoading = true,
                notificationsEnabled = false,
                mealReminders = false,
                shoppingReminders = false,
                cookingReminders = false,
                festivalSuggestions = false,
                achievementNotifications = false
            )
        )
        composeTestRule.onNodeWithText("Meal", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun masterNotificationToggle_isVisible() {
        setupScreen(
            NotificationSettingsUiState(
                isLoading = false,
                notificationsEnabled = true,
                mealReminders = true,
                shoppingReminders = true,
                cookingReminders = true,
                festivalSuggestions = true,
                achievementNotifications = true
            )
        )
        composeTestRule.onNodeWithText("Notifications", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun individualToggleRows_areVisible() {
        setupScreen(
            NotificationSettingsUiState(
                isLoading = false,
                notificationsEnabled = true,
                mealReminders = true,
                shoppingReminders = true,
                cookingReminders = true,
                festivalSuggestions = true,
                achievementNotifications = true
            )
        )
        composeTestRule.onNodeWithText("Meal", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Shopping", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Cooking", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Festival", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Achievement", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun togglingMasterSwitch_callsViewModel() {
        val mockViewModel = setupScreen(
            NotificationSettingsUiState(
                isLoading = false,
                notificationsEnabled = false,
                mealReminders = false,
                shoppingReminders = false,
                cookingReminders = false,
                festivalSuggestions = false,
                achievementNotifications = false
            )
        )
        // Click on the master notification toggle text/row
        composeTestRule.onNodeWithText("Enable Notifications", substring = true, ignoreCase = true)
            .performClick()
        verify { mockViewModel.toggleMasterNotifications(any()) }
    }

    @Test
    fun allToggles_shownWhenNotificationsEnabled() {
        setupScreen(
            NotificationSettingsUiState(
                isLoading = false,
                notificationsEnabled = true,
                mealReminders = true,
                shoppingReminders = false,
                cookingReminders = true,
                festivalSuggestions = false,
                achievementNotifications = true
            )
        )
        composeTestRule.onNodeWithText("Meal", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Shopping", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Cooking", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Festival", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Achievement", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }
}
