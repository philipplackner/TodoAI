package com.plcoding.todoai.agent.domain

import kotlinx.serialization.json.JsonObject

/**
 * Platform-specific runtime that runs a local LLM. Android and iOS both implement it on top of
 * LiteRT-LM. Koog reaches it through [com.plcoding.todoai.agent.data.LiteRtLmClient], and Koog's
 * agent runs the tools.
 */
interface LlmEngine {
    val isLoaded: Boolean

    suspend fun load(modelPath: String, cacheDir: String)

    /**
     * @param tools Function declarations (name, description and a JSON schema of the parameters),
     * the format LiteRT-LM expects.
     */
    suspend fun createConversation(systemPrompt: String, tools: List<JsonObject>): LlmConversation
}
