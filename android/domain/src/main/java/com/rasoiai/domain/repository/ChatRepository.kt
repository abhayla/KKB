package com.rasoiai.domain.repository

import com.rasoiai.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Repository for chat message operations.
 */
interface ChatRepository {
    /**
     * Get all chat messages as a flow.
     */
    fun getMessages(): Flow<List<ChatMessage>>

    /**
     * Send a user message and get AI response.
     */
    suspend fun sendMessage(content: String): Result<ChatMessage>

    /**
     * Send an image message for food analysis.
     * The image will be compressed and sent to the backend for analysis.
     *
     * @param imageUriString String representation of the image URI to analyze
     * @return AI response with food analysis
     */
    suspend fun sendImageMessage(imageUriString: String): Result<ChatMessage>

    /**
     * Clear all chat history.
     */
    suspend fun clearHistory(): Result<Unit>

    /**
     * Get quick action suggestions based on time of day.
     */
    fun getQuickActions(): List<String>
}
