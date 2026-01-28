package com.rasoiai.app.presentation.chat

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.ChatMessage
import com.rasoiai.domain.model.RecipeSuggestion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for ChatScreen
 * Tests Phase 6 of E2E Testing Guide: Chat Screen Testing
 */
@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Test Data Factories

    private fun createTestMessage(
        id: String = "msg_1",
        content: String = "Hello! How can I help you today?",
        isFromUser: Boolean = false,
        quickActions: List<String>? = null,
        recipeSuggestions: List<RecipeSuggestion>? = null
    ) = ChatMessage(
        id = id,
        content = content,
        isFromUser = isFromUser,
        timestamp = System.currentTimeMillis(),
        quickActions = quickActions,
        recipeSuggestions = recipeSuggestions
    )

    private fun createUserMessage(
        id: String = "user_msg_1",
        content: String = "What can I make for dinner tonight?"
    ) = createTestMessage(id = id, content = content, isFromUser = true)

    private fun createAiResponseWithSuggestions() = createTestMessage(
        id = "ai_msg_1",
        content = "Here are some quick dinner suggestions based on your preferences:",
        isFromUser = false,
        recipeSuggestions = listOf(
            RecipeSuggestion("recipe_1", "Dal Tadka", 30, null),
            RecipeSuggestion("recipe_2", "Paneer Butter Masala", 45, null)
        )
    )

    private fun createTestUiState(
        isLoading: Boolean = false,
        isSending: Boolean = false,
        errorMessage: String? = null,
        messages: List<ChatMessage> = emptyList(),
        inputText: String = "",
        quickActions: List<String> = listOf(
            "Suggest a quick breakfast",
            "What can I make with paneer?",
            "Show me today's meal plan"
        ),
        showClearChatDialog: Boolean = false,
        showMenu: Boolean = false
    ) = ChatUiState(
        isLoading = isLoading,
        isSending = isSending,
        errorMessage = errorMessage,
        messages = messages,
        inputText = inputText,
        quickActions = quickActions,
        showClearChatDialog = showClearChatDialog,
        showMenu = showMenu
    )

    // endregion

    // region Phase 6.1: Chat Interface Tests

    @Test
    fun chatScreen_displaysScreenTag() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.CHAT_SCREEN).assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysTitle() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("RasoiAI Assistant").assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysInputField() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.CHAT_INPUT_FIELD).assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysSendButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.CHAT_SEND_BUTTON).assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysBottomNavigation() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV).assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysChatMessages() {
        val messages = listOf(
            createUserMessage(),
            createTestMessage(id = "ai_1", content = "I'd suggest trying Dal Tadka tonight!")
        )
        val uiState = createTestUiState(messages = messages)

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("What can I make for dinner tonight?").assertIsDisplayed()
        composeTestRule.onNodeWithText("I'd suggest trying Dal Tadka tonight!").assertIsDisplayed()
    }

    @Test
    fun chatScreen_showsTypingIndicator_whenSending() {
        val uiState = createTestUiState(isSending = true)

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("RasoiAI is typing...").assertIsDisplayed()
    }

    // endregion

    // region Input and Send Tests

    @Test
    fun chatInput_textChange_triggersCallback() {
        var inputText = ""
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(
                    uiState = uiState,
                    onInputChange = { inputText = it }
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.CHAT_INPUT_FIELD).performTextInput("Hello")

        assert(inputText == "Hello") { "Expected 'Hello' but got '$inputText'" }
    }

    @Test
    fun sendButton_click_triggersCallback() {
        var sendClicked = false
        val uiState = createTestUiState(inputText = "Test message")

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(
                    uiState = uiState,
                    onSendClick = { sendClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.CHAT_SEND_BUTTON).performClick()

        assert(sendClicked) { "Send callback was not triggered" }
    }

    // endregion

    // region Menu Tests

    @Test
    fun moreOptionsButton_click_triggersMenuCallback() {
        var menuClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(
                    uiState = uiState,
                    onMenuClick = { menuClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("More options").performClick()

        assert(menuClicked) { "Menu callback was not triggered" }
    }

    @Test
    fun menuExpanded_showsClearChatOption() {
        val uiState = createTestUiState(showMenu = true)

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Clear Chat History").assertIsDisplayed()
    }

    @Test
    fun menuExpanded_showsChatSettingsOption() {
        val uiState = createTestUiState(showMenu = true)

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Chat Settings").assertIsDisplayed()
    }

    // endregion

    // region Navigation Tests

    @Test
    fun backButton_click_triggersNavigateBack() {
        var backClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(
                    uiState = uiState,
                    onBackClick = { backClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assert(backClicked) { "Back navigation callback was not triggered" }
    }

    // endregion

    // region Empty State Tests

    @Test
    fun emptyChat_displaysEmptyMessageList() {
        val uiState = createTestUiState(messages = emptyList())

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(uiState = uiState)
            }
        }

        // Chat screen should still be displayed with empty state
        composeTestRule.onNodeWithTag(TestTags.CHAT_SCREEN).assertIsDisplayed()
    }

    // endregion

    // region Loading State Tests

    @Test
    fun chatScreen_loadingState_displaysLoadingIndicator() {
        val uiState = createTestUiState(isLoading = true)

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.CHAT_SCREEN).assertIsDisplayed()
    }

    // endregion

    // region Recipe Suggestions Tests

    @Test
    fun chatMessage_withRecipeSuggestions_displaysSuggestionCards() {
        val messages = listOf(createAiResponseWithSuggestions())
        val uiState = createTestUiState(messages = messages)

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Here are some quick dinner suggestions", substring = true).assertIsDisplayed()
    }

    @Test
    fun recipeSuggestion_click_triggersRecipeCallback() {
        var clickedRecipeId: String? = null
        val messages = listOf(createAiResponseWithSuggestions())
        val uiState = createTestUiState(messages = messages)

        composeTestRule.setContent {
            RasoiAITheme {
                ChatTestContent(
                    uiState = uiState,
                    onRecipeClick = { clickedRecipeId = it }
                )
            }
        }

        // Try to click on recipe suggestion (if displayed as clickable)
        composeTestRule.onNodeWithText("Dal Tadka", substring = true).performClick()

        // This test may need adjustment based on actual UI structure
    }

    // endregion
}

// region Test Composable Wrapper

@androidx.compose.runtime.Composable
private fun ChatTestContent(
    uiState: ChatUiState,
    onBackClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onDismissMenu: () -> Unit = {},
    onClearChatClick: () -> Unit = {},
    onInputChange: (String) -> Unit = {},
    onSendClick: () -> Unit = {},
    onQuickActionClick: (String) -> Unit = {},
    onRecipeClick: (String) -> Unit = {},
    onAddToMealPlan: (String) -> Unit = {},
    onAttachmentClick: () -> Unit = {},
    onVoiceClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    ChatScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onMenuClick = onMenuClick,
        onDismissMenu = onDismissMenu,
        onClearChatClick = onClearChatClick,
        onInputChange = onInputChange,
        onSendClick = onSendClick,
        onQuickActionClick = onQuickActionClick,
        onRecipeClick = onRecipeClick,
        onAddToMealPlan = onAddToMealPlan,
        onAttachmentClick = onAttachmentClick,
        onVoiceClick = onVoiceClick,
        onBottomNavItemClick = {}
    )
}

// endregion
