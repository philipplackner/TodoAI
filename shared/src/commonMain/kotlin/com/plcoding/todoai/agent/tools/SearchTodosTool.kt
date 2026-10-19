package com.plcoding.todoai.agent.tools

import com.plcoding.todoai.agent.domain.AgentTool
import com.plcoding.todoai.agent.domain.ToolArguments
import com.plcoding.todoai.agent.domain.ToolException
import com.plcoding.todoai.agent.domain.ToolParameter
import com.plcoding.todoai.todo.domain.Todo
import com.plcoding.todoai.todo.domain.TodoLocalDataSource

class SearchTodosTool(
    private val todoDataSource: TodoLocalDataSource
) : AgentTool {

    override val name = "search_todos"

    override val description = "Finds to-dos about a topic by searching their titles and descriptions. " +
        "Use it whenever the user asks about a specific thing, for example " +
        "\"Do I need to record something?\" -> query \"record\", \"Did I buy milk?\" -> query \"milk\"."

    override val parameters = listOf(
        ToolParameter(
            name = "query",
            type = ToolParameter.Type.STRING,
            description = "One or a few keywords to search for, like \"record\" or \"dentist\"."
        ),
        ToolParameter(
            name = "include_done",
            type = ToolParameter.Type.BOOLEAN,
            description = "Whether to include to-dos that are already done. Defaults to true.",
            isRequired = false
        )
    )

    override suspend fun execute(arguments: ToolArguments): Map<String, Any?> {
        val query = arguments.string("query") ?: throw ToolException("The query is missing.")
        val includeDone = arguments.boolean("include_done") ?: true

        val todos = search(query).filter { includeDone || !it.isDone }

        return mapOf(
            "query" to query,
            "count" to todos.size,
            "todos" to todos.map { it.toToolMap() }
        )
    }

    /**
     * Runs one database search per keyword and ranks to-dos by how many keywords they match.
     * Searching word by word means "record video" still finds "Record AI agent video".
     */
    private suspend fun search(query: String): List<Todo> {
        val keywords = searchKeywords(query).ifEmpty { listOf(query.trim()) }
        val hitsById = mutableMapOf<String, Int>()
        val todosById = mutableMapOf<String, Todo>()
        keywords.forEach { keyword ->
            todoDataSource.searchTodos(keyword).forEach { todo ->
                todosById[todo.id] = todo
                hitsById[todo.id] = (hitsById[todo.id] ?: 0) + 1
            }
        }
        return todosById.values.sortedWith(
            compareByDescending<Todo> { hitsById.getValue(it.id) }.thenBy { it.isDone }
        )
    }
}

/**
 * Splits a search query into keywords. Common words are dropped and simple endings are cut off,
 * so "recording videos" searches for "record" and "video".
 */
fun searchKeywords(query: String): List<String> {
    return query.lowercase()
        .split(NON_WORD)
        .filter { it.length > 1 && it !in STOP_WORDS }
        .map { it.stem() }
        .distinct()
}

private fun String.stem(): String {
    val suffix = STEM_SUFFIXES.firstOrNull { endsWith(it) && length - it.length >= 3 } ?: return this
    return dropLast(suffix.length)
}

private val NON_WORD = Regex("""[^\p{L}\p{N}]+""")
private val STEM_SUFFIXES = listOf("ing", "ed", "es", "s")
private val STOP_WORDS = setOf(
    "a", "an", "the", "and", "or", "to", "do", "does", "did", "i", "me", "my", "we", "our", "you",
    "your", "is", "are", "was", "be", "need", "needs", "have", "has", "any", "anything", "something",
    "thing", "things", "todo", "todos", "to-do", "task", "tasks", "about", "for", "with", "of", "on",
    "in", "at", "it", "that", "this", "there", "what", "which", "still", "should", "must", "got"
)
