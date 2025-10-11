package com.demo.ai.chat.data.repository

import com.demo.ai.chat.data.local.dao.ConversationDao
import com.demo.ai.chat.data.local.dao.MessageDao
import com.demo.ai.chat.data.local.entity.ConversationEntity
import com.demo.ai.chat.data.local.entity.MessageEntity
import com.demo.ai.chat.data.local.mapper.toChatMessage
import com.demo.ai.chat.data.local.mapper.toMessageEntity
import com.demo.ai.chat.data.model.ChatMessage
import com.demo.ai.chat.data.util.TokenCounter
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Manages conversation context and token limits for chat interactions.
 *
 * This class handles:
 * - Persisting messages to the database
 * - Calculating token usage for messages
 * - Pruning conversation history to fit within context window limits
 * - Loading and managing conversation data
 *
 * @property messageDao DAO for accessing message data
 * @property conversationDao DAO for accessing conversation data
 * @property maxTokens Maximum tokens allowed in conversation context (defaults to TokenCounter.MAX_CONTEXT_TOKENS)
 */
class ConversationManager(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val maxTokens: Int = TokenCounter.MAX_CONTEXT_TOKENS
) {

    /**
     * Adds a message to the database with token calculation.
     *
     * This function:
     * - Estimates the token count for the message using TokenCounter
     * - Converts the ChatMessage to a MessageEntity
     * - Persists it to the database
     *
     * @param message The chat message to add
     * @return The MessageEntity that was saved to the database
     */
    suspend fun addMessage(message: ChatMessage): MessageEntity {
        // Ensure conversation exists before adding message
        ensureConversationExists(message.conversationId)

        // Calculate tokens for the message
        val tokens = TokenCounter.estimateTokens(message.text)

        // Create message with calculated tokens
        val messageWithTokens = message.copy(tokens = tokens)

        // Convert to entity and save to database
        val entity = messageWithTokens.toMessageEntity()
        messageDao.insert(entity)

        return entity
    }

    /**
     * Ensures a conversation record exists in the database.
     * Creates it if it doesn't exist.
     *
     * @param conversationId The ID of the conversation to ensure exists
     */
    @OptIn(ExperimentalTime::class)
    private suspend fun ensureConversationExists(conversationId: String) {
        val existing = conversationDao.getById(conversationId)
        if (existing == null) {
            val conversation = ConversationEntity(
                id = conversationId,
                title = "Default",
                createdAt = Clock.System.now().toEpochMilliseconds(),
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
            conversationDao.insert(conversation)
        }
    }

    /**
     * Retrieves conversation context optimized for API calls.
     *
     * This function:
     * - Fetches all messages for the conversation from the database
     * - Prunes the message history to fit within the token limit
     * - Returns messages in chronological order (oldest first)
     *
     * The pruning ensures that the most recent messages are prioritized,
     * and older messages are dropped if they would exceed the context window.
     *
     * @param conversationId The ID of the conversation
     * @return List of ChatMessages that fit within the token limit, in chronological order
     */
    suspend fun getContextForAPI(conversationId: String): List<ChatMessage> {
        // Retrieve all messages from database
        val allMessages = messageDao.getMessagesForConversation(conversationId)

        // Apply pruning to fit within token limits
        return pruneByTokens(allMessages, maxTokens)
    }

    /**
     * Prunes a message list to fit within the specified token limit.
     *
     * Algorithm:
     * 1. Start from the most recent message (end of list)
     * 2. Work backwards, accumulating tokens
     * 3. Stop when adding the next message would exceed the limit
     * 4. Return the kept messages in chronological order (oldest first)
     *
     * This ensures that the most recent context is always preserved,
     * which is typically most relevant for conversation continuity.
     *
     * @param messages List of message entities to prune
     * @param maxTokens Maximum tokens allowed
     * @return List of ChatMessages that fit within the token limit, in chronological order
     */
    private fun pruneByTokens(messages: List<MessageEntity>, maxTokens: Int): List<ChatMessage> {
        if (messages.isEmpty()) return emptyList()

        val keptMessages = mutableListOf<MessageEntity>()
        var totalTokens = 0

        // Work backwards from most recent message
        for (message in messages.reversed()) {
            val messageTokens = message.tokens

            // Check if adding this message would exceed the limit
            if (totalTokens + messageTokens > maxTokens) {
                // Stop adding messages - we've hit the limit
                break
            }

            // Add message and update token count
            keptMessages.add(message)
            totalTokens += messageTokens
        }

        // Reverse to restore chronological order (oldest first) and convert to ChatMessage
        return keptMessages.reversed().map { it.toChatMessage() }
    }

    /**
     * Clears all messages for a conversation.
     *
     * This removes all messages from the database for the specified conversation.
     * The conversation itself is not deleted.
     *
     * @param conversationId The ID of the conversation to clear
     */
    suspend fun clearConversation(conversationId: String) {
        messageDao.deleteForConversation(conversationId)
    }

    /**
     * Gets the total number of messages in a conversation.
     *
     * @param conversationId The ID of the conversation
     * @return The count of messages in the conversation
     */
    suspend fun getMessageCount(conversationId: String): Int {
        return messageDao.getMessageCount(conversationId)
    }

    /**
     * Loads all messages for a conversation from the database.
     *
     * This retrieves the complete conversation history without any pruning.
     * Messages are returned in chronological order (oldest first).
     *
     * @param conversationId The ID of the conversation
     * @return List of all ChatMessages in the conversation
     */
    suspend fun loadConversation(conversationId: String): List<ChatMessage> {
        return messageDao.getMessagesForConversation(conversationId)
            .map { it.toChatMessage() }
    }
}

