package com.rasoiai.data.repository

import android.content.Context
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.rasoiai.data.local.dao.ChatDao
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.ChatImageRequest
import com.rasoiai.domain.model.ChatMessage
import com.rasoiai.domain.model.RecipeSuggestion
import com.rasoiai.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
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
    private val chatDao: ChatDao,
    private val apiService: RasoiApiService,
    @ApplicationContext private val context: Context
) : ChatRepository {

    companion object {
        private const val MAX_IMAGE_SIZE_BYTES = 1024 * 1024 // 1MB
        private const val INITIAL_QUALITY = 85
        private const val MIN_QUALITY = 20
        private const val QUALITY_STEP = 10
    }

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
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteException) {
            Timber.e(e, "Failed to send message")
            Result.failure(e)
        }
    }

    override suspend fun sendImageMessage(imageUriString: String): Result<ChatMessage> {
        return try {
            val imageUri = Uri.parse(imageUriString)

            // 1. Add user message indicating image was sent
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "[Sent a food photo for analysis]",
                isFromUser = true,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(userMessage.toEntity())
            Timber.d("User image message saved: ${userMessage.id}")

            // 2. Read and compress the image
            val base64Image = withContext(Dispatchers.IO) {
                compressAndEncodeImage(imageUri)
            } ?: return Result.failure(Exception("Failed to process image"))

            // 3. Send to backend for analysis
            val response = apiService.sendImageChatMessage(
                ChatImageRequest(
                    message = "Please analyze this food image and provide recipe suggestions.",
                    imageBase64 = base64Image,
                    mediaType = "image/jpeg"
                )
            )

            // 4. Create and save AI response
            val aiMessage = ChatMessage(
                id = response.message.id,
                content = response.message.content,
                isFromUser = false,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(aiMessage.toEntity())
            Timber.d("AI image analysis response saved: ${aiMessage.id}")

            Result.success(aiMessage)
        } catch (e: CancellationException) {
            throw e
        } catch (e: retrofit2.HttpException) {
            Timber.w(e, "HTTP ${e.code()} on send image message")
            Result.failure(e)
        } catch (e: IOException) {
            Timber.w(e, "Network error on send image message")
            Result.failure(e)
        } catch (e: SQLiteException) {
            Timber.e(e, "Failed to persist image message")
            Result.failure(e)
        }
        // Unexpected exceptions propagate per issue #34 — broad catch removed.
    }

    private fun compressAndEncodeImage(uri: Uri): String? {
        return try {
            // Read the image from URI
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return null

            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                Timber.e("Failed to decode bitmap from URI: $uri")
                return null
            }

            // Compress to JPEG with progressive quality reduction if needed
            val outputStream = ByteArrayOutputStream()
            var quality = INITIAL_QUALITY

            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            // Reduce quality until we're under the max size
            while (outputStream.size() > MAX_IMAGE_SIZE_BYTES && quality > MIN_QUALITY) {
                outputStream.reset()
                quality -= QUALITY_STEP
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                Timber.d("Compressed image to quality $quality, size: ${outputStream.size()} bytes")
            }

            // Convert to Base64
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            // Clean up
            bitmap.recycle()
            outputStream.close()

            Timber.d("Image encoded successfully, Base64 length: ${base64.length}")
            base64
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Image processing surfaces many unrelated exception types
            // (FileNotFoundException, OutOfMemoryError-wrapped runtime errors,
            // BitmapFactory decode failures, Base64 encoding edge cases). Per-image
            // failures must not crash the chat flow, so a broad catch is retained
            // here for resilience. Issue #34 narrowing applied to the public Result-
            // returning surfaces (sendMessage, sendImageMessage, clearHistory) only.
            Timber.e(e, "Failed to compress and encode image")
            null
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteException) {
            Timber.e(e, "Failed to clear chat history")
            Result.failure(e)
        }
    }

    override fun getQuickActions(): List<String> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 6..10 -> listOf("What's for breakfast?", "Quick breakfast ideas", "Change my breakfast", "Healthy start")
            hour in 11..15 -> listOf("What's for lunch?", "Swap my lunch", "Quick lunch ideas", "South Indian")
            hour in 16..20 -> listOf("What's for dinner?", "Change dinner", "Family meal ideas", "Quick dinner")
            else -> listOf("Show my meals", "Light snack ideas", "Healthy option", "Simple recipe")
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
