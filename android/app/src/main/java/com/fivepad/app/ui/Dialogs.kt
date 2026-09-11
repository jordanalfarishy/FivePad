package com.fivepad.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.fivepad.app.R
import com.fivepad.app.data.Todo
import com.fivepad.app.data.TodoGroup
import com.fivepad.app.ui.theme.Tokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    groups: List<TodoGroup>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var groupId by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val noneLabel = stringResource(R.string.group_none)
    val selectedLabel = groups.firstOrNull { it.id == groupId }?.name ?: noneLabel

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(),
        title = { Text(stringResource(R.string.task_add)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= Todo.MAX_TEXT_LENGTH) text = it },
                    placeholder = { Text(stringResource(R.string.task_text_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (groups.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.padding(top = Tokens.space3),
                    ) {
                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.group_picker)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(
                                    androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    true,
                                ),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(noneLabel) },
                                onClick = { groupId = null; expanded = false },
                            )
                            groups.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g.name) },
                                    onClick = { groupId = g.id; expanded = false },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text, groupId) },
                enabled = text.isNotBlank(),
            ) { Text(stringResource(R.string.dialog_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@Composable
fun TextPromptDialog(
    title: String,
    initial: String,
    hint: String,
    confirmLabel: String,
    maxLength: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= maxLength) value = it },
                placeholder = { Text(hint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank(),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}
