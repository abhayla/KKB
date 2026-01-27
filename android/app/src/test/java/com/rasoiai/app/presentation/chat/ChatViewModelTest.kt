package com.rasoiai.app.presentation.chat

import app.cash.turbine.test
import com.rasoiai.domain.model.ChatMessage
import com.rasoiai.domain.model.MessageSender
import com.rasoiai.domain.repository.ChatRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockChatRepository: ChatRepository

    private val testMessages = listOf(
        ChatMessage(
            id = "msg-1",
            content = "Hello! How can I help you today?",
            sender = MessageSender.AI,
            timestamp = LocalDateTime.now().minusMinutes(5)
        ),
        ChatMessage(
            id = "msg-2",
            content = "I want a quick dinner recipe",
            sender = MessageSender.USER,
            timestamp = LocalDateTime.now().minusMinutes(4)
        ),
        ChatMessage(
            id = "msg-3",
            content = "Here's a quick Paneer Bhurji recipe...",
            sender = MessageSender.AI,
            timestamp = LocalDateTime.now().minusMinutes(3)
        )
    )

    private val testQuickActions = listOf(
        "Suggest a quick breakfast",
        "What can I make with paneer?",
        "Low-calorie dinner ideas"
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockChatRepository = mockk(relaxed = true)
        coEvery { mockChatRepository.getMessages() } returns flowOf(testMessages)
        every { mockChatRepository.getQuickActions() } returns testQuickActions
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        @DisplayName("Initial state should be loading")
        fun `initial state should be loading`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                val initialState = awaitItem()
                assertTrue(initialState.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("After loading, messages should be populated")
        fun `after loading messages should be populated`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                awaitItem() // Initial with quick actions

                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals(3, state.messages.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Quick actions should be loaded")
        fun `quick actions should be loaded`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(3, state.quickActions.size)
                assertTrue(state.quickActions.contains("Suggest a quick breakfast"))
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Input Handling")
    inner class InputHandling {

        @Test
        @DisplayName("updateInputText should update input text")
        fun `updateInputText should update input text`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateInputText("Hello AI")

                val state = awaitItem()
                assertEquals("Hello AI", state.inputText)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("sendMessage should clear input and set isSending")
        fun `sendMessage should clear input and set isSending`() = runTest {
            coEvery { mockChatRepository.sendMessage(any()) } returns Result.success(Unit)

            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateInputText("Test message")
                awaitItem()

                viewModel.sendMessage()

                val sendingState = awaitItem()
                assertTrue(sendingState.isSending)
                assertEquals("", sendingState.inputText)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("sendMessage with blank text should do nothing")
        fun `sendMessage with blank text should do nothing`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.sendMessage() // Input is empty

                // No state change expected
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onQuickActionClick should send quick action as message")
        fun `onQuickActionClick should send quick action as message`() = runTest {
            coEvery { mockChatRepository.sendMessage(any()) } returns Result.success(Unit)

            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.onQuickActionClick("Suggest a quick breakfast")

                val state = awaitItem()
                assertTrue(state.isSending)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Menu Actions")
    inner class MenuActions {

        @Test
        @DisplayName("toggleMenu should toggle menu visibility")
        fun `toggleMenu should toggle menu visibility`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                awaitItem() // Initial (menu hidden)

                viewModel.toggleMenu()

                val state = awaitItem()
                assertTrue(state.showMenu)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissMenu should hide menu")
        fun `dismissMenu should hide menu`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.toggleMenu()
                awaitItem() // Menu shown

                viewModel.dismissMenu()

                val state = awaitItem()
                assertFalse(state.showMenu)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("showClearChatDialog should show dialog and dismiss menu")
        fun `showClearChatDialog should show dialog and dismiss menu`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.toggleMenu()
                awaitItem() // Menu shown

                viewModel.showClearChatDialog()

                val state = awaitItem()
                assertTrue(state.showClearChatDialog)
                assertFalse(state.showMenu)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissClearChatDialog should hide dialog")
        fun `dismissClearChatDialog should hide dialog`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showClearChatDialog()
                awaitItem() // Dialog shown

                viewModel.dismissClearChatDialog()

                val state = awaitItem()
                assertFalse(state.showClearChatDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("clearChatHistory should clear history")
        fun `clearChatHistory should clear history`() = runTest {
            coEvery { mockChatRepository.clearHistory() } returns Result.success(Unit)

            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showClearChatDialog()
                awaitItem()

                viewModel.clearChatHistory()

                val state = awaitItem()
                assertFalse(state.showClearChatDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        @DisplayName("navigateBack should emit back event")
        fun `navigateBack should emit back event`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.navigateBack()

            viewModel.navigationEvent.test {
                val event = awaitItem()
                assertEquals(ChatNavigationEvent.NavigateBack, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToHome should emit home event")
        fun `navigateToHome should emit home event`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.navigateToHome()

            viewModel.navigationEvent.test {
                val event = awaitItem()
                assertEquals(ChatNavigationEvent.NavigateToHome, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToRecipeDetail should emit recipe detail event")
        fun `navigateToRecipeDetail should emit recipe detail event`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.navigateToRecipeDetail("recipe-123")

            viewModel.navigationEvent.test {
                val event = awaitItem()
                assertTrue(event is ChatNavigationEvent.NavigateToRecipeDetail)
                assertEquals("recipe-123", (event as ChatNavigationEvent.NavigateToRecipeDetail).recipeId)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToGrocery should emit grocery event")
        fun `navigateToGrocery should emit grocery event`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.navigateToGrocery()

            viewModel.navigationEvent.test {
                val event = awaitItem()
                assertEquals(ChatNavigationEvent.NavigateToGrocery, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToFavorites should emit favorites event")
        fun `navigateToFavorites should emit favorites event`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.navigateToFavorites()

            viewModel.navigationEvent.test {
                val event = awaitItem()
                assertEquals(ChatNavigationEvent.NavigateToFavorites, event)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Placeholder Actions")
    inner class PlaceholderActions {

        @Test
        @DisplayName("onVoiceButtonClick should show coming soon message")
        fun `onVoiceButtonClick should show coming soon message`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.onVoiceButtonClick()

                val state = awaitItem()
                assertTrue(state.errorMessage?.contains("Voice input") == true)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onAttachmentButtonClick should show coming soon message")
        fun `onAttachmentButtonClick should show coming soon message`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.onAttachmentButtonClick()

                val state = awaitItem()
                assertTrue(state.errorMessage?.contains("Photo attachment") == true)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("clearError should clear error message")
        fun `clearError should clear error message`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.clearError()

                val state = awaitItem()
                assertNull(state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("sendMessage failure should show error")
        fun `sendMessage failure should show error`() = runTest {
            coEvery { mockChatRepository.sendMessage(any()) } returns Result.failure(Exception("Network error"))

            val viewModel = ChatViewModel(mockChatRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateInputText("Test")
                awaitItem()

                viewModel.sendMessage()
                awaitItem() // Sending

                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertFalse(state.isSending)
                assertTrue(state.errorMessage?.contains("Failed to send") == true)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
