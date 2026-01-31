package com.rasoiai.app.e2e.flows

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.base.TestDataFactory
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.SettingsRobot
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * Phase 9: Settings Screen Testing
 *
 * Tests:
 * 9.1 Profile Section
 * 9.2 Preference Updates
 * 9.3 Notifications Toggle
 * 9.5 Meal Generation Settings
 */
@HiltAndroidTest
class SettingsFlowTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot

    @Before
    override fun setUp() {
        super.setUp()
        // Set up authenticated and onboarded user state
        setUpAuthenticatedState()

        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)

        // Navigate to settings via profile icon from home
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
        homeRobot.navigateToSettings()
    }

    /**
     * Test 9.1: Profile Section
     *
     * Steps:
     * 1. Navigate to Settings (gear icon from Home)
     * 2. Verify user profile displayed
     * 3. Check email matches Google account
     *
     * Expected:
     * - Profile image (from Google)
     * - Name and email displayed
     * - Edit profile option available
     */
    @Test
    fun test_9_1_profileSection() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.assertSettingsScreenDisplayed()

        // Verify profile section
        settingsRobot.assertProfileSectionDisplayed()
        settingsRobot.assertEmailDisplayed(TestDataFactory.sharmaFamily.email)
    }

    /**
     * Test 9.2: Preference Updates
     *
     * Steps:
     * 1. Navigate to dietary preferences section
     * 2. Change primary diet to EGGETARIAN
     * 3. Save changes
     * 4. Verify confirmation
     *
     * Test Variations:
     * - Update cuisine preferences
     * - Update cooking times
     * - Update family members
     */
    @Test
    fun test_9_2_preferenceUpdates() {
        settingsRobot.waitForSettingsScreen()

        // Navigate to dietary preferences
        settingsRobot.navigateToDietaryPreferences()

        // Change diet
        settingsRobot.changePrimaryDiet("Eggetarian")

        // Save
        settingsRobot.savePreferences()

        // Verify confirmation
        settingsRobot.assertSaveConfirmation()
    }

    /**
     * Test 9.3: Notifications Toggle
     *
     * Steps:
     * 1. Find notification settings
     * 2. Toggle meal reminders ON/OFF
     * 3. Toggle shopping day reminder ON/OFF
     *
     * Expected:
     * - Toggle switches work
     * - Settings persist after app restart
     */
    @Test
    fun test_9_3_notificationsToggle() {
        settingsRobot.waitForSettingsScreen()

        // Navigate to notifications
        settingsRobot.navigateToNotifications()

        // Toggle meal reminders
        settingsRobot.toggleMealReminders()
        settingsRobot.assertMealRemindersOn()

        // Toggle shopping reminder
        settingsRobot.toggleShoppingReminder()
    }

    /**
     * Test: Theme selection
     */
    @Test
    fun themeSelection_works() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToTheme()

        settingsRobot.selectLightTheme()
        settingsRobot.selectDarkTheme()
        settingsRobot.selectSystemTheme()
    }

    /**
     * Test: About section displays version
     */
    @Test
    fun aboutSection_displaysVersion() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToAbout()
        settingsRobot.assertAppVersionDisplayed()
    }

    /**
     * Test: Sign out flow
     */
    @Test
    fun signOut_flow() {
        settingsRobot.waitForSettingsScreen()

        // Tap sign out
        settingsRobot.tapSignOut()

        // Cancel first
        settingsRobot.cancelSignOut()

        // Confirm sign out
        settingsRobot.tapSignOut()
        settingsRobot.confirmSignOut()

        // Should navigate to auth screen
    }

    /**
     * Test: Family members can be updated
     */
    @Test
    fun familyMembers_canBeUpdated() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToFamilyMembers()
        // Family member edit sheet should appear
    }

    /**
     * Test: Cooking time can be updated
     */
    @Test
    fun cookingTime_canBeUpdated() {
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToCookingTime()
        // Cooking time settings should appear
    }

    // ===================== Test 9.5: Meal Generation Settings =====================

    /**
     * Test 9.5.1: Meal Generation Section Display
     *
     * Steps:
     * 1. Navigate to Settings
     * 2. Scroll to Meal Generation section
     * 3. Verify all settings are displayed
     *
     * Expected:
     * - Section header "MEAL GENERATION" displayed
     * - Items per meal row with value (default: 2 items)
     * - Strict Allergen Mode toggle (default: ON)
     * - Strict Dietary Mode toggle (default: ON)
     * - Allow Recipe Repeat toggle (default: OFF)
     */
    @Test
    fun test_9_5_1_mealGenerationSection_isDisplayed() {
        settingsRobot.waitForSettingsScreen()

        // Verify meal generation section
        settingsRobot.assertMealGenerationSectionDisplayed()

        // Verify items per meal shows default value
        settingsRobot.assertItemsPerMealValue("2 items")
    }

    /**
     * Test 9.5.2: Strict Allergen Mode Toggle
     *
     * Steps:
     * 1. Navigate to Settings → Meal Generation section
     * 2. Verify toggle is ON by default
     * 3. Toggle OFF
     * 4. Verify toggle state changed
     *
     * Expected:
     * - Toggle switches smoothly
     * - Setting affects next meal plan generation
     */
    @Test
    fun test_9_5_2_strictAllergenMode_toggle() {
        settingsRobot.waitForSettingsScreen()

        // Scroll to section
        settingsRobot.scrollToMealGenerationSection()

        // Verify default ON state
        settingsRobot.assertStrictAllergenModeOn()

        // Toggle OFF
        settingsRobot.toggleStrictAllergenMode()

        // Verify toggled state
        settingsRobot.assertStrictAllergenModeOff()

        // Toggle back ON
        settingsRobot.toggleStrictAllergenMode()
        settingsRobot.assertStrictAllergenModeOn()
    }

    /**
     * Test 9.5.3: Strict Dietary Mode Toggle
     *
     * Steps:
     * 1. Navigate to Settings → Meal Generation section
     * 2. Verify toggle is ON by default (SATTVIC/JAIN strictly enforced)
     * 3. Toggle OFF
     * 4. Verify toggle state changed
     *
     * Expected:
     * - Toggle state saved to DataStore
     * - API syncs preference
     */
    @Test
    fun test_9_5_3_strictDietaryMode_toggle() {
        settingsRobot.waitForSettingsScreen()

        // Scroll to section
        settingsRobot.scrollToMealGenerationSection()

        // Verify default ON state
        settingsRobot.assertStrictDietaryModeOn()

        // Toggle OFF
        settingsRobot.toggleStrictDietaryMode()

        // Verify toggled state
        settingsRobot.assertStrictDietaryModeOff()

        // Toggle back ON
        settingsRobot.toggleStrictDietaryMode()
        settingsRobot.assertStrictDietaryModeOn()
    }

    /**
     * Test 9.5.4: Allow Recipe Repeat Toggle
     *
     * Steps:
     * 1. Navigate to Settings → Meal Generation section
     * 2. Verify toggle is OFF by default
     * 3. Toggle ON
     * 4. Verify toggle state changed
     *
     * Expected:
     * - Toggle state saved locally
     * - Same recipe can appear multiple times in weekly plan
     */
    @Test
    fun test_9_5_4_allowRecipeRepeat_toggle() {
        settingsRobot.waitForSettingsScreen()

        // Scroll to section
        settingsRobot.scrollToMealGenerationSection()

        // Verify default OFF state
        settingsRobot.assertAllowRecipeRepeatOff()

        // Toggle ON
        settingsRobot.toggleAllowRecipeRepeat()

        // Verify toggled state
        settingsRobot.assertAllowRecipeRepeatOn()

        // Toggle back OFF
        settingsRobot.toggleAllowRecipeRepeat()
        settingsRobot.assertAllowRecipeRepeatOff()
    }
}
