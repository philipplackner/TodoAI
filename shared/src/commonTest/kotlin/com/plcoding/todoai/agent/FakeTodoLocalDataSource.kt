package com.plcoding.todoai.agent

import com.plcoding.todoai.todo.domain.Todo
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

class FakeTodoLocalDataSource(initialTodos: List<Todo> = emptyList()) : TodoLocalDataSource {

    val todos = MutableStateFlow(initialTodos)

    override fun observeTodos(): Flow<List<Todo>> = todos

    override suspend fun getTodos(): List<Todo> = todos.value

    override suspend fun getTodosDueBetween(from: LocalDate, to: LocalDate): List<Todo> {
        return todos.value.filter { it.dueDate != null && it.dueDate in from..to }
    }

    override suspend fun getTodosDueBefore(date: LocalDate): List<Todo> {
        return todos.value.filter { it.dueDate != null && it.dueDate < date }
    }

    override suspend fun getTodosWithoutDueDate(): List<Todo> = todos.value.filter { it.dueDate == null }

    override suspend fun findTodosByTitle(query: String): List<Todo> {
        return todos.value.filter { it.title.contains(query, ignoreCase = true) }
    }

    override suspend fun searchTodos(text: String): List<Todo> {
        return todos.value.filter {
            it.title.contains(text, ignoreCase = true) || it.description.contains(text, ignoreCase = true)
        }
    }

    override suspend fun getTodo(id: String): Todo? = todos.value.find { it.id == id }

    override suspend fun upsertTodo(todo: Todo) {
        todos.update { current -> current.filterNot { it.id == todo.id } + todo }
    }

    override suspend fun setDone(id: String, isDone: Boolean) {
        todos.update { current -> current.map { if (it.id == id) it.copy(isDone = isDone) else it } }
    }

    override suspend fun setDueDate(id: String, dueDate: LocalDate?) {
        todos.update { current -> current.map { if (it.id == id) it.copy(dueDate = dueDate) else it } }
    }

    override suspend fun deleteTodo(id: String) {
        todos.update { current -> current.filterNot { it.id == id } }
    }
}

fun todo(
    id: String,
    title: String,
    dueDate: LocalDate? = null,
    isDone: Boolean = false
) = Todo(
    id = id,
    title = title,
    description = "",
    isDone = isDone,
    dueDate = dueDate,
    createdAt = Instant.fromEpochMilliseconds(0),
    updatedAt = Instant.fromEpochMilliseconds(0)
)
