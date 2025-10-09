package com.demo.ai.chat

import androidx.compose.ui.window.ComposeUIViewController
import com.demo.ai.chat.di.appModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {
    // Initialize Koin for iOS
    startKoin {
        modules(appModule)
    }

    App()
}