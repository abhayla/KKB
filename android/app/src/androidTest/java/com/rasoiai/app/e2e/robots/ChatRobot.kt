package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.e2e.base.waitUntilNodeWithTextExists
import com.rasoiai.app.presentation.common.TestTags

/**
 * Robot for Chat screen interactions.
 * Handles chat interface and recipe suggestions.
 */
class ChatRobot(private val composeTestRule: ComposeContentTestRule) {

    /**
     * Wait for chat screen to be displayed.
     */
    fun waitForChatScreen(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(TestTags.CHAT_SCREEN, timeoutMillis)
    }

    /**
     * Assert chat screen is displayed.
     */
    fun assertChatScreenDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.CHAT_SCREEN).assertIsDisplayed()
    }

    // ===================== Message Input =====================

    /**
     * Assert chat input field is displayed.
     */
    fun assertInputFieldDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.CHAT_INPUT_FIELD).assertIsDisplayed()
    }

    /**
     * Assert send button is displayed.
     */
    fun assertSendButtonDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.CHAT_SEND_BUTTON).assertIsDisplayed()
    }

    /**
     * Type message in input field.
     */
    fun typeMessage(message: String) = apply {
        composeTestRule.onNodeWithTag(TestTags.CHAT_INPUT_FIELD).performTextInput(message)
        composeTestRule.waitForIdle()
    }

    /**
     * Tap send button.
     */
    fun tapSend() = apply {
        composeTestRule.onNodeWithTag(TestTags.CHAT_SEND_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Send a complete message.
     */
    fun sendMessage(message: String) = apply {
        typeMessage(message)
        tapSend()
    }

    /**
     * Assert send button is enabled.
     */
    fun assertSendButtonEnabled() = apply {
        composeTestRule.onNodeWithTag(TestTags.CHAT_SEND_BUTTON).assertIsEnabled()
    }

    // ===================== Messages =====================

    /**
     * Assert user message is displayed.
     */
    fun assertUserMessageDisplayed(message: String) = apply {
        composeTestRule.onNodeWithText(message, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Wait for AI response.
     */
    fun waitForAIResponse(timeoutMillis: Long = 10000) = apply {
        // Wait for typing indicator to appear and disappear
        // Or wait for a new message from AI
        composeTestRule.waitForIdle()
        Thread.sleep(2000) // Allow time for response
    }

    /**
     * Assert AI response contains text.
     */
    fun assertAIResponseContains(text: String) = apply {
        composeTestRule.waitUntilNodeWithTextExists(text, 10000)
        composeTestRule.onNodeWithText(text, substring = true).assertIsDisplayed()
    }

    /**
     * Assert typing indicator is displayed.
     */
    fun assertTypingIndicatorDisplayed() = apply {
        composeTestRule.onNodeWithText("Typing", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert no visible chat message contains the given keyword.
     * Checks all rendered text nodes in the chat for the keyword.
     */
    fun assertMessageNotContaining(keyword: String) = apply {
        val nodes = composeTestRule.onAllNodesWithText(keyword, substring = true, ignoreCase = true)
            .fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
            throw AssertionError("Found '$keyword' in ${nodes.size} chat message(s)")
        }
    }

    // ===================== Recipe Suggestions =====================

    /**
     * Assert recipe suggestion card is displayed.
     */
    fun assertRecipeSuggestionDisplayed(recipeName: String) = apply {
        composeTestRule.onNodeWithText(recipeName, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Tap on recipe suggestion card.
     */
    fun tapRecipeSuggestion(recipeName: String) = apply {
        composeTestRule.onNodeWithText(recipeName, substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Quick Actions =====================

    /**
     * Assert quick action chip is displayed.
     */
    fun assertQuickActionDisplayed(actionText: String) = apply {
        composeTestRule.onNodeWithText(actionText, substring = true).assertIsDisplayed()
    }

    /**
     * Tap quick action chip.
     */
    fun tapQuickAction(actionText: String) = apply {
        composeTestRule.onNodeWithText(actionText, substring = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Empty State =====================

    /**
     * Assert empty state or welcome message is displayed.
     */
    fun assertEmptyStateDisplayed() = apply {
        // Look for welcome message or empty state text
        try {
            composeTestRule.onNodeWithText("Welcome", substring = true, ignoreCase = true)
                .assertIsDisplayed()
        } catch (e: Exception) {
            composeTestRule.onNodeWithText("How can I help", substring = true, ignoreCase = true)
                .assertIsDisplayed()
        }
    }

    // ===================== Scroll =====================

    /**
     * Scroll to bottom of chat.
     */
    fun scrollToBottom() = apply {
        // Chat usually auto-scrolls, but we can force it
        composeTestRule.onNodeWithTag(TestTags.CHAT_INPUT_FIELD).performScrollTo()
    }

    /**
     * Scroll to top of chat.
     */
    fun scrollToTop() = apply {
        composeTestRule.onNodeWithTag("${TestTags.CHAT_MESSAGE_PREFIX}0").performScrollTo()
    }
}
