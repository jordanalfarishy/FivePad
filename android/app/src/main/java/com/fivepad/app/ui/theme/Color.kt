package com.fivepad.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Aplikasi ini hanya punya mode gelap.
 *
 * Mode terang sempat dibangun lalu dibuang atas keputusan pemilik produk.
 * Konsekuensinya bukan sekadar lebih sedikit kode: seluruh nilai warna di bawah
 * cukup diverifikasi satu kali, bukan dua, dan tidak ada lagi kelas bug "benar
 * di satu tema, rusak di tema lain".
 *
 * Semua nilai di berkas ini disalin apa adanya dari Figma
 * (file 8QciJU11WIKrBsODe13WZG). Di mana nilai desain tidak lolos kontras
 * WCAG AA, angkanya tetap dipakai dan kekurangannya dicatat di komentar —
 * supaya yang terlihat di layar selalu sama dengan yang terlihat di Figma,
 * dan keputusan menaikkannya tetap ada di tangan pemilik desain.
 */

/** Warna aksen titik slot — §7. Node 3:87–3:91. */
val SlotAccents = listOf(
    Color(0xFFEF7A5A),
    Color(0xFFE0A63F),
    Color(0xFF63BC85),
    Color(0xFF48BEDD),
    Color(0xFFA186D6),
)

/**
 * Latar selayar penuh per slot — warna bingkai "note - *" di Figma.
 * Urutannya dipastikan dari titik mana yang bercincin putih di tiap bingkai.
 */
val SlotSurfaces = listOf(
    Color(0xFF6C1700),
    Color(0xFF68480E),
    Color(0xFF204E32),
    Color(0xFF034151),
    Color(0xFF25183E),
)

/** Teks di atas latar slot: putih penuh. 8,4:1 pada slot paling terang. */
val OnSlotInk = Color(0xFFFFFFFF)

/**
 * Alpha teks sekunder di atas latar slot — penanda Markdown dan teks contoh.
 *
 * Figma memakai 0,40, yang jatuh di 3,0–3,4:1 tergantung slot dan karena itu
 * gagal AA untuk teks biasa. Nilai desain tetap dipakai; yang memakainya hanya
 * penanda sintaks dan placeholder, bukan isi catatan.
 */
const val SECONDARY_ALPHA_ON_SLOT = 0.40f

/**
 * Lapisan chrome di atas latar slot: bilah status, bilah atas, baris judul, dan
 * bilah bawah. Satu warna semi-transparan, bukan lima warna per slot — itulah
 * yang membuat kelima slot terlihat sebagai satu aplikasi.
 */
val ChromeOnSlot = Color.White.copy(alpha = 0.06f)

/** Latar pil navigasi yang sedang aktif. */
val PillActiveOnSlot = Color.White.copy(alpha = 0.16f)

/** Garis rambut pemisah chrome dari isi. Hitam, bukan putih — node 5:1463. */
val HairlineOnSlot = Color.Black.copy(alpha = 0.16f)

/** Garis indikator layar utama Android di tepi bawah. */
val HomeIndicator = Color.White.copy(alpha = 0.16f)

internal val AppSurface = Color(0xFF19191B)
internal val AppBackground = Color(0xFF19191B)
internal val AppSurfaceRaised = Color(0xFF242525)
internal val AppOnSurface = Color(0xFFFFFFFF)
internal val AppOnSurfaceVariant = Color(0xFF9BA3AC)
internal val AppOutline = Color(0xFF3A3C40)

// ---- Layar tugas, nilai diambil langsung dari Figma (node 3:331) ----

/** Bilah atas dan bawah layar tugas. Node 3:334. */
val TasksBar = Color(0xFF232324)

/** Pita pemisah antar grup. Warna tersendiri, bukan latar yang dibiarkan terlihat. */
val TaskSeparator = Color(0xFF131314)

val CheckboxFill = Color(0xFF48484B)
val CheckboxStroke = Color(0xFF6B6B6B)

/** Kotak centang yang sudah dicentang — node 3:550. */
val CheckedFill = Color(0xFF304678)
val CheckedStroke = Color(0xFF425A90)

/**
 * Aksen tindakan dari Figma (node 4:682, teks "New Group").
 *
 * 1,90:1 di atas #19191B — jauh di bawah AA. Nilainya tetap dipakai agar layar
 * sama persis dengan desain; menaikkannya cukup mengubah satu baris ini.
 */
val Accent = Color(0xFF304678)

/** Pegangan seret pada baris tugas — node 5:1500, putih 10%. */
const val DRAG_HANDLE_ALPHA = 0.1f

/**
 * Opasitas teks redup pada layar tugas: label seksi dan tugas yang selesai.
 *
 * Figma memakai 0,40 (3,81:1 pada label seksi, 3,69:1 pada teks tugas selesai).
 * Keduanya gagal AA; angkanya tetap dipakai karena inilah yang diminta desain.
 */
const val MUTED_ALPHA_TASKS = 0.40f
