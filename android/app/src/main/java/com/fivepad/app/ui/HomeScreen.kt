package com.fivepad.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.input.VisualTransformation
import com.fivepad.app.data.Note
import com.fivepad.app.ui.markdown.MarkdownVisualTransformation
import android.app.Activity
import androidx.core.view.WindowCompat
import com.fivepad.app.ui.theme.LocalIsDarkTheme
import com.fivepad.app.ui.theme.LocalOnSlot
import com.fivepad.app.ui.theme.LocalSlotAccents
import com.fivepad.app.ui.theme.LocalSlotSurfaces
import com.fivepad.app.ui.theme.ThemeMode
import com.fivepad.app.ui.theme.Tokens
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(TAB_NOTES) }
    val pager = rememberPagerState(pageCount = { Note.SLOT_COUNT })
    val scope = rememberCoroutineScope()
    val surfaces = LocalSlotSurfaces.current

    // FR-1.4: apa pun yang belum tersimpan harus turun ke disk sebelum aplikasi
    // berhenti, supaya proses yang dimatikan sistem tidak membawa ketikan ikut hilang.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.flushPendingSaves()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notesActive = tab == TAB_NOTES

    // Latar slot selalu cukup pekat untuk teks terang, di kedua mode. Ikon sistem
    // harus mengikuti warna di belakangnya, bukan tema — kalau tidak, jam dan ikon
    // baterai jadi gelap di atas merah dan praktis hilang.
    val view = LocalView.current
    val isDark = LocalIsDarkTheme.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
            if (notesActive) false else !isDark
    }

    // Warna mengikuti posisi geseran secara kontinu, bukan meloncat saat halaman
    // berganti — transisi warna itulah yang membuat perpindahan slot terasa fisik.
    val swipeTint = run {
        val offset = pager.currentPageOffsetFraction
        val neighbour = (pager.currentPage + if (offset >= 0f) 1 else -1)
            .coerceIn(0, surfaces.lastIndex)
        lerp(surfaces[pager.currentPage], surfaces[neighbour], abs(offset).coerceIn(0f, 1f))
    }

    // Pegas melacak target yang bergerak dengan baik, jadi satu animasi ini
    // menangani dua hal sekaligus: geseran antar slot dan pergantian tab.
    val background by animateColorAsState(
        targetValue = if (notesActive) swipeTint else MaterialTheme.colorScheme.background,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "background",
    )

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
            SlotBar(
                activeSlot = pager.currentPage + 1,
                notesActive = notesActive,
                themeMode = themeMode,
                onCycleTheme = onCycleTheme,
                onSelectSlot = { slot ->
                    // Mengetuk titik dari tab Tugas mengembalikan ke catatan —
                    // satu ketukan dari daftar tugas ke slot mana pun.
                    tab = TAB_NOTES
                    scope.launch { pager.animateScrollToPage(slot - 1) }
                },
            )

            Box(Modifier.weight(1f)) {
                if (notesActive) {
                    NotesPane(state = state, vm = vm, pager = pager)
                } else {
                    TodoSection(
                        todos = state.todos,
                        doneCount = state.doneCount,
                        totalCount = state.totalCount,
                        onAdd = vm::addTodo,
                        onToggle = vm::setTodoDone,
                        onEdit = vm::setTodoText,
                        onDelete = vm::deleteTodo,
                        onRestore = vm::restoreTodo,
                    )
                }
            }

            BottomTabs(
                selected = tab,
                todoLabel = if (state.totalCount == 0) {
                    "Tugas"
                } else {
                    "Tugas · ${state.doneCount}/${state.totalCount}"
                },
                onSelect = { tab = it },
            )
        }
    }
}

@Composable
private fun SlotBar(
    activeSlot: Int,
    notesActive: Boolean,
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
    onSelectSlot: (Int) -> Unit,
) {
    val accents = LocalSlotAccents.current
    val onSlot = LocalOnSlot.current

    // Di tab Catatan isinya duduk di atas warna slot; di tab Tugas latarnya netral.
    val ink = if (notesActive) onSlot else MaterialTheme.colorScheme.onSurface

    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = Tokens.space4, end = Tokens.space3, top = Tokens.space1, bottom = Tokens.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Tokens.space1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (slot in 1..Note.SLOT_COUNT) {
                val selected = notesActive && slot == activeSlot
                // Kotak berukuran tetap: titik boleh membesar-mengecil tanpa
                // menggeser tetangganya, sehingga barisnya tidak bergoyang.
                Box(
                    modifier = Modifier
                        .size(Tokens.touchTarget)
                        .clip(CircleShape)
                        .clickable { onSelectSlot(slot) }
                        .semantics {
                            contentDescription =
                                if (selected) "Slot $slot, sedang aktif" else "Slot $slot"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(if (selected) Tokens.dotActive else Tokens.dotInactive)
                            .background(accents[slot - 1], CircleShape)
                            .then(
                                if (selected) {
                                    Modifier.border(2.dp, ink, CircleShape)
                                } else {
                                    // Garis tipis menjaga titik tetap terlihat
                                    // saat warnanya sama dengan latar di belakangnya.
                                    Modifier.border(1.dp, ink.copy(alpha = Tokens.FAINT_ON_SLOT), CircleShape)
                                },
                            ),
                    )
                }
            }
        }

        ThemePill(mode = themeMode, ink = ink, onClick = onCycleTheme)
    }
}

@Composable
private fun ThemePill(mode: ThemeMode, ink: Color, onClick: () -> Unit) {
    val label = when (mode) {
        ThemeMode.SYSTEM -> "Sistem"
        ThemeMode.LIGHT -> "Terang"
        ThemeMode.DARK -> "Gelap"
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(Tokens.radiusPill))
            .border(1.dp, ink.copy(alpha = 0.3f), RoundedCornerShape(Tokens.radiusPill))
            .clickable(onClick = onClick)
            .padding(horizontal = Tokens.space3, vertical = Tokens.space1)
            .semantics { contentDescription = "Tema: $label. Ketuk untuk mengganti." },
    ) {
        Text(
            label,
            color = ink.copy(alpha = 0.85f),
            fontSize = Tokens.captionTextSize,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun NotesPane(
    state: HomeUiState,
    vm: HomeViewModel,
    pager: androidx.compose.foundation.pager.PagerState,
) {
    val onSlot = LocalOnSlot.current
    val active = pager.currentPage + 1
    val label = state.labelFor(active)

    // Transformasi menyimpan hasil terakhirnya, jadi instansnya harus bertahan
    // lintas recomposition — kalau dibuat ulang tiap kali, cache-nya sia-sia.
    val markdown = remember(onSlot) {
        MarkdownVisualTransformation(ink = onSlot, baseSize = Tokens.bodyTextSize)
    }

    Column(Modifier.fillMaxSize()) {
        BasicTextField(
            value = label,
            onValueChange = { vm.onLabelChanged(active, it.take(Note.MAX_LABEL_LENGTH)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = onSlot,
                fontWeight = FontWeight.SemiBold,
            ),
            cursorBrush = SolidColor(onSlot),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.screenPadding, vertical = Tokens.space1),
            decorationBox = { inner ->
                if (label.isEmpty()) {
                    Text(
                        "Beri nama slot ini",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onSlot.copy(alpha = Tokens.MUTED_ON_SLOT),
                    )
                }
                inner()
            },
        )

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val slot = page + 1
            val text = state.draftFor(slot)
            BasicTextField(
                value = text,
                onValueChange = { vm.onBodyChanged(slot, it) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = onSlot,
                    fontSize = Tokens.bodyTextSize,
                    lineHeight = Tokens.bodyLineHeight,
                ),
                cursorBrush = SolidColor(onSlot),
                // Hanya halaman yang sedang tampil yang ditata; menata kelima
                // slot sekaligus berarti empat kali kerja yang tak terlihat.
                visualTransformation = if (page == pager.currentPage) {
                    markdown
                } else {
                    VisualTransformation.None
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Tokens.screenPadding, vertical = Tokens.space2),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text(
                            "Mulai menulis…",
                            color = onSlot.copy(alpha = 0.5f),
                            fontSize = Tokens.bodyTextSize,
                        )
                    }
                    inner()
                },
            )
        }

        EditorFooter(text = state.draftFor(active), onSlot = onSlot)
    }
}

@Composable
private fun EditorFooter(text: String, onSlot: Color) {
    val words = remember(text) { text.split(Regex("\\s+")).count { it.isNotBlank() } }
    val nearLimit = text.length >= Note.BODY_WARN_LENGTH

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Tokens.screenPadding, vertical = Tokens.space2),
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            if (nearLimit) {
                "$words kata · ${text.length} / ${Note.MAX_BODY_LENGTH} karakter"
            } else {
                "$words kata · ${text.length} karakter"
            },
            color = onSlot.copy(alpha = if (nearLimit) 0.95f else Tokens.FAINT_ON_SLOT),
            fontSize = Tokens.captionTextSize,
            fontWeight = if (nearLimit) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun BottomTabs(selected: Int, todoLabel: String, onSelect: (Int) -> Unit) {
    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Tokens.space3, vertical = Tokens.space2),
            horizontalArrangement = Arrangement.spacedBy(Tokens.space2),
        ) {
            TabButton("Catatan", selected == TAB_NOTES, Modifier.weight(1f)) { onSelect(TAB_NOTES) }
            TabButton(todoLabel, selected == TAB_TODOS, Modifier.weight(1f)) { onSelect(TAB_TODOS) }
        }
    }
}

@Composable
private fun TabButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .clip(RoundedCornerShape(Tokens.radiusMd))
            .background(
                if (selected) scheme.onSurface.copy(alpha = 0.07f) else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(vertical = Tokens.space3),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

private const val TAB_NOTES = 0
private const val TAB_TODOS = 1
