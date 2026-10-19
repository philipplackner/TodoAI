package com.plcoding.todoai.todo.presentation

import com.plcoding.todoai.core.presentation.toFormattedDate
import com.plcoding.todoai.core.presentation.toFormattedDateTime
import com.plcoding.todoai.todo.domain.Todo
import kotlinx.datetime.LocalDate

data class TodoUi(
    val id: String,
    val title: String,
    val description: String,
    val isDone: Boolean,
    val formattedCreatedAt: String,
    val formattedDueDate: String? = null,
    val isOverdue: Boolean = false
)

fun Todo.toTodoUi(today: LocalDate): TodoUi = TodoUi(
    id = id,
    title = title,
    description = description,
    isDone = isDone,
    formattedCreatedAt = createdAt.toFormattedDateTime(),
    formattedDueDate = dueDate?.toFormattedDate(),
    isOverdue = !isDone && dueDate != null && dueDate < today
)
