package com.fivepad.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Warna FivePad untuk satu tema.
 *
 * Yang disimpan hanya nilai yang benar-benar berbeda antar tema. Sisanya —
 * garis rambut, teks redup, tepi titik, pegangan seret — diturunkan dari
 * [ink] dengan alpha yang sama di kedua tema, karena memang begitulah Figma
 * menggambarnya: bukan dua warna berbeda, melainkan satu tinta dengan
 * ketebalan berbeda. Menyimpannya sebagai nilai terpisah per tema akan
 * membuka peluang keduanya berbeda tanpa ada yang sengaja membedakannya.
 */
@Immutable
class FivePadColors(
    val isLight: Boolean,
    /** Latar isi — layar catatan dan layar tugas. */
    val background: Color,
    /** Bilah status, bilah atas, baris judul, dan bilah bawah. */
    val bar: Color,
    /** Kartu baris: tugas, pengaturan, dan isi bottom sheet. */
    val row: Color,
    /** Pita pemisah antar bagian. */
    val separator: Color,
    val ink: Color,
    val checkboxFill: Color,
    val checkboxStroke: Color,
    /** Aksen per slot. Nilainya berbeda antar tema agar kontrasnya tetap ada. */
    val slotAccents: List<Color>,
) {
    /**
     * Garis pemisah chrome dari isi.
     *
     * Di tema gelap hitam 16%, di tema terang tinta 16% — keduanya garis gelap,
     * bukan terang. Berkas Figma sempat memakai putih 16% pada bilah atas tema
     * terang; itu menghasilkan garis yang tidak terlihat sama sekali, dan baris
     * judul di bingkai yang sama memakai tinta 16%. Nilai yang terlihat itulah
     * yang dipakai.
     */
    val hairline: Color = if (isLight) ink.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.16f)

    /** Teks sekunder: label seksi, penanda Markdown, tugas selesai, placeholder. */
    val muted: Color = ink.copy(alpha = MUTED_ALPHA)

    /** Garis tepi tipis di dalam setiap titik slot. */
    val dotStroke: Color = ink.copy(alpha = 0.24f)

    /** Cincin titik aktif, digambar di luar lingkaran 24 dp. */
    val dotRing: Color = ink

    /** Pegangan seret pada baris tugas. */
    val dragHandle: Color = ink.copy(alpha = 0.1f)
}

val DarkColors = FivePadColors(
    isLight = false,
    background = Color(0xFF19191B),
    bar = Color(0xFF232324),
    row = Color(0xFF242525),
    separator = Color(0xFF131314),
    ink = Color(0xFFFFFFFF),
    checkboxFill = Color(0xFF48484B),
    checkboxStroke = Color(0xFF6B6B6B),
    slotAccents = listOf(
        Color(0xFFEF7A5A),
        Color(0xFFE0A63F),
        Color(0xFF63BC85),
        Color(0xFF48BEDD),
        Color(0xFFA186D6),
    ),
)

val LightColors = FivePadColors(
    isLight = true,
    background = Color(0xFFEAEAE8),
    bar = Color(0xFFF9F9F9),
    row = Color(0xFFFFFFFF),
    separator = Color(0xFFDDDDDA),
    ink = Color(0xFF25242C),
    checkboxFill = Color(0xFFEFEFED),
    checkboxStroke = Color(0xFFD7D7D7),
    // Bukan versi gelap dari warna yang sama: di atas latar terang, aksen yang
    // dipakai tema gelap akan jatuh di bawah 2:1. Kelimanya nilai tersendiri.
    slotAccents = listOf(
        Color(0xFFE73200),
        Color(0xFFE49200),
        Color(0xFF1B8744),
        Color(0xFF13A4CB),
        Color(0xFF5320B7),
    ),
)

/**
 * Aksen tindakan — sama di kedua tema.
 *
 * `#E6210F` diambil dari sudut terlipat pada ikon aplikasi, jadi warna tindakan
 * dan warna merek akhirnya satu benda. Ia mencapai 3,84:1 di atas latar gelap
 * dan 4,58:1 di atas kartu terang: lolos AA untuk komponen antarmuka dan teks
 * besar di mana pun, dan lolos AA penuh untuk teks kecil hanya di atas kartu
 * putih. Pendahulunya, `#304678`, hanya 1,90:1 — ini kenaikan dua kali lipat.
 */
val Accent = Color(0xFFE6210F)

/** Kotak centang yang sudah dicentang. */
val CheckedFill = Accent
val CheckedStroke = Color(0xFFFF4332)

/**
 * Opasitas titik slot yang tidak aktif, dan teks redup.
 *
 * 0,40 di kedua tema. Di tema gelap teks redup mencapai 3,81:1; di tema terang
 * hanya 2,30:1, karena tinta gelap yang diencerkan kehilangan kontras lebih
 * cepat daripada tinta putih. Nilai desain tetap dipakai di keduanya.
 */
const val MUTED_ALPHA = 0.40f
const val DOT_INACTIVE_ALPHA = 0.40f

/** Latar pil navigasi yang aktif: warna tab itu sendiri, 16%. */
const val PILL_ALPHA = 0.16f
