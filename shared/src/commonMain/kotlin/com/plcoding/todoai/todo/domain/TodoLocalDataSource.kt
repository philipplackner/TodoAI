package com.plcoding.todoai.todo.domain

import kotlinx.coroutines.flow.Flow

interface TodoLocalDataSource {
    fun observeTodos(): Flow<List<Todo>>
    suspend fun getTodo(id: String): Todo?
    suspend fun upsertTodo(todo: Todo)
    suspend fun setDone(id: String, isDone: Boolean)
    suspend fun deleteTodo(id: String)
}
