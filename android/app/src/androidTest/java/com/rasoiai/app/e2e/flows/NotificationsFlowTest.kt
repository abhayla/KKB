package com.rasoiai.app.e2e.flows

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Requirement: #79 - FR-023: Notifications screen E2E flow test
 *
 * Tests the Notifications screen navigation, list display, filtering,
 * mark-all-read, and empty state.
 *
 * All tests are @Ignore because they require:
 * - Running backend with notifications endpoints active
 * - Pre-existing notification data for the test user
 *
 * @see docs/testing/Functional-Requirement-Rule.md
 */
@HiltAndroidTest
class NotificationsFlowTest : BaseE2ETest() {

    companion object {
        private const val TAG = "NotificationsFlowTest"
    }

    private lateinit var homeRobot: HomeRobot

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedStateWithoutMealPlan()

        homeRobot = HomeRobot(composeTestRule)
    }

    // ===================== Helper =====================

    private fun navigateToNotifications() {
        homeRobot.waitForHomeScreen(60000)
        homeRobot.tapNotificationsButton()

        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodes(
                hasTestTag(TestTags.NOTIFICATIONS_SCREEN)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ===================== Tests =====================

    @Test
    @Ignore("Requires running backend with notifications endpoints")
    fun testNavigateToNotifications() {
        navigateToNotifications()

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_SCREEN).assertIsDisplayed()

        Log.i(TAG, "testNavigateToNotifications: navigated via top bar button")
    }

    @Test
    @Ignore("Requires running backend with notifications endpoints")
    fun testNotificationsListDisplayed() {
        navigateToNotifications()

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_LIST).assertIsDisplayed()

        // At least one notification item should be visible
        composeTestRule.onNodeWithTag("${TestTags.NOTIFICATION_ITEM_PREFIX}0").assertIsDisplayed()

        Log.i(TAG, "testNotificationsListDisplayed: notification list with items visible")
    }

    @Test
    @Ignore("Requires running backend with notifications endpoints")
    fun testFilterByUnread() {
        navigateToNotifications()

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_SCREEN).assertIsDisplayed()

        // Tap the "Unread" filter chip
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATION_FILTER_UNREAD).performClick()
        composeTestRule.waitForIdle()

        // The filter should now be active and show only unread notifications
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATION_FILTER_UNREAD).assertIsDisplayed()

        Log.i(TAG, "testFilterByUnread: filtered to unread notifications")
    }

    @Test
    @Ignore("Requires running backend with notifications endpoints")
    fun testFilterByAll() {
        navigateToNotifications()

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_SCREEN).assertIsDisplayed()

        // First switch to Unread, then back to All
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATION_FILTER_UNREAD).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATION_FILTER_ALL).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_LIST).assertIsDisplayed()

        Log.i(TAG, "testFilterByAll: switched back to all notifications")
    }

    @Test
    @Ignore("Requires running backend with notifications endpoints")
    fun testMarkAllRead() {
        navigateToNotifications()

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_SCREEN).assertIsDisplayed()

        // Tap "Mark all read" button
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_MARK_ALL_READ).performClick()
        composeTestRule.waitForIdle()

        // After marking all read, switching to Unread filter should show empty or no items
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATION_FILTER_UNREAD).performClick()
        composeTestRule.waitForIdle()

        Log.i(TAG, "testMarkAllRead: marked all notifications as read")
    }

    @Test
    @Ignore("Requires running backend with notifications endpoints")
    fun testEmptyState() {
        navigateToNotifications()

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_SCREEN).assertIsDisplayed()

        // When there are no notifications, the empty state should be visible
        // This test assumes no notifications exist for the test user
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_EMPTY).assertIsDisplayed()

        Log.i(TAG, "testEmptyState: empty state displayed when no notifications")
    }

    @Test
    @Ignore("Requires running backend with notifications endpoints")
    fun testClearAllNotifications() {
        navigateToNotifications()

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_SCREEN).assertIsDisplayed()

        // Tap "Clear all" button
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_CLEAR_ALL).performClick()
        composeTestRule.waitForIdle()

        // Confirmation dialog should appear
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_CLEAR_ALL_DIALOG).assertIsDisplayed()

        // Confirm the clear action
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_CLEAR_ALL_CONFIRM).performClick()
        composeTestRule.waitForIdle()

        // After clearing, empty state should be visible
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_EMPTY).assertIsDisplayed()

        Log.i(TAG, "testClearAllNotifications: cleared all and empty state shown")
    }
}
