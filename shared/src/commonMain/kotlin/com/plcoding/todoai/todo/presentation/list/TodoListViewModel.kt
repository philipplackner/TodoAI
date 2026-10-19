package com.plcoding.todoai.todo.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import com.plcoding.todoai.todo.presentation.toTodoUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class TodoListViewModel(
    private val todoDataSource: TodoLocalDataSource
) : ViewModel() {

    private val isChatOpen = MutableStateFlow(false)

    val state = combine(
        todoDataSource.observeTodos(),
        isChatOpen
    ) { todos, isChatOpen ->
        TodoListState(
            todos = todos.map { it.toTodoUi(today = Clock.System.todayIn(TimeZone.currentSystemDefault())) },
            isLoading = false,
            isChatOpen = isChatOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TodoListState()
    )

    fun onAction(action: TodoListAction) {
        when (action) {
            is TodoListAction.OnDoneToggle -> {
                viewModelScope.launch {
                    todoDataSource.setDone(action.todoId, action.isDone)
                }
            }
            TodoListAction.OnChatClick -> isChatOpen.value = true
            TodoListAction.OnChatDismiss -> isChatOpen.value = false
        }
    }
}
