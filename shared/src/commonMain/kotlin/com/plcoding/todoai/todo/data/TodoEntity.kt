package com.plcoding.todoai.todo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isDone: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
