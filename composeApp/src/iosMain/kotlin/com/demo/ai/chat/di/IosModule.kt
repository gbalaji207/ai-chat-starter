package com.demo.ai.chat.di

import com.demo.ai.chat.data.local.AppDatabase
import com.demo.ai.chat.data.local.DatabaseBuilder
import org.koin.dsl.module

/**
 * iOS-specific Koin module for Room database and related dependencies.
 * This module provides platform-specific implementations for iOS.
 */
val iosModule = module {

    // AppDatabase - single instance using DatabaseBuilder (no context needed for iOS)
    single<AppDatabase> {
        DatabaseBuilder().build()
    }
}
