package com.plcoding.todoai.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.fail
import ai.koog.serialization.typeToken
import com.plcoding.todoai.agent.data.toJsonObject
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock

class SetTodoDueDateTool(
    private val todoDataSource: TodoLocalDataSource,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) : Tool<SetTodoDueDateTool.Args, JsonObject>(
    argsType = typeToken<Args>(),
    resultType = typeToken<JsonObject>(),
    name = "set_todo_due_date",
    description = "Sets or removes the due date of a to-do."
) {

    @Serializable
    data class Args(
        @property:LLMDescription("Title of the to-do, or a unique part of it.")
        val title: String,
        @property:LLMDescription(
            "The new due date, copied from the user's words without converting it, " +
                "for example ${DateExpressions.DATE_EXAMPLES}. Leave it out to remove the due date."
        )
        val due: String? = null
    )

    override suspend fun execute(args: Args): JsonObject {
        val title = args.title.argOrNull() ?: fail("The title is missing.")
        val dueDate = args.due.argOrNull()?.let {
            DateExpressions.parseDate(it, clock.todayIn(timeZone())) ?: fail(unknownDateMessage(it))
        }

        val match = todoDataSource.matchTodoByTitle(title)
        if (match !is TodoMatch.Found) {
            fail(match.errorMessage(title))
        }
        todoDataSource.setDueDate(id = match.todo.id, dueDate = dueDate)

        return mapOf("updated" to match.todo.copy(dueDate = dueDate).toToolMap()).toJsonObject()
    }
}
