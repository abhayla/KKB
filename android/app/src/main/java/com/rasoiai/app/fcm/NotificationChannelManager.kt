package com.rasoiai.app.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import timber.log.Timber

/**
 * Manages notification channels for the app.
 *
 * Channels:
 * - REMINDERS (high importance): Festival reminders, shopping reminders
 * - UPDATES (default importance): Meal plan updates, recipe suggestions
 * - ACHIEVEMENTS (low importance): Streak milestones
 */
object NotificationChannelManager {

    // Channel IDs
    const val CHANNEL_REMINDERS = "rasoi_reminders"
    const val CHANNEL_UPDATES = "rasoi_updates"
    const val CHANNEL_ACHIEVEMENTS = "rasoi_achievements"

    // Legacy default channel (for backwards compatibility)
    const val CHANNEL_DEFAULT = "rasoi_default"

    /**
     * Creates all notification channels.
     * Should be called during app initialization.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Reminders channel (high importance)
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Festival reminders and shopping notifications"
                enableVibration(true)
                setShowBadge(true)
            }

            // Updates channel (default importance)
            val updatesChannel = NotificationChannel(
                CHANNEL_UPDATES,
                "Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Meal plan updates and recipe suggestions"
                enableVibration(false)
                setShowBadge(true)
            }

            // Achievements channel (low importance)
            val achievementsChannel = NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                "Achievements",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Cooking streak milestones and achievements"
                enableVibration(false)
                setShowBadge(false)
            }

            // Default channel (fallback)
            val defaultChannel = NotificationChannel(
                CHANNEL_DEFAULT,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General notifications"
            }

            notificationManager.createNotificationChannels(
                listOf(remindersChannel, updatesChannel, achievementsChannel, defaultChannel)
            )

            Timber.d("Notification channels created")
        }
    }

    /**
     * Gets the appropriate channel ID for a notification type.
     */
    fun getChannelForType(notificationType: String): String {
        return when (notificationType) {
            "festival_reminder", "shopping_reminder" -> CHANNEL_REMINDERS
            "meal_plan_update", "recipe_suggestion" -> CHANNEL_UPDATES
            "streak_milestone" -> CHANNEL_ACHIEVEMENTS
            else -> CHANNEL_DEFAULT
        }
    }

    /**
     * Deletes a notification channel.
     */
    fun deleteChannel(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.deleteNotificationChannel(channelId)
            Timber.d("Notification channel deleted: $channelId")
        }
    }

    /**
     * Checks if notifications are enabled for the app.
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.areNotificationsEnabled()
    }

    /**
     * Checks if a specific channel is enabled.
     */
    fun isChannelEnabled(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(channelId)
            return channel?.importance != NotificationManager.IMPORTANCE_NONE
        }
        return true
    }
}
