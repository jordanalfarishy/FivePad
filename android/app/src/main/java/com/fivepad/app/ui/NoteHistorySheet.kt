package com.fivepad.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fivepad.app.R
import com.fivepad.app.data.NoteRevision
import java.text.DateFormat
import java.util.Date

@Composable
fun NoteHistorySheet(slot: Int, vm: HomeViewModel, onDismiss: () -> Unit) {
    val history by remember(slot) { vm.history(slot) }.collectAsStateWithLifecycle(emptyList())
    var preview by remember { mutableStateOf<NoteRevision?>(null) }
    var delete by remember { mutableStateOf(false) }
    if (preview == null && !delete) {
        OptionsSheet(
            title = stringResource(R.string.notes_history_title, slot),
            actions = buildList {
                if (history.isEmpty()) add(SheetAction(
                    label = stringResource(R.string.notes_history_empty),
                    description = stringResource(R.string.notes_history_policy),
                    onClick = onDismiss,
                ))
                history.forEach { revision ->
                    add(SheetAction(
                        label = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(revision.createdAt)),
                        description = revision.body.take(100).ifEmpty { "—" },
                        onClick = { preview = revision },
                    ))
                }
                if (history.isNotEmpty()) add(SheetAction(
                    label = stringResource(R.string.notes_delete_history),
                    destructive = true,
                    onClick = { delete = true },
                ))
            },
            onDismiss = { if (preview == null && !delete) onDismiss() },
        )
    }
    preview?.let { revision ->
        NoteRestoreDialog(
            title = stringResource(R.string.notes_restore_title),
            preview = revision.body,
            onDismiss = { preview = null },
            onRestore = { vm.restoreNote(revision); onDismiss() },
        )
    }
    if (delete) AlertDialog(
        onDismissRequest = { delete = false },
        title = { Text(stringResource(R.string.notes_delete_history)) },
        text = { Text(stringResource(R.string.notes_delete_history_warning)) },
        confirmButton = { TextButton(onClick = { vm.deleteHistory(slot); onDismiss() }) { Text(stringResource(R.string.notes_delete)) } },
        dismissButton = { TextButton(onClick = { delete = false }) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
internal fun NoteRestoreDialog(title: String, preview: String, onDismiss: () -> Unit, onRestore: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(stringResource(R.string.notes_restore_warning), Modifier.padding(bottom = 12.dp))
                Text(preview.ifEmpty { "—" }, Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()))
            }
        },
        confirmButton = { TextButton(onClick = onRestore) { Text(stringResource(R.string.notes_restore)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}
