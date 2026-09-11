package com.fivepad.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Grup bernama di dalam daftar tugas.
 *
 * Sengaja **tanpa** kolom induk: grup hanya satu tingkat. Begitu grup boleh
 * berisi grup, kita sudah membangun folder lewat pintu belakang — persis yang
 * ditolak §18. Batas itu ditegakkan dengan tidak menyediakan kolomnya sama
 * sekali, bukan dengan pemeriksaan di UI yang bisa terlewat.
 */
@Entity(tableName = "todo_groups")
data class TodoGroup(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val position: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis(),
    val clientUpdatedAt: Long = System.currentTimeMillis(),
    val deviceId: String? = null,
    val deletedAt: Long? = null,
) {
    companion object {
        const val MAX_NAME_LENGTH = 40
    }
}
