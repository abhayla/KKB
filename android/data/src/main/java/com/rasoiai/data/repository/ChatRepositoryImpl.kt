package com.rasoiai.data.repository

import com.rasoiai.data.local.dao.ChatDao
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.domain.model.ChatMessage
import com.rasoiai.domain.model.RecipeSuggestion
import com.rasoiai.domain.repository.ChatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of ChatRepository with offline-first architecture.
 *
 * Strategy:
 * - All messages stored locally in Room (single source of truth)
 * - AI responses are generated locally for now (backend Claude API integration planned)
 * - Messages persist across app sessions
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao
) : ChatRepository {

    override fun getMessages(): Flow<List<ChatMessage>> {
        return chatDao.getAllMessages().map { entities ->
            if (entities.isEmpty()) {
                // Insert welcome message if no messages exist
                val welcomeMessage = createWelcomeMessage()
                chatDao.insertMessage(welcomeMessage.toEntity())
                listOf(welcomeMessage)
            } else {
                entities.map { it.toDomain() }
            }
        }
    }

    override suspend fun sendMessage(content: String): Result<ChatMessage> {
        return try {
            // Add user message
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = content,
                isFromUser = true,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(userMessage.toEntity())
            Timber.d("User message saved: ${userMessage.id}")

            // Simulate AI thinking delay
            delay(800)

            // Generate AI response based on user message
            val aiResponse = generateAiResponse(content.lowercase())
            chatDao.insertMessage(aiResponse.toEntity())
            Timber.d("AI response saved: ${aiResponse.id}")

            Result.success(aiResponse)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send message")
            Result.failure(e)
        }
    }

    override suspend fun clearHistory(): Result<Unit> {
        return try {
            chatDao.clearAllMessages()
            // Insert welcome message after clearing
            val welcomeMessage = createWelcomeMessage()
            chatDao.insertMessage(welcomeMessage.toEntity())
            Timber.i("Chat history cleared")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear chat history")
            Result.failure(e)
        }
    }

    override fun getQuickActions(): List<String> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 6..10 -> listOf("Quick breakfast", "Healthy start", "Light meal", "Tea-time snack")
            hour in 11..15 -> listOf("Lunch ideas", "Light meal", "Quick recipe", "South Indian")
            hour in 16..20 -> listOf("Dinner suggestions", "Family meal", "Something special", "Quick dinner")
            else -> listOf("Light snack", "Quick bite", "Healthy option", "Simple recipe")
        }
    }

    private fun createWelcomeMessage(): ChatMessage {
        return ChatMessage(
            id = "welcome",
            content = "Hi! I'm your AI cooking assistant. I can help you find recipes, suggest meals, and answer cooking questions. How can I help you today?",
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            quickActions = getQuickActions()
        )
    }

    private fun generateAiResponse(message: String): ChatMessage {
        return when {
            // Paneer recipes
            message.contains("paneer") -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "Great choice! Paneer is so versatile. Here are some delicious paneer recipes you might enjoy:",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                recipeSuggestions = listOf(
                    RecipeSuggestion("palak-paneer", "Palak Paneer", 40, null),
                    RecipeSuggestion("paneer-butter-masala", "Paneer Butter Masala", 40, null),
                    RecipeSuggestion("malai-kofta", "Malai Kofta", 60, null)
                )
            )

            // Breakfast recipes
            message.contains("breakfast") || message.contains("morning") -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "Looking for breakfast ideas? Here are some popular Indian breakfast options:",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                recipeSuggestions = listOf(
                    RecipeSuggestion("paratha", "Aloo Paratha", 40, null),
                    RecipeSuggestion("idli", "Idli Sambar", 50, null),
                    RecipeSuggestion("dosa", "Masala Dosa", 35, null)
                )
            )

            // Quick recipes
            message.contains("quick") || message.contains("fast") || message.contains("easy") -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "Short on time? No worries! These recipes can be made quickly:",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                recipeSuggestions = listOf(
                    RecipeSuggestion("dal-tadka", "Dal Tadka", 35, null),
                    RecipeSuggestion("dosa", "Masala Dosa", 35, null),
                    RecipeSuggestion("idli", "Idli Sambar", 50, null)
                )
            )

            // Dinner recipes
            message.contains("dinner") || message.contains("evening") -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "Here are some wonderful dinner recipes for you and your family:",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                recipeSuggestions = listOf(
                    RecipeSuggestion("rajma", "Rajma Masala", 60, null),
                    RecipeSuggestion("biryani", "Veg Biryani", 75, null),
                    RecipeSuggestion("malai-kofta", "Malai Kofta", 60, null)
                )
            )

            // Lunch recipes
            message.contains("lunch") -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "Perfect! Here are some satisfying lunch options:",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                recipeSuggestions = listOf(
                    RecipeSuggestion("dal-tadka", "Dal Tadka", 35, null),
                    RecipeSuggestion("rajma", "Rajma Masala", 60, null),
                    RecipeSuggestion("chole-bhature", "Chole Bhature", 70, null)
                )
            )

            // South Indian
            message.contains("south") || message.contains("dosa") || message.contains("idli") -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "Love South Indian food! Here are some authentic recipes:",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                recipeSuggestions = listOf(
                    RecipeSuggestion("dosa", "Masala Dosa", 35, null),
                    RecipeSuggestion("idli", "Idli Sambar", 50, null),
                    RecipeSuggestion("biryani", "Veg Biryani", 75, null)
                )
            )

            // North Indian
            message.contains("north") || message.contains("punjabi") -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "North Indian cuisine is rich and flavorful! Try these:",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                recipeSuggestions = listOf(
                    RecipeSuggestion("paratha", "Aloo Paratha", 40, null),
                    RecipeSuggestion("chole-bhature", "Chole Bhature", 70, null),
                    RecipeSuggestion("dal-tadka", "Dal Tadka", 35, null)
                )
            )

            // Dal/Lentils
            message.contains("dal") || message.contains("lentil") -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "Dal is comfort food at its best! Here are some delicious options:",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                recipeSuggestions = listOf(
                    RecipeSuggestion("dal-tadka", "Dal Tadka", 35, null),
                    RecipeSuggestion("rajma", "Rajma Masala", 60, null)
                )
            )

            // Vegetarian
            message.contains("vegetarian") || message.contains("veg") -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "All our recipes are vegetarian! Here are some favorites:",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                recipeSuggestions = listOf(
                    RecipeSuggestion("palak-paneer", "Palak Paneer", 40, null),
                    RecipeSuggestion("biryani", "Veg Biryani", 75, null),
                    RecipeSuggestion("malai-kofta", "Malai Kofta", 60, null)
                )
            )

            // Healthy
            message.contains("healthy") || message.contains("light") -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "Looking for healthy options? These are nutritious and delicious:",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                recipeSuggestions = listOf(
                    RecipeSuggestion("dal-tadka", "Dal Tadka", 35, null),
                    RecipeSuggestion("idli", "Idli Sambar", 50, null),
                    RecipeSuggestion("palak-paneer", "Palak Paneer", 40, null)
                )
            )

            // Special/Celebration
            message.contains("special") || message.contains("celebration") || message.contains("party") -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "For special occasions, these dishes are sure to impress:",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                recipeSuggestions = listOf(
                    RecipeSuggestion("biryani", "Veg Biryani", 75, null),
                    RecipeSuggestion("malai-kofta", "Malai Kofta", 60, null),
                    RecipeSuggestion("paneer-butter-masala", "Paneer Butter Masala", 40, null)
                )
            )

            // Hello/Greetings
            message.contains("hello") || message.contains("hi") || message.contains("hey") -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "Hello! I'm here to help you with meal planning and recipes. What would you like to cook today?",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                quickActions = getQuickActions()
            )

            // Thanks
            message.contains("thank") -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "You're welcome! Happy cooking! Let me know if you need any more help.",
                isFromUser = false,
                timestamp = System.currentTimeMillis()
            )

            // Help
            message.contains("help") -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "I can help you with:\n\n• Finding recipes (just tell me what you're craving!)\n• Suggesting meals based on time of day\n• Recommending dishes by cuisine type\n• Quick recipe ideas when you're short on time\n\nJust ask me anything about cooking!",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                quickActions = getQuickActions()
            )

            // Default response
            else -> ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "I'd be happy to help you find the perfect recipe! Here are some popular dishes you might enjoy:",
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                recipeSuggestions = listOf(
                    RecipeSuggestion("dal-tadka", "Dal Tadka", 35, null),
                    RecipeSuggestion("palak-paneer", "Palak Paneer", 40, null),
                    RecipeSuggestion("paratha", "Aloo Paratha", 40, null)
                )
            )
        }
    }
}
