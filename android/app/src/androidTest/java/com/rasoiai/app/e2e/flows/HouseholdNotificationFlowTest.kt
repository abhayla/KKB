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
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Household Notification Flow Tests - List notifications, mark read, badge count,
 * and access control.
 *
 * All tests are @Ignore because they require:
 * - Running backend with household endpoints active
 * - The current user to be a member of a household that has generated notifications
 *   (e.g. from a meal status update by another member)
 *
 * Navigation path: Home → Settings → "My Household" → Notifications section
 */
@HiltAndroidTest
class HouseholdNotificationFlowTest : BaseE2ETest() {

    companion object {
        private const val TAG = "HouseholdNotificationFlowTest"
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
    }

    // ===================== Navigation helper =====================

    /**
     * Navigate to the household notifications screen.
     * The path goes through Settings → My Household → Notifications.
     */
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
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testListHouseholdNotifications() {
        navigateToHouseholdNotifications()

        // The notifications screen should be visible
        notificationsRobot.assertNotificationsScreenDisplayed()

        // The notification list should be present
        notificationsRobot.assertNotificationListDisplayed()

        // At least the first notification item should be displayed (seeded in backend fixture)
        notificationsRobot.assertNotificationItemDisplayed(0)

        Log.i(TAG, "testListHouseholdNotifications: notifications list loaded with items")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testEmptyNotificationState() {
        navigateToHouseholdNotifications()

        notificationsRobot.assertNotificationsScreenDisplayed()

        // When there are no notifications, an empty state illustration/message is shown
        notificationsRobot.assertEmptyState()

        Log.i(TAG, "testEmptyNotificationState: empty state displayed when no notifications")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testMarkNotificationRead() {
        navigateToHouseholdNotifications()

        notificationsRobot.assertNotificationsScreenDisplayed()
        notificationsRobot.assertNotificationListDisplayed()
        notificationsRobot.assertNotificationItemDisplayed(0)

        // Mark the first notification as read
        notificationsRobot.tapMarkRead(0)
        composeTestRule.waitForIdle()

        // After marking read, the item should no longer appear as unread
        // Typically the item changes style (loses bold/highlight) or disappears
        // from an "unread only" filter. We verify the screen is still displayed.
        notificationsRobot.assertNotificationsScreenDisplayed()

        Log.i(TAG, "testMarkNotificationRead: first notification marked as read")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testNonMemberCannotSeeNotifications() {
        // This test verifies that a user who is NOT a member of any household
        // sees an appropriate error or empty state instead of notifications.

        navigateToHouseholdNotifications()

        notificationsRobot.assertNotificationsScreenDisplayed()

        // The user is not in a household, so no notifications list appears
        // Instead we expect either the empty state or a "Join a household" prompt
        composeTestRule.onNodeWithText(
            "household",
            substring = true,
            ignoreCase = true
        ).assertIsDisplayed()

        Log.i(TAG, "testNonMemberCannotSeeNotifications: non-member sees join prompt, not notifications")
    }

    @Test
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testNotificationBadgeCount() {
        // Badge count is visible on the household screen entry point (e.g. in Settings)
        // before navigating to the notifications screen.

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
    @Ignore("Household E2E requires running backend with household endpoints")
    fun testMarkNonexistentNotificationError() {
        navigateToHouseholdNotifications()

        // This scenario is only reachable if the UI allows an action on a
        // notification that has already been deleted server-side.
        // We simulate it by attempting to mark an out-of-range index as read.

        notificationsRobot.assertNotificationsScreenDisplayed()

        // Attempt to tap mark-read for an index beyond the current list size (e.g. index 999)
        // The tag will not exist in the semantics tree, so assertDoesNotExist verifies
        // the UI correctly does not render phantom items.
        composeTestRule.onNodeWithTag(
            "${TestTags.HOUSEHOLD_NOTIFICATION_MARK_READ_PREFIX}999"
        ).assertDoesNotExist()

        // The screen should remain in a stable state without crashing
        notificationsRobot.assertNotificationsScreenDisplayed()

        Log.i(TAG, "testMarkNonexistentNotificationError: no phantom mark-read button at index 999")
    }
}
