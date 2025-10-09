package com.demo.ai.chat.data.model

/**
 * Sealed class representing different types of chat responses from the AI.
 * This allows for type-safe handling of various response states.
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
     * Represents an error that occurred during chat processing.
     *
     * @property message A human-readable error message
     * @property throwable The underlying exception, if available
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : ChatResponse()

    /**
     * Represents the completion of a streaming response.
     * This signals that no more chunks will be received.
     */
    data object StreamingComplete : ChatResponse()
}

