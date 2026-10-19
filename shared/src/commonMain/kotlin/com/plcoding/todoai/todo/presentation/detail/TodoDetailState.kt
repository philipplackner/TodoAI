package com.plcoding.todoai.todo.presentation.detail

import com.plcoding.todoai.core.presentation.toFormattedDate
import kotlinx.datetime.LocalDate

data class TodoDetailState(
    val title: String = "",
    val description: String = "",
    val isDone: Boolean = false,
    val dueDate: LocalDate? = null,
    val isDatePickerOpen: Boolean = false,
    val formattedCreatedAt: String? = null,
    val formattedUpdatedAt: String? = null,
    val isNewTodo: Boolean = true,
    val isLoading: Boolean = false
) {
    val canSave: Boolean get() = title.isNotBlank() && !isLoading
    val formattedDueDate: String? get() = dueDate?.toFormattedDate()
}
