package com.fivepad.app.ui

import android.content.Intent
import com.fivepad.app.data.Note

/**
 * Apa yang diminta oleh niat (intent) yang membuka aplikasi.
 *
 * FR-6.2 membedakan dua cara membuka: dari peluncur, aplikasi hanya tampil;
 * dari widget atau ubin Pengaturan Cepat, papan ketik langsung terbuka supaya
 * tangkap-cepat benar-benar cepat. Pembedanya harus datang dari niatnya, bukan
 * dari tebakan — `ACTION_MAIN` tidak membawa data maupun extra, jadi peluncur
 * otomatis jatuh ke perilaku "hanya tampil" tanpa perlu diperiksa khusus.
 *
 * Widget dan ubin Pengaturan Cepat memakai tautan `fivepad://slot/3`.
 * ACTION_SEND membawa teks ke pemilih slot tanpa menimpa catatan secara otomatis.
 */
data class LaunchRequest(
    val slot: Int? = null,
    val focusEditor: Boolean = false,
    val sharedText: String? = null,
) {
    companion object {
        const val EXTRA_SLOT = "com.fivepad.app.extra.SLOT"
        const val EXTRA_FOCUS_EDITOR = "com.fivepad.app.extra.FOCUS_EDITOR"

        private const val SCHEME = "fivepad"
        private const val HOST_SLOT = "slot"

        fun from(intent: Intent?): LaunchRequest {
            if (intent == null) return LaunchRequest()
            if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                return LaunchRequest(sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString())
            }

            val data = intent.data
            if (data != null && data.scheme == SCHEME && data.host == HOST_SLOT) {
                // Tautan dibuka dengan sengaja untuk menulis, jadi papan ketik
                // terbuka kecuali penautnya menyatakan sebaliknya.
                return LaunchRequest(
                    slot = data.lastPathSegment?.toIntOrNull()?.takeIf { it in SLOTS },
                    focusEditor = data.getQueryParameter("focus") != "0",
                )
            }

            return LaunchRequest(
                slot = intent.getIntExtra(EXTRA_SLOT, 0).takeIf { it in SLOTS },
                focusEditor = intent.getBooleanExtra(EXTRA_FOCUS_EDITOR, false),
            )
        }

        private val SLOTS = 1..Note.SLOT_COUNT
    }
}
