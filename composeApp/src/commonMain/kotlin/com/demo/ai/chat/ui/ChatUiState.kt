package com.demo.ai.chat.ui

import com.demo.ai.chat.data.model.AIError
import com.demo.ai.chat.data.model.ChatMessage
import com.demo.ai.chat.data.model.MessageRole
import com.demo.ai.chat.data.model.toUserMessage
import com.demo.ai.chat.data.prompts.AIPersonality

/**
 * Represents the UI state for the chat screen.
 *
 * @property messages The list of chat messages in the conversation
 * @property isStreaming True if a message is currently being streamed from the AI
 * @property error An error message to display, or null if no error
 * @property inputText The current text in the input field
 * @property selectedPersonality The currently selected AI personality mode that influences response style
 * @property isRetrying True if the system is currently retrying a failed request
 * @property retryMessage A message explaining the retry status, or null if not retrying
 * @property retryAttempt The current retry attempt number (0 if not retrying)
 * @property maxRetryAttempts The maximum number of retry attempts allowed
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val selectedPersonality: AIPersonality = AIPersonality.Professional,
    val isRetrying: Boolean = false,
    val retryMessage: String? = null,
    val retryAttempt: Int = 0,
    val maxRetryAttempts: Int = 3
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
     * Sets an error message in the UI state using a structured AIError.
     *
     * Converts the technical AIError into a user-friendly message before
     * displaying it. Also clears any retry state.
     *
     * @param error The AIError to display
     * @return A new ChatUiState with the error set, streaming disabled, and retry state cleared
     */
    fun setError(error: AIError): ChatUiState {
        return copy(
            error = error.toUserMessage(),
            isStreaming = false,
            isRetrying = false,
            retryMessage = null
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

    /**
     * Sets the UI state to indicate a retry is in progress.
     *
     * Displays a user-friendly message explaining why the retry is happening,
     * how long until the next attempt, and which attempt number this is.
     *
     * Example message: "⚠️ Network connection failed. Retrying in 2s... (Attempt 1/3)"
     *
     * @param attempt The current retry attempt number (1-based)
     * @param delayMs Milliseconds until the next retry attempt
     * @param error The AIError that triggered this retry
     * @param maxAttempts The maximum number of retry attempts allowed
     * @return A new ChatUiState with retry state set and a formatted retry message
     */
    fun setRetrying(
        attempt: Int,
        delayMs: Long,
        error: AIError,
        maxAttempts: Int = 3
    ): ChatUiState {
        val delaySec = delayMs / 1000
        val errorMessage = error.toUserMessage()
        val formattedMessage =
            "⚠️ $errorMessage Retrying in ${delaySec}s... (Attempt $attempt/$maxAttempts)"

        return copy(
            isRetrying = true,
            retryMessage = formattedMessage,
            retryAttempt = attempt,
            maxRetryAttempts = maxAttempts,
            isStreaming = true // Keep streaming indicator during retry
        )
    }

    /**
     * Clears the retry state, typically called when a retry succeeds or
     * when all retry attempts have been exhausted.
     *
     * @return A new ChatUiState with retry state cleared
     */
    fun clearRetrying(): ChatUiState {
        return copy(
            isRetrying = false,
            retryMessage = null,
            retryAttempt = 0
        )
    }
}
