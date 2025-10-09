package com.demo.ai.chat.data.model

import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Represents a chat message in the conversation.
 *
 * @property id Unique identifier for the message
 * @property text The content of the message
 * @property isUser True if the message is from the user, false if from the AI
 * @property timestamp The time when the message was created (in milliseconds since epoch)
 * @property isStreaming True if the message is currently being streamed, false otherwise
 */
data class ChatMessage @OptIn(ExperimentalTime::class) constructor(
    val id: String = generateId(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val isStreaming: Boolean = false
) {
    companion object {
        /**
         * Generates a unique identifier for a chat message.
         *
         * @return A unique string ID combining timestamp and random value
         */
        @OptIn(ExperimentalTime::class)
        fun generateId(): String {
            return "${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(10000)}"
        }
    }
}
