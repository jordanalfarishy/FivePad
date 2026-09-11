package com.fivepad.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Aplikasi ini hanya punya mode gelap.
 *
 * Mode terang sempat dibangun lalu dibuang atas keputusan pemilik produk.
 * Konsekuensinya bukan sekadar lebih sedikit kode: seluruh nilai kontras di
 * bawah cukup diverifikasi satu kali, bukan dua, dan tidak ada lagi kelas bug
 * "benar di satu tema, rusak di tema lain".
 */

/** Warna aksen titik slot — §7. */
val SlotAccents = listOf(
    Color(0xFFEF7A5A),
    Color(0xFFE0A63F),
    Color(0xFF63BC85),
    Color(0xFF48BEDD),
    Color(0xFFA186D6),
)

/** Latar selayar penuh per slot. 5,5:1 terhadap [OnSlotInk]. */
val SlotSurfaces = listOf(
    Color(0xFF9C3F2D),
    Color(0xFF7C551C),
    Color(0xFF336748),
    Color(0xFF14657F),
    Color(0xFF685192),
)

val OnSlotInk = Color(0xFFE7EBF0)

/**
 * Alpha teks sekunder di atas latar slot: nilai terendah yang masih mencapai
 * 4,5:1 pada kelima slot. Diukur, bukan dikira-kira.
 */
const val SECONDARY_ALPHA_ON_SLOT = 0.86f

/**
 * Aksen tindakan dari Figma. Nilai asli `#304678` hanya 1,90:1 di atas latar
 * gelap — praktis tak terbaca. Hue dipertahankan, kecerahannya disetel.
 */
val Accent = Color(0xFF6380C1)

internal val AppSurface = Color(0xFF19191B)
internal val AppBackground = Color(0xFF19191B)
internal val AppSurfaceRaised = Color(0xFF242525)
internal val AppOnSurface = Color(0xFFE7EBF0)
internal val AppOnSurfaceVariant = Color(0xFF9BA3AC)
internal val AppOutline = Color(0xFF3A3C40)

// ---- Layar tugas, nilai diambil langsung dari Figma ----

/** Pita pemisah antar grup. Warna tersendiri, bukan latar yang dibiarkan terlihat. */
val TaskSeparator = Color(0xFF131314)

val CheckboxFill = Color(0xFF48484B)
val CheckboxStroke = Color(0xFF6B6B6B)
val CheckedStroke = Color(0xFF425A90)

/**
 * Opasitas teks redup pada layar tugas.
 *
 * Figma memakai 0,40, yang hanya mencapai 3,81:1 pada label seksi dan 3,69:1
 * pada teks tugas selesai — keduanya gagal AA. 0,46 adalah nilai terendah yang
 * lolos, dan secara visual nyaris tak terbedakan dari maksud desainnya.
 */
const val MUTED_ALPHA_TASKS = 0.46f
