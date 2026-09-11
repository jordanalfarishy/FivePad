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

    /** Selesai turun ke bawah (FR-2.8), sisanya mengikuti urutan manual. */
    @Query("SELECT * FROM todos WHERE deletedAt IS NULL ORDER BY done ASC, position ASC")
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
}
