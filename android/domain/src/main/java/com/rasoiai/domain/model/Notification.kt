package com.rasoiai.domain.model

/**
 * Types of notifications in the app.
 */
enum class NotificationType(val value: String) {
    /** Festival reminders for upcoming festivals */
    FESTIVAL_REMINDER("festival_reminder"),

    /** Meal plan updates/regeneration notifications */
    MEAL_PLAN_UPDATE("meal_plan_update"),

    /** Shopping/grocery reminders */
    SHOPPING_REMINDER("shopping_reminder"),

    /** Personalized recipe suggestions */
    RECIPE_SUGGESTION("recipe_suggestion"),

    /** Cooking streak milestone achievements */
    STREAK_MILESTONE("streak_milestone");

    companion object {
        fun fromValue(value: String): NotificationType {
            return entries.find { it.value == value } ?: MEAL_PLAN_UPDATE
        }
    }
}

/**
 * Types of actions that can be performed from a notification.
 */
enum class NotificationActionType(val value: String) {
    /** Open a specific recipe */
    OPEN_RECIPE("open_recipe"),

    /** Open meal plan screen */
    OPEN_MEAL_PLAN("open_meal_plan"),

    /** Open grocery list */
    OPEN_GROCERY("open_grocery"),

    /** Open stats/achievements screen */
    OPEN_STATS("open_stats"),

    /** No action - just informational */
    NONE("none");

    companion object {
        fun fromValue(value: String?): NotificationActionType {
            return entries.find { it.value == value } ?: NONE
        }
    }
}

/**
 * Represents a user notification.
 *
 * @property id Unique identifier
 * @property type Type of notification
 * @property title Notification title
 * @property body Notification body/message
 * @property imageUrl Optional image URL for rich notifications
 * @property actionType Type of action to perform when tapped
 * @property actionData Data needed for the action (e.g., recipeId)
 * @property isRead Whether the notification has been read
 * @property createdAt Timestamp when notification was created
 * @property expiresAt Optional timestamp after which notification should be hidden
 */
data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val imageUrl: String? = null,
    val actionType: NotificationActionType = NotificationActionType.NONE,
    val actionData: NotificationActionData? = null,
    val isRead: Boolean = false,
    val createdAt: Long,
    val expiresAt: Long? = null
) {
    /** Check if notification is expired */
    val isExpired: Boolean
        get() = expiresAt != null && expiresAt <= System.currentTimeMillis()

    /** Check if notification has an action */
    val hasAction: Boolean
        get() = actionType != NotificationActionType.NONE
}

/**
 * Data class for notification action payload.
 */
data class NotificationActionData(
    val recipeId: String? = null,
    val mealPlanId: String? = null,
    val festivalId: String? = null,
    val streakCount: Int? = null
)
