package com.demo.ai.chat.data.repository

import com.demo.ai.chat.data.model.ChatMessage
import com.demo.ai.chat.data.model.ChatResponse
import com.demo.ai.chat.data.model.MessageRole
import com.demo.ai.chat.data.prompts.AIPersonality
import com.demo.ai.chat.data.source.OpenAIDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Implementation of ChatRepository that uses OpenAI as the backend.
 *
 * @property dataSource The OpenAI data source for making API calls
 * @property conversationManager Manages conversation persistence and token limits
 */
class ChatRepositoryImpl(
    private val dataSource: OpenAIDataSource,
    private val conversationManager: ConversationManager
) : ChatRepository {

    private val conversationId = "default"

    /**
     * Sends a message to the AI and streams the response.
     *
     * This method:
     * - Saves the user message to the database
     * - Retrieves pruned conversation context from the database
     * - Applies the specified AI personality (if provided) to influence response style
     * - Streams the AI response
     * - Saves the complete AI response to the database
     *
     * @param message The new message text to send
     * @param conversationHistory The list of previous messages (ignored - context comes from database)
     * @param personality Optional AI personality mode that defines the assistant's behavior and response style
     * @return Flow of ChatResponse objects representing the streaming response
     */
    override fun sendMessage(
        message: String,
        conversationHistory: List<ChatMessage>,
        personality: AIPersonality?
    ): Flow<ChatResponse> = flow {
        try {
            // Create and save user message to database
            val userMessage = ChatMessage(
                text = message,
                role = MessageRole.USER,
                conversationId = conversationId
            )
            conversationManager.addMessage(userMessage)

            // Get pruned conversation context from database (respects token limits)
            val context =
                conversationManager.getContextForAPI(conversationId, personality?.systemPrompt)

            // Extract system prompt from personality (if provided)
            val systemPrompt = personality?.systemPrompt

            // Track AI response chunks to save complete response later
            val responseBuilder = StringBuilder()

            // Stream the completion from OpenAI with personality-based system prompt
            dataSource.streamChatCompletion(messages = context).collect { response ->
                when (response) {
                    is ChatResponse.StreamingChunk -> {
                        // Accumulate the chunk
                        responseBuilder.append(response.chunk)
                        // Emit the chunk to the UI
                        emit(response)
                    }

                    is ChatResponse.StreamingComplete -> {
                        // Save the complete AI response to database
                        val aiMessage = ChatMessage(
                            text = responseBuilder.toString(),
                            role = MessageRole.ASSISTANT,
                            conversationId = conversationId
                        )
                        conversationManager.addMessage(aiMessage)

                        // Emit completion to the UI
                        emit(response)
                    }

                    is ChatResponse.Error -> {
                        // Emit error to the UI without saving
                        emit(response)
                    }

                    is ChatResponse.Success -> {
                        // Handle non-streaming success (if ever used)
                        val aiMessage = ChatMessage(
                            text = response.message,
                            role = MessageRole.ASSISTANT,
                            conversationId = conversationId
                        )
                        conversationManager.addMessage(aiMessage)

                        // Emit success to the UI
                        emit(response)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle database or other errors
            emit(
                ChatResponse.Error(
                    message = "Failed to process message: ${e.message ?: "Unknown error"}",
                    throwable = e
                )
            )
        }
    }
}
