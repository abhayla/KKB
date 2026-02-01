package com.rasoiai.app.e2e.util

import android.util.Log
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import kotlin.math.min

/**
 * Retry utilities for E2E tests.
 * Provides exponential backoff retry mechanisms for both
 * action-level and test-level resilience.
 *
 * ## Usage
 * ```kotlin
 * // Generic retry with backoff
 * val result = RetryUtils.retryWithBackoff(RetryConfig()) {
 *     performFlakeyOperation()
 * }
 *
 * // Compose-specific retry
 * composeTestRule.retryAction { onNodeWithTag("tag").performClick() }
 * ```
 */
object RetryUtils {

    @PublishedApi
    internal const val TAG = "RetryUtils"

    /**
     * Configuration for retry behavior with exponential backoff.
     *
     * @param maxAttempts Maximum number of retry attempts (including first try)
     * @param initialDelayMs Initial delay before first retry in milliseconds
     * @param maxDelayMs Maximum delay cap for exponential backoff
     * @param backoffMultiplier Multiplier for each subsequent delay
     */
    data class RetryConfig(
        val maxAttempts: Int = 3,
        val initialDelayMs: Long = 200,
        val maxDelayMs: Long = 2000,
        val backoffMultiplier: Double = 2.0
    ) {
        companion object {
            /** Fast retry config for UI actions */
            val FAST = RetryConfig(
                maxAttempts = 3,
                initialDelayMs = 100,
                maxDelayMs = 500,
                backoffMultiplier = 1.5
            )

            /** Standard config for most operations */
            val STANDARD = RetryConfig(
                maxAttempts = 3,
                initialDelayMs = 200,
                maxDelayMs = 2000,
                backoffMultiplier = 2.0
            )

            /** Backend/network config with longer delays */
            val NETWORK = RetryConfig(
                maxAttempts = 5,
                initialDelayMs = 500,
                maxDelayMs = 5000,
                backoffMultiplier = 2.0
            )
        }
    }

    /**
     * Core retry function with exponential backoff.
     *
     * @param config Retry configuration
     * @param actionName Optional name for logging
     * @param shouldRetry Predicate to determine if exception should trigger retry
     * @param action The action to retry
     * @return Result of the action
     * @throws Exception The last exception if all retries fail
     */
    inline fun <T> retryWithBackoff(
        config: RetryConfig = RetryConfig.STANDARD,
        actionName: String = "action",
        shouldRetry: (Throwable) -> Boolean = { isRetryableException(it) },
        action: () -> T
    ): T {
        var lastException: Throwable? = null
        var currentDelay = config.initialDelayMs

        for (attempt in 1..config.maxAttempts) {
            try {
                return action()
            } catch (e: Throwable) {
                lastException = e

                if (!shouldRetry(e)) {
                    Log.e(TAG, "$actionName: Non-retryable exception on attempt $attempt/${config.maxAttempts}", e)
                    throw e
                }

                if (attempt == config.maxAttempts) {
                    Log.e(TAG, "$actionName: Failed after ${config.maxAttempts} attempts", e)
                    throw e
                }

                Log.w(TAG, "$actionName: Attempt $attempt failed, retrying in ${currentDelay}ms. Error: ${e.message}")

                Thread.sleep(currentDelay)

                // Calculate next delay with exponential backoff, capped at maxDelayMs
                currentDelay = min(
                    (currentDelay * config.backoffMultiplier).toLong(),
                    config.maxDelayMs
                )
            }
        }

        // Should never reach here, but just in case
        throw lastException ?: IllegalStateException("Retry failed with no exception")
    }

    /**
     * Determines if an exception should trigger a retry.
     * Override this for custom retry logic.
     */
    fun isRetryableException(throwable: Throwable): Boolean {
        return when (throwable) {
            // Compose-specific exceptions
            is AssertionError -> true
            is ComposeTimeoutException -> true

            // General flakiness
            is IllegalStateException -> true

            // Network-related (wrapped in other exceptions)
            is java.net.SocketTimeoutException -> true
            is java.net.ConnectException -> true
            is java.io.IOException -> {
                // Retry on network I/O errors
                throwable.message?.contains("timeout", ignoreCase = true) == true ||
                throwable.message?.contains("connection", ignoreCase = true) == true
            }

            else -> false
        }
    }

    /**
     * Tracks retry statistics for debugging and monitoring.
     */
    data class RetryStats(
        var totalAttempts: Int = 0,
        var successfulRetries: Int = 0,
        var failedActions: Int = 0,
        val actionStats: MutableMap<String, Int> = mutableMapOf()
    ) {
        fun recordAttempt(actionName: String, attempt: Int) {
            totalAttempts++
            if (attempt > 1) {
                successfulRetries++
                actionStats[actionName] = (actionStats[actionName] ?: 0) + 1
            }
        }

        fun recordFailure() {
            failedActions++
        }

        fun printSummary() {
            Log.i(TAG, "╔════════════════════════════════════════╗")
            Log.i(TAG, "║         RETRY STATISTICS               ║")
            Log.i(TAG, "╠════════════════════════════════════════╣")
            Log.i(TAG, "║ Total attempts: $totalAttempts")
            Log.i(TAG, "║ Successful retries: $successfulRetries")
            Log.i(TAG, "║ Failed actions: $failedActions")
            if (actionStats.isNotEmpty()) {
                Log.i(TAG, "╠────────────────────────────────────────╣")
                Log.i(TAG, "║ Actions requiring retry:")
                actionStats.forEach { (action, count) ->
                    Log.i(TAG, "║   $action: $count retries")
                }
            }
            Log.i(TAG, "╚════════════════════════════════════════╝")
        }

        fun reset() {
            totalAttempts = 0
            successfulRetries = 0
            failedActions = 0
            actionStats.clear()
        }
    }

    /** Global retry statistics tracker */
    val stats = RetryStats()

    /**
     * Retry with statistics tracking.
     */
    inline fun <T> retryWithStats(
        config: RetryConfig = RetryConfig.STANDARD,
        actionName: String = "action",
        action: () -> T
    ): T {
        var lastAttempt = 0
        try {
            return retryWithBackoff(config, actionName) {
                lastAttempt++
                action()
            }.also {
                stats.recordAttempt(actionName, lastAttempt)
            }
        } catch (e: Throwable) {
            stats.recordFailure()
            throw e
        }
    }
}

/**
 * Extension function to retry a Compose action with exponential backoff.
 *
 * @param config Retry configuration
 * @param actionName Optional name for logging
 * @param action The Compose action to retry (should return SemanticsNodeInteraction)
 * @return The result SemanticsNodeInteraction
 */
fun ComposeContentTestRule.retryAction(
    config: RetryUtils.RetryConfig = RetryUtils.RetryConfig.FAST,
    actionName: String = "Compose action",
    action: () -> SemanticsNodeInteraction
): SemanticsNodeInteraction {
    return RetryUtils.retryWithBackoff(config, actionName) {
        waitForIdle()
        action()
    }
}

/**
 * Extension function to retry a Compose assertion with exponential backoff.
 *
 * @param config Retry configuration
 * @param actionName Optional name for logging
 * @param assertion The assertion block to retry
 */
fun ComposeContentTestRule.retryAssertion(
    config: RetryUtils.RetryConfig = RetryUtils.RetryConfig.STANDARD,
    actionName: String = "Compose assertion",
    assertion: () -> Unit
) {
    RetryUtils.retryWithBackoff(config, actionName) {
        waitForIdle()
        assertion()
    }
}
