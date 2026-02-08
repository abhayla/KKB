package com.rasoiai.app.presentation.notifications

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.Notification
import com.rasoiai.domain.model.NotificationActionData
import com.rasoiai.domain.model.NotificationActionType
import com.rasoiai.domain.model.NotificationType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Requirement: #36 - FR-009: Add UI tests for NotificationsScreen
 *
 * Tests the NotificationsScreen composable with various UI states.
 * Covers: NOTIF-001 through NOTIF-014
 */
@RunWith(AndroidJUnit4::class)
class NotificationsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Test Data Factories

    private fun createTestNotification(
        id: String = "notif_1",
        type: NotificationType = NotificationType.FESTIVAL_REMINDER,
        title: String = "Makar Sankranti in 3 days!",
        body: String = "Plan your festive meals now.",
        isRead: Boolean = false,
        actionType: NotificationActionType = NotificationActionType.OPEN_MEAL_PLAN,
        actionData: NotificationActionData? = null,
        createdAt: Long = System.currentTimeMillis() - 60_000 // 1 minute ago
    ) = Notification(
        id = id,
        type = type,
        title = title,
        body = body,
        imageUrl = null,
        actionType = actionType,
        actionData = actionData,
        isRead = isRead,
        createdAt = createdAt
    )

    private fun createTestUiState(
        isLoading: Boolean = false,
        isRefreshing: Boolean = false,
        errorMessage: String? = null,
        notifications: List<Notification> = emptyList(),
        unreadCount: Int = 0,
        filter: NotificationFilter = NotificationFilter.ALL
    ) = NotificationsUiState(
        isLoading = isLoading,
        isRefreshing = isRefreshing,
        errorMessage = errorMessage,
        notifications = notifications,
        unreadCount = unreadCount,
        filter = filter
    )

    private fun createMixedNotifications(): List<Notification> = listOf(
        createTestNotification(
            id = "1",
            type = NotificationType.FESTIVAL_REMINDER,
            title = "Makar Sankranti in 3 days!",
            body = "Plan your festive meals now.",
            isRead = false,
            createdAt = System.currentTimeMillis() - 60_000 // 1 min ago (Today)
        ),
        createTestNotification(
            id = "2",
            type = NotificationType.MEAL_PLAN_UPDATE,
            title = "Your meal plan is ready!",
            body = "Week of Jan 20-26. 21 meals planned.",
            isRead = true,
            actionType = NotificationActionType.OPEN_MEAL_PLAN,
            createdAt = System.currentTimeMillis() - 3_600_000 // 1 hour ago (Today)
        ),
        createTestNotification(
            id = "3",
            type = NotificationType.SHOPPING_REMINDER,
            title = "Time to shop!",
            body = "Your grocery list has 15 items.",
            isRead = false,
            actionType = NotificationActionType.OPEN_GROCERY,
            createdAt = System.currentTimeMillis() - 7_200_000 // 2 hours ago (Today)
        ),
        createTestNotification(
            id = "4",
            type = NotificationType.RECIPE_SUGGESTION,
            title = "Try Palak Paneer tonight",
            body = "Based on your preferences.",
            isRead = true,
            actionType = NotificationActionType.OPEN_RECIPE,
            actionData = NotificationActionData(recipeId = "recipe_123"),
            createdAt = System.currentTimeMillis() - 7_200_000 // 2 hours ago
        ),
        createTestNotification(
            id = "5",
            type = NotificationType.STREAK_MILESTONE,
            title = "Achievement Unlocked!",
            body = "You earned the '7-Day Streak' badge!",
            isRead = false,
            actionType = NotificationActionType.OPEN_STATS,
            createdAt = System.currentTimeMillis() - 10_800_000 // 3 hours ago
        )
    )

    // endregion

    // region NOTIF-001: Screen Display Tests

    @Test
    fun notificationsScreen_displaysScreenTag() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_SCREEN).assertIsDisplayed()
    }

    @Test
    fun notificationsScreen_displaysTitle() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
    }

    @Test
    fun notificationsScreen_displaysBackButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    // endregion

    // region NOTIF-014: Empty State Tests

    @Test
    fun emptyState_displaysMessage_whenAllFilter() {
        val uiState = createTestUiState(
            notifications = emptyList(),
            filter = NotificationFilter.ALL
        )

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithText("No notifications yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("You'll see festival reminders, meal updates, and shopping list notifications here.", substring = true).assertIsDisplayed()
    }

    @Test
    fun emptyState_displaysUnreadMessage_whenUnreadFilter() {
        val uiState = createTestUiState(
            notifications = listOf(
                createTestNotification(id = "1", isRead = true)
            ),
            filter = NotificationFilter.UNREAD
        )

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithText("No unread notifications").assertIsDisplayed()
    }

    // endregion

    // region Loading State Tests

    @Test
    fun loadingState_displaysLoadingIndicator() {
        val uiState = createTestUiState(isLoading = true)

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_LOADING).assertIsDisplayed()
    }

    @Test
    fun loadingState_doesNotShowEmptyOrList() {
        val uiState = createTestUiState(isLoading = true)

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_EMPTY).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_LIST).assertDoesNotExist()
    }

    // endregion

    // region NOTIF-004: Notification List Display Tests

    @Test
    fun notificationList_displaysNotifications() {
        val notifications = createMixedNotifications()
        val uiState = createTestUiState(
            notifications = notifications,
            unreadCount = 3
        )

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_LIST).assertIsDisplayed()
        composeTestRule.onNodeWithText("Makar Sankranti in 3 days!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your meal plan is ready!").assertIsDisplayed()
    }

    @Test
    fun notificationList_displaysNotificationBody() {
        val notifications = createMixedNotifications()
        val uiState = createTestUiState(
            notifications = notifications,
            unreadCount = 3
        )

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Plan your festive meals now.").assertIsDisplayed()
    }

    @Test
    fun notificationList_displaysGroupHeader() {
        val notifications = createMixedNotifications()
        val uiState = createTestUiState(
            notifications = notifications,
            unreadCount = 3
        )

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        // All test notifications are "Today" since they're created with recent timestamps
        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
    }

    // endregion

    // region Filter Tests

    @Test
    fun filterChips_displayAllAndUnread() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATION_FILTER_ALL).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATION_FILTER_UNREAD).assertIsDisplayed()
    }

    @Test
    fun filterChip_displaysUnreadCount_whenPositive() {
        val notifications = createMixedNotifications()
        val uiState = createTestUiState(
            notifications = notifications,
            unreadCount = 3
        )

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Unread (3)").assertIsDisplayed()
    }

    @Test
    fun filterChip_displaysUnreadWithoutCount_whenZero() {
        val uiState = createTestUiState(unreadCount = 0)

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Unread").assertIsDisplayed()
    }

    @Test
    fun filterChip_click_triggersCallback() {
        var selectedFilter: NotificationFilter? = null
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(
                    uiState = uiState,
                    onFilterSelected = { selectedFilter = it }
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATION_FILTER_UNREAD).performClick()

        assert(selectedFilter == NotificationFilter.UNREAD) {
            "Expected UNREAD filter but got $selectedFilter"
        }
    }

    // endregion

    // region NOTIF-011: Mark All Read Tests

    @Test
    fun markAllRead_displaysButton_whenUnreadExist() {
        val notifications = createMixedNotifications()
        val uiState = createTestUiState(
            notifications = notifications,
            unreadCount = 3
        )

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_MARK_ALL_READ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Mark all read").assertIsDisplayed()
    }

    @Test
    fun markAllRead_hidesButton_whenNoUnread() {
        val uiState = createTestUiState(
            notifications = listOf(
                createTestNotification(id = "1", isRead = true)
            ),
            unreadCount = 0
        )

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_MARK_ALL_READ).assertDoesNotExist()
    }

    @Test
    fun markAllRead_click_triggersCallback() {
        var markAllReadClicked = false
        val uiState = createTestUiState(
            notifications = createMixedNotifications(),
            unreadCount = 3
        )

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(
                    uiState = uiState,
                    onMarkAllRead = { markAllReadClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_MARK_ALL_READ).performClick()

        assert(markAllReadClicked) { "Mark all read callback was not triggered" }
    }

    // endregion

    // region NOTIF-002: Back Navigation Tests

    @Test
    fun backButton_click_triggersCallback() {
        var backClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(
                    uiState = uiState,
                    onNavigateBack = { backClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assert(backClicked) { "Back navigation callback was not triggered" }
    }

    // endregion

    // region NOTIF-010: Notification Click Tests

    @Test
    fun notification_click_triggersCallback() {
        var clickedNotification: Notification? = null
        val notifications = createMixedNotifications()
        val uiState = createTestUiState(
            notifications = notifications,
            unreadCount = 3
        )

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(
                    uiState = uiState,
                    onNotificationClick = { clickedNotification = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Makar Sankranti in 3 days!").performClick()

        assert(clickedNotification != null) { "Notification click callback was not triggered" }
        assert(clickedNotification?.id == "1") {
            "Expected notification id '1' but got '${clickedNotification?.id}'"
        }
    }

    // endregion

    // region NOTIF-005: Notification Type Display Tests

    @Test
    fun notificationTypes_allDisplayCorrectly() {
        val notifications = createMixedNotifications()
        val uiState = createTestUiState(
            notifications = notifications,
            unreadCount = 3
        )

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        // Verify all notification types render their titles
        composeTestRule.onNodeWithText("Makar Sankranti in 3 days!").assertIsDisplayed() // FESTIVAL
        composeTestRule.onNodeWithText("Your meal plan is ready!").assertIsDisplayed()    // MEAL_PLAN
        composeTestRule.onNodeWithText("Time to shop!").assertIsDisplayed()               // SHOPPING
        composeTestRule.onNodeWithText("Try Palak Paneer tonight").assertIsDisplayed()    // RECIPE
        composeTestRule.onNodeWithText("Achievement Unlocked!").assertIsDisplayed()       // STREAK
    }

    // endregion

    // region NOTIF-009: Unread Indicator Tests

    @Test
    fun unreadNotification_displaysUnreadBadge() {
        val notifications = listOf(
            createTestNotification(id = "1", isRead = false, title = "Unread notification")
        )
        val uiState = createTestUiState(
            notifications = notifications,
            unreadCount = 1
        )

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATION_BADGE, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun readNotification_doesNotDisplayUnreadBadge() {
        val notifications = listOf(
            createTestNotification(id = "1", isRead = true, title = "Read notification")
        )
        val uiState = createTestUiState(
            notifications = notifications,
            unreadCount = 0
        )

        composeTestRule.setContent {
            RasoiAITheme {
                NotificationsScreenContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATION_BADGE).assertDoesNotExist()
    }

    // endregion
}
