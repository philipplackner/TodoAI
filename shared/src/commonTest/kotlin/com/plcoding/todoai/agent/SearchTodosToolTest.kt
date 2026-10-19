package com.plcoding.todoai.agent

import com.plcoding.todoai.agent.domain.ToolArguments
import com.plcoding.todoai.agent.domain.ToolException
import com.plcoding.todoai.agent.tools.SearchTodosTool
import com.plcoding.todoai.agent.tools.searchKeywords
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SearchTodosToolTest {

    private val dataSource = FakeTodoLocalDataSource(
        listOf(
            todo("1", "Record AI agent video"),
            todo("2", "Buy groceries").copy(description = "Milk and eggs"),
            todo("3", "Edit the video", isDone = true),
            todo("4", "Call dentist")
        )
    )
    private val tool = SearchTodosTool(dataSource)

    private suspend fun search(query: String, includeDone: Boolean? = null): List<String> {
        val arguments = ToolArguments(buildMap {
            put("query", query)
            if (includeDone != null) put("include_done", includeDone)
        })
        @Suppress("UNCHECKED_CAST")
        val todos = tool.execute(arguments).getValue("todos") as List<Map<String, Any?>>
        return todos.map { it.getValue("title") as String }
    }

    @Test
    fun findsTodoByKeywordInTitle() = runTest {
        assertEquals(listOf("Record AI agent video"), search("record"))
    }

    @Test
    fun wholeQuestionStillFindsTheTodo() = runTest {
        assertEquals(listOf("Record AI agent video"), search("Do I need to record something?"))
    }

    @Test
    fun wordEndingsAreIgnored() = runTest {
        assertEquals(listOf("Record AI agent video"), search("recording"))
    }

    @Test
    fun searchesDescriptions() = runTest {
        assertEquals(listOf("Buy groceries"), search("milk"))
    }

    @Test
    fun todosMatchingMoreKeywordsComeFirst() = runTest {
        assertEquals(listOf("Record AI agent video", "Edit the video"), search("record video"))
    }

    @Test
    fun doneTodosCanBeExcluded() = runTest {
        assertEquals(listOf("Record AI agent video"), search("video", includeDone = false))
    }

    @Test
    fun noMatchReturnsAnEmptyList() = runTest {
        assertEquals(emptyList(), search("taxes"))
    }

    @Test
    fun missingQueryFails() = runTest {
        assertFailsWith<ToolException> { tool.execute(ToolArguments(emptyMap())) }
    }

    @Test
    fun keywordsDropCommonWordsAndEndings() {
        assertEquals(listOf("record", "video"), searchKeywords("Do I still need to be recording videos?"))
        assertEquals(listOf("dentist"), searchKeywords("the DENTIST"))
        assertEquals(emptyList(), searchKeywords("do I need to do something?"))
    }
}
