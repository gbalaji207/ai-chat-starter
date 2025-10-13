package com.demo.ai.chat.data.source

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.demo.ai.chat.data.model.ChatMessage
import com.demo.ai.chat.data.model.ChatResponse
import com.demo.ai.chat.data.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import com.aallam.openai.api.chat.ChatMessage as OpenAIChatMessage

/**
 * Data source that wraps OpenAI API calls for chat completions.
 *
 * @property openAI The OpenAI client instance used for API calls
 */
class OpenAIDataSource(
    private val openAI: OpenAI
) {

    /**
     * Streams chat completion responses from the OpenAI API.
     *
     * @param messages The list of chat messages to send to the API
     * @param temperature Controls randomness in the response (0.0 to 2.0). Default is 0.7
     * @return Flow of ChatResponse objects representing streaming chunks, completion, or errors
     */
    fun streamChatCompletion(
        messages: List<ChatMessage>,
        temperature: Double = 0.7
    ): Flow<ChatResponse> = flow {
        // Convert our ChatMessage format to OpenAI's ChatMessage format
        val openAIMessages = messages.map { message ->
            OpenAIChatMessage(
                role = when (message.role) {
                    MessageRole.USER -> ChatRole.User
                    MessageRole.ASSISTANT -> ChatRole.Assistant
                    MessageRole.SYSTEM -> ChatRole.System
                },
                content = message.text
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
        openAI.chatCompletions(chatCompletionRequest)
            .catch { error ->
                // Emit error response
                emit(
                    ChatResponse.Error(
                        message = error.message ?: "Unknown error occurred",
                        throwable = error
                    )
                )
            }
            .onCompletion { error ->
                // Only emit completion if no error occurred and we received content
                if (error == null && hasContent) {
                    emit(ChatResponse.StreamingComplete)
                }
            }
            .collect { chunk ->
                // Extract content from the chunk
                val content = chunk.choices.firstOrNull()?.delta?.content

                if (content != null) {
                    hasContent = true
                    emit(ChatResponse.StreamingChunk(chunk = content))
                }
            }
    }.catch { error ->
        // Catch any errors that occur during flow creation
        emit(
            ChatResponse.Error(
                message = error.message ?: "Failed to stream chat completion",
                throwable = error
            )
        )
    }
}

