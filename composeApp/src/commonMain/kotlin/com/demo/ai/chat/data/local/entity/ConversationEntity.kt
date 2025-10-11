package com.demo.ai.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a conversation in the chat app.
 *
 * @property id Unique identifier for the conversation (Primary Key).
 * @property title Title/name of the conversation.
 * @property createdAt Timestamp when the conversation was created (in milliseconds).
 * @property updatedAt Timestamp when the conversation was last updated (in milliseconds).
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

