package com.plcoding.todoai.chat.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

class ChatViewModel : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state = _state.asStateFlow()

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.OnInputChange -> {
                _state.update { it.copy(input = action.input) }
            }
            ChatAction.OnSendClick -> sendMessage()
        }
    }

    private fun sendMessage() {
        val currentState = state.value
        if (!currentState.canSend) {
            return
        }
        val message = ChatMessageUi(
            id = Uuid.random().toString(),
            text = currentState.input.trim(),
            isFromUser = true
        )
        _state.update {
            it.copy(
                messages = it.messages + message,
                input = ""
            )
        }
    }
}
