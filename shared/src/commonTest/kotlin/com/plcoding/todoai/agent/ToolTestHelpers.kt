package com.plcoding.todoai.agent

import ai.koog.agents.core.tools.Tool
import ai.koog.serialization.kotlinx.KotlinxSerializer
import ai.koog.serialization.kotlinx.toKoogJSONObject
import com.plcoding.todoai.agent.data.toJsonObject
import com.plcoding.todoai.agent.data.toPlainMap
import com.plcoding.todoai.agent.data.toolArgumentsJson
import kotlinx.serialization.json.JsonObject

/**
 * Calls the tool with arguments as the model sent them, decoded the same way the agent does.
 */
suspend fun <A> Tool<A, JsonObject>.call(vararg arguments: Pair<String, Any?>): Map<String, Any?> {
    val args = decodeArgs(arguments.toMap().toJsonObject().toKoogJSONObject(), KotlinxSerializer(toolArgumentsJson))
    return execute(args).toPlainMap()
}
