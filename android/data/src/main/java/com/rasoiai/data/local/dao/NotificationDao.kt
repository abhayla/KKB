package com.rasoiai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rasoiai.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for notification operations.
 */
@Dao
interface NotificationDao {

    /**
     * Get all notifications ordered by creation date (newest first).
     * Excludes expired notifications.
     */
    @Query("""
        SELECT * FROM notifications
        WHERE expiresAt IS NULL OR expiresAt > :currentTime
        ORDER BY createdAt DESC
    """)
    fun getAllNotifications(currentTime: Long = System.currentTimeMillis()): Flow<List<NotificationEntity>>

    /**
     * Get unread notifications count.
     */
    @Query("""
        SELECT COUNT(*) FROM notifications
        WHERE isRead = 0 AND (expiresAt IS NULL OR expiresAt > :currentTime)
    """)
    fun getUnreadCount(currentTime: Long = System.currentTimeMillis()): Flow<Int>

    /**
     * Get notifications by type.
     */
    @Query("""
        SELECT * FROM notifications
        WHERE type = :type AND (expiresAt IS NULL OR expiresAt > :currentTime)
        ORDER BY createdAt DESC
    """)
    fun getNotificationsByType(type: String, currentTime: Long = System.currentTimeMillis()): Flow<List<NotificationEntity>>

    /**
     * Get a single notification by ID.
     */
    @Query("SELECT * FROM notifications WHERE id = :notificationId")
    suspend fun getNotificationById(notificationId: String): NotificationEntity?

    /**
     * Insert a notification (replace if exists).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    /**
     * Insert multiple notifications.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    /**
     * Update a notification.
     */
    @Update
    suspend fun updateNotification(notification: NotificationEntity)

    /**
     * Mark a notification as read.
     */
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: String)

    /**
     * Mark all notifications as read.
     */
    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    /**
     * Delete a notification.
     */
    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: String)

    /**
     * Delete all notifications.
     */
    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()

    /**
     * Delete expired notifications.
     */
    @Query("DELETE FROM notifications WHERE expiresAt IS NOT NULL AND expiresAt <= :currentTime")
    suspend fun deleteExpiredNotifications(currentTime: Long = System.currentTimeMillis())

    /**
     * Delete old read notifications (older than specified timestamp).
     */
    @Query("DELETE FROM notifications WHERE isRead = 1 AND createdAt < :olderThan")
    suspend fun deleteOldReadNotifications(olderThan: Long)

    /**
     * Get total notification count.
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE expiresAt IS NULL OR expiresAt > :currentTime")
    suspend fun getNotificationCount(currentTime: Long = System.currentTimeMillis()): Int
}
