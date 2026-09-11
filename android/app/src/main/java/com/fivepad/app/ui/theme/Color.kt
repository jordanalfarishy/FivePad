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

/**
 * Warna aksen slot — §7, node 3:87–3:91.
 *
 * Dipakai di dua tempat: titik penanda slot, dan nama catatan slot aktif.
 */
val SlotAccents = listOf(
    Color(0xFFEF7A5A),
    Color(0xFFE0A63F),
    Color(0xFF63BC85),
    Color(0xFF48BEDD),
    Color(0xFFA186D6),
)

/**
 * Opasitas titik slot yang tidak aktif.
 *
 * Figma memakai 0,24; nilainya dinaikkan ke 0,40 atas keputusan pemilik produk.
 * Pada 0,24 titik tidak aktif hanya mencapai 1,5:1 terhadap chrome — memadai
 * untuk sekadar menandai "bukan yang ini", tapi titik inilah juga kontrol untuk
 * berpindah slot (FR-1.6), dan sasaran yang nyaris tak terlihat tidak bisa
 * dibidik. Pada 0,40 nilainya 2,2–2,3:1 dan tetap jelas kalah dari titik aktif,
 * yang tampil penuh sekaligus bercincin putih.
 */
const val DOT_INACTIVE_ALPHA = 0.40f

/** Garis tepi tipis di dalam setiap titik — `stroke-opacity="0.24"`. */
val DotStroke = Color.White.copy(alpha = 0.24f)

/** Cincin titik aktif: putih penuh, digambar DI LUAR lingkaran 24 dp. */
val DotRing = Color(0xFFFFFFFF)

/** Latar pil navigasi yang sedang aktif. */
val PillActive = Color.White.copy(alpha = 0.16f)

/** Garis rambut pemisah chrome dari isi. Hitam, bukan putih — node 5:1463. */
val Hairline = Color.Black.copy(alpha = 0.16f)

internal val AppSurface = Color(0xFF19191B)
internal val AppBackground = Color(0xFF19191B)
internal val AppSurfaceRaised = Color(0xFF242525)
internal val AppOnSurface = Color(0xFFFFFFFF)
internal val AppOnSurfaceVariant = Color(0xFF9BA3AC)
internal val AppOutline = Color(0xFF3A3C40)

// ---- Layar tugas, nilai diambil langsung dari Figma (node 3:331) ----

/**
 * Bilah status, bilah atas, baris judul, dan bilah bawah — di KEDUA tab.
 * Node 3:334 dan 3:92; sejak desain terbaru keduanya memakai nilai yang sama.
 */
val AppBar = Color(0xFF232324)

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
