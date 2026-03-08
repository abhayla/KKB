package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.presentation.common.TestTags

/**
 * Robot for Household Notifications screen interactions.
 * Covers list display, mark-as-read, badge count, and empty state flows.
 */
class HouseholdNotificationsRobot(private val composeTestRule: ComposeContentTestRule) {

    fun waitForNotificationsScreen(timeoutMillis: Long = 10000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(
            TestTags.HOUSEHOLD_NOTIFICATION_SCREEN,
            timeoutMillis
        )
    }

    fun assertNotificationsScreenDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_NOTIFICATION_SCREEN).assertIsDisplayed()
    }

    fun assertNotificationListDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_NOTIFICATION_LIST).assertIsDisplayed()
    }

    fun assertNotificationItemDisplayed(index: Int) = apply {
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_NOTIFICATION_ITEM_PREFIX}$index")
            .assertIsDisplayed()
    }

    fun tapMarkRead(index: Int) = apply {
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_NOTIFICATION_MARK_READ_PREFIX}$index")
            .performClick()
        composeTestRule.waitForIdle()
    }

    fun assertEmptyState() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_NOTIFICATION_EMPTY).assertIsDisplayed()
    }

    fun assertBadgeCount(count: Int) = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_NOTIFICATION_BADGE)
            .assertTextContains("$count", substring = true)
    }

    fun assertBadgeNotDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_NOTIFICATION_BADGE).assertDoesNotExist()
    }
}
