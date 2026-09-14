package com.fivepad.app.ui

import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.fivepad.app.data.Recurrence
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.TextFieldColors
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
internal fun SheetRow(
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
            .playfulClick(onClick = onClick)
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
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
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
    onClose: () -> Unit = onDismiss,
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = scheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var value by rememberSaveable { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Tokens.space5)
                .padding(bottom = Tokens.space4),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Tokens.space3),
            )

            SheetTextField(
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
                Button(
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
 * Petak, bukan daftar. Sebelas tindakan sebagai baris bertumpuk menghasilkan
 * lembar setinggi hampir satu layar, dan lembar setinggi itu menutupi justru
 * catatan yang sedang diformat.
 *
 * Barisnya dikelompokkan menurut **apa yang tersentuh**, bukan menurut nama:
 * baris pertama dan ketiga dan keempat mengubah satu baris penuh, baris kedua
 * hanya mengubah potongan yang dipilih. Kelompoknya tidak berlabel — jaraknya
 * sudah mengatakan hal yang sama tanpa memakan tinggi lembar. Kelompok yang
 * melebihi empat petak digulung ke samping, bukan dibungkus ke bawah, supaya
 * tinggi lembarnya tidak ikut tumbuh saat tindakan bertambah.
 *
 * Sakelar tampilan duduk sebaris dengan judulnya: ia mengubah cara seluruh
 * catatan dibaca, bukan memformat sepotong teks, jadi ia bukan salah satu dari
 * petak-petak itu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFormatSheet(
    markdownView: Boolean,
    accent: Color,
    onToggleView: () -> Unit,
    onAction: (MarkdownAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalFivePadColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Tokens.space4),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Tokens.space5, vertical = Tokens.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.format_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.muted,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.format_view_markdown_short),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(end = Tokens.space2),
                )
                Switch(
                    checked = markdownView,
                    onCheckedChange = { onToggleView() },
                    colors = SwitchDefaults.colors(checkedTrackColor = accent),
                )
            }

            HorizontalDivider(color = colors.hairline)

            BoxWithConstraints(Modifier.padding(Tokens.space3)) {
                // Lebar petak dipatok supaya empat muat pas; yang kelima
                // mengintip di tepi, dan itulah yang memberitahu bahwa barisnya
                // bisa digulung.
                val tile = (maxWidth - Tokens.space2 * (FORMAT_COLUMNS - 1)) / FORMAT_COLUMNS
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.space3)) {
                    FORMAT_GROUPS.forEach { group ->
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Tokens.space2),
                        ) {
                            group.forEach { action ->
                                FormatTile(
                                    action = action,
                                    modifier = Modifier.width(tile),
                                    // Menutup setelah satu tindakan. Lembar yang
                                    // tetap terbuka menutupi teks yang barusan
                                    // diubahnya, dan menilai format tanpa
                                    // melihatnya mustahil.
                                    onClick = {
                                        onAction(action)
                                        onDismiss()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Empat petak muat dalam satu baris; sisanya digulung ke samping. */
private const val FORMAT_COLUMNS = 4

/**
 * Kelompok tindakan menurut apa yang tersentuh.
 *
 * Satu baris penuh: judul, lalu daftar, lalu blok. Sepotong teks: penekanan.
 */
private val FORMAT_GROUPS = listOf(
    listOf(MarkdownAction.HEADER, MarkdownAction.SUB_HEADER),
    listOf(
        MarkdownAction.BOLD,
        MarkdownAction.ITALIC,
        MarkdownAction.STRIKE,
        MarkdownAction.LINK,
        MarkdownAction.CODE,
    ),
    listOf(MarkdownAction.LIST, MarkdownAction.ORDERED_LIST, MarkdownAction.TODO),
    listOf(MarkdownAction.QUOTE),
)

@Composable
private fun FormatTile(action: MarkdownAction, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalFivePadColors.current

    Column(
        modifier
            .clip(RoundedCornerShape(Tokens.radiusMd))
            .background(colors.row)
            .border(1.dp, colors.fieldBorder, RoundedCornerShape(Tokens.radiusMd))
            .playfulClick(onClick = onClick)
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
        MarkdownAction.LINK -> R.string.format_link
    }


/** Task name first; the optional reminder is configured in its own step in this sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    title: String,
    initialText: String,
    initialDue: Long?,
    initialRecurrence: Recurrence = Recurrence.NONE,
    confirmLabel: String,
    /** Enter saves and clears the fields for another task — FR-2.2. */
    repeatable: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?, Recurrence) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by rememberSaveable { mutableStateOf(initialText) }
    var due by rememberSaveable { mutableStateOf(initialDue) }
    var recurrence by rememberSaveable { mutableStateOf(initialRecurrence) }
    var reminderOpen by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }

    ModalBottomSheet(
        // Back, outside taps, and swiping out of the scheduler discard only its draft.
        onDismissRequest = {
            if (reminderOpen) {
                reminderOpen = false
                // ModalBottomSheet has already hidden itself for a swipe/outside tap.
                scope.launch { sheetState.show() }
            } else onDismiss()
        },
        sheetState = sheetState,
        containerColor = scheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        if (reminderOpen) {
            ReminderEditor(
                initialDue = due,
                initialRecurrence = recurrence,
                onCancel = { reminderOpen = false },
                onConfirm = { selectedDue, selectedRecurrence ->
                    due = selectedDue
                    recurrence = selectedRecurrence
                    reminderOpen = false
                },
            )
        } else {
            LaunchedEffect(Unit) {
                // The name field is mounted again after the reminder step.
                // Focus it only for a new, empty draft so the keyboard stays out of the way.
                if (text.isEmpty()) focus.requestFocus()
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = Tokens.space4),
            ) {
                SheetTitle(title)
                SheetTextField(
                    value = text,
                    onValueChange = { if (it.length <= Todo.MAX_TEXT_LENGTH) text = it },
                    placeholder = { Text(stringResource(R.string.task_text_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (text.isBlank()) return@KeyboardActions
                        onConfirm(text, due, recurrence)
                        if (repeatable) {
                            text = ""
                            due = null
                            recurrence = Recurrence.NONE
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
                        ?: stringResource(R.string.reminder_add_optional),
                    description = due?.let { stringResource(recurrence.labelRes()) },
                    icon = painterResource(R.drawable.ic_schedule),
                    tint = scheme.onSurface,
                    onClick = { keyboard?.hide(); reminderOpen = true },
                )
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = Tokens.space4, vertical = Tokens.space2),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dialog_cancel), color = scheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            onConfirm(text, due, recurrence)
                            onDismiss()
                        },
                        enabled = text.isNotBlank(),
                    ) { Text(confirmLabel) }
                }
            }
        }
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Warna slot, bukan aksen aplikasi. Aksen aplikasi merah, dan kolom isian
    // bergaris merah dengan label merah adalah tampilan kolom yang salah isi —
    // padahal kolomnya baru saja terbuka dan belum diisi apa pun.
    val fieldColors = sheetFieldColors(accent)
    var label by rememberSaveable { mutableStateOf(initialLabel) }
    var url by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Tokens.space5)
                .padding(bottom = Tokens.space4),
            verticalArrangement = Arrangement.spacedBy(Tokens.space2),
        ) {
            SheetTitle(stringResource(R.string.format_link))

            SheetTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.link_label)) },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            SheetTextField(
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
                Button(
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

/** Consistent field surface and focus treatment across all sheet forms. */
@Composable
private fun sheetFieldColors(accent: Color = MaterialTheme.colorScheme.onSurface): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalFivePadColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = scheme.onSurface,
        unfocusedTextColor = scheme.onSurface,
        focusedContainerColor = colors.fieldSurface,
        unfocusedContainerColor = colors.fieldSurface,
        disabledContainerColor = colors.fieldSurface,
        errorContainerColor = colors.fieldSurface,
        focusedBorderColor = accent,
        unfocusedBorderColor = colors.fieldBorder,
        focusedLabelColor = colors.fieldSecondary,
        unfocusedLabelColor = colors.fieldSecondary,
        focusedPlaceholderColor = colors.fieldSecondary,
        unfocusedPlaceholderColor = colors.fieldSecondary,
        cursorColor = accent,
    )
}

@Composable
private fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: (@Composable () -> Unit)? = null,
    label: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    colors: TextFieldColors = sheetFieldColors(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier,
        placeholder = placeholder, label = label, singleLine = singleLine,
        colors = colors, shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
    )
}
