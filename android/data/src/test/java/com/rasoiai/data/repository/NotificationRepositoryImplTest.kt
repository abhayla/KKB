package com.rasoiai.data.repository

import android.database.sqlite.SQLiteException
import app.cash.turbine.test
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.dao.NotificationDao
import com.rasoiai.data.local.dao.OfflineQueueDao
import com.rasoiai.data.local.entity.NotificationEntity
import com.rasoiai.data.local.entity.OfflineQueueEntity
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.FcmTokenRequest
import com.rasoiai.data.remote.dto.NotificationDto
import com.rasoiai.data.remote.dto.NotificationsResponse
import com.rasoiai.data.remote.dto.SuccessResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockNotificationDao: NotificationDao
    private lateinit var mockOfflineQueueDao: OfflineQueueDao
    private lateinit var mockApiService: RasoiApiService
    private lateinit var mockNetworkMonitor: NetworkMonitor
    private lateinit var repository: NotificationRepositoryImpl

    // ==================== Test Fixtures ====================

    private val testNotificationEntity = NotificationEntity(
        id = "notif-1",
        type = "meal_plan_update",
        title = "Meal Plan Ready",
        body = "Your weekly meal plan has been generated",
        imageUrl = null,
        actionType = "open_meal_plan",
        actionData = null,
        isRead = false,
        createdAt = 1700000000000L,
        expiresAt = null
    )

    private val testNotificationEntity2 = NotificationEntity(
        id = "notif-2",
        type = "festival_reminder",
        title = "Diwali Coming!",
        body = "Plan your Diwali feast",
        imageUrl = null,
        actionType = "open_meal_plan",
        actionData = null,
        isRead = true,
        createdAt = 1700001000000L,
        expiresAt = null
    )

    private val testNotificationEntity3 = NotificationEntity(
        id = "notif-3",
        type = "streak_milestone",
        title = "7-Day Streak!",
        body = "You've been cooking for 7 days straight",
        imageUrl = null,
        actionType = "open_stats",
        actionData = null,
        isRead = false,
        createdAt = 1700002000000L,
        expiresAt = null
    )

    private val testNotificationDto = NotificationDto(
        id = "notif-api-1",
        type = "meal_plan_update",
        title = "New Meal Plan",
        body = "Your plan for next week is ready",
        imageUrl = null,
        actionType = "open_meal_plan",
        actionData = null,
        isRead = false,
        createdAt = "2026-01-27T10:00:00",
        expiresAt = null
    )

    private val testNotificationDto2 = NotificationDto(
        id = "notif-api-2",
        type = "shopping_reminder",
        title = "Grocery Reminder",
        body = "Time to shop for this week's ingredients",
        imageUrl = null,
        actionType = "open_grocery",
        actionData = null,
        isRead = false,
        createdAt = "2026-01-28T10:00:00",
        expiresAt = null
    )

    private val testNotificationsResponse = NotificationsResponse(
        notifications = listOf(testNotificationDto, testNotificationDto2),
        unreadCount = 2,
        totalCount = 2
    )

    private val emptyNotificationsResponse = NotificationsResponse(
        notifications = emptyList(),
        unreadCount = 0,
        totalCount = 0
    )

    // ==================== Setup / Teardown ====================

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockNotificationDao = mockk(relaxed = true)
        mockOfflineQueueDao = mockk(relaxed = true)
        mockApiService = mockk(relaxed = true)
        mockNetworkMonitor = mockk()
        repository = NotificationRepositoryImpl(
            mockNotificationDao,
            mockOfflineQueueDao,
            mockApiService,
            mockNetworkMonitor
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Tests ====================

    @Nested
    @DisplayName("getNotifications")
    inner class GetNotifications {

        @Test
        @DisplayName("returns Flow of domain notifications from DAO")
        fun returnsFlowFromDao() = runTest {
            every { mockNotificationDao.getAllNotifications(any()) } returns flowOf(
                listOf(testNotificationEntity, testNotificationEntity2)
            )

            repository.getNotifications().test {
                val notifications = awaitItem()
                assertEquals(2, notifications.size)
                assertEquals("notif-1", notifications[0].id)
                assertEquals("Meal Plan Ready", notifications[0].title)
                assertEquals("notif-2", notifications[1].id)
                assertEquals("Diwali Coming!", notifications[1].title)
                assertTrue(notifications[1].isRead)
                awaitComplete()
            }
        }

        @Test
        @DisplayName("returns empty list when DAO has no notifications")
        fun returnsEmptyListWhenNoNotifications() = runTest {
            every { mockNotificationDao.getAllNotifications(any()) } returns flowOf(emptyList())

            repository.getNotifications().test {
                val notifications = awaitItem()
                assertTrue(notifications.isEmpty())
                awaitComplete()
            }
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    inner class GetUnreadCount {

        @Test
        @DisplayName("returns correct unread count from DAO")
        fun returnsCorrectCount() = runTest {
            every { mockNotificationDao.getUnreadCount(any()) } returns flowOf(3)

            repository.getUnreadCount().test {
                val count = awaitItem()
                assertEquals(3, count)
                awaitComplete()
            }
        }

        @Test
        @DisplayName("returns zero when all are read")
        fun returnsZeroWhenAllRead() = runTest {
            every { mockNotificationDao.getUnreadCount(any()) } returns flowOf(0)

            repository.getUnreadCount().test {
                val count = awaitItem()
                assertEquals(0, count)
                awaitComplete()
            }
        }
    }

    @Nested
    @DisplayName("getNotificationsByType")
    inner class GetNotificationsByType {

        @Test
        @DisplayName("returns filtered notifications by type from DAO")
        fun returnsFilteredNotifications() = runTest {
            every {
                mockNotificationDao.getNotificationsByType("festival_reminder", any())
            } returns flowOf(listOf(testNotificationEntity2))

            repository.getNotificationsByType(
                com.rasoiai.domain.model.NotificationType.FESTIVAL_REMINDER
            ).test {
                val notifications = awaitItem()
                assertEquals(1, notifications.size)
                assertEquals("Diwali Coming!", notifications[0].title)
                awaitComplete()
            }
        }
    }

    @Nested
    @DisplayName("getNotificationById")
    inner class GetNotificationById {

        @Test
        @DisplayName("returns notification when exists")
        fun returnsNotificationWhenExists() = runTest {
            coEvery { mockNotificationDao.getNotificationById("notif-1") } returns testNotificationEntity

            val notification = repository.getNotificationById("notif-1")

            assertNotNull(notification)
            assertEquals("notif-1", notification!!.id)
            assertEquals("Meal Plan Ready", notification.title)
        }

        @Test
        @DisplayName("returns null when notification not found")
        fun returnsNullWhenNotFound() = runTest {
            coEvery { mockNotificationDao.getNotificationById("nonexistent") } returns null

            val notification = repository.getNotificationById("nonexistent")

            assertNull(notification)
        }
    }

    @Nested
    @DisplayName("refreshNotifications")
    inner class RefreshNotifications {

        @Test
        @DisplayName("fetches from API and saves to Room when online")
        fun fetchFromApiSavesToRoom() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.getNotifications() } returns testNotificationsResponse

            val result = repository.refreshNotifications()

            assertTrue(result.isSuccess)
            coVerify { mockApiService.getNotifications() }
            coVerify { mockNotificationDao.insertNotifications(any()) }
        }

        @Test
        @DisplayName("returns failure when offline")
        fun returnsFailureWhenOffline() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            val result = repository.refreshNotifications()

            assertTrue(result.isFailure)
            assertEquals("No network connection", result.exceptionOrNull()!!.message)
            coVerify(exactly = 0) { mockApiService.getNotifications() }
        }

        @Test
        @DisplayName("handles empty API response gracefully")
        fun handlesEmptyApiResponse() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.getNotifications() } returns emptyNotificationsResponse

            val result = repository.refreshNotifications()

            assertTrue(result.isSuccess)
            coVerify { mockNotificationDao.insertNotifications(emptyList()) }
        }

        @Test
        @DisplayName("returns failure on network timeout (IOException)")
        fun returnsFailureOnNetworkTimeout() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.getNotifications() } throws IOException("Connection timed out")

            val result = repository.refreshNotifications()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IOException)
        }

        @Test
        @DisplayName("returns failure on HTTP error")
        fun returnsFailureOnHttpError() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.getNotifications() } throws retrofit2.HttpException(
                retrofit2.Response.error<Any>(500, okhttp3.ResponseBody.create(null, ""))
            )

            val result = repository.refreshNotifications()

            assertTrue(result.isFailure)
        }
    }

    @Nested
    @DisplayName("markAsRead")
    inner class MarkAsRead {

        @Test
        @DisplayName("updates DAO and syncs to server when online")
        fun updatesLocalAndSyncsWhenOnline() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.markNotificationAsRead("notif-1") } returns SuccessResponse(
                success = true,
                message = "OK"
            )

            val result = repository.markAsRead("notif-1")

            assertTrue(result.isSuccess)
            coVerify { mockNotificationDao.markAsRead("notif-1") }
            coVerify { mockApiService.markNotificationAsRead("notif-1") }
            coVerify(exactly = 0) { mockOfflineQueueDao.insertAction(any()) }
        }

        @Test
        @DisplayName("updates DAO and queues sync when offline")
        fun updatesLocalAndQueuesSyncWhenOffline() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            val result = repository.markAsRead("notif-1")

            assertTrue(result.isSuccess)
            coVerify { mockNotificationDao.markAsRead("notif-1") }
            coVerify(exactly = 0) { mockApiService.markNotificationAsRead(any()) }
            coVerify { mockOfflineQueueDao.insertAction(any()) }
        }

        @Test
        @DisplayName("queues sync when API call throws IOException")
        fun queuesSyncOnNetworkError() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.markNotificationAsRead("notif-1") } throws IOException("Network error")

            val result = repository.markAsRead("notif-1")

            assertTrue(result.isSuccess)
            coVerify { mockNotificationDao.markAsRead("notif-1") }
            coVerify { mockOfflineQueueDao.insertAction(any()) }
        }

        @Test
        @DisplayName("queues sync when API call throws HttpException (server error)")
        fun queuesSyncOnHttpError() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            // Realistic API error — issue #34 narrowed broad catch so bare RuntimeException now propagates.
            coEvery { mockApiService.markNotificationAsRead("notif-1") } throws HttpException(
                Response.error<Any>(500, okhttp3.ResponseBody.create(null, ""))
            )

            val result = repository.markAsRead("notif-1")

            assertTrue(result.isSuccess)
            coVerify { mockNotificationDao.markAsRead("notif-1") }
            coVerify { mockOfflineQueueDao.insertAction(any()) }
        }

        @Test
        @DisplayName("queued action has correct action type payload")
        fun queuedActionHasCorrectPayload() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(false)
            val actionSlot = slot<OfflineQueueEntity>()
            coEvery { mockOfflineQueueDao.insertAction(capture(actionSlot)) } returns Unit

            repository.markAsRead("notif-1")

            val captured = actionSlot.captured
            assertEquals("mark_notification_read", captured.actionType)
            assertTrue(captured.payload.contains("notif-1"))
            assertEquals("pending", captured.status)
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    inner class MarkAllAsRead {

        @Test
        @DisplayName("updates all notifications in DAO and syncs when online")
        fun updatesAllAndSyncsWhenOnline() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.markAllNotificationsAsRead() } returns SuccessResponse(
                success = true,
                message = "OK"
            )

            val result = repository.markAllAsRead()

            assertTrue(result.isSuccess)
            coVerify { mockNotificationDao.markAllAsRead() }
            coVerify { mockApiService.markAllNotificationsAsRead() }
        }

        @Test
        @DisplayName("updates all locally when offline without error")
        fun updatesLocallyWhenOffline() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            val result = repository.markAllAsRead()

            assertTrue(result.isSuccess)
            coVerify { mockNotificationDao.markAllAsRead() }
            coVerify(exactly = 0) { mockApiService.markAllNotificationsAsRead() }
        }

        @Test
        @DisplayName("succeeds even if API sync fails")
        fun succeedsEvenIfApiSyncFails() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.markAllNotificationsAsRead() } throws IOException("Network error")

            val result = repository.markAllAsRead()

            assertTrue(result.isSuccess)
            coVerify { mockNotificationDao.markAllAsRead() }
        }
    }

    @Nested
    @DisplayName("deleteNotification")
    inner class DeleteNotification {

        @Test
        @DisplayName("removes from DAO and syncs to server when online")
        fun removesLocalAndSyncsWhenOnline() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.deleteNotification("notif-1") } returns SuccessResponse(
                success = true,
                message = "OK"
            )

            val result = repository.deleteNotification("notif-1")

            assertTrue(result.isSuccess)
            coVerify { mockNotificationDao.deleteNotification("notif-1") }
            coVerify { mockApiService.deleteNotification("notif-1") }
            coVerify(exactly = 0) { mockOfflineQueueDao.insertAction(any()) }
        }

        @Test
        @DisplayName("removes from DAO and queues sync when offline")
        fun removesLocalAndQueuesSyncWhenOffline() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            val result = repository.deleteNotification("notif-1")

            assertTrue(result.isSuccess)
            coVerify { mockNotificationDao.deleteNotification("notif-1") }
            coVerify(exactly = 0) { mockApiService.deleteNotification(any()) }
            coVerify { mockOfflineQueueDao.insertAction(any()) }
        }

        @Test
        @DisplayName("queues sync when API throws IOException")
        fun queuesSyncOnNetworkError() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.deleteNotification("notif-1") } throws IOException("Timeout")

            val result = repository.deleteNotification("notif-1")

            assertTrue(result.isSuccess)
            coVerify { mockNotificationDao.deleteNotification("notif-1") }
            coVerify { mockOfflineQueueDao.insertAction(any()) }
        }

        @Test
        @DisplayName("queued delete action has correct payload")
        fun queuedDeleteActionHasCorrectPayload() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(false)
            val actionSlot = slot<OfflineQueueEntity>()
            coEvery { mockOfflineQueueDao.insertAction(capture(actionSlot)) } returns Unit

            repository.deleteNotification("notif-1")

            val captured = actionSlot.captured
            assertEquals("delete_notification", captured.actionType)
            assertTrue(captured.payload.contains("notif-1"))
            assertEquals("pending", captured.status)
        }
    }

    @Nested
    @DisplayName("deleteAllNotifications")
    inner class DeleteAllNotifications {

        @Test
        @DisplayName("deletes all from DAO successfully")
        fun deletesAllSuccessfully() = runTest {
            val result = repository.deleteAllNotifications()

            assertTrue(result.isSuccess)
            coVerify { mockNotificationDao.deleteAllNotifications() }
        }

        @Test
        @DisplayName("returns failure when DAO throws SQLiteException")
        fun returnsFailureOnDaoError() = runTest {
            // Realistic DB error — issue #34 narrowed broad catch so bare RuntimeException now propagates.
            coEvery { mockNotificationDao.deleteAllNotifications() } throws SQLiteException("DB error")

            val result = repository.deleteAllNotifications()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }
    }

    @Nested
    @DisplayName("registerFcmToken")
    inner class RegisterFcmToken {

        @Test
        @DisplayName("calls API directly when online")
        fun callsApiWhenOnline() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.registerFcmToken(any()) } returns SuccessResponse(
                success = true,
                message = "OK"
            )

            val result = repository.registerFcmToken("test-fcm-token-123")

            assertTrue(result.isSuccess)
            coVerify { mockApiService.registerFcmToken(match { it.fcmToken == "test-fcm-token-123" }) }
            coVerify(exactly = 0) { mockOfflineQueueDao.insertAction(any()) }
        }

        @Test
        @DisplayName("queues action when offline")
        fun queuesActionWhenOffline() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            val result = repository.registerFcmToken("test-fcm-token-123")

            assertTrue(result.isSuccess)
            coVerify(exactly = 0) { mockApiService.registerFcmToken(any()) }
            coVerify { mockOfflineQueueDao.insertAction(any()) }
        }

        @Test
        @DisplayName("queues action when API call fails with IOException")
        fun queuesActionOnNetworkFailure() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.registerFcmToken(any()) } throws IOException("Connection refused")

            val result = repository.registerFcmToken("test-fcm-token-123")

            assertTrue(result.isSuccess)
            coVerify { mockOfflineQueueDao.insertAction(any()) }
        }

        @Test
        @DisplayName("queued FCM action has correct payload")
        fun queuedFcmActionHasCorrectPayload() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(false)
            val actionSlot = slot<OfflineQueueEntity>()
            coEvery { mockOfflineQueueDao.insertAction(capture(actionSlot)) } returns Unit

            repository.registerFcmToken("my-fcm-token-abc")

            val captured = actionSlot.captured
            assertEquals("register_fcm_token", captured.actionType)
            assertTrue(captured.payload.contains("my-fcm-token-abc"))
            assertEquals("pending", captured.status)
        }
    }

    @Nested
    @DisplayName("unregisterFcmToken")
    inner class UnregisterFcmToken {

        @Test
        @DisplayName("calls API directly when online")
        fun callsApiWhenOnline() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.unregisterFcmToken("test-token") } returns SuccessResponse(
                success = true,
                message = "OK"
            )

            val result = repository.unregisterFcmToken("test-token")

            assertTrue(result.isSuccess)
            coVerify { mockApiService.unregisterFcmToken("test-token") }
            coVerify(exactly = 0) { mockOfflineQueueDao.insertAction(any()) }
        }

        @Test
        @DisplayName("queues action when offline")
        fun queuesActionWhenOffline() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            val result = repository.unregisterFcmToken("test-token")

            assertTrue(result.isSuccess)
            coVerify(exactly = 0) { mockApiService.unregisterFcmToken(any()) }
            coVerify { mockOfflineQueueDao.insertAction(any()) }
        }
    }

    @Nested
    @DisplayName("cleanupNotifications")
    inner class CleanupNotifications {

        @Test
        @DisplayName("deletes expired and old read notifications")
        fun deletesExpiredAndOldRead() = runTest {
            repository.cleanupNotifications()

            coVerify { mockNotificationDao.deleteExpiredNotifications(any()) }
            coVerify { mockNotificationDao.deleteOldReadNotifications(any()) }
        }

        @Test
        @DisplayName("does not throw when DAO fails with SQLiteException")
        fun doesNotThrowOnDaoError() = runTest {
            // Realistic DB error — issue #34 narrowed broad catch so bare RuntimeException now propagates.
            coEvery { mockNotificationDao.deleteExpiredNotifications(any()) } throws SQLiteException("DB error")

            // Should not throw — cleanupNotifications catches SQLiteException internally (best-effort cleanup)
            repository.cleanupNotifications()
        }
    }

    @Nested
    @DisplayName("insertLocalNotification")
    inner class InsertLocalNotification {

        @Test
        @DisplayName("inserts notification entity to DAO")
        fun insertsToDao() = runTest {
            val notification = com.rasoiai.domain.model.Notification(
                id = "local-notif-1",
                type = com.rasoiai.domain.model.NotificationType.MEAL_PLAN_UPDATE,
                title = "Test Notification",
                body = "Test body",
                isRead = false,
                createdAt = 1700000000000L
            )

            repository.insertLocalNotification(notification)

            coVerify { mockNotificationDao.insertNotification(any()) }
        }
    }

    @Nested
    @DisplayName("processOfflineQueue")
    inner class ProcessOfflineQueue {

        @Test
        @DisplayName("returns failure when offline")
        fun returnsFailureWhenOffline() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            val result = repository.processOfflineQueue()

            assertTrue(result.isFailure)
            assertEquals("No network connection", result.exceptionOrNull()!!.message)
        }

        @Test
        @DisplayName("processes pending actions and returns count")
        fun processesPendingActionsSuccessfully() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            val pendingEntity = OfflineQueueEntity(
                id = "action-1",
                actionType = "mark_notification_read",
                payload = """{"notification_id": "notif-1"}""",
                status = "pending",
                retryCount = 0,
                errorMessage = null,
                createdAt = 1700000000000L,
                lastAttemptAt = null
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(pendingEntity)
            coEvery { mockApiService.markNotificationAsRead(any()) } returns SuccessResponse(
                success = true,
                message = "OK"
            )

            val result = repository.processOfflineQueue()

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull())
            coVerify { mockOfflineQueueDao.markInProgress(eq("action-1"), any()) }
            coVerify { mockApiService.markNotificationAsRead(any()) }
            coVerify { mockOfflineQueueDao.markCompleted(eq("action-1"), any()) }
            coVerify { mockOfflineQueueDao.deleteCompletedActions() }
        }

        @Test
        @DisplayName("marks action as failed on IOException")
        fun marksFailedOnNetworkError() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            val pendingEntity = OfflineQueueEntity(
                id = "action-2",
                actionType = "delete_notification",
                payload = """{"notification_id": "notif-2"}""",
                status = "pending",
                retryCount = 0,
                errorMessage = null,
                createdAt = 1700000000000L,
                lastAttemptAt = null
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(pendingEntity)
            coEvery { mockApiService.deleteNotification(any()) } throws IOException("Timeout")

            val result = repository.processOfflineQueue()

            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull())
            coVerify { mockOfflineQueueDao.markFailed(eq("action-2"), eq("Timeout"), any()) }
        }

        @Test
        @DisplayName("processes empty queue returning zero")
        fun processesEmptyQueue() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockOfflineQueueDao.getPendingActions() } returns emptyList()

            val result = repository.processOfflineQueue()

            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull())
            coVerify { mockOfflineQueueDao.deleteCompletedActions() }
        }
    }

    @Nested
    @DisplayName("triggerSync")
    inner class TriggerSync {

        @Test
        @DisplayName("processes queue and refreshes when online")
        fun processesAndRefreshesWhenOnline() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockOfflineQueueDao.getPendingActions() } returns emptyList()
            coEvery { mockApiService.getNotifications() } returns emptyNotificationsResponse

            repository.triggerSync()

            coVerify { mockOfflineQueueDao.getPendingActions() }
            coVerify { mockApiService.getNotifications() }
        }

        @Test
        @DisplayName("does nothing when offline")
        fun doesNothingWhenOffline() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            repository.triggerSync()

            coVerify(exactly = 0) { mockOfflineQueueDao.getPendingActions() }
            coVerify(exactly = 0) { mockApiService.getNotifications() }
        }
    }

    @Nested
    @DisplayName("getPendingActions")
    inner class GetPendingActions {

        @Test
        @DisplayName("returns Flow of pending actions from OfflineQueueDao")
        fun returnsPendingActionsFlow() = runTest {
            val entity = OfflineQueueEntity(
                id = "action-1",
                actionType = "mark_notification_read",
                payload = """{"notification_id": "notif-1"}""",
                status = "pending",
                retryCount = 0,
                errorMessage = null,
                createdAt = 1700000000000L,
                lastAttemptAt = null
            )
            every { mockOfflineQueueDao.observePendingActions() } returns flowOf(listOf(entity))

            repository.getPendingActions().test {
                val actions = awaitItem()
                assertEquals(1, actions.size)
                assertEquals("action-1", actions[0].id)
                awaitComplete()
            }
        }
    }

    @Nested
    @DisplayName("getPendingActionCount")
    inner class GetPendingActionCount {

        @Test
        @DisplayName("returns pending count from OfflineQueueDao")
        fun returnsPendingCount() = runTest {
            every { mockOfflineQueueDao.getPendingCount() } returns flowOf(5)

            repository.getPendingActionCount().test {
                val count = awaitItem()
                assertEquals(5, count)
                awaitComplete()
            }
        }
    }

    @Nested
    @DisplayName("CancellationException propagation (structured concurrency)")
    inner class CancellationPropagation {

        @Test
        @DisplayName("deleteAllNotifications should propagate CancellationException instead of wrapping in Result.failure")
        fun `deleteAllNotifications should propagate CancellationException`() = runTest {
            coEvery { mockNotificationDao.deleteAllNotifications() } throws CancellationException("cancelled")
            try {
                repository.deleteAllNotifications()
                fail("Expected CancellationException to propagate, got Result wrapper instead")
            } catch (e: CancellationException) {
                assertEquals("cancelled", e.message)
            }
        }
    }

    @Nested
    @DisplayName("Unexpected exception propagation (issue #34)")
    inner class UnexpectedExceptionPropagation {

        private fun http500() = HttpException(
            Response.error<Any>(500, okhttp3.ResponseBody.create(null, ""))
        )

        // ---- markAsRead ----

        @Test
        @DisplayName("markAsRead propagates IllegalStateException instead of wrapping")
        fun `markAsRead propagates IllegalStateException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.markNotificationAsRead("notif-1") } throws IllegalStateException("unexpected")
            try {
                repository.markAsRead("notif-1")
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        @Test
        @DisplayName("markAsRead wraps SQLiteException (DAO write failure) in Result.failure")
        fun `markAsRead wraps SQLiteException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockNotificationDao.markAsRead("notif-1") } throws SQLiteException("disk full")

            val result = repository.markAsRead("notif-1")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }

        @Test
        @DisplayName("markAsRead queues on HttpException from API (inner catch)")
        fun `markAsRead queues on HttpException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.markNotificationAsRead("notif-1") } throws http500()

            val result = repository.markAsRead("notif-1")

            assertTrue(result.isSuccess)
            coVerify { mockOfflineQueueDao.insertAction(any()) }
        }

        // ---- markAllAsRead ----

        @Test
        @DisplayName("markAllAsRead propagates IllegalStateException instead of wrapping")
        fun `markAllAsRead propagates IllegalStateException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.markAllNotificationsAsRead() } throws IllegalStateException("unexpected")
            try {
                repository.markAllAsRead()
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        @Test
        @DisplayName("markAllAsRead succeeds on HttpException from API (inner catch swallows)")
        fun `markAllAsRead swallows HttpException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.markAllNotificationsAsRead() } throws http500()

            val result = repository.markAllAsRead()

            assertTrue(result.isSuccess)
            coVerify { mockNotificationDao.markAllAsRead() }
        }

        @Test
        @DisplayName("markAllAsRead wraps SQLiteException (DAO write failure) in Result.failure")
        fun `markAllAsRead wraps SQLiteException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(false)
            coEvery { mockNotificationDao.markAllAsRead() } throws SQLiteException("disk full")

            val result = repository.markAllAsRead()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }

        // ---- deleteNotification ----

        @Test
        @DisplayName("deleteNotification propagates IllegalStateException instead of wrapping")
        fun `deleteNotification propagates IllegalStateException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.deleteNotification("notif-1") } throws IllegalStateException("unexpected")
            try {
                repository.deleteNotification("notif-1")
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        @Test
        @DisplayName("deleteNotification queues on HttpException from API (inner catch)")
        fun `deleteNotification queues on HttpException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.deleteNotification("notif-1") } throws http500()

            val result = repository.deleteNotification("notif-1")

            assertTrue(result.isSuccess)
            coVerify { mockOfflineQueueDao.insertAction(any()) }
        }

        // ---- deleteAllNotifications ----

        @Test
        @DisplayName("deleteAllNotifications propagates IllegalStateException instead of wrapping")
        fun `deleteAllNotifications propagates IllegalStateException`() = runTest {
            coEvery { mockNotificationDao.deleteAllNotifications() } throws IllegalStateException("unexpected")
            try {
                repository.deleteAllNotifications()
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        // ---- refreshNotifications ----

        @Test
        @DisplayName("refreshNotifications propagates IllegalStateException instead of wrapping")
        fun `refreshNotifications propagates IllegalStateException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.getNotifications() } throws IllegalStateException("unexpected")
            try {
                repository.refreshNotifications()
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        @Test
        @DisplayName("refreshNotifications wraps SQLiteException (insert failure) in Result.failure")
        fun `refreshNotifications wraps SQLiteException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.getNotifications() } returns emptyNotificationsResponse
            coEvery { mockNotificationDao.insertNotifications(any()) } throws SQLiteException("disk full")

            val result = repository.refreshNotifications()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }

        // ---- cleanupNotifications ----

        @Test
        @DisplayName("cleanupNotifications propagates IllegalStateException instead of swallowing")
        fun `cleanupNotifications propagates IllegalStateException`() = runTest {
            coEvery { mockNotificationDao.deleteExpiredNotifications(any()) } throws IllegalStateException("unexpected")
            try {
                repository.cleanupNotifications()
                fail("Expected IllegalStateException to propagate, got silent swallow instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        // ---- registerFcmToken ----

        @Test
        @DisplayName("registerFcmToken propagates IllegalStateException instead of wrapping")
        fun `registerFcmToken propagates IllegalStateException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.registerFcmToken(any()) } throws IllegalStateException("unexpected")
            try {
                repository.registerFcmToken("fcm-token-xyz")
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        @Test
        @DisplayName("registerFcmToken queues on HttpException from API (inner catch)")
        fun `registerFcmToken queues on HttpException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.registerFcmToken(any()) } throws http500()

            val result = repository.registerFcmToken("fcm-token-xyz")

            assertTrue(result.isSuccess)
            coVerify { mockOfflineQueueDao.insertAction(any()) }
        }

        // ---- unregisterFcmToken ----

        @Test
        @DisplayName("unregisterFcmToken propagates IllegalStateException instead of wrapping")
        fun `unregisterFcmToken propagates IllegalStateException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.unregisterFcmToken(any()) } throws IllegalStateException("unexpected")
            try {
                repository.unregisterFcmToken("fcm-token-xyz")
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        // ---- processOfflineQueue ----

        @Test
        @DisplayName("processOfflineQueue propagates IllegalStateException from per-action API call")
        fun `processOfflineQueue propagates IllegalStateException from api`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            val pendingEntity = OfflineQueueEntity(
                id = "action-99",
                actionType = "mark_notification_read",
                payload = """{"notification_id": "notif-x"}""",
                status = "pending",
                retryCount = 0,
                errorMessage = null,
                createdAt = 1700000000000L,
                lastAttemptAt = null
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(pendingEntity)
            coEvery { mockApiService.markNotificationAsRead(any()) } throws IllegalStateException("unexpected")
            try {
                repository.processOfflineQueue()
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        @Test
        @DisplayName("processOfflineQueue propagates IllegalStateException from DAO read")
        fun `processOfflineQueue propagates IllegalStateException from dao`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockOfflineQueueDao.getPendingActions() } throws IllegalStateException("unexpected")
            try {
                repository.processOfflineQueue()
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        @Test
        @DisplayName("processOfflineQueue wraps SQLiteException (DAO read failure) in Result.failure")
        fun `processOfflineQueue wraps SQLiteException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockOfflineQueueDao.getPendingActions() } throws SQLiteException("disk full")

            val result = repository.processOfflineQueue()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }

        @Test
        @DisplayName("processOfflineQueue marks action failed on HttpException (per-action inner catch)")
        fun `processOfflineQueue marks failed on HttpException`() = runTest {
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            val pendingEntity = OfflineQueueEntity(
                id = "action-http",
                actionType = "delete_notification",
                payload = """{"notification_id": "notif-y"}""",
                status = "pending",
                retryCount = 0,
                errorMessage = null,
                createdAt = 1700000000000L,
                lastAttemptAt = null
            )
            coEvery { mockOfflineQueueDao.getPendingActions() } returns listOf(pendingEntity)
            coEvery { mockApiService.deleteNotification(any()) } throws http500()

            val result = repository.processOfflineQueue()

            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull())
            coVerify { mockOfflineQueueDao.markFailed(eq("action-http"), any(), any()) }
        }

        // ---- insertLocalNotification ----

        @Test
        @DisplayName("insertLocalNotification propagates IllegalStateException instead of swallowing")
        fun `insertLocalNotification propagates IllegalStateException`() = runTest {
            coEvery { mockNotificationDao.insertNotification(any()) } throws IllegalStateException("unexpected")
            val notification = com.rasoiai.domain.model.Notification(
                id = "local-notif-err",
                type = com.rasoiai.domain.model.NotificationType.MEAL_PLAN_UPDATE,
                title = "Test",
                body = "Test body",
                isRead = false,
                createdAt = 1700000000000L
            )
            try {
                repository.insertLocalNotification(notification)
                fail("Expected IllegalStateException to propagate, got silent swallow instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        @Test
        @DisplayName("insertLocalNotification swallows SQLiteException (best-effort local insert)")
        fun `insertLocalNotification swallows SQLiteException`() = runTest {
            coEvery { mockNotificationDao.insertNotification(any()) } throws SQLiteException("disk full")
            val notification = com.rasoiai.domain.model.Notification(
                id = "local-notif-err",
                type = com.rasoiai.domain.model.NotificationType.MEAL_PLAN_UPDATE,
                title = "Test",
                body = "Test body",
                isRead = false,
                createdAt = 1700000000000L
            )

            // Should not throw — SQLiteException is the expected failure mode for best-effort local insert.
            repository.insertLocalNotification(notification)
        }

        // ---- queueAction (retained broad catch — side-effect helper) ----

        @Test
        @DisplayName("queueAction swallows SQLiteException (retained broad catch; best-effort queue insert)")
        fun `queueAction swallows SQLiteException`() = runTest {
            coEvery { mockOfflineQueueDao.insertAction(any()) } throws SQLiteException("disk full")
            val action = com.rasoiai.domain.model.OfflineAction(
                id = "a-1",
                actionType = com.rasoiai.domain.model.OfflineActionType.MARK_NOTIFICATION_READ,
                payload = """{"notification_id": "notif-z"}""",
                status = com.rasoiai.domain.model.ActionStatus.PENDING,
                createdAt = 1700000000000L
            )

            // Should not throw — queueAction is a fire-and-forget side-effect helper that must not
            // invalidate the caller's already-completed work.
            repository.queueAction(action)
        }
    }
}
