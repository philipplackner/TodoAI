package com.plcoding.todoai.agent.data

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.utils.time.KoogClock
import com.plcoding.todoai.agent.domain.LlmConversation
import com.plcoding.todoai.agent.domain.LlmEngine
import com.plcoding.todoai.agent.domain.LlmMessage
import com.plcoding.todoai.agent.domain.LlmResponse
import com.plcoding.todoai.agent.domain.ToolResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.uuid.Uuid

object LiteRtLmProvider : LLMProvider(id = "litert-lm", display = "LiteRT-LM")

/**
 * Koog [LLMClient] for the on-device model, on top of the platform [LlmEngine].
 *
 * Koog sends the whole prompt on every call, while a LiteRT-LM conversation keeps its own history
 * and runs the model on every message it gets. So the client keeps one native conversation and
 * only sends the message that is new since the last call. A different system prompt or tool set
 * starts a new conversation.
 *
 * Koog's own `prompt-executor-litert-client` does the same on Android, but it has no iOS
 * implementation, and it doesn't fall back from GPU to CPU or let the app pick the model file.
 */
class LiteRtLmClient(
    private val engine: LlmEngine,
    private val clock: KoogClock = KoogClock.System
) : LLMClient() {

    private val mutex = Mutex()
    private var conversation: LlmConversation? = null
    private var conversationKey: ConversationKey? = null

    // Non-system messages the native conversation already holds, including its own answers.
    private var sentMessages: List<Message> = emptyList()

    override suspend fun execute(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): Message.Assistant =
        mutex.withLock {
            val systemPrompt = prompt.messages.filterIsInstance<Message.System>().joinToString("\n") { it.textContent() }
            val messages = prompt.messages.filterNot { it is Message.System }
            require(messages.isNotEmpty()) { "The prompt has no message to send" }

            val key = ConversationKey(systemPrompt, tools.map { it.toFunctionDeclaration() })
            // If the prompt doesn't continue the native history (it was edited or reset), the
            // earlier messages can't be replayed without running the model on each of them, so a
            // new conversation starts from the last message.
            val canReuse = conversationKey == key && sentMessages == messages.dropLast(1)
            val active = conversation?.takeIf { canReuse } ?: run {
                closeConversation()
                engine.createConversation(systemPrompt, key.tools).also {
                    conversation = it
                    conversationKey = key
                }
            }

            try {
                val response = active.send(messages.last().toLlmMessage()).toAssistantMessage()
                sentMessages = messages + response
                response
            } catch (e: Throwable) {
                // A failed or cancelled turn can leave the native conversation in a broken state.
                closeConversation()
                throw e
            }
        }

    /** Drops the native conversation, so the next call starts a new one. */
    suspend fun reset() {
        mutex.withLock { closeConversation() }
    }

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult {
        throw UnsupportedOperationException("Moderation is not supported for on-device models")
    }

    override fun llmProvider(): LLMProvider = LiteRtLmProvider

    override fun close() {
        closeConversation()
    }

    private fun closeConversation() {
        conversation?.close()
        conversation = null
        conversationKey = null
        sentMessages = emptyList()
    }

    private fun Message.toLlmMessage(): LlmMessage {
        val toolResults = parts.filterIsInstance<MessagePart.Tool.Result>()
        return when {
            this is Message.User && toolResults.isNotEmpty() -> LlmMessage.ToolResults(
                toolResults.map { ToolResult(toolName = it.tool, result = it.toResultMap()) }
            )
            this is Message.User -> LlmMessage.User(textContent())
            else -> error("Can't send a ${role.name} message to the model")
        }
    }

    /**
     * Tools return JSON objects, which go back to the model as they are. Koog reports a failed
     * call as plain text, which becomes an `error` field so the model can correct the call.
     */
    private fun MessagePart.Tool.Result.toResultMap(): Map<String, Any?> {
        if (isError) return mapOf("error" to output)
        val json = runCatching { Json.parseToJsonElement(output) as? JsonObject }.getOrNull()
        return json?.toPlainMap() ?: mapOf("result" to output)
    }

    private fun LlmResponse.toAssistantMessage(): Message.Assistant {
        val parts = buildList {
            if (text.isNotEmpty() || toolCalls.isEmpty()) {
                add(MessagePart.Text(text))
            }
            // LiteRT-LM has no call ids, it matches results to calls by tool name. Koog needs an
            // id to pair them, so each call gets a unique one.
            toolCalls.forEach { call ->
                add(
                    MessagePart.Tool.Call(
                        id = "litert-${call.name}-${Uuid.random()}",
                        tool = call.name,
                        args = call.arguments.values.toJsonObject().toString()
                    )
                )
            }
        }
        return Message.Assistant(parts = parts, metaInfo = ResponseMetaInfo.create(clock))
    }

    private data class ConversationKey(val systemPrompt: String, val tools: List<JsonObject>)

    companion object {
        /** The model is whichever file [LlmEngine.load] opened, so the id is only a label. */
        val model = LLModel(
            provider = LiteRtLmProvider,
            id = "gemma-4-it",
            capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
            contextLength = 4096
        )
    }
}
