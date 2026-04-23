package com.rasoiai.app.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rasoiai.app.R
import com.rasoiai.data.local.dao.NotificationDao
import com.rasoiai.data.local.entity.NotificationEntity
import com.rasoiai.domain.repository.NotificationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

/**
 * Firebase Cloud Messaging service for handling push notifications.
 *
 * Handles:
 * - Receiving push notifications from FCM
 * - Storing notifications locally
 * - Displaying system notifications
 * - Token refresh
 *
 * Note: Uses EntryPointAccessors for Hilt injection since FirebaseMessagingService
 * doesn't support @AndroidEntryPoint directly.
 */
class RasoiFcmService : FirebaseMessagingService() {

    /**
     * Hilt entry point for accessing dependencies in FirebaseMessagingService.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FcmServiceEntryPoint {
        fun notificationDao(): NotificationDao
        fun notificationRepository(): NotificationRepository
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val entryPoint: FcmServiceEntryPoint? by lazy {
        try {
            EntryPointAccessors.fromApplication(
                applicationContext,
                FcmServiceEntryPoint::class.java
            )
        } catch (e: IllegalStateException) {
            // Hilt component not yet created (happens during instrumented tests)
            Timber.w("Hilt component not available for FcmService: ${e.message}")
            null
        }
    }

    private val notificationDao: NotificationDao? by lazy { entryPoint?.notificationDao() }
    private val notificationRepository: NotificationRepository? by lazy { entryPoint?.notificationRepository() }

    /**
     * Called when a new FCM token is generated.
     * This happens on:
     * - Initial app install
     * - App data is cleared
     * - App restored on new device
     * - User uninstalls/reinstalls the app
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("New FCM token: $token")

        // Register token with backend
        serviceScope.launch {
            notificationRepository?.registerFcmToken(token)
                ?: Timber.w("Cannot register FCM token: Hilt component not available")
        }
    }

    /**
     * Called when a message is received.
     * Messages can contain:
     * - notification: Displayed automatically when app is in background
     * - data: Always delivered to this method
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("FCM message received from: ${remoteMessage.from}")

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("FCM data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Handle notification payload (when app is in foreground)
        remoteMessage.notification?.let { notification ->
            Timber.d("FCM notification: ${notification.title}")
            handleNotificationMessage(notification, remoteMessage.data)
        }
    }

    /**
     * Handles data-only messages.
     * These are always delivered to the app regardless of foreground/background state.
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val notificationType = data["type"] ?: "meal_plan_update"
        val notificationId = data["notification_id"] ?: UUID.randomUUID().toString()
        val title = data["title"] ?: "RasoiAI"
        val body = data["body"] ?: ""
        val imageUrl = data["image_url"]
        val actionType = data["action_type"]
        val actionData = data["action_data"]
        val expiresAt = data["expires_at"]?.toLongOrNull()

        // Store notification locally
        serviceScope.launch {
            val dao = notificationDao
            if (dao != null) {
                val entity = NotificationEntity(
                    id = notificationId,
                    type = notificationType,
                    title = title,
                    body = body,
                    imageUrl = imageUrl,
                    actionType = actionType,
                    actionData = actionData,
                    isRead = false,
                    createdAt = System.currentTimeMillis(),
                    expiresAt = expiresAt
                )
                dao.insertNotification(entity)
                Timber.d("Notification stored locally: $notificationId")
            } else {
                Timber.w("Cannot store notification: Hilt component not available")
            }
        }

        // Show system notification
        showNotification(
            id = notificationId.hashCode(),
            channelId = NotificationChannelManager.getChannelForType(notificationType),
            title = title,
            body = body,
            imageUrl = imageUrl,
            data = data
        )
    }

    /**
     * Handles notification payloads (when app is in foreground).
     */
    private fun handleNotificationMessage(
        notification: RemoteMessage.Notification,
        data: Map<String, String>
    ) {
        val notificationType = data["type"] ?: "meal_plan_update"
        val notificationId = data["notification_id"] ?: UUID.randomUUID().toString()

        // Store notification locally
        serviceScope.launch {
            val dao = notificationDao
            if (dao != null) {
                val entity = NotificationEntity(
                    id = notificationId,
                    type = notificationType,
                    title = notification.title ?: "RasoiAI",
                    body = notification.body ?: "",
                    imageUrl = notification.imageUrl?.toString(),
                    actionType = data["action_type"],
                    actionData = data["action_data"],
                    isRead = false,
                    createdAt = System.currentTimeMillis(),
                    expiresAt = data["expires_at"]?.toLongOrNull()
                )
                dao.insertNotification(entity)
            } else {
                Timber.w("Cannot store notification: Hilt component not available")
            }
        }

        // Show system notification (for foreground)
        showNotification(
            id = notificationId.hashCode(),
            channelId = NotificationChannelManager.getChannelForType(notificationType),
            title = notification.title ?: "RasoiAI",
            body = notification.body ?: "",
            imageUrl = notification.imageUrl?.toString(),
            data = data
        )
    }

    /**
     * Displays a system notification.
     */
    private fun showNotification(
        id: Int,
        channelId: String,
        title: String,
        body: String,
        imageUrl: String?,
        data: Map<String, String>
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create pending intent for notification tap
        val intent = createNotificationIntent(data)
        val pendingIntent = PendingIntent.getActivity(
            this,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(getPriorityForChannel(channelId))

        // Add big text style for longer messages
        if (body.length > 50) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        // TODO: Load image with Coil and set as largeIcon if imageUrl is provided

        notificationManager.notify(id, builder.build())
        Timber.d("Notification displayed: $id")
    }

    /**
     * Creates an intent for handling notification taps.
     */
    private fun createNotificationIntent(data: Map<String, String>): Intent {
        // Launch main activity - deep link handling happens there
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent().apply {
                setClassName(packageName, "$packageName.MainActivity")
            }

        // Add data for deep linking
        data["action_type"]?.let { actionType ->
            intent.putExtra("notification_action_type", actionType)
        }
        data["action_data"]?.let { actionData ->
            intent.putExtra("notification_action_data", actionData)
        }
        data["notification_id"]?.let { notificationId ->
            intent.putExtra("notification_id", notificationId)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        return intent
    }

    /**
     * Gets notification priority for a channel.
     */
    private fun getPriorityForChannel(channelId: String): Int {
        return when (channelId) {
            NotificationChannelManager.CHANNEL_REMINDERS -> NotificationCompat.PRIORITY_HIGH
            NotificationChannelManager.CHANNEL_UPDATES -> NotificationCompat.PRIORITY_DEFAULT
            NotificationChannelManager.CHANNEL_ACHIEVEMENTS -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }
}
