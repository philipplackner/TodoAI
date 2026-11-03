package com.plcoding.todoai.agent.data

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
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
 * Small models add arguments that aren't in the schema, or send `"true"` as a string, so the agent
 * decodes tool arguments leniently.
 */
val toolArgumentsJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}

/**
 * Describes a Koog tool as a function declaration whose `parameters` are a JSON schema of type
 * `object`, the format LiteRT-LM expects.
 */
fun ToolDescriptor.toFunctionDeclaration(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description)
    putJsonObject("parameters") {
        putObjectSchema(requiredParameters + optionalParameters, requiredParameters.map { it.name })
    }
}

private fun JsonObjectBuilder.putObjectSchema(properties: List<ToolParameterDescriptor>, required: List<String>) {
    put("type", "object")
    putJsonObject("properties") {
        properties.forEach { put(it.name, it.toSchema()) }
    }
    if (required.isNotEmpty()) {
        putJsonArray("required") { required.forEach { add(it) } }
    }
}

private fun ToolParameterDescriptor.toSchema(): JsonObject = buildJsonObject {
    putType(type)
    if (description.isNotBlank()) {
        put("description", description)
    }
}

private fun JsonObjectBuilder.putType(type: ToolParameterType) {
    when (type) {
        ToolParameterType.String -> put("type", "string")
        ToolParameterType.Integer -> put("type", "integer")
        ToolParameterType.Float -> put("type", "number")
        ToolParameterType.Boolean -> put("type", "boolean")
        ToolParameterType.Null -> put("type", "null")
        is ToolParameterType.Enum -> {
            put("type", "string")
            putJsonArray("enum") { type.entries.forEach { add(it) } }
        }
        is ToolParameterType.List -> {
            put("type", "array")
            putJsonObject("items") { putType(type.itemsType) }
        }
        // Optional arguments arrive as "string or null". Small models and the Gemma chat template
        // handle a plain type better, and leaving the argument out already means null.
        is ToolParameterType.AnyOf -> putType(
            type.types.map { it.type }.firstOrNull { it != ToolParameterType.Null } ?: ToolParameterType.Null
        )
        is ToolParameterType.Object -> putObjectSchema(type.properties, type.requiredProperties)
    }
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
