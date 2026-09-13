package com.fivepad.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Converts calendar dates and clock times to local reminder timestamps. */
object DueDates {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    private fun at(date: LocalDate, time: LocalTime): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    /** Material dates are UTC midnight; interpret the chosen clock time locally. */
    fun fromPickedDate(utcMillis: Long, hour: Int, minute: Int): Long {
        val date = Instant.ofEpochMilli(utcMillis).atZone(ZoneId.of("UTC")).toLocalDate()
        return at(date, LocalTime.of(hour, minute))
    }

    fun toPickedDate(millis: Long): Long = Instant.ofEpochMilli(millis).atZone(zone)
        .toLocalDate().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

    fun localTime(millis: Long): LocalTime = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()

    fun isOverdue(millis: Long): Boolean = millis < System.currentTimeMillis()

    fun format(millis: Long, locale: Locale = Locale.getDefault()): String {
        val dt = Instant.ofEpochMilli(millis).atZone(zone)
        val pattern = if (dt.year == LocalDate.now().year) "d MMM, HH:mm" else "d MMM yyyy, HH:mm"
        return dt.format(DateTimeFormatter.ofPattern(pattern, locale))
    }
}
