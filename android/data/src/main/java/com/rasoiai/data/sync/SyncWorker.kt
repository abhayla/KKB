package com.rasoiai.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rasoiai.data.local.dao.OfflineQueueDao
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.FcmTokenRequest
import com.rasoiai.data.remote.dto.SwapMealRequest
import com.rasoiai.domain.model.OfflineAction
import com.rasoiai.domain.model.OfflineActionType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Background worker for syncing offline data with the server.
 * Handles:
 * - Processing queued offline actions
 * - Syncing notifications
 * - Retrying failed operations
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineQueueDao: OfflineQueueDao,
    private val apiService: RasoiApiService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker started")

        return try {
            var totalProcessed = 0
            var totalFailed = 0

            // Process offline queue
            val queueResult = processOfflineQueue()
            totalProcessed += queueResult.first
            totalFailed += queueResult.second

            // Clean up completed actions
            offlineQueueDao.deleteCompletedActions()

            // Delete old completed actions (older than 7 days)
            val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            offlineQueueDao.deleteOldCompletedActions(sevenDaysAgo)

            Timber.d("SyncWorker completed: $totalProcessed processed, $totalFailed failed")

            if (totalFailed > 0 && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker failed")
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Process all pending offline actions.
     * Returns a pair of (processed count, failed count).
     */
    private suspend fun processOfflineQueue(): Pair<Int, Int> {
        var processedCount = 0
        var failedCount = 0

        val pendingActions = offlineQueueDao.getPendingActions()
        Timber.d("Processing ${pendingActions.size} pending actions")

        for (entity in pendingActions) {
            val action = entity.toDomain()

            try {
                offlineQueueDao.markInProgress(action.id)

                val success = processAction(action)

                if (success) {
                    offlineQueueDao.markCompleted(action.id)
                    processedCount++
                    Timber.d("Completed action: ${action.actionType.value} (${action.id})")
                } else {
                    offlineQueueDao.markFailed(action.id, "Action returned false")
                    failedCount++
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to process action: ${action.actionType.value} (${action.id})")
                offlineQueueDao.markFailed(action.id, e.message ?: "Unknown error")
                failedCount++

                // If max retries reached, log a warning
                if (action.retryCount >= OfflineAction.MAX_RETRIES - 1) {
                    Timber.w("Action ${action.id} has reached max retries, will not be retried")
                }
            }
        }

        return Pair(processedCount, failedCount)
    }

    /**
     * Process a single offline action.
     * Returns true if successful, false otherwise.
     */
    private suspend fun processAction(action: OfflineAction): Boolean {
        return when (action.actionType) {
            OfflineActionType.MARK_NOTIFICATION_READ -> {
                val notificationId = extractNotificationId(action.payload)
                apiService.markNotificationAsRead(notificationId)
                true
            }

            OfflineActionType.DELETE_NOTIFICATION -> {
                val notificationId = extractNotificationId(action.payload)
                apiService.deleteNotification(notificationId)
                true
            }

            OfflineActionType.REGISTER_FCM_TOKEN -> {
                val token = extractFcmToken(action.payload)
                apiService.registerFcmToken(FcmTokenRequest(token))
                true
            }

            OfflineActionType.UNREGISTER_FCM_TOKEN -> {
                val token = extractFcmToken(action.payload)
                apiService.unregisterFcmToken(token)
                true
            }

            OfflineActionType.SWAP_MEAL -> {
                val (planId, itemId) = extractMealIds(action.payload)
                apiService.swapMealItem(planId, itemId, SwapMealRequest())
                Timber.d("SWAP_MEAL: Synced swap for plan=$planId, item=$itemId")
                true
            }

            OfflineActionType.LOCK_MEAL -> {
                val (planId, itemId) = extractMealIds(action.payload)
                apiService.lockMealItem(planId, itemId)
                Timber.d("LOCK_MEAL: Synced lock for plan=$planId, item=$itemId")
                true
            }

            OfflineActionType.REMOVE_MEAL -> {
                val (planId, itemId) = extractMealIds(action.payload)
                apiService.removeMealItem(planId, itemId)
                Timber.d("REMOVE_MEAL: Synced remove for plan=$planId, item=$itemId")
                true
            }

            OfflineActionType.TOGGLE_FAVORITE -> {
                // Favorites are local-only (no backend API)
                // Action already synced to local DB, mark as completed
                Timber.d("TOGGLE_FAVORITE: Local-only operation, marking complete")
                true
            }

            OfflineActionType.UPDATE_GROCERY -> {
                // Grocery updates are local-only (no backend API)
                // Action already synced to local DB, mark as completed
                Timber.d("UPDATE_GROCERY: Local-only operation, marking complete")
                true
            }
        }
    }

    // region Payload Extraction Helpers

    private fun extractNotificationId(payload: String): String {
        return payload.substringAfter("\"notification_id\":")
            .substringAfter("\"")
            .substringBefore("\"")
    }

    private fun extractFcmToken(payload: String): String {
        return payload.substringAfter("\"fcm_token\":")
            .substringAfter("\"")
            .substringBefore("\"")
    }

    private fun extractMealIds(payload: String): Pair<String, String> {
        val planId = payload.substringAfter("\"plan_id\":")
            .substringAfter("\"")
            .substringBefore("\"")
        val itemId = payload.substringAfter("\"item_id\":")
            .substringAfter("\"")
            .substringBefore("\"")
        return Pair(planId, itemId)
    }

    private fun extractRecipeId(payload: String): String {
        return payload.substringAfter("\"recipe_id\":")
            .substringAfter("\"")
            .substringBefore("\"")
    }

    // endregion

    companion object {
        private const val WORK_NAME = "rasoi_sync_worker"
        private const val IMMEDIATE_WORK_NAME = "rasoi_sync_immediate"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val SYNC_INTERVAL_HOURS = 6L

        /**
         * Enqueues periodic sync work.
         * Call this when the app starts or when user logs in.
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                SYNC_INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

            Timber.d("SyncWorker enqueued for periodic execution")
        }

        /**
         * Triggers an immediate sync.
         * Use this when the app comes online after being offline.
         */
        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .addTag(IMMEDIATE_WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )

            Timber.d("Immediate sync triggered")
        }

        /**
         * Cancels all pending sync work.
         * Call this when user logs out.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK_NAME)
            Timber.d("SyncWorker cancelled")
        }
    }
}
