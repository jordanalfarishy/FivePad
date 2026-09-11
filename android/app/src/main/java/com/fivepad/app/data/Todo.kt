package com.fivepad.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Satu tugas pada daftar tugas global.
 *
 * [position] sengaja bertipe pecahan: menyisipkan tugas di antara dua tugas lain
 * cukup dengan mengambil nilai tengahnya, sehingga menggeser satu baris tidak
 * memaksa penulisan ulang seluruh daftar.
 *
 * Penghapusan memakai [deletedAt] alih-alih membuang baris. Di M1 ini hanya
 * membuat undo mudah; di M2 ia menjadi wajib, karena tanpanya perangkat yang lama
 * luring akan menganggap baris yang hilang sebagai baris baru dan menghidupkannya
 * kembali saat menyinkron.
 */
@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val done: Boolean = false,
    val position: Double = 0.0,
    val dueAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val clientUpdatedAt: Long = System.currentTimeMillis(),
    val deviceId: String? = null,
    val deletedAt: Long? = null,
) {
    companion object {
        const val MAX_TEXT_LENGTH = 500
    }
}
