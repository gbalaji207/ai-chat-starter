package com.demo.ai.chat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a message in a conversation.
 *
 * @property id Unique identifier for the message (Primary Key).
 * @property conversationId ID of the conversation this message belongs to (Foreign Key).
 * @property text The message content/text.
 * @property isUser True if the message was sent by the user, false if sent by AI.
 * @property timestamp Timestamp when the message was created (in milliseconds).
 * @property tokens Number of tokens used for this message (0 if unknown).
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val text: String,
    val role: String,  // Store as String: "USER", "ASSISTANT", "SYSTEM"
    val timestamp: Long,
    val tokens: Int = 0
)

