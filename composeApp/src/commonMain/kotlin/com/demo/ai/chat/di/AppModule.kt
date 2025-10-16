package com.demo.ai.chat.di

import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.demo.ai.chat.data.local.AppDatabase
import com.demo.ai.chat.data.local.dao.ConversationDao
import com.demo.ai.chat.data.local.dao.MessageDao
import com.demo.ai.chat.data.repository.ChatRepository
import com.demo.ai.chat.data.repository.ChatRepositoryImpl
import com.demo.ai.chat.data.repository.ConversationManager
import com.demo.ai.chat.data.source.OpenAIDataSource
import com.demo.ai.chat.data.util.RetryPolicy
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

    // RetryPolicy - single instance
    single {
        RetryPolicy(
            maxAttempts = 3,
            baseDelayMs = 1000,
            maxDelayMs = 16000
        )
    }

    // OpenAI data source - single instance
    single {
        OpenAIDataSource(openAI = get())
    }

    // Room Database DAOs - single instances from platform-provided AppDatabase
    single<ConversationDao> {
        get<AppDatabase>().conversationDao()
    }

    single<MessageDao> {
        get<AppDatabase>().messageDao()
    }

    // ConversationManager - single instance for managing conversation context and tokens
    single {
        ConversationManager(
            messageDao = get(),
            conversationDao = get(),
            maxTokens = 3000
        )
    }

    // Chat repository - single instance, bind implementation to interface
    single<ChatRepository> {
        ChatRepositoryImpl(
            dataSource = get(),
            conversationManager = get(),
            retryPolicy = get()
        )
    }

    // ChatViewModel - factory (new instance per request)
    factory {
        ChatViewModel(
            repository = get(),
            conversationManager = get()
        )
    }
}
