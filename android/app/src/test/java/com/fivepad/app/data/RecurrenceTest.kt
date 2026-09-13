package com.fivepad.app.data

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class RecurrenceTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private fun at(value: String, zone: ZoneId = this.zone) =
        LocalDateTime.parse(value).atZone(zone).toInstant().toEpochMilli()

    @Test fun dailySkipsMissedOccurrencesWithoutChangingTheTime() {
        val anchor = at("2026-09-01T08:45")
        assertEquals(at("2026-09-14T08:45"),
            Recurrence.DAILY.nextDue(anchor, at("2026-09-13T10:00"), zone))
    }

    @Test fun weeklyKeepsTheOriginalWeekday() {
        assertEquals(at("2026-09-22T18:30"),
            Recurrence.WEEKLY.nextDue(at("2026-09-01T18:30"), at("2026-09-20T09:00"), zone))
    }

    @Test fun monthlyReturnsToTheOriginalDayAfterShortMonths() {
        val anchor = at("2026-01-31T09:15")
        val february = Recurrence.MONTHLY.nextDue(anchor, anchor, zone)!!
        assertEquals(at("2026-02-28T09:15"), february)
        assertEquals(at("2026-03-31T09:15"), Recurrence.MONTHLY.nextDue(anchor, february, zone))
    }

    @Test fun monthlyHandlesLeapYears() {
        val anchor = at("2028-01-31T09:15")
        assertEquals(at("2028-02-29T09:15"), Recurrence.MONTHLY.nextDue(anchor, anchor, zone))
    }

    @Test fun dailyKeepsLocalTimeAcrossDaylightSaving() {
        val newYork = ZoneId.of("America/New_York")
        val anchor = at("2026-03-07T09:00", newYork)
        val next = Recurrence.DAILY.nextDue(anchor, anchor, newYork)!!
        assertEquals(at("2026-03-08T09:00", newYork), next)
        assertEquals(23 * 60 * 60 * 1000L, next - anchor)
    }

    @Test fun nonexistentClockTimeDoesNotShiftLaterOccurrences() {
        val newYork = ZoneId.of("America/New_York")
        val anchor = at("2026-03-07T02:30", newYork)
        val next = Recurrence.DAILY.nextDue(anchor, anchor, newYork)!!
        assertEquals(at("2026-03-08T03:30", newYork), next)
        assertEquals(at("2026-03-09T02:30", newYork), Recurrence.DAILY.nextDue(anchor, next, newYork))
    }

    @Test fun nonRepeatingTasksHaveNoNextOccurrence() {
        assertNull(Recurrence.NONE.nextDue(0, 0, zone))
    }

    @Test fun completingOnceMarksAnOrdinaryTaskDone() {
        val todo = Todo(text = "One time", dueAt = 1000)
        val completed = todo.withCompletion(true, 2000)
        assertTrue(completed.done)
        assertEquals(todo.dueAt, completed.dueAt)
    }

    @Test fun completingEarlyAdvancesPastTheCurrentDueDate() {
        val due = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
        val todo = Todo(text = "Repeat", dueAt = due, recurrence = Recurrence.DAILY, recurrenceAnchorAt = due)
        val completed = todo.withCompletion(true, due - 1000)
        assertFalse(completed.done)
        assertTrue(completed.dueAt!! > due)
        assertEquals(todo.id, completed.id)
        assertEquals(due, completed.recurrenceAnchorAt)
    }

    @Test fun uncheckingDoesNotAdvanceSchedule() {
        val todo = Todo(done = true, dueAt = 1000, recurrence = Recurrence.DAILY)
        val restored = todo.withCompletion(false, 2000)
        assertFalse(restored.done)
        assertEquals(todo.dueAt, restored.dueAt)
    }
}
