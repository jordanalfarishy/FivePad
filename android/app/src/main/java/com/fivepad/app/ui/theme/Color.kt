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
    /** Permukaan tenang untuk jeda antarbagian. */
    val separator: Color,
    val ink: Color,
    val checkboxFill: Color,
    val checkboxStroke: Color,
    /** Aksen per slot. Nilainya berbeda antar tema agar kontrasnya tetap ada. */
    val slotAccents: List<Color>,
    /**
     * Aksen tindakan tunggal: tombol tambah, pilihan aktif, tautan, dan kotak
     * centang terisi. Birunya sengaja tidak dipakai sebagai permukaan konten.
     */
    val accent: Color,
    /**
     * Opasitas teks sekunder. Dipadukan dengan tinta tiap tema untuk mendekati
     * pasangan referensi #9A9AA2 (gelap) dan #66666E (terang).
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
    /** Teks tautan memakai aksen tindakan yang sama di kedua tema. */
    val link: Color,
) {
    /**
     * Garis pemisah chrome dari isi.
     *
     * Nilai eksplisit menjaga garis tetap terlihat di antara dua permukaan yang
     * sengaja sangat berdekatan.
     */
    val hairline: Color = if (isLight) Color(0xFFE6E6EA) else Color(0xFF2A2A2E)

    /** Opaque form colors keep hints and boundaries readable on either theme. */
    val fieldSurface: Color = if (isLight) Color(0xFFF1F1F3) else Color(0xFF1E1E21)
    val fieldSecondary: Color = if (isLight) Color(0xFF66666E) else Color(0xFF9A9AA2)
    val fieldBorder: Color = if (isLight) Color(0xFFE6E6EA) else Color(0xFF2A2A2E)

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
    background = Color(0xFF0F0F10),
    bar = Color(0xFF0F0F10),
    row = Color(0xFF1E1E21),
    separator = Color(0xFF161618),
    ink = Color(0xFFECECEE),
    checkboxFill = Color(0xFF26262A),
    checkboxStroke = Color(0xFF66666E),
    // Kelimanya mencapai 5,2–7,3:1 di atas chrome, jadi nilai Figma dipakai apa
    // adanya — judul catatan yang memakainya sudah lolos AA.
    slotAccents = listOf(
        Color(0xFFEF7A5A),
        Color(0xFFE0A63F),
        Color(0xFF63BC85),
        Color(0xFF48BEDD),
        Color(0xFFA186D6),
    ),
    accent = Color(0xFF3A7BFD),
    mutedAlpha = 0.64f,
    codeFill = Color(0xFF000000),
    link = Color(0xFF3A7BFD),
)

val LightColors = FivePadColors(
    isLight = true,
    background = Color(0xFFFFFFFF),
    bar = Color(0xFFFFFFFF),
    row = Color(0xFFF1F1F3),
    separator = Color(0xFFF4F4F6),
    ink = Color(0xFF16161A),
    checkboxFill = Color(0xFFF1F1F3),
    checkboxStroke = Color(0xFF9A9AA6),
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
    accent = Color(0xFF3A7BFD),
    mutedAlpha = 0.64f,
    codeFill = Color(0xFFFFFFFF),
    link = Color(0xFF3A7BFD),
)

/** Tepi kotak centang tercentang memakai keadaan tekan dari aksen biru. */
val CheckedStroke = Color(0xFF2E63D6)

/** Isian CTA utama, sama dengan aksen merek dan dipasangkan dengan teks putih. */
val FilledAccent = Color(0xFF3A7BFD)

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

/**
 * Opasitas isian kutipan pada tampilan biasa.
 *
 * Aksen slot 12% di atas latar halaman — nilai yang diukur langsung dari
 * bingkai Figma, dan sama di kedua tema. Dipakai sebagai alpha saat menggambar,
 * bukan sebagai warna jadi, karena aksennya berganti tiap slot.
 */
const val QUOTE_FILL_ALPHA = 0.12f
