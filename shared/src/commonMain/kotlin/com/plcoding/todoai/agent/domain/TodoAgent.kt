package com.plcoding.todoai.agent.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Runs the agent loop: send the user's prompt, execute every tool call the model asks for, feed
 * the results back, and repeat until the model answers with plain text.
 */
class TodoAgent(
    private val engine: LlmEngine,
    tools: List<AgentTool>,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) {
    private val toolsByName = tools.associateBy { it.name }
    private val mutex = Mutex()

    private var conversation: LlmConversation? = null
    private var conversationDate: LocalDate? = null

    fun send(prompt: String): Flow<AgentEvent> = flow {
        mutex.withLock {
            val conversation = currentConversation()
            try {
                var response = conversation.send(LlmMessage.User(prompt))
                var round = 0
                while (response.toolCalls.isNotEmpty() && round < MAX_TOOL_ROUNDS) {
                    round++
                    val results = response.toolCalls.map { call ->
                        emit(AgentEvent.ToolCalled(call))
                        ToolResult(toolName = call.name, result = execute(call))
                    }
                    response = conversation.send(LlmMessage.ToolResults(results))
                }
                emit(AgentEvent.Answer(response.text.trim()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed turn can leave the native conversation in a broken state.
                closeConversation()
                throw e
            }
        }
    }

    suspend fun reset() {
        mutex.withLock { closeConversation() }
    }

    private suspend fun currentConversation(): LlmConversation {
        val today = clock.todayIn(timeZone())
        // The date is part of the system prompt, so a conversation from yesterday would resolve
        // "tomorrow" to the wrong day.
        if (conversationDate != today) {
            closeConversation()
        }
        return conversation ?: engine.createConversation(
            systemPrompt = buildSystemPrompt(today),
            tools = toolsByName.values.toList()
        ).also {
            conversation = it
            conversationDate = today
        }
    }

    private suspend fun execute(call: ToolCall): Map<String, Any?> {
        val tool = toolsByName[call.name]
            ?: return errorResult("Unknown tool '${call.name}'. Available tools: ${toolsByName.keys.joinToString()}.")
        return try {
            tool.execute(call.arguments)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorResult(e.message ?: "The tool failed.")
        }
    }

    private fun errorResult(message: String): Map<String, Any?> = mapOf("error" to message)

    private fun closeConversation() {
        conversation?.close()
        conversation = null
        conversationDate = null
    }

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
    }
}
