package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rasoiai.app.e2e.base.waitUntilNodeWithTextExists

/**
 * Robot for Settings screen interactions.
 * Handles profile, preferences, and notification settings.
 */
class SettingsRobot(private val composeTestRule: ComposeContentTestRule) {

    /**
     * Wait for settings screen to be displayed.
     */
    fun waitForSettingsScreen(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilNodeWithTextExists("Settings", timeoutMillis)
    }

    /**
     * Assert settings screen is displayed.
     */
    fun assertSettingsScreenDisplayed() = apply {
        composeTestRule.onNodeWithText("Settings", ignoreCase = true).assertIsDisplayed()
    }

    // ===================== Profile Section =====================

    /**
     * Assert profile section is displayed.
     */
    fun assertProfileSectionDisplayed() = apply {
        composeTestRule.onNodeWithText("Profile", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert user email is displayed.
     */
    fun assertEmailDisplayed(email: String) = apply {
        composeTestRule.onNodeWithText(email, substring = true).assertIsDisplayed()
    }

    /**
     * Assert user name is displayed.
     */
    fun assertNameDisplayed(name: String) = apply {
        composeTestRule.onNodeWithText(name, substring = true).assertIsDisplayed()
    }

    /**
     * Tap edit profile.
     */
    fun tapEditProfile() = apply {
        composeTestRule.onNodeWithText("Edit Profile", ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Dietary Preferences =====================

    /**
     * Navigate to dietary preferences.
     */
    fun navigateToDietaryPreferences() = apply {
        composeTestRule.onNodeWithText("Dietary Preferences", ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Change primary diet.
     */
    fun changePrimaryDiet(diet: String) = apply {
        composeTestRule.onNodeWithText(diet, ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Save preference changes.
     */
    fun savePreferences() = apply {
        composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert confirmation message is displayed.
     */
    fun assertSaveConfirmation() = apply {
        composeTestRule.onNodeWithText("Saved", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Cuisine Preferences =====================

    /**
     * Navigate to cuisine preferences.
     */
    fun navigateToCuisinePreferences() = apply {
        composeTestRule.onNodeWithText("Cuisine Preferences", ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Cooking Time =====================

    /**
     * Navigate to cooking time settings.
     */
    fun navigateToCookingTime() = apply {
        composeTestRule.onNodeWithText("Cooking Time", ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Family Members =====================

    /**
     * Navigate to family members settings.
     */
    fun navigateToFamilyMembers() = apply {
        composeTestRule.onNodeWithText("Family Members", ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Notifications =====================

    /**
     * Navigate to notification settings.
     */
    fun navigateToNotifications() = apply {
        composeTestRule.onNodeWithText("Notifications", ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Toggle meal reminders.
     */
    fun toggleMealReminders() = apply {
        composeTestRule.onNodeWithText("Meal Reminders", ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert meal reminders is on.
     */
    fun assertMealRemindersOn() = apply {
        // Check toggle state
        composeTestRule.onNodeWithText("Meal Reminders", ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Toggle shopping day reminder.
     */
    fun toggleShoppingReminder() = apply {
        composeTestRule.onNodeWithText("Shopping Reminder", ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Theme =====================

    /**
     * Navigate to theme settings.
     */
    fun navigateToTheme() = apply {
        composeTestRule.onNodeWithText("Theme", ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select light theme.
     */
    fun selectLightTheme() = apply {
        composeTestRule.onNodeWithText("Light", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select dark theme.
     */
    fun selectDarkTheme() = apply {
        composeTestRule.onNodeWithText("Dark", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select system theme.
     */
    fun selectSystemTheme() = apply {
        composeTestRule.onNodeWithText("System", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Account =====================

    /**
     * Tap sign out.
     */
    fun tapSignOut() = apply {
        composeTestRule.onNodeWithText("Sign Out", ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Confirm sign out.
     */
    fun confirmSignOut() = apply {
        composeTestRule.onNodeWithText("Confirm", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Cancel sign out.
     */
    fun cancelSignOut() = apply {
        composeTestRule.onNodeWithText("Cancel", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== About =====================

    /**
     * Navigate to about section.
     */
    fun navigateToAbout() = apply {
        composeTestRule.onNodeWithText("About", ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert app version is displayed.
     */
    fun assertAppVersionDisplayed() = apply {
        composeTestRule.onNodeWithText("Version", substring = true, ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ===================== Navigation =====================

    /**
     * Go back from settings.
     */
    fun goBack() = apply {
        composeTestRule.onNodeWithText("Back", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }
}
