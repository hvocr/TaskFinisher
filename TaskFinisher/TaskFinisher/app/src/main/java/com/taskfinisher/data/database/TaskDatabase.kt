package com.taskfinisher.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Single Room database for the app.
 * Version history:
 *   1 → initial schema
 *
 * To add a migration: increment version, add a Migration object to MIGRATIONS,
 * then pass it to .addMigrations(*MIGRATIONS) in the Hilt module.
 */
@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = true   // Keep schema JSON in /schemas for migration auditing
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        const val DATABASE_NAME = "taskfinisher.db"
    }
}
