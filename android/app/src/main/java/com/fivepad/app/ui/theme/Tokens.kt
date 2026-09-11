package com.fivepad.app.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Titik pendaratan nilai desain.
 *
 * Semua jarak, radius, dan ukuran hidup di sini, bukan tersebar sebagai angka
 * ajaib di dalam composable. Saat desain Figma datang, yang berubah cukup berkas
 * ini — bukan setiap layar. Nilai Figma @1x pada lebar 360 dapat disalin apa
 * adanya, karena 1 px pada @1x sama dengan 1 dp.
 */
object Tokens {

    /** Skala spasi. Kelipatan 4, jadi semua jarak tetap sebidang. */
    val space1 = 4.dp
    val space2 = 8.dp
    val space3 = 12.dp
    val space4 = 16.dp
    val space5 = 20.dp
    val space6 = 24.dp

    /** Sisi kiri-kanan konten utama. */
    val screenPadding = space5

    val radiusSm = 8.dp
    val radiusMd = 10.dp
    val radiusPill = 999.dp

    /** Titik penanda slot: yang terlihat kecil, yang bisa disentuh besar. */
    val dotActive = 20.dp
    val dotInactive = 14.dp

    /**
     * Area sentuh minimum. Pedoman Material meminta 48 dp; titik yang hanya
     * 14–20 dp jauh di bawah itu, jadi sasaran sentuhnya diperbesar tanpa
     * mengubah ukuran lingkaran yang tampak.
     */
    val touchTarget = 48.dp

    val bodyTextSize = 16.sp
    val bodyLineHeight = 25.sp
    val captionTextSize = 12.sp

    /** Opasitas teks sekunder di atas latar slot. */
    const val MUTED_ON_SLOT = 0.55f
    const val FAINT_ON_SLOT = 0.35f
}
