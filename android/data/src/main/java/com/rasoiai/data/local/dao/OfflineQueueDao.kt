package com.rasoiai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rasoiai.data.local.entity.OfflineQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for offline action queue operations.
 */
@Dao
interface OfflineQueueDao {

    /**
     * Get all pending actions ordered by creation time.
     */
    @Query("SELECT * FROM offline_queue WHERE status = 'pending' ORDER BY createdAt ASC")
    suspend fun getPendingActions(): List<OfflineQueueEntity>

    /**
     * Get pending actions as a Flow for observation.
     */
    @Query("SELECT * FROM offline_queue WHERE status = 'pending' ORDER BY createdAt ASC")
    fun observePendingActions(): Flow<List<OfflineQueueEntity>>

    /**
     * Get actions by status.
     */
    @Query("SELECT * FROM offline_queue WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getActionsByStatus(status: String): List<OfflineQueueEntity>

    /**
     * Get an action by ID.
     */
    @Query("SELECT * FROM offline_queue WHERE id = :actionId")
    suspend fun getActionById(actionId: String): OfflineQueueEntity?

    /**
     * Get pending count.
     */
    @Query("SELECT COUNT(*) FROM offline_queue WHERE status = 'pending'")
    fun getPendingCount(): Flow<Int>

    /**
     * Insert an action.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: OfflineQueueEntity)

    /**
     * Insert multiple actions.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActions(actions: List<OfflineQueueEntity>)

    /**
     * Update an action.
     */
    @Update
    suspend fun updateAction(action: OfflineQueueEntity)

    /**
     * Update action status.
     */
    @Query("""
        UPDATE offline_queue
        SET status = :status, lastAttemptAt = :lastAttemptAt
        WHERE id = :actionId
    """)
    suspend fun updateStatus(actionId: String, status: String, lastAttemptAt: Long)

    /**
     * Update action status with error.
     */
    @Query("""
        UPDATE offline_queue
        SET status = :status, errorMessage = :errorMessage,
            retryCount = retryCount + 1, lastAttemptAt = :lastAttemptAt
        WHERE id = :actionId
    """)
    suspend fun updateStatusWithError(
        actionId: String,
        status: String,
        errorMessage: String,
        lastAttemptAt: Long
    )

    /**
     * Mark action as in progress.
     */
    @Query("""
        UPDATE offline_queue
        SET status = 'in_progress', lastAttemptAt = :timestamp
        WHERE id = :actionId
    """)
    suspend fun markInProgress(actionId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark action as completed.
     */
    @Query("""
        UPDATE offline_queue
        SET status = 'completed', lastAttemptAt = :timestamp
        WHERE id = :actionId
    """)
    suspend fun markCompleted(actionId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark action as failed.
     */
    @Query("""
        UPDATE offline_queue
        SET status = 'failed', errorMessage = :errorMessage,
            retryCount = retryCount + 1, lastAttemptAt = :timestamp
        WHERE id = :actionId
    """)
    suspend fun markFailed(
        actionId: String,
        errorMessage: String,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Reset failed actions to pending for retry.
     */
    @Query("""
        UPDATE offline_queue
        SET status = 'pending', errorMessage = NULL
        WHERE status = 'failed' AND retryCount < :maxRetries
    """)
    suspend fun resetFailedForRetry(maxRetries: Int = 3)

    /**
     * Delete an action.
     */
    @Query("DELETE FROM offline_queue WHERE id = :actionId")
    suspend fun deleteAction(actionId: String)

    /**
     * Delete completed actions.
     */
    @Query("DELETE FROM offline_queue WHERE status = 'completed'")
    suspend fun deleteCompletedActions()

    /**
     * Delete all actions.
     */
    @Query("DELETE FROM offline_queue")
    suspend fun deleteAllActions()

    /**
     * Delete old completed actions (older than specified timestamp).
     */
    @Query("DELETE FROM offline_queue WHERE status = 'completed' AND lastAttemptAt < :olderThan")
    suspend fun deleteOldCompletedActions(olderThan: Long)

    /**
     * Get total action count by status.
     */
    @Query("SELECT COUNT(*) FROM offline_queue WHERE status = :status")
    suspend fun getCountByStatus(status: String): Int
}
