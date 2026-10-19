package com.plcoding.todoai.agent

import android.os.Build
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.tool
import com.plcoding.todoai.agent.data.toFunctionDeclaration
import com.plcoding.todoai.agent.domain.AgentTool
import com.plcoding.todoai.agent.domain.LlmConversation
import com.plcoding.todoai.agent.domain.LlmEngine
import com.plcoding.todoai.agent.domain.LlmMessage
import com.plcoding.todoai.agent.domain.LlmResponse
import com.plcoding.todoai.agent.domain.ToolArguments
import com.plcoding.todoai.agent.domain.ToolCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LiteRtLmEngine : LlmEngine {

    private val mutex = Mutex()
    private var engine: Engine? = null

    override val isLoaded: Boolean get() = engine != null

    override suspend fun load(modelPath: String, cacheDir: String) = mutex.withLock {
        if (engine != null) return@withLock
        engine = withContext(Dispatchers.Default) {
            // The emulator's GPU initializes fine but has no OpenCL, so generation would fail later.
            if (isEmulator()) {
                createEngine(modelPath, cacheDir, Backend.CPU()).getOrThrow()
            } else {
                createEngine(modelPath, cacheDir, Backend.GPU())
                    .recoverCatching { createEngine(modelPath, cacheDir, Backend.CPU()).getOrThrow() }
                    .getOrThrow()
            }
        }
    }

    override suspend fun createConversation(
        systemPrompt: String,
        tools: List<AgentTool>
    ): LlmConversation {
        val engine = checkNotNull(engine) { "The model is not loaded" }
        val conversation = withContext(Dispatchers.Default) {
            engine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(systemPrompt),
                    tools = tools.map { tool(DescriptionOnlyTool(it)) },
                    automaticToolCalling = false
                )
            )
        }
        return LiteRtLmConversation(conversation)
    }

    private fun createEngine(modelPath: String, cacheDir: String, backend: Backend): Result<Engine> {
        return runCatching {
            Engine(
                EngineConfig(
                    modelPath = modelPath,
                    backend = backend,
                    maxNumTokens = MAX_NUM_TOKENS,
                    cacheDir = cacheDir
                )
            ).apply { initialize() }
        }
    }

    private fun isEmulator(): Boolean = Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish"

    private companion object {
        const val MAX_NUM_TOKENS = 4096
    }
}

private class LiteRtLmConversation(
    private val conversation: Conversation
) : LlmConversation {

    override suspend fun send(message: LlmMessage): LlmResponse = withContext(Dispatchers.Default) {
        val response = conversation.sendMessage(message.toLiteRtMessage())
        LlmResponse(
            text = response.contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString(separator = "") { it.text },
            toolCalls = response.toolCalls.map { call ->
                ToolCall(name = call.name, arguments = ToolArguments(call.arguments))
            }
        )
    }

    override fun close() {
        conversation.close()
    }

    private fun LlmMessage.toLiteRtMessage(): Message = when (this) {
        is LlmMessage.User -> Message.user(text)
        is LlmMessage.ToolResults -> Message.tool(
            Contents.of(
                results.map { result ->
                    Content.ToolResponse(result.toolName, result.result)
                }
            )
        )
    }
}

/**
 * Only describes the tool to the model. With `automaticToolCalling = false`, LiteRT-LM returns
 * tool calls to us instead of calling [execute], and [com.plcoding.todoai.agent.domain.TodoAgent]
 * runs them.
 */
private class DescriptionOnlyTool(private val tool: AgentTool) : OpenApiTool {
    override fun getToolDescriptionJsonString(): String = tool.toFunctionDeclaration().toString()

    override fun execute(paramsJsonString: String): String {
        error("Tool '${tool.name}' is executed by TodoAgent, not by LiteRT-LM")
    }
}
