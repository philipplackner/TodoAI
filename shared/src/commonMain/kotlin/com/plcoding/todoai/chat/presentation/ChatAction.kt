package com.plcoding.todoai.chat.presentation

sealed interface ChatAction {
    data object OnSendClick : ChatAction
    data object OnDownloadModelClick : ChatAction
    data object OnRetryLoadModelClick : ChatAction
    data object OnNewChatClick : ChatAction
}
