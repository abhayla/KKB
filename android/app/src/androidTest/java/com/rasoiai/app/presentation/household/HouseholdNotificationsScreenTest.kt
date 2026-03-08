package com.rasoiai.app.presentation.household

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.HouseholdNotification
import com.rasoiai.domain.model.HouseholdNotificationType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * UI Tests for HouseholdNotificationsScreen.
 *
 * Tests notification list display, empty state, unread badge,
 * and mark-as-read button visibility.
 */
@RunWith(AndroidJUnit4::class)
class HouseholdNotificationsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Test Data

    private fun createTestNotification(
        id: String = "notif-1",
        title: String = "New Member",
        message: String = "Sunita joined",
        isRead: Boolean = false,
        type: HouseholdNotificationType = HouseholdNotificationType.MEMBER_JOINED
    ) = HouseholdNotification(
        id = id,
        householdId = "hh-1",
        type = type,
        title = title,
        message = message,
        isRead = isRead,
        metadata = null,
        createdAt = LocalDateTime.of(2026, 3, 1, 10, 0)
    )

    // endregion

    @Test
    fun screenTag_displayed() {
        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdNotificationsScreen(
                    uiState = HouseholdNotificationsUiState()
                )
            }
        }

        composeTestRule
            .onNodeWithTag(TestTags.HOUSEHOLD_NOTIFICATION_SCREEN)
            .assertIsDisplayed()
    }

    @Test
    fun loadingIndicator_displayed() {
        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdNotificationsScreen(
                    uiState = HouseholdNotificationsUiState(isLoading = true)
                )
            }
        }

        composeTestRule
            .onNodeWithTag(TestTags.HOUSEHOLD_NOTIFICATION_SCREEN)
            .assertIsDisplayed()
    }

    @Test
    fun notificationList_withItems() {
        val notifications = listOf(
            createTestNotification(
                id = "notif-1",
                title = "New Member",
                message = "Sunita joined the household"
            ),
            createTestNotification(
                id = "notif-2",
                title = "Plan Updated",
                message = "Ramesh regenerated the meal plan",
                type = HouseholdNotificationType.PLAN_REGENERATED
            )
        )

        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdNotificationsScreen(
                    uiState = HouseholdNotificationsUiState(
                        notifications = notifications,
                        unreadCount = 2
                    )
                )
            }
        }

        composeTestRule
            .onNodeWithTag(TestTags.HOUSEHOLD_NOTIFICATION_LIST)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("New Member").assertIsDisplayed()
        composeTestRule.onNodeWithText("Plan Updated").assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(TestTags.HOUSEHOLD_NOTIFICATION_ITEM_PREFIX + "notif-1")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(TestTags.HOUSEHOLD_NOTIFICATION_ITEM_PREFIX + "notif-2")
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_displayed() {
        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdNotificationsScreen(
                    uiState = HouseholdNotificationsUiState(
                        isLoading = false,
                        notifications = emptyList()
                    )
                )
            }
        }

        composeTestRule
            .onNodeWithTag(TestTags.HOUSEHOLD_NOTIFICATION_EMPTY)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("No notifications yet").assertIsDisplayed()
    }

    @Test
    fun unreadCountBadge_displayed() {
        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdNotificationsScreen(
                    uiState = HouseholdNotificationsUiState(
                        notifications = listOf(
                            createTestNotification(id = "n1", isRead = false),
                            createTestNotification(id = "n2", isRead = false),
                            createTestNotification(id = "n3", isRead = false)
                        ),
                        unreadCount = 3
                    )
                )
            }
        }

        composeTestRule
            .onNodeWithTag(TestTags.HOUSEHOLD_NOTIFICATION_BADGE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun markAsReadButton_onUnreadItems() {
        val unreadNotification = createTestNotification(
            id = "notif-unread",
            title = "Unread Notification",
            message = "This is unread",
            isRead = false
        )

        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdNotificationsScreen(
                    uiState = HouseholdNotificationsUiState(
                        notifications = listOf(unreadNotification),
                        unreadCount = 1
                    )
                )
            }
        }

        composeTestRule
            .onNodeWithTag(TestTags.HOUSEHOLD_NOTIFICATION_MARK_READ_PREFIX + "notif-unread")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Mark as read").assertIsDisplayed()
    }
}
