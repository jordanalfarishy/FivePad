package com.fivepad.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY slot")
    fun observeAll(): Flow<List<Note>>

    @Query("UPDATE notes SET body = :body, updatedAt = :now, clientUpdatedAt = :now WHERE slot = :slot")
    suspend fun updateBody(slot: Int, body: String, now: Long)

    @Query("UPDATE notes SET label = :label, updatedAt = :now, clientUpdatedAt = :now WHERE slot = :slot")
    suspend fun updateLabel(slot: Int, label: String, now: Long)
}

@Dao
interface TodoDao {

    /**
     * Selesai turun ke bawah (FR-2.8), sisanya mengikuti urutan manual.
     *
     * `id` menutup kemungkinan seri: posisi pecahan biasanya unik, tapi dua
     * baris bisa kebetulan bernilai sama, dan tanpa pemecah-seri urutannya
     * jadi tak tentu — daftar yang berubah sendiri antar pembukaan.
     */
    @Query("SELECT * FROM todos WHERE deletedAt IS NULL ORDER BY done ASC, position ASC, id ASC")
    fun observeActive(): Flow<List<Todo>>

    @Query("SELECT COALESCE(MAX(position), 0.0) FROM todos WHERE deletedAt IS NULL")
    suspend fun maxPosition(): Double

    @Insert
    suspend fun insert(todo: Todo)

    @Query("UPDATE todos SET done = :done, updatedAt = :now, clientUpdatedAt = :now WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean, now: Long)

    @Query("UPDATE todos SET text = :text, updatedAt = :now, clientUpdatedAt = :now WHERE id = :id")
    suspend fun setText(id: String, text: String, now: Long)

    @Query("UPDATE todos SET deletedAt = :now, updatedAt = :now, clientUpdatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE todos SET deletedAt = NULL, updatedAt = :now, clientUpdatedAt = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long)

    @Query("UPDATE todos SET groupId = :groupId, updatedAt = :now, clientUpdatedAt = :now WHERE id = :id")
    suspend fun setGroup(id: String, groupId: String?, now: Long)

    @Query("UPDATE todos SET dueAt = :dueAt, updatedAt = :now, clientUpdatedAt = :now WHERE id = :id")
    suspend fun setDue(id: String, dueAt: Long?, now: Long)

    @Query("UPDATE todos SET position = :position, updatedAt = :now, clientUpdatedAt = :now WHERE id = :id")
    suspend fun setPosition(id: String, position: Double, now: Long)

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun find(id: String): Todo?

    /** Tugas yang masih menunggu pengingat — dipakai menjadwalkan ulang sesudah reboot. */
    @Query(
        "SELECT * FROM todos WHERE deletedAt IS NULL AND done = 0 " +
            "AND dueAt IS NOT NULL AND dueAt > :now",
    )
    suspend fun pendingReminders(now: Long): List<Todo>

    /**
     * FR-2.15: menghapus grup mengembalikan tugasnya menjadi tanpa grup,
     * bukan ikut menghapusnya. Penghapusan data tidak boleh jadi efek samping
     * tersembunyi dari tindakan yang tampak sepele.
     */
    @Query("UPDATE todos SET groupId = NULL, updatedAt = :now, clientUpdatedAt = :now WHERE groupId = :groupId")
    suspend fun detachFromGroup(groupId: String, now: Long)
}

@Dao
interface TodoGroupDao {

    @Query("SELECT * FROM todo_groups WHERE deletedAt IS NULL ORDER BY position ASC, id ASC")
    fun observeActive(): Flow<List<TodoGroup>>

    @Query("SELECT COALESCE(MAX(position), 0.0) FROM todo_groups WHERE deletedAt IS NULL")
    suspend fun maxPosition(): Double

    @Insert
    suspend fun insert(group: TodoGroup)

    @Query("UPDATE todo_groups SET name = :name, updatedAt = :now, clientUpdatedAt = :now WHERE id = :id")
    suspend fun rename(id: String, name: String, now: Long)

    @Query("UPDATE todo_groups SET deletedAt = :now, updatedAt = :now, clientUpdatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE todo_groups SET position = :position, updatedAt = :now, clientUpdatedAt = :now WHERE id = :id")
    suspend fun setPosition(id: String, position: Double, now: Long)
}
