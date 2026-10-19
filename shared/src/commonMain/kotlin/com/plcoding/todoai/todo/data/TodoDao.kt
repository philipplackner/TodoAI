package com.plcoding.todoai.todo.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos ORDER BY isDone ASC, createdAtMillis DESC")
    fun observeTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodo(id: String): TodoEntity?

    @Upsert
    suspend fun upsertTodo(todo: TodoEntity)

    @Query("UPDATE todos SET isDone = :isDone, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun setDone(id: String, isDone: Boolean, updatedAtMillis: Long)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodo(id: String)
}
