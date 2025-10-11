package com.demo.ai.chat.data.local

/**
 * Database builder for creating AppDatabase instances.
 * Platform-specific implementations handle database instantiation.
 */
expect class DatabaseBuilder {
    /**
     * Builds and returns an AppDatabase instance.
     * @return The configured AppDatabase instance.
     */
    fun build(): AppDatabase
}

