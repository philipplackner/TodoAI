package com.plcoding.todoai.todo.presentation.list

sealed interface TodoListAction {
    data class OnDoneToggle(val todoId: String, val isDone: Boolean) : TodoListAction
    data object OnChatClick : TodoListAction
    data object OnChatDismiss : TodoListAction
}
