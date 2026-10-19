package com.plcoding.todoai.agent.domain

sealed interface LlmMessage {
    data class User(val text: String) : LlmMessage
    data class ToolResults(val results: List<ToolResult>) : LlmMessage
}

data class ToolResult(
    val toolName: String,
    val result: Map<String, Any?>
)

data class LlmResponse(
    val text: String,
    val toolCalls: List<ToolCall>
)

data class ToolCall(
    val name: String,
    val arguments: ToolArguments
)
