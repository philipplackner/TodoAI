package com.plcoding.todoai.agent.tools

import com.plcoding.todoai.agent.domain.AgentTool
import com.plcoding.todoai.agent.domain.ToolArguments
import com.plcoding.todoai.agent.domain.ToolException
import com.plcoding.todoai.agent.domain.ToolParameter
import com.plcoding.todoai.agent.domain.toSpokenDate
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class GetTodosTool(
    private val todoDataSource: TodoLocalDataSource,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) : AgentTool {

    override val name = "get_todos"

    override val description = "Looks up the user's to-dos, filtered by when they are due."

    override val parameters = listOf(
        ToolParameter(
            name = "period",
            type = ToolParameter.Type.STRING,
            description = "When the to-dos are due, copied from the user's words without converting it. " +
                "Examples: ${DateExpressions.PERIOD_EXAMPLES}."
        ),
        ToolParameter(
            name = "include_done",
            type = ToolParameter.Type.BOOLEAN,
            description = "Whether to include to-dos that are already done. Defaults to false.",
            isRequired = false
        )
    )

    override suspend fun execute(arguments: ToolArguments): Map<String, Any?> {
        val periodText = arguments.string("period") ?: "all"
        val today = clock.todayIn(timeZone())
        val filter = DateExpressions.parsePeriod(periodText, today)
            ?: throw ToolException(
                "Couldn't understand the period '$periodText'. Pass the user's wording, for example ${DateExpressions.PERIOD_EXAMPLES}."
            )
        val includeDone = arguments.boolean("include_done") ?: false

        val todos = when (filter) {
            DueFilter.Overdue -> todoDataSource.getTodosDueBefore(today).filter { !it.isDone }
            DueFilter.NoDueDate -> todoDataSource.getTodosWithoutDueDate()
            DueFilter.All -> todoDataSource.getTodos()
            is DueFilter.Between -> todoDataSource.getTodosDueBetween(filter.from, filter.to)
        }.filter { includeDone || !it.isDone }

        return buildMap {
            if (filter is DueFilter.Between) {
                put("from", filter.from.toSpokenDate())
                put("to", filter.to.toSpokenDate())
            }
            put("count", todos.size)
            put("todos", todos.map { it.toToolMap() })
        }
    }
}
