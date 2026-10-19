package com.plcoding.todoai.agent

import com.plcoding.todoai.agent.domain.ToolArguments
import com.plcoding.todoai.agent.domain.ToolException
import com.plcoding.todoai.agent.tools.CreateTodoTool
import com.plcoding.todoai.agent.tools.GetTodosTool
import com.plcoding.todoai.agent.tools.SetTodoDoneTool
import com.plcoding.todoai.agent.tools.SetTodoDueDateTool
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class TodoToolsTest {

    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-10-21T12:00:00Z")
    }

    private fun args(vararg pairs: Pair<String, Any>) = ToolArguments(pairs.toMap())

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.titles(): List<String> {
        return (getValue("todos") as List<Map<String, Any?>>).map { it.getValue("title") as String }
    }

    @Test
    fun getTodosNextWeekReturnsOnlyOpenTodosInThatWeek() = runTest {
        val dataSource = FakeTodoLocalDataSource(
            listOf(
                todo("1", "This week", dueDate = LocalDate(2026, 10, 25)),
                todo("2", "Next Monday", dueDate = LocalDate(2026, 10, 26)),
                todo("3", "Next Sunday", dueDate = LocalDate(2026, 11, 1)),
                todo("4", "Done next week", dueDate = LocalDate(2026, 10, 28), isDone = true),
                todo("5", "No date")
            )
        )
        val tool = GetTodosTool(dataSource, clock, timeZone = { TimeZone.UTC })

        val result = tool.execute(args("period" to "next week"))

        assertEquals(listOf("Next Monday", "Next Sunday"), result.titles())
        assertEquals("Monday, October 26, 2026", result["from"])
        assertEquals("Sunday, November 1, 2026", result["to"])
    }

    @Test
    fun getTodosOverdueIgnoresDoneAndToday() = runTest {
        val dataSource = FakeTodoLocalDataSource(
            listOf(
                todo("1", "Late", dueDate = LocalDate(2026, 10, 20)),
                todo("2", "Late but done", dueDate = LocalDate(2026, 10, 1), isDone = true),
                todo("3", "Today", dueDate = LocalDate(2026, 10, 21))
            )
        )
        val tool = GetTodosTool(dataSource, clock, timeZone = { TimeZone.UTC })

        val result = tool.execute(args("period" to "overdue", "include_done" to true))

        assertEquals(listOf("Late"), result.titles())
        assertNull(result["from"])
    }

    @Test
    fun getTodosRejectsUnknownPeriod() = runTest {
        val tool = GetTodosTool(FakeTodoLocalDataSource(), clock, timeZone = { TimeZone.UTC })

        assertFailsWith<ToolException> { tool.execute(args("period" to "whenever")) }
    }

    @Test
    fun createTodoResolvesTheUsersWording() = runTest {
        val dataSource = FakeTodoLocalDataSource()

        val result = CreateTodoTool(dataSource, clock, timeZone = { TimeZone.UTC })
            .execute(args("title" to "Call mom", "due" to "friday"))

        assertEquals(LocalDate(2026, 10, 23), dataSource.todos.value.single().dueDate)
        val created = result.getValue("created") as Map<*, *>
        assertEquals("Friday, October 23, 2026", created["due_date"])
    }

    @Test
    fun createTodoTreatsNullStringAsMissingDueDate() = runTest {
        val dataSource = FakeTodoLocalDataSource()

        CreateTodoTool(dataSource, clock).execute(args("title" to "Buy milk", "due" to "null"))

        assertNull(dataSource.todos.value.single().dueDate)
    }

    @Test
    fun createTodoWithoutTitleFails() = runTest {
        assertFailsWith<ToolException> { CreateTodoTool(FakeTodoLocalDataSource(), clock).execute(args()) }
    }

    @Test
    fun createTodoRejectsUnknownDateWithoutCreatingAnything() = runTest {
        val dataSource = FakeTodoLocalDataSource()

        val error = assertFailsWith<ToolException> {
            CreateTodoTool(dataSource, clock).execute(args("title" to "Buy milk", "due" to "sometime soon"))
        }

        val message = error.message.orEmpty()
        assertTrue("sometime soon" in message && "in 3 weeks" in message, message)
        assertTrue(dataSource.todos.value.isEmpty())
    }

    @Test
    fun setDoneUsesExactTitleMatchOverPartialMatches() = runTest {
        val dataSource = FakeTodoLocalDataSource(
            listOf(todo("1", "Buy milk"), todo("2", "Buy milk and eggs"))
        )

        SetTodoDoneTool(dataSource).execute(args("title" to "buy milk"))

        assertEquals(listOf(true, false), dataSource.todos.value.sortedBy { it.id }.map { it.isDone })
    }

    @Test
    fun setDoneWithAmbiguousTitleChangesNothingAndListsCandidates() = runTest {
        val dataSource = FakeTodoLocalDataSource(
            listOf(todo("1", "Call mom"), todo("2", "Call dentist"))
        )

        val error = assertFailsWith<ToolException> {
            SetTodoDoneTool(dataSource).execute(args("title" to "call"))
        }

        val message = error.message.orEmpty()
        assertTrue("Call mom" in message && "Call dentist" in message, message)
        assertTrue(dataSource.todos.value.none { it.isDone })
    }

    @Test
    fun setDoneAcceptsStringBoolean() = runTest {
        val dataSource = FakeTodoLocalDataSource(listOf(todo("1", "Call mom", isDone = true)))

        SetTodoDoneTool(dataSource).execute(args("title" to "Call mom", "done" to "false"))

        assertEquals(false, dataSource.todos.value.single().isDone)
    }

    @Test
    fun setDueDateWithoutDateClearsIt() = runTest {
        val dataSource = FakeTodoLocalDataSource(
            listOf(todo("1", "Call mom", dueDate = LocalDate(2026, 10, 30)))
        )

        SetTodoDueDateTool(dataSource).execute(args("title" to "Call mom", "due" to ""))

        assertNull(dataSource.todos.value.single().dueDate)
    }

    @Test
    fun setDueDateResolvesRelativeWeeks() = runTest {
        val dataSource = FakeTodoLocalDataSource(listOf(todo("1", "Call mom")))

        SetTodoDueDateTool(dataSource, clock, timeZone = { TimeZone.UTC })
            .execute(args("title" to "Call mom", "due" to "in 3 weeks"))

        assertEquals(LocalDate(2026, 11, 11), dataSource.todos.value.single().dueDate)
    }
}
