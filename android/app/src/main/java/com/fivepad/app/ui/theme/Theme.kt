package com.fivepad.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Warna aksen titik slot pada mode yang sedang aktif. */
val LocalSlotAccents = staticCompositionLocalOf { SlotAccentLight }

/** Warna latar selayar penuh per slot. */
val LocalSlotSurfaces = staticCompositionLocalOf { SlotSurfaceLight }

/** Warna teks di atas latar slot — satu nilai untuk kelima slot. */
val LocalOnSlot = staticCompositionLocalOf { OnSlotLight }

/** Apakah tema gelap sedang berlaku, setelah ThemeMode.SYSTEM diselesaikan. */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/** Warna teks sekunder di atas latar slot — sudah beralpha, langsung pakai. */
val LocalOnSlotSecondary = staticCompositionLocalOf { OnSlotLight.copy(alpha = SECONDARY_ALPHA_LIGHT) }

private val LightScheme = lightColorScheme(
    // Container disetel eksplisit: FAB dan checkbox memakai primaryContainer,
    // dan tanpa nilai sendiri keduanya jatuh ke ungu bawaan Material 3.
    primary = SlotAccentLight[3],
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = SlotAccentLight[3],
    onPrimaryContainer = Color(0xFFFFFFFF),
    background = LightBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
    surfaceContainer = LightSurfaceRaised,
    surfaceContainerHigh = LightSurfaceRaised,
)

private val DarkScheme = darkColorScheme(
    primary = SlotAccentDark[3],
    onPrimary = Color(0xFF0B1418),
    primaryContainer = SlotAccentDark[3],
    onPrimaryContainer = Color(0xFF0B1418),
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    surfaceContainer = DarkSurfaceRaised,
    surfaceContainerHigh = DarkSurfaceRaised,
)

@Composable
fun FivePadTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val accents: List<Color> = if (dark) SlotAccentDark else SlotAccentLight
    val surfaces: List<Color> = if (dark) SlotSurfaceDark else SlotSurfaceLight
    val onSlot: Color = if (dark) OnSlotDark else OnSlotLight
    val onSlotSecondary = onSlot.copy(
        alpha = if (dark) SECONDARY_ALPHA_DARK else SECONDARY_ALPHA_LIGHT,
    )

    CompositionLocalProvider(
        LocalSlotAccents provides accents,
        LocalSlotSurfaces provides surfaces,
        LocalOnSlot provides onSlot,
        LocalOnSlotSecondary provides onSlotSecondary,
        LocalIsDarkTheme provides dark,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            content = content,
        )
    }
}
