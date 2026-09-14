package com.fivepad.app.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Titik pendaratan nilai desain.
 *
 * Semua jarak, radius, dan ukuran hidup di sini, bukan tersebar sebagai angka
 * ajaib di dalam composable. Skala empat titik dipertahankan, lalu radius dan
 * tinggi kontrol mengikuti bahasa visual gelap dan lembut aplikasi.
 */
object Tokens {

    /** Skala spasi. Kelipatan 4, jadi semua jarak tetap sebidang. */
    val space1 = 4.dp
    val space2 = 8.dp
    val space3 = 12.dp
    val space4 = 16.dp
    val space5 = 20.dp
    val space6 = 24.dp

    /** Sisi kiri-kanan isi catatan. */
    val screenPadding = space4

    val radiusSm = 12.dp
    val radiusMd = 16.dp
    val radiusLg = 18.dp
    val radiusPill = 999.dp

    // ---- Bilah atas catatan ----

    /** Tinggi bilah atas, dan lebar kotak ikon di kedua ujungnya. */
    val topBarHeight = 56.dp

    /** Titik penanda slot: 24 dp, dengan cincin putih 2 dp pada yang aktif. */
    val dot = 24.dp
    val dotRing = 2.dp
    val dotGap = space4

    /** Baris judul catatan memberi ruang untuk gaya nama 17 sp. */
    val titleRowHeight = 40.dp

    /** Pita warna slot di bawah baris judul. */
    val stripeHeight = 4.dp

    // ---- Bilah bawah ----

    val navHeight = 62.dp
    val pillWidth = 84.dp
    val pillHeight = 52.dp

    /**
     * Area sentuh minimum. Pedoman Material meminta 48 dp; titik yang hanya
     * 24 dp jauh di bawah itu, jadi sasaran sentuhnya diperbesar tanpa
     * mengubah ukuran lingkaran yang tampak.
     */
    val touchTarget = 48.dp

    val bodyTextSize = 16.sp

    /** Comfortable note leading shared by source and formatted views. */
    val bodyLineHeight = 28.sp
    val captionTextSize = 12.sp
}
