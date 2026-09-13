package com.fivepad.app.ui

import java.time.Instant
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DueDatesTest {
    @Test fun pickedDateUsesTheSelectedTimeInTheLocalZone() = withZone("Asia/Jakarta") {
        val selected = Instant.parse("2026-09-15T00:00:00Z").toEpochMilli()
        val due = DueDates.fromPickedDate(selected, 20, 45)
        assertEquals(Instant.parse("2026-09-15T13:45:00Z").toEpochMilli(), due)
        assertEquals(selected, DueDates.toPickedDate(due))
        assertEquals(20, DueDates.localTime(due).hour)
        assertEquals(45, DueDates.localTime(due).minute)
    }

    @Test fun datesWestOfUtcDoNotBecomeThePreviousDay() = withZone("America/Los_Angeles") {
        val selected = Instant.parse("2026-09-15T00:00:00Z").toEpochMilli()
        val due = DueDates.fromPickedDate(selected, 0, 15)
        assertEquals(Instant.parse("2026-09-15T07:15:00Z").toEpochMilli(), due)
        assertEquals(selected, DueDates.toPickedDate(due))
    }

    private fun withZone(id: String, action: () -> Unit) {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            action()
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
