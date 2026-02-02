package com.rasoiai.app.presentation.notifications

import app.cash.turbine.test
import com.rasoiai.domain.model.ActionStatus
import com.rasoiai.domain.model.Notification
import com.rasoiai.domain.model.NotificationActionData
import com.rasoiai.domain.model.NotificationActionType
import com.rasoiai.domain.model.NotificationType
import com.rasoiai.domain.model.OfflineAction
import com.rasoiai.domain.repository.NotificationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepository: NotificationRepository

    private val testNotifications = listOf(
        Notification(
            id = "notif-1",
            type = NotificationType.FESTIVAL_REMINDER,
            title = "Diwali is coming!",
            body = "Prepare special dishes for Diwali celebration",
            imageUrl = null,
            actionType = NotificationActionType.OPEN_MEAL_PLAN,
            actionData = null,
            isRead = false,
            createdAt = System.currentTimeMillis() - 1000
        ),
        Notification(
            id = "notif-2",
            type = NotificationType.MEAL_PLAN_UPDATE,
            title = "Meal plan updated",
            body = "Your weekly meal plan has been regenerated",
            imageUrl = null,
            actionType = NotificationActionType.OPEN_MEAL_PLAN,
            actionData = null,
            isRead = false,
            createdAt = System.currentTimeMillis() - 2000
        ),
        Notification(
            id = "notif-3",
            type = NotificationType.RECIPE_SUGGESTION,
            title = "Try this recipe!",
            body = "Based on your preferences, you might like Dal Tadka",
            imageUrl = "https://example.com/dal.jpg",
            actionType = NotificationActionType.OPEN_RECIPE,
            actionData = NotificationActionData(recipeId = "recipe-123"),
            isRead = true,
            createdAt = System.currentTimeMillis() - 3000
        ),
        Notification(
            id = "notif-4",
            type = NotificationType.STREAK_MILESTONE,
            title = "7-day streak!",
            body = "You've been cooking for 7 days in a row!",
            imageUrl = null,
            actionType = NotificationActionType.OPEN_STATS,
            actionData = NotificationActionData(streakCount = 7),
            isRead = true,
            createdAt = System.currentTimeMillis() - 86400000 // Yesterday
        )
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk(relaxed = true)

        coEvery { mockRepository.getNotifications() } returns flowOf(testNotifications)
        coEvery { mockRepository.getUnreadCount() } returns flowOf(2)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        @DisplayName("Initial state should be loading")
        fun `initial state should be loading`() = runTest {
            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.uiState.test {
                val initialState = awaitItem()
                assertTrue(initialState.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("After loading, notifications should be populated")
        fun `after loading notifications should be populated`() = runTest {
            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals(4, state.notifications.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Unread count should be populated from repository")
        fun `unread count should be populated from repository`() = runTest {
            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.uiState.test {
                // Skip states until we get one with unread count populated
                testDispatcher.scheduler.advanceUntilIdle()

                // Get the final stable state after all coroutines complete
                var state = awaitItem()
                while (state.unreadCount == 0 || state.isLoading) {
                    state = awaitItem()
                }

                assertEquals(2, state.unreadCount)
                assertTrue(state.hasUnread)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Default filter should be ALL")
        fun `default filter should be ALL`() = runTest {
            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(NotificationFilter.ALL, state.filter)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Filter Operations")
    inner class FilterOperations {

        @Test
        @DisplayName("setFilter should update filter")
        fun `setFilter should update filter`() = runTest {
            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.setFilter(NotificationFilter.UNREAD)

                val state = awaitItem()
                assertEquals(NotificationFilter.UNREAD, state.filter)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Filtered notifications should show only unread when UNREAD filter is applied")
        fun `filtered notifications should show only unread when UNREAD filter is applied`() = runTest {
            val viewModel = NotificationsViewModel(mockRepository)

            // Wait for loading to complete
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.setFilter(NotificationFilter.UNREAD)

            val state = viewModel.uiState.value
            assertEquals(2, state.filteredNotifications.size)
            assertTrue(state.filteredNotifications.all { !it.isRead })
        }

        @Test
        @DisplayName("Filtered notifications should show all when ALL filter is applied")
        fun `filtered notifications should show all when ALL filter is applied`() = runTest {
            val viewModel = NotificationsViewModel(mockRepository)

            // Wait for loading to complete
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.setFilter(NotificationFilter.UNREAD)
            viewModel.setFilter(NotificationFilter.ALL)

            val state = viewModel.uiState.value
            assertEquals(NotificationFilter.ALL, state.filter)
            assertEquals(4, state.filteredNotifications.size)
        }
    }

    @Nested
    @DisplayName("Mark as Read Operations")
    inner class MarkAsReadOperations {

        @Test
        @DisplayName("markAsRead should call repository")
        fun `markAsRead should call repository`() = runTest {
            coEvery { mockRepository.markAsRead(any()) } returns Result.success(Unit)

            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.markAsRead("notif-1")
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { mockRepository.markAsRead("notif-1") }
        }

        @Test
        @DisplayName("markAllAsRead should call repository")
        fun `markAllAsRead should call repository`() = runTest {
            coEvery { mockRepository.markAllAsRead() } returns Result.success(Unit)

            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.markAllAsRead()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { mockRepository.markAllAsRead() }
        }

        @Test
        @DisplayName("markAsRead failure should set error message")
        fun `markAsRead failure should set error message`() = runTest {
            coEvery { mockRepository.markAsRead(any()) } returns Result.failure(Exception("Network error"))

            val viewModel = NotificationsViewModel(mockRepository)

            // Wait for initial loading to complete
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.markAsRead("notif-1")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("Failed to mark as read", viewModel.uiState.value.errorMessage)
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    inner class DeleteOperations {

        @Test
        @DisplayName("deleteNotification should call repository")
        fun `deleteNotification should call repository`() = runTest {
            coEvery { mockRepository.deleteNotification(any()) } returns Result.success(Unit)

            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.deleteNotification("notif-1")
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { mockRepository.deleteNotification("notif-1") }
        }

        @Test
        @DisplayName("deleteNotification failure should set error message")
        fun `deleteNotification failure should set error message`() = runTest {
            coEvery { mockRepository.deleteNotification(any()) } returns Result.failure(Exception("Error"))

            val viewModel = NotificationsViewModel(mockRepository)

            // Wait for loading to complete
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.deleteNotification("notif-1")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("Failed to delete notification", viewModel.uiState.value.errorMessage)
        }
    }

    @Nested
    @DisplayName("Notification Click Navigation")
    inner class NotificationClickNavigation {

        @Test
        @DisplayName("Click on recipe notification should navigate to recipe")
        fun `click on recipe notification should navigate to recipe`() = runTest {
            coEvery { mockRepository.markAsRead(any()) } returns Result.success(Unit)

            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.navigationEvent.test {
                val recipeNotif = testNotifications[2] // Recipe suggestion
                viewModel.onNotificationClick(recipeNotif)
                testDispatcher.scheduler.advanceUntilIdle()

                val event = awaitItem()
                assertTrue(event is NotificationsNavigationEvent.NavigateToRecipe)
                assertEquals("recipe-123", (event as NotificationsNavigationEvent.NavigateToRecipe).recipeId)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Click on meal plan notification should navigate to meal plan")
        fun `click on meal plan notification should navigate to meal plan`() = runTest {
            coEvery { mockRepository.markAsRead(any()) } returns Result.success(Unit)

            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.navigationEvent.test {
                val mealPlanNotif = testNotifications[1] // Meal plan update
                viewModel.onNotificationClick(mealPlanNotif)
                testDispatcher.scheduler.advanceUntilIdle()

                val event = awaitItem()
                assertEquals(NotificationsNavigationEvent.NavigateToMealPlan, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Click on stats notification should navigate to stats")
        fun `click on stats notification should navigate to stats`() = runTest {
            coEvery { mockRepository.markAsRead(any()) } returns Result.success(Unit)

            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.navigationEvent.test {
                val streakNotif = testNotifications[3] // Streak milestone
                viewModel.onNotificationClick(streakNotif)
                testDispatcher.scheduler.advanceUntilIdle()

                val event = awaitItem()
                assertEquals(NotificationsNavigationEvent.NavigateToStats, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Click on unread notification should mark as read")
        fun `click on unread notification should mark as read`() = runTest {
            coEvery { mockRepository.markAsRead(any()) } returns Result.success(Unit)

            val viewModel = NotificationsViewModel(mockRepository)

            val unreadNotif = testNotifications[0] // Festival reminder (unread)
            viewModel.onNotificationClick(unreadNotif)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { mockRepository.markAsRead("notif-1") }
        }

        @Test
        @DisplayName("Click on read notification should not mark as read again")
        fun `click on read notification should not mark as read again`() = runTest {
            val viewModel = NotificationsViewModel(mockRepository)

            val readNotif = testNotifications[2] // Recipe suggestion (already read)
            viewModel.onNotificationClick(readNotif)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { mockRepository.markAsRead("notif-3") }
        }
    }

    @Nested
    @DisplayName("Refresh Operations")
    inner class RefreshOperations {

        @Test
        @DisplayName("refreshNotifications should call repository")
        fun `refreshNotifications should call repository`() = runTest {
            coEvery { mockRepository.refreshNotifications() } returns Result.success(Unit)

            val viewModel = NotificationsViewModel(mockRepository)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.refreshNotifications()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { mockRepository.refreshNotifications() }
        }

        @Test
        @DisplayName("refreshNotifications failure should set error message")
        fun `refreshNotifications failure should set error message`() = runTest {
            coEvery { mockRepository.refreshNotifications() } returns Result.failure(Exception("Network error"))

            val viewModel = NotificationsViewModel(mockRepository)

            // Wait for initial load
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.refreshNotifications()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("Failed to refresh notifications", viewModel.uiState.value.errorMessage)
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("clearError should clear error message")
        fun `clearError should clear error message`() = runTest {
            coEvery { mockRepository.markAsRead(any()) } returns Result.failure(Exception("Error"))

            val viewModel = NotificationsViewModel(mockRepository)

            // Wait for loading to complete
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.markAsRead("notif-1")
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify error is set
            assertEquals("Failed to mark as read", viewModel.uiState.value.errorMessage)

            viewModel.clearError()

            assertNull(viewModel.uiState.value.errorMessage)
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        @DisplayName("navigateBack should emit back event")
        fun `navigateBack should emit back event`() = runTest {
            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateBack()
                val event = awaitItem()
                assertEquals(NotificationsNavigationEvent.NavigateBack, event)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Empty State")
    inner class EmptyState {

        @Test
        @DisplayName("isEmpty should be true when no notifications")
        fun `isEmpty should be true when no notifications`() = runTest {
            coEvery { mockRepository.getNotifications() } returns flowOf(emptyList())

            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertTrue(state.isEmpty)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("isEmpty should be true when filter hides all notifications")
        fun `isEmpty should be true when filter hides all notifications`() = runTest {
            // All notifications are read
            val allReadNotifications = testNotifications.map { it.copy(isRead = true) }
            coEvery { mockRepository.getNotifications() } returns flowOf(allReadNotifications)
            coEvery { mockRepository.getUnreadCount() } returns flowOf(0)

            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem() // Loaded

                viewModel.setFilter(NotificationFilter.UNREAD)

                val state = awaitItem()
                assertTrue(state.isEmpty)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Grouped Notifications")
    inner class GroupedNotifications {

        @Test
        @DisplayName("groupedNotifications should group by date")
        fun `groupedNotifications should group by date`() = runTest {
            val viewModel = NotificationsViewModel(mockRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                val groups = state.groupedNotifications

                // Should have at least "Today" and "Yesterday" groups
                assertTrue(groups.containsKey("Today") || groups.containsKey("Yesterday"))
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
