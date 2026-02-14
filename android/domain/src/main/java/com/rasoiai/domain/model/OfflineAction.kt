package com.rasoiai.domain.model

/**
 * Status of an offline action in the queue.
 */
enum class ActionStatus(val value: String) {
    /** Action is waiting to be processed */
    PENDING("pending"),

    /** Action is currently being processed */
    IN_PROGRESS("in_progress"),

    /** Action completed successfully */
    COMPLETED("completed"),

    /** Action failed after all retries */
    FAILED("failed");

    companion object {
        fun fromValue(value: String): ActionStatus {
            return entries.find { it.value == value } ?: PENDING
        }
    }
}

/**
 * Types of offline actions that can be queued.
 */
enum class OfflineActionType(val value: String) {
    /** Swap a meal item in the meal plan */
    SWAP_MEAL("swap_meal"),

    /** Lock/unlock a meal item */
    LOCK_MEAL("lock_meal"),

    /** Remove a meal from the plan */
    REMOVE_MEAL("remove_meal"),

    /** Toggle recipe favorite status */
    TOGGLE_FAVORITE("toggle_favorite"),

    /** Update grocery item (check/uncheck) */
    UPDATE_GROCERY("update_grocery"),

    /** Mark notification as read */
    MARK_NOTIFICATION_READ("mark_notification_read"),

    /** Delete a notification */
    DELETE_NOTIFICATION("delete_notification"),

    /** Register FCM token with backend */
    REGISTER_FCM_TOKEN("register_fcm_token"),

    /** Unregister FCM token */
    UNREGISTER_FCM_TOKEN("unregister_fcm_token"),

    /** Toggle recipe rule active/inactive state */
    TOGGLE_RECIPE_RULE("toggle_recipe_rule"),

    /** Toggle nutrition goal active/inactive state */
    TOGGLE_NUTRITION_GOAL("toggle_nutrition_goal");

    companion object {
        fun fromValue(value: String): OfflineActionType {
            return entries.find { it.value == value } ?: SWAP_MEAL
        }
    }
}

/**
 * Represents an action queued for offline sync.
 *
 * @property id Unique identifier
 * @property actionType Type of action to perform
 * @property payload JSON string containing action parameters
 * @property status Current status of the action
 * @property retryCount Number of sync attempts
 * @property errorMessage Error message from last failed attempt
 * @property createdAt Timestamp when action was queued
 * @property lastAttemptAt Timestamp of last sync attempt
 */
data class OfflineAction(
    val id: String,
    val actionType: OfflineActionType,
    val payload: String,
    val status: ActionStatus = ActionStatus.PENDING,
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long,
    val lastAttemptAt: Long? = null
) {
    /** Maximum number of retries before marking as failed */
    companion object {
        const val MAX_RETRIES = 3
    }

    /** Check if action can be retried */
    val canRetry: Boolean
        get() = status == ActionStatus.FAILED && retryCount < MAX_RETRIES

    /** Check if action is pending */
    val isPending: Boolean
        get() = status == ActionStatus.PENDING

    /** Check if action is completed */
    val isCompleted: Boolean
        get() = status == ActionStatus.COMPLETED

    /** Check if action has failed */
    val hasFailed: Boolean
        get() = status == ActionStatus.FAILED
}
