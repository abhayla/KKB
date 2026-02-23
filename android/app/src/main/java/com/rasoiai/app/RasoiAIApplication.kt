package com.rasoiai.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rasoiai.app.fcm.NotificationChannelManager
import com.rasoiai.data.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class RasoiAIApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        initTimber()
        initNotificationChannels()
        initSyncWorker()
    }

    /**
     * Initialize periodic SyncWorker for background data sync.
     * Runs every 6 hours when network is available and battery is not low.
     */
    private fun initSyncWorker() {
        SyncWorker.enqueue(this)
        Timber.d("SyncWorker scheduled for periodic execution")
    }

    /**
     * Initialize notification channels for FCM.
     * Must be done before any notifications are sent.
     */
    private fun initNotificationChannels() {
        NotificationChannelManager.createChannels(this)
        Timber.d("Notification channels initialized")
    }

    /**
     * Configure Coil ImageLoader with optimized caching settings.
     * - Memory cache: 25% of available memory
     * - Disk cache: 100MB for recipe images
     * - Both read and write caching enabled
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB disk cache
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(true) // Smooth image transitions
            .respectCacheHeaders(false) // Ignore server cache headers for offline support
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In release, plant a tree that reports to Crashlytics
            Timber.plant(CrashReportingTree())
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()

    /**
     * A tree which logs important information for crash reporting.
     */
    private class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
                return
            }

            // Log to Crashlytics
            FirebaseCrashlytics.getInstance().log("$tag: $message")
            if (t != null) {
                FirebaseCrashlytics.getInstance().recordException(t)
            }
        }
    }
}
