package com.rasoiai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rasoiai.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(limit: Int = 50): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getMessageCount(): Int

    @Query("DELETE FROM chat_messages WHERE timestamp < :timestamp")
    suspend fun deleteOldMessages(timestamp: Long)
}
