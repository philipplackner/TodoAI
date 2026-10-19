package com.plcoding.todoai.agent.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

private val spokenDateFormat = LocalDate.Format {
    dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
    chars(", ")
    monthName(MonthNames.ENGLISH_FULL)
    char(' ')
    day(padding = Padding.NONE)
    chars(", ")
    year()
}

/**
 * Formats a date like "Friday, October 23, 2026". Tool results use this form so the model can
 * repeat the weekday instead of working it out.
 */
fun LocalDate.toSpokenDate(): String = format(spokenDateFormat)
