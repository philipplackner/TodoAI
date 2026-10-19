package com.plcoding.todoai.agent

import com.plcoding.todoai.agent.domain.AgentEvent
import com.plcoding.todoai.agent.domain.AgentTool
import com.plcoding.todoai.agent.domain.LlmMessage
import com.plcoding.todoai.agent.domain.LlmResponse
import com.plcoding.todoai.agent.domain.ToolArguments
import com.plcoding.todoai.agent.domain.ToolCall
import com.plcoding.todoai.agent.domain.ToolParameter
import com.plcoding.todoai.agent.domain.ToolResult
import com.plcoding.todoai.agent.domain.TodoAgent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class TodoAgentTest {

    private val echoTool = object : AgentTool {
        override val name = "echo"
        override val description = "Echoes its input"
        override val parameters = listOf(ToolParameter("text", ToolParameter.Type.STRING, "Text to echo"))
        override suspend fun execute(arguments: ToolArguments): Map<String, Any?> = arguments.values
    }

    private val failingTool = object : AgentTool {
        override val name = "fail"
        override val description = "Always fails"
        override val parameters = emptyList<ToolParameter>()
        override suspend fun execute(arguments: ToolArguments): Map<String, Any?> = error("Database is locked")
    }

    private class MutableClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private val clock = MutableClock(Instant.parse("2026-10-19T10:00:00Z"))

    private fun agent(engine: FakeLlmEngine) = TodoAgent(
        engine = engine,
        tools = listOf(echoTool, failingTool),
        clock = clock,
        timeZone = { TimeZone.UTC }
    )

    private fun toolCall(name: String, vararg arguments: Pair<String, String>) = LlmResponse(
        text = "",
        toolCalls = listOf(ToolCall(name, ToolArguments(arguments.toMap())))
    )

    private fun FakeLlmEngine.sentToolResult(): ToolResult {
        return assertIs<LlmMessage.ToolResults>(sentMessages[1]).results.single()
    }

    private fun answer(text: String) = LlmResponse(text = text, toolCalls = emptyList())

    @Test
    fun plainAnswerSkipsTools() = runTest {
        val engine = FakeLlmEngine { answer("  Hello  ") }

        val events = agent(engine).send("Hi").toList()

        assertEquals(listOf<AgentEvent>(AgentEvent.Answer("Hello")), events)
        assertEquals(listOf<LlmMessage>(LlmMessage.User("Hi")), engine.sentMessages)
    }

    @Test
    fun toolCallResultIsSentBackBeforeTheAnswer() = runTest {
        val engine = FakeLlmEngine { message ->
            when (message) {
                is LlmMessage.User -> toolCall("echo", "text" to "ping")
                is LlmMessage.ToolResults -> answer("pong")
            }
        }

        val events = agent(engine).send("Echo ping").toList()

        assertEquals(2, events.size)
        assertEquals("echo", assertIs<AgentEvent.ToolCalled>(events[0]).call.name)
        assertEquals(AgentEvent.Answer("pong"), events[1])
        assertEquals(mapOf("text" to "ping"), engine.sentToolResult().result)
    }

    @Test
    fun unknownToolReturnsAnErrorToTheModel() = runTest {
        val engine = FakeLlmEngine { message ->
            when (message) {
                is LlmMessage.User -> toolCall("delete_everything")
                is LlmMessage.ToolResults -> answer("Sorry")
            }
        }

        agent(engine).send("Delete all").toList()

        val error = engine.sentToolResult().result["error"].toString()
        assertTrue("delete_everything" in error && "echo" in error, error)
    }

    @Test
    fun toolExceptionBecomesAnErrorResult() = runTest {
        val engine = FakeLlmEngine { message ->
            when (message) {
                is LlmMessage.User -> toolCall("fail")
                is LlmMessage.ToolResults -> answer("It failed")
            }
        }

        val events = agent(engine).send("Do it").toList()

        assertEquals(AgentEvent.Answer("It failed"), events.last())
        assertEquals("Database is locked", engine.sentToolResult().result["error"])
    }

    @Test
    fun endlessToolCallsStopAfterFiveRounds() = runTest {
        val engine = FakeLlmEngine { toolCall("echo", "text" to "again") }

        val events = agent(engine).send("Loop").toList()

        assertEquals(5, events.count { it is AgentEvent.ToolCalled })
        assertEquals(AgentEvent.Answer(""), events.last())
        assertEquals(6, engine.sentMessages.size)
    }

    @Test
    fun conversationIsReusedOnTheSameDay() = runTest {
        val engine = FakeLlmEngine { answer("ok") }
        val agent = agent(engine)

        agent.send("One").toList()
        clock.instant = Instant.parse("2026-10-19T23:59:00Z")
        agent.send("Two").toList()

        assertEquals(1, engine.systemPrompts.size)
    }

    @Test
    fun newDayStartsAConversationWithTheNewDate() = runTest {
        val engine = FakeLlmEngine { answer("ok") }
        val agent = agent(engine)

        agent.send("One").toList()
        clock.instant = Instant.parse("2026-10-20T00:01:00Z")
        agent.send("Two").toList()

        assertEquals(2, engine.systemPrompts.size)
        assertEquals(1, engine.closedConversations)
        assertTrue("Today is Tuesday, October 20, 2026." in engine.systemPrompts[1])
    }

    @Test
    fun engineFailureClosesTheConversationAndNextPromptStartsFresh() = runTest {
        var shouldFail = true
        val engine = FakeLlmEngine {
            if (shouldFail) error("Context window full") else answer("ok")
        }
        val agent = agent(engine)

        assertFailsWith<IllegalStateException> { agent.send("One").toList() }
        shouldFail = false
        val events = agent.send("Two").toList()

        assertEquals(AgentEvent.Answer("ok"), events.single())
        assertEquals(1, engine.closedConversations)
        assertEquals(2, engine.systemPrompts.size)
    }
}
