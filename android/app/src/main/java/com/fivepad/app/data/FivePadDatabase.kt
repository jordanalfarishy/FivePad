package com.fivepad.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class, Todo::class], version = 1, exportSchema = true)
abstract class FivePadDatabase : RoomDatabase() {

    abstract fun notes(): NoteDao
    abstract fun todos(): TodoDao

    companion object {
        fun build(context: Context): FivePadDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                FivePadDatabase::class.java,
                "fivepad.db",
            ).addCallback(SeedFiveSlots).build()

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
    }
}
