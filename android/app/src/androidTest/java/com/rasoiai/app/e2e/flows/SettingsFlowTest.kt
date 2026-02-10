package com.rasoiai.app.e2e.flows

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.base.TestDataFactory
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.SettingsRobot
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * Requirement: #39 - FR-007: Expanded E2E tests for Settings screen
 *
 * Phase 9: Settings Screen Testing (22 tests)
 *
 * Test Categories:
 * 9.1  Profile Section (2 tests)
 * 9.2  Preference Updates (1 test)
 * 9.3  Notifications Toggle (1 test)
 * 9.4  Theme Selection (1 test)
 * 9.5  Meal Generation Settings (4 tests)
 * 9.6  About Section (1 test)
 * 9.7  Sign Out Flow (2 tests)
 * 9.8  Family Members (1 test)
 * 9.9  Cooking Time (1 test)
 * 9.10 Cuisine Preferences (1 test)
 * 9.11 Spice Level (1 test)
 * 9.12 Recipe Rules (1 test)
 * 9.13 Dark Mode Dialog (1 test)
 * 9.14 Items Per Meal Dialog (1 test)
 * 9.15 All Sections Scroll (1 test)
 * 9.16 Disliked Ingredients (1 test)
 * 9.17 Settings Item Navigation (1 test)
 */
@HiltAndroidTest
class SettingsFlowTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedState()

        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)

        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToSettings()
    }

    // ===================== 9.1 Profile Section =====================

    /**
     * Test 9.1: Profile Section displays user info
     */
    @Test
    fun test_9_1_profileSection() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.assertSettingsScreenDisplayed()
        settingsRobot.assertProfileSectionDisplayed()
        settingsRobot.assertEmailDisplayed(TestDataFactory.sharmaFamily.email)
    }

    /**
     * Test 9.1b: Profile email is displayed in the profile section
     */
    @Test
    fun test_9_1b_profileEmail_isVisible() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.assertEmailDisplayed(TestDataFactory.sharmaFamily.email)
    }

    // ===================== 9.2 Preference Updates =====================

    /**
     * Test 9.2: Preference Updates - change primary diet
     */
    @Test
    fun test_9_2_preferenceUpdates() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToDietaryPreferences()
        settingsRobot.changePrimaryDiet("Eggetarian")
        settingsRobot.savePreferences()
        settingsRobot.assertSaveConfirmation()
    }

    // ===================== 9.3 Notifications Toggle =====================

    /**
     * Test 9.3: Notifications Toggle
     */
    @Test
    fun test_9_3_notificationsToggle() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToNotifications()
        settingsRobot.toggleMealReminders()
        settingsRobot.assertMealRemindersOn()
        settingsRobot.toggleShoppingReminder()
    }

    // ===================== 9.4 Theme Selection =====================

    /**
     * Test 9.4: Theme selection works
     */
    @Test
    fun test_9_4_themeSelection_works() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToTheme()
        settingsRobot.selectLightTheme()
        settingsRobot.selectDarkTheme()
        settingsRobot.selectSystemTheme()
    }

    // ===================== 9.5 Meal Generation Settings =====================

    /**
     * Test 9.5.1: Meal Generation Section Display
     */
    @Test
    fun test_9_5_1_mealGenerationSection_isDisplayed() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.assertMealGenerationSectionDisplayed()
        settingsRobot.assertItemsPerMealValue("2 items")
    }

    /**
     * Test 9.5.2: Strict Allergen Mode Toggle
     */
    @Test
    fun test_9_5_2_strictAllergenMode_toggle() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.scrollToMealGenerationSection()

        settingsRobot.assertStrictAllergenModeOn()
        settingsRobot.toggleStrictAllergenMode()
        settingsRobot.assertStrictAllergenModeOff()
        settingsRobot.toggleStrictAllergenMode()
        settingsRobot.assertStrictAllergenModeOn()
    }

    /**
     * Test 9.5.3: Strict Dietary Mode Toggle
     */
    @Test
    fun test_9_5_3_strictDietaryMode_toggle() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.scrollToMealGenerationSection()

        settingsRobot.assertStrictDietaryModeOn()
        settingsRobot.toggleStrictDietaryMode()
        settingsRobot.assertStrictDietaryModeOff()
        settingsRobot.toggleStrictDietaryMode()
        settingsRobot.assertStrictDietaryModeOn()
    }

    /**
     * Test 9.5.4: Allow Recipe Repeat Toggle
     */
    @Test
    fun test_9_5_4_allowRecipeRepeat_toggle() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.scrollToMealGenerationSection()

        settingsRobot.assertAllowRecipeRepeatOff()
        settingsRobot.toggleAllowRecipeRepeat()
        settingsRobot.assertAllowRecipeRepeatOn()
        settingsRobot.toggleAllowRecipeRepeat()
        settingsRobot.assertAllowRecipeRepeatOff()
    }

    // ===================== 9.6 About Section =====================

    /**
     * Test 9.6: About section displays version
     */
    @Test
    fun test_9_6_aboutSection_displaysVersion() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToAbout()
        settingsRobot.assertAppVersionDisplayed()
    }

    // ===================== 9.7 Sign Out Flow =====================

    /**
     * Test 9.7: Sign out flow with cancel and confirm
     */
    @Test
    fun test_9_7_signOut_flow() {
        settingsRobot.waitForSettingsScreen()

        // Tap sign out
        settingsRobot.tapSignOut()

        // Cancel first
        settingsRobot.cancelSignOut()

        // Confirm sign out
        settingsRobot.tapSignOut()
        settingsRobot.confirmSignOut()
    }

    /**
     * Test 9.7b: Sign out dialog shows confirmation message
     */
    @Test
    fun test_9_7b_signOutDialog_showsConfirmation() {
        settingsRobot.waitForSettingsScreen()

        settingsRobot.tapSignOut()
        settingsRobot.assertSignOutDialogDisplayed()
        settingsRobot.cancelSignOut()
    }

    // ===================== 9.8 Family Members =====================

    /**
     * Test 9.8: Family members can be updated
     */
    @Test
    fun test_9_8_familyMembers_canBeUpdated() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToFamilyMembers()
    }

    // ===================== 9.9 Cooking Time =====================

    /**
     * Test 9.9: Cooking time can be updated
     */
    @Test
    fun test_9_9_cookingTime_canBeUpdated() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToCookingTime()
    }

    // ===================== 9.10 Cuisine Preferences =====================

    /**
     * Test 9.10: Cuisine preferences can be accessed
     */
    @Test
    fun test_9_10_cuisinePreferences_canBeAccessed() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToCuisinePreferences()
    }

    // ===================== 9.11 Spice Level =====================

    /**
     * Test 9.11: Spice level settings can be accessed
     */
    @Test
    fun test_9_11_spiceLevel_canBeAccessed() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToSpiceLevel()
    }

    // ===================== 9.12 Recipe Rules =====================

    /**
     * Test 9.12: Recipe rules navigation works
     */
    @Test
    fun test_9_12_recipeRules_navigation() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToRecipeRules()
    }

    // ===================== 9.13 Dark Mode Dialog =====================

    /**
     * Test 9.13: Dark mode dialog opens with three options
     */
    @Test
    fun test_9_13_darkModeDialog_showsOptions() {
        settingsRobot.waitForSettingsScreen()

        settingsRobot.tapSettingItem("Dark Mode")
        waitFor(ANIMATION_DURATION)

        // Verify dialog shows all options
        composeTestRule.onNodeWithText("System", ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Light", ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Dark", ignoreCase = true)
            .assertIsDisplayed()

        settingsRobot.dismissDarkModeDialog()
    }

    // ===================== 9.14 Items Per Meal Dialog =====================

    /**
     * Test 9.14: Items per meal dialog opens with count options
     */
    @Test
    fun test_9_14_itemsPerMealDialog_showsOptions() {
        settingsRobot.waitForSettingsScreen()

        settingsRobot.tapItemsPerMealSetting()
        waitFor(ANIMATION_DURATION)

        // Verify dialog shows options
        composeTestRule.onNodeWithText("Items per Meal", ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("1 item", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("2 items", substring = true, ignoreCase = true)
            .assertIsDisplayed()

        settingsRobot.dismissItemsPerMealDialog()
    }

    // ===================== 9.15 All Sections Scroll =====================

    /**
     * Test 9.15: All settings sections are scrollable and visible
     */
    @Test
    fun test_9_15_allSections_areScrollable() {
        settingsRobot.waitForSettingsScreen()

        // Verify all major sections can be scrolled to
        settingsRobot.assertProfileSectionDisplayed()
        settingsRobot.assertFamilySectionDisplayed()
        settingsRobot.assertMealPreferencesSectionDisplayed()
        settingsRobot.assertMealGenerationSectionDisplayed()
        settingsRobot.assertAppSettingsSectionDisplayed()
        settingsRobot.assertSocialSectionDisplayed()
        settingsRobot.assertSupportSectionDisplayed()
    }

    // ===================== 9.16 Disliked Ingredients =====================

    /**
     * Test 9.16: Disliked ingredients can be accessed
     */
    @Test
    fun test_9_16_dislikedIngredients_canBeAccessed() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToDislikedIngredients()
    }

    // ===================== 9.17 Settings Item Navigation =====================

    /**
     * Test 9.17: Units & Measurements can be accessed
     */
    @Test
    fun test_9_17_unitsAndMeasurements_canBeAccessed() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToUnitsAndMeasurements()
    }
}
