package com.plcoding.todoai.agent.tools

import com.plcoding.todoai.todo.domain.Todo
import com.plcoding.todoai.todo.domain.TodoLocalDataSource

sealed interface TodoMatch {
    data class Found(val todo: Todo) : TodoMatch
    data object NotFound : TodoMatch
    data class Ambiguous(val candidates: List<Todo>) : TodoMatch
}

/**
 * Finds a to-do by (part of) its title. The model refers to to-dos by title because it copies
 * UUIDs unreliably.
 */
suspend fun TodoLocalDataSource.matchTodoByTitle(title: String): TodoMatch {
    val matches = findTodosByTitle(title.trim())
    val exactMatches = matches.filter { it.title.equals(title.trim(), ignoreCase = true) }
    return when {
        exactMatches.size == 1 -> TodoMatch.Found(exactMatches.single())
        matches.size == 1 -> TodoMatch.Found(matches.single())
        matches.isEmpty() -> TodoMatch.NotFound
        else -> TodoMatch.Ambiguous(matches)
    }
}

fun TodoMatch.errorMessage(title: String): String = when (this) {
    is TodoMatch.Found -> ""
    TodoMatch.NotFound -> "No to-do found with a title like '$title'. Call get_todos to see the existing titles."
    is TodoMatch.Ambiguous -> "Several to-dos match '$title': ${candidates.joinToString { "'${it.title}'" }}. Ask the user which one they mean."
}
