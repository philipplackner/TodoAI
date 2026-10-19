package com.plcoding.todoai.todo.data

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [TodoEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@ConstructedBy(TodoDatabaseConstructor::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract val todoDao: TodoDao

    companion object {
        const val DB_NAME = "todos.db"
    }
}

@Suppress("KotlinNoActualForExpect")
expect object TodoDatabaseConstructor : RoomDatabaseConstructor<TodoDatabase> {
    override fun initialize(): TodoDatabase
}

fun createTodoDatabase(builder: RoomDatabase.Builder<TodoDatabase>): TodoDatabase = builder
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
