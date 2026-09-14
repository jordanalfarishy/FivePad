package com.fivepad.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.fivepad.app.R
import com.fivepad.app.data.Recurrence
import com.fivepad.app.ui.theme.Tokens
import java.util.Date

/** Changes remain local until Save; cancelling leaves the task's reminder untouched. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderEditor(
    initialDue: Long?,
    initialRecurrence: Recurrence,
    onCancel: () -> Unit,
    onConfirm: (Long?, Recurrence) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initialDue?.let(DueDates::toPickedDate),
    )
    var hour by rememberSaveable { mutableStateOf(initialDue?.let { DueDates.localTime(it).hour }) }
    var minute by rememberSaveable { mutableStateOf(initialDue?.let { DueDates.localTime(it).minute }) }
    var recurrence by rememberSaveable { mutableStateOf(initialRecurrence) }
    var timeOpen by rememberSaveable { mutableStateOf(false) }
    val date = dateState.selectedDateMillis
    val selectedHour = hour
    val selectedMinute = minute
    val selectedDue = if (date != null && selectedHour != null && selectedMinute != null) {
        DueDates.fromPickedDate(date, selectedHour, selectedMinute)
    } else null

    Column(
        Modifier.fillMaxWidth().fillMaxHeight(0.9f).navigationBarsPadding().imePadding(),
    ) {
        // Keep Cancel and Save reachable even when the calendar needs scrolling.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Tokens.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.dialog_cancel)) }
            Text(
                stringResource(R.string.reminder_editor_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Button(
                onClick = { selectedDue?.let { onConfirm(it, recurrence) } },
                enabled = selectedDue != null,
            ) { Text(stringResource(R.string.dialog_save)) }
        }
        HorizontalDivider(color = scheme.outlineVariant)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            DatePicker(state = dateState, title = null)
            HorizontalDivider(color = scheme.outlineVariant)
            SheetRow(
                label = if (selectedHour != null && selectedMinute != null) {
                    // Use the device's 12/24-hour format without requiring a date first.
                    val clockDate = DueDates.fromPickedDate(0, selectedHour, selectedMinute)
                    stringResource(R.string.reminder_time_value, DateFormat.getTimeFormat(context).format(Date(clockDate)))
                } else stringResource(R.string.reminder_select_time),
                icon = painterResource(R.drawable.ic_schedule),
                tint = scheme.onSurface,
                onClick = { timeOpen = true },
            )
            Column(Modifier.padding(horizontal = Tokens.space5, vertical = Tokens.space2)) {
                Text(stringResource(R.string.reminder_repeat), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.space2)) {
                    Recurrence.entries.forEach { option ->
                        FilterChip(
                            selected = recurrence == option,
                            onClick = { recurrence = option },
                            label = { Text(stringResource(option.labelRes())) },
                        )
                    }
                }
                if (recurrence != Recurrence.NONE) {
                    Text(
                        stringResource(R.string.repeat_explainer),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Tokens.space3),
                    )
                }
                if (selectedDue == null) {
                    Text(
                        stringResource(R.string.reminder_choose_date_time),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Tokens.space3),
                    )
                }
            }
        }
        if (initialDue != null) {
            HorizontalDivider(color = scheme.outlineVariant)
            TextButton(
                onClick = { onConfirm(null, Recurrence.NONE) },
                modifier = Modifier.fillMaxWidth().padding(vertical = Tokens.space2),
            ) { Text(stringResource(R.string.due_remove)) }
        }
    }

    if (timeOpen) {
        val timeState = rememberTimePickerState(
            initialHour = hour ?: 9,
            initialMinute = minute ?: 0,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { timeOpen = false },
            title = { Text(stringResource(R.string.reminder_time)) },
            text = { TimeInput(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    hour = timeState.hour
                    minute = timeState.minute
                    timeOpen = false
                }) { Text(stringResource(R.string.dialog_save)) }
            },
            dismissButton = {
                TextButton(onClick = { timeOpen = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}
