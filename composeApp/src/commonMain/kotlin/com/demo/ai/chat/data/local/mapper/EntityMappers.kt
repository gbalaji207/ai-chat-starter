package com.demo.ai.chat.data.local.mapper

import com.demo.ai.chat.data.local.entity.ConversationEntity
import com.demo.ai.chat.data.local.entity.MessageEntity
import com.demo.ai.chat.data.model.ChatMessage
import com.demo.ai.chat.data.model.Conversation

/**
 * Converts a MessageEntity from the database to a ChatMessage domain model.
 *
 * Note: The isStreaming field is always set to false since persisted messages
 * are no longer being streamed.
 *
 * @return ChatMessage domain model with all fields mapped from the entity
 */
fun MessageEntity.toChatMessage(): ChatMessage {
    return ChatMessage(
        id = id,
        text = text,
        isUser = isUser,
        timestamp = timestamp,
        isStreaming = false, // Persisted messages are never streaming
        conversationId = conversationId,
        tokens = tokens
    )
}

/**
 * Converts a ChatMessage domain model to a MessageEntity for database persistence.
 *
 * Note: The isStreaming field is not persisted as it's a transient UI state.
 *
 * @return MessageEntity ready to be inserted into the database
 */
fun ChatMessage.toMessageEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        conversationId = conversationId,
        text = text,
        isUser = isUser,
        timestamp = timestamp,
        tokens = tokens
    )
}

/**
 * Converts a ConversationEntity from the database to a Conversation domain model.
 *
 * @return Conversation domain model with all fields mapped from the entity
 */
fun ConversationEntity.toConversation(): Conversation {
    return Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Converts a Conversation domain model to a ConversationEntity for database persistence.
 *
 * @return ConversationEntity ready to be inserted into the database
 */
fun Conversation.toConversationEntity(): ConversationEntity {
    return ConversationEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

