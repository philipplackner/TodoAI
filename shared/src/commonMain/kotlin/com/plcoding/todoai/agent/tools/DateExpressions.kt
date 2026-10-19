package com.plcoding.todoai.agent.tools

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

/**
 * Which to-dos a `get_todos` call asks for, based on their due date.
 */
sealed interface DueFilter {
    data object Overdue : DueFilter
    data object NoDueDate : DueFilter
    data object All : DueFilter
    data class Between(val from: LocalDate, val to: LocalDate) : DueFilter
}

/**
 * Turns the user's own date wording into exact dates. The model only copies phrases like
 * "friday" or "in 3 weeks" into tool arguments, since small models get date arithmetic wrong.
 *
 * Conventions:
 * - Weeks run Monday to Sunday.
 * - "friday" and "coming friday" mean the next Friday after today. "this friday" also accepts
 *   today. "next friday" means the Friday of next week.
 * - Dates without a year, like "november 5", mean the next time that date comes up.
 */
object DateExpressions {

    const val DATE_EXAMPLES =
        "'today', 'tomorrow', 'friday', 'next friday', 'in 3 weeks', 'in 2 months', " +
            "'end of month', 'november 5' or '2026-11-05'"

    const val PERIOD_EXAMPLES =
        "'today', 'this week', 'next week', 'next month', 'friday', 'in 3 weeks', " +
            "'next 10 days', 'until november 5', 'overdue', 'no due date' or 'all'"

    fun parseDate(input: String, today: LocalDate): LocalDate? {
        val words = words(input)
        if (words.isEmpty()) return null
        return parseIsoDate(words) ?: parseRelativeDate(words, today) ?: parseCalendarDate(words, today)
    }

    fun parsePeriod(input: String, today: LocalDate): DueFilter? {
        val words = words(input)
        if (words.isEmpty()) return null
        val text = words.joinToString(" ")

        when (text) {
            "overdue", "late", "past due", "missed" -> return DueFilter.Overdue
            "no due date", "no date", "without due date", "without a due date", "undated", "someday" ->
                return DueFilter.NoDueDate
            "all", "everything", "anything", "any", "all todos", "all to-dos" -> return DueFilter.All
            "this week", "rest of week", "rest of this week" -> return DueFilter.Between(today, endOfWeek(today))
            "next week" -> {
                val start = startOfWeek(today).plus(1, DateTimeUnit.WEEK)
                return DueFilter.Between(start, endOfWeek(start))
            }
            "this month", "rest of month", "rest of this month" -> return DueFilter.Between(today, endOfMonth(today))
            "next month" -> {
                val start = startOfMonth(today).plus(1, DateTimeUnit.MONTH)
                return DueFilter.Between(start, endOfMonth(start))
            }
            "this year", "rest of year", "rest of this year" -> return DueFilter.Between(today, LocalDate(today.year, 12, 31))
            "this weekend", "weekend" -> {
                val saturday = startOfWeek(today).plus(5, DateTimeUnit.DAY)
                return DueFilter.Between(maxOf(saturday, today), endOfWeek(today))
            }
            "next weekend" -> {
                val saturday = startOfWeek(today).plus(12, DateTimeUnit.DAY)
                return DueFilter.Between(saturday, saturday.plus(1, DateTimeUnit.DAY))
            }
        }

        // "next 10 days", "coming 2 weeks", "within a month"
        if (words.first() in listOf("next", "coming", "within") && words.size == 3) {
            val amount = parseNumber(words[1])
            val unit = parseUnit(words[2])
            if (amount != null && unit != null) {
                return DueFilter.Between(today, today.plus(amount, unit))
            }
        }

        // "until friday", "by november 5", "before the 20th"
        if (words.first() in listOf("until", "till", "by", "through", "before")) {
            val end = parseDate(words.drop(1).joinToString(" "), today) ?: return null
            val to = if (words.first() == "before") end.minus(1, DateTimeUnit.DAY) else end
            return if (to < today) null else DueFilter.Between(today, to)
        }

        // "between friday and next tuesday", "from monday to friday"
        if (words.first() in listOf("between", "from")) {
            val separatorIndex = words.indexOfFirst { it == "and" || it == "to" || it == "until" }
            if (separatorIndex > 1) {
                val from = parseDate(words.subList(1, separatorIndex).joinToString(" "), today)
                val to = parseDate(words.drop(separatorIndex + 1).joinToString(" "), today)
                if (from != null && to != null) {
                    return DueFilter.Between(minOf(from, to), maxOf(from, to))
                }
            }
            return null
        }

        // "in 3 weeks" asks about that whole week, "in 2 months" about that whole month.
        if (words.size == 3 && words[0] == "in") {
            val amount = parseNumber(words[1])
            val unit = parseUnit(words[2])
            if (amount != null && unit == DateTimeUnit.WEEK) {
                val start = startOfWeek(today.plus(amount, DateTimeUnit.WEEK))
                return DueFilter.Between(start, endOfWeek(start))
            }
            if (amount != null && unit == DateTimeUnit.MONTH) {
                val start = startOfMonth(today.plus(amount, DateTimeUnit.MONTH))
                return DueFilter.Between(start, endOfMonth(start))
            }
        }

        return parseDate(text, today)?.let { DueFilter.Between(it, it) }
    }

    private fun parseIsoDate(words: List<String>): LocalDate? {
        val text = words.singleOrNull() ?: return null
        if (!ISO_DATE.matches(text)) return null
        return runCatching { LocalDate.parse(text) }.getOrNull()
    }

    private fun parseRelativeDate(words: List<String>, today: LocalDate): LocalDate? {
        val text = words.joinToString(" ")
        when (text) {
            "today", "tonight", "now" -> return today
            "tomorrow", "tmrw" -> return today.plus(1, DateTimeUnit.DAY)
            "day after tomorrow" -> return today.plus(2, DateTimeUnit.DAY)
            "yesterday" -> return today.minus(1, DateTimeUnit.DAY)
            "next week" -> return startOfWeek(today).plus(1, DateTimeUnit.WEEK)
            "next month" -> return startOfMonth(today).plus(1, DateTimeUnit.MONTH)
            "next year" -> return LocalDate(today.year + 1, 1, 1)
            "end of week", "end of this week", "this weekend", "weekend" -> return endOfWeek(today)
            "end of next week" -> return endOfWeek(today.plus(1, DateTimeUnit.WEEK))
            "end of month", "end of this month" -> return endOfMonth(today)
            "end of next month" -> return endOfMonth(startOfMonth(today).plus(1, DateTimeUnit.MONTH))
            "end of year", "end of this year" -> return LocalDate(today.year, 12, 31)
            "next weekend" -> return startOfWeek(today).plus(12, DateTimeUnit.DAY)
        }

        parseWeekdayPhrase(words, today)?.let { return it }

        // "in 3 weeks", "in a month"
        if (words.size == 3 && words[0] == "in") {
            val amount = parseNumber(words[1]) ?: return null
            val unit = parseUnit(words[2]) ?: return null
            return today.plus(amount, unit)
        }

        // "3 weeks from now", "a week from today"
        if (words.size == 4 && words[2] == "from" && words[3] in listOf("now", "today")) {
            val amount = parseNumber(words[0]) ?: return null
            val unit = parseUnit(words[1]) ?: return null
            return today.plus(amount, unit)
        }

        return null
    }

    private fun parseWeekdayPhrase(words: List<String>, today: LocalDate): LocalDate? {
        return when (words.size) {
            1 -> parseWeekday(words[0])?.let { nextWeekday(today, it, includeToday = false) }
            2 -> {
                val weekday = parseWeekday(words[1]) ?: return null
                when (words[0]) {
                    "this" -> nextWeekday(today, weekday, includeToday = true)
                    "coming" -> nextWeekday(today, weekday, includeToday = false)
                    "next" -> weekdayInWeek(startOfWeek(today).plus(1, DateTimeUnit.WEEK), weekday)
                    else -> null
                }
            }
            // "friday next week", "next week friday"
            3 -> when {
                words[1] == "next" && words[2] == "week" -> parseWeekday(words[0])
                words[0] == "next" && words[1] == "week" -> parseWeekday(words[2])
                else -> null
            }?.let { weekdayInWeek(startOfWeek(today).plus(1, DateTimeUnit.WEEK), it) }
            else -> null
        }
    }

    /**
     * Handles "november 5", "nov 5th", "5 november", "5th of november 2027" and "the 20th".
     */
    private fun parseCalendarDate(words: List<String>, today: LocalDate): LocalDate? {
        val dateWords = words.filter { it != "of" }
        val month = dateWords.firstNotNullOfOrNull { parseMonth(it) }
        val numbers = dateWords.mapNotNull { word -> ORDINAL.matchEntire(word)?.groupValues?.get(1)?.toInt() }
        val unknownWords = dateWords.filter { parseMonth(it) == null && !ORDINAL.matches(it) }
        if (unknownWords.isNotEmpty()) return null

        if (month == null) {
            // A lone "20th" means the next 20th of a month. A lone "20" is too ambiguous.
            val day = numbers.singleOrNull() ?: return null
            if (!dateWords.single().any { it.isLetter() }) return null
            return nextDayOfMonth(today, day)
        }

        return when (numbers.size) {
            1 -> {
                val day = numbers.single()
                // Searching up to 8 years ahead finds the next February 29.
                (today.year..today.year + 8).firstNotNullOfOrNull { year ->
                    dateOrNull(year, month, day)?.takeIf { it >= today }
                }
            }
            2 -> {
                val (day, year) = if (numbers[0] > 31) numbers[1] to numbers[0] else numbers[0] to numbers[1]
                if (year < 1000) null else dateOrNull(year, month, day)
            }
            else -> null
        }
    }

    private fun nextDayOfMonth(today: LocalDate, day: Int): LocalDate? {
        var monthStart = startOfMonth(today)
        repeat(12) {
            val candidate = dateOrNull(monthStart.year, monthStart.month, day)
            if (candidate != null && candidate >= today) return candidate
            monthStart = monthStart.plus(1, DateTimeUnit.MONTH)
        }
        return null
    }

    private fun nextWeekday(today: LocalDate, weekday: DayOfWeek, includeToday: Boolean): LocalDate {
        val daysAhead = (weekday.isoDayNumber - today.dayOfWeek.isoDayNumber + 7) % 7
        val offset = if (daysAhead == 0 && !includeToday) 7 else daysAhead
        return today.plus(offset, DateTimeUnit.DAY)
    }

    private fun weekdayInWeek(weekStart: LocalDate, weekday: DayOfWeek): LocalDate {
        return weekStart.plus(weekday.isoDayNumber - 1, DateTimeUnit.DAY)
    }

    private fun startOfWeek(date: LocalDate): LocalDate = date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    private fun endOfWeek(date: LocalDate): LocalDate = startOfWeek(date).plus(6, DateTimeUnit.DAY)

    private fun startOfMonth(date: LocalDate): LocalDate = LocalDate(date.year, date.month, 1)

    private fun endOfMonth(date: LocalDate): LocalDate {
        return startOfMonth(date).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    }

    private fun dateOrNull(year: Int, month: Month, day: Int): LocalDate? {
        return runCatching { LocalDate(year, month.number, day) }.getOrNull()
    }

    private fun words(input: String): List<String> {
        return input.lowercase()
            .replace(PUNCTUATION, " ")
            .replace(A_COUPLE, "couple")
            .split(' ')
            .filter { it.isNotBlank() && it !in FILLER_WORDS }
    }

    private fun parseNumber(word: String): Int? {
        return word.toIntOrNull()?.takeIf { it in 0..1000 } ?: NUMBER_WORDS[word]
    }

    private fun parseUnit(word: String): DateTimeUnit.DateBased? = when (word) {
        "day", "days" -> DateTimeUnit.DAY
        "week", "weeks" -> DateTimeUnit.WEEK
        "month", "months" -> DateTimeUnit.MONTH
        "year", "years" -> DateTimeUnit.YEAR
        else -> null
    }

    private fun parseWeekday(word: String): DayOfWeek? = when (word) {
        "monday", "mon" -> DayOfWeek.MONDAY
        "tuesday", "tue", "tues" -> DayOfWeek.TUESDAY
        "wednesday", "wed" -> DayOfWeek.WEDNESDAY
        "thursday", "thu", "thur", "thurs" -> DayOfWeek.THURSDAY
        "friday", "fri" -> DayOfWeek.FRIDAY
        "saturday", "sat" -> DayOfWeek.SATURDAY
        "sunday", "sun" -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun parseMonth(word: String): Month? = when (word) {
        "january", "jan" -> Month.JANUARY
        "february", "feb" -> Month.FEBRUARY
        "march", "mar" -> Month.MARCH
        "april", "apr" -> Month.APRIL
        "may" -> Month.MAY
        "june", "jun" -> Month.JUNE
        "july", "jul" -> Month.JULY
        "august", "aug" -> Month.AUGUST
        "september", "sep", "sept" -> Month.SEPTEMBER
        "october", "oct" -> Month.OCTOBER
        "november", "nov" -> Month.NOVEMBER
        "december", "dec" -> Month.DECEMBER
        else -> null
    }

    private val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")
    private val ORDINAL = Regex("""(\d{1,4})(st|nd|rd|th)?""")
    private val PUNCTUATION = Regex("""[,.!?;:()"']""")
    private val A_COUPLE = Regex("""\ba couple\b""")
    private val FILLER_WORDS = setOf("on", "the", "due", "at")
    private val NUMBER_WORDS = mapOf(
        "a" to 1, "an" to 1, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11,
        "twelve" to 12, "couple" to 2
    )
}
