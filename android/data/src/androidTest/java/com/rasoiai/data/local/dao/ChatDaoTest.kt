package com.rasoiai.data.local.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.rasoiai.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatDaoTest : BaseDaoTest() {
    private val chatDao: ChatDao get() = database.chatDao()

    private val baseTimestamp = System.currentTimeMillis()

    private val testMessage = ChatMessageEntity(
        id = "msg-1",
        content = "Hello, I need a recipe suggestion!",
        isFromUser = true,
        timestamp = baseTimestamp,
        quickActionsJson = null,
        recipeSuggestionsJson = null
    )

    @Test
    fun insertMessage_andGetAll_returnsMessages() = runTest {
        // Given
        chatDao.insertMessage(testMessage)

        // When & Then
        chatDao.getAllMessages().test {
            val messages = awaitItem()
            assertEquals(1, messages.size)
            assertEquals("Hello, I need a recipe suggestion!", messages.first().content)
            assertTrue(messages.first().isFromUser)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertMessages_returnsMessagesSortedByTimestamp() = runTest {
        // Given
        val messages = listOf(
            testMessage.copy(id = "msg-1", content = "First", timestamp = baseTimestamp),
            testMessage.copy(id = "msg-2", content = "Third", timestamp = baseTimestamp + 2000),
            testMessage.copy(id = "msg-3", content = "Second", timestamp = baseTimestamp + 1000)
        )
        chatDao.insertMessages(messages)

        // When & Then
        chatDao.getAllMessages().test {
            val result = awaitItem()
            assertEquals(3, result.size)
            // Should be sorted by timestamp ASC
            assertEquals("First", result[0].content)
            assertEquals("Second", result[1].content)
            assertEquals("Third", result[2].content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getRecentMessages_returnsLimitedMessagesSortedDesc() = runTest {
        // Given
        val messages = (1..10).map { i ->
            testMessage.copy(
                id = "msg-$i",
                content = "Message $i",
                timestamp = baseTimestamp + (i * 1000)
            )
        }
        chatDao.insertMessages(messages)

        // When & Then
        chatDao.getRecentMessages(5).test {
            val result = awaitItem()
            assertEquals(5, result.size)
            // Should be sorted by timestamp DESC (most recent first)
            assertEquals("Message 10", result[0].content)
            assertEquals("Message 9", result[1].content)
            assertEquals("Message 6", result[4].content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getMessageById_returnsCorrectMessage() = runTest {
        // Given
        val messages = listOf(
            testMessage.copy(id = "msg-1", content = "First"),
            testMessage.copy(id = "msg-2", content = "Second")
        )
        chatDao.insertMessages(messages)

        // When
        val result = chatDao.getMessageById("msg-2")

        // Then
        assertNotNull(result)
        assertEquals("Second", result?.content)
    }

    @Test
    fun getMessageById_whenNotExists_returnsNull() = runTest {
        // When
        val result = chatDao.getMessageById("non-existent")

        // Then
        assertNull(result)
    }

    @Test
    fun clearAllMessages_removesAllMessages() = runTest {
        // Given
        chatDao.insertMessages(listOf(
            testMessage.copy(id = "msg-1"),
            testMessage.copy(id = "msg-2"),
            testMessage.copy(id = "msg-3")
        ))

        // When
        chatDao.clearAllMessages()

        // Then
        assertEquals(0, chatDao.getMessageCount())
    }

    @Test
    fun getMessageCount_returnsCorrectCount() = runTest {
        // Given
        assertEquals(0, chatDao.getMessageCount())

        // When
        chatDao.insertMessages(listOf(
            testMessage.copy(id = "msg-1"),
            testMessage.copy(id = "msg-2"),
            testMessage.copy(id = "msg-3")
        ))

        // Then
        assertEquals(3, chatDao.getMessageCount())
    }

    @Test
    fun deleteOldMessages_removesOldMessages() = runTest {
        // Given
        val oldTimestamp = baseTimestamp - 86400000 // 1 day ago
        val messages = listOf(
            testMessage.copy(id = "msg-old", content = "Old message", timestamp = oldTimestamp),
            testMessage.copy(id = "msg-new", content = "New message", timestamp = baseTimestamp)
        )
        chatDao.insertMessages(messages)

        // When - delete messages older than 12 hours ago
        val cutoff = baseTimestamp - 43200000
        chatDao.deleteOldMessages(cutoff)

        // Then
        assertEquals(1, chatDao.getMessageCount())
        assertNull(chatDao.getMessageById("msg-old"))
        assertNotNull(chatDao.getMessageById("msg-new"))
    }

    @Test
    fun insertMessage_withQuickActions_storesJson() = runTest {
        // Given
        val messageWithActions = testMessage.copy(
            quickActionsJson = """["Quick breakfast","Healthy dinner","Vegetarian options"]"""
        )
        chatDao.insertMessage(messageWithActions)

        // When
        val result = chatDao.getMessageById(messageWithActions.id)

        // Then
        assertNotNull(result)
        assertNotNull(result?.quickActionsJson)
        assertTrue(result?.quickActionsJson?.contains("Quick breakfast") == true)
    }

    @Test
    fun insertMessage_withRecipeSuggestions_storesJson() = runTest {
        // Given
        val messageWithSuggestions = testMessage.copy(
            recipeSuggestionsJson = """[{"id":"recipe-1","name":"Paneer Butter Masala"}]"""
        )
        chatDao.insertMessage(messageWithSuggestions)

        // When
        val result = chatDao.getMessageById(messageWithSuggestions.id)

        // Then
        assertNotNull(result)
        assertNotNull(result?.recipeSuggestionsJson)
        assertTrue(result?.recipeSuggestionsJson?.contains("Paneer Butter Masala") == true)
    }

    @Test
    fun insertMessage_aiMessage_isNotFromUser() = runTest {
        // Given
        val aiMessage = testMessage.copy(
            id = "ai-msg-1",
            content = "Here are some suggestions for you!",
            isFromUser = false
        )
        chatDao.insertMessage(aiMessage)

        // When & Then
        chatDao.getAllMessages().test {
            val messages = awaitItem()
            assertEquals(1, messages.size)
            assertEquals(false, messages.first().isFromUser)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertMessage_withConflict_replacesExisting() = runTest {
        // Given
        chatDao.insertMessage(testMessage)

        // When
        val updatedMessage = testMessage.copy(content = "Updated content")
        chatDao.insertMessage(updatedMessage)

        // Then
        val result = chatDao.getMessageById(testMessage.id)
        assertEquals("Updated content", result?.content)
    }

    @Test
    fun conversationFlow_userAndAiMessages() = runTest {
        // Given - simulate a conversation
        val conversation = listOf(
            testMessage.copy(id = "msg-1", content = "Hi!", isFromUser = true, timestamp = baseTimestamp),
            testMessage.copy(id = "msg-2", content = "Hello! How can I help?", isFromUser = false, timestamp = baseTimestamp + 1000),
            testMessage.copy(id = "msg-3", content = "I want paneer recipes", isFromUser = true, timestamp = baseTimestamp + 2000),
            testMessage.copy(id = "msg-4", content = "Here are some paneer recipes...", isFromUser = false, timestamp = baseTimestamp + 3000)
        )
        chatDao.insertMessages(conversation)

        // When & Then
        chatDao.getAllMessages().test {
            val messages = awaitItem()
            assertEquals(4, messages.size)
            // Verify alternating user/AI pattern
            assertTrue(messages[0].isFromUser)
            assertTrue(!messages[1].isFromUser)
            assertTrue(messages[2].isFromUser)
            assertTrue(!messages[3].isFromUser)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
