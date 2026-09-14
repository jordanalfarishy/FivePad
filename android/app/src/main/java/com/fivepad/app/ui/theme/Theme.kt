package com.fivepad.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.fivepad.app.data.ThemeMode

/**
 * Warna FivePad untuk tema yang sedang aktif.
 *
 * Dipakai berdampingan dengan `MaterialTheme.colorScheme`, bukan
 * menggantikannya: skema Material menangani komponen bawaan (bottom sheet,
 * pemilih tanggal, tombol teks), sedangkan yang di sini menangani nilai yang
 * tidak punya padanan di Material — pita pemisah, aksen slot, tepi titik.
 */
val LocalFivePadColors = staticCompositionLocalOf { DarkColors }

private fun schemeFor(colors: FivePadColors) = if (colors.isLight) {
    lightColorScheme(
        primary = colors.accent,
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = colors.checkedFill,
        onPrimaryContainer = Color(0xFFFFFFFF),
        background = colors.background,
        onBackground = colors.ink,
        surface = colors.separator,
        onSurface = colors.ink,
        // Lembar dan kartu memakai permukaan baris yang sama seperti daftar
        // tugas, jadi keduanya membaca sebagai bahan yang sama.
        surfaceContainerLow = colors.fieldSurface,
        surfaceVariant = colors.row,
        surfaceContainer = colors.row,
        surfaceContainerHigh = colors.row,
        surfaceContainerHighest = colors.row,
        onSurfaceVariant = colors.muted,
        outline = colors.fieldBorder,
        outlineVariant = colors.hairline,
        error = Color(0xFFFF6B6B),
        errorContainer = Color(0xFFFFE4E4),
        onErrorContainer = Color(0xFF5F1010),
    )
} else {
    darkColorScheme(
        primary = colors.accent,
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = colors.checkedFill,
        onPrimaryContainer = Color(0xFFFFFFFF),
        background = colors.background,
        onBackground = colors.ink,
        surface = colors.separator,
        onSurface = colors.ink,
        surfaceContainerLow = colors.fieldSurface,
        surfaceVariant = colors.row,
        surfaceContainer = colors.row,
        surfaceContainerHigh = colors.row,
        surfaceContainerHighest = colors.row,
        onSurfaceVariant = colors.muted,
        outline = colors.fieldBorder,
        outlineVariant = colors.hairline,
        error = Color(0xFFFF6B6B),
        errorContainer = Color(0xFF4A2022),
        onErrorContainer = Color(0xFFFFDADA),
    )
}

private val FivePadShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun FivePadTheme(theme: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val colors = if (theme == ThemeMode.LIGHT) LightColors else DarkColors

    CompositionLocalProvider(LocalFivePadColors provides colors) {
        MaterialTheme(
            colorScheme = schemeFor(colors),
            typography = FivePadTypography,
            shapes = FivePadShapes,
            content = content,
        )
    }
}
