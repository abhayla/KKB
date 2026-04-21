package com.rasoiai.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class OfflineActionTest {

    @Nested
    @DisplayName("ActionStatus.fromValue")
    inner class ActionStatusFromValue {
        @Test
        fun `maps known values`() {
            assertEquals(ActionStatus.PENDING, ActionStatus.fromValue("pending"))
            assertEquals(ActionStatus.IN_PROGRESS, ActionStatus.fromValue("in_progress"))
            assertEquals(ActionStatus.COMPLETED, ActionStatus.fromValue("completed"))
            assertEquals(ActionStatus.FAILED, ActionStatus.fromValue("failed"))
        }

        @Test
        fun `unknown values fall back to PENDING`() {
            assertEquals(ActionStatus.PENDING, ActionStatus.fromValue("unknown"))
            assertEquals(ActionStatus.PENDING, ActionStatus.fromValue(""))
        }
    }

    @Nested
    @DisplayName("OfflineActionType.fromValue")
    inner class OfflineActionTypeFromValue {
        @Test
        fun `maps known value`() {
            assertEquals(
                OfflineActionType.TOGGLE_FAVORITE,
                OfflineActionType.fromValue("toggle_favorite"),
            )
        }

        @Test
        fun `unknown value falls back to SWAP_MEAL`() {
            assertEquals(OfflineActionType.SWAP_MEAL, OfflineActionType.fromValue("???"))
        }

        @Test
        fun `every enum entry round-trips through fromValue`() {
            // Contract: entry.value -> fromValue(value) must return the same entry.
            // Guards against enum value drift.
            OfflineActionType.entries.forEach { entry ->
                assertEquals(entry, OfflineActionType.fromValue(entry.value))
            }
        }
    }

    @Nested
    @DisplayName("OfflineAction state predicates")
    inner class StatePredicates {
        @Test
        fun `isPending only true for PENDING status`() {
            assertTrue(action(status = ActionStatus.PENDING).isPending)
            assertFalse(action(status = ActionStatus.IN_PROGRESS).isPending)
            assertFalse(action(status = ActionStatus.COMPLETED).isPending)
            assertFalse(action(status = ActionStatus.FAILED).isPending)
        }

        @Test
        fun `isCompleted only true for COMPLETED status`() {
            assertFalse(action(status = ActionStatus.PENDING).isCompleted)
            assertTrue(action(status = ActionStatus.COMPLETED).isCompleted)
            assertFalse(action(status = ActionStatus.FAILED).isCompleted)
        }

        @Test
        fun `hasFailed only true for FAILED status`() {
            assertFalse(action(status = ActionStatus.PENDING).hasFailed)
            assertFalse(action(status = ActionStatus.COMPLETED).hasFailed)
            assertTrue(action(status = ActionStatus.FAILED).hasFailed)
        }
    }

    @Nested
    @DisplayName("OfflineAction.canRetry")
    inner class CanRetry {
        @Test
        fun `true when FAILED and retryCount below MAX_RETRIES`() {
            assertTrue(action(status = ActionStatus.FAILED, retryCount = 0).canRetry)
            assertTrue(action(status = ActionStatus.FAILED, retryCount = 2).canRetry)
        }

        @Test
        fun `false when retryCount reaches MAX_RETRIES`() {
            assertFalse(
                action(
                    status = ActionStatus.FAILED,
                    retryCount = OfflineAction.MAX_RETRIES,
                ).canRetry,
            )
            assertFalse(
                action(
                    status = ActionStatus.FAILED,
                    retryCount = OfflineAction.MAX_RETRIES + 5,
                ).canRetry,
            )
        }

        @Test
        fun `false when status is not FAILED, even if retryCount is low`() {
            assertFalse(action(status = ActionStatus.PENDING, retryCount = 0).canRetry)
            assertFalse(action(status = ActionStatus.IN_PROGRESS, retryCount = 0).canRetry)
            assertFalse(action(status = ActionStatus.COMPLETED, retryCount = 0).canRetry)
        }

        @Test
        fun `MAX_RETRIES is documented value (3)`() {
            assertEquals(3, OfflineAction.MAX_RETRIES)
        }
    }

    private fun action(
        status: ActionStatus = ActionStatus.PENDING,
        retryCount: Int = 0,
    ) = OfflineAction(
        id = "a-1",
        actionType = OfflineActionType.SWAP_MEAL,
        payload = "{}",
        status = status,
        retryCount = retryCount,
        createdAt = 0L,
    )
}
