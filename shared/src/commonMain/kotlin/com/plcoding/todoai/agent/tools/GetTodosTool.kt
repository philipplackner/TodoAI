package com.plcoding.todoai.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.fail
import ai.koog.serialization.typeToken
import com.plcoding.todoai.agent.data.toJsonObject
import com.plcoding.todoai.agent.domain.toSpokenDate
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock

class GetTodosTool(
    private val todoDataSource: TodoLocalDataSource,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) : Tool<GetTodosTool.Args, JsonObject>(
    argsType = typeToken<Args>(),
    resultType = typeToken<JsonObject>(),
    name = "get_todos",
    description = "Looks up the user's to-dos, filtered by when they are due."
) {

    @Serializable
    data class Args(
        @property:LLMDescription(
            "When the to-dos are due, copied from the user's words without converting it. " +
                "Examples: ${DateExpressions.PERIOD_EXAMPLES}."
        )
        val period: String,
        @SerialName("include_done")
        @property:LLMDescription("Whether to include to-dos that are already done. Defaults to false.")
        val includeDone: Boolean? = null
    )

    override suspend fun execute(args: Args): JsonObject {
        val periodText = args.period.argOrNull() ?: "all"
        val today = clock.todayIn(timeZone())
        val filter = DateExpressions.parsePeriod(periodText, today)
            ?: fail(
                "Couldn't understand the period '$periodText'. Pass the user's wording, for example ${DateExpressions.PERIOD_EXAMPLES}."
            )
        val includeDone = args.includeDone ?: false

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
        }.toJsonObject()
    }
}
