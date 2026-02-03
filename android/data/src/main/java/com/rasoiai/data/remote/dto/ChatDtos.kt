package com.rasoiai.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request for sending an image message to the chat API.
 */
data class ChatImageRequest(
    val message: String = "Please analyze this food image",
    @SerializedName("image_base64")
    val imageBase64: String,
    @SerializedName("media_type")
    val mediaType: String = "image/jpeg"
)

/**
 * Response from the chat image analysis API.
 */
data class ChatImageResponse(
    val message: ChatMessageDto,
    @SerializedName("has_recipe_suggestions")
    val hasRecipeSuggestions: Boolean = false,
    @SerializedName("recipe_ids")
    val recipeIds: List<String> = emptyList()
)

/**
 * Single chat message from the API.
 */
data class ChatMessageDto(
    val id: String,
    val role: String,
    val content: String,
    @SerializedName("message_type")
    val messageType: String = "text",
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("recipe_suggestions")
    val recipeSuggestions: List<String>? = null
)
