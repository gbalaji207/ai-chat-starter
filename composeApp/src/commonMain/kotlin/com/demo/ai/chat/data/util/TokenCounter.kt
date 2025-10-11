package com.demo.ai.chat.data.util

/**
 * Utility for estimating OpenAI token usage.
 *
 * This is a simplified estimation approach for managing context windows.
 * OpenAI's actual tokenization is more complex and uses BPE (Byte Pair Encoding).
 *
 * ## Estimation Approach:
 * - Words are estimated at ~1.3 tokens per word on average
 * - Punctuation marks are counted separately as individual tokens
 * - This provides a reasonable approximation for context window management
 *
 * ## Limitations:
 * - Not 100% accurate compared to OpenAI's actual tokenizer
 * - May underestimate tokens for technical content or special characters
 * - For precise token counting, use OpenAI's tiktoken library
 *
 * ## Use Cases:
 * - Preventing context window overflow before API calls
 * - Estimating token usage for conversation history
 * - Managing conversation pruning based on token limits
 */
object TokenCounter {

    /**
     * Maximum context tokens to keep in conversation history.
     * This is a conservative limit to ensure we don't exceed the model's context window.
     */
    const val MAX_CONTEXT_TOKENS = 3000

    /**
     * Average tokens per word based on OpenAI's estimation.
     * English text typically averages around 1.3 tokens per word.
     */
    private const val TOKENS_PER_WORD = 1.3

    /**
     * Estimates the number of tokens in the given text.
     *
     * The estimation uses a word-based approach:
     * - Splits text by whitespace to count words
     * - Applies ~1.3 tokens per word multiplier
     * - Counts punctuation marks separately
     * - Rounds up to the nearest integer
     *
     * @param text The text to estimate tokens for
     * @return Estimated number of tokens (0 for blank/empty strings)
     *
     * @sample
     * ```
     * val text = "Hello, world! How are you?"
     * val tokens = TokenCounter.estimateTokens(text) // Returns ~9 tokens
     * ```
     */
    fun estimateTokens(text: String): Int {
        if (text.isBlank()) return 0

        // Count words (split by whitespace)
        val words = text.trim().split(Regex("\\s+"))
        val wordCount = words.size

        // Count punctuation marks as separate tokens
        val punctuationCount = text.count { char ->
            char in setOf('.', ',', '!', '?', ';', ':', '-', '(', ')', '[', ']', '{', '}', '"', '\'')
        }

        // Estimate: words * 1.3 + punctuation
        val estimatedTokens = (wordCount * TOKENS_PER_WORD) + punctuationCount

        // Round up to nearest integer
        return kotlin.math.ceil(estimatedTokens).toInt()
    }

    /**
     * Checks if the given text exceeds the specified token limit.
     *
     * @param text The text to check
     * @param maxTokens The maximum number of tokens allowed (defaults to MAX_CONTEXT_TOKENS)
     * @return True if the estimated token count exceeds the limit, false otherwise
     *
     * @sample
     * ```
     * val longText = "..." // Some long text
     * if (TokenCounter.exceedsLimit(longText)) {
     *     // Prune conversation history or show warning
     * }
     * ```
     */
    fun exceedsLimit(text: String, maxTokens: Int = MAX_CONTEXT_TOKENS): Boolean {
        return estimateTokens(text) > maxTokens
    }
}

