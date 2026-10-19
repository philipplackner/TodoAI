package com.plcoding.todoai.agent.tools

import com.plcoding.todoai.agent.domain.toSpokenDate
import com.plcoding.todoai.todo.domain.Todo

fun Todo.toToolMap(): Map<String, Any?> = buildMap {
    put("title", title)
    if (description.isNotBlank()) {
        put("description", description)
    }
    put("due_date", dueDate?.toSpokenDate())
    put("done", isDone)
}

fun unknownDateMessage(input: String): String {
    return "Couldn't understand the date '$input'. Pass the user's wording, for example ${DateExpressions.DATE_EXAMPLES}."
}
