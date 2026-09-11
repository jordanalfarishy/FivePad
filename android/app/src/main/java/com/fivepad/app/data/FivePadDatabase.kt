package com.fivepad.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Note::class, Todo::class, TodoGroup::class, NoteRevision::class],
    version = 3,
    exportSchema = true,
)
abstract class FivePadDatabase : RoomDatabase() {

    abstract fun notes(): NoteDao
    abstract fun todos(): TodoDao
    abstract fun todoGroups(): TodoGroupDao
    abstract fun noteRevisions(): NoteRevisionDao

    companion object {
        fun build(context: Context): FivePadDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                FivePadDatabase::class.java,
                "fivepad.db",
            )
                .addCallback(SeedFiveSlots)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        /**
         * Menanam tepat lima slot kosong saat basis data pertama dibuat.
         *
         * Ini satu-satunya tempat baris `notes` pernah dibuat — tidak ada insert
         * lain di mana pun, sehingga jumlah slot secara struktural terkunci di lima.
         */
        private object SeedFiveSlots : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                for (slot in 1..Note.SLOT_COUNT) {
                    db.execSQL(
                        "INSERT INTO notes (slot, label, body, updatedAt, clientUpdatedAt, deviceId) " +
                            "VALUES (?, '', '', ?, ?, NULL)",
                        arrayOf<Any>(slot, now, now),
                    )
                }
            }
        }

        /** Riwayat isi slot masuk di v3, untuk FR-1.11. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_revisions (
                        id TEXT NOT NULL PRIMARY KEY,
                        slot INTEGER NOT NULL,
                        body TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Grup tugas masuk di v2. Migrasi ditulis tangan, bukan destruktif —
         * basis data ini sudah memuat catatan sungguhan di perangkat, dan
         * fallback destruktif akan membuangnya tanpa peringatan.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN groupId TEXT DEFAULT NULL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS todo_groups (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        position REAL NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        clientUpdatedAt INTEGER NOT NULL,
                        deviceId TEXT,
                        deletedAt INTEGER
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
