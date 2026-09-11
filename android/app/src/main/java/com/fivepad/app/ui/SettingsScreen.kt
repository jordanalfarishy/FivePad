package com.fivepad.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.fivepad.app.R
import com.fivepad.app.ui.theme.Tokens

@Composable
fun SettingsScreen(onBack: () -> Unit) {
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
            // Tema tidak lagi punya pengaturan: aplikasi hanya bermode gelap.
            // Bagian ini menunggu FR-3 di M2, dan sengaja dibiarkan menjelaskan
            // apa yang belum ada ketimbang menyajikan layar kosong.
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
