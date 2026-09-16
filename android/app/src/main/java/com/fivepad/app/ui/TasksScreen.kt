package com.fivepad.app.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.fivepad.app.R
import com.fivepad.app.data.Todo
import com.fivepad.app.data.Recurrence
import com.fivepad.app.data.TodoGroup
import com.fivepad.app.ui.theme.CheckedStroke
import com.fivepad.app.ui.theme.FilledAccent
import com.fivepad.app.ui.theme.LocalFivePadColors
import com.fivepad.app.ui.theme.Tokens
import kotlin.math.abs
import kotlinx.coroutines.delay

// Measurements taken from the 375 dp task-screen reference.
private val SECTION_PAD_H = 12.dp
private val BLOCK_RADIUS = 12.dp
private val ROW_PAD = 13.dp
private val ROW_GAP = 10.dp
private val SECTION_GAP = 16.dp
private val TASK_ROW_PAD_V = 12.25.dp
private val HANDLE_SIZE = 20.dp
private val SECTION_HEADER_HEIGHT = 42.dp
private val EMPTY_SECTION_HEIGHT = 26.dp
private val FOOTER_ACTION_HEIGHT = 50.dp

/** Seberapa dekat ke tepi daftar sebelum daftarnya ikut bergulir saat menyeret. */
private val AUTOSCROLL_EDGE = 72.dp

/** Tombol empty state. Lebih besar dari tingginya, jadi selalu bulat penuh. */
private val EMPTY_BUTTON_RADIUS = 35.dp

@Composable
fun TasksScreen(
    state: HomeUiState,
    onAddTask: (String, String?, Long?, Recurrence) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onEditTask: (String, String, Long?, Recurrence) -> Unit,
    onDeleteTask: (String) -> Unit,
    onRestoreTask: (String) -> Unit,
    onAddGroup: (String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onMoveTaskToSection: (String, String?, Double?, Double?) -> Unit,
    onMoveGroup: (Int, Int) -> Unit,
    onClearCompleted: () -> Unit,
    clearedCount: Int?,
    onUndoClearCompleted: () -> Unit,
) {
    val colors = LocalFivePadColors.current
    val sections = state.sections
    val context = LocalContext.current
    var pendingUndo by remember { mutableStateOf<String?>(null) }
    var groupOptions by remember { mutableStateOf<TodoGroup?>(null) }
    var renamingGroup by remember { mutableStateOf<TodoGroup?>(null) }
    var addingGroup by remember { mutableStateOf(false) }
    var composing by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Todo?>(null) }

    val listState = rememberLazyListState()
    val drag = remember { DragState() }
    val edgePx = with(LocalDensity.current) { AUTOSCROLL_EDGE.toPx() }

    LaunchedEffect(pendingUndo) {
        if (pendingUndo != null) {
            delay(UNDO_WINDOW_MS)
            pendingUndo = null
        }
    }

    // Saat jari menahan baris di dekat tepi, daftarnya bergulir sendiri — tanpa
    // ini grup yang ada di luar layar tidak bisa dijadikan tujuan sama sekali.
    // Yang digulirkan ditambahkan ke offset baris terbang, supaya baris itu tetap
    // berada persis di bawah jari alih-alih ikut hanyut bersama daftar.
    LaunchedEffect(drag.todo?.id) {
        if (drag.todo == null) return@LaunchedEffect
        while (drag.todo != null) {
            withFrameNanos { }
            val viewport = drag.viewport
            val y = drag.pointerY
            val step = when {
                viewport == Rect.Zero -> 0f
                y < viewport.top + edgePx -> -((viewport.top + edgePx - y) / edgePx) * MAX_SCROLL_STEP
                y > viewport.bottom - edgePx ->
                    ((y - (viewport.bottom - edgePx)) / edgePx) * MAX_SCROLL_STEP
                else -> 0f
            }
            if (step != 0f) {
                drag.offsetY += listState.scrollBy(step)
                drag.refreshPointer()
                drag.target = dropTargetFor(sections, drag)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        // if/else, bukan dua anak Box yang bertumpuk. LazyColumn yang
        // fillMaxSize tetap menempati seluruh layar walau isinya nol butir, dan
        // karena ia digambar belakangan ia menelan setiap ketukan yang ditujukan
        // ke tombol di bawahnya — tombolnya terlihat, tapi tidak pernah kena.
        if (sections.isEmpty()) {
            EmptyState(onAddTask = { composing = UNGROUPED_KEY })
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { drag.viewport = it.boundsInRoot() },
                state = listState,
                contentPadding = PaddingValues(bottom = Tokens.space4),
                // Menyeret baris sudah memakai gestur vertikal; tanpa ini
                // daftarnya ikut bergulir dan barisnya seperti lepas dari jari.
                userScrollEnabled = drag.todo == null,
            ) {
                sections.forEach { section ->
                    item(key = "s-${section.key()}") {
                        SectionColumn(
                            section = section,
                            drag = drag,
                            onOptions = { groupOptions = section.group },
                            onToggle = onToggle,
                            onEdit = { editing = it },
                            onDelete = {
                                onDeleteTask(it)
                                pendingUndo = it
                            },
                            onAdd = { composing = section.key() },
                            onDragMove = { todo, coords, local, dy ->
                                drag.onMove(todo, coords, local, dy)
                                drag.target = dropTargetFor(sections, drag)
                            },
                            onDragEnd = { drag.commit(sections, onMoveTaskToSection) },
                        )
                    }

                    item(key = "gap-${section.key()}") { GroupSeparator() }
                }

                item(key = "task-footer") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(colors.bar)
                            .padding(start = SECTION_PAD_H, top = 10.dp, end = SECTION_PAD_H),
                    ) {
                        // Diredupkan, dan tanpa aksen. "New Task" muncul sekali
                        // per bagian dan itulah tindakan yang dicari orang;
                        // "New Group" muncul sekali di kaki daftar. Kalau
                        // keduanya sama-sama beraksen, yang di kaki justru lebih
                        // menarik mata karena ia sendirian.
                        SecondaryActionRow(
                            label = stringResource(R.string.group_new),
                            labelColor = colors.muted,
                            iconColor = colors.muted,
                            onClick = { addingGroup = true },
                        )

                        // FR-2.9. Barisnya hanya ada saat ada yang bisa dibersihkan.
                        if (state.doneCount > 0) {
                            SecondaryActionRow(
                                label = stringResource(R.string.task_clear_done, state.doneCount),
                                labelColor = colors.muted,
                                iconColor = colors.muted,
                                icon = R.drawable.ic_delete,
                                onClick = onClearCompleted,
                            )
                        }
                    }
                }
            }
        }

        Column(Modifier.align(Alignment.BottomStart)) {
            clearedCount?.let { count ->
                UndoRow(
                    message = stringResource(R.string.task_cleared_done, count),
                    onUndo = onUndoClearCompleted,
                )
            }
            pendingUndo?.let { id ->
                UndoRow(
                    message = stringResource(R.string.task_deleted),
                    onUndo = {
                        onRestoreTask(id)
                        pendingUndo = null
                    },
                )
            }
        }
    }

    // Izin notifikasi diminta setelah tugasnya benar-benar tersimpan, bukan
    // saat tanggalnya dipilih. Diminta lebih awal, dialog sistem menutupi
    // lembar yang masih terbuka — dan tombol kembali yang dipakai untuk
    // menyingkirkan dialog itu ikut menutup lembarnya, membuang suntingan yang
    // belum disimpan. Ditolak pun jatuh temponya tetap tersimpan; yang hilang
    // hanya pengingatnya.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    fun askForReminders() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    composing?.let { sectionKey ->
        TaskEditorSheet(
            title = stringResource(R.string.task_new),
            initialText = "",
            initialDue = null,
            confirmLabel = stringResource(R.string.dialog_add),
            // FR-2.2: Enter menyimpan lalu mengosongkan kolom tanpa menutup
            // lembarnya, sehingga beberapa tugas bisa diketik beruntun.
            repeatable = true,
            onDismiss = { composing = null },
            onConfirm = { text, due, recurrence ->
                onAddTask(text, sectionKey.takeIf { it != UNGROUPED_KEY }, due, recurrence)
                if (due != null) askForReminders()
            },
        )
    }

    editing?.let { todo ->
        TaskEditorSheet(
            title = stringResource(R.string.task_edit),
            initialText = todo.text,
            initialDue = todo.dueAt,
            initialRecurrence = todo.recurrence,
            confirmLabel = stringResource(R.string.dialog_save),
            repeatable = false,
            onDismiss = { editing = null },
            onConfirm = { text, due, recurrence ->
                onEditTask(todo.id, text, due, recurrence)
                if (due != null && due != todo.dueAt) askForReminders()
            },
        )
    }

    groupOptions?.let { group ->
        OptionsSheet(
            title = group.name,
            actions = listOf(
                SheetAction(
                    label = stringResource(R.string.group_rename),
                    icon = painterResource(R.drawable.ic_edit),
                    onClick = { renamingGroup = group },
                ),
                SheetAction(
                    label = stringResource(R.string.move_up),
                    icon = painterResource(R.drawable.ic_arrow_upward),
                    onClick = {
                        val i = state.groups.indexOfFirst { it.id == group.id }
                        if (i > 0) onMoveGroup(i, i - 1)
                    },
                ),
                SheetAction(
                    label = stringResource(R.string.move_down),
                    icon = painterResource(R.drawable.ic_arrow_downward),
                    onClick = {
                        val i = state.groups.indexOfFirst { it.id == group.id }
                        if (i >= 0 && i < state.groups.lastIndex) onMoveGroup(i, i + 1)
                    },
                ),
                SheetAction(
                    label = stringResource(R.string.group_delete),
                    icon = painterResource(R.drawable.ic_delete),
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

    if (addingGroup) {
        TextPromptSheet(
            title = stringResource(R.string.group_new),
            initial = "",
            hint = stringResource(R.string.group_name_hint),
            confirmLabel = stringResource(R.string.dialog_add),
            maxLength = TodoGroup.MAX_NAME_LENGTH,
            onDismiss = { addingGroup = false },
            onConfirm = onAddGroup,
        )
    }
}

// ---------------------------------------------------------------- bagian daftar

@Composable
private fun SectionColumn(
    section: TaskSection,
    drag: DragState,
    onOptions: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (Todo) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit,
    onDragMove: (Todo, LayoutCoordinates, Offset, Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val key = section.key()
    val colors = LocalFivePadColors.current
    val indicator = MaterialTheme.colorScheme.onSurface
    val indicatorThickness = with(LocalDensity.current) { 2.dp.toPx() }

    val carrying = section.todos.any { it.id == drag.todo?.id }

    Column(
        Modifier
            .fillMaxWidth()
            // Baris terbang harus melintas di ATAS bagian lain, dan bagian lain
            // punya baris berlatar padat yang akan menelannya. zIndex di sini
            // mengangkat seluruh bagian, bukan barisnya saja, karena tetangga
            // yang harus dilewati adalah butir-butir daftar, bukan baris.
            .zIndex(if (carrying) 1f else 0f)
            .padding(horizontal = SECTION_PAD_H)
            .onGloballyPositioned { drag.sections[key] = it.boundsInRoot() }
            // Garis sisip digambar sebagai lapisan atas, bukan sebagai baris
            // tambahan: menyisipkan elemen nyata akan menggeser semua tetangganya,
            // mengubah batas yang baru saja diukur, dan membuat sasaran jatuhnya
            // berkedip bolak-balik antara dua posisi.
            .drawWithContent {
                drawContent()
                val target = drag.target ?: return@drawWithContent
                if (target.sectionKey != key) return@drawWithContent
                val here = drag.sections[key] ?: return@drawWithContent
                val rows = section.todos.filter { it.id != drag.todo?.id }
                val y = when {
                    rows.isEmpty() -> 0f
                    target.index < rows.size -> drag.rows[rows[target.index].id]?.top
                    else -> drag.rows[rows.last().id]?.bottom
                } ?: return@drawWithContent
                drawRect(
                    color = indicator,
                    topLeft = Offset(0f, y - here.top - indicatorThickness / 2f),
                    size = Size(size.width, indicatorThickness),
                )
            }
            .then(
                if (carrying) Modifier else Modifier.clip(RoundedCornerShape(BLOCK_RADIUS)),
            )
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, colors.fieldBorder, RoundedCornerShape(BLOCK_RADIUS)),
    ) {
        SectionHeader(group = section.group, onOptions = onOptions, onAdd = onAdd)

        if (section.todos.isNotEmpty()) {
            Column {
                section.todos.forEachIndexed { index, todo ->
                    val dragging = drag.todo?.id == todo.id
                    Box(
                        Modifier
                            .onGloballyPositioned { drag.rows[todo.id] = it.boundsInRoot() }
                            // Baris yang diseret harus menimpa tetangganya saat melintas.
                            .zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (dragging) drag.offsetY else 0f
                                alpha = if (dragging) 0.9f else 1f
                            },
                    ) {
                        TaskRow(
                            todo = todo,
                            dragging = dragging,
                            onToggle = { onToggle(todo.id, it) },
                            onEdit = { onEdit(todo) },
                            onDelete = { onDelete(todo.id) },
                            onDragMove = { coords, local, delta ->
                                onDragMove(todo, coords, local, delta)
                            },
                            onDragEnd = onDragEnd,
                        )
                        if (index > 0) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.fieldBorder),
                            )
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().height(EMPTY_SECTION_HEIGHT))
        }

    }
}

/**
 * Layar tugas yang benar-benar kosong — node 11:62.
 *
 * Bukan daftar kosong dengan baris "New Task" di pojok, melainkan satu kalimat
 * dan satu tombol di tengah layar. Pada pemakaian pertama, baris tambah setinggi
 * 48 dp di tepi kiri atas layar yang selebihnya putih tidak terbaca sebagai
 * ajakan; tombol terisi di tengah terbaca.
 */
@Composable
private fun EmptyState(onAddTask: () -> Unit) {
    val colors = LocalFivePadColors.current

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Tokens.space5),
        ) {
            Text(
                stringResource(R.string.tasks_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(EMPTY_BUTTON_RADIUS))
                    .background(FilledAccent)
                    .clickable(onClick = onAddTask)
                    .padding(horizontal = Tokens.space5, vertical = ROW_PAD),
                horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(HANDLE_SIZE),
                )
                Text(
                    stringResource(R.string.task_new),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(group: TodoGroup?, onOptions: () -> Unit, onAdd: () -> Unit) {
    val colors = LocalFivePadColors.current
    val addTaskLabel = stringResource(R.string.task_new)

    Row(
        Modifier
            .fillMaxWidth()
            .height(SECTION_HEADER_HEIGHT)
            .background(colors.fieldBorder)
            .then(
                if (group == null) {
                    Modifier
                        .playfulClick(onClick = onAdd)
                        .semantics { contentDescription = addTaskLabel }
                } else {
                    Modifier
                },
            )
            .padding(start = ROW_PAD, end = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (group != null) {
            Box(
                Modifier
                    .size(Tokens.space6)
                    .playfulClick(onClick = onOptions),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_more_vert),
                    stringResource(R.string.group_menu),
                    tint = colors.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(Modifier.width(10.dp))
        }
        Text(
            group?.name ?: stringResource(R.string.group_none).uppercase(),
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            color = colors.muted,
            modifier = Modifier.weight(1f),
        )
        if (group != null) {
            Box(
                Modifier
                    .size(Tokens.space6)
                    .playfulClick(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_add),
                    stringResource(R.string.task_new),
                    tint = colors.accent,
                    modifier = Modifier.size(HANDLE_SIZE),
                )
            }
        }
    }
}

/**
 * Aksi pendukung di kaki daftar.
 *
 * Tidak memakai kartu maupun garis tepi: dua tindakan ini jarang dipakai dan
 * tidak boleh terlihat setara dengan tugas yang sedang dikerjakan.
 */
@Composable
private fun SecondaryActionRow(
    label: String,
    labelColor: Color,
    onClick: () -> Unit,
    icon: Int = R.drawable.ic_add,
    iconColor: Color? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(FOOTER_ACTION_HEIGHT)
            .playfulClick(onClick = onClick)
            .padding(horizontal = Tokens.space2),
        horizontalArrangement = Arrangement.spacedBy(Tokens.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Untuk "New Task" dan "New Group" ikonnya selalu beraksen dan hanya
        // teksnya yang berbeda warna — keduanya satu aset yang sama di Figma.
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = iconColor ?: LocalFivePadColors.current.accent,
            modifier = Modifier.size(HANDLE_SIZE),
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor,
        )
    }
}

/** A compact boundary between task groups that stays visible while dragging. */
@Composable
private fun GroupSeparator() {
    Box(Modifier.fillMaxWidth().height(SECTION_GAP))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(
    todo: Todo,
    dragging: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDragMove: (LayoutCoordinates, Offset, Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalFivePadColors.current
    val dismiss = rememberSwipeToDismissBoxState()
    var expanded by rememberSaveable(todo.id) { mutableStateOf(false) }
    var canExpand by remember(todo.id, todo.text) { mutableStateOf(false) }

    LaunchedEffect(dismiss.currentValue) {
        when (dismiss.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
                dismiss.snapTo(SwipeToDismissBoxValue.Settled)
            }
            // Menyunting tidak menghilangkan barisnya, jadi baris harus
            // dikembalikan ke posisi semula setelah lembarnya terbuka.
            SwipeToDismissBoxValue.StartToEnd -> {
                onEdit()
                dismiss.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    SwipeToDismissBox(
        state = dismiss,
        gesturesEnabled = !dragging,
        backgroundContent = {
            val editing = dismiss.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            // The section clips this rectangular swipe layer to the shared card.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (editing) colors.accent else scheme.errorContainer)
                    .padding(horizontal = Tokens.space5),
                contentAlignment = if (editing) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Text(
                    stringResource(if (editing) R.string.task_edit else R.string.task_delete),
                    color = if (editing) Color.White else scheme.onErrorContainer,
                )
            }
        },
    ) {
        var rowCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

        Row(
            Modifier
                .fillMaxWidth()
                .background(scheme.surfaceContainer)
                .onGloballyPositioned { rowCoords = it }
                // Seluruh baris adalah sasaran, bukan hanya kotak centang dan
                // pegangannya. Keduanya tetap ada sebagai penanda — yang
                // berubah hanya luas daerah yang menerima.
                //
                // Menyunting tetap lewat geser ke kanan. Ketukan di mana saja
                // yang membuka lembar sunting berarti setiap usaha mencentang
                // yang meleset sedikit justru membuka lembar.
                .pointerInput(todo.id) {
                    detectDragGesturesAfterLongPress(
                        onDrag = { change, amount ->
                            change.consume()
                            rowCoords?.let { onDragMove(it, change.position, amount.y) }
                        },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                    )
                }
                .playfulClick { onToggle(!todo.done) }
                .padding(horizontal = ROW_PAD, vertical = TASK_ROW_PAD_V),
            horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DragHandle(onMove = onDragMove, onEnd = onDragEnd)

            Checkbox(done = todo.done, onToggle = { onToggle(!todo.done) })

            Column(Modifier.weight(1f)) {
                Text(
                    todo.text,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = if (todo.done) {
                        colors.muted
                    } else {
                        colors.ink
                    },
                    textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result ->
                        if (!expanded) canExpand = result.hasVisualOverflow
                    },
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
                        } + if (todo.recurrence != Recurrence.NONE) {
                            " · ${stringResource(todo.recurrence.labelRes())}"
                        } else "",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (overdue) {
                            scheme.error
                        } else {
                            LocalFivePadColors.current.muted
                        },
                    )
                }
            }

            Box(
                Modifier
                    .size(Tokens.space6)
                    .then(
                        if (canExpand || expanded) {
                            Modifier.playfulClick { expanded = !expanded }
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (canExpand || expanded) {
                    Icon(
                        painterResource(R.drawable.ic_chevron_forward),
                        contentDescription = stringResource(
                            if (expanded) R.string.task_collapse else R.string.task_expand,
                        ),
                        tint = colors.muted,
                        modifier = Modifier
                            .size(Tokens.space6)
                            .graphicsLayer { rotationZ = if (expanded) -90f else 90f },
                    )
                }
            }
        }
    }
}

/**
 * Pegangan seret pada tepi kiri baris.
 *
 * Gesturnya langsung, bukan setelah tekan-lama: pegangan yang harus ditunggu
 * dulu tidak terasa seperti pegangan. Karena hanya bagian sempit ini yang
 * menangkap seretan, geser mendatar di sisa baris tetap milik hapus/sunting.
 */
@Composable
private fun DragHandle(
    onMove: (LayoutCoordinates, Offset, Float) -> Unit,
    onEnd: () -> Unit,
) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val label = stringResource(R.string.task_drag)

    Box(
        Modifier
            .width(HANDLE_SIZE)
            .height(Tokens.space6)
            .onGloballyPositioned { coords = it },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .requiredSize(width = 32.dp, height = Tokens.touchTarget)
                .semantics { contentDescription = label }
                .pointerInput(Unit) {
                    // Khusus tegak. detectDragGestures biasa juga menangkap
                    // gerakan mendatar, dan pegangan ini berada persis di tepi
                    // kiri baris — tempat geser-ke-kanan untuk menyunting
                    // dimulai. Dengan versi tegak, geseran mendatar lewat begitu
                    // saja ke SwipeToDismissBox di belakangnya.
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dy ->
                            change.consume()
                            coords?.let { onMove(it, change.position, dy) }
                        },
                        onDragEnd = onEnd,
                        onDragCancel = onEnd,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_drag_indicator),
                contentDescription = null,
                tint = LocalFivePadColors.current.dragHandle,
                modifier = Modifier.size(HANDLE_SIZE),
            )
        }
    }
}

@Composable
private fun Checkbox(done: Boolean, onToggle: () -> Unit) {
    val colors = LocalFivePadColors.current

    Box(
        Modifier
            .size(HANDLE_SIZE)
            .clip(CircleShape)
            .playfulClick(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Box(
                Modifier
                    .size(HANDLE_SIZE)
                    .clip(CircleShape)
                    .background(colors.checkedFill)
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
                    .size(HANDLE_SIZE)
                    .clip(CircleShape)
                    .background(colors.checkboxFill)
                    .border(1.dp, colors.checkboxStroke, CircleShape),
            )
        }
    }
}

// ------------------------------------------------------------------- seret-lepas

private const val UNGROUPED_KEY = "__ungrouped__"

private fun TaskSection.key(): String = group?.id ?: UNGROUPED_KEY

/** Ke mana sebuah baris akan mendarat: bagian mana, di urutan ke berapa. */
private data class DropTarget(val sectionKey: String, val index: Int)

/**
 * Keadaan seretan yang sedang berlangsung.
 *
 * Batas tiap baris dan tiap bagian disimpan dalam koordinat akar, bukan indeks,
 * karena tujuannya bisa berada di bagian lain — dan hanya koordinat yang punya
 * arti lintas bagian.
 */
@Stable
private class DragState {
    var todo by mutableStateOf<Todo?>(null)
    var offsetY by mutableFloatStateOf(0f)
    var pointerY by mutableFloatStateOf(0f)
    var target by mutableStateOf<DropTarget?>(null)
    var viewport by mutableStateOf(Rect.Zero)

    val rows = mutableMapOf<String, Rect>()
    val sections = mutableMapOf<String, Rect>()

    private var handle: LayoutCoordinates? = null
    private var handleLocal = Offset.Zero

    fun onMove(todo: Todo, coords: LayoutCoordinates, local: Offset, dy: Float) {
        if (this.todo?.id != todo.id) {
            this.todo = todo
            offsetY = 0f
        }
        handle = coords
        handleLocal = local
        offsetY += dy
        refreshPointer()
    }

    /**
     * Membaca ulang posisi jari dari koordinat pegangan yang sekarang.
     *
     * Dipanggil juga saat daftar bergulir sendiri: jari tidak bergerak, jadi
     * tidak ada peristiwa seret baru, tapi seluruh baris di bawahnya bergeser.
     */
    fun refreshPointer() {
        val coords = handle ?: return
        if (!coords.isAttached) return
        pointerY = coords.localToRoot(handleLocal).y
    }

    fun commit(
        sections: List<TaskSection>,
        onMove: (String, String?, Double?, Double?) -> Unit,
    ) {
        val moving = todo
        val where = target
        todo = null
        target = null
        offsetY = 0f
        handle = null
        if (moving == null || where == null) return

        val section = sections.firstOrNull { it.key() == where.sectionKey } ?: return
        val neighbours = section.todos.filter { it.id != moving.id }
        val before = neighbours.getOrNull(where.index - 1)?.position
        val after = neighbours.getOrNull(where.index)?.position
        if (section.group?.id == moving.groupId && before == null && after == null) return
        onMove(moving.id, section.group?.id, before, after)
    }
}

/**
 * Bagian dan urutan tempat baris akan jatuh, dari posisi jari saat ini.
 *
 * Bagian ditentukan lebih dulu dan baru kemudian urutannya, bukan sebaliknya:
 * di antara dua bagian ada pita pemisah, dan jari yang berhenti di sana harus
 * tetap punya jawaban alih-alih membatalkan seretan.
 */
private fun dropTargetFor(sections: List<TaskSection>, drag: DragState): DropTarget? {
    val moving = drag.todo ?: return null
    val y = drag.pointerY

    val section = sections.firstOrNull { s ->
        drag.sections[s.key()]?.let { y >= it.top && y <= it.bottom } == true
    } ?: sections.minByOrNull { s ->
        val r = drag.sections[s.key()] ?: return@minByOrNull Float.MAX_VALUE
        minOf(abs(y - r.top), abs(y - r.bottom))
    } ?: return null

    val neighbours = section.todos.filter { it.id != moving.id }
    var index = neighbours.size
    for ((i, candidate) in neighbours.withIndex()) {
        val r = drag.rows[candidate.id] ?: continue
        if (y < r.center.y) {
            index = i
            break
        }
    }
    return DropTarget(section.key(), index)
}

private const val MAX_SCROLL_STEP = 18f
