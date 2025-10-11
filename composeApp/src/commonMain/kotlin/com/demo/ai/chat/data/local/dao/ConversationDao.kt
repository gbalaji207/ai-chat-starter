package com.demo.ai.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.demo.ai.chat.data.local.entity.ConversationEntity

/**
 * Data Access Object for conversation operations.
 */
@Dao
interface ConversationDao {

    /**
     * Insert or replace a conversation.
     * @param conversation The conversation to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity)

    /**
     * Get all conversations, ordered by most recently updated first.
     * @return List of all conversations.
     */
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ConversationEntity>

    /**
     * Get a conversation by its ID.
     * @param id The conversation ID.
     * @return The conversation if found, null otherwise.
     */
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    /**
     * Update an existing conversation.
     * @param conversation The conversation to update.
     */
    @Update
    suspend fun update(conversation: ConversationEntity)

    /**
     * Delete a conversation by its ID.
     * @param id The conversation ID to delete.
     */
    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)
}

