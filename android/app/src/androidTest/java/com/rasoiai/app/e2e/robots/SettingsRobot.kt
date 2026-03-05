package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.rasoiai.app.e2e.base.isNodeWithTextDisplayed
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.e2e.base.waitUntilNodeWithTextExists
import com.rasoiai.app.presentation.common.TestTags

/**
 * Robot for Settings screen interactions.
 * Handles profile, preferences, and notification settings.
 *
 * Note: Settings screen uses LazyColumn, so performScrollTo() does NOT work.
 * All scroll operations use swipe-based scrolling via scrollToText/scrollToTag helpers.
 */
class SettingsRobot(private val composeTestRule: ComposeContentTestRule) {

    companion object {
        private const val MAX_SCROLL_ATTEMPTS = 8
    }

    /**
     * Scroll the Settings LazyColumn until a node with the given text is visible ON SCREEN.
     * LazyColumn does not support performScrollTo(), so we swipe up repeatedly.
     *
     * IMPORTANT: Uses assertIsDisplayed() instead of just fetchSemanticsNodes().
     * LazyColumn pre-composes items beyond the viewport, so semantics nodes exist
     * even for off-screen items. assertIsDisplayed() checks actual visibility.
     */
    private fun scrollToText(text: String, substring: Boolean = false, ignoreCase: Boolean = true) {
        // First try: use performScrollToNode (reliable for LazyColumn)
        try {
            composeTestRule.onNodeWithTag(TestTags.SETTINGS_LAZY_COLUMN)
                .performScrollToNode(hasText(text, substring = substring, ignoreCase = ignoreCase))
            return
        } catch (_: Exception) {
            // Fallback to swipe-based scrolling
        }
        repeat(MAX_SCROLL_ATTEMPTS) {
            try {
                composeTestRule.onNodeWithText(text, substring = substring, ignoreCase = ignoreCase)
                    .assertIsDisplayed()
                return
            } catch (_: AssertionError) {
                // Node not visible yet — scroll down
            }
            composeTestRule.onNodeWithTag(TestTags.SETTINGS_SCREEN)
                .performTouchInput { swipeUp() }
            composeTestRule.waitForIdle()
            Thread.sleep(300)
        }
    }

    /**
     * Scroll the Settings LazyColumn until a node with the given test tag is visible.
     */
    private fun scrollToTag(tag: String) {
        // First try: use performScrollToNode (reliable for LazyColumn)
        try {
            composeTestRule.onNodeWithTag(TestTags.SETTINGS_LAZY_COLUMN)
                .performScrollToNode(hasTestTag(tag))
            return
        } catch (_: Exception) {
            // Fallback to swipe-based scrolling
        }
        repeat(MAX_SCROLL_ATTEMPTS) {
            try {
                composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
                return
            } catch (_: AssertionError) {
                // Node not rendered yet, scroll down
            }
            composeTestRule.onNodeWithTag(TestTags.SETTINGS_SCREEN)
                .performTouchInput { swipeUp() }
            composeTestRule.waitForIdle()
            Thread.sleep(300)
        }
    }

    /**
     * Wait for settings screen to be displayed, including LazyColumn content.
     * The LazyColumn is hidden behind an `if (uiState.isLoading)` gate,
     * so we must poll for a content element that only exists after loading.
     */
    fun waitForSettingsScreen(timeoutMillis: Long = 10000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(TestTags.SETTINGS_SCREEN, timeoutMillis)
        // Wait for title (TopAppBar)
        composeTestRule.waitUntilNodeWithTextExists("Settings", timeoutMillis)
        // Wait for LazyColumn content to actually load (behind isLoading gate)
        waitForSettingsContentLoaded(timeoutMillis)
    }

    /**
     * Poll for LazyColumn content that only appears after the ViewModel
     * finishes loading (isLoading = false). Looks for "Profile" or "Sign Out"
     * text which always exist inside the LazyColumn.
     */
    private fun waitForSettingsContentLoaded(timeoutMillis: Long = 10000) {
        val startTime = System.currentTimeMillis()
        while ((System.currentTimeMillis() - startTime) < timeoutMillis) {
            if (composeTestRule.isNodeWithTextDisplayed("Profile", substring = true, ignoreCase = true)) return
            if (composeTestRule.isNodeWithTextDisplayed("Sign Out", substring = true, ignoreCase = true)) return
            Thread.sleep(200)
        }
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
        scrollToText("Edit Profile")
        composeTestRule.onNodeWithText("Edit Profile", ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Dietary Preferences =====================

    /**
     * Navigate to dietary preferences.
     */
    fun navigateToDietaryPreferences() = apply {
        scrollToText("Dietary Preferences")
        composeTestRule.onNodeWithText("Dietary Preferences", ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Change primary diet.
     */
    fun changePrimaryDiet(diet: String) = apply {
        scrollToText(diet)
        composeTestRule.onNodeWithText(diet, ignoreCase = true)
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
        scrollToText("Cuisine Preferences")
        composeTestRule.onNodeWithText("Cuisine Preferences", ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Cooking Time =====================

    /**
     * Navigate to cooking time settings.
     */
    fun navigateToCookingTime() = apply {
        scrollToText("Cooking Time")
        composeTestRule.onNodeWithText("Cooking Time", ignoreCase = true)
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
        scrollToText("Add family member")
        composeTestRule.onNodeWithText("Add family member", ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert family member name is displayed in settings.
     */
    fun assertFamilyMemberDisplayed(memberName: String) = apply {
        scrollToText(memberName, substring = true)
        composeTestRule.onNodeWithText(memberName, substring = true)
            .assertIsDisplayed()
    }

    /**
     * Assert household size value is displayed.
     */
    fun assertHouseholdSizeDisplayed(size: Int) = apply {
        composeTestRule.onNodeWithText("$size", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Navigate to recipe rules screen.
     */
    fun navigateToRecipeRules() = apply {
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TestTags.SETTINGS_RECIPE_RULES))
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_RECIPE_RULES)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Notifications =====================

    /**
     * Navigate to notification settings.
     */
    fun navigateToNotifications() = apply {
        scrollToText("Notifications")
        composeTestRule.onNodeWithText("Notifications", ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Toggle meal reminders.
     */
    fun toggleMealReminders() = apply {
        scrollToText("Meal Reminders")
        composeTestRule.onNodeWithText("Meal Reminders", ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert meal reminders is on.
     */
    fun assertMealRemindersOn() = apply {
        scrollToText("Meal Reminders")
        composeTestRule.onNodeWithText("Meal Reminders", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Toggle shopping day reminder.
     */
    fun toggleShoppingReminder() = apply {
        scrollToText("Shopping Reminder")
        composeTestRule.onNodeWithText("Shopping Reminder", ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Theme =====================

    /**
     * Navigate to theme settings.
     */
    fun navigateToTheme() = apply {
        scrollToText("Theme")
        composeTestRule.onNodeWithText("Theme", ignoreCase = true)
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
        scrollToTag(TestTags.SETTINGS_MEAL_GENERATION_SECTION)
        composeTestRule.waitForIdle()
    }

    /**
     * Assert meal generation section is displayed.
     */
    fun assertMealGenerationSectionDisplayed() = apply {
        scrollToText("MEAL GENERATION")
        composeTestRule.onNodeWithText("MEAL GENERATION", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert items per meal displays the expected value.
     */
    fun assertItemsPerMealValue(expectedValue: String) = apply {
        scrollToTag(TestTags.SETTINGS_ITEMS_PER_MEAL)
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_ITEMS_PER_MEAL)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedValue, substring = true)
            .assertIsDisplayed()
    }

    /**
     * Tap items per meal setting.
     */
    fun tapItemsPerMeal() = apply {
        scrollToTag(TestTags.SETTINGS_ITEMS_PER_MEAL)
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_ITEMS_PER_MEAL)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Toggle strict allergen mode.
     */
    fun toggleStrictAllergenMode() = apply {
        scrollToTag(TestTags.SETTINGS_STRICT_ALLERGEN_TOGGLE)
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_STRICT_ALLERGEN_TOGGLE)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert strict allergen mode is ON.
     */
    fun assertStrictAllergenModeOn() = apply {
        scrollToTag(TestTags.SETTINGS_STRICT_ALLERGEN_TOGGLE)
        composeTestRule.onNodeWithText("Strict Allergen Mode", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert strict allergen mode is OFF.
     */
    fun assertStrictAllergenModeOff() = apply {
        scrollToTag(TestTags.SETTINGS_STRICT_ALLERGEN_TOGGLE)
        composeTestRule.onNodeWithText("Strict Allergen Mode", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Toggle strict dietary mode.
     */
    fun toggleStrictDietaryMode() = apply {
        scrollToTag(TestTags.SETTINGS_STRICT_DIETARY_TOGGLE)
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_STRICT_DIETARY_TOGGLE)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert strict dietary mode is ON.
     */
    fun assertStrictDietaryModeOn() = apply {
        scrollToTag(TestTags.SETTINGS_STRICT_DIETARY_TOGGLE)
        composeTestRule.onNodeWithText("Strict Dietary Mode", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert strict dietary mode is OFF.
     */
    fun assertStrictDietaryModeOff() = apply {
        scrollToTag(TestTags.SETTINGS_STRICT_DIETARY_TOGGLE)
        composeTestRule.onNodeWithText("Strict Dietary Mode", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Toggle allow recipe repeat.
     */
    fun toggleAllowRecipeRepeat() = apply {
        scrollToTag(TestTags.SETTINGS_ALLOW_REPEAT_TOGGLE)
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_ALLOW_REPEAT_TOGGLE)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert allow recipe repeat is ON.
     */
    fun assertAllowRecipeRepeatOn() = apply {
        scrollToTag(TestTags.SETTINGS_ALLOW_REPEAT_TOGGLE)
        composeTestRule.onNodeWithText("Allow Recipe Repeat", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert allow recipe repeat is OFF.
     */
    fun assertAllowRecipeRepeatOff() = apply {
        scrollToTag(TestTags.SETTINGS_ALLOW_REPEAT_TOGGLE)
        composeTestRule.onNodeWithText("Allow Recipe Repeat", ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Account =====================

    /**
     * Tap sign out.
     */
    fun tapSignOut() = apply {
        scrollToText("Sign Out")
        composeTestRule.onNodeWithText("Sign Out", ignoreCase = true)
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
        scrollToText("About")
        composeTestRule.onNodeWithText("About", ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert app version is displayed.
     */
    fun assertAppVersionDisplayed() = apply {
        scrollToText("Version", substring = true)
        composeTestRule.onNodeWithText("Version", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Meal Preferences Navigation =====================

    /**
     * Navigate to disliked ingredients.
     */
    fun navigateToDislikedIngredients() = apply {
        scrollToText("Disliked Ingredients")
        composeTestRule.onNodeWithText("Disliked Ingredients", ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Navigate to spice level.
     */
    fun navigateToSpiceLevel() = apply {
        scrollToText("Spice Level")
        composeTestRule.onNodeWithText("Spice Level", ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Navigate to units & measurements.
     */
    fun navigateToUnitsAndMeasurements() = apply {
        scrollToText("Units & Measurements")
        composeTestRule.onNodeWithText("Units & Measurements", ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Section Assertions =====================

    /**
     * Assert meal preferences section is displayed.
     */
    fun assertMealPreferencesSectionDisplayed() = apply {
        scrollToText("MEAL PREFERENCES")
        composeTestRule.onNodeWithText("MEAL PREFERENCES", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert app settings section is displayed.
     */
    fun assertAppSettingsSectionDisplayed() = apply {
        scrollToText("APP SETTINGS")
        composeTestRule.onNodeWithText("APP SETTINGS", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert social section is displayed.
     */
    fun assertSocialSectionDisplayed() = apply {
        scrollToText("SOCIAL")
        composeTestRule.onNodeWithText("SOCIAL", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert support section is displayed.
     */
    fun assertSupportSectionDisplayed() = apply {
        scrollToText("SUPPORT")
        composeTestRule.onNodeWithText("SUPPORT", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert family section is displayed.
     */
    fun assertFamilySectionDisplayed() = apply {
        scrollToText("FAMILY")
        composeTestRule.onNodeWithText("FAMILY", ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Dark Mode Dialog =====================

    /**
     * Select dark mode option: System.
     */
    fun selectDarkModeSystem() = apply {
        composeTestRule.onNodeWithText("System", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select dark mode option: Light.
     */
    fun selectDarkModeLight() = apply {
        composeTestRule.onNodeWithText("Light", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select dark mode option: Dark.
     */
    fun selectDarkModeDark() = apply {
        composeTestRule.onNodeWithText("Dark", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Dismiss dark mode dialog.
     */
    fun dismissDarkModeDialog() = apply {
        composeTestRule.onNodeWithText("Cancel", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Items Per Meal Dialog =====================

    /**
     * Tap items per meal setting.
     */
    fun tapItemsPerMealSetting() = apply {
        scrollToTag(TestTags.SETTINGS_ITEMS_PER_MEAL)
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_ITEMS_PER_MEAL)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select items per meal count.
     */
    fun selectItemsPerMealCount(count: Int) = apply {
        composeTestRule.onNodeWithText("$count item", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Dismiss items per meal dialog.
     */
    fun dismissItemsPerMealDialog() = apply {
        composeTestRule.onNodeWithText("Cancel", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Setting Row Assertions =====================

    /**
     * Assert a specific setting item is displayed.
     */
    fun assertSettingItemDisplayed(settingName: String) = apply {
        scrollToText(settingName)
        composeTestRule.onNodeWithText(settingName, ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Tap a specific setting item.
     */
    fun tapSettingItem(settingName: String) = apply {
        scrollToText(settingName)
        composeTestRule.onNodeWithText(settingName, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert "Coming soon!" snackbar appears after tapping a setting.
     */
    fun assertComingSoonSnackbar() = apply {
        composeTestRule.onNodeWithText("Coming soon!", ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Sign Out Dialog =====================

    /**
     * Assert sign out confirmation dialog is displayed.
     */
    fun assertSignOutDialogDisplayed() = apply {
        composeTestRule.onNodeWithText("Are you sure you want to sign out", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Navigation =====================

    /**
     * Go back from settings via back button.
     */
    fun goBack() = apply {
        composeTestRule.onNodeWithText("Back", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }
}
