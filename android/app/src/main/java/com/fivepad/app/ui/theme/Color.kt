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
    /**
     * Aksen tindakan: tombol tambah, teks "New Group", kotak centang tercentang,
     * dan pil tab Tugas. Berbeda per tema karena satu merah tidak bisa lolos
     * 4,5:1 di atas latar gelap dan latar terang sekaligus — menaikkannya untuk
     * yang satu menurunkannya untuk yang lain.
     */
    val accent: Color,
    /**
     * Opasitas teks sekunder. Nilai terendah yang mencapai 4,5:1 pada permukaan
     * terlemah tema itu. Berbeda antar tema karena tinta gelap yang diencerkan
     * kehilangan kontras jauh lebih cepat daripada tinta putih: 0,47 sudah cukup
     * di tema gelap, tema terang butuh 0,65 untuk kelegapan yang sama.
     */
    val mutedAlpha: Float,
    /**
     * Isian blok kode pada tampilan biasa — node Figma 18:493.
     *
     * Hitam pekat di tema gelap, putih bersih di tema terang. Bukan warna
     * permukaan yang sudah ada: blok kode justru harus terbaca sebagai bahan
     * yang berbeda dari halaman di sekitarnya, dan satu-satunya cara
     * melakukannya di kedua tema adalah pergi ke ujung skala, bukan mendekat.
     */
    val codeFill: Color,
    /** Teks tautan. Nilainya dari Figma; keduanya lolos AA di temanya sendiri. */
    val link: Color,
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

    /** Opaque form colors keep hints and boundaries readable on either theme. */
    val fieldSurface: Color = if (isLight) Color(0xFFC4C4CC) else Color(0xFF3A3A40)
    val fieldSecondary: Color = if (isLight) Color(0xFF484852) else Color(0xFFC4C4CC)
    val fieldBorder: Color = if (isLight) Color(0xFF64646F) else Color(0xFF92929C)

    /** Teks sekunder: label seksi, penanda Markdown, tugas selesai, placeholder. */
    val muted: Color = ink.copy(alpha = mutedAlpha)

    /** Kotak centang yang sudah dicentang — isian beraksen, tepi lebih terang. */
    val checkedFill: Color = accent

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
    // Kelimanya mencapai 5,2–7,3:1 di atas chrome, jadi nilai Figma dipakai apa
    // adanya — judul catatan yang memakainya sudah lolos AA.
    slotAccents = listOf(
        Color(0xFFEF7A5A),
        Color(0xFFE0A63F),
        Color(0xFF63BC85),
        Color(0xFF48BEDD),
        Color(0xFFA186D6),
    ),
    accent = Color(0xFFFF5242),
    mutedAlpha = 0.47f,
    codeFill = Color(0xFF000000),
    link = Color(0xFF39A6FF),
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
    /*
     * Bukan versi gelap dari warna yang sama: di atas latar terang, aksen tema
     * gelap jatuh di bawah 2:1. Kelimanya nilai tersendiri.
     *
     * Nilai Figma — #E73200 #E49200 #1B8744 #13A4CB #5320B7 — dipakai sebagai
     * warna titik, tapi empat dari lima gagal AA sebagai teks judul catatan
     * (2,37–4,34:1). Masing-masing digelapkan pada hue yang sama sampai tepat
     * mencapai 4,5:1; yang kelima sudah lolos dan tidak disentuh. Satu nilai per
     * slot, bukan dua, supaya titik, pita, judul, dan pil tetap satu warna —
     * itulah yang membuat kedua ujung layar menjawab "slot mana" bersama-sama.
     */
    slotAccents = listOf(
        Color(0xFFDB2F00),
        Color(0xFFA06700),
        Color(0xFF1A8442),
        Color(0xFF0E7D9B),
        Color(0xFF5320B7),
    ),
    accent = Color(0xFFC71C0D),
    mutedAlpha = 0.65f,
    codeFill = Color(0xFFFFFFFF),
    link = Color(0xFF3415FF),
)

/** Tepi kotak centang yang tercentang — selalu lebih terang dari isiannya. */
val CheckedStroke = Color(0xFFFF4332)

/**
 * Isian tombol empty state — node 11:147, sama di kedua tema.
 *
 * Punya nilai sendiri, bukan [FivePadColors.accent]. Aksen dipakai sebagai
 * *teks dan ikon* di atas halaman, jadi nilainya harus berbeda per tema agar
 * kontras; di sini warnanya justru yang menjadi latar, dan yang harus kontras
 * adalah putih di atasnya. `#E6210F` memberi 4,58:1 dengan putih — lolos AA di
 * kedua tema dengan satu nilai, persis seperti yang digambar Figma.
 */
val FilledAccent = Color(0xFFE6210F)

/**
 * Opasitas titik slot yang tidak aktif.
 *
 * Tetap 0,40 sesuai desain. Titik yang tidak terpilih adalah *state* tidak
 * aktif, yang dikecualikan WCAG 1.4.11, dan yang wajib teridentifikasi adalah
 * yang terpilih — dan itu tampil beraksen penuh dengan cincin tinta 2 dp di
 * luarnya, 12–15:1 terhadap chrome. Menaikkan alpha-nya lebih jauh justru
 * meratakan beda terpilih dan tidak terpilih: kerugian nyata demi kemenangan
 * aksesibilitas yang semu.
 */
const val DOT_INACTIVE_ALPHA = 0.40f

/** Latar pil navigasi yang aktif: warna tab itu sendiri, 16%. */
const val PILL_ALPHA = 0.16f

/**
 * Opasitas isian kutipan pada tampilan biasa.
 *
 * Aksen slot 12% di atas latar halaman — nilai yang diukur langsung dari
 * bingkai Figma, dan sama di kedua tema. Dipakai sebagai alpha saat menggambar,
 * bukan sebagai warna jadi, karena aksennya berganti tiap slot.
 */
const val QUOTE_FILL_ALPHA = 0.12f
