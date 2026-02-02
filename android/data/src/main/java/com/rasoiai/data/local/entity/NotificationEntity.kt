package com.rasoiai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for user notifications.
 *
 * Notification types:
 * - festival_reminder: Upcoming festival alerts
 * - meal_plan_update: Meal plan changes/regeneration
 * - shopping_reminder: Grocery shopping reminders
 * - recipe_suggestion: Personalized recipe recommendations
 * - streak_milestone: Cooking streak achievements
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["isRead"]),
        Index(value = ["createdAt"]),
        Index(value = ["type"])
    ]
)
data class NotificationEntity(
    @PrimaryKey
    val id: String,

    /** Type of notification: festival_reminder, meal_plan_update, shopping_reminder, recipe_suggestion, streak_milestone */
    val type: String,

    /** Notification title */
    val title: String,

    /** Notification body/message */
    val body: String,

    /** Optional image URL for rich notifications */
    val imageUrl: String? = null,

    /** Action type: open_recipe, open_meal_plan, open_grocery, open_stats, none */
    val actionType: String? = null,

    /** JSON payload with action-specific data (e.g., recipeId, mealPlanId) */
    val actionData: String? = null,

    /** Whether the notification has been read */
    val isRead: Boolean = false,

    /** Timestamp when notification was created */
    val createdAt: Long,

    /** Optional expiration timestamp after which notification should be hidden */
    val expiresAt: Long? = null
)
