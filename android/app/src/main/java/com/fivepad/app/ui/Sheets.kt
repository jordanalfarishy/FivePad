package com.fivepad.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fivepad.app.R
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
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = Tokens.space5,
                    end = Tokens.space5,
                    bottom = Tokens.space3,
                ),
            )

            actions.forEach { action ->
                val tint = if (action.destructive) scheme.error else scheme.onSurface
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            action.onClick()
                            onDismiss()
                        }
                        .padding(horizontal = Tokens.space5, vertical = Tokens.space3),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.space4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (action.icon != null) {
                        Icon(
                            action.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(Modifier.padding(vertical = Tokens.space1)) {
                        Text(
                            action.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = tint,
                        )
                        if (action.description != null) {
                            Text(
                                action.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
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

/** Warna latar aksi geser ke kanan. Dipisah agar dipakai konsisten. */
val RenameSwipeColor: Color get() = Color(0xFF2E4470)
