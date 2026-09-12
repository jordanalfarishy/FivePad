package com.fivepad.app.ui

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
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
import com.fivepad.app.ui.markdown.MarkdownAction
import com.fivepad.app.ui.markdown.MarkdownPalette
import com.fivepad.app.ui.markdown.MarkdownVisualTransformation
import com.fivepad.app.ui.markdown.applyMarkdown
import com.fivepad.app.ui.markdown.continueListOnNewline
import com.fivepad.app.ui.markdown.insertLink
import com.fivepad.app.ui.markdown.selectedLink
import com.fivepad.app.ui.markdown.unlink
import com.fivepad.app.ui.markdown.toggleBox
import com.fivepad.app.ui.theme.DOT_INACTIVE_ALPHA
import com.fivepad.app.ui.theme.LocalFivePadColors
import com.fivepad.app.ui.theme.PILL_ALPHA
import com.fivepad.app.ui.theme.QUOTE_FILL_ALPHA
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

    // Pengaturan masuk dari tepi kanan dan keluar ke arah yang sama. Arah itu
    // yang memberi tahu di mana layar sebelumnya berada: ia tidak hilang, ia
    // hanya bergeser ke kiri — dan tombol kembali mengembalikannya dari sana.
    AnimatedContent(
        targetState = showSettings,
        transitionSpec = {
            if (targetState) {
                slideInHorizontally(SLIDE_SPEC) { it } togetherWith
                    slideOutHorizontally(SLIDE_SPEC) { -it / 4 }
            } else {
                slideInHorizontally(SLIDE_SPEC) { -it / 4 } togetherWith
                    slideOutHorizontally(SLIDE_SPEC) { it }
            }
        },
        label = "settings",
    ) { settings ->
        if (settings) {
            BackHandler { showSettings = false }
            SettingsScreen(onBack = { showSettings = false })
        } else {
            MainScreen(
                vm = vm,
                request = request,
                onRequestHandled = onRequestHandled,
                onOpenSettings = { showSettings = true },
            )
        }
    }
}

/**
 * Laju geser antar layar.
 *
 * 280 ms, bukan bawaan Compose yang lebih lambat: layar ini hanya dua tingkat
 * dalam, dan transisi yang berlama-lama pada navigasi sedangkal itu terasa
 * seperti aplikasi yang menunggu, bukan aplikasi yang menjawab.
 */
private val SLIDE_SPEC = tween<IntOffset>(durationMillis = 280, easing = FastOutSlowInEasing)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainScreen(
    vm: HomeViewModel,
    request: LaunchRequest,
    onRequestHandled: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalFivePadColors.current
    val state by vm.uiState.collectAsStateWithLifecycle()
    val clearedSlot by vm.clearedSlot.collectAsStateWithLifecycle()
    val clearedTodos by vm.clearedTodos.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(TAB_NOTES) }
    val pager = rememberPagerState(pageCount = { Note.SLOT_COUNT })
    val scope = rememberCoroutineScope()
    var slotOptions by remember { mutableStateOf<Int?>(null) }
    var confirmClear by remember { mutableStateOf<Int?>(null) }
    var focusSlot by remember { mutableStateOf<Int?>(null) }
    var formatSheet by remember { mutableStateOf(false) }
    var pendingFormat by remember { mutableStateOf<MarkdownAction?>(null) }
    // Pilihan tampilan berlaku untuk kelima slot sekaligus: ia menyangkut cara
    // membaca, bukan isi catatannya, dan tampilan yang berbeda-beda per slot
    // akan terasa seperti aplikasi yang lupa apa yang barusan dipilih.
    // Tampilan Markdown adalah pilihan membaca, bukan isi catatan — satu
    // sakelar untuk kelima slot, dan bertahan melewati rotasi layar.
    var markdownView by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
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
                    .background(colors.bar),
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
                onOpenFormat = { formatSheet = true },
            )

            Box(Modifier.weight(1f)) {
                if (notesActive) {
                    NotesPane(
                        state = state,
                        vm = vm,
                        pager = pager,
                        focusSlot = focusSlot,
                        onFocusHandled = { focusSlot = null },
                        markdownView = markdownView,
                        pendingFormat = pendingFormat,
                        onFormatHandled = { pendingFormat = null },
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
                // Di tab catatan pil mengikuti warna slot yang sedang terbuka;
                // di tab tugas ia memakai aksen aplikasi. Warnanya selalu sama
                // dengan yang dibawa bilah atas, jadi kedua ujung layar
                // menjawab "slot mana" dengan satu warna yang sama.
                notesColour = colors.slotAccents[pager.currentPage],
            )
        }
    }

    if (formatSheet) {
        TextFormatSheet(
            markdownView = markdownView,
            onToggleView = { markdownView = !markdownView },
            onAction = { pendingFormat = it },
            onDismiss = { formatSheet = false },
        )
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
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(ClipData.newPlainText(LABEL_NOTE, body)),
                            )
                        }
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
    onOpenFormat: () -> Unit,
) {
    val colors = LocalFivePadColors.current
    val accents = colors.slotAccents
    val ink = colors.ink

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(Tokens.topBarHeight)
                .background(colors.bar),
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

            // Di tab catatan kotak ini memuat opsi teks; di tab tugas ia kosong
            // dan hanya menyeimbangkan ikon pengaturan di seberangnya.
            if (notesActive) {
                Box(
                    Modifier
                        .width(Tokens.topBarHeight)
                        .fillMaxHeight()
                        .clickable(onClick = onOpenFormat),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_match_case),
                        contentDescription = stringResource(R.string.note_format),
                        tint = ink,
                        modifier = Modifier.size(Tokens.space6),
                    )
                }
            } else {
                Box(Modifier.width(Tokens.topBarHeight))
            }
        }

        // Bilah atas catatan tidak bergaris — pemisahnya ada di bawah baris
        // judul, yang ikut menggulung bersama isinya.
        if (!notesActive) HorizontalDivider(color = colors.hairline)
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
    val colors = LocalFivePadColors.current
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
                        .background(colors.dotRing, CircleShape),
                )
            }
            Box(
                Modifier
                    .size(Tokens.dot)
                    .alpha(if (selected) 1f else DOT_INACTIVE_ALPHA)
                    .background(colour, CircleShape)
                    .border(1.dp, colors.dotStroke, CircleShape),
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
    notesColour: Color,
) {
    val colors = LocalFivePadColors.current

    Column(Modifier.background(colors.bar)) {
        HorizontalDivider(color = colors.hairline)
        Row(
            Modifier
                .fillMaxWidth()
                .height(Tokens.navHeight),
        ) {
            NavItem(
                selected = selected == TAB_NOTES,
                label = stringResource(R.string.tab_notes),
                accent = notesColour,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(TAB_NOTES) },
                icon = R.drawable.ic_notes,
            )
            NavItem(
                selected = selected == TAB_TODOS,
                label = stringResource(R.string.tab_tasks),
                accent = colors.accent,
                badge = if (totalCount > 0) {
                    stringResource(R.string.tab_tasks_count, doneCount, totalCount)
                } else {
                    null
                },
                modifier = Modifier.weight(1f),
                onClick = { onSelect(TAB_TODOS) },
                icon = R.drawable.ic_tasks,
            )
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
    accent: Color,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onClick: () -> Unit,
    icon: Int,
) {
    // Tab terpilih memakai warnanya sendiri, yang tidak terpilih memakai tinta.
    // Pil, ikon, dan angka semuanya satu warna — kalau pilnya beraksen tapi
    // ikonnya tidak, yang terlihat adalah noda warna, bukan penanda terpilih.
    val tint = if (selected) accent else LocalFivePadColors.current.ink

    Box(modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .width(Tokens.pillWidth)
                .height(Tokens.pillHeight)
                .clip(RoundedCornerShape(Tokens.radiusPill))
                .background(if (selected) accent.copy(alpha = PILL_ALPHA) else Color.Transparent)
                .clickable(onClick = onClick)
                // Ikon tanpa teks butuh label yang dibacakan pembaca layar,
                // kalau tidak navigasinya kosong tak bernama bagi mereka.
                .semantics {
                    contentDescription = if (badge == null) label else "$label, $badge"
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(Tokens.space6),
            )
            if (badge != null) {
                Text(
                    badge,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = tint,
                )
            }
        }
    }
}

/** Pemisah baris di teks tampil, sama seperti yang dipakai penata Markdown. */
private const val LINE_BREAK_CHAR = '\u200B'

/** Pita aksen di tepi kiri kutipan dan blok kode — node Figma 17:485. */
private val BLOCK_BAR_WIDTH = 4.dp

/** Kotak centang catatan: lingkaran 20 dp di dalam petak 24 dp, sama seperti daftar tugas. */
private val NOTE_BOX_SLOT = 24.dp
private val NOTE_BOX_SIZE = 20.dp
private val NOTE_CHECK_WIDTH = 10.dp
private val NOTE_CHECK_HEIGHT = 7.dp

/**
 * Seberapa lebar sasaran ketukan kotak centang.
 *
 * Lebih lebar dari kotaknya sendiri, sampai tepat sebelum huruf pertama:
 * lingkaran 20 dp jauh di bawah sasaran sentuh yang wajar, dan ruang kosong di
 * sebelahnya tidak dipakai apa pun.
 */
private val NOTE_BOX_HIT_WIDTH = 32.dp

@Composable
private fun NotesPane(
    state: HomeUiState,
    vm: HomeViewModel,
    pager: PagerState,
    focusSlot: Int?,
    onFocusHandled: () -> Unit,
    markdownView: Boolean,
    pendingFormat: MarkdownAction?,
    onFormatHandled: () -> Unit,
) {
    val colors = LocalFivePadColors.current
    val ink = colors.ink
    val faint = colors.muted
    val accents = colors.slotAccents
    val check = rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_check))
    val keyboard = LocalSoftwareKeyboardController.current

    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
        val slot = page + 1
        val accent = accents[page]
        val text = state.draftFor(slot)
        val scroll = rememberScrollState()
        var layout by remember(slot) { mutableStateOf<TextLayoutResult?>(null) }
        val bodyFocus = remember { FocusRequester() }

        val palette = remember(colors, accent) {
            MarkdownPalette(
                ink = ink,
                accent = accent,
                quoteFill = accent.copy(alpha = QUOTE_FILL_ALPHA),
                codeFill = colors.codeFill,
                link = colors.link,
                checkboxFill = colors.checkboxFill,
                checkboxStroke = colors.checkboxStroke,
                muted = colors.muted,
                baseSize = Tokens.bodyTextSize,
            )
        }
        val markdown = remember(palette) { MarkdownVisualTransformation(palette) }

        // Editor memegang TextFieldValue, bukan String, karena tindakan format
        // butuh tahu apa yang sedang terseleksi. Sumber kebenarannya tetap draft
        // di ViewModel; blok di bawah menyatukan keduanya saat teks berubah dari
        // luar — pengosongan slot, atau pengurungannya.
        var field by remember(slot) { mutableStateOf(TextFieldValue(text)) }
        if (field.text != text) {
            field = TextFieldValue(
                text = text,
                selection = TextRange(field.selection.start.coerceAtMost(text.length)),
            )
        }

        // Alamat tautan ditanyakan lebih dulu; sampai dijawab, catatannya belum
        // disentuh sama sekali.
        var linkLabel by remember(slot) { mutableStateOf<String?>(null) }

        LaunchedEffect(pendingFormat) {
            val action = pendingFormat ?: return@LaunchedEffect
            if (page != pager.currentPage) return@LaunchedEffect
            if (action == MarkdownAction.LINK) {
                onFormatHandled()
                if (selectedLink(field) != null) {
                    // Menekan "Tautan" di atas tautan yang sudah ada berarti
                    // membongkarnya — sama seperti tindakan format lainnya, yang
                    // semuanya membalik.
                    val next = unlink(field)
                    field = next
                    vm.onBodyChanged(slot, next.text)
                    bodyFocus.requestFocus()
                } else {
                    linkLabel = field.text.substring(field.selection.min, field.selection.max)
                }
                return@LaunchedEffect
            }
            val next = applyMarkdown(field, action)
            field = next
            vm.onBodyChanged(slot, next.text)
            onFormatHandled()
            // Papan ketik dibiarkan terbuka: satu tindakan format hampir tidak
            // pernah berdiri sendiri, dan menutup papan ketik tiap kali membuat
            // rangkaian dua tindakan terasa seperti dua perjalanan.
            bodyFocus.requestFocus()
        }

        LaunchedEffect(focusSlot) {
            if (focusSlot == slot) {
                bodyFocus.requestFocus()
                onFocusHandled()
            }
        }

        linkLabel?.let { initial ->
            LinkSheet(
                initialLabel = initial,
                accent = accent,
                onConfirm = { label, url ->
                    val next = insertLink(field, label, url)
                    field = next
                    vm.onBodyChanged(slot, next.text)
                },
                onDismiss = { linkLabel = null },
            )
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val bodyMinHeight = maxHeight - Tokens.titleRowHeight - Tokens.stripeHeight
            val density = LocalDensity.current
            val titleHeightPx = with(density) { Tokens.titleRowHeight.roundToPx() }
            val barWidthPx = with(density) { BLOCK_BAR_WIDTH.toPx() }
            val boxSlotPx = with(density) { NOTE_BOX_SLOT.toPx() }
            val boxSizePx = with(density) { NOTE_BOX_SIZE.toPx() }
            val boxHitPx = with(density) { NOTE_BOX_HIT_WIDTH.toPx() }
            val checkSize = with(density) {
                Size(NOTE_CHECK_WIDTH.toPx(), NOTE_CHECK_HEIGHT.toPx())
            }

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
                    value = field,
                    onValueChange = { changed ->
                        // FR-1.16: Enter di dalam daftar melanjutkan daftarnya.
                        // Diputuskan di sini, bukan lewat KeyboardActions:
                        // hanya di sini kedua nilai — sebelum dan sesudah —
                        // ada bersamaan, dan tanpa nilai sebelumnya tidak ada
                        // cara membedakan Enter dari tempelan yang memuat
                        // baris baru.
                        val next = continueListOnNewline(field, changed) ?: changed
                        field = next
                        vm.onBodyChanged(slot, next.text)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = ink,
                        fontSize = Tokens.bodyTextSize,
                        lineHeight = Tokens.bodyLineHeight,
                        // Tampilan Markdown memakai huruf lebar tetap, seperti
                        // di Figma: yang ditunjukkannya adalah berkas sumber,
                        // dan penanda yang sejajar jauh lebih mudah dibaca.
                        // Dibiarkan kosong di tampilan biasa, bukan diisi
                        // FontFamily.Default. Keluarga huruf yang ditetapkan di
                        // gaya dasar membuat Compose mengabaikan keluarga huruf
                        // pada rentang di dalamnya — dan `kode sebaris` justru
                        // hidup dari itu.
                        fontFamily = if (markdownView) FontFamily.Monospace else null,
                        // Tanpa ini setiap baris menyusut ke tinggi hurufnya
                        // sendiri, karena tiap baris di sini adalah satu
                        // paragraf — dan Compose memangkas sisa ruang di atas
                        // baris pertama dan di bawah baris terakhir tiap
                        // paragraf. Yang dipangkas itu justru jarak antar baris
                        // yang digambar Figma.
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Proportional,
                            trim = LineHeightStyle.Trim.None,
                        ),
                    ),
                    cursorBrush = SolidColor(accent),
                    // Tampilan Markdown mematikan penataannya sama sekali: yang
                    // terlihat persis yang tersimpan, penanda dan semuanya.
                    visualTransformation = if (markdownView) {
                        VisualTransformation.None
                    } else {
                        markdown
                    },
                    onTextLayout = { layout = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(bodyFocus)
                        // Tinggi minimum sepanjang sisa layar, supaya mengetuk
                        // ruang kosong di bawah teks tetap membuka papan ketik.
                        .heightIn(min = bodyMinHeight)
                        .padding(Tokens.screenPadding)
                        // Latar kutipan, blok kode, dan kotak centang. Semuanya
                        // digambar terpisah karena tidak satu pun bisa
                        // dinyatakan sebagai gaya teks: latar yang penuh selebar
                        // kolom, pita di tepi kiri, dan lingkaran 20 dp.
                        .drawBehind {
                            val render = markdown.last.takeIf { !markdownView }
                                ?: return@drawBehind
                            val lr = layout ?: return@drawBehind
                            // Tata letak bisa tertinggal satu frame dari teks.
                            // Menggambar dengan offset dari teks yang lain akan
                            // menaruh blok di baris yang salah, jadi frame itu
                            // dilewati saja.
                            if (lr.layoutInput.text.length != render.annotated.length) {
                                return@drawBehind
                            }

                            for (block in render.blocks) {
                                val first = lr.getLineForOffset(block.start)
                                val last = lr.getLineForOffset(
                                    (block.end - 1).coerceAtLeast(block.start),
                                )
                                val top = lr.getLineTop(first)
                                val height = lr.getLineBottom(last) - top
                                drawRect(
                                    color = if (block.code) palette.codeFill else palette.quoteFill,
                                    topLeft = Offset(0f, top),
                                    size = Size(size.width, height),
                                )
                                drawRect(
                                    color = accent,
                                    topLeft = Offset(0f, top),
                                    size = Size(barWidthPx, height),
                                )
                            }

                            for (box in render.boxes) {
                                val line = lr.getLineForOffset(box.transformed)
                                val centre = Offset(
                                    boxSlotPx / 2f,
                                    (lr.getLineTop(line) + lr.getLineBottom(line)) / 2f,
                                )
                                if (box.checked) {
                                    drawCircle(accent, boxSizePx / 2f, centre)
                                    translate(
                                        left = centre.x - checkSize.width / 2f,
                                        top = centre.y - checkSize.height / 2f,
                                    ) {
                                        with(check) {
                                            draw(
                                                checkSize,
                                                colorFilter = ColorFilter.tint(Color.White),
                                            )
                                        }
                                    }
                                } else {
                                    drawCircle(palette.checkboxFill, boxSizePx / 2f, centre)
                                    drawCircle(
                                        color = palette.checkboxStroke,
                                        radius = boxSizePx / 2f - 0.5f,
                                        center = centre,
                                        style = Stroke(1f),
                                    )
                                }
                            }
                        }
                        // Dua hal yang harus dicegat sebelum kolom teks
                        // sempat menanganinya sendiri.
                        //
                        // FR-1.8: mengetuk kotak centang membalik statusnya
                        // tanpa masuk mode edit.
                        //
                        // Dan mengetuk ruang kosong di kanan sebuah baris harus
                        // menaruh kursor di ujung baris itu. Pemisah baris di
                        // teks tampil adalah karakter selebar nol yang ikut
                        // menjadi bagian barisnya, jadi kolom teks menganggap
                        // ketukan setelahnya sebagai awal baris berikutnya —
                        // kursor mendarat satu baris di bawah jari.
                        .pointerInput(markdownView) {
                            awaitEachGesture {
                                val down = awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial,
                                )
                                if (markdownView) return@awaitEachGesture
                                val render = markdown.last ?: return@awaitEachGesture
                                val lr = layout ?: return@awaitEachGesture
                                if (lr.layoutInput.text.length != render.annotated.length) {
                                    return@awaitEachGesture
                                }

                                val line = lr.getLineForVerticalPosition(down.position.y)
                                val box = if (down.position.x <= boxHitPx) {
                                    render.boxes.firstOrNull {
                                        lr.getLineForOffset(it.transformed) == line
                                    }
                                } else {
                                    null
                                }
                                val pastEnd = box == null &&
                                    down.position.x > lr.getLineRight(line)
                                if (box == null && !pastEnd) return@awaitEachGesture

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
                                if (!released) return@awaitEachGesture

                                if (box != null) {
                                    val toggled = toggleBox(field.text, box)
                                    field = TextFieldValue(
                                        text = toggled,
                                        selection = TextRange(
                                            field.selection.start.coerceAtMost(toggled.length),
                                        ),
                                    )
                                    vm.onBodyChanged(slot, toggled)
                                    return@awaitEachGesture
                                }

                                var end = lr.getLineEnd(line, visibleEnd = false)
                                // Hanya baris sumber yang berakhir pada pemisah;
                                // baris yang patah karena lebar tidak, dan
                                // ujungnya memang sudah tepat.
                                if (render.annotated.text.getOrNull(end - 1) == LINE_BREAK_CHAR) {
                                    end--
                                }
                                field = field.copy(
                                    selection = TextRange(
                                        render.mapping.transformedToOriginal(end),
                                    ),
                                )
                                bodyFocus.requestFocus()
                                keyboard?.show()
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
                        .background(colors.bar)
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
    val colors = LocalFivePadColors.current

    Box(
        Modifier
            .fillMaxWidth()
            .height(Tokens.titleRowHeight)
            .background(colors.bar),
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
                            color = accent.copy(alpha = colors.mutedAlpha),
                        )
                    }
                    inner()
                }
            },
        )
        HorizontalDivider(
            color = colors.hairline,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Label papan klip. Muncul di pratinjau tempel sebagian aplikasi. */
private const val LABEL_NOTE = "FivePad note"

/** Selama ini jendela urungkan berlaku, di layar catatan maupun layar tugas. */
internal const val UNDO_WINDOW_MS = 5_000L

private const val TAB_NOTES = 0
private const val TAB_TODOS = 1
