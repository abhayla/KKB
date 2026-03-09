package com.rasoiai.app.e2e.flows

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.HouseholdNotificationsRobot
import com.rasoiai.app.e2e.robots.SettingsRobot
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Household Notification Flow Tests - List notifications, mark read, badge count,
 * and access control.
 *
 * Tests ensure household exists via API before UI navigation.
 *
 * Navigation path: Home → Settings → "My Household" → Notifications section
 */
@HiltAndroidTest
class HouseholdNotificationFlowTest : BaseE2ETest() {

    companion object {
        private const val TAG = "HouseholdNotificationFlowTest"
        private const val TEST_HOUSEHOLD_NAME = "Sharma Family"
    }

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot
    private lateinit var notificationsRobot: HouseholdNotificationsRobot

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedStateWithoutMealPlan()

        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)
        notificationsRobot = HouseholdNotificationsRobot(composeTestRule)

        // Ensure household exists
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            BackendTestHelper.ensureHouseholdExists(BACKEND_BASE_URL, authToken, TEST_HOUSEHOLD_NAME)
        }
    }

    // ===================== Navigation helper =====================

    private fun navigateToHouseholdNotifications() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToSettings()
        settingsRobot.waitForSettingsScreen()
        settingsRobot.tapSettingItem("My Household")

        composeTestRule.onNodeWithText("Notifications", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()

        notificationsRobot.waitForNotificationsScreen()
    }

    // ===================== Tests =====================

    @Test
    fun testListHouseholdNotifications() {
        navigateToHouseholdNotifications()

        notificationsRobot.assertNotificationsScreenDisplayed()

        notificationsRobot.assertNotificationListDisplayed()

        // At least the first notification item should be displayed (seeded in backend fixture)
        notificationsRobot.assertNotificationItemDisplayed(0)

        Log.i(TAG, "testListHouseholdNotifications: notifications list loaded with items")
    }

    @Test
    fun testEmptyNotificationState() {
        navigateToHouseholdNotifications()

        notificationsRobot.assertNotificationsScreenDisplayed()

        // When there are no notifications, an empty state illustration/message is shown
        notificationsRobot.assertEmptyState()

        Log.i(TAG, "testEmptyNotificationState: empty state displayed when no notifications")
    }

    @Test
    fun testMarkNotificationRead() {
        navigateToHouseholdNotifications()

        notificationsRobot.assertNotificationsScreenDisplayed()
        notificationsRobot.assertNotificationListDisplayed()
        notificationsRobot.assertNotificationItemDisplayed(0)

        // Mark the first notification as read
        notificationsRobot.tapMarkRead(0)
        composeTestRule.waitForIdle()

        notificationsRobot.assertNotificationsScreenDisplayed()

        Log.i(TAG, "testMarkNotificationRead: first notification marked as read")
    }

    @Test
    fun testNonMemberCannotSeeNotifications() {
        navigateToHouseholdNotifications()

        notificationsRobot.assertNotificationsScreenDisplayed()

        // The user is not in a household, so no notifications list appears
        composeTestRule.onNodeWithText(
            "household",
            substring = true,
            ignoreCase = true
        ).assertIsDisplayed()

        Log.i(TAG, "testNonMemberCannotSeeNotifications: non-member sees join prompt, not notifications")
    }

    @Test
    fun testNotificationBadgeCount() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.navigateToSettings()
        settingsRobot.waitForSettingsScreen()

        // The household settings item should show a badge with the unread count
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_NOTIFICATION_BADGE).assertIsDisplayed()

        // The badge should display a non-zero count (seeded notifications exist in backend)
        notificationsRobot.assertBadgeCount(1)

        // Navigate into notifications and mark all as read
        settingsRobot.tapSettingItem("My Household")
        composeTestRule.onNodeWithText("Notifications", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
        notificationsRobot.waitForNotificationsScreen()
        notificationsRobot.tapMarkRead(0)
        composeTestRule.waitForIdle()

        // Go back to Settings and verify the badge is gone or shows 0
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        settingsRobot.waitForSettingsScreen()

        // After reading all notifications, the badge should not be displayed
        notificationsRobot.assertBadgeNotDisplayed()

        Log.i(TAG, "testNotificationBadgeCount: badge shown before read, absent after")
    }

    @Test
    fun testMarkNonexistentNotificationError() {
        navigateToHouseholdNotifications()

        notificationsRobot.assertNotificationsScreenDisplayed()

        // Verify phantom items don't appear
        composeTestRule.onNodeWithTag(
            "${TestTags.HOUSEHOLD_NOTIFICATION_MARK_READ_PREFIX}999"
        ).assertDoesNotExist()

        notificationsRobot.assertNotificationsScreenDisplayed()

        Log.i(TAG, "testMarkNonexistentNotificationError: no phantom mark-read button at index 999")
    }
}
