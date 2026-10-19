package com.plcoding.todoai.agent

import com.plcoding.todoai.agent.data.toFunctionDeclaration
import com.plcoding.todoai.agent.data.toJsonObject
import com.plcoding.todoai.agent.data.toPlainMap
import com.plcoding.todoai.agent.domain.AgentTool
import com.plcoding.todoai.agent.domain.LlmConversation
import com.plcoding.todoai.agent.domain.LlmEngine
import com.plcoding.todoai.agent.domain.LlmMessage
import com.plcoding.todoai.agent.domain.LlmResponse
import com.plcoding.todoai.agent.domain.ToolArguments
import com.plcoding.todoai.agent.domain.ToolCall
import com.plcoding.todoai.litertlm.litert_lm_conversation_config_create
import com.plcoding.todoai.litertlm.litert_lm_conversation_config_delete
import com.plcoding.todoai.litertlm.litert_lm_conversation_config_set_session_config
import com.plcoding.todoai.litertlm.litert_lm_conversation_config_set_system_message
import com.plcoding.todoai.litertlm.litert_lm_conversation_config_set_tools
import com.plcoding.todoai.litertlm.litert_lm_conversation_create
import com.plcoding.todoai.litertlm.litert_lm_conversation_delete
import com.plcoding.todoai.litertlm.litert_lm_conversation_send_message
import com.plcoding.todoai.litertlm.litert_lm_engine_create
import com.plcoding.todoai.litertlm.litert_lm_engine_settings_create
import com.plcoding.todoai.litertlm.litert_lm_engine_settings_delete
import com.plcoding.todoai.litertlm.litert_lm_engine_settings_set_cache_dir
import com.plcoding.todoai.litertlm.litert_lm_engine_settings_set_max_num_tokens
import com.plcoding.todoai.litertlm.litert_lm_get_last_error_message
import com.plcoding.todoai.litertlm.litert_lm_json_response_delete
import com.plcoding.todoai.litertlm.litert_lm_json_response_get_string
import com.plcoding.todoai.litertlm.litert_lm_session_config_create
import com.plcoding.todoai.litertlm.litert_lm_session_config_delete
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import cnames.structs.LiteRtLmConversation as CConversation
import cnames.structs.LiteRtLmEngine as CEngine

/**
 * Runs LiteRT-LM through its C API. The Swift wrapper only accepts tools declared as Swift
 * structs, while the C API takes them as JSON, so the shared Kotlin tools can be passed as they are.
 */
@OptIn(ExperimentalForeignApi::class)
class LiteRtLmEngine : LlmEngine {

    private val mutex = Mutex()
    private var engine: CPointer<CEngine>? = null

    override val isLoaded: Boolean get() = engine != null

    override suspend fun load(modelPath: String, cacheDir: String) = mutex.withLock {
        if (engine != null) return@withLock
        engine = withContext(Dispatchers.Default) {
            // The iOS simulator has no Metal support for LiteRT-LM, so CPU is the fallback.
            createEngine(modelPath, cacheDir, backend = "gpu")
                ?: createEngine(modelPath, cacheDir, backend = "cpu")
                ?: throw LiteRtLmException("Creating the engine failed: ${lastErrorMessage()}")
        }
    }

    override suspend fun createConversation(
        systemPrompt: String,
        tools: List<AgentTool>
    ): LlmConversation = withContext(Dispatchers.Default) {
        val engine = checkNotNull(engine) { "The model is not loaded" }
        val sessionConfig = litert_lm_session_config_create()
        val conversationConfig = litert_lm_conversation_config_create()
        try {
            litert_lm_conversation_config_set_session_config(conversationConfig, sessionConfig)
            litert_lm_conversation_config_set_system_message(
                conversationConfig,
                textContents(systemPrompt).toString()
            )
            litert_lm_conversation_config_set_tools(
                conversationConfig,
                buildJsonArray {
                    tools.forEach { tool ->
                        addJsonObject {
                            put("type", "function")
                            put("function", tool.toFunctionDeclaration())
                        }
                    }
                }.toString()
            )
            val conversation = litert_lm_conversation_create(engine, conversationConfig)
                ?: throw LiteRtLmException("Creating the conversation failed: ${lastErrorMessage()}")
            LiteRtLmConversation(conversation)
        } finally {
            litert_lm_conversation_config_delete(conversationConfig)
            litert_lm_session_config_delete(sessionConfig)
        }
    }

    private fun createEngine(modelPath: String, cacheDir: String, backend: String): CPointer<CEngine>? {
        val settings = litert_lm_engine_settings_create(modelPath, backend, null, null) ?: return null
        return try {
            litert_lm_engine_settings_set_cache_dir(settings, cacheDir)
            litert_lm_engine_settings_set_max_num_tokens(settings, MAX_NUM_TOKENS)
            litert_lm_engine_create(settings)
        } finally {
            litert_lm_engine_settings_delete(settings)
        }
    }

    private companion object {
        const val MAX_NUM_TOKENS = 4096
    }
}

@OptIn(ExperimentalForeignApi::class)
private class LiteRtLmConversation(
    private var conversation: CPointer<CConversation>?
) : LlmConversation {

    override suspend fun send(message: LlmMessage): LlmResponse = withContext(Dispatchers.Default) {
        val handle = checkNotNull(conversation) { "The conversation is closed" }
        val response = litert_lm_conversation_send_message(handle, message.toJson().toString(), null, null)
            ?: throw LiteRtLmException("Generating a response failed: ${lastErrorMessage()}")
        val responseJson = try {
            litert_lm_json_response_get_string(response)?.toKString()
                ?: throw LiteRtLmException("The response was empty: ${lastErrorMessage()}")
        } finally {
            litert_lm_json_response_delete(response)
        }
        Json.parseToJsonElement(responseJson).jsonObject.toLlmResponse()
    }

    override fun close() {
        conversation?.let { litert_lm_conversation_delete(it) }
        conversation = null
    }

    private fun LlmMessage.toJson(): JsonObject = when (this) {
        is LlmMessage.User -> buildJsonObject {
            put("role", "user")
            put("content", textContents(text))
        }
        is LlmMessage.ToolResults -> buildJsonObject {
            put("role", "tool")
            putJsonArray("content") {
                results.forEach { result ->
                    addJsonObject {
                        put("type", "tool_response")
                        put("name", result.toolName)
                        put("response", result.result.toJsonObject())
                    }
                }
            }
        }
    }

    private fun JsonObject.toLlmResponse(): LlmResponse {
        val text = when (val content = get("content")) {
            is JsonPrimitive -> content.contentOrNull.orEmpty()
            is JsonArray -> content
                .mapNotNull { it as? JsonObject }
                .filter { (it["type"] as? JsonPrimitive)?.contentOrNull == "text" }
                .joinToString(separator = "") { (it["text"] as? JsonPrimitive)?.contentOrNull.orEmpty() }
            else -> ""
        }
        val toolCalls = (get("tool_calls") as? JsonArray).orEmpty().mapNotNull { element ->
            val function = (element as? JsonObject)?.get("function") as? JsonObject ?: return@mapNotNull null
            val name = (function["name"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            ToolCall(name = name, arguments = ToolArguments(function["arguments"].toArguments().toPlainMap()))
        }
        return LlmResponse(text = text, toolCalls = toolCalls)
    }

    /**
     * Arguments usually arrive as an object, but some chat templates encode them as a JSON string.
     */
    private fun JsonElement?.toArguments(): JsonObject = when (this) {
        is JsonObject -> this
        is JsonPrimitive -> contentOrNull
            ?.let { runCatching { Json.parseToJsonElement(it) as? JsonObject }.getOrNull() }
            ?: JsonObject(emptyMap())
        else -> JsonObject(emptyMap())
    }
}

private fun textContents(text: String): JsonArray = buildJsonArray {
    addJsonObject {
        put("type", "text")
        put("text", text)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun lastErrorMessage(): String {
    return litert_lm_get_last_error_message()?.toKString() ?: "unknown error"
}

class LiteRtLmException(message: String) : Exception(message)
