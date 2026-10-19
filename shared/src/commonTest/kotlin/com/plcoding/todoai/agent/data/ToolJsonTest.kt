package com.plcoding.todoai.agent.data

import com.plcoding.todoai.agent.FakeTodoLocalDataSource
import com.plcoding.todoai.agent.domain.ToolArguments
import com.plcoding.todoai.agent.tools.CreateTodoTool
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ToolJsonTest {

    @Test
    fun functionDeclarationHasSchemaWithRequiredParameters() {
        val declaration = CreateTodoTool(FakeTodoLocalDataSource()).toFunctionDeclaration()

        assertEquals("create_todo", declaration.getValue("name").jsonPrimitive.content)
        val parameters = declaration.getValue("parameters").jsonObject
        val properties = parameters.getValue("properties").jsonObject
        assertEquals(setOf("title", "description", "due"), properties.keys)
        assertEquals("string", properties.getValue("title").jsonObject.getValue("type").jsonPrimitive.content)
        assertEquals(listOf("title"), parameters.getValue("required").jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun jsonArgumentsBecomePlainValues() {
        val json = Json.parseToJsonElement("""{"title":"Call mom","done":false,"count":3,"due":null}""").jsonObject

        val arguments = ToolArguments(json.toPlainMap())

        assertEquals("Call mom", arguments.string("title"))
        assertEquals(false, arguments.boolean("done"))
        assertEquals(3L, arguments.values["count"])
        assertNull(arguments.string("due"))
    }

    @Test
    fun resultMapBecomesJson() {
        val result = mapOf(
            "count" to 1,
            "todos" to listOf(mapOf("title" to "Call mom", "due_date" to null, "done" to true))
        )

        assertEquals(
            """{"count":1,"todos":[{"title":"Call mom","due_date":null,"done":true}]}""",
            result.toJsonObject().toString()
        )
    }
}
