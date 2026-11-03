package com.plcoding.todoai.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.fail
import ai.koog.serialization.typeToken
import com.plcoding.todoai.agent.data.toJsonObject
import com.plcoding.todoai.todo.domain.Todo
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

class SearchTodosTool(
    private val todoDataSource: TodoLocalDataSource
) : Tool<SearchTodosTool.Args, JsonObject>(
    argsType = typeToken<Args>(),
    resultType = typeToken<JsonObject>(),
    name = "search_todos",
    description = "Finds to-dos about a topic by searching their titles and descriptions. " +
        "Use it whenever the user asks about a specific thing, for example " +
        "\"Do I need to record something?\" -> query \"record\", \"Did I buy milk?\" -> query \"milk\"."
) {

    @Serializable
    data class Args(
        @property:LLMDescription("One or a few keywords to search for, like \"record\" or \"dentist\".")
        val query: String,
        @SerialName("include_done")
        @property:LLMDescription("Whether to include to-dos that are already done. Defaults to true.")
        val includeDone: Boolean? = null
    )

    override suspend fun execute(args: Args): JsonObject {
        val query = args.query.argOrNull() ?: fail("The query is missing.")
        val includeDone = args.includeDone ?: true

        val todos = search(query).filter { includeDone || !it.isDone }

        return mapOf(
            "query" to query,
            "count" to todos.size,
            "todos" to todos.map { it.toToolMap() }
        ).toJsonObject()
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
