package com.demo.ai.chat.data.model

/**
 * Sealed class representing different types of chat responses from the AI.
 * This allows for type-safe handling of various response states including
 * success, streaming, errors, and retry attempts.
 */
sealed class ChatResponse {

    /**
     * Represents a successful complete response from the AI.
     *
     * @property message The complete message content
     */
    data class Success(val message: String) : ChatResponse()

    /**
     * Represents a chunk of data received during streaming.
     *
     * @property chunk The partial content received in this chunk
     */
    data class StreamingChunk(val chunk: String) : ChatResponse()

    /**
     * Represents the completion of a streaming response.
     * This signals that no more chunks will be received.
     */
    data object StreamingComplete : ChatResponse()

    /**
     * Represents a retry attempt in progress after a transient failure.
     *
     * This state is emitted when an API request fails with a retryable error
     * (e.g., rate limit, network issue, service unavailable) and the system
     * is waiting before making another attempt.
     *
     * Useful for showing retry progress to users (e.g., "Retrying in 2 seconds...")
     *
     * @property attempt The current retry attempt number (1-based). First retry is 1.
     * @property delayMs Milliseconds until the next retry attempt
     * @property error The specific error that triggered this retry attempt
     */
    data class Retrying(
        val attempt: Int,
        val delayMs: Long,
        val error: AIError
    ) : ChatResponse()

    /**
     * Represents an error that occurred during chat processing.
     *
     * This is emitted when a non-retryable error occurs or when all retry
     * attempts have been exhausted.
     *
     * @property error The specific error that occurred, containing type and context
     */
    data class Error(
        val error: AIError
    ) : ChatResponse()
}
