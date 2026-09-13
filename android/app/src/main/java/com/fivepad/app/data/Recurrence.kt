package com.fivepad.app.data

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Calendar repeats preserve the local time and the original day of the month. */
enum class Recurrence {
    NONE, DAILY, WEEKLY, MONTHLY;

    fun nextDue(anchorAt: Long, after: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (this == NONE) return null
        val anchor = Instant.ofEpochMilli(anchorAt).atZone(zone).toLocalDateTime()
        val threshold = Instant.ofEpochMilli(after).atZone(zone).toLocalDateTime()
        val unit = when (this) {
            DAILY -> ChronoUnit.DAYS
            WEEKLY -> ChronoUnit.WEEKS
            MONTHLY -> ChronoUnit.MONTHS
            NONE -> return null
        }
        var step = unit.between(anchor, threshold).coerceAtLeast(0)
        while (true) {
            val candidate = anchor.plus(step, unit).atZone(zone).toInstant().toEpochMilli()
            if (candidate > after) return candidate
            step++
        }
    }
}
