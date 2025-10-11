package com.demo.ai.chat.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * Android implementation of DatabaseBuilder.
 * @param context Android application context.
 */
actual class DatabaseBuilder(private val context: Context) {

    /**
     * Builds and returns an AppDatabase instance for Android.
     * @return The configured AppDatabase instance.
     */
    actual fun build(): AppDatabase {
        val dbFile = context.getDatabasePath("ai_chat.db")

        return Room.databaseBuilder<AppDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
        .build()
    }
}

