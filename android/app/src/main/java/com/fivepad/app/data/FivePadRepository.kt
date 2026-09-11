package com.fivepad.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class FivePadRepository(private val db: FivePadDatabase) {

    private val notes = db.notes()
    private val todos = db.todos()
    private val groups = db.todoGroups()

    fun observeNotes(): Flow<List<Note>> = notes.observeAll()

    fun observeTodos(): Flow<List<Todo>> = todos.observeActive()

    fun observeGroups(): Flow<List<TodoGroup>> = groups.observeActive()

    suspend fun saveBody(slot: Int, body: String) =
        notes.updateBody(slot, body.take(Note.MAX_BODY_LENGTH), now())

    suspend fun saveLabel(slot: Int, label: String) =
        notes.updateLabel(slot, label.take(Note.MAX_LABEL_LENGTH), now())

    /**
     * Tugas baru selalu mendarat di akhir grupnya. Jarak [POSITION_GAP] menyisakan
     * ruang di antara dua tugas untuk penyisipan nanti tanpa menyentuh baris lain.
     */
    suspend fun addTodo(text: String, groupId: String? = null, dueAt: Long? = null): Todo? {
        val trimmed = text.trim().take(Todo.MAX_TEXT_LENGTH)
        if (trimmed.isEmpty()) return null
        val todo = Todo(
            text = trimmed,
            position = todos.maxPosition() + POSITION_GAP,
            groupId = groupId,
            dueAt = dueAt,
        )
        todos.insert(todo)
        return todo
    }

    suspend fun setTodoDone(id: String, done: Boolean) = todos.setDone(id, done, now())

    suspend fun setTodoText(id: String, text: String) =
        todos.setText(id, text.trim().take(Todo.MAX_TEXT_LENGTH), now())

    suspend fun setTodoGroup(id: String, groupId: String?) = todos.setGroup(id, groupId, now())

    suspend fun setTodoDue(id: String, dueAt: Long?) = todos.setDue(id, dueAt, now())

    suspend fun findTodo(id: String): Todo? = todos.find(id)

    suspend fun pendingReminders(now: Long): List<Todo> = todos.pendingReminders(now)

    /**
     * Menempatkan sebuah baris di antara dua tetangganya.
     *
     * Karena posisi bertipe pecahan, menyisipkan cukup mengambil nilai tengah —
     * hanya satu baris yang ditulis, bukan seluruh daftar. Itu penting saat
     * sinkronisasi masuk di M2: satu baris berubah berarti satu baris dikirim.
     */
    private fun between(before: Double?, after: Double?): Double = when {
        before == null && after == null -> POSITION_GAP
        before == null -> after!! - POSITION_GAP
        after == null -> before + POSITION_GAP
        else -> (before + after) / 2
    }

    suspend fun moveTodo(id: String, before: Double?, after: Double?) =
        todos.setPosition(id, between(before, after), now())

    /**
     * Memindahkan tugas ke grup lain sekaligus ke posisi barunya.
     *
     * Keduanya satu transaksi: kalau hanya grupnya yang tersimpan, tugas mendarat
     * di grup tujuan pada urutan lamanya — melompat ke tempat yang tidak dituju
     * siapa pun, dan dari layar tidak terlihat sebagai kegagalan.
     */
    suspend fun moveTodoToGroup(
        id: String,
        groupId: String?,
        before: Double?,
        after: Double?,
    ) = db.withTransaction {
        val stamp = now()
        todos.setGroup(id, groupId, stamp)
        todos.setPosition(id, between(before, after), stamp)
    }

    suspend fun moveGroup(id: String, before: Double?, after: Double?) =
        groups.setPosition(id, between(before, after), now())

    suspend fun deleteTodo(id: String) = todos.softDelete(id, now())

    suspend fun restoreTodo(id: String) = todos.restore(id, now())

    suspend fun addGroup(name: String): String? {
        val trimmed = name.trim().take(TodoGroup.MAX_NAME_LENGTH)
        if (trimmed.isEmpty()) return null
        val group = TodoGroup(name = trimmed, position = groups.maxPosition() + POSITION_GAP)
        groups.insert(group)
        return group.id
    }

    suspend fun renameGroup(id: String, name: String) =
        groups.rename(id, name.trim().take(TodoGroup.MAX_NAME_LENGTH), now())

    /**
     * FR-2.15: tugas di dalam grup **tidak** ikut terhapus, hanya kehilangan
     * grupnya. Keduanya dijalankan dalam satu transaksi supaya tidak pernah ada
     * keadaan di mana grup sudah hilang tapi tugasnya masih menunjuk ke sana —
     * keadaan itu akan membuat tugas menghilang dari layar tanpa pernah dihapus.
     */
    suspend fun deleteGroup(id: String) {
        val stamp = now()
        db.withTransaction {
            todos.detachFromGroup(id, stamp)
            groups.softDelete(id, stamp)
        }
    }

    private fun now() = System.currentTimeMillis()

    private companion object {
        const val POSITION_GAP = 1024.0
    }
}
