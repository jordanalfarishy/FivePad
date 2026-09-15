package com.fivepad.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.fivepad.app.R
import com.fivepad.app.ui.theme.Tokens

/** Shared reminder entry point, including button semantics and standard press feedback. */
@Composable
internal fun ReminderActionButton(
    label: String,
    description: String? = null,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = Tokens.space5, vertical = Tokens.space2)
            .fillMaxWidth()
            .heightIn(min = Tokens.touchTarget + Tokens.space2),
        shape = MaterialTheme.shapes.small,
        border = null,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = scheme.surfaceContainerHigh,
            contentColor = scheme.onSurface,
        ),
        contentPadding = PaddingValues(Tokens.space4),
    ) {
        Icon(
            painterResource(R.drawable.ic_schedule),
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(Tokens.space6),
        )
        Column(
            Modifier.weight(1f).padding(horizontal = Tokens.space3),
            verticalArrangement = Arrangement.spacedBy(Tokens.space1),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
            )
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
        Icon(
            painterResource(R.drawable.ic_chevron_forward),
            contentDescription = null,
            modifier = Modifier.size(Tokens.space5),
        )
    }
}
