package com.demo.ai.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val apiKey = "sk-xxxx"
    val openAI = OpenAI(apiKey, logging = LoggingConfig(LogLevel.All))

    val coroutineScope = rememberCoroutineScope()

    // State management
    var messageInput by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("AI Chat (Day 2)") })
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxSize()
                    .padding(paddingValues).imePadding(),
            ) {

                // Messages List
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    state = listState
                ) {
                    items(messages) { message ->
                        MessageBubble(message)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Loading indicator
                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp).padding(8.dp)
                                )
                                Text("AI is thinking...", modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }

                // Input Area
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        label = { Text("Enter text") },
                        placeholder = { Text("Type something...") },
                        enabled = isLoading.not(),
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (messageInput.isBlank()) return@Button

                            // Call OpenAI API
                            coroutineScope.launch {
                                try {
                                    // Add user message to the list
                                    val userMessage = messageInput
                                    messages = messages + Message(userMessage, isUser = true)
                                    messageInput = ""
                                    isLoading = true

                                    // Auto-scroll to bottom
                                    listState.animateScrollToItem(messages.size - 1)

                                    // Build conversation history for API
                                    val chatMessages = messages.map { msg ->
                                        ChatMessage(
                                            role = if (msg.isUser) ChatRole.User else ChatRole.Assistant,
                                            content = msg.text,
                                        )
                                    }

                                    val chatCompletionRequest = ChatCompletionRequest(
                                        model = ModelId("gpt-3.5-turbo"),
                                        messages = chatMessages,
                                        temperature = 0.7, // Balanced creativity
                                        maxTokens = 500 // Limit response length
                                    )

                                    // Make API call
                                    val response = openAI.chatCompletion(chatCompletionRequest)

                                    // Extract AI response
                                    val aiMessage = response.choices.firstOrNull()?.message?.content
                                        ?: "No response"

                                    // Add AI response to messages
                                    messages = messages + Message(aiMessage, isUser = false)

                                    // Auto-scroll to bottom
                                    listState.animateScrollToItem(messages.size - 1)
                                } catch (e: Exception) {
                                    messages =
                                        messages + Message("Error: ${e.message}", isUser = false)
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = isLoading.not(),
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (message.isUser) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// Data class to represent a chat message
data class Message(
    val text: String, val isUser: Boolean
)