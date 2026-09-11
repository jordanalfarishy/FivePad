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
val SlotAccentsUnusedLight = listOf(
    Color(0xFFC7442A),
    Color(0xFFB0741A),
    Color(0xFF3B8A5B),
    Color(0xFF0B6E8F),
    Color(0xFF6B4E9E),
)

val SlotAccents = listOf(
    Color(0xFFEF7A5A),
    Color(0xFFE0A63F),
    Color(0xFF63BC85),
    Color(0xFF48BEDD),
    Color(0xFFA186D6),
)

/**
 * Latar slot — **satu palet untuk kedua tema**.
 *
 * Tab catatan sengaja tidak ikut berganti terang/gelap: warna slot itu sendiri
 * yang menjadi identitas layar, dan mempertahankannya membuat slot 3 selalu
 * hijau yang sama, bukan dua hijau berbeda tergantung tema. Sisa aplikasi
 * (tugas, pengaturan) tetap mengikuti tema.
 *
 * Efek sampingnya menguntungkan: karena tintanya selalu terang di atas latar
 * pekat, ruang kontras untuk teks sekunder selalu tersedia.
 */
val SlotSurfaces = listOf(
    Color(0xFF9C3F2D),
    Color(0xFF7C551C),
    Color(0xFF336748),
    Color(0xFF14657F),
    Color(0xFF685192),
)

/** Tinta di atas latar slot. Satu nilai, karena latarnya juga satu palet. */
val OnSlotInk = Color(0xFFE7EBF0)

/**
 * Alpha teks sekunder di atas latar slot: nilai terendah yang masih mencapai
 * 4,5:1 pada kelima slot. Diukur, bukan dikira-kira.
 */
const val SECONDARY_ALPHA_ON_SLOT = 0.86f

/**
 * Aksen dari Figma (#304678) untuk tombol tambah dan tautan aksi.
 *
 * Nilai aslinya hanya 1,90:1 di atas latar gelap — praktis tak terbaca. Hue
 * dipertahankan, kecerahannya disetel sampai lolos AA di masing-masing tema.
 */
val AccentLight = Color(0xFF304678)
val AccentDark = Color(0xFF6380C1)

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

// ---- Layar tugas, nilai diambil langsung dari Figma ----

/** Pita pemisah antar grup. Warna tersendiri, bukan latar yang dibiarkan terlihat. */
val TaskSeparatorDark = Color(0xFF131314)
val TaskSeparatorLight = Color(0xFFE6E9EE)

/** Kotak centang kosong. */
val CheckboxFillDark = Color(0xFF48484B)
val CheckboxStrokeDark = Color(0xFF6B6B6B)
val CheckboxFillLight = Color(0xFFE4E8ED)
val CheckboxStrokeLight = Color(0xFFAFB7C0)

/** Tepi kotak centang saat tercentang. */
val CheckedStrokeDark = Color(0xFF425A90)
val CheckedStrokeLight = Color(0xFF5C74A8)

/**
 * Opasitas teks redup pada layar tugas.
 *
 * Figma memakai 0,40, yang hanya mencapai 3,81:1 pada label seksi dan 3,69:1
 * pada teks tugas selesai — keduanya gagal AA. 0,46 adalah nilai terendah yang
 * lolos, dan secara visual nyaris tak terbedakan dari maksud desainnya.
 */
const val MUTED_ALPHA_TASKS = 0.46f
