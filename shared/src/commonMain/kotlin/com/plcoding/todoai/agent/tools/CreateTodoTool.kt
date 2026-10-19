package com.plcoding.todoai.agent.tools

import com.plcoding.todoai.agent.domain.AgentTool
import com.plcoding.todoai.agent.domain.ToolArguments
import com.plcoding.todoai.agent.domain.ToolException
import com.plcoding.todoai.agent.domain.ToolParameter
import com.plcoding.todoai.todo.domain.Todo
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.uuid.Uuid

class CreateTodoTool(
    private val todoDataSource: TodoLocalDataSource,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) : AgentTool {

    override val name = "create_todo"

    override val description = "Creates a new to-do."

    override val parameters = listOf(
        ToolParameter(
            name = "title",
            type = ToolParameter.Type.STRING,
            description = "Short title of the to-do."
        ),
        ToolParameter(
            name = "description",
            type = ToolParameter.Type.STRING,
            description = "Optional extra details.",
            isRequired = false
        ),
        ToolParameter(
            name = "due",
            type = ToolParameter.Type.STRING,
            description = "When it's due, copied from the user's words without converting it, " +
                "for example ${DateExpressions.DATE_EXAMPLES}. Leave it out if the user gave no date.",
            isRequired = false
        )
    )

    override suspend fun execute(arguments: ToolArguments): Map<String, Any?> {
        val title = arguments.string("title") ?: throw ToolException("The title is missing.")
        val dueDate = arguments.string("due")?.let {
            DateExpressions.parseDate(it, clock.todayIn(timeZone())) ?: throw ToolException(unknownDateMessage(it))
        }

        val now = clock.now()
        val todo = Todo(
            id = Uuid.random().toString(),
            title = title,
            description = arguments.string("description").orEmpty(),
            isDone = false,
            dueDate = dueDate,
            createdAt = now,
            updatedAt = now
        )
        todoDataSource.upsertTodo(todo)

        return mapOf("created" to todo.toToolMap())
    }
}
