package com.plcoding.todoai.agent.data

import com.plcoding.todoai.agent.domain.AgentTool
import com.plcoding.todoai.agent.domain.ToolParameter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Describes the tool as a function declaration whose `parameters` are a JSON schema of type
 * `object`, the format LiteRT-LM expects.
 */
fun AgentTool.toFunctionDeclaration(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description)
    putJsonObject("parameters") {
        put("type", "object")
        putJsonObject("properties") {
            parameters.forEach { parameter ->
                putJsonObject(parameter.name) {
                    put("type", parameter.type.toSchemaType())
                    put("description", parameter.description)
                }
            }
        }
        val required = parameters.filter { it.isRequired }
        if (required.isNotEmpty()) {
            putJsonArray("required") { required.forEach { add(it.name) } }
        }
    }
}

private fun ToolParameter.Type.toSchemaType(): String = when (this) {
    ToolParameter.Type.STRING -> "string"
    ToolParameter.Type.BOOLEAN -> "boolean"
}

fun JsonObject.toPlainMap(): Map<String, Any?> = mapValues { it.value.toPlainValue() }

private fun JsonElement.toPlainValue(): Any? = when (this) {
    JsonNull -> null
    is JsonPrimitive -> when {
        isString -> content
        else -> booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
    }
    is JsonObject -> toPlainMap()
    is JsonArray -> map { it.toPlainValue() }
}

fun Map<String, Any?>.toJsonObject(): JsonObject = JsonObject(mapValues { it.value.toJsonElement() })

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Map<*, *> -> JsonObject(entries.associate { (key, value) -> key.toString() to value.toJsonElement() })
    is Iterable<*> -> JsonArray(map { it.toJsonElement() })
    is Array<*> -> JsonArray(map { it.toJsonElement() })
    else -> JsonPrimitive(toString())
}
