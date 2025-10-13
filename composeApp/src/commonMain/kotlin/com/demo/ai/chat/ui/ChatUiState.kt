package com.demo.ai.chat.ui

import com.demo.ai.chat.data.model.ChatMessage
import com.demo.ai.chat.data.model.MessageRole
import com.demo.ai.chat.data.prompts.AIPersonality

/**
 * Represents the UI state for the chat screen.
 *
 * @property messages The list of chat messages in the conversation
 * @property isStreaming True if a message is currently being streamed from the AI
 * @property error An error message to display, or null if no error
 * @property inputText The current text in the input field
 * @property selectedPersonality The currently selected AI personality mode that influences response style
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val selectedPersonality: AIPersonality = AIPersonality.Professional
) {

    /**
     * Creates a new state with a user message added to the conversation.
     *
     * @param text The text content of the user's message
     * @return A new ChatUiState with the user message added
     */
    fun addUserMessage(text: String): ChatUiState {
        val userMessage = ChatMessage(
            text = text,
            role = MessageRole.USER,
        )
        return copy(
            messages = messages + userMessage,
            inputText = ""
        )
    }

    /**
     * Creates a new state with an AI message added to the conversation.
     *
     * @param text The text content of the AI's message
     * @param isStreaming True if this message is being streamed
     * @return A new ChatUiState with the AI message added
     */
    fun addAiMessage(text: String, isStreaming: Boolean = false): ChatUiState {
        val aiMessage = ChatMessage(
            text = text,
            role = MessageRole.ASSISTANT,
            isStreaming = isStreaming
        )
        return copy(
            messages = messages + aiMessage,
            isStreaming = isStreaming
        )
    }

    /**
     * Updates the text of the last message in the conversation.
     * This is useful for updating streaming messages as new chunks arrive.
     *
     * @param text The new text content for the last message
     * @return A new ChatUiState with the last message updated
     */
    fun updateLastMessage(text: String): ChatUiState {
        if (messages.isEmpty()) return this

        val updatedMessages = messages.dropLast(1) + messages.last().copy(text = text)
        return copy(messages = updatedMessages)
    }

    /**
     * Sets an error message in the UI state.
     *
     * @param error The error message to display
     * @return A new ChatUiState with the error set and streaming disabled
     */
    fun setError(error: String): ChatUiState {
        return copy(
            error = error,
            isStreaming = false
        )
    }

    /**
     * Clears any existing error message.
     *
     * @return A new ChatUiState with no error
     */
    fun clearError(): ChatUiState {
        return copy(error = null)
    }

    /**
     * Updates the selected AI personality mode.
     *
     * @param personality The new AI personality to use for future messages
     * @return A new ChatUiState with the personality updated
     */
    fun updatePersonality(personality: AIPersonality): ChatUiState {
        return copy(selectedPersonality = personality)
    }
}
