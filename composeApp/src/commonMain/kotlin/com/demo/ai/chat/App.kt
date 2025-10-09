package com.demo.ai.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.demo.ai.chat.ui.ChatScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        ChatScreen()
    }
}