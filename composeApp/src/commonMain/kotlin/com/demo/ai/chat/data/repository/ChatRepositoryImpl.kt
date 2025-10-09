package com.demo.ai.chat.data.repository

import com.demo.ai.chat.data.model.ChatMessage
import com.demo.ai.chat.data.model.ChatResponse
import com.demo.ai.chat.data.source.OpenAIDataSource
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of ChatRepository that uses OpenAI as the backend.
 *
 * @property dataSource The OpenAI data source for making API calls
 */
class ChatRepositoryImpl(
    private val dataSource: OpenAIDataSource
) : ChatRepository {

    /**
     * Sends a message to the AI and streams the response.
     *
     * This method combines the conversation history with the new user message
     * and delegates to the OpenAI data source for processing.
     *
     * @param message The new message text to send
     * @param conversationHistory The list of previous messages in the conversation
     * @return Flow of ChatResponse objects representing the streaming response
     */
    override fun sendMessage(
        message: String,
        conversationHistory: List<ChatMessage>
    ): Flow<ChatResponse> {
        // Create a new user message
        val userMessage = ChatMessage(
            text = message,
            isUser = true
        )

        // Combine conversation history with the new message
        val allMessages = conversationHistory + userMessage

        // Delegate to the data source for streaming the completion
        return dataSource.streamChatCompletion(allMessages)
    }
}

