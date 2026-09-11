package com.fivepad.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Warna aksen titik slot. */
val LocalSlotAccents = staticCompositionLocalOf { SlotAccents }

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = CheckedFill,
    onPrimaryContainer = Color(0xFFFFFFFF),
    background = AppBackground,
    surface = AppSurface,
    surfaceContainer = AppSurfaceRaised,
    surfaceContainerHigh = AppSurfaceRaised,
    onSurface = AppOnSurface,
    onSurfaceVariant = AppOnSurfaceVariant,
    outline = AppOutline,
    outlineVariant = AppOutline,
)

@Composable
fun FivePadTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSlotAccents provides SlotAccents) {
        MaterialTheme(colorScheme = Scheme, content = content)
    }
}
