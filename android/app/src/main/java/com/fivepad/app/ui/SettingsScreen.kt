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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fivepad.app.R
import com.fivepad.app.ui.theme.ThemeMode
import com.fivepad.app.ui.theme.Tokens

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Tokens.space2, vertical = Tokens.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(Tokens.touchTarget)
                    .clip(RoundedCornerShape(Tokens.radiusPill))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.settings_back),
                    tint = scheme.onSurface,
                )
            }
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                modifier = Modifier.padding(start = Tokens.space2),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = Tokens.space4),
            verticalArrangement = Arrangement.spacedBy(Tokens.space5),
        ) {
            SettingsSection(stringResource(R.string.settings_appearance)) {
                Text(
                    stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Tokens.space2),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.space2)) {
                    ThemeChoice(R.string.theme_system, ThemeMode.SYSTEM, themeMode, onThemeChange, Modifier.weight(1f))
                    ThemeChoice(R.string.theme_light, ThemeMode.LIGHT, themeMode, onThemeChange, Modifier.weight(1f))
                    ThemeChoice(R.string.theme_dark, ThemeMode.DARK, themeMode, onThemeChange, Modifier.weight(1f))
                }
            }

            SettingsSection(stringResource(R.string.settings_account)) {
                Text(
                    stringResource(R.string.settings_account_pending),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Tokens.space3),
        )
        content()
    }
}

@Composable
private fun ThemeChoice(
    labelRes: Int,
    value: ThemeMode,
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val selected = value == current
    Box(
        modifier
            .clip(RoundedCornerShape(Tokens.radiusMd))
            .background(if (selected) scheme.onSurface.copy(alpha = 0.08f) else Color.Transparent)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) scheme.onSurface.copy(alpha = 0.4f) else scheme.outline,
                shape = RoundedCornerShape(Tokens.radiusMd),
            )
            .clickable { onSelect(value) }
            .padding(vertical = Tokens.space3),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(labelRes),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
        )
    }
}
