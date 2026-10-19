package com.plcoding.todoai.todo.presentation.detail

import kotlinx.datetime.LocalDate

sealed interface TodoDetailAction {
    data class OnTitleChange(val title: String) : TodoDetailAction
    data class OnDescriptionChange(val description: String) : TodoDetailAction
    data class OnDoneChange(val isDone: Boolean) : TodoDetailAction
    data object OnDueDateClick : TodoDetailAction
    data object OnDueDatePickerDismiss : TodoDetailAction
    data class OnDueDateChange(val dueDate: LocalDate?) : TodoDetailAction
    data object OnSaveClick : TodoDetailAction
    data object OnDeleteClick : TodoDetailAction
}
