package com.plcoding.todoai.todo.domain

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class Todo(
    val id: String,
    val title: String,
    val description: String,
    val isDone: Boolean,
    val dueDate: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant
)
