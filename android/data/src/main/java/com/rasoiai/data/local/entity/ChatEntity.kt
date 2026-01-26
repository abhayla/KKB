package com.rasoiai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for chat messages.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val quickActionsJson: String? = null, // JSON array of strings
    val recipeSuggestionsJson: String? = null // JSON array of RecipeSuggestion
)
