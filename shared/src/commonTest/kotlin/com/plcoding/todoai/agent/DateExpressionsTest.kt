package com.plcoding.todoai.agent

import com.plcoding.todoai.agent.tools.DateExpressions
import com.plcoding.todoai.agent.tools.DueFilter
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DateExpressionsTest {

    private val monday = LocalDate(2026, 10, 19)
    private val friday = LocalDate(2026, 10, 23)
    private val sunday = LocalDate(2026, 10, 25)

    private fun date(input: String, today: LocalDate = monday) = DateExpressions.parseDate(input, today)
    private fun period(input: String, today: LocalDate = monday) = DateExpressions.parsePeriod(input, today)

    @Test
    fun simpleRelativeDays() {
        assertEquals(monday, date("today"))
        assertEquals(LocalDate(2026, 10, 20), date("Tomorrow"))
        assertEquals(LocalDate(2026, 10, 21), date("the day after tomorrow"))
    }

    @Test
    fun bareWeekdayIsTheNextOneAfterToday() {
        assertEquals(friday, date("friday"))
        assertEquals(friday, date("on Friday"))
        assertEquals(friday, date("fri"))
        assertEquals(LocalDate(2026, 10, 30), date("friday", today = friday))
        assertEquals(LocalDate(2026, 10, 26), date("monday"))
    }

    @Test
    fun thisWeekdayIncludesToday() {
        assertEquals(friday, date("this friday", today = friday))
        assertEquals(friday, date("this friday"))
    }

    @Test
    fun nextWeekdayIsInNextWeek() {
        assertEquals(LocalDate(2026, 10, 30), date("next friday"))
        assertEquals(LocalDate(2026, 10, 30), date("next friday", today = friday))
        assertEquals(LocalDate(2026, 10, 30), date("friday next week"))
        assertEquals(LocalDate(2026, 10, 26), date("next monday", today = sunday))
    }

    @Test
    fun relativeOffsets() {
        assertEquals(LocalDate(2026, 11, 9), date("in 3 weeks"))
        assertEquals(LocalDate(2026, 11, 9), date("in three weeks"))
        assertEquals(LocalDate(2026, 10, 26), date("in a week"))
        assertEquals(LocalDate(2026, 11, 2), date("in a couple weeks"))
        assertEquals(LocalDate(2026, 10, 24), date("in 5 days"))
        assertEquals(LocalDate(2026, 12, 19), date("in 2 months"))
        assertEquals(LocalDate(2027, 10, 19), date("in 1 year"))
        assertEquals(LocalDate(2026, 11, 9), date("3 weeks from now"))
        assertEquals(LocalDate(2026, 10, 26), date("a week from today"))
    }

    @Test
    fun monthOffsetClampsToTheLastDayOfShorterMonths() {
        assertEquals(LocalDate(2027, 2, 28), date("in 1 month", today = LocalDate(2027, 1, 31)))
        assertEquals(LocalDate(2028, 2, 29), date("in a month", today = LocalDate(2028, 1, 31)))
    }

    @Test
    fun endsOfPeriods() {
        assertEquals(sunday, date("end of week"))
        assertEquals(sunday, date("end of the week", today = sunday))
        assertEquals(LocalDate(2026, 11, 1), date("end of next week"))
        assertEquals(LocalDate(2026, 10, 31), date("end of month"))
        assertEquals(LocalDate(2026, 11, 30), date("end of next month"))
        assertEquals(LocalDate(2026, 12, 31), date("end of the year"))
        assertEquals(LocalDate(2026, 10, 26), date("next week"))
        assertEquals(LocalDate(2026, 11, 1), date("next month"))
    }

    @Test
    fun calendarDatesWithoutYearPickTheNextOccurrence() {
        assertEquals(LocalDate(2026, 11, 5), date("November 5"))
        assertEquals(LocalDate(2026, 11, 5), date("nov 5th"))
        assertEquals(LocalDate(2026, 11, 5), date("5th of November"))
        assertEquals(LocalDate(2026, 10, 19), date("october 19"))
        assertEquals(LocalDate(2027, 10, 18), date("october 18"))
        assertEquals(LocalDate(2027, 1, 3), date("jan 3"))
    }

    @Test
    fun calendarDatesWithYear() {
        assertEquals(LocalDate(2027, 3, 1), date("March 1, 2027"))
        assertEquals(LocalDate(2027, 3, 1), date("1 march 2027"))
        assertEquals(LocalDate(2026, 11, 5), date("2026-11-05"))
    }

    @Test
    fun leapDayWithoutYearSkipsToTheNextLeapYear() {
        assertEquals(LocalDate(2028, 2, 29), date("feb 29"))
    }

    @Test
    fun ordinalDayOfMonth() {
        assertEquals(LocalDate(2026, 10, 20), date("the 20th"))
        assertEquals(LocalDate(2026, 11, 5), date("the 5th"))
        assertEquals(LocalDate(2026, 12, 31), date("the 31st", today = LocalDate(2026, 11, 2)))
    }

    @Test
    fun invalidInputReturnsNull() {
        assertNull(date(""))
        assertNull(date("soon"))
        assertNull(date("20"))
        assertNull(date("february 30 2027"))
        assertNull(date("2026-13-01"))
        assertNull(date("next blursday"))
        assertNull(date("in many weeks"))
    }

    @Test
    fun namedPeriods() {
        assertEquals(DueFilter.Between(monday, sunday), period("this week"))
        assertEquals(DueFilter.Between(sunday, sunday), period("this week", today = sunday))
        assertEquals(DueFilter.Between(LocalDate(2026, 10, 26), LocalDate(2026, 11, 1)), period("next week"))
        assertEquals(DueFilter.Between(LocalDate(2026, 10, 26), LocalDate(2026, 11, 1)), period("Next week", today = sunday))
        assertEquals(DueFilter.Between(LocalDate(2026, 11, 1), LocalDate(2026, 11, 30)), period("next month"))
        assertEquals(DueFilter.Between(LocalDate(2026, 10, 24), sunday), period("this weekend"))
        assertEquals(DueFilter.Overdue, period("overdue"))
        assertEquals(DueFilter.NoDueDate, period("no due date"))
        assertEquals(DueFilter.All, period("all"))
    }

    @Test
    fun nextWeekCrossesTheYearBoundary() {
        assertEquals(
            DueFilter.Between(LocalDate(2027, 1, 4), LocalDate(2027, 1, 10)),
            period("next week", today = LocalDate(2026, 12, 30))
        )
    }

    @Test
    fun inWeeksAndMonthsCoversTheWholeWeekOrMonth() {
        assertEquals(DueFilter.Between(LocalDate(2026, 11, 9), LocalDate(2026, 11, 15)), period("in 3 weeks"))
        assertEquals(DueFilter.Between(LocalDate(2026, 12, 1), LocalDate(2026, 12, 31)), period("in 2 months"))
        assertEquals(DueFilter.Between(LocalDate(2026, 10, 24), LocalDate(2026, 10, 24)), period("in 5 days"))
    }

    @Test
    fun rollingRanges() {
        assertEquals(DueFilter.Between(monday, LocalDate(2026, 10, 29)), period("next 10 days"))
        assertEquals(DueFilter.Between(monday, LocalDate(2026, 11, 9)), period("the next 3 weeks"))
        assertEquals(DueFilter.Between(monday, LocalDate(2026, 11, 19)), period("within a month"))
    }

    @Test
    fun openEndedRanges() {
        assertEquals(DueFilter.Between(monday, friday), period("until friday"))
        assertEquals(DueFilter.Between(monday, LocalDate(2026, 11, 5)), period("by November 5"))
        assertEquals(DueFilter.Between(monday, LocalDate(2026, 11, 4)), period("before nov 5"))
        assertEquals(DueFilter.Between(LocalDate(2026, 10, 21), LocalDate(2026, 10, 30)), period("between wednesday and next friday"))
        assertEquals(DueFilter.Between(LocalDate(2026, 10, 21), friday), period("from friday to wednesday"))
    }

    @Test
    fun singleDayPeriods() {
        assertEquals(DueFilter.Between(friday, friday), period("friday"))
        assertEquals(DueFilter.Between(monday, monday), period("today"))
        assertEquals(DueFilter.Between(LocalDate(2026, 11, 5), LocalDate(2026, 11, 5)), period("november 5"))
    }

    @Test
    fun invalidPeriodsReturnNull() {
        assertNull(period("soon"))
        assertNull(period("until whenever"))
        assertNull(period("before today"))
        assertNull(period("between friday and"))
    }
}
