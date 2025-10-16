package com.demo.ai.chat.data.source

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.exception.AuthenticationException
import com.aallam.openai.api.exception.OpenAIAPIException
import com.aallam.openai.api.exception.OpenAIException
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.demo.ai.chat.data.model.AIError
import com.demo.ai.chat.data.model.ChatMessage
import com.demo.ai.chat.data.model.ChatResponse
import com.demo.ai.chat.data.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import com.aallam.openai.api.chat.ChatMessage as OpenAIChatMessage

/**
 * Data source that wraps OpenAI API calls for chat completions.
 *
 * @property openAI The OpenAI client instance used for API calls
 * @property requestTimeoutMs Maximum time in milliseconds to wait for a request to complete (default: 60 seconds)
 */
class OpenAIDataSource(
    private val openAI: OpenAI, private val requestTimeoutMs: Long = 60_000
) {

    /**
     * Streams chat completion responses from the OpenAI API.
     *
     * @param messages The list of chat messages to send to the API
     * @param temperature Controls randomness in the response (0.0 to 2.0). Default is 0.7
     * @return Flow of ChatResponse objects representing streaming chunks, completion, or errors
     */
    fun streamChatCompletion(
        messages: List<ChatMessage>, temperature: Double = 0.7
    ): Flow<ChatResponse> = flow {
        withTimeout(requestTimeoutMs) {
            // Convert our ChatMessage format to OpenAI's ChatMessage format
            val openAIMessages = messages.map { message ->
                OpenAIChatMessage(
                    role = when (message.role) {
                        MessageRole.USER -> ChatRole.User
                        MessageRole.ASSISTANT -> ChatRole.Assistant
                        MessageRole.SYSTEM -> ChatRole.System
                    }, content = message.text
                )
            }

            // Create the chat completion request
            val chatCompletionRequest = ChatCompletionRequest(
                model = ModelId("gpt-3.5-turbo"),
                messages = openAIMessages,
                temperature = temperature,
                maxTokens = 500
            )

            // Track if we received any content
            var hasContent = false

            // Stream the response
            openAI.chatCompletions(chatCompletionRequest).catch { error ->
                // Parse and emit structured error response
                val parsedError = parseError(error)
                emit(ChatResponse.Error(error = parsedError))
            }.onCompletion { error ->
                // Only emit completion if no error occurred and we received content
                if (error == null && hasContent) {
                    emit(ChatResponse.StreamingComplete)
                }
            }.collect { chunk ->
                // Extract content from the chunk
                val content = chunk.choices.firstOrNull()?.delta?.content

                if (content != null) {
                    hasContent = true
                    emit(ChatResponse.StreamingChunk(chunk = content))
                }
            }
        }
    }.catch { error ->
        // Handle timeout and other errors that occur during flow creation
        val parsedError = when (error) {
            is TimeoutCancellationException -> AIError.Timeout
            else -> parseError(error)
        }
        emit(ChatResponse.Error(error = parsedError))
    }

    /**
     * Parses throwable exceptions into structured AIError types.
     *
     * This function maps various exception types to specific AIError variants,
     * enabling type-safe error handling and appropriate retry strategies.
     *
     * ## HTTP Status Code Mapping:
     * - 401/403 → Authentication error (invalid or revoked API key)
     * - 400 → InvalidRequest error (malformed parameters)
     * - 429 → RateLimit error (too many requests, extracts retry-after header)
     * - 500/503 → ServiceUnavailable (OpenAI server issues)
     *
     * ## Exception Type Mapping:
     * - TimeoutCancellationException → Timeout
     * - Network-related exceptions (UnknownHostException, IOException, etc.) → Network
     * - All other exceptions → Unknown
     *
     * ## Retry-After Header Parsing:
     * For rate limit errors (429), attempts to extract the "Retry-After" header
     * from the response. Defaults to 60 seconds if header is not present or cannot be parsed.
     *
     * @param throwable The exception to parse
     * @return A structured AIError representing the specific error type
     */
    private fun parseError(throwable: Throwable): AIError {
        // Handle timeout exceptions
        if (throwable is TimeoutCancellationException) {
            return AIError.Timeout
        }

        // Try to extract HTTP status code from OpenAI exceptions
        val statusCode = if (throwable is OpenAIAPIException) {
            throwable.statusCode
        } else {
            extractStatusCode(throwable)
        }
        val errorMessage = throwable.message ?: "Unknown error occurred"

        return when {
            // Authentication errors (401, 403)
            statusCode == 401 || statusCode == 403 -> {
                AIError.Authentication(message = errorMessage.takeIf { it.isNotBlank() }
                    ?: "Authentication failed. Please check your API key.")
            }

            // Invalid request (400)
            statusCode == 400 -> {
                AIError.InvalidRequest(details = errorMessage.takeIf { it.isNotBlank() }
                    ?: "Invalid request parameters")
            }

            // Rate limit (429)
            statusCode == 429 -> {
                val retryAfter = extractRetryAfter(throwable)
                AIError.RateLimit(retryAfter = retryAfter)
            }

            // Service unavailable (500, 503)
            statusCode == 500 || statusCode == 503 -> {
                AIError.ServiceUnavailable
            }

            // Network-related errors
            isNetworkError(throwable) -> {
                AIError.Network(message = errorMessage.takeIf { it.isNotBlank() }
                    ?: "Network connection failed")
            }

            // Unknown/unexpected errors
            else -> {
                AIError.Unknown(throwable = throwable)
            }
        }
    }

    /**
     * Attempts to extract HTTP status code from various exception types.
     *
     * The openai-kotlin library may wrap HTTP errors in different exception types.
     * This function attempts to extract the status code by parsing the exception
     * message, as reflection is not fully available in Kotlin Multiplatform.
     *
     * @param throwable The exception to extract status code from
     * @return The HTTP status code, or null if it cannot be extracted
     */
    private fun extractStatusCode(throwable: Throwable): Int? {
        return try {
            val message = throwable.message ?: ""

            // Try to find HTTP status code pattern in the message
            // Common patterns: "HTTP 401", "status code: 429", "403 Forbidden", etc.
            val patterns = listOf(
                Regex("HTTP\\s+(\\d{3})"),
                Regex("status\\s+code[:\\s]+(\\d{3})", RegexOption.IGNORE_CASE),
                Regex("(\\d{3})\\s+(Unauthorized|Forbidden|Bad Request|Too Many Requests|Service Unavailable)"),
                Regex("^(\\d{3})") // Status code at the start
            )

            for (pattern in patterns) {
                val match = pattern.find(message)
                if (match != null) {
                    return match.groupValues[1].toIntOrNull()
                }
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Attempts to extract the Retry-After header value from rate limit errors.
     *
     * For HTTP 429 (Rate Limit) errors, the server may include a Retry-After header
     * indicating how many seconds the client should wait before retrying.
     *
     * This function attempts to parse the retry-after value from the exception message.
     *
     * @param throwable The exception to extract retry-after from
     * @return Number of seconds to wait before retrying (defaults to 60 if not found)
     */
    private fun extractRetryAfter(throwable: Throwable): Int {
        return try {
            val message = throwable.message ?: ""

            // Try to find retry-after value in the message
            // Common patterns: "retry after 30", "Retry-After: 60", etc.
            val patterns = listOf(
                Regex("retry[- ]after[:\\s]+(\\d+)", RegexOption.IGNORE_CASE),
                Regex("retry\\s+in\\s+(\\d+)", RegexOption.IGNORE_CASE),
                Regex("wait\\s+(\\d+)\\s+seconds?", RegexOption.IGNORE_CASE)
            )

            for (pattern in patterns) {
                val match = pattern.find(message)
                if (match != null) {
                    return match.groupValues[1].toIntOrNull() ?: 60
                }
            }

            // Default to 60 seconds if not found
            60
        } catch (_: Exception) {
            60
        }
    }

    /**
     * Determines if an exception is network-related.
     *
     * Checks if the exception or its cause chain contains common network-related
     * exception types like UnknownHostException, SocketException, IOException, etc.
     *
     * @param throwable The exception to check
     * @return true if the exception is network-related, false otherwise
     */
    private fun isNetworkError(throwable: Throwable): Boolean {
        val exceptionName = throwable::class.simpleName ?: ""
        val message = throwable.message?.lowercase() ?: ""

        // Check exception type name
        val isNetworkException = exceptionName.contains(
            "UnknownHost",
            ignoreCase = true
        ) || exceptionName.contains(
            "SocketTimeout",
            ignoreCase = true
        ) || exceptionName.contains(
            "ConnectException",
            ignoreCase = true
        ) || exceptionName.contains(
            "IOException",
            ignoreCase = true
        ) || exceptionName.contains("Network", ignoreCase = true)

        // Check message content
        val hasNetworkMessage =
            message.contains("network") || message.contains("connection") || message.contains("host") || message.contains(
                "socket"
            )

        // Check cause chain
        val causeIsNetwork = throwable.cause?.let { isNetworkError(it) } ?: false

        return isNetworkException || hasNetworkMessage || causeIsNetwork
    }
}
