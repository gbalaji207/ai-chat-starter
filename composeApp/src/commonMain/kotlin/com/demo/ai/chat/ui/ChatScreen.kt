package com.demo.ai.chat.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.demo.ai.chat.data.model.ChatMessage
import com.demo.ai.chat.data.model.MessageRole
import com.demo.ai.chat.data.prompts.AIPersonality
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    // Get ViewModel from Koin
    val viewModel = koinViewModel<ChatViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var scrollCounter by remember { mutableStateOf(0) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showPersonalityMenu by remember { mutableStateOf(false) }

    // Auto-scroll when messages change
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(
                index = uiState.messages.size - 1,
                scrollOffset = Int.MAX_VALUE
            )
        }
    }

    // Throttled scroll during streaming
    LaunchedEffect(uiState.messages.lastOrNull()?.text) {
        if (uiState.isStreaming && uiState.messages.isNotEmpty()) {
            scrollCounter++
            if (scrollCounter % 3 == 0) {
                listState.scrollToItem(
                    index = uiState.messages.size - 1,
                    scrollOffset = Int.MAX_VALUE
                )
            }
        } else {
            scrollCounter = 0 // Reset when not streaming
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Chat",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = uiState.selectedPersonality.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Personality selector button
                    IconButton(
                        onClick = {
                            showPersonalityMenu = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonOutline,
                            contentDescription = "Select personality"
                        )
                    }

                    // Personality dropdown menu
                    DropdownMenu(
                        expanded = showPersonalityMenu,
                        onDismissRequest = { showPersonalityMenu = false }
                    ) {
                        AIPersonality.all.forEach { personality ->
                            DropdownMenuItem(
                                text = { Text(personality.name) },
                                onClick = {
                                    viewModel.selectPersonality(personality)
                                    showPersonalityMenu = false
                                },
                                modifier = if (personality == uiState.selectedPersonality) {
                                    Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                } else {
                                    Modifier
                                }
                            )
                        }
                    }

                    // Clear conversation button
                    IconButton(
                        onClick = { showClearDialog = true },
                        enabled = uiState.messages.isNotEmpty() && !uiState.isStreaming
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear conversation"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            // Display error message if present
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.error ?: "")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
        ) {

            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(message)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Streaming indicator - only show when actively streaming and NOT retrying
                if (uiState.isStreaming && !uiState.isRetrying) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI is typing...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // NEW: Retry Progress Indicator - shown between messages and input area
            if (uiState.isRetrying && uiState.retryMessage != null) {
                RetryProgressIndicator(
                    message = uiState.retryMessage!!,
                    attempt = uiState.retryAttempt,
                    maxAttempts = uiState.maxRetryAttempts
                )
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = { viewModel.updateInputText(it) },
                    label = { Text("Enter text") },
                    placeholder = { Text("Type something...") },
                    enabled = !uiState.isStreaming && !uiState.isRetrying,
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        viewModel.sendMessage(uiState.inputText)
                    },
                    enabled = !uiState.isStreaming && !uiState.isRetrying && uiState.inputText.isNotBlank(),
                ) {
                    Text("Send")
                }
            }
        }
    }

    // Clear conversation confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Conversation?") },
            text = {
                Text(
                    "This will permanently delete all messages in this conversation. " +
                            "This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearConversation()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Displays a retry progress indicator with user-friendly messaging.
 *
 * Shows when the app is automatically retrying a failed request, providing:
 * - Clear explanation of what went wrong
 * - Visual progress indicator showing retry attempt
 * - Countdown information
 * - Professional, non-alarming design
 *
 * Design uses warning colors (yellow/orange) rather than error colors (red)
 * to indicate a temporary, recoverable situation.
 *
 * @param message User-friendly message explaining the retry (from AIError.toUserMessage())
 * @param attempt Current retry attempt number (1-based)
 * @param maxAttempts Maximum number of retry attempts allowed
 */
@Composable
private fun RetryProgressIndicator(
    message: String,
    attempt: Int,
    maxAttempts: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Refresh/warning icon
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Retrying",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Message and progress column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Retry message text
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                // Progress bar
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { attempt.toFloat() / maxAttempts.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        trackColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.3f),
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.role == MessageRole.USER) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (message.role == MessageRole.USER) {
                MaterialTheme.colorScheme.primary
            } else {
                // Different color when streaming to show it's being generated
                if (message.isStreaming) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            },
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.text,
                    color = if (message.role == MessageRole.USER) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )

                // Show a cursor/indicator when streaming
                if (message.isStreaming && message.text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "▋",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}