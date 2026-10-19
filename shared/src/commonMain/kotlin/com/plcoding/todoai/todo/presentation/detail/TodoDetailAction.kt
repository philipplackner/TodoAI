package com.plcoding.todoai.todo.presentation.detail

sealed interface TodoDetailAction {
    data class OnTitleChange(val title: String) : TodoDetailAction
    data class OnDescriptionChange(val description: String) : TodoDetailAction
    data class OnDoneChange(val isDone: Boolean) : TodoDetailAction
    data object OnSaveClick : TodoDetailAction
    data object OnDeleteClick : TodoDetailAction
}
