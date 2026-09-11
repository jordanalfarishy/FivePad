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
 * Produsen niat ini — widget (FR-6.3) dan ubin (FR-6.5) — baru datang di M4.
 * Penanganannya dibuat lebih dulu supaya keduanya tinggal mengirim niat, dan
 * jalur ini sudah bisa diuji hari ini lewat tautan `fivepad://slot/3`.
 */
data class LaunchRequest(
    val slot: Int? = null,
    val focusEditor: Boolean = false,
) {
    companion object {
        const val EXTRA_SLOT = "com.fivepad.app.extra.SLOT"
        const val EXTRA_FOCUS_EDITOR = "com.fivepad.app.extra.FOCUS_EDITOR"

        private const val SCHEME = "fivepad"
        private const val HOST_SLOT = "slot"

        fun from(intent: Intent?): LaunchRequest {
            if (intent == null) return LaunchRequest()

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
