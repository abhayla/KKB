package com.rasoiai.domain.repository

import com.rasoiai.domain.model.Notification
import com.rasoiai.domain.model.NotificationType
import com.rasoiai.domain.model.OfflineAction
import kotlinx.coroutines.flow.Flow

/**
 * Repository for notification operations.
 * Follows offline-first architecture with local Room database as source of truth.
 */
interface NotificationRepository {

    // region Notification Operations

    /**
     * Get all notifications as a flow (excludes expired notifications).
     */
    fun getNotifications(): Flow<List<Notification>>

    /**
     * Get unread notification count as a flow.
     */
    fun getUnreadCount(): Flow<Int>

    /**
     * Get notifications filtered by type.
     */
    fun getNotificationsByType(type: NotificationType): Flow<List<Notification>>

    /**
     * Get a single notification by ID.
     */
    suspend fun getNotificationById(id: String): Notification?

    /**
     * Mark a notification as read.
     * Queues for sync if offline.
     */
    suspend fun markAsRead(notificationId: String): Result<Unit>

    /**
     * Mark all notifications as read.
     * Queues for sync if offline.
     */
    suspend fun markAllAsRead(): Result<Unit>

    /**
     * Delete a notification.
     * Queues for sync if offline.
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit>

    /**
     * Delete all notifications locally.
     */
    suspend fun deleteAllNotifications(): Result<Unit>

    /**
     * Refresh notifications from server.
     */
    suspend fun refreshNotifications(): Result<Unit>

    /**
     * Delete expired and old read notifications (cleanup).
     */
    suspend fun cleanupNotifications()

    // endregion

    // region FCM Token Operations

    /**
     * Register FCM token with the backend.
     * Queues for sync if offline.
     */
    suspend fun registerFcmToken(token: String): Result<Unit>

    /**
     * Unregister FCM token from the backend.
     * Queues for sync if offline.
     */
    suspend fun unregisterFcmToken(token: String): Result<Unit>

    // endregion

    // region Offline Queue Operations

    /**
     * Get pending offline actions as a flow.
     */
    fun getPendingActions(): Flow<List<OfflineAction>>

    /**
     * Get count of pending offline actions.
     */
    fun getPendingActionCount(): Flow<Int>

    /**
     * Queue an action for offline sync.
     */
    suspend fun queueAction(action: OfflineAction)

    /**
     * Process all pending offline actions.
     * Called by SyncWorker when network is available.
     */
    suspend fun processOfflineQueue(): Result<Int>

    /**
     * Trigger immediate sync if online.
     */
    suspend fun triggerSync()

    // endregion

    // region Local Notification Operations

    /**
     * Insert a local notification (from FCM or local generation).
     */
    suspend fun insertLocalNotification(notification: Notification)

    // endregion
}
