package com.fivepad.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Salinan isi sebuah slot sebelum isinya dibuang.
 *
 * Tabel ini sudah ada di §9 PRD sebagai `note_revisions`, dan dipakai FR-1.11:
 * "Kosongkan slot" menyimpan isi lama lebih dulu, baru menghapusnya. Tanpa itu,
 * satu ketukan pada tindakan yang tidak bisa diurungkan akan memusnahkan
 * catatan sepanjang apa pun — dan konfirmasi saja tidak menolong orang yang
 * menekan "ya" karena kebiasaan.
 *
 * Revisi hanya dibuat oleh tindakan yang merusak, bukan oleh setiap autosave.
 * Menyimpan setiap ketikan akan mengubah tabel ini jadi log tak berujung yang
 * ikut disinkronkan di M2, demi riwayat yang tidak pernah diminta siapa pun.
 */
@Entity(tableName = "note_revisions")
data class NoteRevision(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val slot: Int,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        /** Retensi §9: sepuluh terakhir per slot, dan tidak lebih tua dari 30 hari. */
        const val KEEP_PER_SLOT = 10
        const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }
}
