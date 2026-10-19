package com.plcoding.todoai.todo.domain

import kotlin.time.Instant

data class Todo(
    val id: String,
    val title: String,
    val description: String,
    val isDone: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
