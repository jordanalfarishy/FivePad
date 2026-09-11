package com.fivepad.app.ui

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fivepad.app.R
import com.fivepad.app.data.Note
import com.fivepad.app.ui.markdown.MarkdownVisualTransformation
import com.fivepad.app.ui.markdown.checkboxAt
import com.fivepad.app.ui.markdown.toggleCheckbox
import com.fivepad.app.ui.theme.AppBar
import com.fivepad.app.ui.theme.DOT_INACTIVE_ALPHA
import com.fivepad.app.ui.theme.DotRing
import com.fivepad.app.ui.theme.DotStroke
import com.fivepad.app.ui.theme.Hairline
import com.fivepad.app.ui.theme.LocalSlotAccents
import com.fivepad.app.ui.theme.PillActive
import com.fivepad.app.ui.theme.Tokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    request: LaunchRequest = LaunchRequest(),
    onRequestHandled: () -> Unit = {},
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    MainScreen(
        vm = vm,
        request = request,
        onRequestHandled = onRequestHandled,
        onOpenSettings = { showSettings = true },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainScreen(
    vm: HomeViewModel,
    request: LaunchRequest,
    onRequestHandled: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val clearedSlot by vm.clearedSlot.collectAsStateWithLifecycle()
    val clearedTodos by vm.clearedTodos.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(TAB_NOTES) }
    val pager = rememberPagerState(pageCount = { Note.SLOT_COUNT })
    val scope = rememberCoroutineScope()
    var slotOptions by remember { mutableStateOf<Int?>(null) }
    var confirmClear by remember { mutableStateOf<Int?>(null) }
    var focusSlot by remember { mutableStateOf<Int?>(null) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.slot_copied)

    // FR-6.2. Dibuka dari peluncur, niatnya kosong dan blok ini tidak berbuat
    // apa-apa; dibuka dari widget atau tautan, slotnya dipilih dan papan ketik
    // menyusul lewat focusSlot.
    LaunchedEffect(request) {
        val slot = request.slot
        if (slot != null) {
            tab = TAB_NOTES
            pager.scrollToPage(slot - 1)
        }
        if (request.focusEditor) {
            tab = TAB_NOTES
            focusSlot = slot ?: pager.currentPage + 1
        }
        if (slot != null || request.focusEditor) onRequestHandled()
    }

    LaunchedEffect(clearedSlot) {
        if (clearedSlot != null) {
            delay(UNDO_WINDOW_MS)
            vm.dismissClearedSlot()
        }
    }

    LaunchedEffect(clearedTodos) {
        if (clearedTodos != null) {
            delay(UNDO_WINDOW_MS)
            vm.dismissClearedTodos()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.flushPendingSaves()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notesActive = tab == TAB_NOTES

    // Kedua tab memakai permukaan yang sama persis. Warna slot tidak lagi
    // mengisi layar; yang membawanya tinggal titik penanda dan nama catatan.
    val background = MaterialTheme.colorScheme.background

    Box(
        Modifier
            .fillMaxSize()
            .background(background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(AppBar),
            )

            TopBar(
                notesActive = notesActive,
                activeSlot = pager.currentPage + 1,
                onOpenSettings = onOpenSettings,
                onSelectSlot = { slot ->
                    tab = TAB_NOTES
                    scope.launch { pager.animateScrollToPage(slot - 1) }
                },
                onSlotOptions = { slot -> slotOptions = slot },
            )

            Box(Modifier.weight(1f)) {
                if (notesActive) {
                    NotesPane(
                        state = state,
                        vm = vm,
                        pager = pager,
                        focusSlot = focusSlot,
                        onFocusHandled = { focusSlot = null },
                    )
                } else {
                    TasksScreen(
                        state = state,
                        onAddTask = vm::addTodo,
                        onToggle = vm::setTodoDone,
                        onEditTask = vm::setTodoText,
                        onDeleteTask = vm::deleteTodo,
                        onRestoreTask = vm::restoreTodo,
                        onAddGroup = vm::addGroup,
                        onRenameGroup = vm::renameGroup,
                        onDeleteGroup = vm::deleteGroup,
                        onSetDue = vm::setTodoDue,
                        onMoveTaskToSection = vm::moveTodoToSection,
                        onMoveGroup = { from, to -> vm.moveGroup(state.groups, from, to) },
                        onClearCompleted = vm::clearCompleted,
                        clearedCount = clearedTodos?.size,
                        onUndoClearCompleted = vm::undoClearCompleted,
                    )
                }
            }

            clearedSlot?.let { cleared ->
                UndoRow(
                    message = stringResource(R.string.slot_cleared, cleared.slot),
                    onUndo = vm::undoClearSlot,
                )
            }

            BottomNav(
                selected = tab,
                doneCount = state.doneCount,
                totalCount = state.totalCount,
                onSelect = { tab = it },
            )
        }
    }

    slotOptions?.let { slot ->
        val label = state.labelFor(slot)
        OptionsSheet(
            title = label.ifEmpty { stringResource(R.string.slot_description, slot) },
            actions = listOf(
                SheetAction(
                    label = stringResource(R.string.slot_copy),
                    icon = painterResource(R.drawable.ic_copy),
                    onClick = {
                        val body = state.draftFor(slot)
                        clipboard.setText(AnnotatedString(body))
                        // Android 13 ke atas sudah menampilkan konfirmasinya
                        // sendiri; menambah toast di sana berarti dua pesan
                        // untuk satu tindakan.
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                        }
                    },
                ),
                SheetAction(
                    label = stringResource(R.string.slot_clear),
                    icon = painterResource(R.drawable.ic_delete),
                    destructive = true,
                    onClick = { confirmClear = slot },
                ),
            ),
            onDismiss = { slotOptions = null },
        )
    }

    confirmClear?.let { slot ->
        // FR-1.11 meminta konfirmasi. Lembar kedua, bukan dialog: tindakannya
        // datang dari lembar pertama, dan memindahkan pertanyaannya ke tengah
        // layar membuat jari harus berpindah jauh untuk membatalkan.
        OptionsSheet(
            title = stringResource(R.string.slot_clear_confirm_title),
            actions = listOf(
                SheetAction(
                    label = stringResource(R.string.slot_clear),
                    icon = painterResource(R.drawable.ic_delete),
                    description = stringResource(R.string.slot_clear_explainer),
                    destructive = true,
                    onClick = { vm.clearSlot(slot) },
                ),
            ),
            onDismiss = { confirmClear = null },
        )
    }
}

/** Baris urungkan bersama, dipakai layar catatan maupun layar tugas. */
@Composable
fun UndoRow(message: String, onUndo: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerHigh)
            .padding(start = Tokens.space4, end = Tokens.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
        TextButton(onClick = onUndo) { Text(stringResource(R.string.task_undo)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopBar(
    notesActive: Boolean,
    activeSlot: Int,
    onOpenSettings: () -> Unit,
    onSelectSlot: (Int) -> Unit,
    onSlotOptions: (Int) -> Unit,
) {
    val accents = LocalSlotAccents.current
    val ink = MaterialTheme.colorScheme.onSurface

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(Tokens.topBarHeight)
                .background(AppBar),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(Tokens.topBarHeight)
                    .fillMaxHeight()
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.settings_open),
                    tint = ink,
                    modifier = Modifier.size(Tokens.space6),
                )
            }

            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (notesActive) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Tokens.dotGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (slot in 1..Note.SLOT_COUNT) {
                            SlotDot(
                                colour = accents[slot - 1],
                                selected = slot == activeSlot,
                                slot = slot,
                                onClick = { onSelectSlot(slot) },
                                onLongClick = { onSlotOptions(slot) },
                            )
                        }
                    }
                } else {
                    Text(
                        stringResource(R.string.tab_tasks),
                        fontSize = 22.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.Bold,
                        color = ink,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Penyeimbang selebar ikon pengaturan, supaya isi tengah benar-benar
            // di tengah. Nanti ditempati avatar akun di M2.
            Box(Modifier.width(Tokens.topBarHeight))
        }

        // Bilah atas catatan tidak bergaris — pemisahnya ada di bawah baris
        // judul, yang ikut menggulung bersama isinya.
        if (!notesActive) HorizontalDivider(color = Hairline)
    }
}

/**
 * Titik penanda slot.
 *
 * Lingkarannya 24 dp sesuai desain, sasaran sentuhnya 40 × 48 dp lewat
 * [requiredSize] yang menembus batasan induk. 40 dp, bukan 48: jarak antar
 * pusat titik hanya 40 dp, jadi sasaran yang lebih lebar akan saling tindih dan
 * membuat titik tetangga mencuri ketukan.
 *
 * Cincin titik aktif digambar DI LUAR lingkaran 24 dp, bukan di dalamnya —
 * di Figma ia sebuah drop shadow putih berjari-jari 2 dp, dan kotak tata
 * letaknya tetap 24 dp. Menggambarnya ke dalam akan memakan warna slot justru
 * pada titik yang paling perlu terlihat.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlotDot(
    colour: Color,
    selected: Boolean,
    slot: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val label = if (selected) {
        stringResource(R.string.slot_description_active, slot)
    } else {
        stringResource(R.string.slot_description, slot)
    }

    Box(Modifier.size(Tokens.dot), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .requiredSize(width = 40.dp, height = Tokens.touchTarget)
                // Tekan-lama membuka tindakan milik slot itu (FR-1.10, FR-1.11).
                // Titiknya ADALAH slotnya, jadi tindakan slot tinggal di sana —
                // bilah atas tidak perlu tombol tambahan, dan tata letak Figma
                // tetap utuh.
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .requiredSize(Tokens.dot + Tokens.dotRing * 2)
                        .background(DotRing, CircleShape),
                )
            }
            Box(
                Modifier
                    .size(Tokens.dot)
                    .alpha(if (selected) 1f else DOT_INACTIVE_ALPHA)
                    .background(colour, CircleShape)
                    .border(1.dp, DotStroke, CircleShape),
            )
        }
    }
}

@Composable
private fun BottomNav(
    selected: Int,
    doneCount: Int,
    totalCount: Int,
    onSelect: (Int) -> Unit,
) {
    Column(Modifier.background(AppBar)) {
        HorizontalDivider(color = Hairline)
        Row(
            Modifier
                .fillMaxWidth()
                .height(Tokens.navHeight),
        ) {
            NavItem(
                selected = selected == TAB_NOTES,
                label = stringResource(R.string.tab_notes),
                modifier = Modifier.weight(1f),
                onClick = { onSelect(TAB_NOTES) },
            ) {
                Icon(
                    painterResource(R.drawable.ic_notes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(Tokens.space6),
                )
            }
            NavItem(
                selected = selected == TAB_TODOS,
                label = stringResource(R.string.tab_tasks),
                badge = if (totalCount > 0) {
                    stringResource(R.string.tab_tasks_count, doneCount, totalCount)
                } else {
                    null
                },
                modifier = Modifier.weight(1f),
                onClick = { onSelect(TAB_TODOS) },
            ) {
                Icon(
                    painterResource(R.drawable.ic_tasks),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(Tokens.space6),
                )
            }
        }
        Spacer(
            Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars),
        )
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onSurface

    Box(modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .width(Tokens.pillWidth)
                .height(Tokens.pillHeight)
                .clip(RoundedCornerShape(Tokens.radiusPill))
                .background(if (selected) PillActive else Color.Transparent)
                .clickable(onClick = onClick)
                // Ikon tanpa teks butuh label yang dibacakan pembaca layar,
                // kalau tidak navigasinya kosong tak bernama bagi mereka.
                .semantics {
                    contentDescription = if (badge == null) label else "$label, $badge"
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        ) {
            icon()
            if (badge != null) {
                Text(
                    badge,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = ink,
                )
            }
        }
    }
}

@Composable
private fun NotesPane(
    state: HomeUiState,
    vm: HomeViewModel,
    pager: PagerState,
    focusSlot: Int?,
    onFocusHandled: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val faint = ink.copy(alpha = MARKER_ALPHA)
    val accents = LocalSlotAccents.current

    val markdown = remember(ink) {
        MarkdownVisualTransformation(ink = ink, baseSize = Tokens.bodyTextSize)
    }

    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
        val slot = page + 1
        val accent = accents[page]
        val text = state.draftFor(slot)
        val scroll = rememberScrollState()
        var layout by remember(slot) { mutableStateOf<TextLayoutResult?>(null) }
        val bodyFocus = remember { FocusRequester() }

        LaunchedEffect(focusSlot) {
            if (focusSlot == slot) {
                bodyFocus.requestFocus()
                onFocusHandled()
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val bodyMinHeight = maxHeight - Tokens.titleRowHeight - Tokens.stripeHeight
            val titleHeightPx = with(LocalDensity.current) { Tokens.titleRowHeight.roundToPx() }

            Column(Modifier.verticalScroll(scroll)) {
                // Judul ikut menggulung bersama isinya, bukan terpaku di bilah
                // atas: di layar ponsel setiap baris yang dipaku memakan ruang
                // menulis, dan nama slot cuma perlu dilihat sesekali.
                SlotTitleField(
                    label = state.labelFor(slot),
                    accent = accent,
                    onChange = { vm.onLabelChanged(slot, it.take(Note.MAX_LABEL_LENGTH)) },
                )

                // Tempat duduk pita saat daftar belum digulir. Pitanya sendiri
                // digambar sebagai lapisan atas supaya bisa menempel di tepi,
                // dan tanpa penyangga ini isi catatan akan tersembunyi di
                // bawahnya sejak baris pertama.
                Spacer(Modifier.height(Tokens.stripeHeight))

                BasicTextField(
                    value = text,
                    onValueChange = { vm.onBodyChanged(slot, it) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = ink,
                        fontSize = Tokens.bodyTextSize,
                        lineHeight = Tokens.bodyLineHeight,
                    ),
                    cursorBrush = SolidColor(accent),
                    visualTransformation = if (page == pager.currentPage) {
                        markdown
                    } else {
                        VisualTransformation.None
                    },
                    onTextLayout = { layout = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(bodyFocus)
                        // Tinggi minimum sepanjang sisa layar, supaya mengetuk
                        // ruang kosong di bawah teks tetap membuka papan ketik.
                        .heightIn(min = bodyMinHeight)
                        .padding(Tokens.screenPadding)
                        // FR-1.8: mengetuk `- [ ]` membalik statusnya tanpa masuk
                        // mode edit. Ketukan dicegat pada pass Initial dan
                        // dikonsumsi hanya bila benar-benar mengenai penanda —
                        // kalau tidak, kolom teks sudah lebih dulu memindahkan
                        // kursor dan membuka papan ketik.
                        .pointerInput(text) {
                            awaitEachGesture {
                                val down = awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial,
                                )
                                val lr = layout ?: return@awaitEachGesture
                                val hit = checkboxAt(text, lr.getOffsetForPosition(down.position))
                                    ?: return@awaitEachGesture

                                down.consume()

                                // waitForUpOrCancellation() memperlakukan pointer
                                // yang sudah dikonsumsi sebagai gestur batal dan
                                // langsung mengembalikan null — jadi angkat-jari
                                // ditunggu manual pada pass Initial yang sama.
                                var released = false
                                while (true) {
                                    val change = awaitPointerEvent(PointerEventPass.Initial)
                                        .changes
                                        .firstOrNull { it.id == down.id } ?: break
                                    change.consume()
                                    if (!change.pressed) {
                                        released = true
                                        break
                                    }
                                }

                                if (released) {
                                    vm.onBodyChanged(slot, toggleCheckbox(text, hit))
                                }
                            }
                        },
                    decorationBox = { inner ->
                        if (text.isEmpty()) {
                            Text(
                                stringResource(R.string.note_placeholder),
                                color = faint,
                                fontSize = Tokens.bodyTextSize,
                                lineHeight = Tokens.bodyLineHeight,
                            )
                        }
                        inner()
                    },
                )
            }

            // Pita menempel di tepi atas begitu judul tergulung habis: judul
            // boleh pergi, penanda slot tidak. Offset-nya mengikuti penyangga
            // di dalam gulungan sampai mentok di nol, jadi keduanya tidak
            // pernah terlihat dua kali.
            SlotStripe(
                slot = slot,
                colour = accent,
                modifier = Modifier.offset {
                    IntOffset(0, (titleHeightPx - scroll.value).coerceAtLeast(0))
                },
            )

            // Penghitung karakter hanya muncul saat ambang batas sudah dekat.
            // Di luar itu ia cuma hiasan yang tidak ada di desain, dan ruang
            // layar lebih berguna untuk menulis.
            if (text.length >= Note.BODY_WARN_LENGTH) {
                Text(
                    stringResource(
                        R.string.editor_counter_limit,
                        text.length,
                        Note.MAX_BODY_LENGTH,
                    ),
                    color = ink,
                    fontSize = Tokens.captionTextSize,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Tokens.space2)
                        .clip(RoundedCornerShape(Tokens.radiusSm))
                        .background(AppBar)
                        .padding(horizontal = Tokens.space2, vertical = Tokens.space1),
                )
            }
        }
    }
}

/**
 * Nama catatan, dalam warna slotnya sendiri.
 *
 * Sejak latar selayar penuh dilepas, baris ini dan deretan titik adalah satu-
 * satunya yang memberi tahu slot mana yang sedang terbuka. Karena itu warnanya
 * penuh saat ada isinya, dan hanya placeholder yang diredupkan ke 40%.
 */
@Composable
private fun SlotTitleField(
    label: String,
    accent: Color,
    onChange: (String) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(Tokens.titleRowHeight)
            .background(AppBar),
        contentAlignment = Alignment.TopCenter,
    ) {
        BasicTextField(
            value = label,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = accent,
                fontSize = 14.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(accent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.screenPadding),
            decorationBox = { inner ->
                // Kotak pembungkus dipakai supaya placeholder ikut rata tengah;
                // textAlign saja hanya mengatur teks yang sudah ada isinya.
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    if (label.isEmpty()) {
                        Text(
                            stringResource(R.string.slot_label_placeholder),
                            fontSize = 14.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = accent.copy(alpha = MARKER_ALPHA),
                        )
                    }
                    inner()
                }
            },
        )
        HorizontalDivider(
            color = Hairline,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Opasitas penanda sintaks Markdown dan teks contoh — 0,40 sesuai Figma.
 * Di atas `#19191B` nilainya 3,8:1 dan gagal AA; dipakai hanya untuk penanda
 * dan placeholder, tidak pernah untuk isi catatan.
 */
private const val MARKER_ALPHA = 0.40f

/** Selama ini jendela urungkan berlaku, di layar catatan maupun layar tugas. */
internal const val UNDO_WINDOW_MS = 5_000L

private const val TAB_NOTES = 0
private const val TAB_TODOS = 1
