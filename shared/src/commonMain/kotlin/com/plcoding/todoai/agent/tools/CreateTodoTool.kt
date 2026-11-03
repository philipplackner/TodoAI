package com.plcoding.todoai.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.fail
import ai.koog.serialization.typeToken
import com.plcoding.todoai.agent.data.toJsonObject
import com.plcoding.todoai.todo.domain.Todo
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock
import kotlin.uuid.Uuid

class CreateTodoTool(
    private val todoDataSource: TodoLocalDataSource,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) : Tool<CreateTodoTool.Args, JsonObject>(
    argsType = typeToken<Args>(),
    resultType = typeToken<JsonObject>(),
    name = "create_todo",
    description = "Creates a new to-do."
) {

    @Serializable
    data class Args(
        @property:LLMDescription("Short title of the to-do.")
        val title: String,
        @property:LLMDescription("Optional extra details.")
        val description: String = "",
        @property:LLMDescription(
            "When it's due, copied from the user's words without converting it, " +
                "for example ${DateExpressions.DATE_EXAMPLES}. Leave it out if the user gave no date."
        )
        val due: String = ""
    )

    override suspend fun execute(args: Args): JsonObject {
        val title = args.title.argOrNull() ?: fail("The title is missing.")
        val dueDate = args.due.argOrNull()?.let {
            DateExpressions.parseDate(it, clock.todayIn(timeZone())) ?: fail(unknownDateMessage(it))
        }

        val now = clock.now()
        val todo = Todo(
            id = Uuid.random().toString(),
            title = title,
            description = args.description.argOrNull().orEmpty(),
            isDone = false,
            dueDate = dueDate,
            createdAt = now,
            updatedAt = now
        )
        todoDataSource.upsertTodo(todo)

        return mapOf("created" to todo.toToolMap()).toJsonObject()
    }
}
