package com.demo.ai.chat

import android.app.Application
import com.demo.ai.chat.di.androidModule
import com.demo.ai.chat.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Application class for initializing Koin dependency injection.
 */
class ChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin for Android
        startKoin {
            androidLogger()
            androidContext(this@ChatApplication)
            modules(appModule, androidModule)
        }
    }
}
