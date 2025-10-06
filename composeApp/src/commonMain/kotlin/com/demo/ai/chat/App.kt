package com.demo.ai.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import aichatstarter.composeapp.generated.resources.Res
import aichatstarter.composeapp.generated.resources.compose_multiplatform
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    val apiKey = "sk-xxxx"

    val coroutineScope = rememberCoroutineScope()

    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = {
                showContent = !showContent

                coroutineScope.launch {
                    val openAI = OpenAI(apiKey, logging = LoggingConfig(LogLevel.All))

                    println("> Getting available models...")
                    openAI.models().forEach(::println)

                    println("\n> Getting gpt-oss model...")
                    val model = openAI.model(modelId = ModelId("gpt-3.5-turbo"))
                    println(model)

                    val chatCompletionRequest = ChatCompletionRequest(
                        model.id, listOf(
                            ChatMessage(role = ChatRole.User, content = "Hello, AI!")
                        )
                    )
                    openAI.chatCompletion(chatCompletionRequest).choices.forEach(::println)

                    println("\n>️ Creating chat completions stream...")
                    openAI.chatCompletions(chatCompletionRequest)
                        .onEach { print(it.choices.first().delta?.content.orEmpty()) }
                        .onCompletion { println() }
                        .launchIn(this)
                        .join()
                }
            }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}