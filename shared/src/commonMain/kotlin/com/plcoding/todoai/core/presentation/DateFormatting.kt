package com.plcoding.todoai.core.presentation

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val dateTimeFormat = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day()
    chars(", ")
    year()
    chars(", ")
    hour()
    char(':')
    minute()
}

fun Instant.toFormattedDateTime(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    return toLocalDateTime(timeZone).format(dateTimeFormat)
}
