package com.rasoiai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for offline action queue.
 *
 * Stores actions performed offline that need to be synced when connectivity is restored.
 * Used by SyncWorker to process pending operations.
 *
 * Action types:
 * - swap_meal: Swap a meal item
 * - lock_meal: Lock/unlock a meal item
 * - remove_meal: Remove a meal from the plan
 * - toggle_favorite: Add/remove recipe from favorites
 * - update_grocery: Update grocery item status
 * - mark_notification_read: Mark notification as read
 * - delete_notification: Delete a notification
 * - register_fcm_token: Register FCM token with backend
 */
@Entity(
    tableName = "offline_queue",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["actionType"])
    ]
)
data class OfflineQueueEntity(
    @PrimaryKey
    val id: String,

    /** Type of action to perform */
    val actionType: String,

    /** JSON payload containing action parameters */
    val payload: String,

    /** Status: pending, in_progress, completed, failed */
    val status: String = "pending",

    /** Number of sync attempts */
    val retryCount: Int = 0,

    /** Error message from last failed attempt */
    val errorMessage: String? = null,

    /** Timestamp when action was queued */
    val createdAt: Long,

    /** Timestamp of last sync attempt */
    val lastAttemptAt: Long? = null
)
