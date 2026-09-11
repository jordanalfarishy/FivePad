package com.fivepad.app.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Titik pendaratan nilai desain.
 *
 * Semua jarak, radius, dan ukuran hidup di sini, bukan tersebar sebagai angka
 * ajaib di dalam composable. Saat desain Figma datang, yang berubah cukup berkas
 * ini — bukan setiap layar. Bingkai Figma berlebar 375 dipakai apa adanya,
 * karena 1 px pada @1x sama dengan 1 dp.
 */
object Tokens {

    /** Skala spasi. Kelipatan 4, jadi semua jarak tetap sebidang. */
    val space1 = 4.dp
    val space2 = 8.dp
    val space3 = 12.dp
    val space4 = 16.dp
    val space5 = 20.dp
    val space6 = 24.dp

    /** Sisi kiri-kanan isi catatan — `p-[16px]` pada node 3:128. */
    val screenPadding = space4

    val radiusSm = 8.dp
    val radiusMd = 10.dp
    val radiusPill = 999.dp

    // ---- Bilah atas catatan (node 3:99) ----

    /** Tinggi bilah atas, dan lebar kotak ikon di kedua ujungnya. */
    val topBarHeight = 56.dp

    /** Titik penanda slot: 24 dp, dengan cincin putih 2 dp pada yang aktif. */
    val dot = 24.dp
    val dotRing = 2.dp
    val dotGap = space4

    /** Baris judul catatan — node 5:1463. */
    val titleRowHeight = 32.dp

    // ---- Bilah bawah (node 3:262) ----

    val navHeight = 56.dp
    val pillWidth = 72.dp
    val pillHeight = 36.dp

    /**
     * Area sentuh minimum. Pedoman Material meminta 48 dp; titik yang hanya
     * 24 dp jauh di bawah itu, jadi sasaran sentuhnya diperbesar tanpa
     * mengubah ukuran lingkaran yang tampak.
     */
    val touchTarget = 48.dp

    val bodyTextSize = 16.sp

    /** `leading-[24px]` pada isi catatan — node 3:132. */
    val bodyLineHeight = 24.sp
    val captionTextSize = 12.sp
}
