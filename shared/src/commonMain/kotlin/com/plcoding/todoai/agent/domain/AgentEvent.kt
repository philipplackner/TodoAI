package com.plcoding.todoai.agent.domain

sealed interface AgentEvent {
    data class ToolCalled(val call: ToolCall) : AgentEvent
    data class Answer(val text: String) : AgentEvent
}
