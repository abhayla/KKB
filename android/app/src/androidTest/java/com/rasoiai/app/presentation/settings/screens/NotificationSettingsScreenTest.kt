package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screen_displaysTitle() {
        composeTestRule.setContent {
            RasoiAITheme {
                NotificationSettingsTestContent(
                    uiState = NotificationSettingsUiState(
                        isLoading = false,
                        notificationsEnabled = true,
                        mealReminders = true,
                        shoppingReminders = true,
                        cookingReminders = true,
                        festivalSuggestions = true,
                        achievementNotifications = true
                    )
                )
            }
        }
        // "Notifications" substring matches TopAppBar title, toggle rows, and label — take the first
        composeTestRule.onAllNodesWithText("Notifications", substring = true, ignoreCase = false)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_hidesContent() {
        composeTestRule.setContent {
            RasoiAITheme {
                NotificationSettingsTestContent(
                    uiState = NotificationSettingsUiState(
                        isLoading = true,
                        notificationsEnabled = false,
                        mealReminders = false,
                        shoppingReminders = false,
                        cookingReminders = false,
                        festivalSuggestions = false,
                        achievementNotifications = false
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Meal", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun masterNotificationToggle_isVisible() {
        composeTestRule.setContent {
            RasoiAITheme {
                NotificationSettingsTestContent(
                    uiState = NotificationSettingsUiState(
                        isLoading = false,
                        notificationsEnabled = true,
                        mealReminders = true,
                        shoppingReminders = true,
                        cookingReminders = true,
                        festivalSuggestions = true,
                        achievementNotifications = true
                    )
                )
            }
        }
        // Master toggle row title is "All Notifications" — assert on unique exact text
        composeTestRule.onNodeWithText("All Notifications", substring = false, ignoreCase = false)
            .assertIsDisplayed()
    }

    @Test
    fun individualToggleRows_areVisible() {
        composeTestRule.setContent {
            RasoiAITheme {
                NotificationSettingsTestContent(
                    uiState = NotificationSettingsUiState(
                        isLoading = false,
                        notificationsEnabled = true,
                        mealReminders = true,
                        shoppingReminders = true,
                        cookingReminders = true,
                        festivalSuggestions = true,
                        achievementNotifications = true
                    )
                )
            }
        }
        // Use exact row titles to avoid multi-node ambiguity from subtitle text
        composeTestRule.onNodeWithText("Meal Plan Reminders", substring = false, ignoreCase = false)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Shopping List Reminders", substring = false, ignoreCase = false)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Cooking Time Reminders", substring = false, ignoreCase = false)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Festival Food Suggestions", substring = false, ignoreCase = false)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Achievements", substring = false, ignoreCase = false)
            .assertIsDisplayed()
    }

    @Test
    fun togglingMasterSwitch_callsCallback() {
        var toggleValue: Boolean? = null
        composeTestRule.setContent {
            RasoiAITheme {
                NotificationSettingsTestContent(
                    uiState = NotificationSettingsUiState(
                        isLoading = false,
                        notificationsEnabled = false,
                        mealReminders = false,
                        shoppingReminders = false,
                        cookingReminders = false,
                        festivalSuggestions = false,
                        achievementNotifications = false
                    ),
                    onToggleMasterNotifications = { toggleValue = it }
                )
            }
        }
        // Master toggle row title is "All Notifications" (not "Enable Notifications")
        composeTestRule.onNodeWithText("All Notifications", substring = false, ignoreCase = false)
            .performClick()
        assert(toggleValue != null) { "onToggleMasterNotifications callback was not triggered" }
    }

    @Test
    fun allToggles_shownWhenNotificationsEnabled() {
        composeTestRule.setContent {
            RasoiAITheme {
                NotificationSettingsTestContent(
                    uiState = NotificationSettingsUiState(
                        isLoading = false,
                        notificationsEnabled = true,
                        mealReminders = true,
                        shoppingReminders = false,
                        cookingReminders = true,
                        festivalSuggestions = false,
                        achievementNotifications = true
                    )
                )
            }
        }
        // Use exact row titles to avoid multi-node ambiguity from subtitle text
        composeTestRule.onNodeWithText("Meal Plan Reminders", substring = false, ignoreCase = false)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Shopping List Reminders", substring = false, ignoreCase = false)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Cooking Time Reminders", substring = false, ignoreCase = false)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Festival Food Suggestions", substring = false, ignoreCase = false)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Achievements", substring = false, ignoreCase = false)
            .assertIsDisplayed()
    }
}

@androidx.compose.runtime.Composable
private fun NotificationSettingsTestContent(
    uiState: NotificationSettingsUiState,
    onNavigateBack: () -> Unit = {},
    onToggleMasterNotifications: (Boolean) -> Unit = {},
    onToggleMealReminders: (Boolean) -> Unit = {},
    onToggleShoppingReminders: (Boolean) -> Unit = {},
    onToggleCookingReminders: (Boolean) -> Unit = {},
    onToggleFestivalSuggestions: (Boolean) -> Unit = {},
    onToggleAchievementNotifications: (Boolean) -> Unit = {}
) {
    NotificationSettingsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onToggleMasterNotifications = onToggleMasterNotifications,
        onToggleMealReminders = onToggleMealReminders,
        onToggleShoppingReminders = onToggleShoppingReminders,
        onToggleCookingReminders = onToggleCookingReminders,
        onToggleFestivalSuggestions = onToggleFestivalSuggestions,
        onToggleAchievementNotifications = onToggleAchievementNotifications
    )
}
