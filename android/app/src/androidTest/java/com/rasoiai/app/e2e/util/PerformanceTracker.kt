package com.rasoiai.app.e2e.util

import android.os.SystemClock
import android.util.Log

/**
 * Tracks performance metrics during E2E tests.
 * Logs to Logcat and prints summary at end.
 *
 * ## Usage
 * ```kotlin
 * // In @Before
 * PerformanceTracker.reset()
 *
 * // During test
 * PerformanceTracker.measure("Home Screen Load", PerformanceTracker.HOME_SCREEN_LOAD_MS) {
 *     // code to measure
 * }
 *
 * // In @After
 * PerformanceTracker.printSummary()
 * ```
 *
 * ## Important Notes
 * - Threshold violations log warnings but do NOT fail tests
 * - Use Logcat filter "E2E_PERF" to see all metrics
 */
object PerformanceTracker {
    private const val TAG = "E2E_PERF"

    data class Metric(
        val name: String,
        val durationMs: Long,
        val thresholdMs: Long
    ) {
        val passed: Boolean get() = durationMs <= thresholdMs
        val status: String get() = if (passed) "✓ PASS" else "⚠️ WARN"
    }

    private val metrics = mutableListOf<Metric>()

    // Thresholds (in milliseconds)
    const val HOME_SCREEN_LOAD_MS = 3000L
    const val MEAL_GENERATION_MS = 15000L
    const val RECIPE_DETAIL_LOAD_MS = 500L
    const val GROCERY_LIST_LOAD_MS = 1000L
    const val SCREEN_TRANSITION_MS = 300L

    /**
     * Resets all tracked metrics.
     * Call in @Before method of each test class.
     */
    fun reset() {
        metrics.clear()
        Log.d(TAG, "Performance tracker reset")
    }

    /**
     * Measure a timed operation and record the metric.
     *
     * @param name Human-readable name for the metric
     * @param thresholdMs Maximum acceptable duration in milliseconds
     * @param block The code to measure
     * @return The result of the block
     */
    inline fun <T> measure(name: String, thresholdMs: Long, block: () -> T): T {
        val startTime = SystemClock.elapsedRealtime()
        val result = block()
        val duration = SystemClock.elapsedRealtime() - startTime

        record(name, duration, thresholdMs)

        return result
    }

    /**
     * Record a metric with pre-measured duration.
     * Use when you need to measure across async operations.
     *
     * @param name Human-readable name for the metric
     * @param durationMs The measured duration in milliseconds
     * @param thresholdMs Maximum acceptable duration in milliseconds
     */
    fun record(name: String, durationMs: Long, thresholdMs: Long) {
        val metric = Metric(name, durationMs, thresholdMs)
        metrics.add(metric)

        Log.d(TAG, "${metric.status} $name: ${durationMs}ms (threshold: ${thresholdMs}ms)")
    }

    /**
     * Get the last recorded duration for a specific metric name.
     * Returns null if no metric with that name exists.
     */
    fun getLastDuration(name: String): Long? {
        return metrics.lastOrNull { it.name == name }?.durationMs
    }

    /**
     * Get all recorded metrics.
     */
    fun getAllMetrics(): List<Metric> = metrics.toList()

    /**
     * Check if all metrics passed their thresholds.
     */
    fun allPassed(): Boolean = metrics.all { it.passed }

    /**
     * Get count of passed metrics.
     */
    fun passedCount(): Int = metrics.count { it.passed }

    /**
     * Get count of failed (warned) metrics.
     */
    fun warnedCount(): Int = metrics.count { !it.passed }

    /**
     * Print summary table to Logcat.
     * Call in @After method of each test class.
     */
    fun printSummary() {
        if (metrics.isEmpty()) {
            Log.i(TAG, "No performance metrics recorded")
            return
        }

        val sb = StringBuilder()
        sb.appendLine()
        sb.appendLine("╔══════════════════════════════════════════════════════════════╗")
        sb.appendLine("║                 E2E PERFORMANCE SUMMARY                       ║")
        sb.appendLine("╠══════════════════════════════════════════════════════════════╣")
        sb.appendLine("║ Metric                    │ Actual    │ Threshold │ Status   ║")
        sb.appendLine("╠───────────────────────────┼───────────┼───────────┼──────────╣")

        for (metric in metrics) {
            val name = metric.name.padEnd(25).take(25)
            val actual = "${metric.durationMs}ms".padStart(9)
            val threshold = "< ${metric.thresholdMs}ms".padStart(9)
            val status = metric.status.padEnd(8)
            sb.appendLine("║ $name │ $actual │ $threshold │ $status ║")
        }

        sb.appendLine("╚══════════════════════════════════════════════════════════════╝")

        val passed = passedCount()
        val total = metrics.size
        val warnings = warnedCount()

        sb.appendLine()
        sb.appendLine("Summary: $passed/$total passed" + if (warnings > 0) ", $warnings warnings" else "")

        Log.i(TAG, sb.toString())
    }

    /**
     * Print a brief inline summary (single line).
     */
    fun printInlineSummary() {
        val passed = passedCount()
        val total = metrics.size
        val warnings = warnedCount()

        val summaryText = if (warnings > 0) {
            "Performance: $passed/$total passed, $warnings warnings"
        } else {
            "Performance: $passed/$total passed"
        }

        Log.i(TAG, summaryText)
    }
}
