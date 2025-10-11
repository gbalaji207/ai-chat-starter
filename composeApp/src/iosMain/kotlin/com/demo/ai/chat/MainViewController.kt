package com.demo.ai.chat

import androidx.compose.ui.window.ComposeUIViewController
import com.demo.ai.chat.di.appModule
import com.demo.ai.chat.di.iosModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {
    // Initialize Koin for iOS
    startKoin {
        modules(appModule, iosModule)
    }

    App()
}