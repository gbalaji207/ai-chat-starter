package com.demo.ai.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.demo.ai.chat.data.local.entity.MessageEntity

/**
 * Data Access Object for message operations.
 */
@Dao
interface MessageDao {

    /**
     * Insert or replace a message.
     * @param message The message to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    /**
     * Insert or replace multiple messages.
     * @param messages The messages to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    /**
     * Get all messages for a conversation, ordered by timestamp ascending (oldest first).
     * @param conversationId The conversation ID.
     * @return List of messages in the conversation.
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesForConversation(conversationId: String): List<MessageEntity>

    /**
     * Get the most recent messages for a conversation, ordered by timestamp descending (newest first).
     * @param conversationId The conversation ID.
     * @param limit Maximum number of messages to return.
     * @return List of recent messages.
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(conversationId: String, limit: Int): List<MessageEntity>

    /**
     * Delete all messages for a conversation.
     * @param conversationId The conversation ID.
     */
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteForConversation(conversationId: String)

    /**
     * Delete a message by its ID.
     * @param id The message ID to delete.
     */
    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Get the total count of messages for a conversation.
     * @param conversationId The conversation ID.
     * @return The number of messages in the conversation.
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: String): Int
}

