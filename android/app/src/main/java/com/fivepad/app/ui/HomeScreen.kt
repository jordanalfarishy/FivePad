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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.fivepad.app.data.Note
import com.fivepad.app.ui.theme.LocalSlotColors
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

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

    Scaffold { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding(),
        ) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Catatan") })
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = {
                        Text(
                            if (state.totalCount == 0) "Tugas"
                            else "Tugas · ${state.doneCount}/${state.totalCount}",
                        )
                    },
                )
            }

            when (tab) {
                0 -> NotesPane(state = state, vm = vm)
                else -> TodoSection(
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
    }
}

@Composable
private fun NotesPane(state: HomeUiState, vm: HomeViewModel) {
    val pager = rememberPagerState(pageCount = { Note.SLOT_COUNT })
    val scope = rememberCoroutineScope()
    val slotColors = LocalSlotColors.current

    // Menggeser halaman dan mengetuk titik harus menghasilkan keadaan yang sama,
    // jadi pager-lah satu-satunya sumber kebenaran untuk slot aktif.
    val active = pager.currentPage + 1

    Column(Modifier.fillMaxSize()) {
        SlotRow(
            activeSlot = active,
            colors = slotColors,
            onSelect = { slot -> scope.launch { pager.animateScrollToPage(slot - 1) } },
        )

        SlotLabelField(
            label = state.labelFor(active),
            accent = slotColors[active - 1],
            onChange = { vm.onLabelChanged(active, it) },
        )

        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            val slot = page + 1
            NoteEditor(
                text = state.draftFor(slot),
                accent = slotColors[page],
                onChange = { vm.onBodyChanged(slot, it) },
            )
        }
    }
}

@Composable
private fun SlotRow(activeSlot: Int, colors: List<Color>, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (slot in 1..Note.SLOT_COUNT) {
            val selected = slot == activeSlot
            Box(
                Modifier
                    .size(if (selected) 22.dp else 16.dp)
                    .background(colors[slot - 1], CircleShape)
                    .then(
                        if (selected) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(slot) }
                    .semantics {
                        contentDescription =
                            if (selected) "Slot $slot, sedang aktif" else "Slot $slot"
                    },
            )
        }
    }
}

@Composable
private fun SlotLabelField(label: String, accent: Color, onChange: (String) -> Unit) {
    BasicTextField(
        value = label,
        onValueChange = { onChange(it.take(Note.MAX_LABEL_LENGTH)) },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        ),
        cursorBrush = SolidColor(accent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        decorationBox = { inner ->
            if (label.isEmpty()) {
                Text(
                    "Beri nama slot ini",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            inner()
        },
    )
}

@Composable
private fun NoteEditor(text: String, accent: Color, onChange: (String) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        BasicTextField(
            value = text,
            onValueChange = onChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            cursorBrush = SolidColor(accent),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text(
                        "Mulai menulis…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                    )
                }
                inner()
            },
        )

        if (text.length >= Note.BODY_WARN_LENGTH) {
            Text(
                "${text.length} / ${Note.MAX_BODY_LENGTH} karakter",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
            )
        }
    }
}
