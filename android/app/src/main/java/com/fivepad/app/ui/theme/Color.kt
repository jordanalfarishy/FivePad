package com.fivepad.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet slot punya dua peran yang sengaja dipisah.
 *
 * [SlotAccentLight] / [SlotAccentDark] adalah warna §7 asli — untuk titik
 * penanda, di mana warna hanya menempati area kecil.
 *
 * Sisanya adalah latar selayar penuh. Keduanya disetel sampai punya **ruang
 * kontras tersisa**: teks utama jauh di atas ambang, sehingga teks sekunder
 * masih bisa diredupkan dan tetap lolos AA. Palet sebelumnya disetel tepat di
 * 4,6:1, yang berarti transparansi sekecil apa pun langsung melanggar NFR-8 —
 * placeholder pun tidak mungkin dibuat tanpa gagal.
 */
val SlotAccentLight = listOf(
    Color(0xFFC7442A),
    Color(0xFFB0741A),
    Color(0xFF3B8A5B),
    Color(0xFF0B6E8F),
    Color(0xFF6B4E9E),
)

val SlotAccentDark = listOf(
    Color(0xFFEF7A5A),
    Color(0xFFE0A63F),
    Color(0xFF63BC85),
    Color(0xFF48BEDD),
    Color(0xFFA186D6),
)

/**
 * Latar mode terang: tint cerah dengan tinta gelap, 7,0:1 pada kekuatan penuh.
 *
 * Ini perubahan arah. Palet terang sebelumnya memakai warna jenuh dengan teks
 * putih — itu tema *berwarna*, bukan tema *terang*, dan tidak menyisakan ruang
 * untuk hierarki teks.
 */
val SlotSurfaceLight = listOf(
    Color(0xFFDB9081),
    Color(0xFFD3973D),
    Color(0xFF63B383),
    Color(0xFF26AFDD),
    Color(0xFFAB9BC8),
)

/** Latar mode gelap, sesuai desain Figma. 5,5:1 pada kekuatan penuh. */
val SlotSurfaceDark = listOf(
    Color(0xFF9C3F2D),
    Color(0xFF7C551C),
    Color(0xFF336748),
    Color(0xFF14657F),
    Color(0xFF685192),
)

val OnSlotLight = Color(0xFF14181F)
val OnSlotDark = Color(0xFFE7EBF0)

/**
 * Alpha teks sekunder di atas latar slot, diukur bukan dikira-kira: nilai
 * terendah yang masih mencapai 4,5:1 pada kelima slot mode tersebut.
 */
const val SECONDARY_ALPHA_LIGHT = 0.76f
const val SECONDARY_ALPHA_DARK = 0.86f

internal val LightSurface = Color(0xFFFFFFFF)
internal val LightBackground = Color(0xFFF7F8FA)
internal val LightSurfaceRaised = Color(0xFFFFFFFF)
internal val LightOnSurface = Color(0xFF14181F)
internal val LightOnSurfaceVariant = Color(0xFF565F6B)
internal val LightOutline = Color(0xFFD4DAE1)

internal val DarkSurface = Color(0xFF19191B)
internal val DarkBackground = Color(0xFF19191B)
internal val DarkSurfaceRaised = Color(0xFF242525)
internal val DarkOnSurface = Color(0xFFE7EBF0)
internal val DarkOnSurfaceVariant = Color(0xFF9BA3AC)
internal val DarkOutline = Color(0xFF3A3C40)
