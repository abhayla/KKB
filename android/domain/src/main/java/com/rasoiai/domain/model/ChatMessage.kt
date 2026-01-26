package com.rasoiai.domain.model

/**
 * Represents a chat message in the AI assistant conversation.
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val quickActions: List<String>? = null,
    val recipeSuggestions: List<RecipeSuggestion>? = null
)

/**
 * Recipe suggestion shown in chat messages.
 */
data class RecipeSuggestion(
    val recipeId: String,
    val recipeName: String,
    val cookTimeMinutes: Int,
    val imageUrl: String?
)
