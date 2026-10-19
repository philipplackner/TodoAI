package com.plcoding.todoai.todo.presentation.detail

sealed interface TodoDetailEvent {
    data object NavigateBack : TodoDetailEvent
}
