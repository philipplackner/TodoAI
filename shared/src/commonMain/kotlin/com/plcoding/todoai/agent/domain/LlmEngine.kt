package com.plcoding.todoai.agent.domain

/**
 * Platform-specific runtime that runs a local LLM. Android and iOS both implement it on top of
 * LiteRT-LM. Tool execution is not part of the engine, it lives in [TodoAgent].
 */
interface LlmEngine {
    val isLoaded: Boolean

    suspend fun load(modelPath: String, cacheDir: String)

    suspend fun createConversation(systemPrompt: String, tools: List<AgentTool>): LlmConversation
}
