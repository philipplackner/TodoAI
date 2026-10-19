package com.plcoding.todoai.agent.domain

/**
 * One chat with the model, created by [LlmEngine.createConversation]. It keeps the history,
 * so each [send] only passes the new message.
 */
interface LlmConversation {
    suspend fun send(message: LlmMessage): LlmResponse
    fun close()
}
