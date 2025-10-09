package com.demo.ai.chat.di

import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.demo.ai.chat.data.repository.ChatRepository
import com.demo.ai.chat.data.repository.ChatRepositoryImpl
import com.demo.ai.chat.data.source.OpenAIDataSource
import com.demo.ai.chat.ui.ChatViewModel
import org.koin.dsl.module

/**
 * Koin dependency injection module for the application.
 * Defines how dependencies are created and provided.
 */
val appModule = module {

    // OpenAI client - single instance
    single {
        OpenAI(
            token = "sk-xxxx",
            logging = LoggingConfig(LogLevel.All)
        )
    }

    // OpenAI data source - single instance
    single {
        OpenAIDataSource(openAI = get())
    }

    // Chat repository - single instance, bind implementation to interface
    single<ChatRepository> {
        ChatRepositoryImpl(dataSource = get())
    }

    // ChatViewModel - factory (new instance per request)
    factory {
        ChatViewModel(repository = get())
    }
}
