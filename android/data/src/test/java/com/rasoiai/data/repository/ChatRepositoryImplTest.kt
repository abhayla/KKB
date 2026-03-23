package com.rasoiai.data.repository

import android.content.Context
import app.cash.turbine.test
import com.rasoiai.data.local.dao.ChatDao
import com.rasoiai.data.local.entity.ChatMessageEntity
import com.rasoiai.data.remote.api.RasoiApiService
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockChatDao: ChatDao
    private lateinit var mockApiService: RasoiApiService
    private lateinit var mockContext: Context
    private lateinit var repository: ChatRepositoryImpl

    private val testMessageEntity = ChatMessageEntity(
        id = "msg-1",
        content = "Hello!",
        isFromUser = true,
        timestamp = System.currentTimeMillis(),
        quickActionsJson = null,
        recipeSuggestionsJson = null
    )

    private val testAiMessageEntity = ChatMessageEntity(
        id = "msg-2",
        content = "Hi! How can I help you today?",
        isFromUser = false,
        timestamp = System.currentTimeMillis(),
        quickActionsJson = "[\"Quick breakfast\",\"Healthy start\"]",
        recipeSuggestionsJson = null
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockChatDao = mockk(relaxed = true)
        mockApiService = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)

        repository = ChatRepositoryImpl(
            chatDao = mockChatDao,
            apiService = mockApiService,
            context = mockContext
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("getMessages")
    inner class GetMessages {

        @Test
        @DisplayName("Should return messages from DAO")
        fun `should return messages from DAO`() = runTest {
            // Given
            every { mockChatDao.getAllMessages() } returns flowOf(listOf(testMessageEntity, testAiMessageEntity))

            // When & Then
            repository.getMessages().test {
                val messages = awaitItem()

                assertEquals(2, messages.size)
                assertEquals("Hello!", messages.first().content)
                assertTrue(messages.first().isFromUser)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should insert welcome message when empty")
        fun `should insert welcome message when empty`() = runTest {
            // Given
            every { mockChatDao.getAllMessages() } returns flowOf(emptyList())

            // When & Then
            repository.getMessages().test {
                val messages = awaitItem()

                assertEquals(1, messages.size)
                assertFalse(messages.first().isFromUser)
                assertTrue(messages.first().content.contains("AI cooking assistant"))
                coVerify { mockChatDao.insertMessage(any()) }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("sendMessage")
    inner class SendMessage {

        @Test
        @DisplayName("Should save user message and return AI response")
        fun `should save user message and return AI response`() = runTest {
            // When
            val result = repository.sendMessage("I want paneer recipes")

            // Then
            assertTrue(result.isSuccess)
            assertFalse(result.getOrNull()?.isFromUser ?: true)
            assertNotNull(result.getOrNull()?.content)

            // Verify both user and AI messages were saved
            coVerify(exactly = 2) { mockChatDao.insertMessage(any()) }
        }

        @Test
        @DisplayName("Should return recipe suggestions for paneer query")
        fun `should return recipe suggestions for paneer query`() = runTest {
            // When
            val result = repository.sendMessage("I want paneer")

            // Then
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.recipeSuggestions?.isNotEmpty() ?: false)
            assertTrue(result.getOrNull()?.content?.contains("paneer", ignoreCase = true) ?: false)
        }

        @Test
        @DisplayName("Should return recipe suggestions for breakfast query")
        fun `should return recipe suggestions for breakfast query`() = runTest {
            // When
            val result = repository.sendMessage("What should I have for breakfast?")

            // Then
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.recipeSuggestions?.isNotEmpty() ?: false)
            assertTrue(result.getOrNull()?.content?.contains("breakfast", ignoreCase = true) ?: false)
        }

        @Test
        @DisplayName("Should return quick actions for greeting")
        fun `should return quick actions for greeting`() = runTest {
            // When
            val result = repository.sendMessage("Hello!")

            // Then
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.quickActions?.isNotEmpty() ?: false)
        }

        @Test
        @DisplayName("Should return help content for help query")
        fun `should return help content for help query`() = runTest {
            // When
            val result = repository.sendMessage("Can you help me?")

            // Then
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.content?.contains("help", ignoreCase = true) ?: false)
        }
    }

    @Nested
    @DisplayName("clearHistory")
    inner class ClearHistory {

        @Test
        @DisplayName("Should clear all messages and insert welcome")
        fun `should clear all messages and insert welcome`() = runTest {
            // When
            val result = repository.clearHistory()

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockChatDao.clearAllMessages() }
            coVerify { mockChatDao.insertMessage(match { !it.isFromUser }) }
        }
    }

    @Nested
    @DisplayName("getQuickActions")
    inner class GetQuickActions {

        @Test
        @DisplayName("Should return time-appropriate quick actions")
        fun `should return time appropriate quick actions`() {
            // When
            val actions = repository.getQuickActions()

            // Then
            assertTrue(actions.isNotEmpty())
            assertTrue(actions.size >= 4)
        }
    }

    @Nested
    @DisplayName("Network Timeout and Exception Handling")
    inner class NetworkTimeoutAndExceptionHandling {

        @Test
        @DisplayName("Should return error when sendMessage encounters SocketTimeoutException from DAO")
        fun `should return error when sendMessage encounters SocketTimeoutException`() = runTest {
            // Given — DAO throws (simulating underlying storage/network failure)
            coEvery { mockChatDao.insertMessage(any()) } throws java.net.SocketTimeoutException("timeout")

            // When
            val result = repository.sendMessage("Hello")

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is java.net.SocketTimeoutException)
        }

        @Test
        @DisplayName("Should return error when sendImageMessage encounters IOException")
        fun `should return error when sendImageMessage encounters IOException`() = runTest {
            // Given — API throws on image upload
            coEvery { mockApiService.sendImageChatMessage(any()) } throws java.io.IOException("Network unreachable")

            // When
            val result = repository.sendImageMessage("content://media/image/1")

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is java.io.IOException || result.exceptionOrNull() is Exception)
        }
    }
}
