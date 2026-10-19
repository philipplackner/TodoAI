package com.plcoding.todoai.todo.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun todoDatabaseBuilder(context: Context): RoomDatabase.Builder<TodoDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(TodoDatabase.DB_NAME)
    return Room.databaseBuilder<TodoDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}
