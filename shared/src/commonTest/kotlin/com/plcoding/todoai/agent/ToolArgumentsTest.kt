package com.plcoding.todoai.agent

import com.plcoding.todoai.agent.domain.ToolArguments
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ToolArgumentsTest {

    @Test
    fun emptyAndNullStringsCountAsMissing() {
        val arguments = ToolArguments(mapOf("a" to "", "b" to " null ", "c" to null, "d" to " Call mom "))

        assertNull(arguments.string("a"))
        assertNull(arguments.string("b"))
        assertNull(arguments.string("c"))
        assertNull(arguments.string("missing"))
        assertEquals("Call mom", arguments.string("d"))
    }

    @Test
    fun booleansAreAcceptedAsStrings() {
        val arguments = ToolArguments(mapOf("a" to true, "b" to "false", "c" to "maybe"))

        assertEquals(true, arguments.boolean("a"))
        assertEquals(false, arguments.boolean("b"))
        assertNull(arguments.boolean("c"))
    }
}
