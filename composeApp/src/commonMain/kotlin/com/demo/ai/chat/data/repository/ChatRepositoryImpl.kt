package com.demo.ai.chat.data.repository

import com.demo.ai.chat.data.model.AIError
import com.demo.ai.chat.data.model.ChatMessage
import com.demo.ai.chat.data.model.ChatResponse
import com.demo.ai.chat.data.model.MessageRole
import com.demo.ai.chat.data.prompts.AIPersonality
import com.demo.ai.chat.data.source.OpenAIDataSource
import com.demo.ai.chat.data.util.RetryPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Implementation of ChatRepository that uses OpenAI as the backend.
 *
 * Implements automatic retry logic with exponential backoff for transient failures
 * such as network errors, rate limits, and service unavailability.
 *
 * @property dataSource The OpenAI data source for making API calls
 * @property conversationManager Manages conversation persistence and token limits
 * @property retryPolicy Policy defining retry behavior (max attempts, backoff delays, retryable errors)
 */
class ChatRepositoryImpl(
    private val dataSource: OpenAIDataSource,
    private val conversationManager: ConversationManager,
    private val retryPolicy: RetryPolicy = RetryPolicy()
) : ChatRepository {

    private val conversationId = "default"

    /**
     * Private exception class used for retry flow control.
     *
     * Thrown when a retryable error occurs during API streaming,
     * allowing the retry loop to catch and handle it appropriately.
     */
    private class RetryableException(val error: AIError) : Exception()

    /**
     * Sends a message to the AI and streams the response with automatic retry logic.
     *
     * This method:
     * - Saves the user message to the database
     * - Retrieves pruned conversation context from the database
     * - Applies the specified AI personality (if provided) to influence response style
     * - Streams the AI response with automatic retry on transient failures
     * - Saves the complete AI response to the database
     *
     * ## Retry Behavior:
     * - Network errors, rate limits, timeouts, and service unavailability are automatically retried
     * - Authentication and invalid request errors are NOT retried (require user intervention)
     * - Retries use exponential backoff with jitter to avoid overwhelming the server
     * - Emits ChatResponse.Retrying events to inform UI of retry progress
     * - Database errors are NOT retried (fail immediately)
     *
     * @param message The new message text to send
     * @param conversationHistory The list of previous messages (ignored - context comes from database)
     * @param personality Optional AI personality mode that defines the assistant's behavior and response style
     * @return Flow of ChatResponse objects representing the streaming response, retries, or errors
     */
    override fun sendMessage(
        message: String,
        conversationHistory: List<ChatMessage>,
        personality: AIPersonality?
    ): Flow<ChatResponse> = flow {
        // Step 1: Handle database operations (should NOT retry on failure)
        val context = try {
            // Create and save user message to database
            val userMessage = ChatMessage(
                text = message,
                role = MessageRole.USER,
                conversationId = conversationId
            )
            conversationManager.addMessage(userMessage)

            // Get pruned conversation context from database (respects token limits)
            conversationManager.getContextForAPI(conversationId, personality?.systemPrompt)
        } catch (e: Exception) {
            // Database error - don't retry, just emit error and return
            emit(ChatResponse.Error(error = AIError.Unknown(throwable = e)))
            return@flow
        }

        // Step 2: Retry loop for API calls
        var currentAttempt = 0
        var lastError: AIError? = null
        var shouldContinue = true

        while (currentAttempt <= retryPolicy.maxAttempts && shouldContinue) {
            // Track AI response chunks (reset on each retry attempt)
            val responseBuilder = StringBuilder()

            try {
                // Stream the completion from OpenAI
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

                            // Success! Stop the retry loop
                            shouldContinue = false
                        }

                        is ChatResponse.Error -> {
                            // Extract the AIError and decide whether to retry
                            lastError = response.error

                            // Check if this error is retryable
                            val statusCode = extractStatusCode(response.error)

                            if (!retryPolicy.shouldRetry(statusCode, currentAttempt)) {
                                // Not retryable or max attempts reached - emit error and stop
                                emit(response)
                                shouldContinue = false
                            } else {
                                // This error is retryable - throw to trigger retry loop
                                throw RetryableException(response.error)
                            }
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

                            // Success! Stop the retry loop
                            shouldContinue = false
                        }

                        is ChatResponse.Retrying -> {
                            // Pass through retrying events (shouldn't happen from data source, but handle it)
                            emit(response)
                        }
                    }
                }

                // If we get here, streaming completed - stop the retry loop
                shouldContinue = false

            } catch (e: RetryableException) {
                // Retryable error occurred
                currentAttempt++

                if (currentAttempt >= retryPolicy.maxAttempts) {
                    // Max attempts reached - emit final error and stop
                    emit(ChatResponse.Error(error = e.error))
                    shouldContinue = false
                } else {
                    // Calculate backoff delay with jitter
                    val baseDelay = retryPolicy.calculateBackoff(currentAttempt)
                    val delayMs = retryPolicy.addJitter(baseDelay)

                    // Emit retrying state to UI (1-based attempt number for user display)
                    emit(
                        ChatResponse.Retrying(
                            attempt = currentAttempt,
                            delayMs = delayMs,
                            error = e.error
                        )
                    )

                    // Wait before retrying
                    delay(delayMs)

                    // Continue to next iteration (retry)
                }
            } catch (e: Exception) {
                // Unexpected non-retryable exception
                emit(ChatResponse.Error(error = AIError.Unknown(throwable = e)))
                shouldContinue = false
            }
        }

        // If we exhaust all retries without success (shouldn't reach here, but safety net)
        if (shouldContinue && lastError != null) {
            emit(ChatResponse.Error(error = lastError))
        }
    }

    /**
     * Extracts a pseudo-HTTP status code from an AIError for retry policy evaluation.
     *
     * Maps AIError types to status codes that can be checked by RetryPolicy:
     * - Network errors → 0 (special code, but retryable)
     * - RateLimit → 429 (retryable with special handling)
     * - Authentication → 401 (NOT retryable)
     * - InvalidRequest → 400 (NOT retryable)
     * - Timeout → 0 (retryable)
     * - ServiceUnavailable → 503 (retryable)
     * - Unknown → 500 (retryable)
     *
     * @param error The AIError to extract status code from
     * @return A status code for retry policy evaluation
     */
    private fun extractStatusCode(error: AIError): Int {
        return when (error) {
            is AIError.Network -> 0 // Not HTTP, but retryable
            is AIError.RateLimit -> 429 // Retryable with backoff
            is AIError.Authentication -> 401 // NOT retryable
            is AIError.InvalidRequest -> 400 // NOT retryable
            is AIError.Timeout -> 0 // Retryable
            is AIError.ServiceUnavailable -> 503 // Retryable
            is AIError.Unknown -> 500 // Retryable (might be transient)
        }
    }
}
