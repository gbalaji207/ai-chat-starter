package com.demo.ai.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.ai.chat.data.model.AIError
import com.demo.ai.chat.data.model.ChatResponse
import com.demo.ai.chat.data.prompts.AIPersonality
import com.demo.ai.chat.data.repository.ChatRepository
import com.demo.ai.chat.data.repository.ConversationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the chat screen state and business logic.
 *
 * Handles automatic retry logic with user-friendly messaging, showing:
 * - Retry progress with countdown timers
 * - Clear, actionable error messages
 * - Streaming response updates
 *
 * @property repository The repository for chat operations
 * @property conversationManager Manages conversation persistence and token limits
 */
class ChatViewModel(
    private val repository: ChatRepository,
    private val conversationManager: ConversationManager
) : ViewModel() {

    private val conversationId = "default"

    private val _uiState = MutableStateFlow(ChatUiState())

    /**
     * The current UI state for the chat screen.
     */
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Load existing conversation messages from database on initialization
        loadConversationHistory()
    }

    /**
     * Loads the conversation history from the database.
     */
    private fun loadConversationHistory() {
        viewModelScope.launch {
            try {
                val messages = conversationManager.loadConversation(conversationId)
                _uiState.update { it.copy(messages = messages) }
            } catch (e: Exception) {
                // Handle error gracefully - start with empty conversation
                _uiState.update {
                    it.setError(AIError.Unknown(throwable = e))
                }
            }
        }
    }

    /**
     * Sends a message to the AI and handles the streaming response.
     *
     * Handles various response states including:
     * - Streaming chunks (incremental response updates)
     * - Retry attempts (with user-friendly progress messages)
     * - Errors (with actionable, non-technical messages)
     * - Completion (successful response finish)
     *
     * ## Retry State Management:
     * When a transient error occurs (network, rate limit, timeout), the system
     * automatically retries with exponential backoff. The UI shows:
     * - User-friendly explanation of what went wrong
     * - Countdown timer until next retry attempt
     * - Current attempt number out of total attempts
     *
     * When retry succeeds, the retry state is automatically cleared.
     *
     * @param message The message text to send
     */
    fun sendMessage(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            // Add user message to state
            _uiState.update { it.addUserMessage(message) }

            // Add empty AI message with streaming flag
            _uiState.update { it.addAiMessage(text = "", isStreaming = true) }

            // Get conversation history (excluding the empty AI message we just added)
            val conversationHistory = _uiState.value.messages.dropLast(1)

            // Get the current personality
            val personality = _uiState.value.selectedPersonality

            // Call repository with personality and collect the streaming response
            repository.sendMessage(message, conversationHistory, personality)
                .collect { response ->
                    when (response) {
                        is ChatResponse.StreamingChunk -> {
                            // Accumulate the chunk to the last message
                            _uiState.update { state ->
                                val currentText = state.messages.lastOrNull()?.text ?: ""
                                state.updateLastMessage(currentText + response.chunk)
                            }
                        }

                        is ChatResponse.StreamingComplete -> {
                            // Mark streaming as complete and clear retry state
                            _uiState.update { state ->
                                val updatedMessages = state.messages.dropLast(1) +
                                        state.messages.last().copy(isStreaming = false)
                                state.copy(
                                    messages = updatedMessages,
                                    isStreaming = false
                                ).clearRetrying()  // Clear retry state on success
                            }
                        }

                        is ChatResponse.Retrying -> {
                            // Update UI to show retry progress with user-friendly message
                            _uiState.update { state ->
                                state.setRetrying(
                                    attempt = response.attempt,
                                    delayMs = response.delayMs,
                                    error = response.error,
                                    maxAttempts = 3
                                )
                            }
                        }

                        is ChatResponse.Error -> {
                            // Remove the empty AI message and set user-friendly error
                            _uiState.update { state ->
                                state.copy(
                                    messages = state.messages.dropLast(1),
                                    isStreaming = false
                                ).setError(response.error)
                            }
                        }

                        is ChatResponse.Success -> {
                            // Handle non-streaming success response (if ever used)
                            _uiState.update { state ->
                                state.updateLastMessage(response.message)
                                    .copy(isStreaming = false)
                                    .clearRetrying()  // Clear retry state on success
                            }
                        }
                    }
                }
        }
    }

    /**
     * Clears the current conversation.
     * Deletes all messages from the database and clears the UI state.
     */
    fun clearConversation() {
        viewModelScope.launch {
            try {
                // Clear messages from database
                conversationManager.clearConversation(conversationId)

                // Clear messages from UI state
                _uiState.update { it.copy(messages = emptyList()) }
            } catch (e: Exception) {
                // Handle error gracefully with user-friendly message
                _uiState.update {
                    it.setError(AIError.Unknown(throwable = e))
                }
            }
        }
    }

    /**
     * Updates the input text in the UI state.
     *
     * @param text The new input text
     */
    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * Clears any error message from the UI state.
     */
    fun clearError() {
        _uiState.update { it.clearError() }
    }

    /**
     * Updates the selected AI personality mode.
     * This allows users to switch between different AI response styles (e.g., Professional, Creative, Code Reviewer).
     *
     * @param personality The AI personality to use for future messages
     */
    fun selectPersonality(personality: AIPersonality) {
        _uiState.update { it.updatePersonality(personality) }
    }
}
