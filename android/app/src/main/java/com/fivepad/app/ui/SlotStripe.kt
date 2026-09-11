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
import androidx.compose.ui.unit.dp
import com.fivepad.app.ui.theme.Tokens

/**
 * Pita 4 dp di bawah nama catatan, dalam warna slot yang sedang terbuka.
 *
 * Setiap slot punya **pola isian yang berbeda**, bukan hanya warna yang
 * berbeda. Sekitar satu dari dua belas pria mengalami buta warna merah-hijau;
 * bagi mereka slot 1 (oranye) dan slot 3 (hijau) adalah dua rona lumpur yang
 * nyaris sama, dan sejak latar selayar penuh dilepas, warna itulah satu-satunya
 * yang menjawab "saya sedang di slot mana". Pola menjadikannya dua saluran
 * informasi, bukan satu — persis yang diminta NFR-8, dan pendekatan yang sama
 * dipakai mode buta warna Trello.
 *
 * Polanya sengaja dibedakan oleh **panjang goresan dan arah**, bukan oleh
 * kerapatan saja: pada pita setinggi 4 dp, dua pola yang hanya berbeda
 * kerapatannya akan terbaca sama begitu layar dilihat sambil lalu.
 *
 * Polanya selalu menyala, tanpa sakelar. Aksesibilitas yang disembunyikan di
 * balik pengaturan adalah aksesibilitas yang tidak pernah ditemukan orang yang
 * membutuhkannya, dan pada pita 4 dp biayanya bagi yang lain praktis nol.
 */
@Composable
fun SlotStripe(slot: Int, colour: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(Tokens.stripeHeight),
    ) {
        when (slot) {
            1 -> drawRect(colour, Offset.Zero, size)
            2 -> dashes(colour, on = 12.dp.toPx(), off = 6.dp.toPx())
            3 -> dashes(colour, on = 4.dp.toPx(), off = 4.dp.toPx())
            4 -> diagonals(colour, on = 6.dp.toPx(), off = 6.dp.toPx())
            else -> rails(colour)
        }
    }
}

/** Garis putus-putus tegak lurus. Panjang goresan yang membedakan slot 2 dari slot 3. */
private fun DrawScope.dashes(colour: Color, on: Float, off: Float) {
    var x = 0f
    while (x < size.width) {
        drawRect(colour, Offset(x, 0f), Size(minOf(on, size.width - x), size.height))
        x += on + off
    }
}

/**
 * Goresan miring. Kemiringannya persis setinggi pita, jadi sudutnya 45° pada
 * kepadatan layar mana pun — bukan sudut yang berubah-ubah mengikuti perangkat.
 */
private fun DrawScope.diagonals(colour: Color, on: Float, off: Float) {
    val h = size.height
    var x = -h
    while (x < size.width) {
        val path = Path().apply {
            moveTo(x, h)
            lineTo(x + on, h)
            lineTo(x + on + h, 0f)
            lineTo(x + h, 0f)
            close()
        }
        drawPath(path, colour)
        x += on + off
    }
}

/** Dua rel tipis dengan celah di tengah — satu-satunya pola yang terpecah mendatar. */
private fun DrawScope.rails(colour: Color) {
    val rail = size.height * 0.35f
    drawRect(colour, Offset.Zero, Size(size.width, rail))
    drawRect(colour, Offset(0f, size.height - rail), Size(size.width, rail))
}
