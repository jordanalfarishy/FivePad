package com.fivepad.app.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fivepad.app.R
import com.fivepad.app.data.Todo
import com.fivepad.app.data.TodoGroup
import com.fivepad.app.ui.theme.CheckboxFill
import com.fivepad.app.ui.theme.CheckboxStroke
import com.fivepad.app.ui.theme.CheckedStroke
import com.fivepad.app.ui.theme.MUTED_ALPHA_TASKS
import com.fivepad.app.ui.theme.TaskSeparator
import com.fivepad.app.ui.theme.Tokens
import kotlinx.coroutines.delay

// Nilai diambil langsung dari Figma (node 3:377).
private val SECTION_PAD_H = 8.dp
private val SECTION_PAD_V = 4.dp
private val ITEM_GAP = 2.dp
private val BLOCK_RADIUS = 12.dp
private val ROW_RADIUS = 4.dp
private val ROW_PAD = 12.dp
private val ROW_GAP = 8.dp
private val SEPARATOR_HEIGHT = 7.dp

/** 8 atas + 20 isi + 8 bawah, sesuai `px-[12px] py-[8px]` di Figma. */
private val ADD_ROW_HEIGHT = 36.dp

/** 12 atas + 24 isi + 12 bawah, sesuai `p-[12px]` di Figma. */
private val TASK_ROW_HEIGHT = 48.dp

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
    onSetDue: (String, Long?) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var pendingUndo by remember { mutableStateOf<String?>(null) }
    var groupOptions by remember { mutableStateOf<TodoGroup?>(null) }
    var renamingGroup by remember { mutableStateOf<TodoGroup?>(null) }
    var renamingTask by remember { mutableStateOf<Todo?>(null) }
    var taskOptions by remember { mutableStateOf<Todo?>(null) }
    var duePicker by remember { mutableStateOf<Todo?>(null) }
    var showDatePicker by remember { mutableStateOf<Todo?>(null) }

    // Izin diminta saat pengguna benar-benar memasang jatuh tempo, bukan di
    // pembukaan pertama: permintaan tanpa konteks lebih sering ditolak, dan
    // aplikasi ini berguna penuh tanpa notifikasi.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* ditolak pun jatuh temponya tetap tersimpan, hanya tanpa pengingat */ }

    LaunchedEffect(pendingUndo) {
        if (pendingUndo != null) {
            delay(UNDO_WINDOW_MS)
            pendingUndo = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Tokens.space6),
        ) {
            state.sections.forEachIndexed { index, section ->
                item(key = "s-${section.group?.id ?: "none"}") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SECTION_PAD_H, vertical = SECTION_PAD_V),
                        verticalArrangement = Arrangement.spacedBy(ITEM_GAP),
                    ) {
                        SectionHeader(
                            group = section.group,
                            onOptions = { groupOptions = section.group },
                        )

                        if (section.todos.isNotEmpty()) {
                            // Sudut luar 12 dp memangkas baris pertama dan terakhir,
                            // sementara tiap baris tetap punya sudut 4 dp-nya sendiri.
                            Column(
                                Modifier.clip(RoundedCornerShape(BLOCK_RADIUS)),
                                verticalArrangement = Arrangement.spacedBy(ITEM_GAP),
                            ) {
                                section.todos.forEach { todo ->
                                    TaskRow(
                                        todo = todo,
                                        onToggle = { onToggle(todo.id, it) },
                                        onOptions = { taskOptions = todo },
                                        onRename = { renamingTask = todo },
                                        onDelete = {
                                            onDeleteTask(todo.id)
                                            pendingUndo = todo.id
                                        },
                                    )
                                }
                            }
                        }

                        InlineAddRow(
                            label = stringResource(R.string.task_add),
                            labelColor = scheme.onSurface,
                            hintRes = R.string.task_text_hint,
                            maxLength = Todo.MAX_TEXT_LENGTH,
                            onCommit = { onAddTask(it, section.group?.id) },
                        )
                    }
                }

                item(key = "sep-$index") { Separator() }
            }

            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SECTION_PAD_H, vertical = SECTION_PAD_V),
                ) {
                    InlineAddRow(
                        label = stringResource(R.string.group_new),
                        labelColor = scheme.primary,
                        hintRes = R.string.group_name_hint,
                        maxLength = TodoGroup.MAX_NAME_LENGTH,
                        onCommit = onAddGroup,
                    )
                }
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

    taskOptions?.let { todo ->
        OptionsSheet(
            title = todo.text,
            actions = buildList {
                add(
                    SheetAction(
                        label = stringResource(R.string.group_rename),
                        onClick = { renamingTask = todo },
                    ),
                )
                add(
                    SheetAction(
                        label = stringResource(
                            if (todo.dueAt == null) R.string.due_set else R.string.due_change,
                        ),
                        description = todo.dueAt?.let { DueDates.format(it) },
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermission.launch(
                                    android.Manifest.permission.POST_NOTIFICATIONS,
                                )
                            }
                            duePicker = todo
                        },
                    ),
                )
                if (todo.dueAt != null) {
                    add(
                        SheetAction(
                            label = stringResource(R.string.due_remove),
                            onClick = { onSetDue(todo.id, null) },
                        ),
                    )
                }
                add(
                    SheetAction(
                        label = stringResource(R.string.task_delete),
                        destructive = true,
                        onClick = { onDeleteTask(todo.id); pendingUndo = todo.id },
                    ),
                )
            },
            onDismiss = { taskOptions = null },
        )
    }

    duePicker?.let { todo ->
        OptionsSheet(
            title = stringResource(R.string.due_set),
            actions = listOf(
                SheetAction(stringResource(R.string.due_today)) {
                    onSetDue(todo.id, DueDates.todayEvening())
                },
                SheetAction(stringResource(R.string.due_tomorrow)) {
                    onSetDue(todo.id, DueDates.tomorrowMorning())
                },
                SheetAction(stringResource(R.string.due_next_week)) {
                    onSetDue(todo.id, DueDates.nextWeek())
                },
                SheetAction(stringResource(R.string.due_pick)) { showDatePicker = todo },
            ),
            onDismiss = { duePicker = null },
        )
    }

    showDatePicker?.let { todo ->
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            onSetDue(todo.id, DueDates.fromPickedDate(it))
                        }
                        showDatePicker = null
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) { Text(stringResource(R.string.dialog_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        ) { DatePicker(state = pickerState) }
    }

    groupOptions?.let { group ->
        OptionsSheet(
            title = group.name,
            actions = listOf(
                SheetAction(
                    label = stringResource(R.string.group_rename),
                    onClick = { renamingGroup = group },
                ),
                SheetAction(
                    label = stringResource(R.string.group_delete),
                    // Menyebut apa yang TIDAK terjadi, karena itulah yang
                    // ditakutkan saat menghapus wadah berisi sesuatu.
                    description = stringResource(R.string.group_delete_explainer),
                    destructive = true,
                    onClick = { onDeleteGroup(group.id) },
                ),
            ),
            onDismiss = { groupOptions = null },
        )
    }

    renamingGroup?.let { group ->
        TextPromptSheet(
            title = stringResource(R.string.group_rename),
            initial = group.name,
            hint = stringResource(R.string.group_name_hint),
            confirmLabel = stringResource(R.string.dialog_save),
            maxLength = TodoGroup.MAX_NAME_LENGTH,
            onDismiss = { renamingGroup = null },
            onConfirm = { onRenameGroup(group.id, it) },
        )
    }

    renamingTask?.let { todo ->
        TextPromptSheet(
            title = stringResource(R.string.task_rename),
            initial = todo.text,
            hint = stringResource(R.string.task_text_hint),
            confirmLabel = stringResource(R.string.dialog_save),
            maxLength = Todo.MAX_TEXT_LENGTH,
            onDismiss = { renamingTask = null },
            onConfirm = { onEditTask(todo.id, it) },
        )
    }
}

@Composable
private fun Separator() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(SEPARATOR_HEIGHT)
            .background(TaskSeparator),
    )
}

@Composable
private fun SectionHeader(group: TodoGroup?, onOptions: () -> Unit) {
    val scheme = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SECTION_PAD_H, vertical = SECTION_PAD_V),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            (group?.name ?: stringResource(R.string.group_none)).uppercase(),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface.copy(alpha = MUTED_ALPHA_TASKS),
            modifier = Modifier.weight(1f),
        )
        // Kelompok tanpa grup bukan grup, jadi tidak bisa diubah nama atau dihapus.
        if (group != null) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                // Kotak tata letak tetap 24 dp; area sentuh dilebarkan ke 48 dp
                // lewat requiredSize, yang menembus batasan induk tanpa ikut
                // menambah tinggi baris.
                Box(
                    Modifier
                        .requiredSize(Tokens.touchTarget)
                        .clip(CircleShape)
                        .clickable(onClick = onOptions),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.group_menu),
                        tint = scheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * Baris "Add task" / "New group" yang berubah jadi kolom isian di tempat.
 *
 * [hadFocus] bukan hiasan: `onFocusChanged` menyala sekali saat komposisi
 * pertama dengan `isFocused = false`, dan tanpa penjaga ini baris langsung
 * menutup dirinya sebelum fokus sempat mendarat — sehingga ketukan pengguna
 * tampak tidak melakukan apa-apa sama sekali.
 */
@Composable
private fun InlineAddRow(
    label: String,
    labelColor: Color,
    hintRes: Int,
    maxLength: Int,
    onCommit: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var editing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var hadFocus by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(editing) {
        if (editing) {
            hadFocus = false
            focus.requestFocus()
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ROW_RADIUS))
            .then(if (editing) Modifier else Modifier.clickable { editing = true })
            .height(ADD_ROW_HEIGHT)
            .padding(horizontal = ROW_PAD),
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Figma menetapkan lebar 24 tapi tidak tingginya — tingginya mengikuti
        // ikon 20 dp. Kotak persegi akan menambah 4 dp tak terlihat di tiap baris.
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.ic_add),
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }

        if (!editing) {
            Text(
                label,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor,
            )
        } else {
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= maxLength) text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = scheme.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                // Enter menyimpan lalu mengosongkan kolom tanpa menutupnya,
                // sehingga beberapa entri bisa diketik beruntun.
                keyboardActions = KeyboardActions(onDone = {
                    onCommit(text)
                    text = ""
                }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus)
                    .onFocusChanged { st ->
                        if (st.isFocused) {
                            hadFocus = true
                        } else if (hadFocus && text.isBlank()) {
                            editing = false
                        }
                    },
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text(
                            stringResource(hintRes),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = scheme.onSurface.copy(alpha = MUTED_ALPHA_TASKS),
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
    onOptions: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dismiss = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismiss.currentValue) {
        when (dismiss.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
                dismiss.snapTo(SwipeToDismissBoxValue.Settled)
            }
            // Ubah nama tidak menghilangkan barisnya, jadi baris harus
            // dikembalikan ke posisi semula setelah sheet dibuka.
            SwipeToDismissBoxValue.StartToEnd -> {
                onRename()
                dismiss.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    SwipeToDismissBox(
        state = dismiss,
        backgroundContent = {
            val renaming = dismiss.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            // Di-clip dengan bentuk yang sama seperti barisnya; tanpa ini latarnya
            // mengintip lewat sudut membulat dan terbaca sebagai garis tipis.
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(ROW_RADIUS))
                    .background(if (renaming) RenameSwipeColor else scheme.errorContainer)
                    .padding(horizontal = Tokens.space5),
                contentAlignment = if (renaming) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Text(
                    stringResource(if (renaming) R.string.task_rename else R.string.task_delete),
                    color = if (renaming) Color.White else scheme.onErrorContainer,
                )
            }
        },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ROW_RADIUS))
                .background(scheme.surfaceContainer)
                .clickable(onClick = onOptions)
                // Minimum, bukan tinggi pasti: baris dengan jatuh tempo perlu
                // ruang untuk barisan keduanya.
                .heightIn(min = TASK_ROW_HEIGHT)
                .padding(horizontal = ROW_PAD, vertical = Tokens.space2),
            horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(done = todo.done, onToggle = { onToggle(!todo.done) })

            Column(Modifier.weight(1f)) {
                Text(
                    todo.text,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = if (todo.done) {
                        scheme.onSurface.copy(alpha = MUTED_ALPHA_TASKS)
                    } else {
                        scheme.onSurface
                    },
                    textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                )

                todo.dueAt?.let { due ->
                    val overdue = !todo.done && DueDates.isOverdue(due)
                    Text(
                        // NFR-8 melarang warna jadi satu-satunya pembeda, jadi
                        // keterlambatan juga dinyatakan dengan kata, bukan merah saja.
                        if (overdue) {
                            "${stringResource(R.string.due_overdue)} · ${DueDates.format(due)}"
                        } else {
                            DueDates.format(due)
                        },
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (overdue) {
                            scheme.error
                        } else {
                            scheme.onSurface.copy(alpha = MUTED_ALPHA_TASKS)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Checkbox(done: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(scheme.primary)
                    .border(1.dp, CheckedStroke, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(width = 10.dp, height = 7.dp),
                )
            }
        } else {
            Box(
                Modifier
                    .size(19.dp)
                    .clip(CircleShape)
                    .background(CheckboxFill)
                    .border(1.dp, CheckboxStroke, CircleShape),
            )
        }
    }
}

private const val UNDO_WINDOW_MS = 5_000L
