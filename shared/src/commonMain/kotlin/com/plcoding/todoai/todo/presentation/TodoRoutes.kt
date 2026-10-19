package com.plcoding.todoai.todo.presentation

import kotlinx.serialization.Serializable

@Serializable
data object TodoListRoute

@Serializable
data class TodoDetailRoute(val todoId: String? = null)
