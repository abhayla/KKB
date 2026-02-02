package com.rasoiai.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Notification response from the API.
 */
data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("action_type")
    val actionType: String? = null,
    @SerializedName("action_data")
    val actionData: NotificationActionDataDto? = null,
    @SerializedName("is_read")
    val isRead: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("expires_at")
    val expiresAt: String? = null
)

/**
 * Notification action data from the API.
 */
data class NotificationActionDataDto(
    @SerializedName("recipe_id")
    val recipeId: String? = null,
    @SerializedName("meal_plan_id")
    val mealPlanId: String? = null,
    @SerializedName("festival_id")
    val festivalId: String? = null,
    @SerializedName("streak_count")
    val streakCount: Int? = null
)

/**
 * Response containing a list of notifications.
 */
data class NotificationsResponse(
    val notifications: List<NotificationDto>,
    @SerializedName("unread_count")
    val unreadCount: Int,
    @SerializedName("total_count")
    val totalCount: Int
)

/**
 * Request to register FCM token.
 */
data class FcmTokenRequest(
    @SerializedName("fcm_token")
    val fcmToken: String,
    @SerializedName("device_type")
    val deviceType: String = "android"
)

/**
 * Request to unregister FCM token.
 */
data class FcmTokenDeleteRequest(
    @SerializedName("fcm_token")
    val fcmToken: String
)

/**
 * Generic success response.
 */
data class SuccessResponse(
    val success: Boolean,
    val message: String? = null
)
