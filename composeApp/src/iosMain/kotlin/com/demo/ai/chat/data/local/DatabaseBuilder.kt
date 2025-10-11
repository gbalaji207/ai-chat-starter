package com.demo.ai.chat.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSHomeDirectory

/**
 * iOS implementation of DatabaseBuilder.
 */
actual class DatabaseBuilder {

    /**
     * Builds and returns an AppDatabase instance for iOS.
     * @return The configured AppDatabase instance.
     */
    actual fun build(): AppDatabase {
        val dbFilePath = NSHomeDirectory() + "/ai_chat.db"

        return Room.databaseBuilder<AppDatabase>(
            name = dbFilePath
        )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
        .build()
    }
}

