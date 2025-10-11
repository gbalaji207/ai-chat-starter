package com.demo.ai.chat.data.model

/**
 * Domain model representing a conversation.
 *
 * @property id Unique identifier for the conversation
 * @property title Title/name of the conversation
 * @property createdAt Timestamp when the conversation was created (in milliseconds)
 * @property updatedAt Timestamp when the conversation was last updated (in milliseconds)
 */
data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

