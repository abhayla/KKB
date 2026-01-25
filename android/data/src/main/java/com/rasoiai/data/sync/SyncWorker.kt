package com.rasoiai.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Background worker for syncing offline data with the server.
 * Handles:
 * - Uploading queued offline actions
 * - Downloading updated meal plans
 * - Syncing favorites and grocery lists
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    // TODO: Inject repositories when implemented
    // private val mealPlanRepository: MealPlanRepository,
    // private val recipeRepository: RecipeRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker started")

        return try {
            // TODO: Implement sync logic
            // 1. Upload queued offline actions
            // syncOfflineQueue()

            // 2. Download updated meal plans
            // syncMealPlans()

            // 3. Sync favorites
            // syncFavorites()

            // 4. Sync grocery list
            // syncGroceryList()

            Timber.d("SyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker failed")
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val WORK_NAME = "rasoi_sync_worker"
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
         * Cancels all pending sync work.
         * Call this when user logs out.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.d("SyncWorker cancelled")
        }
    }
}
