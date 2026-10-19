package com.plcoding.todoai.todo.data

import com.plcoding.todoai.todo.domain.Todo
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class RoomTodoDataSource(
    private val dao: TodoDao
) : TodoLocalDataSource {

    override fun observeTodos(): Flow<List<Todo>> {
        return dao.observeTodos().map { entities -> entities.map { it.toTodo() } }
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

    override suspend fun deleteTodo(id: String) {
        dao.deleteTodo(id)
    }
}
