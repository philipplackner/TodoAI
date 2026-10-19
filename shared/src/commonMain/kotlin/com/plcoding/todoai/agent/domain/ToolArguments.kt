package com.plcoding.todoai.agent.domain

/**
 * The arguments of a tool call as the model sent them.
 */
data class ToolArguments(val values: Map<String, Any?>) {

    /**
     * Small models sometimes send `"null"` or an empty string instead of leaving out an optional
     * argument, so both count as missing.
     */
    fun string(name: String): String? {
        val value = values[name]?.toString()?.trim()
        return value?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    }

    fun boolean(name: String): Boolean? = when (val value = values[name]) {
        is Boolean -> value
        is String -> value.trim().toBooleanStrictOrNull()
        else -> null
    }
}
