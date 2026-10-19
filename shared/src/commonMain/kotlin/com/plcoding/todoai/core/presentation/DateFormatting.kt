package com.plcoding.todoai.core.presentation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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

private val dateFormat = LocalDate.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day()
    chars(", ")
    year()
}

fun Instant.toFormattedDateTime(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    return toLocalDateTime(timeZone).format(dateTimeFormat)
}

fun LocalDate.toFormattedDate(): String = format(dateFormat)

/**
 * Material 3 date pickers work with UTC midnight millis, independent of the device time zone.
 */
fun LocalDate.toDatePickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

fun Long.datePickerMillisToLocalDate(): LocalDate {
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
}
