package com.rasoiai.data.repository

import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.dao.NotificationDao
import com.rasoiai.data.local.dao.OfflineQueueDao
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.FcmTokenRequest
import com.rasoiai.domain.model.ActionStatus
import com.rasoiai.domain.model.Notification
import com.rasoiai.domain.model.NotificationType
import com.rasoiai.domain.model.OfflineAction
import com.rasoiai.domain.model.OfflineActionType
import com.rasoiai.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NotificationRepository with offline-first architecture.
 *
 * Strategy:
 * - Local Room database is the single source of truth
 * - Operations are performed locally immediately
 * - Changes are synced to server when online (or queued if offline)
 * - SyncWorker processes the offline queue periodically
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val offlineQueueDao: OfflineQueueDao,
    private val apiService: RasoiApiService,
    private val networkMonitor: NetworkMonitor
) : NotificationRepository {

    // region Notification Operations

    override fun getNotifications(): Flow<List<Notification>> {
        return notificationDao.getAllNotifications().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getUnreadCount(): Flow<Int> {
        return notificationDao.getUnreadCount()
    }

    override fun getNotificationsByType(type: NotificationType): Flow<List<Notification>> {
        return notificationDao.getNotificationsByType(type.value).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getNotificationById(id: String): Notification? {
        return notificationDao.getNotificationById(id)?.toDomain()
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            // Update locally immediately
            notificationDao.markAsRead(notificationId)
            Timber.d("Notification $notificationId marked as read locally")

            // Sync to server if online, otherwise queue
            if (networkMonitor.isOnline.first()) {
                try {
                    apiService.markNotificationAsRead(notificationId)
                    Timber.d("Notification $notificationId marked as read on server")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to sync mark-as-read to server, will retry later")
                    queueAction(createMarkReadAction(notificationId))
                }
            } else {
                queueAction(createMarkReadAction(notificationId))
                Timber.d("Offline - queued mark-as-read action for $notificationId")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark notification as read: $notificationId")
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(): Result<Unit> {
        return try {
            // Update locally immediately
            notificationDao.markAllAsRead()
            Timber.d("All notifications marked as read locally")

            // Sync to server if online
            if (networkMonitor.isOnline.first()) {
                try {
                    apiService.markAllNotificationsAsRead()
                    Timber.d("All notifications marked as read on server")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to sync mark-all-read to server")
                    // We don't queue this action since local state is already correct
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark all notifications as read")
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            // Delete locally immediately
            notificationDao.deleteNotification(notificationId)
            Timber.d("Notification $notificationId deleted locally")

            // Sync to server if online, otherwise queue
            if (networkMonitor.isOnline.first()) {
                try {
                    apiService.deleteNotification(notificationId)
                    Timber.d("Notification $notificationId deleted on server")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to sync delete to server, will retry later")
                    queueAction(createDeleteAction(notificationId))
                }
            } else {
                queueAction(createDeleteAction(notificationId))
                Timber.d("Offline - queued delete action for $notificationId")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete notification: $notificationId")
            Result.failure(e)
        }
    }

    override suspend fun deleteAllNotifications(): Result<Unit> {
        return try {
            notificationDao.deleteAllNotifications()
            Timber.d("All notifications deleted locally")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete all notifications")
            Result.failure(e)
        }
    }

    override suspend fun refreshNotifications(): Result<Unit> {
        return try {
            if (!networkMonitor.isOnline.first()) {
                Timber.d("Offline - cannot refresh notifications")
                return Result.failure(Exception("No network connection"))
            }

            val response = apiService.getNotifications()
            val entities = response.notifications.map { it.toEntity() }

            // Insert all notifications (replace existing)
            notificationDao.insertNotifications(entities)
            Timber.i("Refreshed ${entities.size} notifications from server")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh notifications")
            Result.failure(e)
        }
    }

    override suspend fun cleanupNotifications() {
        try {
            // Delete expired notifications
            notificationDao.deleteExpiredNotifications()

            // Delete read notifications older than 30 days
            val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            notificationDao.deleteOldReadNotifications(thirtyDaysAgo)

            Timber.d("Notification cleanup completed")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup notifications")
        }
    }

    // endregion

    // region FCM Token Operations

    override suspend fun registerFcmToken(token: String): Result<Unit> {
        return try {
            if (networkMonitor.isOnline.first()) {
                try {
                    apiService.registerFcmToken(FcmTokenRequest(token))
                    Timber.d("FCM token registered on server")
                    return Result.success(Unit)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to register FCM token on server, will retry later")
                    queueAction(createRegisterFcmTokenAction(token))
                }
            } else {
                queueAction(createRegisterFcmTokenAction(token))
                Timber.d("Offline - queued FCM token registration")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to register FCM token")
            Result.failure(e)
        }
    }

    override suspend fun unregisterFcmToken(token: String): Result<Unit> {
        return try {
            if (networkMonitor.isOnline.first()) {
                try {
                    apiService.unregisterFcmToken(token)
                    Timber.d("FCM token unregistered from server")
                    return Result.success(Unit)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to unregister FCM token from server")
                    queueAction(createUnregisterFcmTokenAction(token))
                }
            } else {
                queueAction(createUnregisterFcmTokenAction(token))
                Timber.d("Offline - queued FCM token unregistration")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister FCM token")
            Result.failure(e)
        }
    }

    // endregion

    // region Offline Queue Operations

    override fun getPendingActions(): Flow<List<OfflineAction>> {
        return offlineQueueDao.observePendingActions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPendingActionCount(): Flow<Int> {
        return offlineQueueDao.getPendingCount()
    }

    override suspend fun queueAction(action: OfflineAction) {
        try {
            offlineQueueDao.insertAction(action.toEntity())
            Timber.d("Queued action: ${action.actionType.value} (${action.id})")
        } catch (e: Exception) {
            Timber.e(e, "Failed to queue action: ${action.actionType.value}")
        }
    }

    override suspend fun processOfflineQueue(): Result<Int> {
        var processedCount = 0

        return try {
            if (!networkMonitor.isOnline.first()) {
                Timber.d("Offline - cannot process queue")
                return Result.failure(Exception("No network connection"))
            }

            val pendingActions = offlineQueueDao.getPendingActions()
            Timber.d("Processing ${pendingActions.size} pending actions")

            for (entity in pendingActions) {
                val action = entity.toDomain()

                try {
                    offlineQueueDao.markInProgress(action.id)

                    when (action.actionType) {
                        OfflineActionType.MARK_NOTIFICATION_READ -> {
                            val notificationId = extractNotificationId(action.payload)
                            apiService.markNotificationAsRead(notificationId)
                        }

                        OfflineActionType.DELETE_NOTIFICATION -> {
                            val notificationId = extractNotificationId(action.payload)
                            apiService.deleteNotification(notificationId)
                        }

                        OfflineActionType.REGISTER_FCM_TOKEN -> {
                            val token = extractFcmToken(action.payload)
                            apiService.registerFcmToken(FcmTokenRequest(token))
                        }

                        OfflineActionType.UNREGISTER_FCM_TOKEN -> {
                            val token = extractFcmToken(action.payload)
                            apiService.unregisterFcmToken(token)
                        }

                        else -> {
                            Timber.w("Unknown action type: ${action.actionType}")
                        }
                    }

                    offlineQueueDao.markCompleted(action.id)
                    processedCount++
                    Timber.d("Completed action: ${action.actionType.value} (${action.id})")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to process action: ${action.actionType.value} (${action.id})")
                    offlineQueueDao.markFailed(action.id, e.message ?: "Unknown error")

                    // If max retries reached, keep it as failed
                    if (action.retryCount >= OfflineAction.MAX_RETRIES - 1) {
                        Timber.w("Action ${action.id} has reached max retries")
                    }
                }
            }

            // Clean up completed actions
            offlineQueueDao.deleteCompletedActions()

            Timber.i("Processed $processedCount/${pendingActions.size} actions")
            Result.success(processedCount)
        } catch (e: Exception) {
            Timber.e(e, "Failed to process offline queue")
            Result.failure(e)
        }
    }

    override suspend fun triggerSync() {
        if (networkMonitor.isOnline.first()) {
            processOfflineQueue()
            refreshNotifications()
        }
    }

    // endregion

    // region Local Notification Operations

    override suspend fun insertLocalNotification(notification: Notification) {
        try {
            notificationDao.insertNotification(notification.toEntity())
            Timber.d("Inserted local notification: ${notification.id}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to insert local notification")
        }
    }

    // endregion

    // region Helper Functions

    private fun createMarkReadAction(notificationId: String): OfflineAction {
        return OfflineAction(
            id = UUID.randomUUID().toString(),
            actionType = OfflineActionType.MARK_NOTIFICATION_READ,
            payload = """{"notification_id": "$notificationId"}""",
            status = ActionStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createDeleteAction(notificationId: String): OfflineAction {
        return OfflineAction(
            id = UUID.randomUUID().toString(),
            actionType = OfflineActionType.DELETE_NOTIFICATION,
            payload = """{"notification_id": "$notificationId"}""",
            status = ActionStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createRegisterFcmTokenAction(token: String): OfflineAction {
        return OfflineAction(
            id = UUID.randomUUID().toString(),
            actionType = OfflineActionType.REGISTER_FCM_TOKEN,
            payload = """{"fcm_token": "$token"}""",
            status = ActionStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createUnregisterFcmTokenAction(token: String): OfflineAction {
        return OfflineAction(
            id = UUID.randomUUID().toString(),
            actionType = OfflineActionType.UNREGISTER_FCM_TOKEN,
            payload = """{"fcm_token": "$token"}""",
            status = ActionStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun extractNotificationId(payload: String): String {
        // Simple JSON parsing - in production, use Gson
        return payload.substringAfter("\"notification_id\":")
            .substringAfter("\"")
            .substringBefore("\"")
    }

    private fun extractFcmToken(payload: String): String {
        return payload.substringAfter("\"fcm_token\":")
            .substringAfter("\"")
            .substringBefore("\"")
    }

    // endregion
}
