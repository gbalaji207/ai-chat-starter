package com.demo.ai.chat.data.model

/**
 * Sealed class hierarchy representing all possible errors that can occur
 * when interacting with the AI chat service (OpenAI API).
 *
 * This error model enables:
 * - Type-safe error handling
 * - Appropriate retry logic based on error type
 * - User-friendly error messages
 */
sealed class AIError {

    /**
     * Network connectivity error.
     *
     * Occurs when:
     * - Device has no internet connection
     * - DNS resolution fails
     * - Network request times out at the transport level
     *
     * Retry strategy: Exponential backoff recommended
     *
     * @property message Human-readable description of the network issue
     */
    data class Network(val message: String) : AIError()

    /**
     * Rate limit exceeded error (HTTP 429).
     *
     * Occurs when:
     * - Too many requests sent to OpenAI API in a short time
     * - API quota limits reached
     *
     * Retry strategy: Wait for [retryAfter] seconds before retrying
     *
     * @property retryAfter Number of seconds to wait before the next request
     */
    data class RateLimit(val retryAfter: Int) : AIError()

    /**
     * Authentication or authorization error (HTTP 401/403).
     *
     * Occurs when:
     * - API key is invalid or missing
     * - API key doesn't have permission for the requested operation
     * - API key has been revoked
     *
     * Retry strategy: Do not retry, requires user intervention
     *
     * @property message Description of the authentication failure
     */
    data class Authentication(val message: String) : AIError()

    /**
     * Invalid request error (HTTP 400).
     *
     * Occurs when:
     * - Request parameters are malformed
     * - Required parameters are missing
     * - Parameter values are out of valid range (e.g., temperature > 2.0)
     * - Model name is invalid
     *
     * Retry strategy: Do not retry, fix request parameters first
     *
     * @property details Specific details about what's wrong with the request
     */
    data class InvalidRequest(val details: String) : AIError()

    /**
     * Request timeout error.
     *
     * Occurs when:
     * - Request takes longer than the configured timeout duration
     * - Stream response stops mid-way
     *
     * Retry strategy: Retry with exponential backoff, consider increasing timeout
     */
    data object Timeout : AIError()

    /**
     * Service unavailable error (HTTP 503).
     *
     * Occurs when:
     * - OpenAI API is temporarily down for maintenance
     * - OpenAI servers are overloaded
     * - Upstream service dependencies are failing
     *
     * Retry strategy: Retry with exponential backoff and jitter
     */
    data object ServiceUnavailable : AIError()

    /**
     * Unknown or unexpected error.
     *
     * Occurs when:
     * - An error doesn't match any known error type
     * - Unexpected exception is thrown
     * - Response parsing fails
     *
     * Retry strategy: May retry once, then escalate/log for investigation
     *
     * @property throwable The underlying exception or error
     */
    data class Unknown(val throwable: Throwable) : AIError()
}

/**
 * Converts a technical AIError into a user-friendly, actionable message.
 *
 * This extension function translates technical error types into clear messages
 * that end users can understand and act upon. Messages are intentionally
 * non-technical and provide guidance on what the user should do next.
 *
 * ## Design Principles:
 * - Avoid technical jargon (no HTTP codes, exception names, etc.)
 * - Provide actionable guidance (what the user should do)
 * - Be reassuring and professional
 * - Don't expose security-sensitive details (like API key errors)
 *
 * ## Usage Example:
 * ```kotlin
 * when (val response = chatRepository.sendMessage(text)) {
 *     is ChatResponse.Error -> {
 *         val userMessage = response.error.toUserMessage()
 *         showErrorDialog(userMessage) // "Couldn't connect to the AI service..."
 *     }
 * }
 * ```
 *
 * @return A user-friendly error message appropriate for displaying in the UI
 */
fun AIError.toUserMessage(): String {
    return when (this) {
        is AIError.Network -> {
            "Couldn't connect to the AI service. Please check your internet connection and try again."
        }

        is AIError.RateLimit -> {
            "Too many requests right now. Please wait $retryAfter seconds before trying again."
        }

        is AIError.Authentication -> {
            "There's an issue with the API configuration. Please contact support."
        }

        is AIError.InvalidRequest -> {
            "That message couldn't be processed. Please try rephrasing it or making it shorter."
        }

        is AIError.Timeout -> {
            "The request is taking too long. Please try again with a shorter message."
        }

        is AIError.ServiceUnavailable -> {
            "The AI service is temporarily unavailable. Please try again in a few moments."
        }

        is AIError.Unknown -> {
            "Something unexpected happened. Please try again."
        }
    }
}
