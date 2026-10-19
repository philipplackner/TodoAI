package com.plcoding.todoai.chat.presentation

sealed interface ChatAction {
    data class OnInputChange(val input: String) : ChatAction
    data object OnSendClick : ChatAction
}
