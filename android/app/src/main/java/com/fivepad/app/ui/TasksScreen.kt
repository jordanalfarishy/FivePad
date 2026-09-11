package com.fivepad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.fivepad.app.R
import com.fivepad.app.data.Todo
import com.fivepad.app.data.TodoGroup
import com.fivepad.app.ui.theme.Tokens
import kotlinx.coroutines.delay

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
    var renaming by remember { mutableStateOf<TodoGroup?>(null) }
    var showNewGroup by remember { mutableStateOf(false) }

    LaunchedEffect(pendingUndo) {
        if (pendingUndo != null) {
            delay(UNDO_WINDOW_MS)
            pendingUndo = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (state.totalCount == 0 && state.groups.isEmpty()) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(Tokens.space6),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.tasks_empty),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                )
                Text(
                    stringResource(R.string.tasks_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Tokens.space2),
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = Tokens.space2,
                    end = Tokens.space2,
                    top = Tokens.space2,
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(Tokens.space4),
            ) {
                if (state.totalCount > 0) {
                    item {
                        Text(
                            stringResource(R.string.task_progress, state.doneCount, state.totalCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Tokens.space2),
                        )
                    }
                }

                item {
                    TextButton(
                        onClick = { showNewGroup = true },
                        modifier = Modifier.padding(start = Tokens.space1),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            stringResource(R.string.group_new),
                            modifier = Modifier.padding(start = Tokens.space2),
                        )
                    }
                }

                state.sections.forEach { section ->
                    item(key = section.group?.id ?: "ungrouped") {
                        SectionCard(
                            group = section.group,
                            todos = section.todos,
                            onToggle = onToggle,
                            onEditTask = onEditTask,
                            onDeleteTask = {
                                onDeleteTask(it)
                                pendingUndo = it
                            },
                            onRename = { renaming = section.group },
                            onDelete = { section.group?.let { g -> onDeleteGroup(g.id) } },
                        )
                    }
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
private fun SectionCard(
    group: TodoGroup?,
    todos: List<Todo>,
    onToggle: (String, Boolean) -> Unit,
    onEditTask: (String, String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = Tokens.space2, bottom = Tokens.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                (group?.name ?: stringResource(R.string.group_none)).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                // onSurfaceVariant, bukan abu-abu redup: label grup adalah
                // informasi navigasi, jadi harus lolos AA seperti teks lain.
                color = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (group != null) {
                GroupMenu(onRename = onRename, onDelete = onDelete)
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.radiusMd))
                // Batas eksplisit, bukan mengandalkan beda warna isian. Kartu
                // desain hanya berbeda 1,14:1 dari latar — jauh di bawah 3:1
                // yang dituntut untuk batas komponen.
                .border(1.dp, scheme.outline, RoundedCornerShape(Tokens.radiusMd))
                .background(scheme.surfaceContainer),
        ) {
            if (todos.isEmpty()) {
                Text(
                    stringResource(R.string.tasks_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(Tokens.space4),
                )
            } else {
                todos.forEachIndexed { index, todo ->
                    TaskRow(
                        todo = todo,
                        onToggle = { onToggle(todo.id, it) },
                        onEdit = { onEditTask(todo.id, it) },
                        onDelete = { onDeleteTask(todo.id) },
                    )
                    if (index != todos.lastIndex) {
                        HorizontalDivider(color = scheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupMenu(onRename: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .size(Tokens.touchTarget)
                .clip(CircleShape)
                .clickable { open = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.group_menu),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.group_rename)) },
                onClick = { open = false; onRename() },
            )
            DropdownMenuItem(
                text = {
                    Column {
                        Text(stringResource(R.string.group_delete))
                        // Menjelaskan apa yang TIDAK terjadi, karena itulah yang
                        // orang takutkan saat menghapus wadah berisi sesuatu.
                        Text(
                            stringResource(R.string.group_delete_explainer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                onClick = { open = false; onDelete() },
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
                .background(scheme.surfaceContainer)
                .padding(end = Tokens.space3),
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
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (todo.done) scheme.primary else scheme.surfaceContainer)
                        .border(
                            width = if (todo.done) 0.dp else 2.dp,
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
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }

            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= Todo.MAX_TEXT_LENGTH) text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    // Tugas selesai memakai onSurfaceVariant yang lolos AA, bukan
                    // abu-abu 3,68:1 dari desain. Coretan sudah cukup menandai status.
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
