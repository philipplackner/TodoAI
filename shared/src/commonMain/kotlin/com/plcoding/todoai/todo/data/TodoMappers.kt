package com.plcoding.todoai.todo.data

import com.plcoding.todoai.todo.domain.Todo
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

fun TodoEntity.toTodo(): Todo = Todo(
    id = id,
    title = title,
    description = description,
    isDone = isDone,
    dueDate = dueDateEpochDays?.let { LocalDate.fromEpochDays(it) },
    createdAt = Instant.fromEpochMilliseconds(createdAtMillis),
    updatedAt = Instant.fromEpochMilliseconds(updatedAtMillis)
)

fun Todo.toTodoEntity(): TodoEntity = TodoEntity(
    id = id,
    title = title,
    description = description,
    isDone = isDone,
    dueDateEpochDays = dueDate?.toEpochDays(),
    createdAtMillis = createdAt.toEpochMilliseconds(),
    updatedAtMillis = updatedAt.toEpochMilliseconds()
)
