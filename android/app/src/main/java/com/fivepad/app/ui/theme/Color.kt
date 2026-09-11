package com.fivepad.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet slot punya dua peran yang sengaja dipisah.
 *
 * [SlotAccentLight] / [SlotAccentDark] adalah warna §7 yang asli — dipakai untuk
 * titik penanda slot, di mana warna hanya menempati area kecil.
 *
 * [SlotSurfaceLight] / [SlotSurfaceDark] adalah turunannya untuk latar selayar
 * penuh. Warna §7 tidak bisa dipakai langsung sebagai latar: dengan teks putih,
 * slot 3 hanya mencapai 4,22:1 dan gagal WCAG AA (NFR-8), sementara slot 2 satu-
 * satunya yang menuntut teks gelap sehingga tampak seperti cacat, bukan desain.
 * Hue dipertahankan, hanya kecerahan yang disetel sampai seluruh lima slot lolos
 * dengan satu warna teks yang sama.
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

/** Latar selayar penuh, mode terang. Teks putih: 4,61–4,62:1 — lolos AA. */
val SlotSurfaceLight = listOf(
    Color(0xFFCE472C),
    Color(0xFFA06918),
    Color(0xFF388356),
    Color(0xFF0D7EA4),
    Color(0xFF8367B4),
)

/**
 * Latar selayar penuh, mode gelap. Teks [OnSlotDark]: 5,5:1.
 * Sengaja jauh lebih pekat daripada warna aksen — satu layar penuh #E0A63F di
 * ruang gelap menyilaukan, bukan nyaman.
 */
val SlotSurfaceDark = listOf(
    Color(0xFF9C3F2D),
    Color(0xFF7C551C),
    Color(0xFF336748),
    Color(0xFF14657F),
    Color(0xFF685192),
)

val OnSlotLight = Color(0xFFFFFFFF)
val OnSlotDark = Color(0xFFE7EBF0)

internal val LightSurface = Color(0xFFFFFFFF)
internal val LightBackground = Color(0xFFF7F8FA)
internal val LightOnSurface = Color(0xFF14181F)
internal val LightOnSurfaceVariant = Color(0xFF6B7684)
internal val LightOutline = Color(0xFFE4E8ED)

internal val DarkSurface = Color(0xFF181C22)
internal val DarkBackground = Color(0xFF101318)
internal val DarkOnSurface = Color(0xFFE7EBF0)
internal val DarkOnSurfaceVariant = Color(0xFF8593A2)
internal val DarkOutline = Color(0xFF262D36)
