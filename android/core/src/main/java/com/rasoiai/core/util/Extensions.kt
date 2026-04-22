package com.rasoiai.core.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Common extension functions used throughout the app.
 */

/**
 * Safely executes a suspend function and returns Result.
 *
 * CancellationException is rethrown (never wrapped in Result.failure) so
 * structured-concurrency cancellation still works through this helper —
 * per `.claude/rules/android-kotlin.md`: "NEVER catch CancellationException".
 */
suspend fun <T> safeCall(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Extension to format minutes to human-readable duration.
 * Example: 90 -> "1h 30m"
 */
fun Int.toReadableDuration(): String {
    return when {
        this < 60 -> "${this}m"
        this % 60 == 0 -> "${this / 60}h"
        else -> "${this / 60}h ${this % 60}m"
    }
}

/**
 * Extension to format servings count.
 * Example: 4 -> "4 servings"
 */
fun Int.toServingsText(): String {
    return if (this == 1) "$this serving" else "$this servings"
}

/**
 * Extension to capitalize first letter of each word.
 */
fun String.toTitleCase(): String {
    return split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
}

/**
 * Extension to truncate text with ellipsis.
 */
fun String.truncate(maxLength: Int): String {
    return if (length <= maxLength) this else "${take(maxLength - 3)}..."
}
