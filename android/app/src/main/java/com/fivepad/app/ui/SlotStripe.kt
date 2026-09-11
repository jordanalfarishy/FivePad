package com.fivepad.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.fivepad.app.ui.theme.Tokens

/**
 * Seberapa terang nada kedua pita terhadap aksennya.
 *
 * Pola dibentuk oleh dua nada dari warna yang sama, bukan oleh warna aksen dan
 * celah kosong. Celah kosong berarti latar hampir-hitam ikut jadi bagian pola:
 * hasilnya pita yang terlihat rusak atau setengah terhapus, dan tepi atas-bawah
 * pita jadi bergerigi. Dengan dua nada, pita tetap satu bidang utuh dan yang
 * berubah hanya teksturnya.
 */
private const val TINT = 0.38f

/**
 * Pita 4 dp di bawah nama catatan, dalam warna slot yang sedang terbuka.
 *
 * Setiap slot punya **pola isian yang berbeda**, bukan hanya warna yang
 * berbeda. Sekitar satu dari dua belas pria mengalami buta warna merah-hijau;
 * bagi mereka slot 1 (oranye) dan slot 3 (hijau) adalah dua rona lumpur yang
 * nyaris sama, dan sejak latar selayar penuh dilepas, warna itulah satu-satunya
 * yang menjawab "saya sedang di slot mana". Pola menjadikannya dua saluran
 * informasi, bukan satu — persis yang diminta NFR-8.
 *
 * Kelima polanya mengikuti mode buta warna Trello: kisi silang, belah ketupat,
 * miring kanan, tegak, dan miring kiri. Tidak ada yang polos — pola yang polos
 * bukan pola, dan slot yang memakainya akan jadi satu-satunya yang kembali
 * bergantung pada warna saja.
 *
 * Slot 1 dan slot 2 adalah pasangan paling berisiko tertukar — keduanya
 * berbasis belah ketupat. Yang memisahkannya dibuat dua lapis: **figur lawan
 * dasar** (slot 1 sebagian besar aksen dengan kisi tipis di atasnya, slot 2
 * sebagian besar nada terang dengan segitiga aksen di sela-selanya) dan
 * **skala** (kisi 9 dp lawan ketupat 4 dp). Satu lapis saja tidak cukup: dua
 * pola yang hanya berbeda kerapatan akan terbaca sama pada pita setinggi 4 dp.
 *
 * Polanya selalu menyala, tanpa sakelar. Aksesibilitas yang disembunyikan di
 * balik pengaturan adalah aksesibilitas yang tidak pernah ditemukan orang yang
 * membutuhkannya, dan pada pita 4 dp biayanya bagi yang lain praktis nol.
 */
@Composable
fun SlotStripe(slot: Int, colour: Color, modifier: Modifier = Modifier) {
    val tint = lerp(colour, Color.White, TINT)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(Tokens.stripeHeight),
    ) {
        drawRect(colour, Offset.Zero, size)
        when (slot) {
            1 -> crosshatch(tint, stroke = 1.2.dp.toPx(), period = 9.dp.toPx())
            2 -> diamonds(tint, period = 4.dp.toPx())
            3 -> diagonals(tint, on = 3.dp.toPx(), period = 8.dp.toPx(), leansRight = true)
            4 -> verticals(tint, on = 2.dp.toPx(), period = 5.dp.toPx())
            else -> diagonals(tint, on = 3.dp.toPx(), period = 8.dp.toPx(), leansRight = false)
        }
    }
}

/** Goresan miring, setebal [on] dan berjarak [period]. Kemiringannya tepat 45°. */
private fun DrawScope.diagonals(tint: Color, on: Float, period: Float, leansRight: Boolean) {
    val h = size.height
    var x = -h
    while (x < size.width) {
        val path = Path().apply {
            if (leansRight) {
                moveTo(x, h)
                lineTo(x + on, h)
                lineTo(x + on + h, 0f)
                lineTo(x + h, 0f)
            } else {
                moveTo(x, 0f)
                lineTo(x + on, 0f)
                lineTo(x + on + h, h)
                lineTo(x + h, h)
            }
            close()
        }
        drawPath(path, tint)
        x += period
    }
}

/**
 * Kisi belah ketupat: goresan tipis ke dua arah sekaligus.
 *
 * Sebagian besar pita tetap warna aksen dan kisinya hanya garis di atasnya —
 * kebalikan dari [diamonds], yang justru didominasi nada terang.
 */
private fun DrawScope.crosshatch(tint: Color, stroke: Float, period: Float) {
    diagonals(tint, on = stroke, period = period, leansRight = true)
    diagonals(tint, on = stroke, period = period, leansRight = false)
}

/**
 * Belah ketupat yang saling bersinggungan ujungnya — papan catur yang diputar
 * 45°, sama seperti label kuning Trello. Sisa ruangnya membentuk segitiga
 * warna aksen di tepi atas dan bawah.
 */
private fun DrawScope.diamonds(tint: Color, period: Float) {
    val h = size.height
    val half = period / 2f
    var cx = 0f
    while (cx - half < size.width) {
        val path = Path().apply {
            moveTo(cx - half, h / 2f)
            lineTo(cx, 0f)
            lineTo(cx + half, h / 2f)
            lineTo(cx, h)
            close()
        }
        drawPath(path, tint)
        cx += period
    }
}

/** Goresan tegak. Satu-satunya pola tanpa kemiringan sama sekali. */
private fun DrawScope.verticals(tint: Color, on: Float, period: Float) {
    var x = 0f
    while (x < size.width) {
        drawRect(tint, Offset(x, 0f), Size(minOf(on, size.width - x), size.height))
        x += period
    }
}
