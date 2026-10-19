package com.plcoding.todoai.agent

import com.plcoding.todoai.agent.domain.AgentTool
import com.plcoding.todoai.agent.domain.LlmConversation
import com.plcoding.todoai.agent.domain.LlmEngine
import com.plcoding.todoai.agent.domain.LlmMessage
import com.plcoding.todoai.agent.domain.LlmResponse

class FakeLlmEngine(
    private val respond: (LlmMessage) -> LlmResponse
) : LlmEngine {

    val sentMessages = mutableListOf<LlmMessage>()
    val systemPrompts = mutableListOf<String>()
    var closedConversations = 0
        private set

    override val isLoaded: Boolean = true

    override suspend fun load(modelPath: String, cacheDir: String) = Unit

    override suspend fun createConversation(systemPrompt: String, tools: List<AgentTool>): LlmConversation {
        systemPrompts += systemPrompt
        return object : LlmConversation {
            override suspend fun send(message: LlmMessage): LlmResponse {
                sentMessages += message
                return respond(message)
            }

            override fun close() {
                closedConversations++
            }
        }
    }
}
