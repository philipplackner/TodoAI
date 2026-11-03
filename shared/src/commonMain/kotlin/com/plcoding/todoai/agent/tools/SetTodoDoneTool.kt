package com.plcoding.todoai.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.fail
import ai.koog.serialization.typeToken
import com.plcoding.todoai.agent.data.toJsonObject
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

class SetTodoDoneTool(
    private val todoDataSource: TodoLocalDataSource
) : Tool<SetTodoDoneTool.Args, JsonObject>(
    argsType = typeToken<Args>(),
    resultType = typeToken<JsonObject>(),
    name = "set_todo_done",
    description = "Marks a to-do as done or as not done."
) {

    @Serializable
    data class Args(
        @property:LLMDescription("Title of the to-do, or a unique part of it.")
        val title: String,
        @property:LLMDescription("True to mark it as done, false to mark it as not done. Defaults to true.")
        val done: Boolean? = null
    )

    override suspend fun execute(args: Args): JsonObject {
        val title = args.title.argOrNull() ?: fail("The title is missing.")
        val isDone = args.done ?: true

        val match = todoDataSource.matchTodoByTitle(title)
        if (match !is TodoMatch.Found) {
            fail(match.errorMessage(title))
        }
        todoDataSource.setDone(id = match.todo.id, isDone = isDone)

        return mapOf("updated" to match.todo.copy(isDone = isDone).toToolMap()).toJsonObject()
    }
}
