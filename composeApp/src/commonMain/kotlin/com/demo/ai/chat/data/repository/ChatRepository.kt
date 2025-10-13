package com.demo.ai.chat.data.repository

import com.demo.ai.chat.data.model.ChatMessage
import com.demo.ai.chat.data.model.ChatResponse
import com.demo.ai.chat.data.prompts.AIPersonality
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for chat operations.
 * This abstraction allows for easier testing and separation of concerns.
 */
interface ChatRepository {

    /**
     * Sends a message to the AI and streams the response.
     *
     * @param message The new message text to send
     * @param conversationHistory The list of previous messages in the conversation
     * @param personality Optional AI personality mode that defines the assistant's behavior and response style
     * @return Flow of ChatResponse objects representing the streaming response
     */
    fun sendMessage(
        message: String,
        conversationHistory: List<ChatMessage>,
        personality: AIPersonality? = null
    ): Flow<ChatResponse>
}
