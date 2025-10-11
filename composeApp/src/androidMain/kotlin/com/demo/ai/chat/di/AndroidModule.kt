package com.demo.ai.chat.di

import com.demo.ai.chat.data.local.AppDatabase
import com.demo.ai.chat.data.local.DatabaseBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific Koin module for Room database and related dependencies.
 * This module provides platform-specific implementations for Android.
 */
val androidModule = module {

    // AppDatabase - single instance using DatabaseBuilder with Android context
    single<AppDatabase> {
        DatabaseBuilder(androidContext()).build()
    }
}
