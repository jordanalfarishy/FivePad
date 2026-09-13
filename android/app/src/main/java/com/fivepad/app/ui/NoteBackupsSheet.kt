package com.fivepad.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fivepad.app.FivePadApplication
import com.fivepad.app.R
import com.fivepad.app.backup.NoteBackupCodec
import com.fivepad.app.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun NoteBackupsSheet(vm: HomeViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FivePadApplication
    val scope = rememberCoroutineScope()
    var imported by remember { mutableStateOf<List<Note>?>(null) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var browsing by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun failure() { Toast.makeText(context, R.string.notes_file_failed, Toast.LENGTH_LONG).show() }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val notes = vm.exportNotes()
                withContext(Dispatchers.IO) {
                    checkNotNull(context.contentResolver.openOutputStream(uri, "wt")).bufferedWriter().use {
                        it.write(NoteBackupCodec.encode(notes))
                    }
                }
                Toast.makeText(context, R.string.notes_exported, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { failure() } finally { busy = false }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                imported = withContext(Dispatchers.IO) {
                    checkNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8_192)
                        var count = stream.read(buffer)
                        while (count != -1) {
                            require(output.size() + count <= NoteBackupCodec.MAX_BYTES)
                            output.write(buffer, 0, count)
                            count = stream.read(buffer)
                        }
                        NoteBackupCodec.decode(output.toString("UTF-8"))
                    }
                }
            } catch (_: Exception) { failure() } finally { busy = false }
        }
    }

    if (imported == null && !delete) OptionsSheet(
        title = stringResource(if (browsing) R.string.notes_daily_backups else R.string.notes_backups),
        actions = when {
            busy -> listOf(SheetAction(stringResource(R.string.notes_working), onClick = {}))
            browsing -> buildList {
                files.forEach { file ->
                    add(SheetAction(file.nameWithoutExtension, onClick = {
                        scope.launch {
                            try { imported = withContext(Dispatchers.IO) { app.noteBackups.read(file) } }
                            catch (_: Exception) { failure() }
                        }
                    }))
                }
                if (files.isEmpty()) add(SheetAction(stringResource(R.string.notes_backups_empty), onClick = {}))
                if (files.isNotEmpty()) add(SheetAction(stringResource(R.string.notes_delete_backups), destructive = true, onClick = { delete = true }))
                add(SheetAction(stringResource(R.string.settings_back), onClick = { browsing = false }))
            }
            else -> listOf(
                SheetAction(stringResource(R.string.notes_export_all), description = stringResource(R.string.notes_backup_scope), onClick = { export.launch("FivePad-notes.json") }),
                SheetAction(stringResource(R.string.notes_import), onClick = { import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }),
                SheetAction(stringResource(R.string.notes_daily_backups), description = stringResource(R.string.notes_backup_policy), onClick = {
                    scope.launch {
                        files = withContext(Dispatchers.IO) { app.noteBackups.list() }
                        browsing = true
                    }
                }),
            )
        },
        // Nested actions/pickers keep this owner alive until their results arrive.
        onDismiss = {},
        onClose = onDismiss,
    )
    imported?.let { notes ->
        val labels = (1..Note.SLOT_COUNT).map { stringResource(R.string.slot_description, it) }
        NoteRestoreDialog(
            title = stringResource(R.string.notes_import_preview),
            preview = notes.joinToString("\n\n") { note ->
                "${labels[note.slot - 1]} · ${note.label}\n${note.body}"
            },
            onDismiss = { imported = null },
            onRestore = { vm.importNotes(notes); onDismiss() },
        )
    }
    if (delete) AlertDialog(
        onDismissRequest = { delete = false },
        title = { Text(stringResource(R.string.notes_delete_backups)) },
        text = { Text(stringResource(R.string.notes_delete_backups_warning)) },
        confirmButton = { TextButton(onClick = {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { app.noteBackups.deleteAll() }
                    files = emptyList()
                    delete = false
                } catch (_: Exception) { failure() }
            }
        }) { Text(stringResource(R.string.notes_delete)) } },
        dismissButton = { TextButton(onClick = { delete = false }) { Text(stringResource(android.R.string.cancel)) } },
    )
}
