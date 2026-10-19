package com.plcoding.todoai.chat.presentation

sealed interface ChatMessageUi {
    val id: String

    data class User(override val id: String, val text: String) : ChatMessageUi
    data class Assistant(override val id: String, val text: String) : ChatMessageUi
    data class ToolCall(override val id: String, val toolName: String, val arguments: String) : ChatMessageUi
    data class Error(override val id: String, val type: ChatError) : ChatMessageUi
}

enum class ChatError {
    GENERATION_FAILED,
    EMPTY_ANSWER
}
