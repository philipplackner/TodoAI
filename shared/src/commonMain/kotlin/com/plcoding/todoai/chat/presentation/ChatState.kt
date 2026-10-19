package com.plcoding.todoai.chat.presentation

import androidx.compose.runtime.Stable

@Stable
data class ChatState(
    val messages: List<ChatMessageUi> = emptyList(),
    val input: String = "",
    val isGenerating: Boolean = false
) {
    val canSend: Boolean get() = input.isNotBlank() && !isGenerating
}
