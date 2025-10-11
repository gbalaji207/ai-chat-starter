package com.demo.ai.chat.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.demo.ai.chat.data.local.dao.ConversationDao
import com.demo.ai.chat.data.local.dao.MessageDao
import com.demo.ai.chat.data.local.entity.ConversationEntity
import com.demo.ai.chat.data.local.entity.MessageEntity

/**
 * Room database for the AI Chat application.
 * Contains conversations and messages tables.
 */
@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to conversation data operations.
     */
    abstract fun conversationDao(): ConversationDao

    /**
     * Provides access to message data operations.
     */
    abstract fun messageDao(): MessageDao
}

/**
 * Database constructor for Room KMP.
 * Platform-specific implementations will be provided in androidMain and iosMain.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>

