package com.plcoding.todoai.todo.presentation

import com.plcoding.todoai.core.presentation.toFormattedDateTime
import com.plcoding.todoai.todo.domain.Todo

data class TodoUi(
    val id: String,
    val title: String,
    val description: String,
    val isDone: Boolean,
    val formattedCreatedAt: String
)

fun Todo.toTodoUi(): TodoUi = TodoUi(
    id = id,
    title = title,
    description = description,
    isDone = isDone,
    formattedCreatedAt = createdAt.toFormattedDateTime()
)
