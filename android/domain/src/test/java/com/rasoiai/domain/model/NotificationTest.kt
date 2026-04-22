package com.rasoiai.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NotificationTest {

    @Nested
    @DisplayName("NotificationType.fromValue")
    inner class NotificationTypeFromValue {
        @Test
        fun `known values map to enum entries`() {
            assertEquals(NotificationType.FESTIVAL_REMINDER, NotificationType.fromValue("festival_reminder"))
            assertEquals(NotificationType.STREAK_MILESTONE, NotificationType.fromValue("streak_milestone"))
        }

        @Test
        fun `unknown values fall back to MEAL_PLAN_UPDATE`() {
            assertEquals(NotificationType.MEAL_PLAN_UPDATE, NotificationType.fromValue("garbage"))
        }

        @Test
        fun `every entry round-trips`() {
            NotificationType.entries.forEach { entry ->
                assertEquals(entry, NotificationType.fromValue(entry.value))
            }
        }
    }

    @Nested
    @DisplayName("NotificationActionType.fromValue")
    inner class NotificationActionTypeFromValue {
        @Test
        fun `known values map to entries`() {
            assertEquals(NotificationActionType.OPEN_RECIPE, NotificationActionType.fromValue("open_recipe"))
            assertEquals(NotificationActionType.NONE, NotificationActionType.fromValue("none"))
        }

        @Test
        fun `null falls back to NONE`() {
            // fromValue accepts nullable — null is 'no action'.
            assertEquals(NotificationActionType.NONE, NotificationActionType.fromValue(null))
        }

        @Test
        fun `unknown values fall back to NONE`() {
            assertEquals(NotificationActionType.NONE, NotificationActionType.fromValue("bogus"))
        }

        @Test
        fun `every entry round-trips`() {
            NotificationActionType.entries.forEach { entry ->
                assertEquals(entry, NotificationActionType.fromValue(entry.value))
            }
        }
    }

    @Nested
    @DisplayName("Notification.isExpired")
    inner class IsExpired {
        @Test
        fun `null expiresAt is never expired`() {
            assertFalse(notification(expiresAt = null).isExpired)
        }

        @Test
        fun `expiresAt in the past is expired`() {
            val pastMs = System.currentTimeMillis() - 10_000L
            assertTrue(notification(expiresAt = pastMs).isExpired)
        }

        @Test
        fun `expiresAt far in the future is not expired`() {
            val futureMs = System.currentTimeMillis() + 60_000L
            assertFalse(notification(expiresAt = futureMs).isExpired)
        }
    }

    @Nested
    @DisplayName("Notification.hasAction")
    inner class HasAction {
        @Test
        fun `default NONE actionType has no action`() {
            assertFalse(notification().hasAction)
        }

        @Test
        fun `non-NONE actionTypes have an action`() {
            NotificationActionType.entries
                .filterNot { it == NotificationActionType.NONE }
                .forEach { entry ->
                    assertTrue(notification(actionType = entry).hasAction, "Expected hasAction=true for $entry")
                }
        }
    }

    private fun notification(
        actionType: NotificationActionType = NotificationActionType.NONE,
        expiresAt: Long? = null,
    ) = Notification(
        id = "n-1",
        type = NotificationType.MEAL_PLAN_UPDATE,
        title = "Title",
        body = "Body",
        actionType = actionType,
        createdAt = 0L,
        expiresAt = expiresAt,
    )
}
