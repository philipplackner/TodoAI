package com.plcoding.todoai.todo.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.plcoding.todoai.core.presentation.toFormattedDateTime
import com.plcoding.todoai.todo.domain.Todo
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import com.plcoding.todoai.todo.presentation.TodoDetailRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.uuid.Uuid

class TodoDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val todoDataSource: TodoLocalDataSource
) : ViewModel() {

    private val todoId = savedStateHandle.toRoute<TodoDetailRoute>().todoId
    private var existingTodo: Todo? = null

    private val _state = MutableStateFlow(
        TodoDetailState(
            title = savedStateHandle[KEY_TITLE] ?: "",
            description = savedStateHandle[KEY_DESCRIPTION] ?: "",
            isDone = savedStateHandle[KEY_IS_DONE] ?: false,
            dueDate = savedStateHandle.get<Long>(KEY_DUE_DATE)?.let { LocalDate.fromEpochDays(it) },
            isNewTodo = todoId == null,
            isLoading = todoId != null
        )
    )
    val state = _state.asStateFlow()

    private val _events = Channel<TodoDetailEvent>()
    val events = _events.receiveAsFlow()

    init {
        if (todoId != null) {
            loadTodo(todoId)
        }
    }

    fun onAction(action: TodoDetailAction) {
        when (action) {
            is TodoDetailAction.OnTitleChange -> {
                savedStateHandle[KEY_TITLE] = action.title
                _state.update { it.copy(title = action.title) }
            }
            is TodoDetailAction.OnDescriptionChange -> {
                savedStateHandle[KEY_DESCRIPTION] = action.description
                _state.update { it.copy(description = action.description) }
            }
            is TodoDetailAction.OnDoneChange -> {
                savedStateHandle[KEY_IS_DONE] = action.isDone
                _state.update { it.copy(isDone = action.isDone) }
            }
            TodoDetailAction.OnDueDateClick -> {
                _state.update { it.copy(isDatePickerOpen = true) }
            }
            TodoDetailAction.OnDueDatePickerDismiss -> {
                _state.update { it.copy(isDatePickerOpen = false) }
            }
            is TodoDetailAction.OnDueDateChange -> {
                savedStateHandle[KEY_DUE_DATE] = action.dueDate?.toEpochDays()
                _state.update { it.copy(dueDate = action.dueDate, isDatePickerOpen = false) }
            }
            TodoDetailAction.OnSaveClick -> saveTodo()
            TodoDetailAction.OnDeleteClick -> deleteTodo()
        }
    }

    private fun loadTodo(id: String) {
        viewModelScope.launch {
            val todo = todoDataSource.getTodo(id)
            if (todo == null) {
                _events.send(TodoDetailEvent.NavigateBack)
                return@launch
            }
            existingTodo = todo
            _state.update {
                it.copy(
                    title = savedStateHandle[KEY_TITLE] ?: todo.title,
                    description = savedStateHandle[KEY_DESCRIPTION] ?: todo.description,
                    isDone = savedStateHandle[KEY_IS_DONE] ?: todo.isDone,
                    dueDate = if (savedStateHandle.contains(KEY_DUE_DATE)) it.dueDate else todo.dueDate,
                    formattedCreatedAt = todo.createdAt.toFormattedDateTime(),
                    formattedUpdatedAt = todo.updatedAt.toFormattedDateTime(),
                    isLoading = false
                )
            }
        }
    }

    private fun saveTodo() {
        val currentState = state.value
        if (!currentState.canSave) {
            return
        }
        viewModelScope.launch {
            val now = Clock.System.now()
            val todo = existingTodo?.copy(
                title = currentState.title.trim(),
                description = currentState.description.trim(),
                isDone = currentState.isDone,
                dueDate = currentState.dueDate,
                updatedAt = now
            ) ?: Todo(
                id = Uuid.random().toString(),
                title = currentState.title.trim(),
                description = currentState.description.trim(),
                isDone = currentState.isDone,
                dueDate = currentState.dueDate,
                createdAt = now,
                updatedAt = now
            )
            todoDataSource.upsertTodo(todo)
            _events.send(TodoDetailEvent.NavigateBack)
        }
    }

    private fun deleteTodo() {
        val id = todoId ?: return
        viewModelScope.launch {
            todoDataSource.deleteTodo(id)
            _events.send(TodoDetailEvent.NavigateBack)
        }
    }

    private companion object {
        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_IS_DONE = "isDone"
        const val KEY_DUE_DATE = "dueDate"
    }
}
