package com.plcoding.todoai.agent.tools

import com.plcoding.todoai.agent.domain.AgentTool
import com.plcoding.todoai.agent.domain.ToolArguments
import com.plcoding.todoai.agent.domain.ToolException
import com.plcoding.todoai.agent.domain.ToolParameter
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class SetTodoDueDateTool(
    private val todoDataSource: TodoLocalDataSource,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) : AgentTool {

    override val name = "set_todo_due_date"

    override val description = "Sets or removes the due date of a to-do."

    override val parameters = listOf(
        ToolParameter(
            name = "title",
            type = ToolParameter.Type.STRING,
            description = "Title of the to-do, or a unique part of it."
        ),
        ToolParameter(
            name = "due",
            type = ToolParameter.Type.STRING,
            description = "The new due date, copied from the user's words without converting it, " +
                "for example ${DateExpressions.DATE_EXAMPLES}. Leave it out to remove the due date.",
            isRequired = false
        )
    )

    override suspend fun execute(arguments: ToolArguments): Map<String, Any?> {
        val title = arguments.string("title") ?: throw ToolException("The title is missing.")
        val dueDate = arguments.string("due")?.let {
            DateExpressions.parseDate(it, clock.todayIn(timeZone())) ?: throw ToolException(unknownDateMessage(it))
        }

        val match = todoDataSource.matchTodoByTitle(title)
        if (match !is TodoMatch.Found) {
            throw ToolException(match.errorMessage(title))
        }
        todoDataSource.setDueDate(id = match.todo.id, dueDate = dueDate)

        return mapOf("updated" to match.todo.copy(dueDate = dueDate).toToolMap())
    }
}
