package com.plcoding.todoai.todo.presentation.detail

data class TodoDetailState(
    val title: String = "",
    val description: String = "",
    val isDone: Boolean = false,
    val formattedCreatedAt: String? = null,
    val formattedUpdatedAt: String? = null,
    val isNewTodo: Boolean = true,
    val isLoading: Boolean = false
) {
    val canSave: Boolean get() = title.isNotBlank() && !isLoading
}
