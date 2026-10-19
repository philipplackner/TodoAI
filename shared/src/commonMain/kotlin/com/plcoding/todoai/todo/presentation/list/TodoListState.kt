package com.plcoding.todoai.todo.presentation.list

import androidx.compose.runtime.Stable
import com.plcoding.todoai.todo.presentation.TodoUi

@Stable
data class TodoListState(
    val todos: List<TodoUi> = emptyList(),
    val isLoading: Boolean = true,
    val isChatOpen: Boolean = false
) {
    val openCount: Int get() = todos.count { !it.isDone }
    val doneCount: Int get() = todos.count { it.isDone }
}
