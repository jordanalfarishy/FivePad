package com.fivepad.app.ui

import androidx.annotation.StringRes
import com.fivepad.app.R
import com.fivepad.app.data.Recurrence

@StringRes
fun Recurrence.labelRes(): Int = when (this) {
    Recurrence.NONE -> R.string.repeat_none
    Recurrence.DAILY -> R.string.repeat_daily
    Recurrence.WEEKLY -> R.string.repeat_weekly
    Recurrence.MONTHLY -> R.string.repeat_monthly
}
