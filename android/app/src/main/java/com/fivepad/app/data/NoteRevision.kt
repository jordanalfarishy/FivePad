package com.fivepad.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Local recovery text: before clear/restore/import and periodically during editing.
 * Normal edits checkpoint at most once every five minutes; clearing text also
 * checkpoints immediately. Retention is bounded to ten versions and thirty days.
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
