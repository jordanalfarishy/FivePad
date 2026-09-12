package com.fivepad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.fivepad.app.R
import com.fivepad.app.data.Todo
import com.fivepad.app.ui.markdown.MarkdownAction
import com.fivepad.app.ui.theme.LocalFivePadColors
import com.fivepad.app.ui.theme.Tokens

/** Satu tindakan di dalam [OptionsSheet]. */
data class SheetAction(
    val label: String,
    val icon: Painter? = null,
    /** Baris penjelas di bawah label — dipakai untuk menerangkan akibat yang tidak jelas. */
    val description: String? = null,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Satu baris tindakan di dalam sebuah lembar.
 *
 * Ikonnya wajib, bukan hiasan: dalam daftar tindakan yang semuanya berupa teks,
 * mata harus membaca tiap baris untuk menemukan yang dicari. Bentuk ikon dikenali
 * lebih dulu daripada kata, dan "hapus" yang salah ketuk tidak bisa ditarik kembali.
 */
@Composable
private fun SheetRow(
    label: String,
    icon: Painter?,
    tint: Color,
    description: String? = null,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Tokens.space5, vertical = Tokens.space3),
        horizontalArrangement = Arrangement.spacedBy(Tokens.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(Tokens.space5),
            )
        }
        Column(Modifier.padding(vertical = Tokens.space1)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = Tokens.space5,
            end = Tokens.space5,
            bottom = Tokens.space3,
        ),
    )
}

/**
 * Daftar tindakan sebagai bottom sheet, menggantikan menu tarik-turun.
 *
 * Di layar ponsel, menu yang muncul menempel pada tombolnya mendarat di ujung
 * jangkauan ibu jari dan target sentuhnya kecil. Sheet selalu muncul dari tepi
 * bawah, punya baris setinggi 56 dp, dan bisa ditutup dengan menyeret ke bawah.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsSheet(
    title: String,
    actions: List<SheetAction>,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = Tokens.space4),
        ) {
            SheetTitle(title)

            actions.forEach { action ->
                SheetRow(
                    label = action.label,
                    icon = action.icon,
                    tint = if (action.destructive) scheme.error else scheme.onSurface,
                    description = action.description,
                    onClick = {
                        action.onClick()
                        onDismiss()
                    },
                )
            }
        }
    }
}

/**
 * Kolom isian satu baris sebagai bottom sheet.
 *
 * Dipakai menggantikan dialog di tengah layar: dengan papan ketik terbuka,
 * sheet dan kolomnya tetap berdekatan di paruh bawah, sementara dialog tengah
 * terdorong ke atas dan menjauh dari tempat jari berada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextPromptSheet(
    title: String,
    initial: String,
    hint: String,
    confirmLabel: String,
    maxLength: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState()
    var value by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Tokens.space5)
                .padding(bottom = Tokens.space4),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Tokens.space3),
            )

            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= maxLength) value = it },
                placeholder = { Text(hint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (value.isNotBlank()) {
                        onConfirm(value)
                        onDismiss()
                    }
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = Tokens.space2),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel), color = scheme.onSurfaceVariant)
                }
                TextButton(
                    onClick = {
                        onConfirm(value)
                        onDismiss()
                    },
                    enabled = value.isNotBlank(),
                ) { Text(confirmLabel) }
            }
        }
    }
}

/**
 * Menu penyuntingan teks catatan — ikon `match_case` di bilah atas.
 *
 * Petak, bukan daftar. Dua belas tindakan sebagai baris bertumpuk menghasilkan
 * lembar setinggi hampir satu layar, dan lembar setinggi itu menutupi justru
 * catatan yang sedang diformat — hasil tiap ketukan baru terlihat setelah
 * lembarnya disingkirkan. Sebagai petak empat kolom semuanya muat tanpa
 * digulung, dan dua pertiga layar tetap terlihat di atasnya.
 *
 * Tiap petak memuat penanda Markdown yang akan ditulis tindakan itu. Itu bukan
 * hiasan: sakelar di baris paling atas bisa memperlihatkan teks mentahnya kapan
 * saja, jadi cepat atau lambat penandanya akan terlihat. Lebih baik ia dikenali
 * di tempat ia dipakai.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFormatSheet(
    markdownView: Boolean,
    onToggleView: () -> Unit,
    onAction: (MarkdownAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalFivePadColors.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = Tokens.space4),
        ) {
            SheetTitle(stringResource(R.string.format_title))

            // Sakelar tampilan berdiri sendiri sebagai baris penuh: ia mengubah
            // cara seluruh catatan dibaca, bukan memformat sepotong teks.
            SheetRow(
                label = stringResource(
                    if (markdownView) R.string.format_view_normal else R.string.format_view_markdown,
                ),
                icon = painterResource(
                    if (markdownView) R.drawable.ic_match_case else R.drawable.ic_code,
                ),
                tint = scheme.onSurface,
                onClick = {
                    onToggleView()
                    onDismiss()
                },
            )

            HorizontalDivider(color = colors.hairline)

            Column(
                Modifier.padding(horizontal = Tokens.space3, vertical = Tokens.space3),
                verticalArrangement = Arrangement.spacedBy(Tokens.space2),
            ) {
                MarkdownAction.entries.chunked(FORMAT_COLUMNS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Tokens.space2)) {
                        row.forEach { action ->
                            FormatTile(
                                action = action,
                                modifier = Modifier.weight(1f),
                                // Menutup setelah satu tindakan. Lembar yang
                                // tetap terbuka menutupi teks yang barusan
                                // diubahnya, dan menilai format tanpa melihatnya
                                // mustahil.
                                onClick = {
                                    onAction(action)
                                    onDismiss()
                                },
                            )
                        }
                        // Baris terakhir yang tidak penuh tetap sejajar dengan
                        // baris di atasnya, bukan melebar mengisi sisanya.
                        repeat(FORMAT_COLUMNS - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/** Empat kolom: dua belas tindakan jatuh tepat menjadi tiga baris penuh. */
private const val FORMAT_COLUMNS = 4

@Composable
private fun FormatTile(action: MarkdownAction, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalFivePadColors.current

    Column(
        modifier
            .clip(RoundedCornerShape(Tokens.space2))
            .background(colors.ink.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(vertical = Tokens.space2, horizontal = Tokens.space1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            action.syntax,
            fontFamily = FontFamily.Monospace,
            fontSize = Tokens.bodyTextSize,
            lineHeight = Tokens.bodyLineHeight,
            color = scheme.onSurface,
            maxLines = 1,
        )
        Text(
            stringResource(action.labelRes),
            fontSize = Tokens.captionTextSize,
            lineHeight = 14.sp,
            color = colors.muted,
            textAlign = TextAlign.Center,
            // Dua baris tetap, supaya label sependek "Kode" dan sepanjang
            // "Daftar Bernomor" menghasilkan petak setinggi sama.
            minLines = 2,
            maxLines = 2,
        )
    }
}

private val MarkdownAction.labelRes: Int
    get() = when (this) {
        MarkdownAction.HEADER -> R.string.format_header
        MarkdownAction.SUB_HEADER -> R.string.format_sub_header
        MarkdownAction.BOLD -> R.string.format_bold
        MarkdownAction.ITALIC -> R.string.format_italic
        MarkdownAction.STRIKE -> R.string.format_strike
        MarkdownAction.LIST -> R.string.format_list
        MarkdownAction.ORDERED_LIST -> R.string.format_ordered_list
        MarkdownAction.TODO -> R.string.format_todo
        MarkdownAction.QUOTE -> R.string.format_quote
        MarkdownAction.CODE -> R.string.format_code
        MarkdownAction.CODE_BLOCK -> R.string.format_code_block
        MarkdownAction.LINK -> R.string.format_link
    }


/**
 * Lembar tunggal untuk membuat maupun menyunting satu tugas.
 *
 * Teks dan jatuh tempo ditanyakan di tempat yang sama. Memisahkannya jadi dua
 * langkah berarti jatuh tempo hanya dipasang oleh orang yang sudah tahu ia ada
 * di menu — padahal jatuh tempo itulah yang membuat tugas muncul kembali tepat
 * waktu. Pilihan tanggal dibentangkan di dalam lembar ini, bukan di lembar
 * kedua: dua bottom sheet bertumpuk saling merebut gestur tutupnya.
 *
 * Tanpa tindakan hapus. Menghapus sudah punya jalannya sendiri — geser ke kiri —
 * dan tindakan merusak yang punya dua pintu berarti dua peluang salah tekan
 * untuk satu hal yang tidak bisa diurungkan setelah lima detik berlalu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    title: String,
    initialText: String,
    initialDue: Long?,
    confirmLabel: String,
    /** Enter menyimpan lalu mengosongkan kolom, bukan menutup lembar — FR-2.2. */
    repeatable: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState()
    var text by remember { mutableStateOf(initialText) }
    var due by remember { mutableStateOf(initialDue) }
    var dueOpen by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    fun setDue(value: Long?) {
        due = value
        dueOpen = false
    }

    LaunchedEffect(Unit) { focus.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = Tokens.space4),
        ) {
            SheetTitle(title)

            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= Todo.MAX_TEXT_LENGTH) text = it },
                placeholder = { Text(stringResource(R.string.task_text_hint)) },
                // Satu baris, bukan sekadar gaya: pada kolom multi-baris tombol
                // Enter menyisipkan baris baru dan tidak pernah memicu
                // ImeAction.Done — sehingga FR-2.2 ("Enter menyimpan lalu
                // mengosongkan kolom") diam-diam tidak pernah berjalan.
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (text.isBlank()) return@KeyboardActions
                    onConfirm(text, due)
                    if (repeatable) {
                        // Jatuh temponya ikut direset: tanggal tugas sebelumnya
                        // yang menempel diam-diam pada tugas berikutnya adalah
                        // pengingat yang tidak pernah diminta siapa pun.
                        text = ""
                        due = null
                        dueOpen = false
                    } else {
                        onDismiss()
                    }
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Tokens.space5)
                    .focusRequester(focus),
            )

            SheetRow(
                label = due?.let { DueDates.format(it) }
                    ?: stringResource(R.string.due_set),
                icon = painterResource(R.drawable.ic_schedule),
                tint = scheme.onSurface,
                onClick = { dueOpen = !dueOpen },
            )

            if (dueOpen) {
                SheetRow(
                    label = stringResource(R.string.due_today),
                    icon = painterResource(R.drawable.ic_today),
                    tint = scheme.onSurfaceVariant,
                    onClick = { setDue(DueDates.todayEvening()) },
                )
                SheetRow(
                    label = stringResource(R.string.due_tomorrow),
                    icon = painterResource(R.drawable.ic_arrow_forward),
                    tint = scheme.onSurfaceVariant,
                    onClick = { setDue(DueDates.tomorrowMorning()) },
                )
                SheetRow(
                    label = stringResource(R.string.due_next_week),
                    icon = painterResource(R.drawable.ic_date_range),
                    tint = scheme.onSurfaceVariant,
                    onClick = { setDue(DueDates.nextWeek()) },
                )
                SheetRow(
                    label = stringResource(R.string.due_pick),
                    icon = painterResource(R.drawable.ic_calendar_month),
                    tint = scheme.onSurfaceVariant,
                    onClick = { picking = true },
                )
            }

            if (due != null) {
                SheetRow(
                    label = stringResource(R.string.due_remove),
                    icon = painterResource(R.drawable.ic_close),
                    tint = scheme.onSurfaceVariant,
                    onClick = { setDue(null) },
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Tokens.space4, vertical = Tokens.space2),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel), color = scheme.onSurfaceVariant)
                }
                TextButton(
                    onClick = {
                        onConfirm(text, due)
                        onDismiss()
                    },
                    enabled = text.isNotBlank(),
                ) { Text(confirmLabel) }
            }
        }
    }

    if (picking) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { setDue(DueDates.fromPickedDate(it)) }
                        picking = false
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) { Text(stringResource(R.string.dialog_save)) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        ) { DatePicker(state = pickerState) }
    }
}

/**
 * Lembar alamat tautan.
 *
 * Ada karena tampilan biasa menyembunyikan `](alamat)` begitu polanya lengkap:
 * tautan yang disisipkan langsung ke catatan akan lenyap dari layar pada detik
 * yang sama alamatnya harus diketik. Jadi alamatnya ditanyakan lebih dulu, dan
 * yang masuk ke catatan sudah jadi.
 *
 * Label terisi dari teks yang sedang terseleksi — itu hampir selalu label yang
 * dimaksud — dan fokus mendarat di kolom alamat, satu-satunya yang belum bisa
 * ditebak dari apa pun.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkSheet(
    initialLabel: String,
    accent: Color,
    onConfirm: (label: String, url: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState()
    // Warna slot, bukan aksen aplikasi. Aksen aplikasi merah, dan kolom isian
    // bergaris merah dengan label merah adalah tampilan kolom yang salah isi —
    // padahal kolomnya baru saja terbuka dan belum diisi apa pun.
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accent,
        focusedLabelColor = accent,
        cursorColor = accent,
    )
    var label by remember { mutableStateOf(initialLabel) }
    var url by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Tokens.space5)
                .padding(bottom = Tokens.space4),
            verticalArrangement = Arrangement.spacedBy(Tokens.space2),
        ) {
            SheetTitle(stringResource(R.string.format_link))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.link_label)) },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.link_url)) },
                placeholder = { Text(LINK_HINT) },
                singleLine = true,
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (url.isNotBlank()) {
                            onConfirm(label, url.trim())
                            onDismiss()
                        }
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel), color = scheme.onSurfaceVariant)
                }
                TextButton(
                    onClick = {
                        onConfirm(label, url.trim())
                        onDismiss()
                    },
                    // Label boleh kosong — Markdown mengizinkannya, dan
                    // alamatnya sendiri lalu yang tampil. Alamat kosong tidak:
                    // itu tautan yang tidak menuju ke mana pun.
                    enabled = url.isNotBlank(),
                ) { Text(stringResource(R.string.dialog_save)) }
            }
        }
    }
}

/** Contoh alamat. Bukan nilai awal — kolom yang sudah terisi cenderung dikirim apa adanya. */
private const val LINK_HINT = "https://"
