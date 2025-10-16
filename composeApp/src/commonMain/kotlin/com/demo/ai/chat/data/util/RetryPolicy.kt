package com.demo.ai.chat.data.util

import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Retry policy for handling transient failures in API requests.
 *
 * Implements exponential backoff with jitter to gracefully handle temporary errors
 * like rate limits, network issues, and service unavailability.
 *
 * ## Exponential Backoff Algorithm
 * The delay between retries increases exponentially: delay = baseDelayMs * 2^attempt
 * - Attempt 0: 1 second
 * - Attempt 1: 2 seconds
 * - Attempt 2: 4 seconds
 * - Attempt 3: 8 seconds
 * - Attempt 4: 16 seconds (capped at maxDelayMs)
 *
 * This prevents overwhelming the server while it recovers from issues.
 *
 * ## Why Jitter?
 * Adding random variance (±20%) prevents the "thundering herd" problem where multiple
 * clients retry at exactly the same time, potentially causing another spike in traffic.
 * Jitter spreads out retry attempts more evenly over time.
 *
 * ## Usage Example
 * ```kotlin
 * val policy = RetryPolicy()
 * var attempt = 0
 *
 * while (attempt < policy.maxAttempts) {
 *     try {
 *         val response = makeApiCall()
 *         return response
 *     } catch (e: Exception) {
 *         if (policy.shouldRetry(statusCode, attempt)) {
 *             val delay = policy.addJitter(policy.calculateBackoff(attempt))
 *             delay(delay)
 *             attempt++
 *         } else {
 *             throw e
 *         }
 *     }
 * }
 * ```
 *
 * @property maxAttempts Maximum number of retry attempts (default: 3)
 * @property baseDelayMs Base delay in milliseconds for exponential backoff (default: 1000ms)
 * @property maxDelayMs Maximum delay in milliseconds to cap backoff (default: 16000ms)
 * @property retryableStatusCodes HTTP status codes that warrant a retry (default: 429, 500, 503)
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val baseDelayMs: Long = 1000,
    val maxDelayMs: Long = 16000,
    val retryableStatusCodes: Set<Int> = setOf(429, 500, 503)
) {

    /**
     * Determines whether a request should be retried based on the HTTP status code
     * and the current attempt number.
     *
     * A retry is allowed when:
     * - The current attempt is less than [maxAttempts]
     * - The HTTP status code is in the [retryableStatusCodes] set
     *
     * @param statusCode The HTTP status code from the failed request
     * @param attempt The current attempt number (0-indexed)
     * @return true if the request should be retried, false otherwise
     */
    fun shouldRetry(statusCode: Int, attempt: Int): Boolean {
        return attempt < maxAttempts && statusCode in retryableStatusCodes
    }

    /**
     * Calculates the exponential backoff delay for a given attempt.
     *
     * Uses the formula: min(baseDelayMs * 2^attempt, maxDelayMs)
     *
     * Example progression with defaults:
     * - Attempt 0: min(1000 * 2^0, 16000) = 1000ms (1s)
     * - Attempt 1: min(1000 * 2^1, 16000) = 2000ms (2s)
     * - Attempt 2: min(1000 * 2^2, 16000) = 4000ms (4s)
     * - Attempt 3: min(1000 * 2^3, 16000) = 8000ms (8s)
     * - Attempt 4: min(1000 * 2^4, 16000) = 16000ms (16s - capped)
     *
     * @param attempt The current attempt number (0-indexed)
     * @return The calculated delay in milliseconds before the next retry
     */
    fun calculateBackoff(attempt: Int): Long {
        val exponentialDelay = baseDelayMs * 2.0.pow(attempt).toLong()
        return min(exponentialDelay, maxDelayMs)
    }

    /**
     * Adds random jitter to a delay value to prevent thundering herd problem.
     *
     * Adds ±20% random variance to the input delay. This spreads out retry attempts
     * from multiple clients, preventing them from all hitting the server at the exact
     * same time after a failure.
     *
     * Example:
     * - Input: 1000ms → Output: random value between 800ms and 1200ms
     * - Input: 5000ms → Output: random value between 4000ms and 6000ms
     *
     * @param delayMs The base delay in milliseconds
     * @return The delay with jitter applied (±20% variance)
     */
    fun addJitter(delayMs: Long): Long {
        val jitterRange = (delayMs * 0.2).toLong()
        val minDelay = delayMs - jitterRange
        val maxDelay = delayMs + jitterRange
        return Random.nextLong(minDelay, maxDelay + 1)
    }
}

