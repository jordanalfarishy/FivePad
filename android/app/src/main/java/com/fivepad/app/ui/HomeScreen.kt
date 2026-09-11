package com.fivepad.app.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fivepad.app.R
import com.fivepad.app.data.Note
import com.fivepad.app.ui.markdown.MarkdownVisualTransformation
import com.fivepad.app.ui.theme.LocalIsDarkTheme
import com.fivepad.app.ui.theme.LocalOnSlot
import com.fivepad.app.ui.theme.LocalOnSlotSecondary
import com.fivepad.app.ui.theme.LocalSlotAccents
import com.fivepad.app.ui.theme.LocalSlotSurfaces
import com.fivepad.app.ui.theme.ThemeMode
import com.fivepad.app.ui.theme.Tokens
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            themeMode = themeMode,
            onThemeChange = onThemeChange,
            onBack = { showSettings = false },
        )
        return
    }

    MainScreen(vm = vm, onOpenSettings = { showSettings = true })
}

@Composable
private fun MainScreen(vm: HomeViewModel, onOpenSettings: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(TAB_NOTES) }
    val pager = rememberPagerState(pageCount = { Note.SLOT_COUNT })
    val scope = rememberCoroutineScope()
    val surfaces = LocalSlotSurfaces.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.flushPendingSaves()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notesActive = tab == TAB_NOTES
    val isDark = LocalIsDarkTheme.current
    val onSlotInk = LocalOnSlot.current
    val onSlotMuted = LocalOnSlotSecondary.current

    // Latar slot selalu pekat di kedua tema, jadi di tab catatan ikon sistem
    // selalu terang. Di tab tugas barulah ia mengikuti tema.
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
            if (notesActive) false else !isDark
    }

    val swipeTint = run {
        val offset = pager.currentPageOffsetFraction
        val neighbour = (pager.currentPage + if (offset >= 0f) 1 else -1)
            .coerceIn(0, surfaces.lastIndex)
        lerp(surfaces[pager.currentPage], surfaces[neighbour], abs(offset).coerceIn(0f, 1f))
    }

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
            TopBar(
                notesActive = notesActive,
                activeSlot = pager.currentPage + 1,
                onOpenSettings = onOpenSettings,
                onSelectSlot = { slot ->
                    tab = TAB_NOTES
                    scope.launch { pager.animateScrollToPage(slot - 1) }
                },
            )

            Box(Modifier.weight(1f)) {
                if (notesActive) {
                    NotesPane(state = state, vm = vm, pager = pager)
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
                    )
                }
            }

            BottomNav(
                selected = tab,
                doneCount = state.doneCount,
                totalCount = state.totalCount,
                onSelect = { tab = it },
                // Di tab catatan bilah bawah ikut warna slot, jadi seluruh layar
                // membaca sebagai satu bidang warna; di tab tugas kembali netral.
                container = if (notesActive) background else MaterialTheme.colorScheme.surface,
                ink = if (notesActive) onSlotInk else MaterialTheme.colorScheme.onSurface,
                inkMuted = if (notesActive) onSlotMuted else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TopBar(
    notesActive: Boolean,
    activeSlot: Int,
    onOpenSettings: () -> Unit,
    onSelectSlot: (Int) -> Unit,
) {
    val accents = LocalSlotAccents.current
    val onSlot = LocalOnSlot.current
    val ink = if (notesActive) onSlot else MaterialTheme.colorScheme.onSurface

    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Tokens.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(Tokens.touchTarget)
                .clip(CircleShape)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_settings),
                contentDescription = stringResource(R.string.settings_open),
                tint = ink,
            )
        }

        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (notesActive) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Tokens.space1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (slot in 1..Note.SLOT_COUNT) {
                        val selected = slot == activeSlot
                        val label = if (selected) {
                            stringResource(R.string.slot_description_active, slot)
                        } else {
                            stringResource(R.string.slot_description, slot)
                        }
                        Box(
                            Modifier
                                .size(Tokens.touchTarget)
                                .clip(CircleShape)
                                .clickable { onSelectSlot(slot) }
                                .semantics { contentDescription = label },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(if (selected) Tokens.dotActive else Tokens.dotInactive)
                                    .background(accents[slot - 1], CircleShape)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) ink else ink.copy(alpha = 0.35f),
                                        shape = CircleShape,
                                    ),
                            )
                        }
                    }
                }
            } else {
                Text(
                    stringResource(R.string.tab_tasks),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ink,
                )
            }
        }

        // Penyeimbang selebar ikon pengaturan, supaya isi tengah benar-benar
        // di tengah. Nanti ditempati avatar akun di M2.
        Box(Modifier.size(Tokens.touchTarget))
    }
}

@Composable
private fun BottomNav(
    selected: Int,
    doneCount: Int,
    totalCount: Int,
    onSelect: (Int) -> Unit,
    container: Color,
    ink: Color,
    inkMuted: Color,
) {
    Column(Modifier.background(container)) {
        HorizontalDivider(color = ink.copy(alpha = 0.15f))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Tokens.space3, vertical = Tokens.space2),
        ) {
            NavItem(
                selected = selected == TAB_NOTES,
                label = stringResource(R.string.tab_notes),
                ink = ink,
                inkMuted = inkMuted,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(TAB_NOTES) },
            ) {
                Icon(painterResource(R.drawable.ic_notes), contentDescription = null, tint = it)
            }
            NavItem(
                selected = selected == TAB_TODOS,
                label = stringResource(R.string.tab_tasks),
                ink = ink,
                inkMuted = inkMuted,
                badge = if (totalCount > 0) {
                    stringResource(R.string.tab_tasks_count, doneCount, totalCount)
                } else {
                    null
                },
                modifier = Modifier.weight(1f),
                onClick = { onSelect(TAB_TODOS) },
            ) {
                Icon(painterResource(R.drawable.ic_tasks), contentDescription = null, tint = it)
            }
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    label: String,
    ink: Color,
    inkMuted: Color,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    val tint = if (selected) ink else inkMuted

    Box(modifier, contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .clip(RoundedCornerShape(Tokens.radiusPill))
                .background(if (selected) ink.copy(alpha = 0.14f) else Color.Transparent)
                .clickable(onClick = onClick)
                // Ikon tanpa teks butuh label yang dibacakan pembaca layar,
                // kalau tidak navigasinya kosong tak bernama bagi mereka.
                .semantics {
                    contentDescription = if (badge == null) label else "$label, $badge"
                }
                .padding(horizontal = Tokens.space4, vertical = Tokens.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.space2),
        ) {
            icon(tint)
            if (badge != null) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = tint,
                )
            }
        }
    }
}

@Composable
private fun NotesPane(state: HomeUiState, vm: HomeViewModel, pager: PagerState) {
    val onSlot = LocalOnSlot.current
    val onSlotSecondary = LocalOnSlotSecondary.current
    val active = pager.currentPage + 1
    val label = state.labelFor(active)

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
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(onSlot),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.screenPadding, vertical = Tokens.space2),
            decorationBox = { inner ->
                // Kotak pembungkus dipakai supaya placeholder ikut rata tengah;
                // textAlign saja hanya mengatur teks yang sudah ada isinya.
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (label.isEmpty()) {
                        Text(
                            stringResource(R.string.slot_label_placeholder),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = onSlotSecondary,
                        )
                    }
                    inner()
                }
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
                            stringResource(R.string.note_placeholder),
                            color = onSlotSecondary,
                            fontSize = Tokens.bodyTextSize,
                        )
                    }
                    inner()
                },
            )
        }

        EditorFooter(text = state.draftFor(active), ink = onSlotSecondary)
    }
}

@Composable
private fun EditorFooter(text: String, ink: Color) {
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
                stringResource(R.string.editor_counter_limit, words, text.length, Note.MAX_BODY_LENGTH)
            } else {
                stringResource(R.string.editor_counter, words, text.length)
            },
            color = ink,
            fontSize = Tokens.captionTextSize,
            fontWeight = if (nearLimit) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private const val TAB_NOTES = 0
private const val TAB_TODOS = 1
