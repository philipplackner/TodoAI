package com.plcoding.todoai.todo.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos ORDER BY isDone ASC, createdAtMillis DESC")
    fun observeTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos ORDER BY isDone ASC, dueDateEpochDays IS NULL, dueDateEpochDays ASC")
    suspend fun getTodos(): List<TodoEntity>

    @Query(
        """
        SELECT * FROM todos
        WHERE dueDateEpochDays BETWEEN :fromEpochDays AND :toEpochDays
        ORDER BY dueDateEpochDays ASC
        """
    )
    suspend fun getTodosDueBetween(fromEpochDays: Long, toEpochDays: Long): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE dueDateEpochDays < :epochDays ORDER BY dueDateEpochDays ASC")
    suspend fun getTodosDueBefore(epochDays: Long): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE dueDateEpochDays IS NULL ORDER BY createdAtMillis DESC")
    suspend fun getTodosWithoutDueDate(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE title LIKE '%' || :query || '%' ORDER BY isDone ASC")
    suspend fun findTodosByTitle(query: String): List<TodoEntity>

    @Query(
        """
        SELECT * FROM todos
        WHERE title LIKE '%' || :text || '%' OR description LIKE '%' || :text || '%'
        ORDER BY isDone ASC, createdAtMillis DESC
        """
    )
    suspend fun searchTodos(text: String): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodo(id: String): TodoEntity?

    @Upsert
    suspend fun upsertTodo(todo: TodoEntity)

    @Query("UPDATE todos SET isDone = :isDone, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun setDone(id: String, isDone: Boolean, updatedAtMillis: Long)

    @Query("UPDATE todos SET dueDateEpochDays = :dueDateEpochDays, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun setDueDate(id: String, dueDateEpochDays: Long?, updatedAtMillis: Long)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodo(id: String)
}
