package com.fivepad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.fivepad.app.data.Todo
import kotlinx.coroutines.delay

@Composable
fun TodoSection(
    todos: List<Todo>,
    doneCount: Int,
    totalCount: Int,
    onAdd: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onRestore: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var pendingUndo by remember { mutableStateOf<String?>(null) }

    // Penghapusan bersifat soft delete, jadi menawarkan urungkan selama lima detik
    // tidak memerlukan apa pun selain menyimpan id terakhir yang dihapus.
    LaunchedEffect(pendingUndo) {
        if (pendingUndo != null) {
            delay(UNDO_WINDOW_MS)
            pendingUndo = null
        }
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = draft,
            onValueChange = { if (it.length <= Todo.MAX_TEXT_LENGTH) draft = it },
            placeholder = { Text("Tambah tugas…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            // Enter menyimpan lalu mengosongkan kolom, sehingga tugas berikutnya
            // bisa langsung diketik tanpa menyentuh apa pun (FR-2.2).
            keyboardActions = KeyboardActions(onDone = {
                onAdd(draft)
                draft = ""
            }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )

        if (totalCount > 0) {
            Text(
                "$doneCount dari $totalCount selesai",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        Box(Modifier.fillMaxSize()) {
            if (todos.isEmpty()) {
                Text(
                    "Belum ada tugas.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(todos, key = { it.id }) { todo ->
                        TodoRow(
                            todo = todo,
                            onToggle = { onToggle(todo.id, it) },
                            onEdit = { onEdit(todo.id, it) },
                            onDelete = {
                                onDelete(todo.id)
                                pendingUndo = todo.id
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            pendingUndo?.let { id ->
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Tugas dihapus", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = {
                        onRestore(id)
                        pendingUndo = null
                    }) { Text("Urungkan") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoRow(
    todo: Todo,
    onToggle: (Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val dismiss = rememberSwipeToDismissBoxState()

    // Menolak perubahan lewat confirmValueChange sudah usang; keadaan akhir
    // diamati saja, lalu baris dikembalikan ke posisi semula setelah dihapus.
    LaunchedEffect(dismiss.currentValue) {
        if (dismiss.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
            dismiss.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Hapus", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        var text by remember(todo.id, todo.text) { mutableStateOf(todo.text) }

        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = todo.done, onCheckedChange = onToggle)

            // Diedit di tempat lalu disimpan saat fokus lepas — tidak ada layar
            // atau dialog terpisah untuk mengubah satu baris teks (FR-2.4).
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= Todo.MAX_TEXT_LENGTH) text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .onFocusChanged { focus ->
                        if (!focus.isFocused && text != todo.text) onEdit(text)
                    },
            )
        }
    }
}

private const val UNDO_WINDOW_MS = 5_000L
