package com.fivepad.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet lima slot dari PRD §7.
 *
 * Warna bersifat tetap dan tidak dapat diubah pengguna — konsistensinyalah yang
 * membuat "yang hijau itu urusan klien A" bisa dihafal tanpa membaca label.
 * Karena itu palet ini juga tidak ikut berubah saat Material You aktif.
 */
val SlotColorsLight = listOf(
    Color(0xFFC7442A),
    Color(0xFFB0741A),
    Color(0xFF3B8A5B),
    Color(0xFF0B6E8F),
    Color(0xFF6B4E9E),
)

val SlotColorsDark = listOf(
    Color(0xFFEF7A5A),
    Color(0xFFE0A63F),
    Color(0xFF63BC85),
    Color(0xFF48BEDD),
    Color(0xFFA186D6),
)

internal val LightSurface = Color(0xFFFFFFFF)
internal val LightBackground = Color(0xFFEEF1F4)
internal val LightOnSurface = Color(0xFF14181F)
internal val LightOnSurfaceVariant = Color(0xFF6B7684)
internal val LightOutline = Color(0xFFDCE2E8)

internal val DarkSurface = Color(0xFF181C22)
internal val DarkBackground = Color(0xFF101318)
internal val DarkOnSurface = Color(0xFFE7EBF0)
internal val DarkOnSurfaceVariant = Color(0xFF8593A2)
internal val DarkOutline = Color(0xFF2A313A)
