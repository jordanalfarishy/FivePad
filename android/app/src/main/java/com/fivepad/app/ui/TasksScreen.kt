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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
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

/** Margin kiri-kanan blok baris, mengikuti inset 8 px di Figma. */
private val BLOCK_INSET = Tokens.space2

/** Jarak dari tepi blok ke awal teks — sejajar untuk baris, pembatas, dan "New Task". */
private val TEXT_INSET = 44.dp

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
            contentPadding = PaddingValues(top = Tokens.space1, bottom = Tokens.space6),
        ) {
            state.sections.forEach { section ->
                item(key = "h-${section.group?.id ?: "none"}") {
                    SectionHeader(
                        group = section.group,
                        onRename = { renaming = section.group },
                        onDelete = { section.group?.let { onDeleteGroup(it.id) } },
                    )
                }

                if (section.todos.isNotEmpty()) {
                    item(key = "b-${section.group?.id ?: "none"}") {
                        TaskBlock(
                            todos = section.todos,
                            onToggle = onToggle,
                            onEdit = onEditTask,
                            onDelete = {
                                onDeleteTask(it)
                                pendingUndo = it
                            },
                        )
                    }
                }

                item(key = "a-${section.group?.id ?: "none"}") {
                    InlineAddRow(
                        label = stringResource(R.string.task_add),
                        labelColor = scheme.onSurface,
                        onCommit = { onAddTask(it, section.group?.id) },
                    )
                    // Pita pemisah antar seksi: latar yang dibiarkan terlihat,
                    // bukan garis — sesuai Rectangle 46/47 di Figma.
                    Spacer(Modifier.height(Tokens.space3))
                }
            }

            item {
                InlineAddRow(
                    label = stringResource(R.string.group_new),
                    labelColor = scheme.primary,
                    onCommit = onAddGroup,
                    maxLength = TodoGroup.MAX_NAME_LENGTH,
                    hintRes = R.string.group_name_hint,
                )
            }
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

    if (showNewGroup) {
        TextPromptDialog(
            title = stringResource(R.string.group_new),
            initial = "",
            hint = stringResource(R.string.group_name_hint),
            confirmLabel = stringResource(R.string.dialog_add),
            maxLength = TodoGroup.MAX_NAME_LENGTH,
            onDismiss = { showNewGroup = false },
            onConfirm = { onAddGroup(it); showNewGroup = false },
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
            onConfirm = { onRenameGroup(group.id, it); renaming = null },
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
            .padding(start = Tokens.space4, end = Tokens.space2)
            .heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            (group?.name ?: stringResource(R.string.group_none)).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            // onSurfaceVariant, bukan abu-abu 3,81:1 dari desain — label seksi
            // adalah informasi navigasi dan harus lolos AA seperti teks lain.
            color = scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        // Kelompok "tanpa grup" tidak punya menu: ia bukan grup, jadi tidak bisa
        // diubah namanya maupun dihapus.
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
                        modifier = Modifier.size(18.dp),
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
}

@Composable
private fun TaskBlock(
    todos: List<Todo>,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = BLOCK_INSET)
            .clip(RoundedCornerShape(Tokens.radiusMd))
            .background(scheme.surfaceContainer),
    ) {
        todos.forEachIndexed { index, todo ->
            TaskRow(
                todo = todo,
                onToggle = { onToggle(todo.id, it) },
                onEdit = { onEdit(todo.id, it) },
                onDelete = { onDelete(todo.id) },
            )
            if (index != todos.lastIndex) {
                HorizontalDivider(
                    Modifier.padding(start = TEXT_INSET),
                    color = scheme.outlineVariant,
                )
            }
        }
    }
}

/**
 * Baris "New Task" / "New Group" yang berubah menjadi kolom isian di tempat.
 *
 * Enter menyimpan lalu **membiarkan kolomnya terbuka**, sehingga beberapa entri
 * bisa diketik beruntun tanpa kembali menekan tombol. Menutup sendiri saat
 * ditinggalkan dalam keadaan kosong.
 */
@Composable
private fun InlineAddRow(
    label: String,
    labelColor: Color,
    onCommit: (String) -> Unit,
    maxLength: Int = Todo.MAX_TEXT_LENGTH,
    hintRes: Int = R.string.task_text_hint,
) {
    val scheme = MaterialTheme.colorScheme
    var editing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    Row(
        Modifier
            .fillMaxWidth()
            .then(if (editing) Modifier else Modifier.clickable { editing = true })
            .padding(start = BLOCK_INSET + 12.dp, end = Tokens.space4)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(Tokens.space3))

        if (!editing) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = labelColor)
        } else {
            LaunchedEffect(Unit) { focus.requestFocus() }
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= maxLength) text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onCommit(text)
                    text = ""
                }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus)
                    .onFocusChanged { if (!it.isFocused && text.isBlank()) editing = false },
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text(
                            stringResource(hintRes),
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
                .background(scheme.surfaceContainer)
                .heightIn(min = 48.dp)
                .padding(end = Tokens.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(TEXT_INSET)
                    .clip(CircleShape)
                    .clickable { onToggle(!todo.done) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (todo.done) scheme.primary else scheme.onSurface.copy(alpha = 0.18f))
                        .border(
                            width = if (todo.done) 0.dp else 1.5.dp,
                            color = if (todo.done) Color.Transparent else scheme.onSurfaceVariant,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (todo.done) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }

            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= Todo.MAX_TEXT_LENGTH) text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
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
