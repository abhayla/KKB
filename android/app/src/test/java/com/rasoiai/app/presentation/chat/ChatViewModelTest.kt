package com.rasoiai.app.presentation.chat

import app.cash.turbine.test
import com.rasoiai.domain.model.ChatMessage
import com.rasoiai.domain.repository.ChatRepository
import com.rasoiai.domain.repository.MealPlanRepository
import io.mockk.coEvery
import io.mockk.coVerify
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

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockChatRepository: ChatRepository
    private lateinit var mockMealPlanRepository: MealPlanRepository

    private val testMessages = listOf(
        ChatMessage(
            id = "msg-1",
            content = "Hello! How can I help you today?",
            isFromUser = false,
            timestamp = System.currentTimeMillis() - 300000
        ),
        ChatMessage(
            id = "msg-2",
            content = "I want a quick dinner recipe",
            isFromUser = true,
            timestamp = System.currentTimeMillis() - 240000
        ),
        ChatMessage(
            id = "msg-3",
            content = "Here's a quick Paneer Bhurji recipe...",
            isFromUser = false,
            timestamp = System.currentTimeMillis() - 180000
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
        mockMealPlanRepository = mockk(relaxed = true)
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
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.uiState.test {
                val initialState = awaitItem()
                assertTrue(initialState.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("After loading, messages should be populated")
        fun `after loading messages should be populated`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

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
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

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
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateInputText("Hello AI")

                val state = awaitItem()
                assertEquals("Hello AI", state.inputText)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("sendMessage should clear input and send message")
        fun `sendMessage should clear input and send message`() = runTest {
            val responseMessage = ChatMessage(id = "msg-response", content = "Test response", isFromUser = false, timestamp = System.currentTimeMillis())
            coEvery { mockChatRepository.sendMessage(any()) } returns Result.success(responseMessage)

            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateInputText("Test message")
                awaitItem()

                viewModel.sendMessage()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals("", state.inputText)
                assertFalse(state.isSending) // Sending completed
                cancelAndIgnoreRemainingEvents()
            }

            coVerify { mockChatRepository.sendMessage("Test message") }
        }

        @Test
        @DisplayName("sendMessage with blank text should do nothing")
        fun `sendMessage with blank text should do nothing`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

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
            val responseMessage = ChatMessage(id = "msg-response", content = "Here is a suggestion", isFromUser = false, timestamp = System.currentTimeMillis())
            coEvery { mockChatRepository.sendMessage(any()) } returns Result.success(responseMessage)

            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.onQuickActionClick("Suggest a quick breakfast")
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isSending) // Sending completed
                cancelAndIgnoreRemainingEvents()
            }

            coVerify { mockChatRepository.sendMessage("Suggest a quick breakfast") }
        }
    }

    @Nested
    @DisplayName("Menu Actions")
    inner class MenuActions {

        @Test
        @DisplayName("toggleMenu should toggle menu visibility")
        fun `toggleMenu should toggle menu visibility`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

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
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

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
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.toggleMenu()
                awaitItem() // Menu shown

                viewModel.showClearChatDialog()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertTrue(state.showClearChatDialog)
                assertFalse(state.showMenu)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissClearChatDialog should hide dialog")
        fun `dismissClearChatDialog should hide dialog`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

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

            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showClearChatDialog()
                awaitItem() // Dialog shown

                viewModel.clearChatHistory()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
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
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateBack()
                val event = awaitItem()
                assertEquals(ChatNavigationEvent.NavigateBack, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToHome should emit home event")
        fun `navigateToHome should emit home event`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateToHome()
                val event = awaitItem()
                assertEquals(ChatNavigationEvent.NavigateToHome, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToRecipeDetail should emit recipe detail event")
        fun `navigateToRecipeDetail should emit recipe detail event`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateToRecipeDetail("recipe-123")
                val event = awaitItem()
                assertTrue(event is ChatNavigationEvent.NavigateToRecipeDetail)
                assertEquals("recipe-123", (event as ChatNavigationEvent.NavigateToRecipeDetail).recipeId)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToGrocery should emit grocery event")
        fun `navigateToGrocery should emit grocery event`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateToGrocery()
                val event = awaitItem()
                assertEquals(ChatNavigationEvent.NavigateToGrocery, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToFavorites should emit favorites event")
        fun `navigateToFavorites should emit favorites event`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateToFavorites()
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
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.onVoiceButtonClick()

                val state = awaitItem()
                assertTrue(state.errorMessage?.contains("Voice input") == true)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onAttachmentButtonClick should show image source dialog")
        fun `onAttachmentButtonClick should show image source dialog`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.onAttachmentButtonClick()

                val state = awaitItem()
                assertTrue(state.showImageSourceDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Image Attachment")
    inner class ImageAttachment {

        @Test
        @DisplayName("dismissImageSourceDialog should hide dialog")
        fun `dismissImageSourceDialog should hide dialog`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.onAttachmentButtonClick()
                val dialogShown = awaitItem()
                assertTrue(dialogShown.showImageSourceDialog)

                viewModel.dismissImageSourceDialog()
                val dialogHidden = awaitItem()
                assertFalse(dialogHidden.showImageSourceDialog)

                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("clearSelectedImage should reset image state")
        fun `clearSelectedImage should reset image state`() = runTest {
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.clearSelectedImage()

                val state = viewModel.uiState.value
                assertNull(state.selectedImageUri)
                assertFalse(state.isUploadingImage)

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
            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.clearError()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertNull(state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("sendMessage failure should show error")
        fun `sendMessage failure should show error`() = runTest {
            coEvery { mockChatRepository.sendMessage(any()) } returns Result.failure(Exception("Network error"))

            val viewModel = ChatViewModel(mockChatRepository, mockMealPlanRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateInputText("Test")
                awaitItem()

                viewModel.sendMessage()
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isSending)
                assertTrue(state.errorMessage?.contains("Failed to send") == true)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
