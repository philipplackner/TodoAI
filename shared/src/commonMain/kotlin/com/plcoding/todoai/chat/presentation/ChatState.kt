package com.plcoding.todoai.chat.presentation

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import com.plcoding.todoai.agent.domain.ModelState

@Stable
data class ChatState(
    val messages: List<ChatMessageUi> = emptyList(),
    val input: TextFieldState = TextFieldState(),
    val isGenerating: Boolean = false,
    val modelState: ModelState = ModelState.Ready
) {
    val isModelReady: Boolean get() = modelState == ModelState.Ready
    val canSend: Boolean get() = input.text.isNotBlank() && !isGenerating && isModelReady
}
