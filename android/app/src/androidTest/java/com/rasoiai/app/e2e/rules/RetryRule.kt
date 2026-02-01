package com.rasoiai.app.e2e.rules

import android.util.Log
import androidx.compose.ui.test.ComposeTimeoutException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit TestRule that retries failed tests with configurable retry count.
 * Useful for handling flaky E2E tests that may fail due to timing issues.
 *
 * ## Usage
 * ```kotlin
 * @get:Rule(order = 2)  // After Hilt and Compose rules
 * val retryRule = RetryRule(maxRetries = 2)
 * ```
 *
 * ## With cleanup callback
 * ```kotlin
 * @get:Rule(order = 2)
 * val retryRule = RetryRule(
 *     maxRetries = 2,
 *     onRetry = { attempt, error ->
 *         Log.d("Test", "Retrying test, attempt $attempt after: ${error.message}")
 *         // Cleanup state between retries
 *     }
 * )
 * ```
 *
 * ## Important Notes
 * - This rule retries the ENTIRE test method, including @Before setup
 * - Use with caution - retries can hide real bugs
 * - Only retries on specific exception types (configurable)
 * - Maximum 5 retries to prevent infinite loops
 */
class RetryRule(
    private val maxRetries: Int = 2,
    private val onRetry: ((attempt: Int, error: Throwable) -> Unit)? = null,
    private val retryOnExceptions: Set<Class<out Throwable>> = DEFAULT_RETRY_EXCEPTIONS
) : TestRule {

    companion object {
        private const val TAG = "RetryRule"
        private const val MAX_ALLOWED_RETRIES = 5

        /** Default exceptions that trigger a retry.
         *  Note: IllegalStateException is excluded because Hilt doesn't support re-injection
         *  on the same test instance, and retrying would cause "Called inject() multiple times" error.
         */
        val DEFAULT_RETRY_EXCEPTIONS: Set<Class<out Throwable>> = setOf(
            AssertionError::class.java,
            ComposeTimeoutException::class.java
        )

        /**
         * Creates a RetryRule that retries on all exceptions.
         * Use with caution!
         */
        fun retryAll(maxRetries: Int = 2): RetryRule {
            return RetryRule(
                maxRetries = maxRetries,
                retryOnExceptions = emptySet() // Empty set means retry all
            )
        }
    }

    init {
        require(maxRetries in 0..MAX_ALLOWED_RETRIES) {
            "maxRetries must be between 0 and $MAX_ALLOWED_RETRIES"
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                var lastException: Throwable? = null
                val testName = "${description.className}.${description.methodName}"

                for (attempt in 1..(maxRetries + 1)) {
                    try {
                        base.evaluate()

                        if (attempt > 1) {
                            Log.i(TAG, "[$testName] PASSED on attempt $attempt (after ${attempt - 1} retries)")
                        }
                        return // Test passed
                    } catch (e: Throwable) {
                        lastException = e

                        // Check if we should retry this exception
                        if (!shouldRetry(e)) {
                            Log.e(TAG, "[$testName] Non-retryable exception: ${e::class.java.simpleName}", e)
                            throw e
                        }

                        // Check if we have retries left
                        if (attempt > maxRetries) {
                            Log.e(TAG, "[$testName] FAILED after $maxRetries retries", e)
                            throw e
                        }

                        // Log retry attempt
                        Log.w(TAG, "[$testName] Attempt $attempt failed: ${e.message}")
                        Log.w(TAG, "[$testName] Retrying... (${maxRetries - attempt + 1} retries remaining)")

                        // Call cleanup callback
                        try {
                            onRetry?.invoke(attempt, e)
                        } catch (cleanupError: Throwable) {
                            Log.e(TAG, "[$testName] Error in onRetry callback", cleanupError)
                        }
                    }
                }

                // Should never reach here, but just in case
                throw lastException ?: IllegalStateException("Retry rule failed unexpectedly")
            }
        }
    }

    /**
     * Determines if the given exception should trigger a retry.
     */
    private fun shouldRetry(throwable: Throwable): Boolean {
        // Never retry Hilt injection errors - these indicate framework issues, not flakiness
        if (isHiltInjectionError(throwable)) {
            Log.d(TAG, "Not retrying Hilt injection error")
            return false
        }

        // Empty set means retry all exceptions
        if (retryOnExceptions.isEmpty()) {
            return true
        }

        // Check if the exception or any of its causes match
        var current: Throwable? = throwable
        while (current != null) {
            if (retryOnExceptions.any { it.isInstance(current) }) {
                return true
            }
            current = current.cause
        }

        return false
    }

    /**
     * Checks if the exception is a Hilt injection error.
     * These should never be retried as they indicate framework setup issues.
     */
    private fun isHiltInjectionError(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            val message = current.message ?: ""
            if (current is IllegalStateException &&
                (message.contains("Called inject() multiple times") ||
                 message.contains("Hilt") ||
                 message.contains("component"))) {
                return true
            }
            current = current.cause
        }
        return false
    }
}

/**
 * Annotation to mark a test that should not be retried.
 * Use on tests that are known to be deterministic or where retries
 * would hide real bugs.
 *
 * Note: This annotation is for documentation purposes.
 * The RetryRule does not currently check for this annotation,
 * but it can be extended to do so.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoRetry

/**
 * Annotation to configure retry behavior for a specific test.
 *
 * Note: This annotation is for documentation purposes.
 * The RetryRule does not currently check for this annotation,
 * but it can be extended to do so.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Retry(val maxRetries: Int = 2)
