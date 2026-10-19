package com.plcoding.todoai.chat.presentation

import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.todoai.agent.data.ModelManager
import com.plcoding.todoai.agent.domain.AgentEvent
import com.plcoding.todoai.agent.domain.TodoAgent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class ChatViewModel(
    private val agent: TodoAgent,
    private val modelManager: ModelManager
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState(modelState = modelManager.state.value))
    val state = _state.asStateFlow()

    private var generationJob: Job? = null

    init {
        modelManager.state
            .onEach { modelState -> _state.update { it.copy(modelState = modelState) } }
            .launchIn(viewModelScope)
        modelManager.prepare()
    }

    fun onAction(action: ChatAction) {
        when (action) {
            ChatAction.OnSendClick -> sendMessage()
            ChatAction.OnDownloadModelClick -> modelManager.download()
            ChatAction.OnRetryLoadModelClick -> modelManager.prepare()
            ChatAction.OnNewChatClick -> startNewChat()
        }
    }

    private fun sendMessage() {
        val currentState = state.value
        if (!currentState.canSend) {
            return
        }
        val prompt = currentState.input.text.toString().trim()
        currentState.input.clearText()
        _state.update {
            it.copy(
                messages = it.messages + ChatMessageUi.User(id = newId(), text = prompt),
                isGenerating = true
            )
        }
        generationJob = viewModelScope.launch {
            try {
                agent.send(prompt).collect { event ->
                    appendMessage(event.toChatMessageUi())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Agent failed: $e")
                appendMessage(ChatMessageUi.Error(id = newId(), type = ChatError.GENERATION_FAILED))
            } finally {
                _state.update { it.copy(isGenerating = false) }
            }
        }
    }

    private fun startNewChat() {
        generationJob?.cancel()
        _state.update { it.copy(messages = emptyList(), isGenerating = false) }
        viewModelScope.launch { agent.reset() }
    }

    private fun appendMessage(message: ChatMessageUi) {
        _state.update { it.copy(messages = it.messages + message) }
    }

    private fun AgentEvent.toChatMessageUi(): ChatMessageUi = when (this) {
        is AgentEvent.ToolCalled -> ChatMessageUi.ToolCall(
            id = newId(),
            toolName = call.name,
            arguments = call.arguments.values.values.joinToString()
        )
        is AgentEvent.Answer -> if (text.isBlank()) {
            ChatMessageUi.Error(id = newId(), type = ChatError.EMPTY_ANSWER)
        } else {
            ChatMessageUi.Assistant(id = newId(), text = text)
        }
    }

    private fun newId(): String = Uuid.random().toString()
}
