package com.plcoding.todoai.agent.domain

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.config.MissingToolsConversionStrategy
import ai.koog.agents.core.agent.config.ToolCallDescriber
import ai.koog.agents.core.agent.context.AIAgentFunctionalContext
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.tools.ToolBase
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.serialization.kotlinx.KotlinxSerializer
import com.plcoding.todoai.agent.data.LiteRtLmClient
import com.plcoding.todoai.agent.data.toPlainMap
import com.plcoding.todoai.agent.data.toolArgumentsJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Runs the to-do agent with Koog: send the user's prompt, execute every tool call the model asks
 * for, feed the results back, and repeat until the model answers with plain text.
 *
 * Each [send] runs a new Koog agent that starts from the conversation so far, so follow-up
 * questions keep their context.
 */
class TodoAgent(
    private val client: LiteRtLmClient,
    private val tools: List<ToolBase<*, *>>,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) {
    private val executor = MultiLLMPromptExecutor(client)
    private val mutex = Mutex()

    private var history: Prompt? = null
    private var historyDate: LocalDate? = null

    fun send(prompt: String): Flow<AgentEvent> = channelFlow {
        mutex.withLock {
            val today = clock.todayIn(timeZone())
            // The date is part of the system prompt, so a conversation from yesterday would resolve
            // "tomorrow" to the wrong day.
            val startPrompt = history.takeIf { historyDate == today }
                ?: prompt("todo-agent") { system(buildSystemPrompt(today)) }

            var endPrompt: Prompt? = null
            val agent = AIAgent(
                promptExecutor = executor,
                agentConfig = AIAgentConfig(
                    prompt = startPrompt,
                    model = LiteRtLmClient.model,
                    maxAgentIterations = MAX_AGENT_ITERATIONS,
                    missingToolsConversionStrategy = KeepToolCalls,
                    serializer = KotlinxSerializer(toolArgumentsJson)
                ),
                strategy = functionalStrategy<String, String>("todo-agent") { input ->
                    answer(input, onToolCall = { send(AgentEvent.ToolCalled(it)) })
                        .also { endPrompt = llm.readSession { this.prompt } }
                },
                toolRegistry = ToolRegistry { tools(tools) }
            )

            try {
                val answer = agent.run(prompt)
                history = endPrompt
                historyDate = today
                send(AgentEvent.Answer(answer.trim()))
            } catch (e: CancellationException) {
                forgetConversation()
                throw e
            } catch (e: Exception) {
                // A failed turn can leave the native conversation in a broken state.
                forgetConversation()
                throw e
            }
        }
    }

    suspend fun reset() {
        mutex.withLock { forgetConversation() }
    }

    private suspend fun AIAgentFunctionalContext.answer(
        input: String,
        onToolCall: suspend (ToolCall) -> Unit
    ): String {
        var response = requestLLM(input)
        var round = 0
        while (getToolCalls(response).isNotEmpty() && round < MAX_TOOL_ROUNDS) {
            round++
            val calls = getToolCalls(response)
            calls.forEach { onToolCall(it.toToolCall()) }
            response = sendToolResults(executeTools(calls))
        }
        return response.textContent(separator = "")
    }

    private suspend fun forgetConversation() {
        history = null
        historyDate = null
        client.reset()
    }

    private fun MessagePart.Tool.Call.toToolCall() = ToolCall(name = tool, arguments = ToolArguments(argsJson.toPlainMap()))

    private fun Message.Assistant.textContent(separator: String): String =
        parts.filterIsInstance<MessagePart.Text>().joinToString(separator) { it.text }

    private fun buildSystemPrompt(today: LocalDate): String = """
        You are a helpful assistant inside a to-do app. You can read and change the user's to-dos only through the provided tools.
        Today is ${today.toSpokenDate()}.

        Rules:
        - The user's to-dos are their tasks and plans. For any question about what they need to do, have done or plan to do, call a tool first. Never answer from memory and never make up to-dos.
        - To find to-dos about a topic, like "Do I need to record something?", call search_todos with a keyword. To find to-dos by due date, call get_todos.
        - Never calculate dates yourself. Pass dates and periods to the tools in the user's own words, like "friday" or "in 3 weeks". The app works out the exact date.
        - If a tool can't understand a date, try again with simpler wording or ask the user.
        - Keep titles short and don't put the due date into the title.
        - Refer to to-dos by their title.
        - Keep answers short and write plain text without Markdown, since the chat can't render it.
        - Use the dates exactly as the tools return them.
        - When listing to-dos, put each one on its own line starting with "• ".
    """.trimIndent()

    private companion object {
        const val MAX_TOOL_ROUNDS = 5

        /**
         * By default Koog rewrites calls to tools that aren't registered (a model inventing a tool
         * name) as text. The LiteRT-LM conversation already holds the call, so the prompt has to
         * stay as it is, and the "unknown tool" error goes back as a normal tool result.
         */
        val KeepToolCalls = object : MissingToolsConversionStrategy(ToolCallDescriber.JSON) {
            override fun convertPrompt(prompt: Prompt, tools: List<ToolDescriptor>): Prompt = prompt
        }

        // Each LLM request and tool run is a step; five tool rounds need well under this.
        const val MAX_AGENT_ITERATIONS = 50
    }
}
