package com.fivepad.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Satu dari lima slot catatan permanen.
 *
 * Slot di-seed sekali saat basis data dibuat dan tidak pernah bertambah maupun
 * berkurang — itu prinsip P1 di PRD. Karena [slot] adalah primary key dengan
 * nilai tetap 1..5, tidak ada jalur di lapisan data yang bisa membuat slot keenam.
 *
 * Kolom [clientUpdatedAt] dan [deviceId] belum dipakai di M1; keduanya sudah ada
 * sejak awal supaya M2 (sinkronisasi) tidak perlu migrasi skema.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val slot: Int,
    val label: String = "",
    val body: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val clientUpdatedAt: Long = System.currentTimeMillis(),
    val deviceId: String? = null,
) {
    companion object {
        const val SLOT_COUNT = 5
        const val MAX_LABEL_LENGTH = 24
        const val MAX_BODY_LENGTH = 50_000
        const val BODY_WARN_LENGTH = 45_000
    }
}
