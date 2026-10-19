package com.plcoding.todoai.agent.tools

import com.plcoding.todoai.agent.domain.AgentTool
import com.plcoding.todoai.agent.domain.ToolArguments
import com.plcoding.todoai.agent.domain.ToolException
import com.plcoding.todoai.agent.domain.ToolParameter
import com.plcoding.todoai.todo.domain.TodoLocalDataSource

class SetTodoDoneTool(
    private val todoDataSource: TodoLocalDataSource
) : AgentTool {

    override val name = "set_todo_done"

    override val description = "Marks a to-do as done or as not done."

    override val parameters = listOf(
        ToolParameter(
            name = "title",
            type = ToolParameter.Type.STRING,
            description = "Title of the to-do, or a unique part of it."
        ),
        ToolParameter(
            name = "done",
            type = ToolParameter.Type.BOOLEAN,
            description = "True to mark it as done, false to mark it as not done. Defaults to true.",
            isRequired = false
        )
    )

    override suspend fun execute(arguments: ToolArguments): Map<String, Any?> {
        val title = arguments.string("title") ?: throw ToolException("The title is missing.")
        val isDone = arguments.boolean("done") ?: true

        val match = todoDataSource.matchTodoByTitle(title)
        if (match !is TodoMatch.Found) {
            throw ToolException(match.errorMessage(title))
        }
        todoDataSource.setDone(id = match.todo.id, isDone = isDone)

        return mapOf("updated" to match.todo.copy(isDone = isDone).toToolMap())
    }
}
