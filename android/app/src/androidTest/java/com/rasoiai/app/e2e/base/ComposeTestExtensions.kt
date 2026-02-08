package com.rasoiai.app.e2e.base

import android.util.Log
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeDown
import com.rasoiai.app.e2e.util.RetryUtils
import kotlin.math.min

/**
 * Extension functions for Compose UI testing.
 * Provides fluent API for common test operations.
 */

// Node existence and display

fun ComposeContentTestRule.assertNodeWithTagDisplayed(tag: String): SemanticsNodeInteraction {
    return onNodeWithTag(tag).assertIsDisplayed()
}

fun ComposeContentTestRule.assertNodeWithTextDisplayed(text: String): SemanticsNodeInteraction {
    return onNodeWithText(text).assertIsDisplayed()
}

fun ComposeContentTestRule.assertNodeWithTagExists(tag: String): SemanticsNodeInteraction {
    return onNodeWithTag(tag).assertExists()
}

fun ComposeContentTestRule.assertNodeWithTagDoesNotExist(tag: String) {
    onNodeWithTag(tag).assertDoesNotExist()
}

// Click actions

fun ComposeContentTestRule.clickNodeWithTag(tag: String): SemanticsNodeInteraction {
    return onNodeWithTag(tag).performClick()
}

fun ComposeContentTestRule.clickNodeWithText(text: String): SemanticsNodeInteraction {
    return onNodeWithText(text).performClick()
}

// Text input

fun ComposeContentTestRule.inputTextToNodeWithTag(tag: String, text: String): SemanticsNodeInteraction {
    val node = onNodeWithTag(tag)
    node.performTextInput(text)
    return node
}

fun ComposeContentTestRule.clearTextOnNodeWithTag(tag: String): SemanticsNodeInteraction {
    val node = onNodeWithTag(tag)
    node.performTextClearance()
    return node
}

fun ComposeContentTestRule.replaceTextOnNodeWithTag(tag: String, text: String): SemanticsNodeInteraction {
    val node = onNodeWithTag(tag)
    node.performTextClearance()
    node.performTextInput(text)
    return node
}

// Scroll operations

fun ComposeContentTestRule.scrollToNodeWithTag(tag: String): SemanticsNodeInteraction {
    return onNodeWithTag(tag).performScrollTo()
}

fun ComposeContentTestRule.scrollToNodeWithText(text: String): SemanticsNodeInteraction {
    return onNodeWithText(text).performScrollTo()
}

// Swipe gestures

fun SemanticsNodeInteraction.swipeLeftAction(): SemanticsNodeInteraction {
    performTouchInput { swipeLeft() }
    return this
}

fun SemanticsNodeInteraction.swipeRightAction(): SemanticsNodeInteraction {
    performTouchInput { swipeRight() }
    return this
}

fun SemanticsNodeInteraction.swipeUpAction(): SemanticsNodeInteraction {
    performTouchInput { swipeUp() }
    return this
}

fun SemanticsNodeInteraction.swipeDownAction(): SemanticsNodeInteraction {
    performTouchInput { swipeDown() }
    return this
}

// Collection operations

fun ComposeContentTestRule.countNodesWithTag(tag: String): Int {
    return onAllNodesWithTag(tag).fetchSemanticsNodes().size
}

fun ComposeContentTestRule.countNodesWithText(text: String): Int {
    return onAllNodesWithText(text).fetchSemanticsNodes().size
}

// Wait utilities

fun ComposeContentTestRule.waitUntilNodeWithTagExists(
    tag: String,
    timeoutMillis: Long = 5000
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
}

fun ComposeContentTestRule.waitUntilNodeWithTextExists(
    text: String,
    timeoutMillis: Long = 5000
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
}

fun ComposeContentTestRule.waitUntilNodeWithTagDisappears(
    tag: String,
    timeoutMillis: Long = 5000
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
    }
}

// Assertion helpers

fun SemanticsNodeInteraction.assertEnabled(): SemanticsNodeInteraction {
    assertIsEnabled()
    return this
}

fun SemanticsNodeInteraction.assertDisabled(): SemanticsNodeInteraction {
    assertIsNotEnabled()
    return this
}

fun SemanticsNodeInteraction.assertSelected(): SemanticsNodeInteraction {
    assertIsSelected()
    return this
}

fun SemanticsNodeInteraction.assertNotSelected(): SemanticsNodeInteraction {
    assertIsNotSelected()
    return this
}

// Combined operations for common patterns

/**
 * Scroll to an element and click it.
 */
fun ComposeContentTestRule.scrollToAndClickNodeWithTag(tag: String): SemanticsNodeInteraction {
    return onNodeWithTag(tag)
        .performScrollTo()
        .performClick()
}

/**
 * Scroll to an element and click it by text.
 */
fun ComposeContentTestRule.scrollToAndClickNodeWithText(text: String): SemanticsNodeInteraction {
    return onNodeWithText(text)
        .performScrollTo()
        .performClick()
}

/**
 * Wait for a node to appear and then click it.
 */
fun ComposeContentTestRule.waitAndClickNodeWithTag(
    tag: String,
    timeoutMillis: Long = 5000
): SemanticsNodeInteraction {
    waitUntilNodeWithTagExists(tag, timeoutMillis)
    return onNodeWithTag(tag).performClick()
}

/**
 * Wait for a node to appear and then click it by text.
 */
fun ComposeContentTestRule.waitAndClickNodeWithText(
    text: String,
    timeoutMillis: Long = 5000
): SemanticsNodeInteraction {
    waitUntilNodeWithTextExists(text, timeoutMillis)
    return onNodeWithText(text).performClick()
}

// ============================================================
// Retry Extensions with Exponential Backoff
// ============================================================

private const val TAG = "ComposeTestExt"

/**
 * Wait for a node with exponential backoff polling.
 * More efficient than fixed-interval polling for slow-loading content.
 *
 * @param tag The test tag to wait for
 * @param timeoutMillis Maximum time to wait
 * @param initialPollMs Initial polling interval
 * @param maxPollMs Maximum polling interval (cap for backoff)
 * @param backoffMultiplier Multiplier for each poll interval
 */
fun ComposeContentTestRule.waitUntilWithBackoff(
    tag: String,
    timeoutMillis: Long = 5000,
    initialPollMs: Long = 50,
    maxPollMs: Long = 500,
    backoffMultiplier: Double = 1.5,
    useUnmergedTree: Boolean = true
) {
    val startTime = System.currentTimeMillis()
    var currentPoll = initialPollMs

    while ((System.currentTimeMillis() - startTime) < timeoutMillis) {
        if (onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree).fetchSemanticsNodes().isNotEmpty()) {
            return
        }

        Thread.sleep(currentPoll)
        currentPoll = min((currentPoll * backoffMultiplier).toLong(), maxPollMs)
    }

    // Final check - throw error with descriptive message
    if (onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree).fetchSemanticsNodes().isEmpty()) {
        throw AssertionError(
            "Timeout waiting for node with tag '$tag' after ${timeoutMillis}ms"
        )
    }
}

/**
 * Wait for text to appear with exponential backoff polling.
 */
fun ComposeContentTestRule.waitUntilTextWithBackoff(
    text: String,
    timeoutMillis: Long = 5000,
    initialPollMs: Long = 50,
    maxPollMs: Long = 500,
    substring: Boolean = false
) {
    val startTime = System.currentTimeMillis()
    var currentPoll = initialPollMs

    while ((System.currentTimeMillis() - startTime) < timeoutMillis) {
        if (onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()) {
            return
        }

        Thread.sleep(currentPoll)
        currentPoll = min((currentPoll * 1.5).toLong(), maxPollMs)
    }

    // Final check - throw error with descriptive message
    if (onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isEmpty()) {
        throw AssertionError(
            "Timeout waiting for text '$text' after ${timeoutMillis}ms"
        )
    }
}

/**
 * Click a node with retry logic.
 * Useful for flaky click operations that may fail due to timing issues.
 *
 * @param tag The test tag of the node to click
 * @param maxAttempts Maximum number of click attempts
 * @param delayBetweenAttempts Delay between retry attempts in milliseconds
 */
fun ComposeContentTestRule.clickWithRetry(
    tag: String,
    maxAttempts: Int = 3,
    delayBetweenAttempts: Long = 200
): SemanticsNodeInteraction {
    return RetryUtils.retryWithBackoff(
        config = RetryUtils.RetryConfig(
            maxAttempts = maxAttempts,
            initialDelayMs = delayBetweenAttempts,
            maxDelayMs = 1000,
            backoffMultiplier = 1.5
        ),
        actionName = "clickWithRetry($tag)"
    ) {
        waitForIdle()
        onNodeWithTag(tag).performClick()
    }
}

/**
 * Click a node by text with retry logic.
 */
fun ComposeContentTestRule.clickTextWithRetry(
    text: String,
    maxAttempts: Int = 3,
    ignoreCase: Boolean = false
): SemanticsNodeInteraction {
    return RetryUtils.retryWithBackoff(
        config = RetryUtils.RetryConfig.FAST.copy(maxAttempts = maxAttempts),
        actionName = "clickTextWithRetry($text)"
    ) {
        waitForIdle()
        onNodeWithText(text, ignoreCase = ignoreCase).performClick()
    }
}

/**
 * Wait for network-dependent content to appear.
 * Uses longer timeouts and backoff suitable for API calls.
 *
 * @param tag The test tag of the content to wait for
 * @param timeoutMillis Maximum time to wait (default 15s for network content)
 */
fun ComposeContentTestRule.waitForNetworkContent(
    tag: String,
    timeoutMillis: Long = 15000
) {
    waitUntilWithBackoff(
        tag = tag,
        timeoutMillis = timeoutMillis,
        initialPollMs = 200,
        maxPollMs = 1000,
        backoffMultiplier = 1.5
    )
}

/**
 * Wait for network-dependent text content to appear.
 */
fun ComposeContentTestRule.waitForNetworkText(
    text: String,
    timeoutMillis: Long = 15000
) {
    waitUntilTextWithBackoff(
        text = text,
        timeoutMillis = timeoutMillis,
        initialPollMs = 200,
        maxPollMs = 1000
    )
}

/**
 * Scroll to and click a node with retry.
 * Combines scroll and click with retry for list items.
 */
fun ComposeContentTestRule.scrollToAndClickWithRetry(
    tag: String,
    maxAttempts: Int = 3
): SemanticsNodeInteraction {
    return RetryUtils.retryWithBackoff(
        config = RetryUtils.RetryConfig.FAST.copy(maxAttempts = maxAttempts),
        actionName = "scrollToAndClickWithRetry($tag)"
    ) {
        waitForIdle()
        onNodeWithTag(tag)
            .performScrollTo()
            .performClick()
    }
}

/**
 * Assert a node is displayed with retry.
 * Useful when the node might take time to appear or stabilize.
 */
fun ComposeContentTestRule.assertDisplayedWithRetry(
    tag: String,
    maxAttempts: Int = 3,
    timeoutMillis: Long = 5000
) {
    // First wait for the node to exist
    waitUntilWithBackoff(tag, timeoutMillis)

    // Then assert with retry in case of timing issues
    RetryUtils.retryWithBackoff(
        config = RetryUtils.RetryConfig.FAST.copy(maxAttempts = maxAttempts),
        actionName = "assertDisplayedWithRetry($tag)"
    ) {
        waitForIdle()
        onNodeWithTag(tag).assertIsDisplayed()
    }
}

/**
 * Wait for a bottom sheet or dialog to appear.
 * Uses appropriate timing for sheet animations.
 *
 * @param sheetTag The test tag of the sheet content
 * @param animationDelayMs Expected animation duration to complete
 */
fun ComposeContentTestRule.waitForSheet(
    sheetTag: String,
    animationDelayMs: Long = 350,
    timeoutMillis: Long = 5000
) {
    // Wait for compose animations to settle
    waitForIdle()

    // Small delay for sheet animation to start
    Thread.sleep(animationDelayMs)

    // Wait for sheet content to appear
    waitUntilWithBackoff(
        tag = sheetTag,
        timeoutMillis = timeoutMillis,
        initialPollMs = 50,
        maxPollMs = 200
    )
}

/**
 * Wait for sheet content by text.
 */
fun ComposeContentTestRule.waitForSheetText(
    text: String,
    animationDelayMs: Long = 350,
    timeoutMillis: Long = 5000
) {
    waitForIdle()
    Thread.sleep(animationDelayMs)
    waitUntilTextWithBackoff(text, timeoutMillis, initialPollMs = 50, maxPollMs = 200)
}
