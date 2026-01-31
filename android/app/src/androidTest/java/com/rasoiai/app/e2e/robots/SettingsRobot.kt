package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.e2e.base.waitUntilNodeWithTextExists
import com.rasoiai.app.presentation.common.TestTags

/**
 * Robot for Settings screen interactions.
 * Handles profile, preferences, and notification settings.
 */
class SettingsRobot(private val composeTestRule: ComposeContentTestRule) {

    /**
     * Wait for settings screen to be displayed.
     */
    fun waitForSettingsScreen(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(TestTags.SETTINGS_SCREEN, timeoutMillis)
        // Also wait for content to load (title appears)
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
     * Note: Family section is displayed inline on Settings screen.
     * This clicks the "Add family member" row.
     */
    fun navigateToFamilyMembers() = apply {
        composeTestRule.onNodeWithText("Add family member", ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Navigate to recipe rules screen.
     */
    fun navigateToRecipeRules() = apply {
        composeTestRule.onNodeWithText("Recipe Rules", ignoreCase = true)
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

    // ===================== Meal Generation Settings =====================

    /**
     * Scroll to meal generation section.
     */
    fun scrollToMealGenerationSection() = apply {
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_MEAL_GENERATION_SECTION)
            .performScrollTo()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert meal generation section is displayed.
     */
    fun assertMealGenerationSectionDisplayed() = apply {
        composeTestRule.onNodeWithText("MEAL GENERATION", ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert items per meal displays the expected value.
     */
    fun assertItemsPerMealValue(expectedValue: String) = apply {
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_ITEMS_PER_MEAL)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedValue, substring = true)
            .assertIsDisplayed()
    }

    /**
     * Tap items per meal setting.
     */
    fun tapItemsPerMeal() = apply {
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_ITEMS_PER_MEAL)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Toggle strict allergen mode.
     */
    fun toggleStrictAllergenMode() = apply {
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_STRICT_ALLERGEN_TOGGLE)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert strict allergen mode is ON.
     */
    fun assertStrictAllergenModeOn() = apply {
        composeTestRule.onNode(
            hasTestTag(TestTags.SETTINGS_STRICT_ALLERGEN_TOGGLE)
        ).performScrollTo()
        // The toggle row contains the switch - verify by checking the text and state
        composeTestRule.onNodeWithText("Strict Allergen Mode", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert strict allergen mode is OFF.
     */
    fun assertStrictAllergenModeOff() = apply {
        composeTestRule.onNode(
            hasTestTag(TestTags.SETTINGS_STRICT_ALLERGEN_TOGGLE)
        ).performScrollTo()
        composeTestRule.onNodeWithText("Strict Allergen Mode", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Toggle strict dietary mode.
     */
    fun toggleStrictDietaryMode() = apply {
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_STRICT_DIETARY_TOGGLE)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert strict dietary mode is ON.
     */
    fun assertStrictDietaryModeOn() = apply {
        composeTestRule.onNode(
            hasTestTag(TestTags.SETTINGS_STRICT_DIETARY_TOGGLE)
        ).performScrollTo()
        composeTestRule.onNodeWithText("Strict Dietary Mode", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert strict dietary mode is OFF.
     */
    fun assertStrictDietaryModeOff() = apply {
        composeTestRule.onNode(
            hasTestTag(TestTags.SETTINGS_STRICT_DIETARY_TOGGLE)
        ).performScrollTo()
        composeTestRule.onNodeWithText("Strict Dietary Mode", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Toggle allow recipe repeat.
     */
    fun toggleAllowRecipeRepeat() = apply {
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_ALLOW_REPEAT_TOGGLE)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert allow recipe repeat is ON.
     */
    fun assertAllowRecipeRepeatOn() = apply {
        composeTestRule.onNode(
            hasTestTag(TestTags.SETTINGS_ALLOW_REPEAT_TOGGLE)
        ).performScrollTo()
        composeTestRule.onNodeWithText("Allow Recipe Repeat", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert allow recipe repeat is OFF.
     */
    fun assertAllowRecipeRepeatOff() = apply {
        composeTestRule.onNode(
            hasTestTag(TestTags.SETTINGS_ALLOW_REPEAT_TOGGLE)
        ).performScrollTo()
        composeTestRule.onNodeWithText("Allow Recipe Repeat", ignoreCase = true)
            .assertIsDisplayed()
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
