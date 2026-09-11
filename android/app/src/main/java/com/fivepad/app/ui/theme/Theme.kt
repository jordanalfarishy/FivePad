package com.fivepad.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
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
        surface = colors.background,
        onSurface = colors.ink,
        // Lembar dan kartu memakai permukaan baris yang sama seperti daftar
        // tugas, jadi keduanya membaca sebagai bahan yang sama.
        surfaceContainer = colors.row,
        surfaceContainerHigh = colors.row,
        surfaceContainerHighest = colors.row,
        onSurfaceVariant = colors.muted,
        outline = colors.hairline,
        outlineVariant = colors.hairline,
    )
} else {
    darkColorScheme(
        primary = colors.accent,
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = colors.checkedFill,
        onPrimaryContainer = Color(0xFFFFFFFF),
        background = colors.background,
        onBackground = colors.ink,
        surface = colors.background,
        onSurface = colors.ink,
        surfaceContainer = colors.row,
        surfaceContainerHigh = colors.row,
        surfaceContainerHighest = colors.row,
        onSurfaceVariant = colors.muted,
        outline = colors.hairline,
        outlineVariant = colors.hairline,
    )
}

@Composable
fun FivePadTheme(theme: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val colors = if (theme == ThemeMode.LIGHT) LightColors else DarkColors

    CompositionLocalProvider(LocalFivePadColors provides colors) {
        MaterialTheme(colorScheme = schemeFor(colors), content = content)
    }
}
