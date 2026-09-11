package com.fivepad.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Warna slot aktif, disediakan lewat tema agar komponen tidak perlu tahu mode gelap. */
val LocalSlotColors = staticCompositionLocalOf { SlotColorsLight }

private val LightScheme = lightColorScheme(
    primary = SlotColorsLight[3],
    background = LightBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
)

private val DarkScheme = darkColorScheme(
    primary = SlotColorsDark[3],
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
)

@Composable
fun FivePadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val slots: List<Color> = if (darkTheme) SlotColorsDark else SlotColorsLight
    CompositionLocalProvider(LocalSlotColors provides slots) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            content = content,
        )
    }
}
