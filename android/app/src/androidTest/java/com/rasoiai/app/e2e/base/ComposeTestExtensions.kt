package com.rasoiai.app.e2e.base

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
