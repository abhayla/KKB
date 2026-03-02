package com.rasoiai.app.e2e.util

import android.util.Log

/**
 * Structured step-level reporting for journey tests.
 *
 * Usage:
 * ```kotlin
 * val logger = JourneyStepLogger("J04")
 * logger.step(1, 5, "Navigate to Home") { homeRobot.waitForHomeScreen() }
 * logger.step(2, 5, "Select a meal")   { homeRobot.tapFirstMealCard() }
 * logger.printSummary()
 * ```
 *
 * Output in Logcat (tag `JOURNEY`):
 * ```
 * [JOURNEY] J04: STEP 1/5 — Navigate to Home: PASS (1234ms)
 * [JOURNEY] J04: STEP 2/5 — Select a meal: FAIL (567ms) — AssertionError: ...
 * ```
 *
 * Re-throws on failure so JUnit marks the test FAILED.
 * Subsequent steps are skipped because the exception propagates.
 */
class JourneyStepLogger(private val journeyId: String) {

    private data class StepResult(
        val stepNum: Int,
        val name: String,
        val passed: Boolean,
        val durationMs: Long,
        val error: Throwable? = null
    )

    private val results = mutableListOf<StepResult>()

    /**
     * Executes [block] as a named step, logging the outcome.
     * Re-throws any exception so the test fails at this point.
     */
    fun step(stepNum: Int, totalSteps: Int, name: String, block: () -> Unit) {
        val start = System.currentTimeMillis()
        try {
            block()
            val duration = System.currentTimeMillis() - start
            results.add(StepResult(stepNum, name, passed = true, durationMs = duration))
            Log.i(TAG, "[$journeyId] STEP $stepNum/$totalSteps — $name: PASS (${duration}ms)")
        } catch (t: Throwable) {
            val duration = System.currentTimeMillis() - start
            results.add(StepResult(stepNum, name, passed = false, durationMs = duration, error = t))
            Log.e(TAG, "[$journeyId] STEP $stepNum/$totalSteps — $name: FAIL (${duration}ms) — ${t.javaClass.simpleName}: ${t.message}")
            throw t
        }
    }

    /** Prints a pass/fail summary. Call in a `finally` block to ensure it runs on failure. */
    fun printSummary() {
        val passed = results.count { it.passed }
        val failed = results.count { !it.passed }
        val total = results.size
        val totalTime = results.sumOf { it.durationMs }

        Log.i(TAG, "")
        Log.i(TAG, "========================================")
        Log.i(TAG, "[$journeyId] SUMMARY: $passed/$total passed, $failed failed (${totalTime}ms)")
        for (r in results) {
            val status = if (r.passed) "PASS" else "FAIL"
            Log.i(TAG, "  Step ${r.stepNum}: $status — ${r.name} (${r.durationMs}ms)")
        }
        Log.i(TAG, "========================================")
        Log.i(TAG, "")
    }

    companion object {
        private const val TAG = "JOURNEY"
    }
}
