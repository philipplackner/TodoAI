package com.plcoding.todoai.todo.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface TodoLocalDataSource {
    fun observeTodos(): Flow<List<Todo>>
    suspend fun getTodos(): List<Todo>
    suspend fun getTodosDueBetween(from: LocalDate, to: LocalDate): List<Todo>
    suspend fun getTodosDueBefore(date: LocalDate): List<Todo>
    suspend fun getTodosWithoutDueDate(): List<Todo>
    suspend fun findTodosByTitle(query: String): List<Todo>
    suspend fun searchTodos(text: String): List<Todo>
    suspend fun getTodo(id: String): Todo?
    suspend fun upsertTodo(todo: Todo)
    suspend fun setDone(id: String, isDone: Boolean)
    suspend fun setDueDate(id: String, dueDate: LocalDate?)
    suspend fun deleteTodo(id: String)
}
