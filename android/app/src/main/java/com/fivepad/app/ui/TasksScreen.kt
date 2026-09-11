package com.fivepad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.fivepad.app.R
import com.fivepad.app.data.Todo
import com.fivepad.app.data.TodoGroup
import com.fivepad.app.ui.theme.Tokens
import kotlinx.coroutines.delay

/** Sejajar dengan awal teks tugas, bukan dengan tepi layar. */
private val TEXT_INSET = Tokens.touchTarget

@Composable
fun TasksScreen(
    state: HomeUiState,
    onAddTask: (String, String?) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onEditTask: (String, String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onRestoreTask: (String) -> Unit,
    onAddGroup: (String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var pendingUndo by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var showNewGroup by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<TodoGroup?>(null) }

    LaunchedEffect(pendingUndo) {
        if (pendingUndo != null) {
            delay(UNDO_WINDOW_MS)
            pendingUndo = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            state.sections.forEach { section ->
                item(key = "h-${section.group?.id ?: "none"}") {
                    SectionHeader(
                        group = section.group,
                        onRename = { renaming = section.group },
                        onDelete = { section.group?.let { onDeleteGroup(it.id) } },
                    )
                }

                items(
                    count = section.todos.size,
                    key = { section.todos[it].id },
                ) { index ->
                    val todo = section.todos[index]
                    TaskRow(
                        todo = todo,
                        onToggle = { onToggle(todo.id, it) },
                        onEdit = { onEditTask(todo.id, it) },
                        onDelete = {
                            onDeleteTask(todo.id)
                            pendingUndo = todo.id
                        },
                    )
                    if (index != section.todos.lastIndex) {
                        HorizontalDivider(
                            Modifier.padding(start = TEXT_INSET),
                            color = scheme.outlineVariant,
                        )
                    }
                }

                item(key = "a-${section.group?.id ?: "none"}") {
                    InlineAddTask(
                        groupId = section.group?.id,
                        onAdd = onAddTask,
                    )
                    Spacer(Modifier.height(Tokens.space5))
                }
            }

            item {
                TextButton(
                    onClick = { showNewGroup = true },
                    modifier = Modifier.padding(start = Tokens.space2),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.group_new),
                        modifier = Modifier.padding(start = Tokens.space2),
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Tokens.space4),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.task_add))
        }

        pendingUndo?.let { id ->
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(scheme.surfaceContainerHigh)
                    .padding(horizontal = Tokens.space4, vertical = Tokens.space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.task_deleted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )
                TextButton(onClick = {
                    onRestoreTask(id)
                    pendingUndo = null
                }) { Text(stringResource(R.string.task_undo)) }
            }
        }
    }

    if (showAdd) {
        AddTaskDialog(
            groups = state.groups,
            onDismiss = { showAdd = false },
            onConfirm = { text, groupId ->
                onAddTask(text, groupId)
                showAdd = false
            },
        )
    }

    if (showNewGroup) {
        TextPromptDialog(
            title = stringResource(R.string.group_new),
            initial = "",
            hint = stringResource(R.string.group_name_hint),
            confirmLabel = stringResource(R.string.dialog_add),
            maxLength = TodoGroup.MAX_NAME_LENGTH,
            onDismiss = { showNewGroup = false },
            onConfirm = {
                onAddGroup(it)
                showNewGroup = false
            },
        )
    }

    renaming?.let { group ->
        TextPromptDialog(
            title = stringResource(R.string.group_rename),
            initial = group.name,
            hint = stringResource(R.string.group_name_hint),
            confirmLabel = stringResource(R.string.dialog_save),
            maxLength = TodoGroup.MAX_NAME_LENGTH,
            onDismiss = { renaming = null },
            onConfirm = {
                onRenameGroup(group.id, it)
                renaming = null
            },
        )
    }
}

@Composable
private fun SectionHeader(group: TodoGroup?, onRename: () -> Unit, onDelete: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = Tokens.space4, end = Tokens.space2, top = Tokens.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            group?.name ?: stringResource(R.string.group_none),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (group != null) {
            Box {
                Box(
                    Modifier
                        .size(Tokens.touchTarget)
                        .clip(CircleShape)
                        .clickable { menuOpen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.group_menu),
                        tint = scheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.group_rename)) },
                        onClick = { menuOpen = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(stringResource(R.string.group_delete))
                                // Menyebut apa yang TIDAK terjadi, karena itulah
                                // yang ditakutkan saat menghapus wadah berisi sesuatu.
                                Text(
                                    stringResource(R.string.group_delete_explainer),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
    HorizontalDivider(color = scheme.outlineVariant)
}

/**
 * Penambahan di tempat, mengikuti Todoist: baris "Add task" berubah jadi kolom
 * isian, dan Enter menyimpan lalu **membiarkan kolomnya tetap terbuka** supaya
 * tugas berikutnya bisa langsung diketik. Menutup sendiri saat ditinggalkan kosong.
 */
@Composable
private fun InlineAddTask(groupId: String?, onAdd: (String, String?) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var editing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    if (!editing) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { editing = true }
                .padding(start = Tokens.space4, top = Tokens.space3, bottom = Tokens.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                stringResource(R.string.task_add),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Tokens.space3),
            )
        }
    } else {
        LaunchedEffect(Unit) { focus.requestFocus() }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = Tokens.space4, end = Tokens.space4, top = Tokens.space2, bottom = Tokens.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= Todo.MAX_TEXT_LENGTH) text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onAdd(text, groupId)
                    text = ""
                }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus)
                    .onFocusChanged { if (!it.isFocused && text.isBlank()) editing = false }
                    .padding(vertical = Tokens.space3),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text(
                            stringResource(R.string.task_text_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    inner()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(
    todo: Todo,
    onToggle: (Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dismiss = rememberSwipeToDismissBoxState()

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
                    .background(scheme.errorContainer)
                    .padding(horizontal = Tokens.space5),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(stringResource(R.string.task_delete), color = scheme.onErrorContainer)
            }
        },
    ) {
        var text by remember(todo.id, todo.text) { mutableStateOf(todo.text) }

        Row(
            Modifier
                .fillMaxWidth()
                .background(scheme.background)
                .padding(end = Tokens.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(Tokens.touchTarget)
                    .clip(CircleShape)
                    .clickable { onToggle(!todo.done) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(21.dp)
                        .clip(CircleShape)
                        .background(if (todo.done) scheme.primary else scheme.background)
                        .border(
                            width = if (todo.done) 0.dp else 1.5.dp,
                            color = scheme.onSurfaceVariant,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (todo.done) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= Todo.MAX_TEXT_LENGTH) text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    // Tugas selesai tetap memakai warna yang lolos AA; coretan
                    // sudah cukup menandai status tanpa mengorbankan keterbacaan.
                    color = if (todo.done) scheme.onSurfaceVariant else scheme.onSurface,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = Tokens.space3)
                    .onFocusChanged { focus ->
                        if (!focus.isFocused && text != todo.text) onEdit(text)
                    },
            )
        }
    }
}

private const val UNDO_WINDOW_MS = 5_000L
