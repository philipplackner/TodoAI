package com.plcoding.todoai.todo.data

import com.plcoding.todoai.todo.domain.Todo
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

class RoomTodoDataSource(
    private val dao: TodoDao
) : TodoLocalDataSource {

    override fun observeTodos(): Flow<List<Todo>> {
        return dao.observeTodos().map { entities -> entities.map { it.toTodo() } }
    }

    override suspend fun getTodos(): List<Todo> {
        return dao.getTodos().map { it.toTodo() }
    }

    override suspend fun getTodosDueBetween(from: LocalDate, to: LocalDate): List<Todo> {
        return dao.getTodosDueBetween(
            fromEpochDays = from.toEpochDays(),
            toEpochDays = to.toEpochDays()
        ).map { it.toTodo() }
    }

    override suspend fun getTodosDueBefore(date: LocalDate): List<Todo> {
        return dao.getTodosDueBefore(date.toEpochDays()).map { it.toTodo() }
    }

    override suspend fun getTodosWithoutDueDate(): List<Todo> {
        return dao.getTodosWithoutDueDate().map { it.toTodo() }
    }

    override suspend fun findTodosByTitle(query: String): List<Todo> {
        return dao.findTodosByTitle(query).map { it.toTodo() }
    }

    override suspend fun searchTodos(text: String): List<Todo> {
        return dao.searchTodos(text).map { it.toTodo() }
    }

    override suspend fun getTodo(id: String): Todo? {
        return dao.getTodo(id)?.toTodo()
    }

    override suspend fun upsertTodo(todo: Todo) {
        dao.upsertTodo(todo.toTodoEntity())
    }

    override suspend fun setDone(id: String, isDone: Boolean) {
        dao.setDone(
            id = id,
            isDone = isDone,
            updatedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun setDueDate(id: String, dueDate: LocalDate?) {
        dao.setDueDate(
            id = id,
            dueDateEpochDays = dueDate?.toEpochDays(),
            updatedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun deleteTodo(id: String) {
        dao.deleteTodo(id)
    }
}
