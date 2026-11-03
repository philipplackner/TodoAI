package com.plcoding.todoai.agent.data

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.executor.clients.openai.base.OpenAICompatibleToolDescriptorSchemaGenerator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Small models add arguments that aren't in the schema, send `"true"` as a string or `null` for an
 * optional argument, so the agent decodes tool arguments leniently.
 */
val toolArgumentsJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

private val schemaGenerator = OpenAICompatibleToolDescriptorSchemaGenerator()

/**
 * Describes a Koog tool as a function declaration, the format LiteRT-LM expects. Optional tool
 * arguments have defaults instead of being nullable, so the schema has plain types rather than
 * "string or null", which small models handle better.
 */
fun ToolDescriptor.toFunctionDeclaration(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description)
    put("parameters", schemaGenerator.generate(this@toFunctionDeclaration))
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
